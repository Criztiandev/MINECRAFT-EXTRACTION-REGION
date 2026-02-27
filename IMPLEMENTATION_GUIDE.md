# Extraction Region Editor - Quick Setup Guide

This plugin completely overhauls your server's flow into three distinct zones: **Entry Regions** (Warzone Portals), **Chest Regions** (Loot Timers), and **Extraction Regions** (The Escape points).

Here is the exact step-by-step workflow to implement this flawlessly on your live server!

---

## Part 1: Setting up an Entry Portal (Spawn to Warzone)

**Goal:** Create a portal zone at your server Spawn that safely random-drops players into a configured Warzone.

1.  **Select the Portal Bounds:** Give yourself a wand (`/lr wand`) and left/right click the physical portal blocks at your Spawn where players will walk into.
2.  **Create the Region:** Run `/lr create spawn_portal entry_region`.
3.  **Wand the Drop Zone:** Now fly to your actual Warzone map. Use your Wand to select a massive boundary area (e.g., from one corner of a ruined city to another). *Do NOT run the create command again.*
4.  **Assign the Drop Zone:**
    *   Type `/lr` and click **Entry Regions**.
    *   Click on your `spawn_portal` region.
    *   Click the **Set Drop Zone** icon. It will instantly read your wand selection and map the coordinates mathematically.
5.  **Configure Drop Effects:** Click **Entry Settings** in the GUI and assign "Slow Falling" (10s) and "Blindness" (3s) to make the drop disorienting and immersive.

---

## Part 2: Setting up Chest Regions (Bulk Loot Resupplies)

**Goal:** Scatter chests across a map and synchronize them all to replenish exactly at predictable real-world times.

1.  **Physically Place Chests:** Go into your Warzone map and manually place down your `extraction-chest` models wherever you want them (e.g., behind boxes, on roofs, etc.) using your existing spawn egg tools.
2.  **Select the Zone:** Use your Wand (`/lr wand`) to select the entire geographic sector enclosing those physical chests.
3.  **Create the Region:** Run `/lr create sector_alpha chest_replenish`.
4.  **Configure the CRON Timer:** 
    *   Type `/lr` and click **Chest Regions**.
    *   Click on your `sector_alpha` region.
    *   Click the **Region Overview** paper. You should instantly see a dynamically counted list of all Chests captured inside that zone!
    *   Click the **Timer Config** and set it to `360` (6 hours). 
    *   *Result:* The plugin will automatically fire an asynchronous refresh sequence at exactly 00:00, 06:00, 12:00, and 18:00 (PHT) to roll new loot and revert all captured chests back to READY.

---

## Part 3: Setting up Extraction Regions (The Great Escape)

**Goal:** Create high-stakes exit portals heavily guarded by cooldowns and PVP interruptions.

1.  **Build the Exit Room:** Build a designated room/platform on the edge of your Warzone. Place a unique block exactly in the center (this is your Conduit).
2.  **Select the Area:** Use your Wand (`/lr wand`) to select the dimensions of the entire room.
3.  **Create the Region:** Run `/lr create north_exit extraction`.
4.  **Set the Conduit Block:**
    *   Type `/lr` and click **Extraction Regions** -> Click `north_exit`.
    *   Click **Set Conduit Block**, close the menu, and punch the unique block in the center of the room.
5.  **Configure Extraction Settings:**
    *   Open the Action menu and click **Extraction Settings**.
    *   Set the **Cooldown Timer** (e.g., 60 minutes) and the **Max Capacity** (e.g., RNG between 1 and 4 players).
    *   Toggle the **Mimic Trap** to `true` (5% chance of explosion instead of extracting).
6.  **Test the PvP Mechanics!**
    *   Step into the room and stare at the Conduit block to start the 5-second countdown.
    *   The server will broadcast the alarm: *"⚠️ An extraction has been initiated!"*.
    *   If you take damage or look away, the extraction is violently blocked and canceled.

**Congratulations! You have now completed a full end-to-end loot extraction loop.**
