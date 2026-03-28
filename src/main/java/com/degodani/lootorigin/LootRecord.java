package com.degodani.lootorigin;

import java.util.List;

// This class is our "blueprint" so Java understands the Loot Tracker files
public class LootRecord {

    public String name; // The source of the loot (e.g., "Zulrah" or "Barrows")
    public List<LootDrop> drops; // A list of all the items that dropped

    // This is the blueprint for the individual items inside the drop list
    public static class LootDrop {
        public int id;
        public String name; // The item name (e.g., "Papaya tree seed")
        public int quantity; // How many dropped
    }
}