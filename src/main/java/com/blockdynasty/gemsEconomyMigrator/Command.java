package com.blockdynasty.gemsEconomyMigrator;


import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.Console;

//gemsMigrator
public class Command implements CommandExecutor {
    private final GemsEconomyMigrator plugin;
    private final DbManager dbManager;
    public Command(GemsEconomyMigrator plugin, DbManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if(sender instanceof ConsoleCommandSender){
            if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
                plugin.getLogger().info("Starting migration process...");
                dbManager.migrateData();
                dbManager.migrateAccounts();
                plugin.getLogger().info("Migration process completed.");
            } else {
                sender.sendMessage("Usage: /gemsMigrator start");
            }
        }else {
            sender.sendMessage("This command can only be executed from the console.");
        }
        return true;
    }
}
