package com.degodani.lootorigin;

import java.util.List;

public class OfficialLootRecord {
    public String name;
    public int count;
    public String type;
    public List<OfficialLootDrop> drops;

    public static class OfficialLootDrop {
        public int id;
        public String name;
        public int qty;
    }
}