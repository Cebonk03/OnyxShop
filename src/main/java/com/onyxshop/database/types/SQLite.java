package com.onyxshop.database.types;

import com.google.gson.Gson;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PlayerPriceModifier;
import com.onyxshop.providers.ScheduleManager;
import com.onyxshop.util.SimplePair;
import org.bukkit.Bukkit;

public class SQLite implements Database {
   private Connection conn;

   public SQLite() {
   }

   @Override
   public synchronized boolean connect() {
      try {
         Class.forName("org.sqlite.JDBC");
         this.conn = DriverManager.getConnection("jdbc:sqlite:" + OnyxShop.getInstance().getDataFolder() + File.separator + "database.db");
         this.createTables();
         this.migrate();
         return true;
      } catch (Exception exception) {
         SendMessage.errorMessage(Lang.ERROR_OCCURRED_WHILE_CONNECTING_TO_DATABASE.get());
         exception.printStackTrace();
         return false;
      }
   }

   private synchronized void ensureColumn(String tableName, String columnName, String columnDefinition) {
      try {
         ResultSet resultSet = this.conn.createStatement().executeQuery("PRAGMA table_info(" + tableName + ");");

         try {
            while (resultSet.next()) {
               if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                  return;
               }
            }
         } finally {
            resultSet.close();
         }
      } catch (SQLException exception) {
      }

