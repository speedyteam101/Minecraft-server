package com.speedyteam101.demonslayer;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/** Demon Slayer (Kimetsu no Yaiba) themed gameplay for Paper 1.20.1. Unofficial fan project. */
public final class DemonSlayerPlugin extends JavaPlugin {
    private static DemonSlayerPlugin instance;

    private Combat combat;
    private Slayers slayers;
    private Forms forms;
    private Demons demons;

    public static DemonSlayerPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Keys.init(this);

        combat = new Combat(this);
        slayers = new Slayers(this);
        forms = new Forms();
        demons = new Demons(this);
        Items.registerRecipes();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(combat, this);
        pm.registerEvents(demons, this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new StyleMenu.Clicks(this), this);

        PluginCommand command = getCommand("slayer");
        if (command != null) {
            SlayerCommand executor = new SlayerCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        slayers.start();
        demons.scanLoaded();
        demons.start();
        getLogger().info("Demon Slayer ready: " + Style.values().length + " styles, " + forms.count()
                + " forms, " + DemonType.values().length + " demon types.");
    }

    @Override
    public void onDisable() {
        if (demons != null) {
            demons.shutdown();
        }
    }

    public Combat combat() {
        return combat;
    }

    public Slayers slayers() {
        return slayers;
    }

    public Forms forms() {
        return forms;
    }

    public Demons demons() {
        return demons;
    }
}
