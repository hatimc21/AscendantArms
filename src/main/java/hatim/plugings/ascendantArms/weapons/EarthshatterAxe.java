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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EarthshatterAxe implements MythicWeapon, UpdatableWeapon {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private final Map<UUID, Long> slamCooldowns = new HashMap<>();
    private final Map<UUID, Long> armorCooldowns = new HashMap<>();

    @Override
    public String getID() { return "EARTHSHATTER_AXE"; }

    @Override
    public String getDisplayName() { return ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Earthshatter Axe"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
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
        lore.add(ChatColor.GRAY + "An axe that wields seismic, unstoppable force.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Unyielding");
        if (level >= 3) lore.add(ChatColor.AQUA + "Right-Click: Seismic Slam");
        if (level >= 5) lore.add(ChatColor.AQUA + "Shift + Right-Click: Earthen Armor");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Fissure");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) return;

        long cooldown = 10000;
        if (slamCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (slamCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Seismic Slam is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }
        slamCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);

        double radius = 4.0;
        double damage = 6.0;
        boolean createFissure = level >= 7;

        for (Entity entity : player.getNearbyEntities(radius, 2, radius)) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                Vector direction = livingEntity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.setY(0.5);
                livingEntity.setVelocity(direction.multiply(1.5));
                livingEntity.damage(damage, player);
            }
        }

        // --- CORRECTED PARTICLE ---
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().subtract(0, 0.2, 0), 100, radius / 2, 0.1, radius / 2, Material.DIRT.createBlockData());

        if (createFissure) {
            Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
            createFissureLine(player.getLocation(), playerDirection);
        }
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 5) return;

        long cooldown = 25000;
        if (armorCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (armorCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Earthen Armor is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }
        armorCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        int absorptionHearts = 4;
        if (level >= 6) absorptionHearts = 8;

        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, absorptionHearts / 4 - 1));
        player.sendMessage(ChatColor.GREEN + "You summon a shield of hardened earth!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);
        // --- CORRECTED PARTICLE ---
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
    }

    private void createFissureLine(Location start, Vector direction) {
        new BukkitRunnable() {
            int distance = 0;
            @Override
            public void run() {
                if (distance > 8) this.cancel();
                Location currentLoc = start.clone().add(direction.clone().multiply(distance));
                // --- CORRECTED PARTICLE ---
                currentLoc.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 20, 0.2, 0.1, 0.2, Material.DIRT.createBlockData());
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(2.0);
                    }
                }
                distance++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}