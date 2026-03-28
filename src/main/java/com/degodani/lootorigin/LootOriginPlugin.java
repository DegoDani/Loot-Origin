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
	private ItemManager itemManager; // We need this to get the actual Name of the item, not just the ID!

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

		// Initialize your new panel
		panel = new LootOriginPanel(this);

		final BufferedImage icon = ImageUtil.loadImageResource(LootOriginPlugin.class, "/icon.png");

		// Build the button for the RuneLite sidebar
		navButton = NavigationButton.builder()
				.tooltip("Loot Origin")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		// Add the button to the toolbar
		clientToolbar.addNavigation(navButton);
		// Tell the plugin where to save the file, and load it immediately!
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

	// This runs instantly when you open RuneLite
	private void loadVault() {
		if (!vaultFile.exists()) return; // If it's your first time, the file won't exist yet

		try (FileReader reader = new FileReader(vaultFile)) {
			// This tells Gson exactly what shape the vault data should be
			Type type = new TypeToken<Map<String, Map<String, Integer>>>(){}.getType();
			Map<String, Map<String, Integer>> loaded = new Gson().fromJson(reader, type);

			if (loaded != null) {
				masterLootVault.clear();
				masterLootVault.putAll(loaded);
			}
		} catch (Exception e) {
			log.error("Failed to load Vault from hard drive", e);
		}
	}

	// Uses RuneLite's official background worker! No more zombies!
	private void saveVault() {
		executor.submit(() -> {
			try (FileWriter writer = new FileWriter(vaultFile)) {
				new Gson().toJson(masterLootVault, writer);
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
			// Find the actual name of the item using its ID
			String itemName = itemManager.getItemComposition(item.getId()).getName().toLowerCase();

			// Standard vault math: Find the shelf, update the quantity
			masterLootVault.putIfAbsent(itemName, new HashMap<>());
			Map<String, Integer> sources = masterLootVault.get(itemName);

			int currentTotal = sources.getOrDefault(sourceName, 0);
			sources.put(sourceName, currentTotal + item.getQuantity());
		}

		// Silently save the updated Vault to your hard drive!
		saveVault();
	}
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// 1. Check if it's an item
		// 2. Check if holding SHIFT
		// 3. ONLY trigger when the "Examine" option is being built to prevent duplicates
		if (event.getItemId() != -1 && client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals("Examine"))
		{
			client.createMenuEntry(-1)
					.setOption("Check origin")
					.setTarget(event.getTarget())
					.setIdentifier(event.getItemId())
					.setType(MenuAction.RUNELITE)
					.setForceLeftClick(true) // This tells RuneLite to make this the Shift+Left Click action!
					.onClick(e -> {
						// Get the item's name
						String itemName = itemManager.getItemComposition(event.getItemId()).getName();

						// The Force-Open Command! (Safely wrapped so it doesn't cause UI stuttering)
						SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));

						// Fire up the search engine!
						calculateOrigins(itemName);
					});
		}
	}
	private void calculateOrigins(String targetItemName) {
		// Look in the vault for the item (convert to lowercase to match our vault format)
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
		// Open the file browser on the main UI thread safely
		SwingUtilities.invokeLater(() -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select RuneLite Loot Tracker JSON");

			// If the user selects a file and clicks "Open"
			if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				panel.showText("Importing data... please wait!");

				// Use RuneLite's official background worker to read the massive file!
				executor.submit(() -> {
					try (FileReader reader = new FileReader(file)) {
						Gson gson = new Gson();
						OfficialLootRecord[] records = gson.fromJson(reader, OfficialLootRecord[].class);

						if (records != null) {
							// Empty the vault just in case they click import twice
							masterLootVault.clear();

							for (OfficialLootRecord record : records) {
								if (record.drops != null) {
									for (OfficialLootRecord.OfficialLootDrop drop : record.drops) {
										if (drop.name == null) continue;

										// Standardize the name to lowercase
										String itemName = drop.name.toLowerCase();

										// If the vault doesn't have a shelf for this item yet, build one!
										masterLootVault.putIfAbsent(itemName, new HashMap<>());

										// Get the shelf for this item
										Map<String, Integer> sources = masterLootVault.get(itemName);

										// Add the quantity to whatever is already there
										int currentTotal = sources.getOrDefault(record.name, 0);
										sources.put(record.name, currentTotal + drop.qty);
									}
								}
							}
							panel.showText("Success! Imported data from " + records.length + " different sources into the Vault.\n\nYou can now check items instantly!");

							// Save it permanently!
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