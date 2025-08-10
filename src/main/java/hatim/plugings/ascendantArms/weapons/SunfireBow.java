package hatim.plugings.ascendantArms.weapons;

import hatim.plugings.ascendantArms.AscendantArms;
import hatim.plugings.ascendantArms.MythicWeapon;
import hatim.plugings.ascendantArms.PersistentDataHandler;
import hatim.plugings.ascendantArms.UpdatableWeapon;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SunfireBow implements MythicWeapon, UpdatableWeapon, Listener {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private final Map<UUID, Long> explosiveShotCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isCharged = new HashMap<>();

    public SunfireBow() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getID() { return "SUNFIRE_BOW"; }

    @Override
    public String getDisplayName() { return ChatColor.GOLD + "" + ChatColor.BOLD + "Sunfire Bow"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(getDisplayName());
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
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
        lore.add(ChatColor.GRAY + "A bow imbued with the sun's explosive fury.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Flare Arrow");
        if (level >= 3) lore.add(ChatColor.AQUA + "Shift + Right-Click: Charge Explosive Shot");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Supernova");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        // This method is now only for NON-sneaking actions.
        // Default bow drawing is handled, so this can be empty.
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        // --- THIS IS THE NEW LOGIC FOR CHARGING THE SHOT ---
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) return;

        // Prevent the player from drawing their bow when they just want to charge the shot.
        event.setCancelled(true);

        long cooldown = 10000;
        if (level >= 5) cooldown = 8000;

        if (explosiveShotCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (explosiveShotCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Explosive Shot is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }

        // Check if already charged
        if (isCharged.getOrDefault(player.getUniqueId(), false)) {
            player.sendMessage(ChatColor.YELLOW + "Your next shot is already explosive!");
            return;
        }

        isCharged.put(player.getUniqueId(), true);
        player.sendMessage(ChatColor.GOLD + "Explosive Shot charged! Your next fully drawn arrow will be explosive.");
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.5f);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player shooter = (Player) event.getEntity();
        ItemStack bow = event.getBow();
        if (bow == null || !getID().equals(PersistentDataHandler.getMythicID(bow))) return;

        Arrow arrow = (Arrow) event.getProjectile();
        // Passive: Flare Arrow always applies.
        arrow.setFireTicks(100);

        // Check if the player has a charged shot ready and the bow was fully drawn.
        if (isCharged.getOrDefault(shooter.getUniqueId(), false) && event.getForce() >= 1.0) {
            arrow.setMetadata("EXPLOSIVE_ARROW", new FixedMetadataValue(plugin, true));
            shooter.getWorld().spawnParticle(Particle.LAVA, shooter.getLocation().add(0, 1, 0), 10);

            // Consume the charge and set the cooldown.
            isCharged.remove(shooter.getUniqueId());
            explosiveShotCooldowns.put(shooter.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        if (!arrow.hasMetadata("EXPLOSIVE_ARROW")) return;

        if (!(arrow.getShooter() instanceof Player)) return;
        Player shooter = (Player) arrow.getShooter();
        ItemStack bow = shooter.getInventory().getItemInMainHand();

        Location impactLoc = arrow.getLocation();
        int level = PersistentDataHandler.getMasteryLevel(bow);

        float radius = 3.0f;
        if (level >= 7) radius = 5.0f;

        impactLoc.getWorld().createExplosion(impactLoc, radius, false, false, shooter);
        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1);

        if (level >= 7) {
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks > 60) this.cancel();
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 50, 2.5, 0.5, 2.5, 0.01);
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, 2.5, 1, 2.5)) {
                        if (entity instanceof LivingEntity && !entity.equals(shooter)) {
                            entity.setFireTicks(40);
                        }
                    }
                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }

        arrow.remove();
    }
}