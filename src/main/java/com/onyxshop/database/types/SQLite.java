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
      } catch (Exception var2) {
         SendMessage.errorMessage(Lang.ERROR_OCCURRED_WHILE_CONNECTING_TO_DATABASE.get());
         var2.printStackTrace();
         return false;
      }
   }

   private synchronized void ensureColumn(String var1, String var2, String var3) {
      try {
         ResultSet var4 = this.conn.createStatement().executeQuery("PRAGMA table_info(" + var1 + ");");

         try {
            while (var4.next()) {
               if (var2.equalsIgnoreCase(var4.getString("name"))) {
                  return;
               }
            }
         } finally {
            var4.close();
         }
      } catch (SQLException var6) {
      }

      try {
         this.conn.createStatement().executeUpdate("ALTER TABLE " + var1 + " ADD COLUMN " + var2 + " " + var3 + ";");
      } catch (SQLException var5) {
         SendMessage.warnMessage("Could not add " + var1 + "." + var2 + ": " + var5.getMessage());
      }
   }

   private synchronized void migrate() {
      this.ensureColumn("playerstock", "restock", "TEXT");
      this.ensureColumn("itemstock", "restock", "TEXT");
      this.ensureColumn("playerSellLimits", "restock", "TEXT");
      this.ensureColumn("sellLimits", "restock", "TEXT");
   }

   @Override
   public synchronized int removeExpiredPlayerData(long var1) throws SQLException {
      HashSet<String> var3 = new HashSet<>();
      Statement var4 = this.conn.createStatement();
      ResultSet var5 = var4.executeQuery("SELECT DISTINCT uuid FROM playerstock;");
      int var6 = 0;
      long var7 = new Date().getTime() - var1;
      SendMessage.logDebugMessage("Checking " + var5.getFetchSize() + " player stock entries...");

      while (var5.next()) {
         String var9 = var5.getString("uuid");
         if (Bukkit.getOfflinePlayer(UUID.fromString(var9)).getLastPlayed() < var7) {
            var3.add(var9);
         } else {
            var6++;
         }
      }

      var5 = var4.executeQuery("SELECT DISTINCT uuid FROM playerSellLimits;");
      SendMessage.logDebugMessage("Checking " + var5.getFetchSize() + " player sell limit entries...");

      while (var5.next()) {
         String var14 = var5.getString("uuid");
         if (Bukkit.getOfflinePlayer(UUID.fromString(var14)).getLastPlayed() < var7) {
            var3.add(var14);
         } else {
            var6++;
         }
      }

      SendMessage.logDebugMessage("Found recent database entries for " + var6 + " players, skipping...");
      SendMessage.logDebugMessage("Deleting expired entries...");
      var4 = this.conn.createStatement();

      for (String var10 : var3) {
         var4.executeUpdate("DELETE FROM playerstock WHERE uuid = '" + var10 + "';");
         var4.executeUpdate("DELETE FROM playerSellLimits WHERE uuid = '" + var10 + "';");
      }

      SendMessage.logDebugMessage("Deleted " + var3.size() + " expired unique player entries from database, vacuuming...");
      var4 = this.conn.createStatement();
      var4.execute("VACUUM");
      SendMessage.logDebugMessage("Vacuum completed");
      var5.close();
      var4.close();
      return var3.size();
   }

   @Override
   public synchronized Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException {
      Statement var1 = this.conn.createStatement();
      SendMessage.logDebugMessage("Retrieving item indexes from database...");
      HashMap<String, Set<String>> var3 = new HashMap<>();

      for (String var7 : new String[]{
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
         ResultSet var2 = var1.executeQuery("SELECT DISTINCT item FROM " + var7 + ";");

         while (var2.next()) {
            var3.computeIfAbsent(var2.getString("item"), var0 -> new HashSet<>()).add(var7);
         }
      }

      HashMap<String, Map<String, Set<String>>> var11 = new HashMap<>();

      for (Entry<String, Set<String>> var14 : var3.entrySet()) {
         var11.computeIfAbsent(var14.getKey().split("\\.")[0], var0 -> new HashMap<>())
            .put(var14.getKey().split("\\.", 2)[1], var14.getValue());
      }

      SendMessage.logDebugMessage("Checking if items still exist...");

      for (String var15 : var11.keySet()) {
         if (ConfigManager.getShops().contains(var15)) {
            List<String> var16 = OnyxShop.getInstance().getConfigManager().getItemsRaw(var15);
            HashMap<String, Set<String>> var8 = new HashMap<>();

            for (String var10 : var11.get(var15).keySet()) {
               if (!var16.contains(var10)) {
                  var8.put(var10, var11.get(var15).get(var10));
               }
            }

            var11.put(var15, var8);
         }
      }

      return var11;
   }

   @Override
   public synchronized void removeExpiredItemData(Map<String, Map<String, Set<String>>> var1) throws SQLException {
      SendMessage.logDebugMessage("Removing loaded expired item data from database...");
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         List<String> var5 = new ArrayList<>();
         if (ConfigManager.getShops().contains(var4)) {
            var5 = OnyxShop.getInstance().getConfigManager().getItemsRaw(var4);
         }

         for (Entry<String, Set<String>> var7 : var1.get(var4).entrySet()) {
            if (!var5.contains(var7.getKey())) {
               SendMessage.logDebugMessage(
                  "Removing data for item '"
                     + var4
                     + "."
                     + (String)var7.getKey()
                     + "' from "
                     + Arrays.toString(((Set)var7.getValue()).toArray())
                     + " tables..."
               );

               for (String var9 : var7.getValue()) {
                  var2.executeUpdate("DELETE FROM " + var9 + " WHERE item = '" + var4 + "." + (String)var7.getKey() + "';");
               }
            }
         }
      }

      SendMessage.logDebugMessage(
         "Successfully removed "
            + var1.entrySet()
               .stream()
               .flatMap(var0 -> ((Map)var0.getValue()).keySet().stream().map(var1x -> (String)var0.getKey() + "." + var1x))
               .distinct()
               .count()
            + " non existing item entries from database"
      );
      SendMessage.logDebugMessage("Resizing file...");
      var2 = this.conn.createStatement();
      var2.execute("VACUUM");
      var2.close();
      SendMessage.logDebugMessage("Vacuum completed, database successfully cleaned from expired item entries");
   }

   public synchronized void migrateV4toV5() throws SQLException {
      long var1 = System.currentTimeMillis();
      OnyxShop var3 = OnyxShop.getInstance();
      HashMap<String, String> var4 = new HashMap<>();
      SendMessage.infoMessage("Loading original stocks from database...");
      Map<String, Integer> var5 = this.getOriginalStocks();
      SendMessage.infoMessage("Loading global stock from database...");
      Map<String, SimplePair<Integer, Long>> var6 = this.getGlobalStock();
      SendMessage.infoMessage("Loading player stock from database...");
      Map<String, Map<String, SimplePair<Integer, Long>>> var7 = this.getPlayerStock();
      SendMessage.infoMessage("Loading original sell limits from database...");
      Map<String, Integer> var8 = this.getOriginalLimits();
      SendMessage.infoMessage("Loading global sell limits from database...");
      Map<String, SimplePair<Integer, Long>> var9 = this.getGlobalSellLimits();
      SendMessage.infoMessage("Loading player sell limits from database...");
      Map<String, Map<String, SimplePair<Integer, Long>>> var10 = this.getPlayerSellLimits();
      SendMessage.infoMessage("Started migration of loaded data...");
      Statement var11 = this.conn.createStatement();
      var11.executeUpdate("DROP TABLE dynamicStock;");
      var11.executeUpdate("DROP TABLE maxStocks;");
      var3.getLimitedStockProvider().emptyCache();
      var11.executeUpdate("DROP TABLE original_stocks;");
      var11.executeUpdate("DROP TABLE playerstock;");
      var11.executeUpdate("DROP TABLE itemstock;");
      var3.getLimitedSellProvider().emptyCache();
      var11.executeUpdate("DROP TABLE original_limits;");
      var11.executeUpdate("DROP TABLE playerSellLimits;");
      var11.executeUpdate("DROP TABLE sellLimits;");
      var11.close();
      this.createTables();

      for (Entry<String, Integer> var13 : var5.entrySet()) {
         String var14 = this.getNewItem(var4, var3, (String)var13.getKey());
         if (var14 != null) {
            this.setOriginalStock(var14, (Integer)var13.getValue());
         }
      }

      for (Entry<String, SimplePair<Integer, Long>> var20 : var6.entrySet()) {
         String var25 = this.getNewItem(var4, var3, (String)var20.getKey());
         if (var25 != null) {
            this.setGlobalStock(var25, (SimplePair<Integer, Long>)var20.getValue());
         }
      }

      for (Entry<String, Map<String, SimplePair<Integer, Long>>> var21 : var7.entrySet()) {
         String var26 = this.getNewItem(var4, var3, (String)var21.getKey());
         if (var26 != null) {
            this.setPlayerStock(var26, (Map<String, SimplePair<Integer, Long>>)var21.getValue());
         }
      }

      for (Entry<String, Integer> var22 : var8.entrySet()) {
         String var27 = this.getNewItem(var4, var3, (String)var22.getKey());
         if (var27 != null) {
            this.setOriginalLimit(var27, (Integer)var22.getValue());
         }
      }

      for (Entry<String, SimplePair<Integer, Long>> var23 : var9.entrySet()) {
         String var28 = this.getNewItem(var4, var3, (String)var23.getKey());
         if (var28 != null) {
            this.setGlobalLimit(var28, (SimplePair<Integer, Long>)var23.getValue());
         }
      }

      for (Entry<String, Map<String, SimplePair<Integer, Long>>> var24 : var10.entrySet()) {
         String var29 = this.getNewItem(var4, var3, (String)var24.getKey());
         if (var29 != null) {
            this.setPlayerLimit(var29, (Map<String, SimplePair<Integer, Long>>)var24.getValue());
         }
      }

      SendMessage.infoMessage("Successfully migrated loaded data into the database in " + (System.currentTimeMillis() - var1) + "ms");
   }

   private synchronized String getNewItem(Map<String, String> var1, OnyxShop var2, String var3) {
      String var4 = (String)var1.get(var3);
      if (var4 != null) {
         return var4;
      } else {
         String[] var5 = var3.split("\\.");
         if (var5.length != 2) {
            if (var5.length == 4) {
               return null;
            } else {
               SendMessage.logDebugMessage("Failed to migrate item " + var3 + " to new format, skipping...");
               return null;
            }
         } else {
            String var6 = var5[0];
            String var7 = var5[1];
            var4 = var2.getConfigManager().getItemsRaw(var6).stream().filter(var1x -> var1x.split("\\.")[2].equalsIgnoreCase(var7)).findFirst().orElse(null);
            var4 = var6 + "." + var4;
            var1.put(var3, var4);
            return var4;
         }
      }
   }

   @Override
   public synchronized void closeConnection() {
      try {
         this.conn.close();
      } catch (SQLException var2) {
         SendMessage.errorMessage("A error occurred while closing the connection with the database");
         var2.printStackTrace();
      }
   }

   private synchronized void createTables() {
      try {
         Statement var1 = this.conn.createStatement();
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS dynamicStock (item TEXT NOT NULL UNIQUE,stock INTEGER NOT NULL);");
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS maxStocks (item TEXT NOT NULL UNIQUE,maxStock INTEGER NOT NULL);");
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS original_stocks (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL);");
         var1.executeUpdate(
            "CREATE TABLE IF NOT EXISTS playerstock (item TEXT NOT NULL,uuid TEXT NOT NULL,amount INTEGER NOT NULL,restock TEXT,UNIQUE(item,uuid));"
         );
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS itemstock (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL,restock TEXT);");
         var1.executeUpdate(
            "CREATE TABLE IF NOT EXISTS item_stock_schedules(item TEXT NOT NULL,mode INTEGER NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
         );
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS original_limits (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL);");
         var1.executeUpdate(
            "CREATE TABLE IF NOT EXISTS playerSellLimits (item TEXT NOT NULL,uuid TEXT NOT NULL,amount INTEGER NOT NULL,restock TEXT,UNIQUE(item,uuid));"
         );
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS sellLimits (item TEXT NOT NULL UNIQUE,amount INTEGER NOT NULL,restock TEXT);");
         var1.executeUpdate(
            "CREATE TABLE IF NOT EXISTS sell_limit_schedules(item TEXT NOT NULL,mode INTEGER NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
         );
         var1.executeUpdate("CREATE TABLE IF NOT EXISTS player_price_modifiers (uuid TEXT NOT NULL UNIQUE, data TEXT NOT NULL);");
         var1.close();
      } catch (SQLException var2) {
         var2.printStackTrace();
      }
   }

   @Override
   public synchronized Map<String, Integer> loadDynamicPricingStock() throws SQLException {
      HashMap var1 = new HashMap();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT * FROM dynamicStock;");

      while (var3.next()) {
         var1.put(var3.getString("item"), var3.getInt("stock"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized Integer getDynamicStock(String var1) {
      int var2;
      try {
         Statement var3 = this.conn.createStatement();
         ResultSet var4 = var3.executeQuery("SELECT stock FROM dynamicStock WHERE item = '" + var1 + "';");
         var2 = var4.getInt("stock");
         var4.close();
         var3.close();
      } catch (SQLException var5) {
         return null;
      }

      return var2;
   }

   @Override
   public synchronized Map<String, Integer> getDynamicStocks(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Statement var3 = this.conn.createStatement();
         ResultSet var4 = var3.executeQuery("SELECT item,stock FROM dynamicStock WHERE item IN ('" + String.join("','", var1) + "');");

         while (var4.next()) {
            var2.put(var4.getString("item"), var4.getInt("stock"));
         }

         var4.close();
         var3.close();
         return var2;
      }
   }

   @Override
   public synchronized Map<String, Integer> loadDynamicMaxStocks() throws SQLException {
      HashMap var1 = new HashMap();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT * FROM maxStocks;");

      while (var3.next()) {
         var1.put(var3.getString("item"), var3.getInt("maxStock"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized void setDynamicMaxStock(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO maxStocks VALUES('" + var1 + "'," + var2 + ");");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void saveDynamicStock(Map<String, Integer> var1) {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("REPLACE INTO dynamicStock VALUES(?,?);");

         try {
            for (Entry var4 : var1.entrySet()) {
               var2.setString(1, (String)var4.getKey());
               var2.setInt(2, (Integer)var4.getValue());
               var2.execute();
            }
         } catch (Throwable var6) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (var2 != null) {
            var2.close();
         }
      } catch (SQLException var7) {
         var7.printStackTrace();
      }
   }

   @Override
   public synchronized void setDynamicStock(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO dynamicStock VALUES('" + var1 + "'," + var2 + ");");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized int getOriginalStock(String var1) {
      try {
         Statement var2 = this.conn.createStatement();
         ResultSet var3 = var2.executeQuery("SELECT * FROM original_stocks WHERE item = '" + var1 + "';");
         int var4 = var3.getInt("amount");
         var3.close();
         var2.close();
         return var4;
      } catch (SQLException var5) {
         return 0;
      }
   }

   @Override
   public synchronized void setOriginalStock(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO original_stocks VALUES('" + var1 + "'," + var2 + ");");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setItemStock(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO itemstock(item, amount) VALUES('" + var1 + "', '" + var2 + "');");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setItemStock(Map<String, Integer> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         var2.executeUpdate("REPLACE INTO itemstock(item, amount) VALUES('" + var4 + "', " + var1.get(var4) + ");");
      }

      var2.close();
   }

   @Override
   public synchronized void setItemRestockTimes(Map<String, Long> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         var2.executeUpdate("REPLACE INTO itemstock VALUES('" + var4 + "', 0, '" + var1.get(var4) + "');");
      }

      var2.close();
   }

   @Override
   public synchronized int getItemStock(String var1) throws SQLException {
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT amount FROM itemstock WHERE item = '" + var1 + "';");
      if (!var3.next()) {
         throw new SQLException();
      } else {
         int var4 = var3.getInt("amount");
         var3.close();
         var2.close();
         return var4;
      }
   }

   @Override
   public synchronized Map<String, Integer> getItemStocks(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Statement var3 = this.conn.createStatement();
         ResultSet var4 = var3.executeQuery("SELECT item,amount FROM itemstock WHERE item IN ('" + String.join("','", var1) + "');");

         while (var4.next()) {
            var2.put(var4.getString("item"), var4.getInt("amount"));
         }

         var4.close();
         var3.close();
         return var2;
      }
   }

   @Override
   public synchronized Map<String, Integer> getAllItemStocks() throws SQLException {
      HashMap var1 = new HashMap();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT item,amount FROM itemstock;");

      while (var3.next()) {
         var1.put(var3.getString("item"), var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized Map<String, Integer> getOriginalStocks() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM original_stocks;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), var2.getInt("amount"));
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized void setOriginalStocks(Map<String, Integer> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (Entry var4 : var1.entrySet()) {
         var2.executeUpdate("REPLACE INTO original_stocks VALUES('" + (String)var4.getKey() + "', " + var4.getValue() + ");");
      }

      var2.close();
   }

   private synchronized Map<String, SimplePair<Integer, Long>> getGlobalStock() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM itemstock;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), new SimplePair<>(var2.getInt("amount"), var2.getLong("restock")));
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized void setGlobalStock(String var1, SimplePair<Integer, Long> var2) throws SQLException {
      Statement var3 = this.conn.createStatement();
      var3.executeUpdate("REPLACE INTO itemstock VALUES('" + var1 + "', " + var2.key + ", " + var2.value + ");");
      var3.close();
   }

   private synchronized Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerStock() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM playerstock;");
      HashMap<String, Map<String, SimplePair<Integer, Long>>> var3 = new HashMap<>();

      while (var2.next()) {
         String var4 = var2.getString("item");
         Map<String, SimplePair<Integer, Long>> var5 = var3.getOrDefault(var4, new HashMap<>());
         var5.put(var2.getString("uuid"), new SimplePair<>(var2.getInt("amount"), var2.getLong("restock")));
         var3.put(var4, var5);
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized void setPlayerStock(String var1, Map<String, SimplePair<Integer, Long>> var2) throws SQLException {
      Statement var3 = this.conn.createStatement();

      for (Entry var5 : var2.entrySet()) {
         var3.executeUpdate(
            "REPLACE INTO playerstock VALUES('"
               + var1
               + "', '"
               + (String)var5.getKey()
               + "', "
               + ((SimplePair)var5.getValue()).key
               + ", "
               + ((SimplePair)var5.getValue()).value
               + ");"
         );
      }

      var3.close();
   }

   @Override
   public synchronized void executeQuery(SQLBatch.Type var1, Query... var2) {
      try {
         Statement var3 = this.conn.createStatement();

         for (Query var7 : var2) {
            var3.executeUpdate(var7.format(this.getTable(var1, var7)));
         }

         var3.close();
      } catch (SQLException var8) {
         SendMessage.errorMessage("SQLite query batch failed: " + var8.getMessage());
         var8.printStackTrace();
      }
   }

   @Override
   public synchronized void executeBatch(SQLBatch.Type var1, Set<Query> var2) {
      try {
         Statement var3 = this.conn.createStatement();

         for (Query var5 : var2) {
            var3.executeUpdate(var5.format(this.getTable(var1, var5)));
         }

         var3.close();
      } catch (SQLException var6) {
         SendMessage.errorMessage("SQLite write batch failed: " + var6.getMessage());
         var6.printStackTrace();
      }
   }

   private synchronized String getTable(SQLBatch.Type var1, Query var2) {
      switch (var1) {
         case STOCK:
            if (var2 instanceof ItemStockQuery
               || var2 instanceof GlobalRestockTimerQuery
               || var2 instanceof RemoveStockQuery
               || var2 instanceof ClearStockQuery) {
               return "itemstock";
            } else if (var2 instanceof PlayerStockQuery
               || var2 instanceof UpdatePlayerStockQuery
               || var2 instanceof PlayerRestockTimerQuery
               || var2 instanceof RemovePlayerStockQuery
               || var2 instanceof RemovePlayerStocksQuery
               || var2 instanceof ClearPlayerStockQuery) {
               return "playerstock";
            } else if (var2 instanceof RestockScheduleQuery) {
               return "item_stock_schedules";
            }
         case SELL_LIMIT:
            if (var2 instanceof ItemStockQuery
               || var2 instanceof GlobalRestockTimerQuery
               || var2 instanceof RemoveStockQuery
               || var2 instanceof ClearStockQuery) {
               return "sellLimits";
            } else if (var2 instanceof PlayerStockQuery
               || var2 instanceof UpdatePlayerStockQuery
               || var2 instanceof PlayerRestockTimerQuery
               || var2 instanceof RemovePlayerStockQuery
               || var2 instanceof RemovePlayerStocksQuery
               || var2 instanceof ClearPlayerStockQuery) {
               return "playerSellLimits";
            } else if (var2 instanceof RestockScheduleQuery) {
               return "sell_limit_schedules";
            }
         default:
            return null;
      }
   }

   @Override
   public synchronized PlayerPriceModifier loadPlayerPriceModifiers(UUID var1) throws SQLException {
      PreparedStatement var2 = this.conn.prepareStatement("SELECT data FROM player_price_modifiers WHERE uuid=?");

      PlayerPriceModifier var4;
      try {
         var2.setString(1, var1.toString());
         ResultSet var3 = var2.executeQuery();

         try {
            var4 = var3.next() ? (PlayerPriceModifier)new Gson().fromJson(var3.getString(1), PlayerPriceModifier.class) : null;
         } catch (Throwable var8) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var9) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }
         }

         throw var9;
      }

      if (var2 != null) {
         var2.close();
      }

      return var4;
   }

   @Override
   public synchronized void savePlayerPriceModifiers(UUID var1, PlayerPriceModifier var2) throws SQLException {
      if (var2.getSectionModifiers().isEmpty() && var2.getItemModifiers().isEmpty()) {
         PreparedStatement var10 = this.conn.prepareStatement("DELETE FROM player_price_modifiers WHERE uuid = ?;");

         try {
            var10.setString(1, var1.toString());
            var10.execute();
         } catch (Throwable var9) {
            if (var10 != null) {
               try {
                  var10.close();
               } catch (Throwable var7) {
                  var9.addSuppressed(var7);
               }
            }

            throw var9;
         }

         if (var10 != null) {
            var10.close();
         }
      } else {
         PreparedStatement var3 = this.conn.prepareStatement("REPLACE INTO player_price_modifiers VALUES(?,?);");

         try {
            var3.setString(1, var1.toString());
            var3.setString(2, new Gson().toJson(var2));
            var3.execute();
         } catch (Throwable var8) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var8.addSuppressed(var6);
               }
            }

            throw var8;
         }

         if (var3 != null) {
            var3.close();
         }
      }
   }

   @Override
   public synchronized Map<String, Long> getItemRestockTime() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT item,restock FROM itemstock WHERE restock IS NOT NULL;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), var2.getLong("restock"));
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized void setPlayerStockAmount(UUID var1, String var2, Integer var3) {
      try {
         Statement var4 = this.conn.createStatement();
         var4.executeUpdate("REPLACE INTO playerstock(item,uuid,amount) VALUES('" + var2 + "', '" + var1 + "', " + var3 + ");");
         var4.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerStockAmount(String var1, Integer var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("UPDATE playerstock SET amount = " + var2 + " WHERE item = '" + var1 + "';");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerStock(Map<String, Map<UUID, Integer>> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         Map<UUID, Integer> var5 = var1.get(var4);

         for (UUID var7 : var5.keySet()) {
            var2.executeUpdate("REPLACE INTO playerstock(item,uuid,amount) VALUES('" + var4 + "', '" + var7 + "', " + var5.get(var7) + ");");
         }
      }

      var2.close();
   }

   @Override
   public synchronized void setPlayerRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         Map<UUID, Long> var5 = var1.get(var4);

         for (UUID var7 : var5.keySet()) {
            var2.executeUpdate("REPLACE INTO playerstock VALUES('" + var4 + "', '" + var7 + "', 0, '" + var5.get(var7) + "');");
         }
      }

      var2.close();
   }

   @Override
   public synchronized int getPlayerStockAmount(UUID var1, String var2) throws SQLException {
      Statement var3 = this.conn.createStatement();
      ResultSet var4 = var3.executeQuery("SELECT amount FROM playerstock WHERE item = '" + var2 + "' AND uuid = '" + var1 + "';");
      if (!var4.next()) {
         throw new SQLException();
      } else {
         int var5 = var4.getInt("amount");
         var4.close();
         var3.close();
         return var5;
      }
   }

   @Override
   public synchronized Map<String, Integer> getPlayerStockAmounts(Collection<String> var1, UUID var2) throws SQLException {
      HashMap var3 = new HashMap();
      if (var1.isEmpty()) {
         return var3;
      } else {
         Statement var4 = this.conn.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT item,amount FROM playerstock WHERE item IN ('" + String.join("','", var1) + "') AND uuid = '" + var2 + "';");

         while (var5.next()) {
            var3.put(var5.getString("item"), var5.getInt("amount"));
         }

         var5.close();
         var4.close();
         return var3;
      }
   }

   @Override
   public synchronized Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException {
      HashMap<String, Map<UUID, Integer>> var1 = new HashMap<>();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT item,uuid,amount FROM playerstock;");

      while (var3.next()) {
         var1.computeIfAbsent(var3.getString("item"), var0 -> new HashMap<>()).put(UUID.fromString(var3.getString("uuid")), var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT item,restock FROM playerstock WHERE restock IS NOT NULL;");
      HashMap<String, Map<UUID, Long>> var3 = new HashMap<>();

      while (var2.next()) {
         Statement var4 = this.conn.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT item,uuid,restock FROM playerstock WHERE restock IS NOT NULL AND item = '" + var2.getString("item") + "';");
         HashMap<UUID, Long> var6 = new HashMap<>();

         while (var5.next()) {
            var6.put(UUID.fromString(var5.getString("uuid")), var5.getLong("restock"));
         }

         var5.close();
         var4.close();
         var3.put(var2.getString("item"), var6);
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized Map<String, Integer> getPlayersStock(String var1) throws SQLException {
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT uuid,amount FROM playerstock WHERE item = '" + var1 + "';");
      HashMap var4 = new HashMap();

      while (var3.next()) {
         String var5 = var3.getString("uuid");
         var4.put(var5, var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var4;
   }

   @Override
   public synchronized boolean resetDynamicPricing() {
      try {
         Statement var1 = this.conn.createStatement();
         var1.executeUpdate("DROP TABLE dynamicStock;");
         var1.executeUpdate("DROP TABLE maxStocks;");
         var1.close();
         this.createTables();
         return true;
      } catch (SQLException var2) {
         SendMessage.warnMessage("Failed to delete table from database");
         var2.printStackTrace();
         return false;
      }
   }

   @Override
   public synchronized int getOriginalLimit(String var1) {
      try {
         Statement var2 = this.conn.createStatement();
         ResultSet var3 = var2.executeQuery("SELECT * FROM original_limits WHERE item = '" + var1 + "';");
         int var4 = var3.getInt("amount");
         var3.close();
         var2.close();
         return var4;
      } catch (SQLException var5) {
         return 0;
      }
   }

   @Override
   public synchronized void setOriginalLimit(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO original_limits VALUES('" + var1 + "'," + var2 + ");");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setSellLimit(String var1, int var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("REPLACE INTO sellLimits(item, amount) VALUES('" + var1 + "', '" + var2 + "');");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setSellLimit(Map<String, Integer> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         var2.executeUpdate("REPLACE INTO sellLimits(item, amount) VALUES('" + var4 + "', " + var1.get(var4) + ");");
      }

      var2.close();
   }

   @Override
   public synchronized void setSellLimitRestockTimes(Map<String, Long> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         var2.executeUpdate("REPLACE INTO sellLimits VALUES('" + var4 + "', 0, '" + var1.get(var4) + "');");
      }

      var2.close();
   }

   @Override
   public synchronized int getSellLimit(String var1) throws SQLException {
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT amount FROM sellLimits WHERE item = '" + var1 + "';");
      if (!var3.next()) {
         throw new SQLException();
      } else {
         int var4 = var3.getInt("amount");
         var3.close();
         var2.close();
         return var4;
      }
   }

   @Override
   public synchronized Map<String, Integer> getSellLimits(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Statement var3 = this.conn.createStatement();
         ResultSet var4 = var3.executeQuery("SELECT item,amount FROM sellLimits WHERE item IN ('" + String.join("','", var1) + "');");

         while (var4.next()) {
            var2.put(var4.getString("item"), var4.getInt("amount"));
         }

         var4.close();
         var3.close();
         return var2;
      }
   }

   @Override
   public synchronized Map<String, Integer> getAllSellLimits() throws SQLException {
      HashMap var1 = new HashMap();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT item,amount FROM sellLimits;");

      while (var3.next()) {
         var1.put(var3.getString("item"), var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized Map<String, Long> getSellLimitRestockTimes() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT item,restock FROM sellLimits WHERE restock IS NOT NULL;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), var2.getLong("restock"));
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized void setPlayerSellLimit(UUID var1, String var2, Integer var3) {
      try {
         Statement var4 = this.conn.createStatement();
         var4.executeUpdate("REPLACE INTO playerSellLimits(item,uuid,amount) VALUES('" + var2 + "', '" + var1 + "', " + var3 + ");");
         var4.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerSellLimit(String var1, Integer var2) {
      try {
         Statement var3 = this.conn.createStatement();
         var3.executeUpdate("UPDATE playerSellLimits SET amount = " + var2 + " WHERE item = '" + var1 + "';");
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }
   }

   @Override
   public synchronized void setPlayerSellLimit(Map<String, Map<UUID, Integer>> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         Map<UUID, Integer> var5 = var1.get(var4);

         for (UUID var7 : var5.keySet()) {
            var2.executeUpdate("REPLACE INTO playerSellLimits(item,uuid,amount) VALUES('" + var4 + "', '" + var7 + "', " + var5.get(var7) + ");");
         }
      }

      var2.close();
   }

   @Override
   public synchronized void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException {
      Statement var2 = this.conn.createStatement();

      for (String var4 : var1.keySet()) {
         Map<UUID, Long> var5 = var1.get(var4);

         for (UUID var7 : var5.keySet()) {
            var2.executeUpdate("REPLACE INTO playerSellLimits VALUES('" + var4 + "', '" + var7 + "', 0, '" + var5.get(var7) + "');");
         }
      }

      var2.close();
   }

   @Override
   public synchronized int getPlayerSellLimit(UUID var1, String var2) throws SQLException {
      Statement var3 = this.conn.createStatement();
      ResultSet var4 = var3.executeQuery("SELECT amount FROM playerSellLimits WHERE item = '" + var2 + "' AND uuid = '" + var1 + "';");
      if (!var4.next()) {
         throw new SQLException();
      } else {
         int var5 = var4.getInt("amount");
         var4.close();
         var3.close();
         return var5;
      }
   }

   @Override
   public synchronized Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> var1, UUID var2) throws SQLException {
      HashMap var3 = new HashMap();
      if (var1.isEmpty()) {
         return var3;
      } else {
         Statement var4 = this.conn.createStatement();
         ResultSet var5 = var4.executeQuery(
            "SELECT item,amount FROM playerSellLimits WHERE item IN ('" + String.join("','", var1) + "') AND uuid = '" + var2 + "';"
         );

         while (var5.next()) {
            var3.put(var5.getString("item"), var5.getInt("amount"));
         }

         var5.close();
         var4.close();
         return var3;
      }
   }

   @Override
   public synchronized Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT item,restock FROM playerSellLimits WHERE restock IS NOT NULL;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         Statement var4 = this.conn.createStatement();
         ResultSet var5 = var4.executeQuery(
            "SELECT item,uuid,restock FROM playerSellLimits WHERE restock IS NOT NULL AND item = '" + var2.getString("item") + "';"
         );
         HashMap var6 = new HashMap();

         while (var5.next()) {
            var6.put(UUID.fromString(var5.getString("uuid")), var5.getLong("restock"));
         }

         var5.close();
         var4.close();
         var3.put(var2.getString("item"), var6);
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized Map<String, Integer> getPlayerSellLimits(String var1) throws SQLException {
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT uuid,amount FROM playerSellLimits WHERE item = '" + var1 + "';");
      HashMap var4 = new HashMap();

      while (var3.next()) {
         String var5 = var3.getString("uuid");
         var4.put(var5, var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var4;
   }

   @Override
   public synchronized Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException {
      HashMap<String, Map<UUID, Integer>> var1 = new HashMap<>();
      Statement var2 = this.conn.createStatement();
      ResultSet var3 = var2.executeQuery("SELECT item,uuid,amount FROM playerSellLimits;");

      while (var3.next()) {
         var1.computeIfAbsent(var3.getString("item"), var0 -> new HashMap<>()).put(UUID.fromString(var3.getString("uuid")), var3.getInt("amount"));
      }

      var3.close();
      var2.close();
      return var1;
   }

   @Override
   public synchronized void setItemStockSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException {
      PreparedStatement var2 = this.conn.prepareStatement("REPLACE INTO item_stock_schedules VALUES(?,?,?);");

      for (Entry var4 : var1.entrySet()) {
         var2.setString(1, ((ScheduleManager.Item)var4.getKey()).getItemPath());
         var2.setInt(2, ((ScheduleManager.Item)var4.getKey()).mode);
         var2.setString(3, ((Long)var4.getValue()).toString());
         var2.execute();
      }
   }

   @Override
   public synchronized Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM item_stock_schedules;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(new ScheduleManager.Item(var2.getString("item"), var2.getInt("mode")), Long.parseLong(var2.getString("restock")));
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException {
      PreparedStatement var2 = this.conn.prepareStatement("REPLACE INTO sell_limit_schedules VALUES(?,?,?);");

      for (Entry var4 : var1.entrySet()) {
         var2.setString(1, ((ScheduleManager.Item)var4.getKey()).getItemPath());
         var2.setInt(2, ((ScheduleManager.Item)var4.getKey()).mode);
         var2.setString(3, ((Long)var4.getValue()).toString());
         var2.execute();
      }
   }

   @Override
   public synchronized Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM sell_limit_schedules;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(new ScheduleManager.Item(var2.getString("item"), var2.getInt("mode")), Long.parseLong(var2.getString("restock")));
      }

      var2.close();
      var1.close();
      return var3;
   }

   @Override
   public synchronized Map<String, Integer> getOriginalLimits() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM original_limits;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), var2.getInt("amount"));
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized Map<String, SimplePair<Integer, Long>> getGlobalSellLimits() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM sellLimits;");
      HashMap var3 = new HashMap();

      while (var2.next()) {
         var3.put(var2.getString("item"), new SimplePair<>(var2.getInt("amount"), var2.getLong("restock")));
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized void setGlobalLimit(String var1, SimplePair<Integer, Long> var2) throws SQLException {
      Statement var3 = this.conn.createStatement();
      var3.executeUpdate("REPLACE INTO sellLimits VALUES('" + var1 + "', " + var2.key + ", " + var2.value + ");");
      var3.close();
   }

   private synchronized Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerSellLimits() throws SQLException {
      Statement var1 = this.conn.createStatement();
      ResultSet var2 = var1.executeQuery("SELECT * FROM playerSellLimits;");
      HashMap<String, Map<String, SimplePair<Integer, Long>>> var3 = new HashMap<>();

      while (var2.next()) {
         String var4 = var2.getString("item");
         Map<String, SimplePair<Integer, Long>> var5 = var3.getOrDefault(var4, new HashMap<>());
         var5.put(var2.getString("uuid"), new SimplePair<>(var2.getInt("amount"), var2.getLong("restock")));
         var3.put(var4, var5);
      }

      var2.close();
      var1.close();
      return var3;
   }

   private synchronized void setPlayerLimit(String var1, Map<String, SimplePair<Integer, Long>> var2) throws SQLException {
      Statement var3 = this.conn.createStatement();

      for (Entry var5 : var2.entrySet()) {
         var3.executeUpdate(
            "REPLACE INTO playerSellLimits VALUES('"
               + var1
               + "', '"
               + (String)var5.getKey()
               + "', "
               + ((SimplePair)var5.getValue()).key
               + ", "
               + ((SimplePair)var5.getValue()).value
               + ");"
         );
      }

      var3.close();
   }

   @Override
   public synchronized void setRotatingShopItems(String var1, List<String> var2, long var3) {
   }

   @Override
   public synchronized SimplePair<List<String>, Long> getRotatingShopItems(String var1) {
      return null;
   }
}
