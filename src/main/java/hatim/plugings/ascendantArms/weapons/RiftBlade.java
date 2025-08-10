package hatim.plugings.ascendantArms.weapons;

import hatim.plugings.ascendantArms.AscendantArms;
import hatim.plugings.ascendantArms.MythicWeapon;
import hatim.plugings.ascendantArms.PersistentDataHandler;
import hatim.plugings.ascendantArms.UpdatableWeapon;
import org.bukkit.*;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RiftBlade implements MythicWeapon, UpdatableWeapon {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    // Cooldown map specific to this weapon's abilities.
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getID() { return "RIFT_BLADE"; }

    @Override
    public String getDisplayName() { return ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Rift Blade"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
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
        lore.add(ChatColor.GRAY + "A blade that cuts through space itself.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) {
            lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        } else {
            lore.add(ChatColor.GOLD + "XP: MAX");
        }
        lore.add("");
        lore.add(ChatColor.AQUA + "Right-Click: Blink Strike");
        if (level >= 3) lore.add(ChatColor.AQUA + "Shift + Right-Click: Rift Pull");
        if (level >= 6) lore.add(ChatColor.GREEN + "Passive: Grants Speed after Blink Strike.");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Rift Phase");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (isOnCooldown(player, PersistentDataHandler.getMasteryLevel(item))) return;

        LivingEntity target = getTargetEntity(player, 15);
        if (target == null) {
            player.sendMessage(ChatColor.GRAY + "No target in sight.");
            return;
        }

        Location startLoc = player.getLocation();
        Vector behindVector = target.getLocation().getDirection().normalize().multiply(-2);
        Location endLoc = target.getLocation().clone().add(behindVector);
        if (!isSafeLocation(endLoc)) endLoc = target.getLocation();

        endLoc.setPitch(startLoc.getPitch());
        endLoc.setYaw(startLoc.getYaw());

        player.teleport(endLoc);
        target.damage(8.0, player);

        player.getWorld().playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.getWorld().playSound(endLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.PORTAL, target.getEyeLocation(), 50, 0.5, 0.5, 0.5, 0.2);

        // --- Mastery Unlocks ---
        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level >= 6) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1)); // Speed II for 2s
        }
        if (level >= 7) { // Ultimate: Rift Phase
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0)); // Invisibility for 2s
            player.getWorld().playSound(endLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
        }

        setCooldown(player);
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) {
            player.sendMessage(ChatColor.RED + "This ability unlocks at Mastery Level 3.");
            return;
        }

        if (isOnCooldown(player, level)) return;

        RayTraceResult rayTrace = player.rayTraceBlocks(30);
        if (rayTrace == null || rayTrace.getHitBlock() == null) {
            player.sendMessage(ChatColor.GRAY + "You must target a solid block.");
            return;
        }

        Location wellLocation = rayTrace.getHitBlock().getLocation().add(0.5, 1, 0.5);
        player.getWorld().playSound(wellLocation, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 60; // 3 seconds
            // --- Mastery Unlocks ---
            final double pullRadius = (level >= 5) ? 12.0 : 10.0; // Larger pull at level 5

            @Override
            public void run() {
                if (ticks > duration) this.cancel();
                wellLocation.getWorld().spawnParticle(Particle.REVERSE_PORTAL, wellLocation, 10, 0.5, 0.5, 0.5, 0.0);
                for (Entity entity : wellLocation.getWorld().getNearbyEntities(wellLocation, pullRadius, pullRadius, pullRadius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        Vector pullVector = wellLocation.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.5);
                        entity.setVelocity(entity.getVelocity().add(pullVector));
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        setCooldown(player);
    }

    // --- HELPER METHODS ---

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isOnCooldown(Player player, int level) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;

        // Cooldown scales with level. Base 8s, -0.5s per level.
        long timeSinceLastUse = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
        double baseCooldown = 8000; // 8 seconds in milliseconds
        double reduction = (level - 1) * 500; // -0.5s per level above 1
        long cooldown = (long) (baseCooldown - reduction);

        if (timeSinceLastUse < cooldown) {
            long timeLeft = (cooldown - timeSinceLastUse) / 1000;
            player.sendMessage(ChatColor.RED + "The Rift is unstable. Wait " + (timeLeft + 1) + "s.");
            return true;
        }
        return false;
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range,
                entity -> entity instanceof LivingEntity && !entity.equals(player));
        return (result != null && result.getHitEntity() instanceof LivingEntity) ? (LivingEntity) result.getHitEntity() : null;
    }

    private boolean isSafeLocation(Location loc) {
        return loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable();
    }
}