package hatim.plugings.ascendantArms.weapons;

import hatim.plugings.ascendantArms.AscendantArms;
import hatim.plugings.ascendantArms.MythicWeapon;
import hatim.plugings.ascendantArms.PersistentDataHandler;
import hatim.plugings.ascendantArms.UpdatableWeapon;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle; // Make sure this is imported
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TidalTrident implements MythicWeapon, UpdatableWeapon {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private final Map<UUID, Long> geyserCooldowns = new HashMap<>();

    @Override
    public String getID() { return "TIDAL_TRIDENT"; }

    @Override
    public String getDisplayName() { return ChatColor.AQUA + "" + ChatColor.BOLD + "Tidal Trident"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getDisplayName());
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        PersistentDataHandler.setData(item, PersistentDataHandler.MYTHIC_ID_KEY, PersistentDataType.STRING, getID());
        PersistentDataHandler.setData(item, PersistentDataHandler.MASTERY_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        PersistentDataHandler.setData(item, PersistentDataHandler.MASTERY_XP_KEY, PersistentDataType.INTEGER, 0);

        updateLore(item);
        return item;
    }

    @Override
    public void updateLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int level = PersistentDataHandler.getMasteryLevel(item);
        int xp = PersistentDataHandler.getMasteryXp(item);
        int xpToNextLevel = hatim.plugings.ascendantArms.MasteryManager.getXpForNextLevel(level);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A trident that commands the might of the ocean.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Ocean's Grace");
        if (level >= 3) lore.add(ChatColor.AQUA + "Right-Click Block: Geyser");
        if (level >= 5) lore.add(ChatColor.AQUA + "Passive: Healing Mist");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Maelstrom");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (event.getClickedBlock() == null) {
            return;
        }

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) return;

        long cooldown = 12000;
        if (geyserCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (geyserCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Geyser is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }

        event.setCancelled(true);
        geyserCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        Block targetBlock = event.getClickedBlock();
        Location geyserLoc = targetBlock.getLocation().add(0.5, 1, 0.5);

        geyserLoc.getWorld().playSound(geyserLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 20;
            final boolean isMaelstrom = level >= 7;
            final double launchPower = isMaelstrom ? 1.5 : 1.2;

            @Override
            public void run() {
                if (ticks > duration) this.cancel();

                // --- THIS IS THE CORRECTED LINE ---
                geyserLoc.getWorld().spawnParticle(Particle.SPLASH, geyserLoc, 100, 1, 1, 1, 0.1);
                geyserLoc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, geyserLoc, 20, 0.5, 1, 0.5, 0.1);

                for (Entity entity : geyserLoc.getWorld().getNearbyEntities(geyserLoc, 1.5, 2, 1.5)) {
                    if (entity instanceof LivingEntity) {
                        entity.setVelocity(new Vector(0, launchPower, 0));
                    }
                }

                if (isMaelstrom) {
                    for (Entity entity : geyserLoc.getWorld().getNearbyEntities(geyserLoc, 6, 4, 6)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            Vector pullVector = geyserLoc.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.4);
                            entity.setVelocity(entity.getVelocity().add(pullVector));
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        // No Shift + Right-Click ability for this weapon.
    }
}