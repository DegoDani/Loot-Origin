package com.degodani.lootorigin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LootOriginPanel extends PluginPanel {

    private final JTextArea resultsArea;
    private final LootOriginPlugin plugin;

    public LootOriginPanel(LootOriginPlugin plugin) {
        super();
        this.plugin = plugin;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Loot Origin");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        resultsArea = new JTextArea("Shift + LMB an item and click 'check origin' option to see data here!");
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setEditable(false);
        resultsArea.setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(resultsArea, BorderLayout.CENTER);

        JButton importButton = new JButton("Import Official Web Data");

        importButton.addActionListener(e -> plugin.importData());

        add(importButton, BorderLayout.SOUTH);
    }

    public void showText(String newText) {
        SwingUtilities.invokeLater(() -> {
            resultsArea.setText(newText);
        });
    }
}