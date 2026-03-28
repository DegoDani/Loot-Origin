package com.degodani.lootorigin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
	name = "Loot Origin",
	description = "Shows where you obtained specific items from"
)
public class LootOriginPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ScheduledExecutorService executor;

	private LootOriginPanel panel;
	private NavigationButton navButton;
	private final Map<String, Map<String, Integer>> masterLootVault = new HashMap<>();
	private File vaultFile;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Loot Origin started!");

		// Initialize panel
		panel = new LootOriginPanel(this);

		final BufferedImage icon = ImageUtil.loadImageResource(LootOriginPlugin.class, "/icon.png");

		// Button
		navButton = NavigationButton.builder()
				.tooltip("Loot Origin")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
		vaultFile = new File(RuneLite.RUNELITE_DIR, "loot-origin-vault.json");
		loadVault();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Loot Origin stopped!");
		// Remove the button when the plugin is turned off
		clientToolbar.removeNavigation(navButton);
	}

	// This runs when you open RuneLite
	private void loadVault() {
		if (!vaultFile.exists()) return;

		try (FileReader reader = new FileReader(vaultFile)) {
			// Gson
			Type type = new TypeToken<Map<String, Map<String, Integer>>>(){}.getType();
			Map<String, Map<String, Integer>> loaded = gson.fromJson(reader, type);

			if (loaded != null) {
				masterLootVault.clear();
				masterLootVault.putAll(loaded);
			}
		} catch (Exception e) {
			log.error("Failed to load Vault from hard drive", e);
		}
	}

	// RuneLite's official background worker
	private void saveVault() {
		executor.submit(() -> {
			try (FileWriter writer = new FileWriter(vaultFile)) {
				gson.toJson(masterLootVault, writer);
			} catch (Exception e) {
				log.error("Failed to save Vault", e);
			}
		});
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event) {
		String sourceName = event.getNpc().getName();
		if (sourceName == null) return;

		// Loop through every item the monster just dropped
		for (ItemStack item : event.getItems()) {
			String itemName = itemManager.getItemComposition(item.getId()).getName().toLowerCase();

			masterLootVault.putIfAbsent(itemName, new HashMap<>());
			Map<String, Integer> sources = masterLootVault.get(itemName);

			int currentTotal = sources.getOrDefault(sourceName, 0);
			sources.put(sourceName, currentTotal + item.getQuantity());
		}
		saveVault();
	}
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Check if it's an item, holding shift, only when examine option is built
		if (event.getItemId() != -1 && client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals("Examine"))
		{
			client.createMenuEntry(-1)
					.setOption("Check origin")
					.setTarget(event.getTarget())
					.setIdentifier(event.getItemId())
					.setType(MenuAction.RUNELITE)
					.setForceLeftClick(true)
					.onClick(e -> {
						String itemName = itemManager.getItemComposition(event.getItemId()).getName();
						SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
						calculateOrigins(itemName);
					});
		}
	}
	private void calculateOrigins(String targetItemName) {
		Map<String, Integer> sourceTotals = masterLootVault.get(targetItemName.toLowerCase());

		if (sourceTotals == null || sourceTotals.isEmpty()) {
			panel.showText("No records found in Vault for: " + targetItemName + "\n\nDid you import your data yet?");
			return;
		}

		int grandTotal = 0;
		for (int qty : sourceTotals.values()) {
			grandTotal += qty;
		}

		// Sort from highest to lowest
		List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(sourceTotals.entrySet());
		sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

		// Build the display text
		StringBuilder sb = new StringBuilder();
		sb.append("Origins for: ").append(targetItemName).append("\n");
		sb.append("Total Tracked: ").append(grandTotal).append("\n");
		sb.append("--------------------------\n\n");

		for (Map.Entry<String, Integer> entry : sortedList) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}

		panel.showText(sb.toString());
	}
	public void importData() {
		// Open the file browser on the main UI thread
		SwingUtilities.invokeLater(() -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select RuneLite Loot Tracker JSON");

			// If user selects file and clicks open
			if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				panel.showText("Importing data... please wait!");

				executor.submit(() -> {
					try (FileReader reader = new FileReader(file)) {
						OfficialLootRecord[] records = gson.fromJson(reader, OfficialLootRecord[].class);

						if (records != null) {
							// Empty the vault just in case they click import twice
							masterLootVault.clear();

							for (OfficialLootRecord record : records) {
								if (record.drops != null) {
									for (OfficialLootRecord.OfficialLootDrop drop : record.drops) {
										if (drop.name == null) continue;
										String itemName = drop.name.toLowerCase();
										masterLootVault.putIfAbsent(itemName, new HashMap<>());
										Map<String, Integer> sources = masterLootVault.get(itemName);
										int currentTotal = sources.getOrDefault(record.name, 0);
										sources.put(record.name, currentTotal + drop.qty);
									}
								}
							}
							panel.showText("Success! Imported data from " + records.length + " different sources into the Vault.\n\nYou can now check items instantly!");

							saveVault();
						}
					} catch (Exception e) {
						log.error("Failed to parse official data", e);
						panel.showText("Error reading the JSON file. Check the console.");
					}
				});
			}
		});
	}
}