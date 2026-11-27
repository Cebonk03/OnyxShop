package com.onyxshop.database.types;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.Database;
import com.onyxshop.database.SQLBatch;
import com.onyxshop.database.queries.ClearPlayerStockQuery;
import com.onyxshop.database.queries.ClearStockQuery;
import com.onyxshop.database.queries.GlobalRestockTimerQuery;
import com.onyxshop.database.queries.ItemStockQuery;
import com.onyxshop.database.queries.PlayerRestockTimerQuery;
import com.onyxshop.database.queries.PlayerStockQuery;
import com.onyxshop.database.queries.Query;
import com.onyxshop.database.queries.RemovePlayerStockQuery;
import com.onyxshop.database.queries.RemovePlayerStocksQuery;
import com.onyxshop.database.queries.RemoveStockQuery;
import com.onyxshop.database.queries.RestockScheduleQuery;
import com.onyxshop.database.queries.UpdatePlayerStockQuery;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PlayerPriceModifier;
import com.onyxshop.providers.ScheduleManager;
import com.onyxshop.util.SimplePair;
import org.bukkit.Bukkit;

public class MySQL implements Database {
   private HikariDataSource pool;
   private final String tablePrefix = ConfigManager.getConfig().getString("mysql.table-prefix");
   private final String dynamicStock = this.tablePrefix + "dynamicStock";
   private final String maxStocks = this.tablePrefix + "maxStocks";
   private final String originalStock = this.tablePrefix + "original_stocks";
   private final String playerstock = this.tablePrefix + "playerstock";
   private final String itemstock = this.tablePrefix + "itemstock";
   private final String item_stock_schedules = this.tablePrefix + "item_stock_schedules";
   private final String rotating_shop_sync = this.tablePrefix + "rotating_shop_sync";
   private final String originalLimits = this.tablePrefix + "original_limits";
   private final String sellLimits = this.tablePrefix + "sellLimits";
   private final String playerSellLimits = this.tablePrefix + "playerSellLimits";
   private final String sell_limit_schedules = this.tablePrefix + "sell_limit_schedules";
   private final String player_price_modifiers = this.tablePrefix + "player_price_modifiers";

   public MySQL() {
   }

   @Override
   public boolean connect() {
      try {
         HikariConfig hikariConfig = new HikariConfig();
         hikariConfig.setJdbcUrl(
            "jdbc:mysql://"
               + ConfigManager.getConfig().getString("mysql.host")
               + ":"
               + ConfigManager.getConfig().getString("mysql.port")
               + "/"
               + ConfigManager.getConfig().getString("mysql.database")
         );
         hikariConfig.setUsername(ConfigManager.getConfig().getString("mysql.username"));
         hikariConfig.setPassword(ConfigManager.getConfig().getString("mysql.password"));
         hikariConfig.addDataSourceProperty("userServerPrepStmts", "true");
         hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
         hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
         hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
         if (ConfigManager.getConfig().contains("mysql.pool.useSSL")) {
            hikariConfig.addDataSourceProperty("useSSL", ConfigManager.getConfig().getBoolean("mysql.pool.useSSL"));
         }

         if (ConfigManager.getConfig().contains("mysql.pool.verifyServerCertificate")) {
            hikariConfig.addDataSourceProperty("verifyServerCertificate", ConfigManager.getConfig().getBoolean("mysql.pool.verifyServerCertificate"));
         }

         this.pool = new HikariDataSource(hikariConfig);
         this.pool.setMaxLifetime(ConfigManager.getConfig().getLong("mysql.pool.maximum-lifetime"));
         this.pool.setIdleTimeout(ConfigManager.getConfig().getLong("mysql.pool.idle-timeout"));
         this.pool.setMaximumPoolSize(ConfigManager.getConfig().getInt("mysql.pool.maximum-pool-size"));
         this.pool.setMinimumIdle(ConfigManager.getConfig().getInt("mysql.pool.minimum-idle"));
         this.createTables();
         this.alterOriginalLimitColumn();
         return true;
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to create a connection to the MySQL database");
         exception.printStackTrace();
         return false;
      }
   }

