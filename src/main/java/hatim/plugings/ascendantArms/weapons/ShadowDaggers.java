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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShadowDaggers implements MythicWeapon, UpdatableWeapon, Listener {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private final Map<UUID, Long> smokeBombCooldowns = new HashMap<>();
    private final Map<UUID, Long> shadowStepCooldowns = new HashMap<>();

    public ShadowDaggers() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getID() { return "SHADOW_DAGGERS"; }

    @Override
    public String getDisplayName() { return ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Shadow Daggers"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHERITE_SHOVEL);
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
        lore.add(ChatColor.GRAY + "Twin blades that strike from the shadows.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Venomous Strike");
        if (level >= 3) lore.add(ChatColor.AQUA + "Shift + Right-Click: Smoke Bomb");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Right-Click Poisoned Enemy: Shadow Step");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 7) return;

        LivingEntity target = getTargetEntity(player, 15);
        if (target != null && target.hasPotionEffect(PotionEffectType.POISON)) {
            long cooldown = 15000;
            if (shadowStepCooldowns.containsKey(player.getUniqueId())) {
                long timeLeft = (shadowStepCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    player.sendMessage(ChatColor.RED + "Shadow Step is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                    return;
                }
            }
            shadowStepCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            Location endLoc = target.getLocation().clone().add(target.getLocation().getDirection().normalize().multiply(-1.5));
            endLoc.setPitch(player.getLocation().getPitch());
            endLoc.setYaw(player.getLocation().getYaw());

            player.teleport(endLoc);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
            // --- CORRECTED PARTICLE FOR TELEPORT POOF ---
            player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0);
        }
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        int level = PersistentDataHandler.getMasteryLevel(item);
        if (level < 3) return;

        long cooldown = 20000;
        if (smokeBombCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (smokeBombCooldowns.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Smoke Bomb is on cooldown for " + (timeLeft / 1000 + 1) + "s.");
                return;
            }
        }
        smokeBombCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        Location loc = player.getLocation();
        // --- CORRECTED PARTICLE FOR LARGE SMOKE CLOUD ---
        loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc.add(0, 1, 0), 50, 2, 1, 2, 0.01);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);

        int duration = level >= 6 ? 100 : 60;
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) return;

        Player damager = (Player) event.getDamager();
        ItemStack weapon = damager.getInventory().getItemInMainHand();

        if (!getID().equals(PersistentDataHandler.getMythicID(weapon))) return;

        LivingEntity target = (LivingEntity) event.getEntity();
        int level = PersistentDataHandler.getMasteryLevel(weapon);

        int duration = 100;
        int amplifier = level >= 5 ? 1 : 0;

        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier));
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range,
                entity -> entity instanceof LivingEntity && !entity.equals(player));
        return (result != null && result.getHitEntity() instanceof LivingEntity) ? (LivingEntity) result.getHitEntity() : null;
    }
}