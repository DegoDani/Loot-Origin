package com.degodani.lootorigin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LootOriginPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LootOriginPlugin.class);
		RuneLite.main(args);
	}
}