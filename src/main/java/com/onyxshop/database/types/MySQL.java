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
         HikariConfig var1 = new HikariConfig();
         var1.setJdbcUrl(
            "jdbc:mysql://"
               + ConfigManager.getConfig().getString("mysql.host")
               + ":"
               + ConfigManager.getConfig().getString("mysql.port")
               + "/"
               + ConfigManager.getConfig().getString("mysql.database")
         );
         var1.setUsername(ConfigManager.getConfig().getString("mysql.username"));
         var1.setPassword(ConfigManager.getConfig().getString("mysql.password"));
         var1.addDataSourceProperty("userServerPrepStmts", "true");
         var1.addDataSourceProperty("cachePrepStmts", "true");
         var1.addDataSourceProperty("prepStmtCacheSize", "250");
         var1.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
         if (ConfigManager.getConfig().contains("mysql.pool.useSSL")) {
            var1.addDataSourceProperty("useSSL", ConfigManager.getConfig().getBoolean("mysql.pool.useSSL"));
         }

         if (ConfigManager.getConfig().contains("mysql.pool.verifyServerCertificate")) {
            var1.addDataSourceProperty("verifyServerCertificate", ConfigManager.getConfig().getBoolean("mysql.pool.verifyServerCertificate"));
         }

         this.pool = new HikariDataSource(var1);
         this.pool.setMaxLifetime(ConfigManager.getConfig().getLong("mysql.pool.maximum-lifetime"));
         this.pool.setIdleTimeout(ConfigManager.getConfig().getLong("mysql.pool.idle-timeout"));
         this.pool.setMaximumPoolSize(ConfigManager.getConfig().getInt("mysql.pool.maximum-pool-size"));
         this.pool.setMinimumIdle(ConfigManager.getConfig().getInt("mysql.pool.minimum-idle"));
         this.createTables();
         this.alterOriginalLimitColumn();
         return true;
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to create a connection to the MySQL database");
         var2.printStackTrace();
         return false;
      }
   }

   private void createTables() {
      try {
         Connection var1 = this.pool.getConnection();

         try {
            Statement var2 = var1.createStatement();

            try {
               var2.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.dynamicStock + " (item VARCHAR(100) NOT NULL, stock INT NOT NULL, UNIQUE(item));");
               var2.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.maxStocks + " (item VARCHAR(100) NOT NULL, maxStock INT NOT NULL, UNIQUE(item));");
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.playerstock
                     + " (item VARCHAR(100) NOT NULL, uuid VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, restock TEXT, UNIQUE(item,uuid));"
               );
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.itemstock + " (item VARCHAR(100) NOT NULL, amount INT NOT NULL, restock VARCHAR(50), UNIQUE(item));"
               );
               var2.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.originalStock + " (item VARCHAR(100) NOT NULL,amount INT NOT NULL, UNIQUE(item));");
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.item_stock_schedules
                     + "(item VARCHAR(100) NOT NULL,mode INT NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
               );
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.rotating_shop_sync
                     + "(shop VARCHAR(100) NOT NULL,items TEXT NOT NULL,nextRotation BIGINT NOT NULL,UNIQUE(shop));"
               );
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.playerSellLimits
                     + " (item VARCHAR(100) NOT NULL, uuid VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, restock TEXT, UNIQUE(item,uuid));"
               );
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.sellLimits + " (item VARCHAR(100) NOT NULL, amount INT NOT NULL, restock VARCHAR(50), UNIQUE(item));"
               );
               var2.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.originalLimits + " (item VARCHAR(100) NOT NULL,amount INT NOT NULL, UNIQUE(item));");
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS "
                     + this.sell_limit_schedules
                     + "(item VARCHAR(100) NOT NULL,mode INT NOT NULL,restock TEXT NOT NULL,UNIQUE(item,mode));"
               );
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS " + this.player_price_modifiers + " (uuid VARCHAR(36) NOT NULL, data VARCHAR(15360) NOT NULL,UNIQUE(uuid));"
               );
            } catch (Throwable var7) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (Throwable var8) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (var1 != null) {
            var1.close();
         }
      } catch (SQLException var9) {
         var9.printStackTrace();
      }
   }

   public void alterOriginalLimitColumn() {
      try {
         Connection var1 = this.pool.getConnection();

         try {
            Statement var2 = var1.createStatement();

            try {
               var2.executeUpdate("ALTER TABLE " + this.originalLimits + " DROP COLUMN restock;");
            } catch (Throwable var7) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (Throwable var8) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (var1 != null) {
            var1.close();
         }
      } catch (SQLException var9) {
      }
   }

   @Override
   public int removeExpiredPlayerData(long var1) throws SQLException {
      HashSet<String> var3 = new HashSet<>();
      int var4 = 0;
      long var5 = new Date().getTime() - var1;
      Connection var7 = this.pool.getConnection();

      try {
         Statement var8 = var7.createStatement();

         try {
            ResultSet var9 = var8.executeQuery("SELECT DISTINCT uuid FROM playerstock;");

            try {
               SendMessage.logDebugMessage("Checking " + var9.getFetchSize() + " player stock entries...");

               while (var9.next()) {
                  String var10 = var9.getString("uuid");
                  if (Bukkit.getOfflinePlayer(UUID.fromString(var10)).getLastPlayed() < var5) {
                     var3.add(var10);
                  } else {
                     var4++;
                  }
               }
            } catch (Throwable var21) {
               if (var9 != null) {
                  try {
                     var9.close();
                  } catch (Throwable var17) {
                     var21.addSuppressed(var17);
                  }
               }

               throw var21;
            }

            if (var9 != null) {
               var9.close();
            }

            var9 = var8.executeQuery("SELECT DISTINCT uuid FROM playerSellLimits;");

            try {
               SendMessage.logDebugMessage("Checking " + var9.getFetchSize() + " player sell limit entries...");

               while (var9.next()) {
                  String var28 = var9.getString("uuid");
                  if (Bukkit.getOfflinePlayer(UUID.fromString(var28)).getLastPlayed() < var5) {
                     var3.add(var28);
                  } else {
                     var4++;
                  }
               }
            } catch (Throwable var20) {
               if (var9 != null) {
                  try {
                     var9.close();
                  } catch (Throwable var16) {
                     var20.addSuppressed(var16);
                  }
               }

               throw var20;
            }

            if (var9 != null) {
               var9.close();
            }
         } catch (Throwable var22) {
            if (var8 != null) {
               try {
                  var8.close();
               } catch (Throwable var15) {
                  var22.addSuppressed(var15);
               }
            }

            throw var22;
         }

         if (var8 != null) {
            var8.close();
         }
      } catch (Throwable var23) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var14) {
               var23.addSuppressed(var14);
            }
         }

         throw var23;
      }

      if (var7 != null) {
         var7.close();
      }

      SendMessage.logDebugMessage("Found recent database entries for " + var4 + " players, skipping...");
      SendMessage.logDebugMessage("Deleting expired entries...");
      var7 = this.pool.getConnection();

      try {
         Statement var25 = var7.createStatement();

         try {
            for (String var29 : var3) {
               var25.executeUpdate("DELETE FROM playerstock WHERE uuid = '" + var29 + "';");
               var25.executeUpdate("DELETE FROM playerSellLimits WHERE uuid = '" + var29 + "';");
               SendMessage.logDebugMessage("Deleted expired player data for " + var29);
            }
         } catch (Throwable var18) {
            if (var25 != null) {
               try {
                  var25.close();
               } catch (Throwable var13) {
                  var18.addSuppressed(var13);
               }
            }

            throw var18;
         }

         if (var25 != null) {
            var25.close();
         }
      } catch (Throwable var19) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var12) {
               var19.addSuppressed(var12);
            }
         }

         throw var19;
      }

      if (var7 != null) {
         var7.close();
      }

      SendMessage.logDebugMessage("Deleted " + var3.size() + " expired unique player entries from database");
      return var3.size();
   }

   @Override
   public Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException {
      SendMessage.logDebugMessage("Retrieving item indexes from database...");
      HashMap<String, Set<String>> var1 = new HashMap<>();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
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
               ResultSet var8 = var3.executeQuery("SELECT DISTINCT item FROM " + var7 + ";");

               try {
                  while (var8.next()) {
                     var1.computeIfAbsent(var8.getString("item"), var0 -> new HashSet<>()).add(var7);
                  }
               } catch (Throwable var14) {
                  if (var8 != null) {
                     try {
                        var8.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (var8 != null) {
                  var8.close();
               }
            }
         } catch (Throwable var15) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var12) {
                  var15.addSuppressed(var12);
               }
            }

            throw var15;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var16) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var11) {
               var16.addSuppressed(var11);
            }
         }

         throw var16;
      }

      if (var2 != null) {
         var2.close();
      }

      HashMap<String, Map<String, Set<String>>> var17 = new HashMap<>();

      for (Entry var20 : var1.entrySet()) {
         var17.computeIfAbsent(((String)var20.getKey()).split("\\.")[0], var0 -> new HashMap<>())
            .put(((String)var20.getKey()).split("\\.", 2)[1], (Set)var20.getValue());
      }

      SendMessage.logDebugMessage("Checking if items still exist...");

      for (String var21 : var17.keySet()) {
         if (ConfigManager.getShops().contains(var21)) {
            List<String> var22 = OnyxShop.getInstance().getConfigManager().getItemsRaw(var21);
            HashMap<String, Set<String>> var23 = new HashMap<>();

            for (String var25 : var17.get(var21).keySet()) {
               if (!var22.contains(var25)) {
                  var23.put(var25, var17.get(var21).get(var25));
               }
            }

            var17.put(var21, var23);
         }
      }

      return var17;
   }

   @Override
   public void removeExpiredItemData(Map<String, Map<String, Set<String>>> var1) throws SQLException {
      SendMessage.logDebugMessage("Removing loaded expired item data from database...");
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               List<String> var6 = new ArrayList<>();
               if (ConfigManager.getShops().contains(var5)) {
                  var6 = OnyxShop.getInstance().getConfigManager().getItemsRaw(var5);
               }

               for (Entry var8 : var1.get(var5).entrySet()) {
                  if (!var6.contains(var8.getKey())) {
                     SendMessage.logDebugMessage(
                        "Removing data for item '" + var5 + "." + (String)var8.getKey() + "' from " + ((Set)var8.getValue()).size() + " tables..."
                     );

                     for (String var10 : (Set<String>)var8.getValue()) {
                        var3.executeUpdate("DELETE FROM " + var10 + " WHERE item = '" + (String)var8.getKey() + "';");
                     }
                  }
               }
            }
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var14) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }
         }

         throw var14;
      }

      if (var2 != null) {
         var2.close();
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
      SendMessage.logDebugMessage("Database successfully cleaned from expired item entries");
   }

   public void migrateV4toV5() throws SQLException {
      long var1 = System.currentTimeMillis();
      OnyxShop var3 = OnyxShop.getInstance();
      HashMap var4 = new HashMap();
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
      Connection var11 = this.pool.getConnection();

      try {
         Statement var12 = var11.createStatement();

         try {
            var12.executeUpdate("DROP TABLE " + this.dynamicStock + ";");
            var12.executeUpdate("DROP TABLE " + this.maxStocks + ";");
            var3.getLimitedStockProvider().emptyCache();
            var12.executeUpdate("DROP TABLE " + this.originalStock + ";");
            var12.executeUpdate("DROP TABLE " + this.playerstock + ";");
            var12.executeUpdate("DROP TABLE " + this.itemstock + ";");
            var3.getLimitedSellProvider().emptyCache();
            var12.executeUpdate("DROP TABLE " + this.originalLimits + ";");
            var12.executeUpdate("DROP TABLE " + this.playerSellLimits + ";");
            var12.executeUpdate("DROP TABLE " + this.sellLimits + ";");
         } catch (Throwable var17) {
            if (var12 != null) {
               try {
                  var12.close();
               } catch (Throwable var16) {
                  var17.addSuppressed(var16);
               }
            }

            throw var17;
         }

         if (var12 != null) {
            var12.close();
         }
      } catch (Throwable var18) {
         if (var11 != null) {
            try {
               var11.close();
            } catch (Throwable var15) {
               var18.addSuppressed(var15);
            }
         }

         throw var18;
      }

      if (var11 != null) {
         var11.close();
      }

      this.createTables();

      for (Entry var25 : var5.entrySet()) {
         String var13 = this.getNewItem(var4, var3, (String)var25.getKey());
         if (var13 != null) {
            this.setOriginalStock(var13, (Integer)var25.getValue());
         }
      }

      for (Entry var26 : var6.entrySet()) {
         String var31 = this.getNewItem(var4, var3, (String)var26.getKey());
         if (var31 != null) {
            this.setGlobalStock(var31, (SimplePair<Integer, Long>)var26.getValue());
         }
      }

      for (Entry var27 : var7.entrySet()) {
         String var32 = this.getNewItem(var4, var3, (String)var27.getKey());
         if (var32 != null) {
            this.setPlayerStock(var32, (Map<String, SimplePair<Integer, Long>>)var27.getValue());
         }
      }

      for (Entry var28 : var8.entrySet()) {
         String var33 = this.getNewItem(var4, var3, (String)var28.getKey());
         if (var33 != null) {
            this.setOriginalLimit(var33, (Integer)var28.getValue());
         }
      }

      for (Entry var29 : var9.entrySet()) {
         String var34 = this.getNewItem(var4, var3, (String)var29.getKey());
         if (var34 != null) {
            this.setGlobalLimit(var34, (SimplePair<Integer, Long>)var29.getValue());
         }
      }

      for (Entry var30 : var10.entrySet()) {
         String var35 = this.getNewItem(var4, var3, (String)var30.getKey());
         if (var35 != null) {
            this.setPlayerLimit(var35, (Map<String, SimplePair<Integer, Long>>)var30.getValue());
         }
      }

      SendMessage.infoMessage("Successfully migrated loaded data into the database in " + (System.currentTimeMillis() - var1) + "ms");
   }

   private String getNewItem(Map<String, String> var1, OnyxShop var2, String var3) {
      String var4 = (String)var1.get(var3);
      if (var4 != null) {
         return var4;
      } else {
         String[] var5 = var3.split("\\.");
         if (var5.length != 2) {
            if (var5.length == 4) {
               return null;
            } else {
               SendMessage.logDebugMessage("Invalid item format '" + var3 + "', skipping...");
               return null;
            }
         } else {
            String var6 = var5[0];
            String var7 = var5[1];
            var4 = var2.getConfigManager().getItemsRaw(var6).stream().filter(var1x -> var1x.split("\\.")[2].equalsIgnoreCase(var7)).findFirst().orElse(null);
            if (var4 == null) {
               SendMessage.logDebugMessage("Failed to migrate item '" + var3 + "' to new format because it does not exist, skipping...");
            }

            var4 = var6 + "." + var4;
            var1.put(var3, var4);
            return var4;
         }
      }
   }

   @Override
   public void closeConnection() {
      this.pool.close();
   }

   @Override
   public void executeQuery(SQLBatch.Type var1, Query... var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               for (Query var8 : var2) {
                  var4.executeUpdate(var8.format(this.getTable(var1, var8)));
               }
            } catch (Throwable var11) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var12) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var13) {
         var13.printStackTrace();
      }
   }

   @Override
   public void executeBatch(SQLBatch.Type var1, Set<Query> var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               for (Query var6 : var2) {
                  var4.executeUpdate(var6.format(this.getTable(var1, var6)));
               }
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   private String getTable(SQLBatch.Type var1, Query var2) {
      switch (var1) {
         case STOCK:
            if (var2 instanceof ItemStockQuery
               || var2 instanceof GlobalRestockTimerQuery
               || var2 instanceof RemoveStockQuery
               || var2 instanceof ClearStockQuery) {
               return this.itemstock;
            } else if (var2 instanceof PlayerStockQuery
               || var2 instanceof UpdatePlayerStockQuery
               || var2 instanceof PlayerRestockTimerQuery
               || var2 instanceof RemovePlayerStockQuery
               || var2 instanceof RemovePlayerStocksQuery
               || var2 instanceof ClearPlayerStockQuery) {
               return this.playerstock;
            } else if (var2 instanceof RestockScheduleQuery) {
               return this.item_stock_schedules;
            }
         case SELL_LIMIT:
            if (var2 instanceof ItemStockQuery
               || var2 instanceof GlobalRestockTimerQuery
               || var2 instanceof RemoveStockQuery
               || var2 instanceof ClearStockQuery) {
               return this.sellLimits;
            } else if (var2 instanceof PlayerStockQuery
               || var2 instanceof UpdatePlayerStockQuery
               || var2 instanceof PlayerRestockTimerQuery
               || var2 instanceof RemovePlayerStockQuery
               || var2 instanceof RemovePlayerStocksQuery
               || var2 instanceof ClearPlayerStockQuery) {
               return this.playerSellLimits;
            } else if (var2 instanceof RestockScheduleQuery) {
               return this.sell_limit_schedules;
            }
         default:
            return null;
      }
   }

   @Override
   public PlayerPriceModifier loadPlayerPriceModifiers(UUID var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      PlayerPriceModifier var5;
      try {
         PreparedStatement var3 = var2.prepareStatement("SELECT data FROM " + this.player_price_modifiers + " WHERE uuid=?");

         try {
            var3.setString(1, var1.toString());
            ResultSet var4 = var3.executeQuery();

            try {
               var5 = var4.next() ? (PlayerPriceModifier)new Gson().fromJson(var4.getString(1), PlayerPriceModifier.class) : null;
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var5;
   }

   @Override
   public void savePlayerPriceModifiers(UUID var1, PlayerPriceModifier var2) throws SQLException {
      if (var2.getSectionModifiers().isEmpty() && var2.getItemModifiers().isEmpty()) {
         Connection var15 = this.pool.getConnection();

         try {
            PreparedStatement var16 = var15.prepareStatement("DELETE FROM " + this.player_price_modifiers + " WHERE uuid = ?;");

            try {
               var16.setString(1, var1.toString());
               var16.execute();
            } catch (Throwable var12) {
               if (var16 != null) {
                  try {
                     var16.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw var12;
            }

            if (var16 != null) {
               var16.close();
            }
         } catch (Throwable var13) {
            if (var15 != null) {
               try {
                  var15.close();
               } catch (Throwable var9) {
                  var13.addSuppressed(var9);
               }
            }

            throw var13;
         }

         if (var15 != null) {
            var15.close();
         }
      } else {
         Connection var3 = this.pool.getConnection();

         try {
            PreparedStatement var4 = var3.prepareStatement("REPLACE INTO " + this.player_price_modifiers + " VALUES(?,?);");

            try {
               var4.setString(1, var1.toString());
               var4.setString(2, new Gson().toJson(var2));
               var4.execute();
            } catch (Throwable var11) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var14) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var14.addSuppressed(var7);
               }
            }

            throw var14;
         }

         if (var3 != null) {
            var3.close();
         }
      }
   }

   @Override
   public Map<String, Integer> loadDynamicPricingStock() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.dynamicStock + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("stock"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public Integer getDynamicStock(String var1) {
      int var2;
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               ResultSet var5 = var4.executeQuery("SELECT stock FROM " + this.dynamicStock + " WHERE item = '" + var1 + "';");
               var5.next();
               var2 = var5.getInt("stock");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         return null;
      }

      return var2;
   }

   @Override
   public Map<String, Integer> getDynamicStocks(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               ResultSet var5 = var4.executeQuery("SELECT item,stock FROM " + this.dynamicStock + " WHERE item IN ('" + String.join("','", var1) + "');");

               try {
                  while (var5.next()) {
                     var2.put(var5.getString("item"), var5.getInt("stock"));
                  }
               } catch (Throwable var11) {
                  if (var5 != null) {
                     try {
                        var5.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (var5 != null) {
                  var5.close();
               }
            } catch (Throwable var12) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var13.addSuppressed(var8);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }

         return var2;
      }
   }

   @Override
   public Map<String, Integer> loadDynamicMaxStocks() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.maxStocks + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("maxStock"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setDynamicMaxStock(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.maxStocks + " VALUES('" + var1 + "'," + var2 + ");");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void saveDynamicStock(Map<String, Integer> var1) {
      try {
         Connection var2 = this.pool.getConnection();

         try {
            PreparedStatement var3 = var2.prepareStatement("REPLACE INTO " + this.dynamicStock + " VALUES(?,?);");

            try {
               for (Entry var5 : var1.entrySet()) {
                  var3.setString(1, (String)var5.getKey());
                  var3.setInt(2, (Integer)var5.getValue());
                  var3.execute();
               }
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
      } catch (SQLException var10) {
         var10.printStackTrace();
      }
   }

   @Override
   public void setDynamicStock(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.dynamicStock + " VALUES('" + var1 + "'," + var2 + ");");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public Map<String, Integer> getOriginalStocks() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.originalStock + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public int getOriginalStock(String var1) {
      try {
         Connection var2 = this.pool.getConnection();

         int var5;
         try {
            Statement var3 = var2.createStatement();

            try {
               ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.originalStock + " WHERE item = '" + var1 + "';");
               var5 = var4.getInt("amount");
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

         return var5;
      } catch (SQLException var10) {
         return 0;
      }
   }

   @Override
   public void setOriginalStock(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.originalStock + " VALUES('" + var1 + "'," + var2 + ");");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setItemStock(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.itemstock + "(item, amount) VALUES('" + var1 + "', '" + var2 + "');");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setItemStock(Map<String, Integer> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               var3.executeUpdate("REPLACE INTO " + this.itemstock + "(item, amount) VALUES('" + var5 + "', " + var1.get(var5) + ");");
            }
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
   }

   @Override
   public void setItemRestockTimes(Map<String, Long> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               var3.executeUpdate("REPLACE INTO " + this.itemstock + " VALUES('" + var5 + "', 0, '" + var1.get(var5) + "');");
            }
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
   }

   @Override
   public int getItemStock(String var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      int var5;
      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT amount FROM " + this.itemstock + " WHERE item = '" + var1 + "';");
            var4.next();
            var5 = var4.getInt("amount");
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

      return var5;
   }

   @Override
   public Map<String, Integer> getItemStocks(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               ResultSet var5 = var4.executeQuery("SELECT item,amount FROM " + this.itemstock + " WHERE item IN ('" + String.join("','", var1) + "');");

               try {
                  while (var5.next()) {
                     var2.put(var5.getString("item"), var5.getInt("amount"));
                  }
               } catch (Throwable var11) {
                  if (var5 != null) {
                     try {
                        var5.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (var5 != null) {
                  var5.close();
               }
            } catch (Throwable var12) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var13.addSuppressed(var8);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }

         return var2;
      }
   }

   @Override
   public Map<String, Integer> getAllItemStocks() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,amount FROM " + this.itemstock + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void setOriginalStocks(Map<String, Integer> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (Entry var5 : var1.entrySet()) {
               var3.executeUpdate("REPLACE INTO " + this.originalStock + " VALUES('" + (String)var5.getKey() + "', " + var5.getValue() + ");");
            }
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
   }

   private Map<String, SimplePair<Integer, Long>> getGlobalStock() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.itemstock + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), new SimplePair<>(var4.getInt("amount"), var4.getLong("restock")));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void setGlobalStock(String var1, SimplePair<Integer, Long> var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            var4.executeUpdate("REPLACE INTO " + this.itemstock + " VALUES('" + var1 + "', " + var2.key + ", " + var2.value + ");");
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }
   }

   private Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerStock() throws SQLException {
      HashMap<String, Map<String, SimplePair<Integer, Long>>> var1 = new HashMap<>();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.playerstock + ";");

            try {
               while (var4.next()) {
                  String var5 = var4.getString("item");
                  Map var6 = var1.getOrDefault(var5, new HashMap());
                  var6.put(var4.getString("uuid"), new SimplePair<>(var4.getInt("amount"), var4.getLong("restock")));
                  var1.put(var5, var6);
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void setPlayerStock(String var1, Map<String, SimplePair<Integer, Long>> var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            for (Entry var6 : var2.entrySet()) {
               var4.executeUpdate(
                  "REPLACE INTO "
                     + this.playerstock
                     + " VALUES('"
                     + var1
                     + "', '"
                     + (String)var6.getKey()
                     + "', "
                     + ((SimplePair)var6.getValue()).key
                     + ", "
                     + ((SimplePair)var6.getValue()).value
                     + ");"
               );
            }
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }
   }

   @Override
   public Map<String, Long> getItemRestockTime() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,restock FROM " + this.itemstock + " WHERE restock IS NOT NULL;");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getLong("restock"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setPlayerStockAmount(UUID var1, String var2, Integer var3) {
      try {
         Connection var4 = this.pool.getConnection();

         try {
            Statement var5 = var4.createStatement();

            try {
               var5.executeUpdate("REPLACE INTO " + this.playerstock + "(item,uuid,amount) VALUES('" + var2 + "', '" + var1 + "', " + var3 + ");");
            } catch (Throwable var10) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var11) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (SQLException var12) {
         var12.printStackTrace();
      }
   }

   @Override
   public void setPlayerStockAmount(String var1, Integer var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("UPDATE " + this.playerstock + " SET amount = " + var2 + " WHERE item = '" + var1 + "';");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setPlayerStock(Map<String, Map<UUID, Integer>> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               Map<UUID, Integer> var6 = var1.get(var5);

               for (UUID var8 : var6.keySet()) {
                  var3.executeUpdate("REPLACE INTO " + this.playerstock + "(item,uuid,amount) VALUES('" + var5 + "', '" + var8 + "', " + var6.get(var8) + ");");
               }
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   @Override
   public void setPlayerRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               Map<UUID, Long> var6 = var1.get(var5);

               for (UUID var8 : var6.keySet()) {
                  var3.executeUpdate("REPLACE INTO " + this.playerstock + " VALUES('" + var5 + "', '" + var8 + "', 0, '" + var6.get(var8) + "');");
               }
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   @Override
   public int getPlayerStockAmount(UUID var1, String var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      int var6;
      try {
         Statement var4 = var3.createStatement();

         try {
            ResultSet var5 = var4.executeQuery("SELECT amount FROM " + this.playerstock + " WHERE item = '" + var2 + "' AND uuid = '" + var1 + "';");
            var5.next();
            var6 = var5.getInt("amount");
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }

      return var6;
   }

   @Override
   public Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,restock FROM " + this.playerstock + " WHERE restock IS NOT NULL;");

            try {
               while (var4.next()) {
                  HashMap var5 = new HashMap();
                  Statement var6 = var2.createStatement();

                  try {
                     ResultSet var7 = var6.executeQuery(
                        "SELECT item,uuid,restock FROM " + this.playerstock + " WHERE restock IS NOT NULL AND item = '" + var4.getString("item") + "';"
                     );

                     try {
                        while (var7.next()) {
                           var5.put(UUID.fromString(var7.getString("uuid")), var7.getLong("restock"));
                        }
                     } catch (Throwable var15) {
                        if (var7 != null) {
                           try {
                              var7.close();
                           } catch (Throwable var14) {
                              var15.addSuppressed(var14);
                           }
                        }

                        throw var15;
                     }

                     if (var7 != null) {
                        var7.close();
                     }
                  } catch (Throwable var16) {
                     if (var6 != null) {
                        try {
                           var6.close();
                        } catch (Throwable var13) {
                           var16.addSuppressed(var13);
                        }
                     }

                     throw var16;
                  }

                  if (var6 != null) {
                     var6.close();
                  }

                  var1.put(var4.getString("item"), var5);
               }
            } catch (Throwable var17) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var12) {
                     var17.addSuppressed(var12);
                  }
               }

               throw var17;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var18) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var11) {
                  var18.addSuppressed(var11);
               }
            }

            throw var18;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var19) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var10) {
               var19.addSuppressed(var10);
            }
         }

         throw var19;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public Map<String, Integer> getPlayersStock(String var1) throws SQLException {
      HashMap var2 = new HashMap();
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            ResultSet var5 = var4.executeQuery("SELECT uuid,amount FROM " + this.playerstock + " WHERE item = '" + var1 + "';");

            try {
               while (var5.next()) {
                  String var6 = var5.getString("uuid");
                  var2.put(var6, var5.getInt("amount"));
               }
            } catch (Throwable var11) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var12) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var13) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }
         }

         throw var13;
      }

      if (var3 != null) {
         var3.close();
      }

      return var2;
   }

   @Override
   public Map<String, Integer> getPlayerStockAmounts(Collection<String> var1, UUID var2) throws SQLException {
      HashMap var3 = new HashMap();
      if (var1.isEmpty()) {
         return var3;
      } else {
         Connection var4 = this.pool.getConnection();

         try {
            Statement var5 = var4.createStatement();

            try {
               ResultSet var6 = var5.executeQuery(
                  "SELECT item,amount FROM " + this.playerstock + " WHERE item IN ('" + String.join("','", var1) + "') AND uuid = '" + var2 + "';"
               );

               try {
                  while (var6.next()) {
                     var3.put(var6.getString("item"), var6.getInt("amount"));
                  }
               } catch (Throwable var12) {
                  if (var6 != null) {
                     try {
                        var6.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (var6 != null) {
                  var6.close();
               }
            } catch (Throwable var13) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }
               }

               throw var13;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var14) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var14.addSuppressed(var9);
               }
            }

            throw var14;
         }

         if (var4 != null) {
            var4.close();
         }

         return var3;
      }
   }

   @Override
   public Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException {
      HashMap<String, Map<UUID, Integer>> var1 = new HashMap<>();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,uuid,amount FROM " + this.playerstock + ";");

            try {
               while (var4.next()) {
                  var1.computeIfAbsent(var4.getString("item"), var0 -> new HashMap<>()).put(UUID.fromString(var4.getString("uuid")), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public boolean resetDynamicPricing() {
      try {
         Connection var1 = this.pool.getConnection();

         boolean var3;
         try {
            Statement var2 = var1.createStatement();

            try {
               var2.executeUpdate("DROP TABLE " + this.dynamicStock + ";");
               var2.executeUpdate("DROP TABLE " + this.maxStocks + ";");
               this.createTables();
               var3 = true;
            } catch (Throwable var7) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (Throwable var8) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (var1 != null) {
            var1.close();
         }

         return var3;
      } catch (SQLException var9) {
         SendMessage.warnMessage("Failed to delete DynamicPricing tables from database");
         var9.printStackTrace();
         return false;
      }
   }

   @Override
   public int getOriginalLimit(String var1) {
      try {
         Connection var2 = this.pool.getConnection();

         int var5;
         try {
            Statement var3 = var2.createStatement();

            try {
               ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.originalLimits + " WHERE item = '" + var1 + "';");
               var5 = var4.getInt("amount");
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

         return var5;
      } catch (SQLException var10) {
         return 0;
      }
   }

   @Override
   public void setOriginalLimit(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.originalLimits + " VALUES('" + var1 + "'," + var2 + ");");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setSellLimit(String var1, int var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("REPLACE INTO " + this.sellLimits + "(item, amount) VALUES('" + var1 + "', '" + var2 + "');");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setSellLimit(Map<String, Integer> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               var3.executeUpdate("REPLACE INTO " + this.sellLimits + "(item, amount) VALUES('" + var5 + "', " + var1.get(var5) + ");");
            }
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
   }

   @Override
   public void setSellLimitRestockTimes(Map<String, Long> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               var3.executeUpdate("REPLACE INTO " + this.sellLimits + " VALUES('" + var5 + "', 0, '" + var1.get(var5) + "');");
            }
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
   }

   @Override
   public int getSellLimit(String var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      int var5;
      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT amount FROM " + this.sellLimits + " WHERE item = '" + var1 + "';");
            var4.next();
            var5 = var4.getInt("amount");
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

      return var5;
   }

   @Override
   public Map<String, Integer> getSellLimits(Collection<String> var1) throws SQLException {
      HashMap var2 = new HashMap();
      if (var1.isEmpty()) {
         return var2;
      } else {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               ResultSet var5 = var4.executeQuery("SELECT item,amount FROM " + this.sellLimits + " WHERE item IN ('" + String.join("','", var1) + "');");

               try {
                  while (var5.next()) {
                     var2.put(var5.getString("item"), var5.getInt("amount"));
                  }
               } catch (Throwable var11) {
                  if (var5 != null) {
                     try {
                        var5.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (var5 != null) {
                  var5.close();
               }
            } catch (Throwable var12) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var13.addSuppressed(var8);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }

         return var2;
      }
   }

   @Override
   public Map<String, Integer> getAllSellLimits() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,amount FROM " + this.sellLimits + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public Map<String, Long> getSellLimitRestockTimes() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,restock FROM " + this.sellLimits + " WHERE restock IS NOT NULL;");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getLong("restock"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setPlayerSellLimit(UUID var1, String var2, Integer var3) {
      try {
         Connection var4 = this.pool.getConnection();

         try {
            Statement var5 = var4.createStatement();

            try {
               var5.executeUpdate("REPLACE INTO " + this.playerSellLimits + "(item,uuid,amount) VALUES('" + var2 + "', '" + var1 + "', " + var3 + ");");
            } catch (Throwable var10) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var11) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (SQLException var12) {
         var12.printStackTrace();
      }
   }

   @Override
   public void setPlayerSellLimit(String var1, Integer var2) {
      try {
         Connection var3 = this.pool.getConnection();

         try {
            Statement var4 = var3.createStatement();

            try {
               var4.executeUpdate("UPDATE " + this.playerSellLimits + " SET amount = " + var2 + " WHERE item = '" + var1 + "';");
            } catch (Throwable var9) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (SQLException var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void setPlayerSellLimit(Map<String, Map<UUID, Integer>> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               Map<UUID, Integer> var6 = var1.get(var5);

               for (UUID var8 : var6.keySet()) {
                  var3.executeUpdate(
                     "REPLACE INTO " + this.playerSellLimits + "(item,uuid,amount) VALUES('" + var5 + "', '" + var8 + "', " + var6.get(var8) + ");"
                  );
               }
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   @Override
   public void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            for (String var5 : var1.keySet()) {
               Map<UUID, Long> var6 = var1.get(var5);

               for (UUID var8 : var6.keySet()) {
                  var3.executeUpdate("REPLACE INTO " + this.playerSellLimits + " VALUES('" + var5 + "', '" + var8 + "', 0, '" + var6.get(var8) + "');");
               }
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   @Override
   public int getPlayerSellLimit(UUID var1, String var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      int var6;
      try {
         Statement var4 = var3.createStatement();

         try {
            ResultSet var5 = var4.executeQuery("SELECT amount FROM " + this.playerSellLimits + " WHERE item = '" + var2 + "' AND uuid = '" + var1 + "';");
            var5.next();
            var6 = var5.getInt("amount");
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }

      return var6;
   }

   @Override
   public Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,restock FROM " + this.playerSellLimits + " WHERE restock IS NOT NULL;");

            try {
               while (var4.next()) {
                  HashMap var5 = new HashMap();
                  Statement var6 = var2.createStatement();

                  try {
                     ResultSet var7 = var6.executeQuery(
                        "SELECT item,uuid,restock FROM " + this.playerSellLimits + " WHERE restock IS NOT NULL AND item = '" + var4.getString("item") + "';"
                     );

                     try {
                        while (var7.next()) {
                           var5.put(UUID.fromString(var7.getString("uuid")), var7.getLong("restock"));
                        }
                     } catch (Throwable var15) {
                        if (var7 != null) {
                           try {
                              var7.close();
                           } catch (Throwable var14) {
                              var15.addSuppressed(var14);
                           }
                        }

                        throw var15;
                     }

                     if (var7 != null) {
                        var7.close();
                     }
                  } catch (Throwable var16) {
                     if (var6 != null) {
                        try {
                           var6.close();
                        } catch (Throwable var13) {
                           var16.addSuppressed(var13);
                        }
                     }

                     throw var16;
                  }

                  if (var6 != null) {
                     var6.close();
                  }

                  var1.put(var4.getString("item"), var5);
               }
            } catch (Throwable var17) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var12) {
                     var17.addSuppressed(var12);
                  }
               }

               throw var17;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var18) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var11) {
                  var18.addSuppressed(var11);
               }
            }

            throw var18;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var19) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var10) {
               var19.addSuppressed(var10);
            }
         }

         throw var19;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public Map<String, Integer> getPlayerSellLimits(String var1) throws SQLException {
      HashMap var2 = new HashMap();
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            ResultSet var5 = var4.executeQuery("SELECT uuid,amount FROM " + this.playerSellLimits + " WHERE item = '" + var1 + "';");

            try {
               while (var5.next()) {
                  String var6 = var5.getString("uuid");
                  var2.put(var6, var5.getInt("amount"));
               }
            } catch (Throwable var11) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var12) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var13) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }
         }

         throw var13;
      }

      if (var3 != null) {
         var3.close();
      }

      return var2;
   }

   @Override
   public Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> var1, UUID var2) throws SQLException {
      HashMap var3 = new HashMap();
      if (var1.isEmpty()) {
         return var3;
      } else {
         Connection var4 = this.pool.getConnection();

         try {
            Statement var5 = var4.createStatement();

            try {
               ResultSet var6 = var5.executeQuery(
                  "SELECT item,amount FROM " + this.playerSellLimits + " WHERE item IN ('" + String.join("','", var1) + "') AND uuid = '" + var2 + "';"
               );

               try {
                  while (var6.next()) {
                     var3.put(var6.getString("item"), var6.getInt("amount"));
                  }
               } catch (Throwable var12) {
                  if (var6 != null) {
                     try {
                        var6.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (var6 != null) {
                  var6.close();
               }
            } catch (Throwable var13) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }
               }

               throw var13;
            }

            if (var5 != null) {
               var5.close();
            }
         } catch (Throwable var14) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var14.addSuppressed(var9);
               }
            }

            throw var14;
         }

         if (var4 != null) {
            var4.close();
         }

         return var3;
      }
   }

   @Override
   public Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException {
      HashMap<String, Map<UUID, Integer>> var1 = new HashMap<>();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT item,uuid,amount FROM " + this.playerSellLimits + ";");

            try {
               while (var4.next()) {
                  var1.computeIfAbsent(var4.getString("item"), var0 -> new HashMap<>()).put(UUID.fromString(var4.getString("uuid")), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setItemStockSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         PreparedStatement var3 = var2.prepareStatement("REPLACE INTO " + this.item_stock_schedules + " VALUES(?,?,?);");

         try {
            for (Entry var5 : var1.entrySet()) {
               var3.setString(1, ((ScheduleManager.Item)var5.getKey()).getItemPath());
               var3.setInt(2, ((ScheduleManager.Item)var5.getKey()).mode);
               var3.setString(3, ((Long)var5.getValue()).toString());
               var3.execute();
            }
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
   }

   @Override
   public Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.item_stock_schedules + ";");

            try {
               while (var4.next()) {
                  var1.put(new ScheduleManager.Item(var4.getString("item"), var4.getInt("mode")), Long.parseLong(var4.getString("restock")));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      try {
         PreparedStatement var3 = var2.prepareStatement("REPLACE INTO " + this.sell_limit_schedules + " VALUES(?,?,?);");

         try {
            for (Entry var5 : var1.entrySet()) {
               var3.setString(1, ((ScheduleManager.Item)var5.getKey()).getItemPath());
               var3.setInt(2, ((ScheduleManager.Item)var5.getKey()).mode);
               var3.setString(3, ((Long)var5.getValue()).toString());
               var3.execute();
            }
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
   }

   @Override
   public Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.sell_limit_schedules + ";");

            try {
               while (var4.next()) {
                  var1.put(new ScheduleManager.Item(var4.getString("item"), var4.getInt("mode")), Long.parseLong(var4.getString("restock")));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public Map<String, Integer> getOriginalLimits() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.originalLimits + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), var4.getInt("amount"));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   @Override
   public void setRotatingShopItems(String var1, List<String> var2, long var3) {
      try {
         Connection var5 = this.pool.getConnection();

         try {
            PreparedStatement var6 = var5.prepareStatement("REPLACE INTO " + this.rotating_shop_sync + " VALUES(?,?,?);");

            try {
               var6.setString(1, var1);
               var6.setString(2, String.join(",", var2));
               var6.setLong(3, var3);
               var6.execute();
            } catch (Throwable var11) {
               if (var6 != null) {
                  try {
                     var6.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var6 != null) {
               var6.close();
            }
         } catch (Throwable var12) {
            if (var5 != null) {
               try {
                  var5.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (var5 != null) {
            var5.close();
         }
      } catch (SQLException var13) {
         var13.printStackTrace();
      }
   }

   @Override
   public SimplePair<List<String>, Long> getRotatingShopItems(String var1) throws SQLException {
      Connection var2 = this.pool.getConnection();

      SimplePair var13;
      label100: {
         try {
            PreparedStatement var3;
            label102: {
               var3 = var2.prepareStatement("SELECT items,nextRotation FROM " + this.rotating_shop_sync + " WHERE shop = ?;");

               try {
                  var3.setString(1, var1);
                  ResultSet var4 = var3.executeQuery();

                  label85: {
                     try {
                        if (!var4.next()) {
                           var13 = null;
                           break label85;
                        }

                        var13 = new SimplePair<>(Arrays.asList(var4.getString("items").split(",")), var4.getLong("nextRotation"));
                     } catch (Throwable var10) {
                        if (var4 != null) {
                           try {
                              var4.close();
                           } catch (Throwable var9) {
                              var10.addSuppressed(var9);
                           }
                        }

                        throw var10;
                     }

                     if (var4 != null) {
                        var4.close();
                     }
                     break label102;
                  }

                  if (var4 != null) {
                     var4.close();
                  }
               } catch (Throwable var11) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var8) {
                        var11.addSuppressed(var8);
                     }
                  }

                  throw var11;
               }

               if (var3 != null) {
                  var3.close();
               }
               break label100;
            }

            if (var3 != null) {
               var3.close();
            }
         } catch (Throwable var12) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var7) {
                  var12.addSuppressed(var7);
               }
            }

            throw var12;
         }

         if (var2 != null) {
            var2.close();
         }

         return var13;
      }

      if (var2 != null) {
         var2.close();
      }

      return var13;
   }

   private Map<String, SimplePair<Integer, Long>> getGlobalSellLimits() throws SQLException {
      HashMap var1 = new HashMap();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.sellLimits + ";");

            try {
               while (var4.next()) {
                  var1.put(var4.getString("item"), new SimplePair<>(var4.getInt("amount"), var4.getLong("restock")));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void setGlobalLimit(String var1, SimplePair<Integer, Long> var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            var4.executeUpdate("REPLACE INTO " + this.sellLimits + " VALUES('" + var1 + "', " + var2.key + ", " + var2.value + ");");
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }
   }

   private Map<String, Map<String, SimplePair<Integer, Long>>> getPlayerSellLimits() throws SQLException {
      HashMap<String, Map<String, SimplePair<Integer, Long>>> var1 = new HashMap<>();
      Connection var2 = this.pool.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM " + this.playerSellLimits + ";");

            try {
               while (var4.next()) {
                  String var5 = var4.getString("item");
                  Map<String, SimplePair<Integer, Long>> var6 = var1.getOrDefault(var5, new HashMap<>());
                  var6.put(var4.getString("uuid"), new SimplePair<>(var4.getInt("amount"), var4.getLong("restock")));
                  var1.put(var5, var6);
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void setPlayerLimit(String var1, Map<String, SimplePair<Integer, Long>> var2) throws SQLException {
      Connection var3 = this.pool.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            for (Entry var6 : var2.entrySet()) {
               var4.executeUpdate(
                  "REPLACE INTO "
                     + this.playerSellLimits
                     + " VALUES('"
                     + var1
                     + "', '"
                     + (String)var6.getKey()
                     + "', "
                     + ((SimplePair)var6.getValue()).key
                     + ", "
                     + ((SimplePair)var6.getValue()).value
                     + ");"
               );
            }
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var3 != null) {
         var3.close();
      }
   }
}
