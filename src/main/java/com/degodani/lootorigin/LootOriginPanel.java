package com.degodani.lootorigin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LootOriginPanel extends PluginPanel {

    // 1. The variables
    private final JTextArea resultsArea;
    private final LootOriginPlugin plugin;

    // 2. The Constructor (THIS is the puzzle piece that catches "this")
    // Make sure it says (LootOriginPlugin plugin) inside the parentheses!
    public LootOriginPanel(LootOriginPlugin plugin) {
        super();
        this.plugin = plugin;

        // Give the panel a nice 10-pixel border
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        // Create a title at the top
        JLabel titleLabel = new JLabel("Loot Origin");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Create the text area that will eventually hold our loot data
        resultsArea = new JTextArea("Shift + LMB an item and click 'check origin' option to see data here!");
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setEditable(false); // Prevents you from accidentally typing in it
        resultsArea.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Add the text area to the center of the panel
        add(resultsArea, BorderLayout.CENTER);

        // Create the Import button and stick it at the bottom
        JButton importButton = new JButton("Import Official Web Data");

        // When clicked, tell the main plugin to run the import method (which we will write in Step 3)
        importButton.addActionListener(e -> plugin.importData());

        add(importButton, BorderLayout.SOUTH);
    }

    // The Safety Valve: This ensures text updates NEVER cause game lag!
    public void showText(String newText) {
        SwingUtilities.invokeLater(() -> {
            resultsArea.setText(newText);
        });
    }
}