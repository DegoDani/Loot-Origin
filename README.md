# Loot Origin
Instantly see exactly which monsters and events dropped the items currently sitting in your bank or inventory.

Instead of scrolling through endless lists in the default loot tracker, Loot Origin allows you to select a specific item and instantly view a ranked list of every source that has ever dropped it for your account.

## How to Use
1. Hold `Shift` and press `LMB` on an item 

2. Click `Check origin` on any item in your inventory, bank, or equipment interface.

The Loot Origin side panel will pop open, displaying the total amount tracked and a highest-to-lowest list of every monster/event that dropped it.

## Setup: The Data Vault
To ensure 100% accuracy without causing game lag, this plugin uses a local "Vault" system. Because it uses your official RuneLite data, you need to import your baseline history once:

1. Log into [runelite.net](https://runelite.net/account/loot-tracker) and go to your **Loot Tracker** page. This can be done through RuneLite via the green arrow symbol in the top right.
2. Make sure to select the correct character in the side panel.
3. Click the **Export** button at the bottom of the side panel to download your lifetime loot history as a `.json` file.

Open the Loot Origin side panel in-game and click "**Import Official Web Data**". Select your downloaded file.

That's it! You only need to do this once. Moving forward, the plugin has a "Live Feed" that will automatically intercept your NPC kills and permanently save new drops to your Vault behind the scenes.