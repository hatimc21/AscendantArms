package hatim.plugings.ascendantArms;

import hatim.plugings.ascendantArms.weapons.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class AscendantArms extends JavaPlugin {

    private WeaponManager weaponManager;
    private CommandManager commandManager;
    private MasteryManager masteryManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        this.weaponManager = new WeaponManager(this);
        getServer().getPluginManager().registerEvents(this.weaponManager, this);

        this.commandManager = new CommandManager(this, this.weaponManager);
        getCommand("mythic").setExecutor(this.commandManager);

        this.masteryManager = new MasteryManager(this, this.weaponManager);
        getServer().getPluginManager().registerEvents(this.masteryManager, this);

        this.bossBarManager = new BossBarManager(this, this.weaponManager);
        getServer().getPluginManager().registerEvents(this.bossBarManager, this);

        registerWeapons();
        getLogger().info("Ascendant Arms has been forged and is ready for battle!");
    }

    private void registerWeapons() {
        // --- Standard Weapons ---
        weaponManager.registerWeapon(new RiftBlade());
        weaponManager.registerWeapon(new TidalTrident());
        weaponManager.registerWeapon(new EarthshatterAxe());
        weaponManager.registerWeapon(new ShadowDaggers());
        weaponManager.registerWeapon(new Soulscythe());

        // --- Weapons that are also Event Listeners ---
        // For these, we create the instance first, register its specific events,
        // and then register it with our WeaponManager.

        SunfireBow sunfireBow = new SunfireBow();
        getServer().getPluginManager().registerEvents(sunfireBow, this);
        weaponManager.registerWeapon(sunfireBow);

        CycloneCrossbow cycloneCrossbow = new CycloneCrossbow(); // This will also be a listener
        getServer().getPluginManager().registerEvents(cycloneCrossbow, this);
        weaponManager.registerWeapon(cycloneCrossbow);
    }

    @Override
    public void onDisable() {
        getLogger().info("Ascendant Arms has been sheathed.");
    }

    public WeaponManager getWeaponManager() {
        return weaponManager;
    }
}