      try {
         this.conn.createStatement().executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";");
      } catch (SQLException exception) {
         SendMessage.warnMessage("Could not add " + tableName + "." + columnName + ": " + exception.getMessage());
      }
   }

   private synchronized void migrate() {
      this.ensureColumn("playerstock", "restock", "TEXT");
      this.ensureColumn("itemstock", "restock", "TEXT");
      this.ensureColumn("playerSellLimits", "restock", "TEXT");
      this.ensureColumn("sellLimits", "restock", "TEXT");
   }

   @Override
   public synchronized int removeExpiredPlayerData(long value) throws SQLException {
      HashSet<String> values = new HashSet<>();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT DISTINCT uuid FROM playerstock;");
      int value2 = 0;
      long time = new Date().getTime() - value;
      SendMessage.logDebugMessage("Checking " + resultSet.getFetchSize() + " player stock entries...");

      while (resultSet.next()) {
         String text = resultSet.getString("uuid");
         if (Bukkit.getOfflinePlayer(UUID.fromString(text)).getLastPlayed() < time) {
            values.add(text);
         } else {
            value2++;
         }
      }

      resultSet = statement.executeQuery("SELECT DISTINCT uuid FROM playerSellLimits;");
      SendMessage.logDebugMessage("Checking " + resultSet.getFetchSize() + " player sell limit entries...");

      while (resultSet.next()) {
         String text = resultSet.getString("uuid");
         if (Bukkit.getOfflinePlayer(UUID.fromString(text)).getLastPlayed() < time) {
            values.add(text);
         } else {
            value2++;
         }
      }

      SendMessage.logDebugMessage("Found recent database entries for " + value2 + " players, skipping...");
      SendMessage.logDebugMessage("Deleting expired entries...");
      statement = this.conn.createStatement();

      for (String playerId : values) {
         statement.executeUpdate("DELETE FROM playerstock WHERE uuid = '" + playerId + "';");
         statement.executeUpdate("DELETE FROM playerSellLimits WHERE uuid = '" + playerId + "';");
      }

      SendMessage.logDebugMessage("Deleted " + values.size() + " expired unique player entries from database, vacuuming...");
      statement = this.conn.createStatement();
      statement.execute("VACUUM");
      SendMessage.logDebugMessage("Vacuum completed");
      resultSet.close();
      statement.close();
      return values.size();
   }

   @Override
   public synchronized Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException {
      Statement statement = this.conn.createStatement();
      SendMessage.logDebugMessage("Retrieving item indexes from database...");
      HashMap<String, Set<String>> map = new HashMap<>();

      for (String table : new String[]{
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
         ResultSet resultSet = statement.executeQuery("SELECT DISTINCT item FROM " + table + ";");

         while (resultSet.next()) {
            map.computeIfAbsent(resultSet.getString("item"), key -> new HashSet<>()).add(table);
         }
      }

      HashMap<String, Map<String, Set<String>>> map2 = new HashMap<>();

      for (Entry<String, Set<String>> entry : map.entrySet()) {
         map2.computeIfAbsent(entry.getKey().split("\\.")[0], key -> new HashMap<>())
            .put(entry.getKey().split("\\.", 2)[1], entry.getValue());
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
   public synchronized void removeExpiredItemData(Map<String, Map<String, Set<String>>> map) throws SQLException {
      SendMessage.logDebugMessage("Removing loaded expired item data from database...");
      Statement statement = this.conn.createStatement();

      for (String itemId : map.keySet()) {
         List<String> values = new ArrayList<>();
         if (ConfigManager.getShops().contains(itemId)) {
            values = OnyxShop.getInstance().getConfigManager().getItemsRaw(itemId);
         }

         for (Entry<String, Set<String>> entry : map.get(itemId).entrySet()) {
            if (!values.contains(entry.getKey())) {
               SendMessage.logDebugMessage(
                  "Removing data for item '"
                     + itemId
                     + "."
                     + (String)entry.getKey()
                     + "' from "
                     + Arrays.toString(((Set)entry.getValue()).toArray())
                     + " tables..."
               );

               for (String text : entry.getValue()) {
                  statement.executeUpdate("DELETE FROM " + text + " WHERE item = '" + itemId + "." + (String)entry.getKey() + "';");
               }
            }
         }
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
      SendMessage.logDebugMessage("Resizing file...");
      statement = this.conn.createStatement();
      statement.execute("VACUUM");
      statement.close();
      SendMessage.logDebugMessage("Vacuum completed, database successfully cleaned from expired item entries");
   }

   public synchronized void migrateV4toV5() throws SQLException {
      long startTime = System.currentTimeMillis();
      OnyxShop plugin = OnyxShop.getInstance();
      HashMap<String, String> map = new HashMap<>();
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
      Statement statement = this.conn.createStatement();
      statement.executeUpdate("DROP TABLE dynamicStock;");
      statement.executeUpdate("DROP TABLE maxStocks;");
      plugin.getLimitedStockProvider().emptyCache();
      statement.executeUpdate("DROP TABLE original_stocks;");
      statement.executeUpdate("DROP TABLE playerstock;");
      statement.executeUpdate("DROP TABLE itemstock;");
      plugin.getLimitedSellProvider().emptyCache();
      statement.executeUpdate("DROP TABLE original_limits;");
      statement.executeUpdate("DROP TABLE playerSellLimits;");
      statement.executeUpdate("DROP TABLE sellLimits;");
      statement.close();
      this.createTables();

      for (Entry<String, Integer> entry : originalStocks.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setOriginalStock(newItem, (Integer)entry.getValue());
         }
      }

      for (Entry<String, SimplePair<Integer, Long>> entry : globalStock.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setGlobalStock(newItem, (SimplePair<Integer, Long>)entry.getValue());
         }
      }

      for (Entry<String, Map<String, SimplePair<Integer, Long>>> entry : playerStock.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setPlayerStock(newItem, (Map<String, SimplePair<Integer, Long>>)entry.getValue());
         }
      }

      for (Entry<String, Integer> entry : originalLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setOriginalLimit(newItem, (Integer)entry.getValue());
         }
      }

      for (Entry<String, SimplePair<Integer, Long>> entry : globalSellLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setGlobalLimit(newItem, (SimplePair<Integer, Long>)entry.getValue());
         }
      }

      for (Entry<String, Map<String, SimplePair<Integer, Long>>> entry : playerSellLimits.entrySet()) {
         String newItem = this.getNewItem(map, plugin, (String)entry.getKey());
         if (newItem != null) {
            this.setPlayerLimit(newItem, (Map<String, SimplePair<Integer, Long>>)entry.getValue());
         }
      }

      SendMessage.infoMessage("Successfully migrated loaded data into the database in " + (System.currentTimeMillis() - startTime) + "ms");
   }

   private synchronized String getNewItem(Map<String, String> itemIds, OnyxShop plugin, String shopId) {
      String text = (String)itemIds.get(shopId);
      if (text != null) {
         return text;
      } else {
         String[] values = shopId.split("\\.");
         if (values.length != 2) {
            if (values.length == 4) {
               return null;
            } else {
               SendMessage.logDebugMessage("Failed to migrate item " + shopId + " to new format, skipping...");
               return null;
            }
         } else {
            String text2 = values[0];
            String text3 = values[1];
            text = plugin.getConfigManager().getItemsRaw(text2).stream().filter(element -> element.split("\\.")[2].equalsIgnoreCase(text3)).findFirst().orElse(null);
            text = text2 + "." + text;
            itemIds.put(shopId, text);
            return text;
         }
      }
   }

   @Override
   public synchronized void closeConnection() {
      try {
         this.conn.close();
      } catch (SQLException exception) {
         SendMessage.errorMessage("A error occurred while closing the connection with the database");
         exception.printStackTrace();
      }
   }

   private synchronized void createTables() {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS dynamicStock (item TEXT NOT NULL UNIQUE,stock INTEGER NOT NULL);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS maxStocks (item TEXT NOT NULL UNIQUE,maxStock INTEGER NOT NULL);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS original_stocks (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL);");
         statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS playerstock (item TEXT NOT NULL,uuid TEXT NOT NULL,amount INTEGER NOT NULL,restock TEXT,UNIQUE(item,uuid));"
         );
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS itemstock (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL,restock TEXT);");
         statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS item_stock_schedules(item TEXT NOT NULL,mode INTEGER NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
         );
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS original_limits (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL);");
         statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS playerSellLimits (item TEXT NOT NULL,uuid TEXT NOT NULL,amount INTEGER NOT NULL,restock TEXT,UNIQUE(item,uuid));"
         );
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS sellLimits (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL,restock TEXT);");
         statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS sell_limit_schedules(item TEXT NOT NULL,mode INTEGER NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
         );
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_price_modifiers (uuid TEXT NOT NULL UNIQUE, data TEXT NOT NULL);");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized Map<String, Integer> loadDynamicPricingStock() throws SQLException {
      HashMap itemIds = new HashMap();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM dynamicStock;");

      while (resultSet.next()) {
         itemIds.put(resultSet.getString("item"), resultSet.getInt("stock"));
      }

      resultSet.close();
      statement.close();
      return itemIds;
   }

   @Override
   public synchronized Integer getDynamicStock(String itemId) {
      int value;
      try {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT stock FROM dynamicStock WHERE item = '" + itemId + "';");
         value = resultSet.getInt("stock");
         resultSet.close();
         statement.close();
      } catch (SQLException exception) {
         return null;
      }

      return value;
   }

   @Override
   public synchronized Map<String, Integer> getDynamicStocks(Collection<String> values) throws SQLException {
      HashMap map = new HashMap();
      if (values.isEmpty()) {
         return map;
      } else {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT item,stock FROM dynamicStock WHERE item IN ('" + String.join("','", values) + "');");

         while (resultSet.next()) {
            map.put(resultSet.getString("item"), resultSet.getInt("stock"));
         }

         resultSet.close();
         statement.close();
         return map;
      }
   }

   @Override
   public synchronized Map<String, Integer> loadDynamicMaxStocks() throws SQLException {
      HashMap map = new HashMap();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM maxStocks;");

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), resultSet.getInt("maxStock"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized void setDynamicMaxStock(String itemId, int value) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO maxStocks VALUES('" + itemId + "'," + value + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void saveDynamicStock(Map<String, Integer> stocks) {
      try {
         PreparedStatement statement = this.conn.prepareStatement("REPLACE INTO dynamicStock VALUES(?,?);");

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
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setDynamicStock(String itemId, int value) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO dynamicStock VALUES('" + itemId + "'," + value + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized int getOriginalStock(String itemId) {
      try {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT * FROM original_stocks WHERE item = '" + itemId + "';");
         int amount = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return amount;
      } catch (SQLException exception) {
         return 0;
      }
   }

   @Override
   public synchronized void setOriginalStock(String itemId, int value) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO original_stocks VALUES('" + itemId + "'," + value + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setItemStock(String itemId, int stock) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO itemstock(item, amount) VALUES('" + itemId + "', '" + stock + "');");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setItemStock(Map<String, Integer> stocks) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : stocks.keySet()) {
         statement.executeUpdate("REPLACE INTO itemstock(item, amount) VALUES('" + itemId + "', " + stocks.get(itemId) + ");");
      }

      statement.close();
   }

   @Override
   public synchronized void setItemRestockTimes(Map<String, Long> restockTimes) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : restockTimes.keySet()) {
         statement.executeUpdate("REPLACE INTO itemstock VALUES('" + itemId + "', 0, '" + restockTimes.get(itemId) + "');");
      }

      statement.close();
   }

   @Override
   public synchronized int getItemStock(String itemId) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT amount FROM itemstock WHERE item = '" + itemId + "';");
      if (!resultSet.next()) {
         throw new SQLException();
      } else {
         int stock = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return stock;
      }
   }

   @Override
   public synchronized Map<String, Integer> getItemStocks(Collection<String> itemIds) throws SQLException {
      HashMap map = new HashMap();
      if (itemIds.isEmpty()) {
         return map;
      } else {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM itemstock WHERE item IN ('" + String.join("','", itemIds) + "');");

         while (resultSet.next()) {
            map.put(resultSet.getString("item"), resultSet.getInt("amount"));
         }

         resultSet.close();
         statement.close();
         return map;
      }
   }

   @Override
   public synchronized Map<String, Integer> getAllItemStocks() throws SQLException {
      HashMap itemIds = new HashMap();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM itemstock;");

      while (resultSet.next()) {
         itemIds.put(resultSet.getString("item"), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return itemIds;
   }

   @Override
   public synchronized Map<String, Integer> getOriginalStocks() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM original_stocks;");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   private synchronized void setOriginalStocks(Map<String, Integer> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (Entry entry : map.entrySet()) {
         statement.executeUpdate("REPLACE INTO original_stocks VALUES('" + (String)entry.getKey() + "', " + entry.getValue() + ");");
      }

      statement.close();
   }

   private synchronized Map<String, SimplePair<Integer, Long>> getGlobalStock() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM itemstock;");
      HashMap itemIds = new HashMap();

      while (resultSet.next()) {
         itemIds.put(resultSet.getString("item"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
      }

      resultSet.close();
      statement.close();
      return itemIds;
   }

   private synchronized void setGlobalStock(String itemId, SimplePair<Integer, Long> simplePair) throws SQLException {
      Statement statement = this.conn.createStatement();
      statement.executeUpdate("REPLACE INTO itemstock VALUES('" + itemId + "', " + simplePair.key + ", " + simplePair.value + ");");
      statement.close();
   }

   private synchronized Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerStock() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM playerstock;");
      HashMap<String, Map<String, SimplePair<Integer, Long>>> map = new HashMap<>();

      while (resultSet.next()) {
         String text = resultSet.getString("item");
         Map<String, SimplePair<Integer, Long>> orDefault = map.getOrDefault(text, new HashMap<>());
         orDefault.put(resultSet.getString("uuid"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
         map.put(text, orDefault);
      }

      resultSet.close();
      statement.close();
      return map;
   }

   private synchronized void setPlayerStock(String itemId, Map<String, SimplePair<Integer, Long>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (Entry entry : map.entrySet()) {
         statement.executeUpdate(
            "REPLACE INTO playerstock VALUES('"
               + itemId
               + "', '"
               + (String)entry.getKey()
               + "', "
               + ((SimplePair)entry.getValue()).key
               + ", "
               + ((SimplePair)entry.getValue()).value
               + ");"
         );
      }

      statement.close();
   }

   @Override
   public synchronized void executeQuery(SQLBatch.Type type, Query... types) {
      try {
         Statement statement = this.conn.createStatement();

         for (Query query : types) {
            statement.executeUpdate(query.format(this.getTable(type, query)));
         }

         statement.close();
      } catch (SQLException exception) {
         SendMessage.errorMessage("SQLite query batch failed: " + exception.getMessage());
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void executeBatch(SQLBatch.Type type, Set<Query> values) {
      try {
         Statement statement = this.conn.createStatement();

         for (Query query : values) {
            statement.executeUpdate(query.format(this.getTable(type, query)));
         }

         statement.close();
      } catch (SQLException exception) {
         SendMessage.errorMessage("SQLite write batch failed: " + exception.getMessage());
         exception.printStackTrace();
      }
   }

   private synchronized String getTable(SQLBatch.Type type, Query query) {
      switch (type) {
         case STOCK:
            if (query instanceof ItemStockQuery
               || query instanceof GlobalRestockTimerQuery
               || query instanceof RemoveStockQuery
               || query instanceof ClearStockQuery) {
               return "itemstock";
            } else if (query instanceof PlayerStockQuery
               || query instanceof UpdatePlayerStockQuery
               || query instanceof PlayerRestockTimerQuery
               || query instanceof RemovePlayerStockQuery
               || query instanceof RemovePlayerStocksQuery
               || query instanceof ClearPlayerStockQuery) {
               return "playerstock";
            } else if (query instanceof RestockScheduleQuery) {
               return "item_stock_schedules";
            }
         case SELL_LIMIT:
            if (query instanceof ItemStockQuery
               || query instanceof GlobalRestockTimerQuery
               || query instanceof RemoveStockQuery
               || query instanceof ClearStockQuery) {
               return "sellLimits";
            } else if (query instanceof PlayerStockQuery
               || query instanceof UpdatePlayerStockQuery
               || query instanceof PlayerRestockTimerQuery
               || query instanceof RemovePlayerStockQuery
               || query instanceof RemovePlayerStocksQuery
               || query instanceof ClearPlayerStockQuery) {
               return "playerSellLimits";
            } else if (query instanceof RestockScheduleQuery) {
               return "sell_limit_schedules";
            }
         default:
            return null;
      }
   }

   @Override
   public synchronized PlayerPriceModifier loadPlayerPriceModifiers(UUID playerId) throws SQLException {
      PreparedStatement statement = this.conn.prepareStatement("SELECT data FROM player_price_modifiers WHERE uuid=?");

      PlayerPriceModifier playerPriceModifier;
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

      return playerPriceModifier;
   }

   @Override
   public synchronized void savePlayerPriceModifiers(UUID playerId, PlayerPriceModifier playerPriceModifier) throws SQLException {
      if (playerPriceModifier.getSectionModifiers().isEmpty() && playerPriceModifier.getItemModifiers().isEmpty()) {
         PreparedStatement statement = this.conn.prepareStatement("DELETE FROM player_price_modifiers WHERE uuid = ?;");

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
      } else {
         PreparedStatement statement = this.conn.prepareStatement("REPLACE INTO player_price_modifiers VALUES(?,?);");

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
      }
   }

   @Override
   public synchronized Map<String, Long> getItemRestockTime() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM itemstock WHERE restock IS NOT NULL;");
      HashMap itemIds = new HashMap();

      while (resultSet.next()) {
         itemIds.put(resultSet.getString("item"), resultSet.getLong("restock"));
      }

      resultSet.close();
      statement.close();
      return itemIds;
   }

   @Override
   public synchronized void setPlayerStockAmount(UUID playerId, String itemId, Integer amount) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO playerstock(item,uuid,amount) VALUES('" + itemId + "', '" + playerId + "', " + amount + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerStockAmount(String itemId, Integer stock) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("UPDATE playerstock SET amount = " + stock + " WHERE item = '" + itemId + "';");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerStock(Map<String, Map<UUID, Integer>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : map.keySet()) {
         Map<UUID, Integer> stocks = map.get(itemId);

         for (UUID uniqueId : stocks.keySet()) {
            statement.executeUpdate("REPLACE INTO playerstock(item,uuid,amount) VALUES('" + itemId + "', '" + uniqueId + "', " + stocks.get(uniqueId) + ");");
         }
      }

      statement.close();
   }

   @Override
   public synchronized void setPlayerRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : map.keySet()) {
         Map<UUID, Long> restockTimes = map.get(itemId);

         for (UUID uniqueId : restockTimes.keySet()) {
            statement.executeUpdate("REPLACE INTO playerstock VALUES('" + itemId + "', '" + uniqueId + "', 0, '" + restockTimes.get(uniqueId) + "');");
         }
      }

      statement.close();
   }

   @Override
   public synchronized int getPlayerStockAmount(UUID playerId, String itemId) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT amount FROM playerstock WHERE item = '" + itemId + "' AND uuid = '" + playerId + "';");
      if (!resultSet.next()) {
         throw new SQLException();
      } else {
         int amount = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return amount;
      }
   }

   @Override
   public synchronized Map<String, Integer> getPlayerStockAmounts(Collection<String> itemIds, UUID uniqueId) throws SQLException {
      HashMap amounts = new HashMap();
      if (itemIds.isEmpty()) {
         return amounts;
      } else {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM playerstock WHERE item IN ('" + String.join("','", itemIds) + "') AND uuid = '" + uniqueId + "';");

         while (resultSet.next()) {
            amounts.put(resultSet.getString("item"), resultSet.getInt("amount"));
         }

         resultSet.close();
         statement.close();
         return amounts;
      }
   }

   @Override
   public synchronized Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException {
      HashMap<String, Map<UUID, Integer>> map = new HashMap<>();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,uuid,amount FROM playerstock;");

      while (resultSet.next()) {
         map.computeIfAbsent(resultSet.getString("item"), key -> new HashMap<>()).put(UUID.fromString(resultSet.getString("uuid")), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM playerstock WHERE restock IS NOT NULL;");
      HashMap<String, Map<UUID, Long>> map = new HashMap<>();

      while (resultSet.next()) {
         Statement statement2 = this.conn.createStatement();
         ResultSet resultSet2 = statement2.executeQuery("SELECT item,uuid,restock FROM playerstock WHERE restock IS NOT NULL AND item = '" + resultSet.getString("item") + "';");
         HashMap<UUID, Long> map2 = new HashMap<>();

         while (resultSet2.next()) {
            map2.put(UUID.fromString(resultSet2.getString("uuid")), resultSet2.getLong("restock"));
         }

         resultSet2.close();
         statement2.close();
         map.put(resultSet.getString("item"), map2);
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized Map<String, Integer> getPlayersStock(String itemId) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT uuid,amount FROM playerstock WHERE item = '" + itemId + "';");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         String text = resultSet.getString("uuid");
         map.put(text, resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized boolean resetDynamicPricing() {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("DROP TABLE dynamicStock;");
         statement.executeUpdate("DROP TABLE maxStocks;");
         statement.close();
         this.createTables();
         return true;
      } catch (SQLException exception) {
         SendMessage.warnMessage("Failed to delete table from database");
         exception.printStackTrace();
         return false;
      }
   }

   @Override
   public synchronized int getOriginalLimit(String text) {
      try {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT * FROM original_limits WHERE item = '" + text + "';");
         int limit = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return limit;
      } catch (SQLException exception) {
         return 0;
      }
   }

   @Override
   public synchronized void setOriginalLimit(String text, int limit) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO original_limits VALUES('" + text + "'," + limit + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setSellLimit(String text, int limit) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO sellLimits(item, amount) VALUES('" + text + "', '" + limit + "');");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setSellLimit(Map<String, Integer> limits) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String key : limits.keySet()) {
         statement.executeUpdate("REPLACE INTO sellLimits(item, amount) VALUES('" + key + "', " + limits.get(key) + ");");
      }

      statement.close();
   }

   @Override
   public synchronized void setSellLimitRestockTimes(Map<String, Long> limits) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String key : limits.keySet()) {
         statement.executeUpdate("REPLACE INTO sellLimits VALUES('" + key + "', 0, '" + limits.get(key) + "');");
      }

      statement.close();
   }

   @Override
   public synchronized int getSellLimit(String text) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT amount FROM sellLimits WHERE item = '" + text + "';");
      if (!resultSet.next()) {
         throw new SQLException();
      } else {
         int limit = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return limit;
      }
   }

   @Override
   public synchronized Map<String, Integer> getSellLimits(Collection<String> values) throws SQLException {
      HashMap map = new HashMap();
      if (values.isEmpty()) {
         return map;
      } else {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM sellLimits WHERE item IN ('" + String.join("','", values) + "');");

         while (resultSet.next()) {
            map.put(resultSet.getString("item"), resultSet.getInt("amount"));
         }

         resultSet.close();
         statement.close();
         return map;
      }
   }

   @Override
   public synchronized Map<String, Integer> getAllSellLimits() throws SQLException {
      HashMap map = new HashMap();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,amount FROM sellLimits;");

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized Map<String, Long> getSellLimitRestockTimes() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM sellLimits WHERE restock IS NOT NULL;");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), resultSet.getLong("restock"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized void setPlayerSellLimit(UUID playerId, String text, Integer limit) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("REPLACE INTO playerSellLimits(item,uuid,amount) VALUES('" + text + "', '" + playerId + "', " + limit + ");");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerSellLimit(String itemId, Integer limit) {
      try {
         Statement statement = this.conn.createStatement();
         statement.executeUpdate("UPDATE playerSellLimits SET amount = " + limit + " WHERE item = '" + itemId + "';");
         statement.close();
      } catch (SQLException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerSellLimit(Map<String, Map<UUID, Integer>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : map.keySet()) {
         Map<UUID, Integer> limits = map.get(itemId);

         for (UUID uniqueId : limits.keySet()) {
            statement.executeUpdate("REPLACE INTO playerSellLimits(item,uuid,amount) VALUES('" + itemId + "', '" + uniqueId + "', " + limits.get(uniqueId) + ");");
         }
      }

      statement.close();
   }

   @Override
   public synchronized void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (String itemId : map.keySet()) {
         Map<UUID, Long> limits = map.get(itemId);

         for (UUID uniqueId : limits.keySet()) {
            statement.executeUpdate("REPLACE INTO playerSellLimits VALUES('" + itemId + "', '" + uniqueId + "', 0, '" + limits.get(uniqueId) + "');");
         }
      }

      statement.close();
   }

   @Override
   public synchronized int getPlayerSellLimit(UUID playerId, String text) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT amount FROM playerSellLimits WHERE item = '" + text + "' AND uuid = '" + playerId + "';");
      if (!resultSet.next()) {
         throw new SQLException();
      } else {
         int limit = resultSet.getInt("amount");
         resultSet.close();
         statement.close();
         return limit;
      }
   }

   @Override
   public synchronized Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> itemIds, UUID uniqueId) throws SQLException {
      HashMap map = new HashMap();
      if (itemIds.isEmpty()) {
         return map;
      } else {
         Statement statement = this.conn.createStatement();
         ResultSet resultSet = statement.executeQuery(
            "SELECT item,amount FROM playerSellLimits WHERE item IN ('" + String.join("','", itemIds) + "') AND uuid = '" + uniqueId + "';"
         );

         while (resultSet.next()) {
            map.put(resultSet.getString("item"), resultSet.getInt("amount"));
         }

         resultSet.close();
         statement.close();
         return map;
      }
   }

   @Override
   public synchronized Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,restock FROM playerSellLimits WHERE restock IS NOT NULL;");
      HashMap itemPaths = new HashMap();

      while (resultSet.next()) {
         Statement statement2 = this.conn.createStatement();
         ResultSet resultSet2 = statement2.executeQuery(
            "SELECT item,uuid,restock FROM playerSellLimits WHERE restock IS NOT NULL AND item = '" + resultSet.getString("item") + "';"
         );
         HashMap map = new HashMap();

         while (resultSet2.next()) {
            map.put(UUID.fromString(resultSet2.getString("uuid")), resultSet2.getLong("restock"));
         }

         resultSet2.close();
         statement2.close();
         itemPaths.put(resultSet.getString("item"), map);
      }

      resultSet.close();
      statement.close();
      return itemPaths;
   }

   @Override
   public synchronized Map<String, Integer> getPlayerSellLimits(String itemId) throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT uuid,amount FROM playerSellLimits WHERE item = '" + itemId + "';");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         String text = resultSet.getString("uuid");
         map.put(text, resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException {
      HashMap<String, Map<UUID, Integer>> map = new HashMap<>();
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT item,uuid,amount FROM playerSellLimits;");

      while (resultSet.next()) {
         map.computeIfAbsent(resultSet.getString("item"), key -> new HashMap<>()).put(UUID.fromString(resultSet.getString("uuid")), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized void setItemStockSchedulers(Map<ScheduleManager.Item, Long> stocks) throws SQLException {
      PreparedStatement statement = this.conn.prepareStatement("REPLACE INTO item_stock_schedules VALUES(?,?,?);");

      for (Entry entry : stocks.entrySet()) {
         statement.setString(1, ((ScheduleManager.Item)entry.getKey()).getItemPath());
         statement.setInt(2, ((ScheduleManager.Item)entry.getKey()).mode);
         statement.setString(3, ((Long)entry.getValue()).toString());
         statement.execute();
      }
   }

   @Override
   public synchronized Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM item_stock_schedules;");
      HashMap itemIds = new HashMap();

      while (resultSet.next()) {
         itemIds.put(new ScheduleManager.Item(resultSet.getString("item"), resultSet.getInt("mode")), Long.parseLong(resultSet.getString("restock")));
      }

      resultSet.close();
      statement.close();
      return itemIds;
   }

   @Override
   public synchronized void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> limits) throws SQLException {
      PreparedStatement statement = this.conn.prepareStatement("REPLACE INTO sell_limit_schedules VALUES(?,?,?);");

      for (Entry entry : limits.entrySet()) {
         statement.setString(1, ((ScheduleManager.Item)entry.getKey()).getItemPath());
         statement.setInt(2, ((ScheduleManager.Item)entry.getKey()).mode);
         statement.setString(3, ((Long)entry.getValue()).toString());
         statement.execute();
      }
   }

   @Override
   public synchronized Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM sell_limit_schedules;");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         map.put(new ScheduleManager.Item(resultSet.getString("item"), resultSet.getInt("mode")), Long.parseLong(resultSet.getString("restock")));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   @Override
   public synchronized Map<String, Integer> getOriginalLimits() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM original_limits;");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), resultSet.getInt("amount"));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   private synchronized Map<String, SimplePair<Integer, Long>> getGlobalSellLimits() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM sellLimits;");
      HashMap map = new HashMap();

      while (resultSet.next()) {
         map.put(resultSet.getString("item"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
      }

      resultSet.close();
      statement.close();
      return map;
   }

   private synchronized void setGlobalLimit(String text, SimplePair<Integer, Long> simplePair) throws SQLException {
      Statement statement = this.conn.createStatement();
      statement.executeUpdate("REPLACE INTO sellLimits VALUES('" + text + "', " + simplePair.key + ", " + simplePair.value + ");");
      statement.close();
   }

   private synchronized Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerSellLimits() throws SQLException {
      Statement statement = this.conn.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT * FROM playerSellLimits;");
      HashMap<String, Map<String, SimplePair<Integer, Long>>> map = new HashMap<>();

      while (resultSet.next()) {
         String text = resultSet.getString("item");
         Map<String, SimplePair<Integer, Long>> orDefault = map.getOrDefault(text, new HashMap<>());
         orDefault.put(resultSet.getString("uuid"), new SimplePair<>(resultSet.getInt("amount"), resultSet.getLong("restock")));
         map.put(text, orDefault);
      }

      resultSet.close();
      statement.close();
      return map;
   }

   private synchronized void setPlayerLimit(String itemId, Map<String, SimplePair<Integer, Long>> map) throws SQLException {
      Statement statement = this.conn.createStatement();

      for (Entry entry : map.entrySet()) {
         statement.executeUpdate(
            "REPLACE INTO playerSellLimits VALUES('"
               + itemId
               + "', '"
               + (String)entry.getKey()
               + "', "
               + ((SimplePair)entry.getValue()).key
               + ", "
               + ((SimplePair)entry.getValue()).value
               + ");"
         );
      }

      statement.close();
   }

   @Override
   public synchronized void setRotatingShopItems(String shopId, List<String> values, long value) {
   }

   @Override
   public synchronized SimplePair<List<String>, Long> getRotatingShopItems(String shopId) {
      return null;
   }
}
