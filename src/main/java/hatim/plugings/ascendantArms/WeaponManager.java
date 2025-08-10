package hatim.plugings.ascendantArms;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class WeaponManager implements Listener {

    private final AscendantArms plugin;
    private final Map<String, MythicWeapon> weapons = new HashMap<>();

    public WeaponManager(AscendantArms plugin) {
        this.plugin = plugin;
    }

    public void registerWeapon(MythicWeapon weapon) {
        weapons.put(weapon.getID(), weapon);
        plugin.getLogger().info("Registered Mythic Weapon: " + weapon.getDisplayName());
    }


    public MythicWeapon getWeapon(String id) {
        return weapons.get(id);
    }

    // --- NEW DYNAMIC METHOD ---
    /**
     * Finds the correct weapon based on the item's ID and tells it to update its lore.
     * This makes our system dynamic and able to handle any number of weapons.
     * @param item The Mythic Weapon ItemStack to update.
     */
    public void updateWeaponLore(ItemStack item) {
        if (!PersistentDataHandler.isMythic(item)) return;

        String weaponID = PersistentDataHandler.getMythicID(item);
        MythicWeapon weapon = getWeapon(weaponID);

        if (weapon instanceof UpdatableWeapon) {
            ((UpdatableWeapon) weapon).updateLore(item);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        String weaponID = PersistentDataHandler.getMythicID(itemInHand);
        if (weaponID == null) return;

        MythicWeapon weapon = getWeapon(weaponID);
        if (weapon == null) return;

        if (player.isSneaking()) {
            weapon.handleShiftRightClick(event);
        } else {
            weapon.handleRightClick(event);
        }
    }
}