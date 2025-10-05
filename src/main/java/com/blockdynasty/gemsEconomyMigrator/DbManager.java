package com.blockdynasty.gemsEconomyMigrator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Server;
import org.json.simple.JSONObject;

import java.net.Socket;
import java.sql.Connection;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DbManager {
    private final String gemsTablePrefix;
    private HikariDataSource gemsEconomyDataSource;
    private HikariDataSource blockDynastyDataSource;

    //entry gemsTablePrefix from config.yml
    public DbManager(String gemsHost, int gemsPort, String gemsTablePrefix,String gemsDb, String gemsUser, String gemsPass,
                     String bdHost, int bdPort, String bdDb, String bdUser, String bdPass) {
        this.gemsTablePrefix = gemsTablePrefix;
        try {
            // Initialize GemsEconomy database connection
            HikariConfig gemsConfig = new HikariConfig();
            gemsConfig.setJdbcUrl("jdbc:mysql://" + gemsHost + ":" + gemsPort + "/" + gemsDb + "?allowPublicKeyRetrieval=true&useSSL=false");
            gemsConfig.setUsername(gemsUser);
            gemsConfig.setPassword(gemsPass);
            gemsConfig.setMaxLifetime(1500000);
            gemsConfig.addDataSourceProperty("cachePrepStmts", "true");
            gemsConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            gemsConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            gemsConfig.addDataSourceProperty("userServerPrepStmts", "true");
            gemsEconomyDataSource = new HikariDataSource(gemsConfig);

            // Initialize BlockDynasty database connection
            HikariConfig bdConfig = new HikariConfig();
            bdConfig.setJdbcUrl("jdbc:mysql://" + bdHost + ":" + bdPort + "/" + bdDb + "?allowPublicKeyRetrieval=true&useSSL=false");
            bdConfig.setUsername(bdUser);
            bdConfig.setPassword(bdPass);
            bdConfig.setMaxLifetime(1500000);
            bdConfig.addDataSourceProperty("cachePrepStmts", "true");
            bdConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            bdConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            bdConfig.addDataSourceProperty("userServerPrepStmts", "true");
            blockDynastyDataSource = new HikariDataSource(bdConfig);
        }catch (Exception e){
            throw new RuntimeException("Error initializing database connections");
        }


    }

    public Connection getGemsConnection() throws SQLException {
        return gemsEconomyDataSource.getConnection();
    }

    public Connection getBlockDynastyConnection() throws SQLException {
        return blockDynastyDataSource.getConnection();
    }

    public void closeConnections() {
        if (gemsEconomyDataSource != null && !gemsEconomyDataSource.isClosed()) {
            gemsEconomyDataSource.close();
        }

        if (blockDynastyDataSource != null && !blockDynastyDataSource.isClosed()) {
            blockDynastyDataSource.close();
        }
    }


    //metodo para transferir los datos de una base de datos a otra
    public void migrateData() {
        final String GET_GEMS_CURRENCIES = "SELECT * FROM `" + this.gemsTablePrefix + "_currencies`";
        final String CHECK_BD_CURRENCY = "SELECT COUNT(*) FROM currency WHERE uuid = ?";
        //migrar los datos de una base de datos a otra
        final String INSERT_BD_CURRENCY = "INSERT INTO `currency` (`uuid`, `name_singular`, `name_plural`, "
                + "`default_balance`, `symbol`, `decimal_supported`, `default_currency`, `transferable`, "
                + "`color`, `exchange_rate`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


        try (Connection gemsConn = getGemsConnection();
             Connection bdConn = getBlockDynastyConnection()) {

            // Migrate currencies
            try (var gemsStmt = gemsConn.createStatement();
                 var bdCheckCurrency = bdConn.prepareStatement(CHECK_BD_CURRENCY);
                 var bdInsertCurrency = bdConn.prepareStatement(INSERT_BD_CURRENCY)) {

                var currenciesRS = gemsStmt.executeQuery(GET_GEMS_CURRENCIES);
                int currencyCount = 0;

                while (currenciesRS.next()) {
                    String uuid = currenciesRS.getString("uuid");

                    // Check if currency exists in BlockDynasty
                    bdCheckCurrency.setString(1, uuid);
                    var checkRS = bdCheckCurrency.executeQuery();
                    checkRS.next();
                    if (checkRS.getInt(1) > 0) {
                        // Currency already exists, could update if needed
                        continue;
                    }

                    // Map and insert currency
                    bdInsertCurrency.setString(1, uuid);
                    bdInsertCurrency.setString(2, currenciesRS.getString("name_singular"));
                    bdInsertCurrency.setString(3, currenciesRS.getString("name_plural"));
                    bdInsertCurrency.setDouble(4, currenciesRS.getDouble("default_balance"));
                    bdInsertCurrency.setString(5, nullToEmpty(currenciesRS.getString("symbol")));
                    bdInsertCurrency.setBoolean(6, currenciesRS.getBoolean("decimals_supported"));
                    // Map is_default to default_currency
                    bdInsertCurrency.setBoolean(7, currenciesRS.getBoolean("is_default"));
                    // Map payable to transferable
                    bdInsertCurrency.setBoolean(8, currenciesRS.getBoolean("payable"));
                    bdInsertCurrency.setString(9, nullToEmpty(currenciesRS.getString("color")));
                    bdInsertCurrency.setDouble(10, currenciesRS.getDouble("exchange_rate"));

                    bdInsertCurrency.executeUpdate();
                    currencyCount++;
                }

                System.out.println("Migrated " + currencyCount + " currencies");
            }
        } catch (SQLException e) {
            System.err.println("Error during migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void migrateAccounts() {
        final String GET_GEMS_ACCOUNTS = "SELECT * FROM `" + this.gemsTablePrefix + "_accounts`";
        final String CHECK_BD_ACCOUNT = "SELECT COUNT(*) FROM account WHERE uuid = ?";
        final String INSERT_BD_WALLET = "INSERT INTO wallet () VALUES ()";
        final String INSERT_BD_ACCOUNT = "INSERT INTO account (nickname, uuid, block, can_receive_currency, wallet_id) VALUES (?, ?, ?, ?, ?)";
        final String INSERT_BD_BALANCE = "INSERT INTO balance (amount, currency_id, wallet_id) VALUES (?, ?, ?)";

        try (Connection gemsConn = getGemsConnection();
             Connection bdConn = getBlockDynastyConnection()) {

            try (var gemsStmt = gemsConn.createStatement();
                 var bdCheckAccount = bdConn.prepareStatement(CHECK_BD_ACCOUNT);
                 var bdInsertWallet = bdConn.prepareStatement(INSERT_BD_WALLET, Statement.RETURN_GENERATED_KEYS);
                 var bdInsertAccount = bdConn.prepareStatement(INSERT_BD_ACCOUNT);
                 var bdInsertBalance = bdConn.prepareStatement(INSERT_BD_BALANCE); ) {

                var accountsRS = gemsStmt.executeQuery(GET_GEMS_ACCOUNTS);
                int accountCount = 0;
                int balanceCount = 0;

                JSONParser parser = new JSONParser();

                while (accountsRS.next()) {
                    String uuid = accountsRS.getString("uuid");
                    String nickname = accountsRS.getString("nickname");
                    boolean payable = accountsRS.getBoolean("payable");
                    String balanceData = accountsRS.getString("balance_data");

                    // Check if account exists
                    bdCheckAccount.setString(1, uuid);
                    var checkRS = bdCheckAccount.executeQuery();
                    checkRS.next();
                    if (checkRS.getInt(1) > 0) {
                        continue;
                    }

                    // Create wallet
                    bdInsertWallet.executeUpdate();
                    ResultSet walletKeys = bdInsertWallet.getGeneratedKeys();
                    if (!walletKeys.next()) {
                        System.out.println("Failed to get generated wallet ID for " + nickname);
                        continue;
                    }
                    int walletId = walletKeys.getInt(1);

                    // Create account
                    bdInsertAccount.setString(1, nickname);
                    bdInsertAccount.setString(2, uuid);
                    bdInsertAccount.setBoolean(3, false); // block
                    bdInsertAccount.setBoolean(4, payable); // can_receive_currency
                    bdInsertAccount.setInt(5, walletId);
                    bdInsertAccount.executeUpdate();

                    // Parse balance data
                    try {
                        JSONObject balances = (JSONObject) parser.parse(balanceData);
                        for (Object key : balances.keySet()) {
                            String currencyUuid = (String) key;
                            double amount =  (Double) balances.get(key);

                            // Use UUID directly as the currency_id
                            bdInsertBalance.setDouble(1, amount);
                            bdInsertBalance.setString(2, currencyUuid);
                            bdInsertBalance.setInt(3, walletId);
                            bdInsertBalance.executeUpdate();
                            balanceCount++;
                        }
                    } catch (ParseException e) {
                        System.out.println("Error parsing balance data for " + nickname + ": " + e.getMessage());
                    }

                    accountCount++;
                }

                System.out.println("Migrated " + accountCount + " accounts");
            }
        } catch (SQLException e) {
            System.out.println("Error migrating accounts: " + e.getMessage());
        }
    }


    private String nullToEmpty(String input) {
        return input == null ? "" : input;
    }
}
