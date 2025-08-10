package hatim.plugings.ascendantArms;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the entire Mastery system, including XP gain and leveling up (Ascension).
 * This class listens for mob kills to award XP to held Mythic Weapons.
 */
public class MasteryManager implements Listener {

    private final AscendantArms plugin;
    private final WeaponManager weaponManager;
    // A map to define how much XP each type of mob is worth.
    private final Map<EntityType, Integer> xpValues = new HashMap<>();

    public MasteryManager(AscendantArms plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        loadXpValues();
    }

    private void loadXpValues() {
        // Define base XP values for different mob types.
        xpValues.put(EntityType.ZOMBIE, 10);
        xpValues.put(EntityType.SKELETON, 10);
        xpValues.put(EntityType.SPIDER, 8);
        xpValues.put(EntityType.CREEPER, 15);
        xpValues.put(EntityType.ENDERMAN, 25);
        xpValues.put(EntityType.PIGLIN, 12);
        xpValues.put(EntityType.PIGLIN_BRUTE, 50);
        xpValues.put(EntityType.BLAZE, 20);
        xpValues.put(EntityType.WITHER, 1000);
        xpValues.put(EntityType.ENDER_DRAGON, 2000);
        xpValues.put(EntityType.PLAYER, 100);
    }

    /**
     * Calculates the XP required to reach the next level.
     * @param currentLevel The current mastery level of the weapon.
     * @return The total XP needed for the current level tier.
     */
    public static int getXpForNextLevel(int currentLevel) {
        if (currentLevel >= 7) return 0; // Max level
        // A scaling formula: 100, 250, 450, 700, 1000, 1350
        return 50 * currentLevel * currentLevel + 50 * currentLevel;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killed = event.getEntity();
        Player killer = killed.getKiller(); // getKiller() returns the player who dealt the final blow.

        // Check if the killer is a player and is holding a Mythic Weapon.
        if (killer == null) return;
        ItemStack itemInHand = killer.getInventory().getItemInMainHand();
        if (!PersistentDataHandler.isMythic(itemInHand)) return;

        int level = PersistentDataHandler.getMasteryLevel(itemInHand);
        // Don't grant XP if the weapon is max level.
        if (level >= 7) return;

        // Get the XP value for the killed mob, defaulting to 5 if not specified.
        int xpGained = xpValues.getOrDefault(killed.getType(), 5);
        int currentXp = PersistentDataHandler.getMasteryXp(itemInHand);
        int newXp = currentXp + xpGained;

        // --- ASCENSION CHECK ---
        int xpToNextLevel = getXpForNextLevel(level);
        if (newXp >= xpToNextLevel) {
            // The weapon is ready to ascend! Cap the XP and notify the player.
            newXp = xpToNextLevel;
            killer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Your " + itemInHand.getItemMeta().getDisplayName() + ChatColor.GOLD + "" + ChatColor.BOLD + " is ready to Ascend! Use /mythic ascend.");
            killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        killer.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "+ " + xpGained + " Mastery XP");

        // Set the new XP value on the item's data.
        PersistentDataHandler.setData(itemInHand, PersistentDataHandler.MASTERY_XP_KEY, PersistentDataType.INTEGER, newXp);

        // Ask the WeaponManager to dynamically update the item's lore.
        weaponManager.updateWeaponLore(itemInHand);
    }
}