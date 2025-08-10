package hatim.plugings.ascendantArms;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A utility class to handle all interactions with Persistent Data Containers (PDC).
 * This allows us to attach custom data (like level, XP, etc.) directly to an ItemStack,
 * which is saved even after server restarts.
 */
public class PersistentDataHandler {

    private static final JavaPlugin plugin = JavaPlugin.getPlugin(AscendantArms.class);

    // --- KEYS ---
    // We define our unique keys here. This prevents typos in our code.
    public static final NamespacedKey MYTHIC_ID_KEY = new NamespacedKey(plugin, "mythic_id");
    public static final NamespacedKey MASTERY_LEVEL_KEY = new NamespacedKey(plugin, "mastery_level");
    public static final NamespacedKey MASTERY_XP_KEY = new NamespacedKey(plugin, "mastery_xp");

    // --- GENERIC DATA METHODS ---
    // These are powerful helper methods that reduce code duplication.

    public static <T, Z> void setData(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, type, value);
        item.setItemMeta(meta);
    }

    public static <T, Z> Z getData(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type) {
        if (item == null || item.getItemMeta() == null) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(key, type, null);
    }

    // --- MYTHIC-SPECIFIC HELPER METHODS ---
    // These methods make our main code much cleaner and more readable.

    /**
     * Checks if a given item is a Mythic Weapon by looking for the ID tag.
     * @param item The item to check.
     * @return true if the item is a Mythic Weapon.
     */
    public static boolean isMythic(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(MYTHIC_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Gets the unique ID of a Mythic Weapon from its data container.
     * THIS IS THE METHOD THAT FIXES THE ERROR.
     * @param item The item to check.
     * @return The string ID (e.g., "RIFT_BLADE"), or null if it's not a Mythic Weapon.
     */
    public static String getMythicID(ItemStack item) {
        return getData(item, MYTHIC_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Gets the Mastery Level of a Mythic Weapon.
     * @param item The item to check.
     * @return The mastery level, or 0 if it's not set.
     */
    public static int getMasteryLevel(ItemStack item) {
        Integer level = getData(item, MASTERY_LEVEL_KEY, PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    /**
     * Gets the Mastery XP of a Mythic Weapon.
     * @param item The item to check.
     * @return The mastery XP, or 0 if it's not set.
     */
    public static int getMasteryXp(ItemStack item) {
        Integer xp = getData(item, MASTERY_XP_KEY, PersistentDataType.INTEGER);
        return xp != null ? xp : 0;
    }
}