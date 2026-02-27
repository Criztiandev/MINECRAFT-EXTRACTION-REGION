# Extraction Region Editor - In-Game Testing Walkthrough

Now that the plugin is installed on your server, here is the ultimate testing checklist to verify every single piece of functionality works natively in your multiplayer environment.

---

## 🏗️ Phase 1: Test the Entry Portals (Warzone Drops)

**Goal:** Ensure players are dropped securely into the warzone when they touch the spawn portal.

1. **Setup:** Create your Entry Region at spawn and bind its drop-zone to the warzone. (See `IMPLEMENTATION_GUIDE.md`).
2. **The Walk In:** Walk your character into the physical spawn portal border.
3. **Verify Teleportation:** You should instantly vanish from spawn.
4. **Verify Coordinates:** Press `F3` and verify your current X/Y/Z coordinates are strictly confined between the Drop Zone boundary corners you set.
5. **Verify Potion Effects:** Open your inventory and check that you have **Slow Falling** and **Blindness** applied for the exact durations you configured in the Entry Settings GUI.
6. **Concurrency Test (Optional):** Ask 2 or 3 other players to walk into the portal at the exact same time as you. Verify all of you are scattered randomly and do not clip into each other.

---

## 🧰 Phase 2: Test the Chest Regions (Timer Restock)

**Goal:** Verify that chests accurately restock on the PHT 6-hour interval system or when forced.

1. **Setup:** Place down several Extract Chests (`extraction-chest` plugin) in an area, loot them so they are completely empty, and capture that area into a Chest Region using `/lr`.
2. **Verify Detection:** Open `/lr`, click your Chest Region, and click the **Overview** paper. Verify it mathematically counted exactly how many chests you placed inside that boundary.
3. **The 'Force Replenish' Test:**
   * Inside the Region Action Menu, click the **Force Replenish Now** button.
   * Walk back up to the chests you just looted.
   * **Result:** All chests inside the boundary should have reverted to their `READY` state and generated a brand new set of randomized loot!
4. **The Boundary Test:** Loot a chest *outside* of the wand boundary. Click Force Replenish again. Ensure the chest outside is completely ignored.
5. **The Interval Test:** Set the timer in the GUI. The chests will now automatically run this exact same Replenish task at exactly 00:00, 06:00, 12:00, and 18:00 server time.

---

## ⚔️ Phase 3: Test the Extraction Regions (The Great Escape)

**Goal:** Verify players can extract safely, but only if they survive the grueling extraction rules.

1. **Setup:** Create an Extraction region and punch a center block to set it as the Conduit.
2. **The Line-of-Sight Test:**
   * Walk inside the region and stare exactly at the Conduit block. 
   * You will hear a heartbeat and hear the alarm broadcast to the server.
   * Quickly **look away** or step backward. 
   * **Result:** You hear glass shatter, and the extraction timer is instantly canceled.
3. **The PVP Interruption Test:**
   * Stare at the conduit and wait 3 or 4 seconds.
   * Have a friend punch you or shoot you with an arrow.
   * **Result:** The damage should register, glass should shatter, and the extraction is instantly violently canceled.
4. **The Block Griefing Test:**
   * Hand a friend a pickaxe and some dirt blocks. Have them try to mine the floor or build a wall inside the Extraction zone.
   * **Result:** They receive a red warning message and the blocks immediately snap back into place.
5. **The Successful Escape:**
   * Defend the room. Stare at the conduit for exactly 5 seconds without moving or taking damage.
   * **Result:** You consume 1 capacity from the room, hear an Enderman teleport sound, and are safely dropped back exactly at your server's Spawn point!

---

If all 3 phases perform exactly as written above, your Extraction Region system is 100% stable and ready for production!
