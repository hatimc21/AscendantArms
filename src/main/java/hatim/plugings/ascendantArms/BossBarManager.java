package hatim.plugings.ascendantArms;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the creation, display, and removal of Mastery Boss Bars for players
 * holding Mythic Weapons.
 */
public class BossBarManager implements Listener {

    private final AscendantArms plugin;
    private final WeaponManager weaponManager;
    // We store a BossBar for each online player. This allows us to hide or show it as needed.
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    public BossBarManager(AscendantArms plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        // Start a repeating task to update the lore and BossBar for held items.
        startHeldItemUpdater();
    }

    // --- CORE EVENT HANDLERS ---

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (PersistentDataHandler.isMythic(newItem)) {
            showBossBar(player);
            updateBossBar(player, newItem);
        } else {
            hideBossBar(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        hideBossBar(player);
        playerBossBars.remove(player.getUniqueId());
    }

    // --- BOSS BAR MANAGEMENT METHODS ---

    private void showBossBar(Player player) {
        BossBar bossBar = playerBossBars.computeIfAbsent(player.getUniqueId(), k ->
                Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID)
        );
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
    }

    /**
     * Updates a player's Boss Bar with the stats from a given Mythic Weapon.
     */
    public void updateBossBar(Player player, ItemStack mythicWeapon) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar == null || !PersistentDataHandler.isMythic(mythicWeapon)) {
            hideBossBar(player);
            return;
        }

        if (!bossBar.isVisible()) {
            showBossBar(player);
        }

        String weaponID = PersistentDataHandler.getMythicID(mythicWeapon);
        int level = PersistentDataHandler.getMasteryLevel(mythicWeapon);
        int xp = PersistentDataHandler.getMasteryXp(mythicWeapon);
        int xpToNextLevel = MasteryManager.getXpForNextLevel(level);

        MythicWeapon weapon = weaponManager.getWeapon(weaponID);
        if (weapon == null) return;

        bossBar.setColor(getBarColorForWeapon(weaponID));

        if (level >= 7) {
            bossBar.setTitle(weapon.getDisplayName() + " §8- §6§lMax Mastery!");
            bossBar.setProgress(1.0);
        } else if (xp >= xpToNextLevel) {
            bossBar.setTitle(weapon.getDisplayName() + " §8- §6§lReady to Ascend!");
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setProgress(1.0);
        } else {
            bossBar.setTitle(weapon.getDisplayName() + " §8- §bMastery " + level + "/7");
            bossBar.setProgress((double) xp / xpToNextLevel);
        }
    }

    /**
     * A repeating task that checks the player's held item every second.
     * This ensures the BossBar and Lore are always up-to-date, even if no events fire.
     */
    private void startHeldItemUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (PersistentDataHandler.isMythic(item)) {
                        updateBossBar(player, item);
                        weaponManager.updateWeaponLore(item);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L); // Start after 5s, repeat every 1s (20 ticks)
    }

    private BarColor getBarColorForWeapon(String weaponID) {
        switch (weaponID) {
            case "RIFT_BLADE": return BarColor.PURPLE;
            case "SUNFIRE_BOW": return BarColor.RED;
            case "TIDAL_TRIDENT": return BarColor.BLUE;
            case "EARTHSHATTER_AXE": return BarColor.GREEN;
            case "CYCLONE_CROSSBOW": return BarColor.WHITE;
            case "SHADOW_DAGGERS": case "SOULSCYTHE": return BarColor.PINK;
            default: return BarColor.PURPLE;
        }
    }
}