package hatim.plugings.ascendantArms;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A blueprint for all Mythic Weapons. Every weapon class we create
 * MUST implement this interface, which guarantees it will have these methods.
 * This is the foundation of our organized, scalable weapon system.
 */
public interface MythicWeapon {

    /**
     * @return The unique, internal ID for this weapon (e.g., "RIFT_BLADE"). Must be uppercase.
     */
    String getID();

    /**
     * @return The formatted, colored display name of the weapon (e.g., "§5§lRift Blade").
     */
    String getDisplayName();

    /**
     * @return A method that creates a fresh, level 1 version of this weapon's ItemStack.
     */
    ItemStack createItemStack();

    /**
     * This method will contain the logic for the weapon's primary ability (Right-Click).
     * @param event The PlayerInteractEvent that triggered this action.
     */
    void handleRightClick(PlayerInteractEvent event);

    /**
     * This method will contain the logic for the weapon's secondary ability (Shift + Right-Click).
     * @param event The PlayerInteractEvent that triggered this action.
     */
    void handleShiftRightClick(PlayerInteractEvent event);

}