   private void createTables() {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.dynamicStock + " (item VARCHAR(100) NOT NULL, stock INT NOT NULL, UNIQUE(item));");
               statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.maxStocks + " (item VARCHAR(100) NOT NULL, maxStock INT NOT NULL, UNIQUE(item));");
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.playerstock
                     + " (item VARCHAR(100) NOT NULL, uuid VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, restock TEXT, UNIQUE(item,uuid));"
               );
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.itemstock + " (item VARCHAR(100) NOT NULL, amount INT NOT NULL, restock VARCHAR(50), UNIQUE(item));"
               );
               statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.originalStock + " (item VARCHAR(100) NOT NULL,amount INT NOT NULL, UNIQUE(item));");
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.item_stock_schedules
                     + "(item VARCHAR(100) NOT NULL,mode INT NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
               );
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.rotating_shop_sync
                     + "(shop VARCHAR(100) NOT NULL,items TEXT NOT NULL,nextRotation BIGINT NOT NULL,UNIQUE(shop));"
               );
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.playerSellLimits
                     + " (item VARCHAR(100) NOT NULL, uuid VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, restock TEXT, UNIQUE(item,uuid));"
               );
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.sellLimits + " (item VARCHAR(100) NOT NULL, amount INT NOT NULL, restock VARCHAR(50), UNIQUE(item));"
               );
               statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.originalLimits + " (item VARCHAR(100) NOT NULL,amount INT NOT NULL, UNIQUE(item));");
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.sell_limit_schedules
                     + "(item VARCHAR(100) NOT NULL,mode INT NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
               );
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.player_price_modifiers + " (uuid VARCHAR(36) NOT NULL, data VARCHAR(15360) NOT NULL,UNIQUE(uuid));"
               );
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   public void alterOriginalLimitColumn() {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("ALTER TABLE " + this.originalLimits + " DROP COLUMN restock;");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
      }
   }

   @Override
   public int removeExpiredPlayerData(long value) throws SQLException {
      HashSet<String> values = new HashSet<>();
      int value2 = 0;
      long time = new Date().getTime() - value;
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT DISTINCT uuid FROM playerstock;");

            try {
               SendMessage.logDebugMessage("Checking " + resultSet.getFetchSize() + " player stock entries...");

               while (resultSet.next()) {
                  String text = resultSet.getString("uuid");
                  if (Bukkit.getOfflinePlayer(UUID.fromString(text)).getLastPlayed() < time) {
                     values.add(text);
                  } else {
                     value2++;
                  }
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }

            resultSet = statement.executeQuery("SELECT DISTINCT uuid FROM playerSellLimits;");

            try {
               SendMessage.logDebugMessage("Checking " + resultSet.getFetchSize() + " player sell limit entries...");

               while (resultSet.next()) {
                  String text = resultSet.getString("uuid");
                  if (Bukkit.getOfflinePlayer(UUID.fromString(text)).getLastPlayed() < time) {
                     values.add(text);
                  } else {
                     value2++;
                  }
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      SendMessage.logDebugMessage("Found recent database entries for " + value2 + " players, skipping...");
      SendMessage.logDebugMessage("Deleting expired entries...");
      connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String playerName : values) {
               statement.executeUpdate("DELETE FROM playerstock WHERE uuid = '" + playerName + "';");
               statement.executeUpdate("DELETE FROM playerSellLimits WHERE uuid = '" + playerName + "';");
               SendMessage.logDebugMessage("Deleted expired player data for " + playerName);
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      SendMessage.logDebugMessage("Deleted " + values.size() + " expired unique player entries from database");
      return values.size();
   }

   @Override
   public Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException {
      SendMessage.logDebugMessage("Retrieving item indexes from database...");
      HashMap<String, Set<String>> map = new HashMap<>();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String itemId : new String[]{
               "dynamicStock",
               "maxStocks",
               "original_stocks",
               "playerstock",
               "itemstock",
               "item_stock_schedules",
               "original_limits",
               "playerSellLimits",
               "sellLimits",
               "sell_limit_schedules"
            }) {
               ResultSet resultSet = statement.executeQuery("SELECT DISTINCT item FROM " + itemId + ";");

               try {
                  while (resultSet.next()) {
                     map.computeIfAbsent(resultSet.getString("item"), key -> new HashSet<>()).add(itemId);
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      HashMap<String, Map<String, Set<String>>> map2 = new HashMap<>();

      for (Entry entry : map.entrySet()) {
         map2.computeIfAbsent(((String)entry.getKey()).split("\\.")[0], key -> new HashMap<>())
            .put(((String)entry.getKey()).split("\\.", 2)[1], (Set)entry.getValue());
      }

      SendMessage.logDebugMessage("Checking if items still exist...");

      for (String key : map2.keySet()) {
         if (ConfigManager.getShops().contains(key)) {
            List<String> itemsRaw = OnyxShop.getInstance().getConfigManager().getItemsRaw(key);
            HashMap<String, Set<String>> map3 = new HashMap<>();

            for (String text : map2.get(key).keySet()) {
               if (!itemsRaw.contains(text)) {
                  map3.put(text, map2.get(key).get(text));
               }
            }

            map2.put(key, map3);
         }
      }

      return map2;
   }

   @Override
   public void removeExpiredItemData(Map<String, Map<String, Set<String>>> map) throws SQLException {
      SendMessage.logDebugMessage("Removing loaded expired item data from database...");
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String itemId : map.keySet()) {
               List<String> values = new ArrayList<>();
               if (ConfigManager.getShops().contains(itemId)) {
                  values = OnyxShop.getInstance().getConfigManager().getItemsRaw(itemId);
               }

               for (Entry entry : map.get(itemId).entrySet()) {
                  if (!values.contains(entry.getKey())) {
                     SendMessage.logDebugMessage(
                        "Removing data for item '" + itemId + "." + (String)entry.getKey() + "' from " + ((Set)entry.getValue()).size() + " tables..."
                     );

                     for (String text : (Set<String>)entry.getValue()) {
                        statement.executeUpdate("DELETE FROM " + text + " WHERE item = '" + (String)entry.getKey() + "';");
                     }
                  }
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      SendMessage.logDebugMessage(
         "Successfully removed "
            + map.entrySet()
               .stream()
               .flatMap(entry -> ((Map)entry.getValue()).keySet().stream().map(element -> (String)entry.getKey() + "." + element))
               .distinct()
               .count()
            + " non existing item entries from database"
      );
      SendMessage.logDebugMessage("Database successfully cleaned from expired item entries");
   }

   public void migrateV4toV5() throws SQLException {
      long startTime = System.currentTimeMillis();
      OnyxShop plugin = OnyxShop.getInstance();
      HashMap map = new HashMap();
      SendMessage.infoMessage("Loading original stocks from database...");
      Map<String, Integer> originalStocks = this.getOriginalStocks();
      SendMessage.infoMessage("Loading global stock from database...");
      Map<String, SimplePair<Integer, Long>> globalStock = this.getGlobalStock();
      SendMessage.infoMessage("Loading player stock from database...");
      Map<String, Map<String, SimplePair<Integer, Long>>> playerStock = this.getPlayerStock();
      SendMessage.infoMessage("Loading original sell limits from database...");
      Map<String, Integer> originalLimits = this.getOriginalLimits();
      SendMessage.infoMessage("Loading global sell limits from database...");
      Map<String, SimplePair<Integer, Long>> globalSellLimits = this.getGlobalSellLimits();
      SendMessage.infoMessage("Loading player sell limits from database...");
      Map<String, Map<String, SimplePair<Integer, Long>>> playerSellLimits = this.getPlayerSellLimits();
      SendMessage.infoMessage("Started migration of loaded data...");
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            statement.executeUpdate("DROP TABLE " + this.dynamicStock + ";");
            statement.executeUpdate("DROP TABLE " + this.maxStocks + ";");
            plugin.getLimitedStockProvider().emptyCache();
            statement.executeUpdate("DROP TABLE " + this.originalStock + ";");
            statement.executeUpdate("DROP TABLE " + this.playerstock + ";");
            statement.executeUpdate("DROP TABLE " + this.itemstock + ";");
            plugin.getLimitedSellProvider().emptyCache();
            statement.executeUpdate("DROP TABLE " + this.originalLimits + ";");
            statement.executeUpdate("DROP TABLE " + this.playerSellLimits + ";");
            statement.executeUpdate("DROP TABLE " + this.sellLimits + ";");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      this.createTables();

      for (Entry entry : originalStocks.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setOriginalStock(newItem, (Integer)entry.getValue());
         }
      }

      for (Entry entry : globalStock.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setGlobalStock(newItem, (SimplePair<Integer, Long>)entry.getValue());
         }
      }

      for (Entry entry : playerStock.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setPlayerStock(newItem, (Map<String, SimplePair<Integer, Long>>)entry.getValue());
         }
      }

      for (Entry entry : originalLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setOriginalLimit(newItem, (Integer)entry.getValue());
         }
      }

      for (Entry entry : globalSellLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setGlobalLimit(newItem, (SimplePair<Integer, Long>)entry.getValue());
         }
      }

      for (Entry entry : playerSellLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setPlayerLimit(newItem, (Map<String, SimplePair<Integer, Long>>)entry.getValue());
         }
      }

      SendMessage.infoMessage("Successfully migrated loaded data into the database in " + (System.currentTimeMillis() - startTime) + "ms");
   }

   private String getNewItem(Map<String, String> itemIds, OnyxShop plugin, String shopId) {
      String text = (String)itemIds.get(shopId);
      if (text != null) {
         return text;
      } else {
         String[] values = shopId.split("\\.");
         if (values.length != 2) {
            if (values.length == 4) {
               return null;
            } else {
               SendMessage.logDebugMessage("Invalid item format '" + shopId + "', skipping...");
               return null;
            }
         } else {
            String text2 = values[0];
            String text3 = values[1];
            text = plugin.getConfigManager().getItemsRaw(text2).stream().filter(element -> element.split("\\.")[2].equalsIgnoreCase(text3)).findFirst().orElse(null);
            if (text == null) {
               SendMessage.logDebugMessage("Failed to migrate item '" + shopId + "' to new format because it does not exist, skipping...");
            }

            text = text2 + "." + text;
            itemIds.put(shopId, text);
            return text;
         }
      }
   }

   @Override
   public void closeConnection() {
      this.pool.close();
   }

   @Override
   public void executeQuery(SQLBatch.Type type, Query... types) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               for (Query query : types) {
                  statement.executeUpdate(query.format(this.getTable(type, query)));
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void executeBatch(SQLBatch.Type type, Set<Query> values) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               for (Query query : values) {
                  statement.executeUpdate(query.format(this.getTable(type, query)));
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   private String getTable(SQLBatch.Type type, Query query) {
      switch (type) {
         case STOCK:
            if (query instanceof ItemStockQuery
               || query instanceof GlobalRestockTimerQuery
               || query instanceof RemoveStockQuery
               || query instanceof ClearStockQuery) {
               return this.itemstock;
            } else if (query instanceof PlayerStockQuery
               || query instanceof UpdatePlayerStockQuery
               || query instanceof PlayerRestockTimerQuery
               || query instanceof RemovePlayerStockQuery
               || query instanceof RemovePlayerStocksQuery
               || query instanceof ClearPlayerStockQuery) {
               return this.playerstock;
            } else if (query instanceof RestockScheduleQuery) {
               return this.item_stock_schedules;
            }
         case SELL_LIMIT:
            if (query instanceof ItemStockQuery
               || query instanceof GlobalRestockTimerQuery
               || query instanceof RemoveStockQuery
               || query instanceof ClearStockQuery) {
               return this.sellLimits;
            } else if (query instanceof PlayerStockQuery
               || query instanceof UpdatePlayerStockQuery
               || query instanceof PlayerRestockTimerQuery
               || query instanceof RemovePlayerStockQuery
               || query instanceof RemovePlayerStocksQuery
               || query instanceof ClearPlayerStockQuery) {
               return this.playerSellLimits;
            } else if (query instanceof RestockScheduleQuery) {
               return this.sell_limit_schedules;
            }
         default:
            return null;
      }
   }

   @Override
   public PlayerPriceModifier loadPlayerPriceModifiers(UUID playerId) throws SQLException {
      Connection connection = this.pool.getConnection();

      PlayerPriceModifier playerPriceModifier;
      try {
         PreparedStatement statement = connection.prepareStatement("SELECT data FROM " + this.player_price_modifiers + " WHERE uuid=?");

         try {
            statement.setString(1, playerId.toString());
            ResultSet resultSet = statement.executeQuery();

            try {
               playerPriceModifier = resultSet.next() ? (PlayerPriceModifier)new Gson().fromJson(resultSet.getString(1), PlayerPriceModifier.class) : null;
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return playerPriceModifier;
   }

   @Override
   public void savePlayerPriceModifiers(UUID playerId, PlayerPriceModifier playerPriceModifier) throws SQLException {
      if (playerPriceModifier.getSectionModifiers().isEmpty() && playerPriceModifier.getItemModifiers().isEmpty()) {
         Connection connection = this.pool.getConnection();

         try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM " + this.player_price_modifiers + " WHERE uuid = ?;");

            try {
               statement.setString(1, playerId.toString());
               statement.execute();
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } else {
         Connection connection = this.pool.getConnection();

         try {
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + this.player_price_modifiers + " VALUES(?,?);");

            try {
               statement.setString(1, playerId.toString());
               statement.setString(2, new Gson().toJson(playerPriceModifier));
               statement.execute();
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public Map<String, Integer> loadDynamicPricingStock() throws SQLException {
      HashMap itemIds = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.dynamicStock + ";");

            try {
               while (resultSet.next()) {
                  itemIds.put(resultSet.getString("item"), resultSet.getInt("stock"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return itemIds;
   }

   @Override
   public Integer getDynamicStock(String itemId) {
      int value;
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT stock FROM " + this.dynamicStock + " WHERE item = '" + itemId + "';");
               resultSet.next();
               value = resultSet.getInt("stock");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         return null;
      }

      return value;
   }

   @Override
   public Map<String, Integer> getDynamicStocks(Collection<String> values) throws SQLException {
      HashMap map = new HashMap();
      if (values.isEmpty()) {
         return map;
      } else {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT item,stock FROM " + this.dynamicStock + " WHERE item IN ('" + String.join("','", values) + "');");

               try {
                  while (resultSet.next()) {
                     map.put(resultSet.getString("item"), resultSet.getInt("stock"));
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return map;
      }
   }

   @Override
   public Map<String, Integer> loadDynamicMaxStocks() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.maxStocks + ";");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), resultSet.getInt("maxStock"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public void setDynamicMaxStock(String itemId, int value) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.maxStocks + " VALUES('" + itemId + "'," + value + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void saveDynamicStock(Map<String, Integer> stocks) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + this.dynamicStock + " VALUES(?,?);");

            try {
               for (Entry entry : stocks.entrySet()) {
                  statement.setString(1, (String)entry.getKey());
                  statement.setInt(2, (Integer)entry.getValue());
                  statement.execute();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setDynamicStock(String itemId, int value) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.dynamicStock + " VALUES('" + itemId + "'," + value + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public Map<String, Integer> getOriginalStocks() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.originalStock + ";");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public int getOriginalStock(String itemId) {
      try {
         Connection connection = this.pool.getConnection();

         int value;
         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.originalStock + " WHERE item = '" + itemId + "';");
               value = resultSet.getInt("amount");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return value;
      } catch (SQLException exception) {
         return 0;
      }
   }

   @Override
   public void setOriginalStock(String itemId, int value) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.originalStock + " VALUES('" + itemId + "'," + value + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setItemStock(String itemId, int stock) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.itemstock + "(item, amount) VALUES('" + itemId + "', '" + stock + "');");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setItemStock(Map<String, Integer> stocks) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String itemId : stocks.keySet()) {
               statement.executeUpdate("REPLACE INTO " + this.itemstock + "(item, amount) VALUES('" + itemId + "', " + stocks.get(itemId) + ");");
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public void setItemRestockTimes(Map<String, Long> restockTimes) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String itemId : restockTimes.keySet()) {
               statement.executeUpdate("REPLACE INTO " + this.itemstock + " VALUES('" + itemId + "', 0, '" + restockTimes.get(itemId) + "');");
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public int getItemStock(String itemId) throws SQLException {
      Connection connection = this.pool.getConnection();

      int stock;
      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT amount FROM " + this.itemstock + " WHERE item = '" + itemId + "';");
            resultSet.next();
            stock = resultSet.getInt("amount");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return stock;
   }

   @Override
   public Map<String, Integer> getItemStocks(Collection<String> itemIds) throws SQLException {
      HashMap map = new HashMap();
      if (itemIds.isEmpty()) {
         return map;
      } else {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM " + this.itemstock + " WHERE item IN ('" + String.join("','", itemIds) + "');");

               try {
                  while (resultSet.next()) {
                     map.put(resultSet.getString("item"), resultSet.getInt("amount"));
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return map;
      }
   }

   @Override
   public Map<String, Integer> getAllItemStocks() throws SQLException {
      HashMap itemIds = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM " + this.itemstock + ";");

            try {
               while (resultSet.next()) {
                  itemIds.put(resultSet.getString("item"), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return itemIds;
   }

   private void setOriginalStocks(Map<String, Integer> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (Entry entry : map.entrySet()) {
               statement.executeUpdate("REPLACE INTO " + this.originalStock + " VALUES('" + (String)entry.getKey() + "', " + entry.getValue() + ");");
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   private Map<String, SimplePair<Integer, Long>> getGlobalStock() throws SQLException {
      HashMap itemIds = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.itemstock + ";");

            try {
               while (resultSet.next()) {
                  itemIds.put(resultSet.getString("item"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return itemIds;
   }

   private void setGlobalStock(String itemId, SimplePair<Integer, Long> simplePair) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            statement.executeUpdate("REPLACE INTO " + this.itemstock + " VALUES('" + itemId + "', " + simplePair.key + ", " + simplePair.value + ");");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   private Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerStock() throws SQLException {
      HashMap<String, Map<String, SimplePair<Integer, Long>>> map = new HashMap<>();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.playerstock + ";");

            try {
               while (resultSet.next()) {
                  String text = resultSet.getString("item");
                  Map orDefault = map.getOrDefault(text, new HashMap());
                  orDefault.put(resultSet.getString("uuid"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
                  map.put(text, orDefault);
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   private void setPlayerStock(String playerName, Map<String, SimplePair<Integer, Long>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (Entry entry : map.entrySet()) {
               statement.executeUpdate(
                  "REPLACE INTO "
                     + this.playerstock
                     + " VALUES('"
                     + playerName
                     + "', '"
                     + (String)entry.getKey()
                     + "', "
                     + ((SimplePair)entry.getValue()).key
                     + ", "
                     + ((SimplePair)entry.getValue()).value
                     + ");"
               );
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public Map<String, Long> getItemRestockTime() throws SQLException {
      HashMap itemIds = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM " + this.itemstock + " WHERE restock IS NOT NULL;");

            try {
               while (resultSet.next()) {
                  itemIds.put(resultSet.getString("item"), resultSet.getLong("restock"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return itemIds;
   }

   @Override
   public void setPlayerStockAmount(UUID playerId, String itemId, Integer amount) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.playerstock + "(item,uuid,amount) VALUES('" + itemId + "', '" + playerId + "', " + amount + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setPlayerStockAmount(String playerName, Integer stock) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("UPDATE " + this.playerstock + " SET amount = " + stock + " WHERE item = '" + playerName + "';");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setPlayerStock(Map<String, Map<UUID, Integer>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String playerName : map.keySet()) {
               Map<UUID, Integer> stocks = map.get(playerName);

               for (UUID uniqueId : stocks.keySet()) {
                  statement.executeUpdate("REPLACE INTO " + this.playerstock + "(item,uuid,amount) VALUES('" + playerName + "', '" + uniqueId + "', " + stocks.get(uniqueId) + ");");
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public void setPlayerRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String playerName : map.keySet()) {
               Map<UUID, Long> restockTimes = map.get(playerName);

               for (UUID uniqueId : restockTimes.keySet()) {
                  statement.executeUpdate("REPLACE INTO " + this.playerstock + " VALUES('" + playerName + "', '" + uniqueId + "', 0, '" + restockTimes.get(uniqueId) + "');");
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public int getPlayerStockAmount(UUID playerId, String itemId) throws SQLException {
      Connection connection = this.pool.getConnection();

      int amount;
      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT amount FROM " + this.playerstock + " WHERE item = '" + itemId + "' AND uuid = '" + playerId + "';");
            resultSet.next();
            amount = resultSet.getInt("amount");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return amount;
   }

   @Override
   public Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException {
      HashMap playerNames = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM " + this.playerstock + " WHERE restock IS NOT NULL;");

            try {
               while (resultSet.next()) {
                  HashMap map = new HashMap();
                  Statement statement2 = connection.createStatement();

                  try {
                     ResultSet resultSet2 = statement2.executeQuery(
                        "SELECT item,uuid,restock FROM " + this.playerstock + " WHERE restock IS NOT NULL AND item = '" + resultSet.getString("item") + "';"
                     );

                     try {
                        while (resultSet2.next()) {
                           map.put(UUID.fromString(resultSet2.getString("uuid")), resultSet2.getLong("restock"));
                        }
                     } catch (Throwable exception) {
                        if (resultSet2 != null) {
                           try {
                              resultSet2.close();
                           } catch (Throwable exception2) {
                              exception.addSuppressed(exception2);
                           }
                        }

                        throw exception;
                     }

                     if (resultSet2 != null) {
                        resultSet2.close();
                     }
                  } catch (Throwable exception) {
                     if (statement2 != null) {
                        try {
                           statement2.close();
                        } catch (Throwable exception2) {
                           exception.addSuppressed(exception2);
                        }
                     }

                     throw exception;
                  }

                  if (statement2 != null) {
                     statement2.close();
                  }

                  playerNames.put(resultSet.getString("item"), map);
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return playerNames;
   }

   @Override
   public Map<String, Integer> getPlayersStock(String itemId) throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT uuid,amount FROM " + this.playerstock + " WHERE item = '" + itemId + "';");

            try {
               while (resultSet.next()) {
                  String text = resultSet.getString("uuid");
                  map.put(text, resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public Map<String, Integer> getPlayerStockAmounts(Collection<String> playerNames, UUID uniqueId) throws SQLException {
      HashMap itemIds = new HashMap();
      if (playerNames.isEmpty()) {
         return itemIds;
      } else {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery(
                  "SELECT item,amount FROM " + this.playerstock + " WHERE item IN ('" + String.join("','", playerNames) + "') AND uuid = '" + uniqueId + "';"
               );

               try {
                  while (resultSet.next()) {
                     itemIds.put(resultSet.getString("item"), resultSet.getInt("amount"));
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return itemIds;
      }
   }

   @Override
   public Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException {
      HashMap<String, Map<UUID, Integer>> map = new HashMap<>();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,uuid,amount FROM " + this.playerstock + ";");

            try {
               while (resultSet.next()) {
                  map.computeIfAbsent(resultSet.getString("item"), key -> new HashMap<>()).put(UUID.fromString(resultSet.getString("uuid")), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public boolean resetDynamicPricing() {
      try {
         Connection connection = this.pool.getConnection();

         boolean flag;
         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("DROP TABLE " + this.dynamicStock + ";");
               statement.executeUpdate("DROP TABLE " + this.maxStocks + ";");
               this.createTables();
               flag = true;
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return flag;
      } catch (SQLException exception) {
         SendMessage.warnMessage("Failed to delete DynamicPricing tables from database");
         exception.printStackTrace();
         return false;
      }
   }

   @Override
   public int getOriginalLimit(String text) {
      try {
         Connection connection = this.pool.getConnection();

         int limit;
         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.originalLimits + " WHERE item = '" + text + "';");
               limit = resultSet.getInt("amount");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return limit;
      } catch (SQLException exception) {
         return 0;
      }
   }

   @Override
   public void setOriginalLimit(String text, int limit) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.originalLimits + " VALUES('" + text + "'," + limit + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setSellLimit(String text, int limit) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.sellLimits + "(item, amount) VALUES('" + text + "', '" + limit + "');");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setSellLimit(Map<String, Integer> limits) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String key : limits.keySet()) {
               statement.executeUpdate("REPLACE INTO " + this.sellLimits + "(item, amount) VALUES('" + key + "', " + limits.get(key) + ");");
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public void setSellLimitRestockTimes(Map<String, Long> limits) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String key : limits.keySet()) {
               statement.executeUpdate("REPLACE INTO " + this.sellLimits + " VALUES('" + key + "', 0, '" + limits.get(key) + "');");
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public int getSellLimit(String text) throws SQLException {
      Connection connection = this.pool.getConnection();

      int limit;
      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT amount FROM " + this.sellLimits + " WHERE item = '" + text + "';");
            resultSet.next();
            limit = resultSet.getInt("amount");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return limit;
   }

   @Override
   public Map<String, Integer> getSellLimits(Collection<String> values) throws SQLException {
      HashMap map = new HashMap();
      if (values.isEmpty()) {
         return map;
      } else {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM " + this.sellLimits + " WHERE item IN ('" + String.join("','", values) + "');");

               try {
                  while (resultSet.next()) {
                     map.put(resultSet.getString("item"), resultSet.getInt("amount"));
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return map;
      }
   }

   @Override
   public Map<String, Integer> getAllSellLimits() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM " + this.sellLimits + ";");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public Map<String, Long> getSellLimitRestockTimes() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM " + this.sellLimits + " WHERE restock IS NOT NULL;");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), resultSet.getLong("restock"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public void setPlayerSellLimit(UUID playerId, String text, Integer limit) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("REPLACE INTO " + this.playerSellLimits + "(item,uuid,amount) VALUES('" + text + "', '" + playerId + "', " + limit + ");");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setPlayerSellLimit(String playerName, Integer limit) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               statement.executeUpdate("UPDATE " + this.playerSellLimits + " SET amount = " + limit + " WHERE item = '" + playerName + "';");
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void setPlayerSellLimit(Map<String, Map<UUID, Integer>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String playerName : map.keySet()) {
               Map<UUID, Integer> limits = map.get(playerName);

               for (UUID uniqueId : limits.keySet()) {
                  statement.executeUpdate(
                     "REPLACE INTO " + this.playerSellLimits + "(item,uuid,amount) VALUES('" + playerName + "', '" + uniqueId + "', " + limits.get(uniqueId) + ");"
                  );
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (String playerName : map.keySet()) {
               Map<UUID, Long> limits = map.get(playerName);

               for (UUID uniqueId : limits.keySet()) {
                  statement.executeUpdate("REPLACE INTO " + this.playerSellLimits + " VALUES('" + playerName + "', '" + uniqueId + "', 0, '" + limits.get(uniqueId) + "');");
               }
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public int getPlayerSellLimit(UUID playerId, String text) throws SQLException {
      Connection connection = this.pool.getConnection();

      int limit;
      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT amount FROM " + this.playerSellLimits + " WHERE item = '" + text + "' AND uuid = '" + playerId + "';");
            resultSet.next();
            limit = resultSet.getInt("amount");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return limit;
   }

   @Override
   public Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException {
      HashMap playerNames = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM " + this.playerSellLimits + " WHERE restock IS NOT NULL;");

            try {
               while (resultSet.next()) {
                  HashMap map = new HashMap();
                  Statement statement2 = connection.createStatement();

                  try {
                     ResultSet resultSet2 = statement2.executeQuery(
                        "SELECT item,uuid,restock FROM " + this.playerSellLimits + " WHERE restock IS NOT NULL AND item = '" + resultSet.getString("item") + "';"
                     );

                     try {
                        while (resultSet2.next()) {
                           map.put(UUID.fromString(resultSet2.getString("uuid")), resultSet2.getLong("restock"));
                        }
                     } catch (Throwable exception) {
                        if (resultSet2 != null) {
                           try {
                              resultSet2.close();
                           } catch (Throwable exception2) {
                              exception.addSuppressed(exception2);
                           }
                        }

                        throw exception;
                     }

                     if (resultSet2 != null) {
                        resultSet2.close();
                     }
                  } catch (Throwable exception) {
                     if (statement2 != null) {
                        try {
                           statement2.close();
                        } catch (Throwable exception2) {
                           exception.addSuppressed(exception2);
                        }
                     }

                     throw exception;
                  }

                  if (statement2 != null) {
                     statement2.close();
                  }

                  playerNames.put(resultSet.getString("item"), map);
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return playerNames;
   }

   @Override
   public Map<String, Integer> getPlayerSellLimits(String playerName) throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT uuid,amount FROM " + this.playerSellLimits + " WHERE item = '" + playerName + "';");

            try {
               while (resultSet.next()) {
                  String text = resultSet.getString("uuid");
                  map.put(text, resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> playerNames, UUID uniqueId) throws SQLException {
      HashMap map = new HashMap();
      if (playerNames.isEmpty()) {
         return map;
      } else {
         Connection connection = this.pool.getConnection();

         try {
            Statement statement = connection.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery(
                  "SELECT item,amount FROM " + this.playerSellLimits + " WHERE item IN ('" + String.join("','", playerNames) + "') AND uuid = '" + uniqueId + "';"
               );

               try {
                  while (resultSet.next()) {
                     map.put(resultSet.getString("item"), resultSet.getInt("amount"));
                  }
               } catch (Throwable exception) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return map;
      }
   }

   @Override
   public Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException {
      HashMap<String, Map<UUID, Integer>> map = new HashMap<>();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT item,uuid,amount FROM " + this.playerSellLimits + ";");

            try {
               while (resultSet.next()) {
                  map.computeIfAbsent(resultSet.getString("item"), key -> new HashMap<>()).put(UUID.fromString(resultSet.getString("uuid")), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public void setItemStockSchedulers(Map<ScheduleManager.Item, Long> stocks) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + this.item_stock_schedules + " VALUES(?,?,?);");

         try {
            for (Entry entry : stocks.entrySet()) {
               statement.setString(1, ((ScheduleManager.Item)entry.getKey()).getItemPath());
               statement.setInt(2, ((ScheduleManager.Item)entry.getKey()).mode);
               statement.setString(3, ((Long)entry.getValue()).toString());
               statement.execute();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException {
      HashMap itemIds = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.item_stock_schedules + ";");

            try {
               while (resultSet.next()) {
                  itemIds.put(new ScheduleManager.Item(resultSet.getString("item"), resultSet.getInt("mode")), Long.parseLong(resultSet.getString("restock")));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return itemIds;
   }

   @Override
   public void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> limits) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + this.sell_limit_schedules + " VALUES(?,?,?);");

         try {
            for (Entry entry : limits.entrySet()) {
               statement.setString(1, ((ScheduleManager.Item)entry.getKey()).getItemPath());
               statement.setInt(2, ((ScheduleManager.Item)entry.getKey()).mode);
               statement.setString(3, ((Long)entry.getValue()).toString());
               statement.execute();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.sell_limit_schedules + ";");

            try {
               while (resultSet.next()) {
                  map.put(new ScheduleManager.Item(resultSet.getString("item"), resultSet.getInt("mode")), Long.parseLong(resultSet.getString("restock")));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public Map<String, Integer> getOriginalLimits() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.originalLimits + ";");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), resultSet.getInt("amount"));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   @Override
   public void setRotatingShopItems(String shopId, List<String> values, long value) {
      try {
         Connection connection = this.pool.getConnection();

         try {
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + this.rotating_shop_sync + " VALUES(?,?,?);");

            try {
               statement.setString(1, shopId);
               statement.setString(2, String.join(",", values));
               statement.setLong(3, value);
               statement.execute();
            } catch (Throwable exception) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public SimplePair<List<String>, Long> getRotatingShopItems(String shopId) throws SQLException {
      Connection connection = this.pool.getConnection();

      SimplePair simplePair;
      label100: {
         try {
            PreparedStatement statement;
            label102: {
               statement = connection.prepareStatement("SELECT items,nextRotation FROM " + this.rotating_shop_sync + " WHERE shop = ?;");

               try {
                  statement.setString(1, shopId);
                  ResultSet resultSet = statement.executeQuery();

                  label85: {
                     try {
                        if (!resultSet.next()) {
                           simplePair = null;
                           break label85;
                        }

                        simplePair = new SimplePair<>(Arrays.asList(resultSet.getString("items").split(",")), resultSet.getLong("nextRotation"));
                     } catch (Throwable exception) {
                        if (resultSet != null) {
                           try {
                              resultSet.close();
                           } catch (Throwable exception2) {
                              exception.addSuppressed(exception2);
                           }
                        }

                        throw exception;
                     }

                     if (resultSet != null) {
                        resultSet.close();
                     }
                     break label102;
                  }

                  if (resultSet != null) {
                     resultSet.close();
                  }
               } catch (Throwable exception) {
                  if (statement != null) {
                     try {
                        statement.close();
                     } catch (Throwable exception2) {
                        exception.addSuppressed(exception2);
                     }
                  }

                  throw exception;
               }

               if (statement != null) {
                  statement.close();
               }
               break label100;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable exception) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (connection != null) {
            connection.close();
         }

         return simplePair;
      }

      if (connection != null) {
         connection.close();
      }

      return simplePair;
   }

   private Map<String, SimplePair<Integer, Long>> getGlobalSellLimits() throws SQLException {
      HashMap map = new HashMap();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.sellLimits + ";");

            try {
               while (resultSet.next()) {
                  map.put(resultSet.getString("item"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   private void setGlobalLimit(String text, SimplePair<Integer, Long> simplePair) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            statement.executeUpdate("REPLACE INTO " + this.sellLimits + " VALUES('" + text + "', " + simplePair.key + ", " + simplePair.value + ");");
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }

   private Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerSellLimits() throws SQLException {
      HashMap<String, Map<String, SimplePair<Integer, Long>>> map = new HashMap<>();
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.playerSellLimits + ";");

            try {
               while (resultSet.next()) {
                  String text = resultSet.getString("item");
                  Map<String, SimplePair<Integer, Long>> orDefault = map.getOrDefault(text, new HashMap<>());
                  orDefault.put(resultSet.getString("uuid"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
                  map.put(text, orDefault);
               }
            } catch (Throwable exception) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (resultSet != null) {
               resultSet.close();
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }

      return map;
   }

   private void setPlayerLimit(String playerName, Map<String, SimplePair<Integer, Long>> map) throws SQLException {
      Connection connection = this.pool.getConnection();

      try {
         Statement statement = connection.createStatement();

         try {
            for (Entry entry : map.entrySet()) {
               statement.executeUpdate(
                  "REPLACE INTO "
                     + this.playerSellLimits
                     + " VALUES('"
                     + playerName
                     + "', '"
                     + (String)entry.getKey()
                     + "', "
                     + ((SimplePair)entry.getValue()).key
                     + ", "
                     + ((SimplePair)entry.getValue()).value
                     + ");"
               );
            }
         } catch (Throwable exception) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }
            }

            throw exception;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable exception) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (connection != null) {
         connection.close();
      }
   }
}
