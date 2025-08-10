package hatim.plugings.ascendantArms;

import org.bukkit.inventory.ItemStack;

/**
 * An interface for Mythic Weapons that have lore that needs to be dynamically updated,
 * for example, when gaining XP or leveling up. This allows the WeaponManager to handle
 * lore updates for any weapon without knowing its specific type.
 */
public interface UpdatableWeapon {

    /**
     * Updates the lore of the given ItemStack to reflect its current data (level, XP, etc.).
     * @param item The item whose lore should be updated.
     */
    void updateLore(ItemStack item);
}