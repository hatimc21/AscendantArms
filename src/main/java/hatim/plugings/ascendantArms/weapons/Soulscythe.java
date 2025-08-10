package hatim.plugings.ascendantArms.weapons;

import hatim.plugings.ascendantArms.AscendantArms;
import hatim.plugings.ascendantArms.MythicWeapon;
import hatim.plugings.ascendantArms.UpdatableWeapon;
import hatim.plugings.ascendantArms.PersistentDataHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Soulscythe implements MythicWeapon, UpdatableWeapon, Listener {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private final Map<UUID, Long> reapCooldowns = new HashMap<>();

    public Soulscythe() {
        // This weapon needs to be a listener to catch mob deaths for Soul Rip.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getID() { return "SOULSCYTHE"; }

    @Override
    public String getDisplayName() { return ChatColor.DARK_RED + "" + ChatColor.BOLD + "Soulscythe"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHERITE_HOE);
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
        lore.add(ChatColor.GRAY + "A scythe that reaps the very essence of its foes.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Soul Rip");
        if (level >= 3) lore.add(ChatColor.AQUA + "Shift + Right-Click: Reap");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Soul Rend");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        // No standard Right-Click ability.
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) return;

        long cooldown = 8000; // 8 seconds
        if (reapCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (reapCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Reap is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }
        reapCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1);

        // --- Reap Logic ---
        double damage = 5.0; // 2.5 hearts
        for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player);

                int duration = level >= 4 ? 80 : 60; // 4s if level 4+, 3s otherwise
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, 0)); // Wither I
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killed = event.getEntity();
        Player killer = killed.getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!getID().equals(PersistentDataHandler.getMythicID(weapon))) return;

        // --- Passive: Soul Rip ---
        int level = PersistentDataHandler.getMasteryLevel(weapon);
        double chance = 0.25; // 25% base chance
        if (level >= 6) chance = 1.0; // 100% chance at level 6

        if (Math.random() < chance) {
            spawnSoulFragment(killer, killed.getLocation().add(0, 0.5, 0), level);
        }
    }

    private void spawnSoulFragment(Player owner, Location location, int level) {
        // We spawn a nearly-invisible, marker Armor Stand to represent the soul fragment.
        ArmorStand soul = location.getWorld().spawn(location, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(true);
        });

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 100 || soul.isDead() || owner.isDead() || !owner.isOnline()) {
                    soul.remove();
                    this.cancel();
                    return;
                }

                // Visual effect for the soul
                soul.getWorld().spawnParticle(Particle.SOUL, soul.getLocation(), 1, 0, 0, 0, 0);

                // Homing effect
                if (owner.getLocation().distanceSquared(soul.getLocation()) < 2.25) { // If player is within 1.5 blocks
                    double healAmount = level >= 5 ? 4.0 : 2.0; // 2 hearts if level 5+, 1 heart otherwise
                    owner.setHealth(Math.min(owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), owner.getHealth() + healAmount));

                    owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
                    soul.remove();
                    this.cancel();
                } else {
                    Vector direction = owner.getEyeLocation().toVector().subtract(soul.getLocation().toVector()).normalize();
                    soul.teleport(soul.getLocation().add(direction.multiply(0.3)));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}