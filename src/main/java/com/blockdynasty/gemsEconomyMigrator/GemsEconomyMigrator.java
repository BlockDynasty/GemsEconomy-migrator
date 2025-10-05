package com.blockdynasty.gemsEconomyMigrator;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class GemsEconomyMigrator extends JavaPlugin {
    private DbManager dbManager;

    @Override
    public void onLoad() {
        File configFile = new File(getDataFolder(), "config.yml");
        boolean isFirstRun = !configFile.exists();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        if(!isFirstRun){
            try {
                dbManager = new DbManager(
                        getConfig().getString("gemsEconomy.host"),
                        getConfig().getInt("gemsEconomy.port"),
                        getConfig().getString("gemsEconomy.tablePrefix"),
                        getConfig().getString("gemsEconomy.database"),
                        getConfig().getString("gemsEconomy.username"),
                        getConfig().getString("gemsEconomy.password"),

                        getConfig().getString("blockDynasty.host"),
                        getConfig().getInt("blockDynasty.port"),
                        getConfig().getString("blockDynasty.database"),
                        getConfig().getString("blockDynasty.username"),
                        getConfig().getString("blockDynasty.password")
                );
            }catch (Exception e){
                getLogger().severe("Error initializing database connections, ensure the configuration is correct and restart.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }else {
            getLogger().info("Configuration file created. Please configure the database settings in config.yml and restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("GemsEconomyMigrator has been loaded.");
    }

    @Override
    public void onEnable() {
        //register command /startMigration economy
        this.getCommand("gemsMigrator").setExecutor(new Command(this,dbManager));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        //desconectar de ambas bases de datos
        if (dbManager != null) {
            dbManager.closeConnections();
        }
        getLogger().info("GemsEconomyMigrator has been disabled.");
    }
}
