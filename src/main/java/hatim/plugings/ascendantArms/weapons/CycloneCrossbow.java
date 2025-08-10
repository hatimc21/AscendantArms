package hatim.plugings.ascendantArms.weapons;

import hatim.plugings.ascendantArms.AscendantArms;
import hatim.plugings.ascendantArms.MythicWeapon;
import hatim.plugings.ascendantArms.UpdatableWeapon;
import hatim.plugings.ascendantArms.PersistentDataHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CycloneCrossbow implements MythicWeapon, UpdatableWeapon, Listener {

    private final AscendantArms plugin = JavaPlugin.getPlugin(AscendantArms.class);
    private static final String GALE_BOLT_METADATA = "GaleBolt";

    @Override
    public String getID() { return "CYCLONE_CROSSBOW"; }

    @Override
    public String getDisplayName() { return ChatColor.WHITE + "" + ChatColor.BOLD + "Cyclone Crossbow"; }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
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
        lore.add(ChatColor.GRAY + "A crossbow that fires bolts of pure wind.");
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Mastery: " + level + " / 7");
        if (level < 7) lore.add(ChatColor.DARK_AQUA + "XP: " + xp + " / " + xpToNextLevel);
        else lore.add(ChatColor.GOLD + "XP: MAX");
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive: Gale Bolt");
        if (level >= 3) lore.add(ChatColor.AQUA + "Passive: Tailwind");
        if (level >= 7) lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE: Piercing Gale");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public void handleRightClick(PlayerInteractEvent event) {
        // Default crossbow behavior handles this.
    }

    @Override
    public void handleShiftRightClick(PlayerInteractEvent event) {
        // No Shift + Right-Click ability.
    }

    // --- EVENT LISTENERS FOR PASSIVE ABILITIES ---

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof AbstractArrow)) {
            return;
        }
        Player shooter = (Player) event.getEntity();
        ItemStack crossbow = event.getBow();

        // Check if the bow used is our Cyclone Crossbow
        if (crossbow == null || !getID().equals(PersistentDataHandler.getMythicID(crossbow))) {
            return;
        }

        AbstractArrow arrow = (AbstractArrow) event.getProjectile();
        int level = PersistentDataHandler.getMasteryLevel(crossbow);

        // Level 1: Gale Bolt (Faster Projectile)
        Vector velocity = arrow.getVelocity();
        arrow.setVelocity(velocity.multiply(1.5)); // 50% faster
        arrow.setMetadata(GALE_BOLT_METADATA, new FixedMetadataValue(plugin, level));
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);

        // Level 3: Tailwind (Speed Boost)
        if (level >= 3) {
            int duration = level >= 6 ? 60 : 40; // 3s if level 6+, 2s otherwise
            shooter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0)); // Speed I
        }

        // Level 7: Piercing Gale
        if (level >= 7) {
            arrow.setPierceLevel(1); // Allows the arrow to pass through one enemy.
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        // This event lets us modify the damage dealt by our special arrow.
        if (!(event.getDamager() instanceof AbstractArrow) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        AbstractArrow arrow = (AbstractArrow) event.getDamager();
        if (!arrow.hasMetadata(GALE_BOLT_METADATA)) {
            return;
        }

        int level = arrow.getMetadata(GALE_BOLT_METADATA).get(0).asInt();

        // Level 5: Armor Penetration
        if (level >= 5) {
            // Add a small amount of extra damage that bypasses armor calculations.
            // This is a simplified way to represent armor penetration.
            double bonusDamage = 2.0; // 1 heart of bonus damage
            LivingEntity target = (LivingEntity) event.getEntity();
            target.setHealth(Math.max(0, target.getHealth() - bonusDamage));
            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 15);
        }
    }
}