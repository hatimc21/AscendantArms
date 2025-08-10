package hatim.plugings.ascendantArms;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles all logic for the /mythic command and its sub-commands.
 * It implements CommandExecutor, which is the required interface for handling commands.
 */
public class CommandManager implements CommandExecutor {

    private final AscendantArms plugin;
    private final WeaponManager weaponManager;

    public CommandManager(AscendantArms plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /mythic <give|ascend|setlevel>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "ascend":
                return handleAscendCommand(sender);
            case "setlevel":
                return handleSetLevelCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Usage: /mythic <give|ascend|setlevel>");
                return true;
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ascendantarms.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run by a player to receive the item.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mythic give <weapon_id>");
            return true;
        }

        Player player = (Player) sender;
        String weaponID = args[1].toUpperCase();
        MythicWeapon weapon = weaponManager.getWeapon(weaponID);

        if (weapon == null) {
            player.sendMessage(ChatColor.RED + "Unknown weapon ID: " + weaponID);
            return true;
        }

        ItemStack itemToGive = weapon.createItemStack();
        player.getInventory().addItem(itemToGive);
        player.sendMessage(ChatColor.GREEN + "You have been given the " + weapon.getDisplayName());
        return true;
    }

    private boolean handleAscendCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!PersistentDataHandler.isMythic(itemInHand)) {
            player.sendMessage(ChatColor.RED + "You must be holding a Mythic Weapon to ascend it.");
            return true;
        }

        int currentLevel = PersistentDataHandler.getMasteryLevel(itemInHand);
        if (currentLevel >= 7) {
            player.sendMessage(ChatColor.GOLD + "This weapon has already reached maximum mastery.");
            return true;
        }

        int currentXp = PersistentDataHandler.getMasteryXp(itemInHand);
        int xpToNextLevel = MasteryManager.getXpForNextLevel(currentLevel);

        if (currentXp < xpToNextLevel) {
            player.sendMessage(ChatColor.RED + "This weapon is not yet ready to ascend. It needs more Mastery XP.");
            return true;
        }

        int levelCost = 5 + (currentLevel * 5);
        if (player.getLevel() < levelCost) {
            player.sendMessage(ChatColor.RED + "You do not have enough experience to ascend this weapon. You need " + levelCost + " levels.");
            return true;
        }

        player.setLevel(player.getLevel() - levelCost);
        int newLevel = currentLevel + 1;
        PersistentDataHandler.setData(itemInHand, PersistentDataHandler.MASTERY_LEVEL_KEY, PersistentDataType.INTEGER, newLevel);
        PersistentDataHandler.setData(itemInHand, PersistentDataHandler.MASTERY_XP_KEY, PersistentDataType.INTEGER, 0);

        weaponManager.updateWeaponLore(itemInHand);

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Your " + itemInHand.getItemMeta().getDisplayName() + ChatColor.GOLD + "" + ChatColor.BOLD + " has ascended to Mastery Level " + newLevel + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.5f);

        return true;
    }

    /**
     * Handles the new /mythic setlevel <level> command for admins.
     */
    private boolean handleSetLevelCommand(CommandSender sender, String[] args) {
        // Permission and player checks
        if (!sender.hasPermission("ascendantarms.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run by a player.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mythic setlevel <level>");
            return true;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if holding a mythic weapon
        if (!PersistentDataHandler.isMythic(itemInHand)) {
            player.sendMessage(ChatColor.RED + "You must be holding a Mythic Weapon to set its level.");
            return true;
        }

        // Try to parse the level number from the command argument
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
            return true;
        }

        // Validate the level is within our bounds (1-7)
        if (level < 1 || level > 7) {
            player.sendMessage(ChatColor.RED + "Mastery level must be between 1 and 7.");
            return true;
        }

        // All checks passed, set the data
        PersistentDataHandler.setData(itemInHand, PersistentDataHandler.MASTERY_LEVEL_KEY, PersistentDataType.INTEGER, level);
        // Also reset XP to 0 to avoid confusion
        PersistentDataHandler.setData(itemInHand, PersistentDataHandler.MASTERY_XP_KEY, PersistentDataType.INTEGER, 0);

        // Update the lore to reflect the change
        weaponManager.updateWeaponLore(itemInHand);

        player.sendMessage(ChatColor.GREEN + "Set " + itemInHand.getItemMeta().getDisplayName() + ChatColor.GREEN + " to Mastery Level " + level + ".");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

        return true;
    }
}