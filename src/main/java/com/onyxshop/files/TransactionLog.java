package com.onyxshop.files;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.SQLQueries;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.JsonUtil;
import com.onyxshop.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class TransactionLog {
   private File file;
   private TransactionLog.Database db;
   private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private final ArrayList<String> cache = new ArrayList<>();
   private final OnyxShop plugin;
   private List<TransactionLog.SimpleTransaction> TRANSACTION_CACHE;

   public TransactionLog(OnyxShop var1) {
      this.plugin = var1;
   }

   public boolean setup() {
      if (this.db != null) {
         this.saveUnsaved();
      }

      this.file = new File(this.plugin.getDataFolder(), "transaction-log.txt");
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var3) {
            SendMessage.errorMessage("Failed to create the transactions log file");
            return false;
         }
      }

      try {
         this.db = new TransactionLog.Database();
         return true;
      } catch (IOException var2) {
         SendMessage.warnMessage("Failed to create transaction directory...");
         return false;
      }
   }

   public void log(String var1) {
      this.cache.add("[" + this.dateFormatter.format(new Date().getTime()) + "] - " + var1);
   }

   public void log(UUID var1, ShopItem var2, Map<EcoType, Double> var3, int var4, Transaction.Mode var5, Transaction.Type var6) {
      this.db.cacheSingle(var1.toString(), var2.getItemPath(), new Gson().toJson(var3), var4, var6, var5);
      if (this.TRANSACTION_CACHE != null) {
         this.TRANSACTION_CACHE.add(new TransactionLog.SimpleTransaction(Instant.now().toEpochMilli(), var1.toString(), var2.getItemPath(), var4, var5.name()));
      }
   }

   public void log(UUID var1, Map<ShopItem, Integer> var2, Map<EcoType, Double> var3, int var4, Transaction.Mode var5, Transaction.Type var6) {
      String var7 = new Gson()
         .toJson(var2.entrySet().stream().map(var1x -> new TransactionLog.Item((Entry<ShopItem, Integer>)var1x)).collect(Collectors.toList()));
      String var8 = new Gson().toJson(var3);
      this.db.cacheMultiple(var1.toString(), var7, var8, var4, var6, var5);
      if (this.TRANSACTION_CACHE != null) {
         long var9 = Instant.now().toEpochMilli();

         for (Entry var12 : var2.entrySet()) {
            this.TRANSACTION_CACHE
               .add(
                  new TransactionLog.SimpleTransaction(var9, var1.toString(), ((ShopItem)var12.getKey()).getItemPath(), (Integer)var12.getValue(), var5.name())
               );
         }
      }
   }

   public Collection<TransactionLog.SimpleTransaction> getTransactions(TransactionLog.TimeFrame var1, Transaction.Mode var2) {
      long[] var3 = this.getTimeFrameRange(var1);
      return this.TRANSACTION_CACHE
         .stream()
         .filter(var2x -> var2x.action == var2 && var2x.date >= var3[0] && var2x.date <= var3[1])
         .collect(Collectors.toList());
   }

   private long[] getTimeFrameRange(TransactionLog.TimeFrame var1) {
      long var2 = Instant.now().toEpochMilli();
      if (var1 == TransactionLog.TimeFrame.MONTH) {
         return new long[]{var2 - TimeUnit.DAYS.toMillis(30L), var2};
      } else if (var1 == TransactionLog.TimeFrame.WEEK) {
         return new long[]{var2 - TimeUnit.DAYS.toMillis(7L), var2};
      } else {
         long var4 = TimeUnit.DAYS.toMillis(1L);
         long var6 = this.getStartOfDay();
         return new long[]{var6, var6 + var4};
      }
   }

   private long getStartOfDay() {
      LocalDateTime var1 = LocalDateTime.now();
      LocalDateTime var2 = var1.toLocalDate().atTime(LocalTime.MIN);
      Instant var3 = var2.atZone(ZoneId.systemDefault()).toInstant();
      return var3.toEpochMilli();
   }

   private void save() {
      if (!this.cache.isEmpty()) {
         try {
            Files.write(this.file.toPath(), this.cache, StandardOpenOption.APPEND);
            this.cache.clear();
         } catch (IOException var2) {
            SendMessage.errorMessage("Failed to create the transactions log file");
         }
      }
   }

   public void saveUnsaved() {
      if (this.db != null) {
         this.db.saveUnsaved();
      }

      this.save();
   }

   public void loadCache() {
      if (this.TRANSACTION_CACHE == null) {
         this.saveUnsaved();

         try {
            this.TRANSACTION_CACHE = this.db.getAllSimple();
         } catch (Exception var2) {
            SendMessage.warnMessage("A unknown error occurred while loading transaction cache from database:\n" + this.stackTraceToString(var2));
         }
      }
   }

   public String exportLogs() {
      this.loadCache();
      ArrayList var1 = this.plugin.getTransactionLog().retrieveAllTransactions();
      Gson var2 = new Gson();
      StringBuilder var3 = new StringBuilder();
      var3.append("{\"generalStats\":" + var2.toJson(new TransactionLog.GeneralStats(var1)) + "},");
      StringBuilder var4 = new StringBuilder();
      var4.append("{\"topPlayers\":[{\"PBM\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY)) + "},");
      var4.append("{\"PBW\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY)) + "},");
      var4.append("{\"PBD\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY)) + "},");
      var4.append("{\"PSM\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL)) + "},");
      var4.append("{\"PSW\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL)) + "},");
      var4.append("{\"PSD\":" + var2.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL)) + "}]},");
      StringBuilder var5 = new StringBuilder();
      var5.append("{\"topItems\":[{\"IBM\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY, true)) + "},");
      var5.append("{\"IBW\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY, true)) + "},");
      var5.append("{\"IBD\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY, true)) + "},");
      var5.append("{\"ISM\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL, true)) + "},");
      var5.append("{\"ISW\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL, true)) + "},");
      var5.append("{\"ISD\":" + var2.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL, true)) + "}]}");
      String var6 = var2.toJson(var1, ArrayList.class);
      return "{\"stats\":["
         + var3
         + var4
         + var5
         + "],\"dates\":"
         + Arrays.toString(this.db.getAllLoaded().stream().map(var0 -> "\"" + var0.toString() + "\"").toArray())
         + ",\"logs\":"
         + var6
         + "}";
   }

   private List<TransactionLog.SimplePrice> getCashFlow(ArrayList<TransactionLog.AdvancedTransaction> var1, Transaction.Mode var2) {
      Map<String, Double> var3 = var1.stream()
         .filter(var1x -> var1x.action == var2)
         .flatMap(var0 -> var0.prices.stream())
         .collect(Collectors.groupingBy(TransactionLog.Price::getEcoType, Collectors.summingDouble(TransactionLog.Price::getAmount)));
      ArrayList var4 = new ArrayList();

      for (String var6 : var3.keySet()) {
         String var7 = this.plugin.formatNullablePrice(var6, (Double)var3.get(var6));
         if (var7 != null) {
            var4.add(new TransactionLog.SimplePrice(this.formatHTMLColors(var7)));
         }
      }

      return var4;
   }

   public ArrayList<TransactionLog.TopPlayer> getTopThreePlayers(TransactionLog.TimeFrame var1, Transaction.Mode var2) {
      HashMap<UUID, Integer> var3 = new HashMap<>();

      for (TransactionLog.SimpleTransaction var5 : this.getTransactions(var1, var2)) {
         UUID var6 = var5.uuid;
         int var7 = var5.amount;
         var3.put(var6, var3.getOrDefault(var6, 0) + var7);
      }

      ArrayList var9 = new ArrayList(var3.entrySet());
      var9.sort(Entry.comparingByValue().reversed());
      ArrayList var10 = new ArrayList();

      for (int var11 = 0; var11 < 10; var11++) {
         try {
            Entry var12 = (Entry)var9.get(var11);
            var10.add(new TransactionLog.TopPlayer((UUID)var12.getKey(), (Integer)var12.getValue()));
         } catch (IndexOutOfBoundsException var8) {
            break;
         }
      }

      return var10;
   }

   public ArrayList<TransactionLog.TopItem> getTopThreeItems(TransactionLog.TimeFrame var1, Transaction.Mode var2) {
      return this.getTopThreeItems(var1, var2, false);
   }

   private ArrayList<TransactionLog.TopItem> getTopThreeItems(TransactionLog.TimeFrame var1, Transaction.Mode var2, boolean var3) {
      HashMap<String, Integer> var4 = new HashMap<>();

      for (TransactionLog.SimpleTransaction var6 : this.getTransactions(var1, var2)) {
         String var7 = var6.itemPath;
         int var8 = var6.amount;
         var4.put(var7, var4.getOrDefault(var7, 0) + var8);
      }

      ArrayList var10 = new ArrayList(var4.entrySet());
      var10.sort(Entry.comparingByValue().reversed());
      ArrayList var11 = new ArrayList();

      for (int var12 = 0; var12 < 10; var12++) {
         try {
            Entry var13 = (Entry)var10.get(var12);
            var11.add(new TransactionLog.TopItem((String)var13.getKey(), (Integer)var13.getValue(), var3));
         } catch (IndexOutOfBoundsException var9) {
            break;
         }
      }

      return var11;
   }

   public int getItemTransactions(Transaction.Mode var1, TransactionLog.TimeFrame var2, String var3, String var4, UUID var5) {
      long[] var6 = this.getTimeFrameRange(var2);
      int var7 = 0;

      for (TransactionLog.SimpleTransaction var9 : this.TRANSACTION_CACHE) {
         if (var9.action == var1
            && var9.date >= var6[0]
            && var9.date <= var6[1]
            && (var5 == null || var9.uuid.equals(var5))
            && (var3 == null || (var4 == null ? var9.itemPath.startsWith(var3 + ".") : var9.itemPath.equals(var3 + "." + var4)))) {
            var7 += var9.amount;
         }
      }

      return var7;
   }

   public ArrayList<TransactionLog.AdvancedTransaction> retrieveAllTransactions() {
      ArrayList var1 = new ArrayList();

      for (TransactionLog.SimpleMonth var3 : this.db.getAllLoaded()) {
         try {
            var1.addAll(this.db.getAllAdvanced(var3));
         } catch (Exception var5) {
            SendMessage.warnMessage("Failed to retrieve logged transactions from " + var3.month + "/" + var3.year);
            var5.printStackTrace();
         }
      }

      Collections.sort(var1, new Comparator<TransactionLog.AdvancedTransaction>() {
         public int compare(TransactionLog.AdvancedTransaction var1, TransactionLog.AdvancedTransaction var2) {
            return Long.compare(var2.date, var1.date);
         }
      });
      return var1;
   }

   private String stackTraceToString(Exception var1) {
      StringWriter var2 = new StringWriter();
      PrintWriter var3 = new PrintWriter(var2);
      var1.printStackTrace(var3);
      return var2.toString();
   }

   private String formatHTMLColors(String var1) {
      StringBuilder var2 = new StringBuilder();
      TransactionLog.Line var3 = new TransactionLog.Line();
      TransactionLog.Color var4 = new TransactionLog.Color();
      TransactionLog.Format var5 = new TransactionLog.Format();
      boolean var6 = false;

      try {
         for (int var7 = 0; var7 < var1.length(); var7++) {
            char var8 = var1.charAt(var7);
            if (var8 == 167) {
               char var9 = var1.charAt(var7 + 1);
               if (var9 == 'x' && var1.charAt(var7 + 8) == 167) {
                  if (!var4.c.isEmpty()) {
                     if (!var5.start.isEmpty()) {
                        var2.append(var5.complete(var3.get()));
                     } else {
                        var2.append(var3.get());
                     }

                     var2.append(var4.get());
                  }

                  var2.append("<a style=\"color:" + this.getRGB(var1, var7) + "\">");
                  var4.c = "</a>";
                  var7 += 13;
               } else {
                  ChatColor var10 = ChatColor.getByChar(var9);
                  if (var10 != null) {
                     if (var10.isColor()) {
                        if (!var4.c.isEmpty()) {
                           if (!var5.start.isEmpty()) {
                              var2.append(var5.complete(var3.get()));
                           } else {
                              var2.append(var3.get());
                           }

                           var2.append(var4.get());
                        }

                        var2.append("<a style=\"color:" + var10.name().toLowerCase(Locale.ROOT).replace("_", "") + "\">");
                        var4.c = "</a>";
                        var7++;
                     } else if (var10.isFormat()) {
                        String var11 = this.getFormat(var10);
                        if (var11 != null) {
                           if (!var5.start.isEmpty()) {
                              var2.append(var5.complete(var3.get()));
                           } else {
                              var2.append(var3.get());
                           }

                           var5.start = var5.start + "<" + var11 + ">";
                           var5.ending = "</" + var11 + ">" + var5.ending;
                           var7++;
                        } else {
                           var3.text = var3.text + var8;
                        }
                     } else if (var10 == ChatColor.RESET) {
                        this.closeElement(var2, var4, var5, var3);
                        var7++;
                     }
                  }
               }
            } else {
               var3.text = var3.text + var8;
            }
         }

         if (!var5.start.isEmpty()) {
            var2.append(var5.complete(var3.get()));
         } else {
            var2.append(var3.get());
         }

         if (!var4.c.isEmpty()) {
            var2.append(var4.get());
         }
      } catch (IndexOutOfBoundsException var12) {
      }

      return var2.toString();
   }

   private void closeElement(StringBuilder var1, TransactionLog.Color var2, TransactionLog.Format var3, TransactionLog.Line var4) {
      if (!var2.c.isEmpty()) {
         if (!var3.start.isEmpty()) {
            var1.append(var3.complete(var4.get()));
         } else {
            var1.append(var4.get());
         }

         var1.append(var2.get());
      } else {
         if (!var3.start.isEmpty()) {
            var1.append(var3.complete(var4.get()));
         } else {
            var1.append(var4.get());
         }

         var1.append(var4.get());
      }
   }

   private String getFormat(ChatColor var1) {
      switch (var1) {
         case ITALIC:
            return "i";
         case BOLD:
            return "b";
         case STRIKETHROUGH:
            return "del";
         case UNDERLINE:
            return "u";
         default:
            return null;
      }
   }

   private String getRGB(String var1, int var2) {
      return var1.substring(var2, var2 + 14).replace("§", "").replace("x", "#");
   }

   private ArrayList<TransactionLog.Price> deserializePrices(String var1) {
      JsonObject var2 = (JsonObject)new Gson().fromJson(var1, JsonObject.class);
      return var2.entrySet()
         .stream()
         .map(var1x -> new TransactionLog.Price((String)var1x.getKey(), Double.parseDouble(((JsonElement)var1x.getValue()).getAsString())))
         .collect(Collectors.toCollection(ArrayList::new));
   }

   private List<TransactionLog.MultipleItem> deserializeItems(String var1) {
      try {
         JSONArray var2 = JsonUtil.parseArray(var1);
         return ((Stream<Object>)var2.stream()).map(var2x -> {
            try {
               JSONObject var3x = JsonUtil.parseJson(var2x.toString());
               return new TransactionLog.MultipleItem(Integer.valueOf(var3x.get("amount").toString()), var3x.get("itemPath").toString());
            } catch (ParseException var4) {
               SendMessage.warnMessage("Failed to parse item '" + var1 + "' for transaction, skipping...");
               var4.printStackTrace();
               return new TransactionLog.MultipleItem(0, "unknown");
            }
         }).collect(Collectors.toList());
      } catch (ParseException var3) {
         SendMessage.errorMessage("Failed to parse item(s) '" + var1 + "' for transaction, skipping...");
         var3.printStackTrace();
         return null;
      }
   }

   public final class AdvancedTransaction {
      public final long date;
      public final TransactionLog.Player player;
      public final Object items;
      public final ArrayList<TransactionLog.Price> prices;
      public final int amount;
      public final Transaction.Mode action;
      public final String type;

      public AdvancedTransaction(
         long nullx, String nullxx, Object nullxxx, ArrayList<TransactionLog.Price> nullxxxx, int nullxxxxx, String nullxxxxxx, String nullxxxxxxx
      ) {
         this.date = nullx;
         this.player = TransactionLog.this.new Player(UUID.fromString(nullxx));
         this.items = nullxxx;
         this.prices = nullxxxx;
         this.amount = nullxxxxx;
         this.action = Transaction.Mode.valueOf(nullxxxxxx);
         this.type = nullxxxxxxx;
      }
   }

   private final class Color {
      String c = "";

      public Color() {
      }

      public String get() {
         String var1 = this.c;
         this.c = "";
         return var1;
      }

      public void addColor(String var1) {
         this.c = var1 + this.c;
      }
   }

   private class Database {
      private Connection conn;
      private final String table;
      private final LinkedList<TransactionLog.Table> tables = new LinkedList<>();
      private final ArrayList<TransactionLog.PendingEntry> multiple_item_transactions = new ArrayList<>();
      private final ArrayList<TransactionLog.PendingEntry> single_item_transactions = new ArrayList<>();

      public List<TransactionLog.SimpleMonth> getAllLoaded() {
         return this.tables.stream().map(var0 -> var0.date).collect(Collectors.toList());
      }

      public List<TransactionLog.SimpleMonth> getAllSince(TransactionLog.SimpleMonth var1) {
         return this.getAllLoaded().stream().filter(var1x -> var1x.equals(var1) || var1x.isLaterThen(var1)).collect(Collectors.toList());
      }

      public Database() throws IOException {
         Calendar var2 = Calendar.getInstance();
         this.table = "TRANSACTIONS_" + (var2.get(2) + 1) + "_" + var2.get(1);

         try {
            Files.createDirectory(Paths.get(TransactionLog.this.plugin.getDataFolder() + File.separator + "transactions"));
         } catch (FileAlreadyExistsException var4) {
         }

         this.connect();
      }

      public boolean connect() {
         try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection(
               "jdbc:sqlite:" + TransactionLog.this.plugin.getDataFolder() + File.separator + "transactions" + File.separator + "transactions.db"
            );
            this.createTable();
            this.loadTables();
            this.checkTableConstraints();
            return true;
         } catch (Exception var2) {
            SendMessage.errorMessage(Lang.ERROR_OCCURRED_WHILE_CONNECTING_TO_DATABASE.get());
            var2.printStackTrace();
            return false;
         }
      }

      private void checkTableConstraints() {
         try {
            boolean var1 = false;
            Statement var2 = this.conn.createStatement();
            ResultSet var3 = var2.executeQuery("PRAGMA index_list('MULTIPLE_ITEM_" + this.table + "');");
            if (var3.next()) {
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS MULTIPLE_ITEM_"
                     + this.table
                     + "_NEW (date REAL NOT NULL,uuid TEXT NOT NULL,items TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
               );
               var2.executeUpdate(
                  "INSERT INTO MULTIPLE_ITEM_"
                     + this.table
                     + "_NEW (date, uuid, items, prices, amount, action, method) SELECT date, uuid, items, prices, amount, action, method FROM MULTIPLE_ITEM_"
                     + this.table
                     + ";"
               );
               var2.executeUpdate("DROP TABLE MULTIPLE_ITEM_" + this.table + ";");
               var2.executeUpdate("ALTER TABLE MULTIPLE_ITEM_" + this.table + "_NEW RENAME TO MULTIPLE_ITEM_" + this.table + ";");
               var1 = true;
            }

            ResultSet var4 = var2.executeQuery("PRAGMA index_list('SINGLE_ITEM_" + this.table + "');");
            if (var4.next()) {
               var2.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS SINGLE_ITEM_"
                     + this.table
                     + "_NEW (date REAL NOT NULL,uuid TEXT NOT NULL,item TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
               );
               var2.executeUpdate(
                  "INSERT INTO SINGLE_ITEM_"
                     + this.table
                     + "_NEW (date, uuid, item, prices, amount, action, method) SELECT date, uuid, item, prices, amount, action, method FROM SINGLE_ITEM_"
                     + this.table
                     + ";"
               );
               var2.executeUpdate("DROP TABLE SINGLE_ITEM_" + this.table + ";");
               var2.executeUpdate("ALTER TABLE SINGLE_ITEM_" + this.table + "_NEW RENAME TO SINGLE_ITEM_" + this.table + ";");
               var1 = true;
            }

            if (var1) {
               SendMessage.infoMessage("Successfully removed unique constraint from transaction db tables");
            }
         } catch (SQLException var5) {
            SendMessage.warnMessage("Failed to migrate database to new format for " + this.table);
            var5.printStackTrace();
         }
      }

      private void createTable() {
         try {
            Statement var1 = this.conn.createStatement();
            var1.executeUpdate(
               "CREATE TABLE IF NOT EXISTS MULTIPLE_ITEM_"
                  + this.table
                  + " (date REAL NOT NULL,uuid TEXT NOT NULL,items TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
            );
            var1.executeUpdate(
               "CREATE TABLE IF NOT EXISTS SINGLE_ITEM_"
                  + this.table
                  + " (date REAL NOT NULL,uuid TEXT NOT NULL,item TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
            );
            var1.close();
         } catch (SQLException var2) {
            var2.printStackTrace();
         }
      }

      private void loadTables() {
         HashSet<String> var1 = new HashSet<>();

         try {
            Statement var2 = this.conn.createStatement();

            try {
               ResultSet var3 = var2.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table';");

               try {
                  while (var3.next()) {
                     var1.add(var3.getString(1));
                  }
               } catch (Throwable var9) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (var3 != null) {
                  var3.close();
               }
            } catch (Throwable var10) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var7) {
                     var10.addSuppressed(var7);
                  }
               }

               throw var10;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (SQLException var11) {
            SendMessage.errorMessage("Failed to load transaction tables");
            var11.printStackTrace();
         }

         for (String var13 : var1) {
            try {
               TransactionLog.SimpleMonth var4 = TransactionLog.this.new SimpleMonth(var13);
               TransactionLog.Table var5 = this.tables
                  .stream()
                  .filter(var1x -> var1x.date.equals(var4))
                  .findFirst()
                  .orElse(TransactionLog.this.new Table(var4));
               if (var13.contains("MULTIPLE")) {
                  var5.multiple_items = var13;
               } else if (var13.contains("SINGLE")) {
                  var5.single_items = var13;
               }

               this.tables.remove(var5);
               this.tables.add(var5);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException var6) {
            }
         }

         Collections.sort(this.tables, new Comparator<TransactionLog.Table>() {
            public int compare(TransactionLog.Table var1, TransactionLog.Table var2) {
               return var1.date.compareTo(var2.date);
            }
         });
      }

      private synchronized void executeBatch(List<TransactionLog.PendingEntry> var1) {
         if (var1.isEmpty()) {
            return;
         }

         try {
            for (TransactionLog.PendingEntry var4 : var1) {
               PreparedStatement var5 = this.conn
                  .prepareStatement("INSERT INTO " + var4.table + " (date, uuid, " + var4.column + ", prices, amount, action, method) VALUES(?,?,?,?,?,?,?);");

               try {
                  var5.setLong(1, var4.date);
                  var5.setString(2, var4.uuid);
                  var5.setString(3, var4.payload);
                  var5.setString(4, var4.prices);
                  var5.setInt(5, var4.amount);
                  var5.setString(6, var4.action);
                  var5.setString(7, var4.method);
                  var5.executeUpdate();
               } finally {
                  var5.close();
               }
            }
         } catch (SQLException var6) {
            SendMessage.errorMessage("Failed to write " + var1.size() + " transaction log entries: " + var6.getMessage());
            var6.printStackTrace();
         }
      }

      private void cacheMultiple(String var1, String var2, String var3, int var4, Transaction.Type var5, Transaction.Mode var6) {
         this.multiple_item_transactions
            .add(new TransactionLog.PendingEntry("MULTIPLE_ITEM_" + this.table, "items", var1, var2, var3, var4, var6.name(), var5.enumName()));
         if (this.multiple_item_transactions.size() >= 10) {
            List<TransactionLog.PendingEntry> var7 = new ArrayList<>(this.multiple_item_transactions);
            this.multiple_item_transactions.clear();
            TransactionLog.this.plugin.runTaskAsync(() -> this.executeBatch(var7));
         }
      }

      private void cacheSingle(String var1, String var2, String var3, int var4, Transaction.Type var5, Transaction.Mode var6) {
         this.single_item_transactions
            .add(new TransactionLog.PendingEntry("SINGLE_ITEM_" + this.table, "item", var1, var2, var3, var4, var6.name(), var5.enumName()));
         if (this.single_item_transactions.size() >= 10) {
            List<TransactionLog.PendingEntry> var7 = new ArrayList<>(this.single_item_transactions);
            this.single_item_transactions.clear();
            TransactionLog.this.plugin.runTaskAsync(() -> this.executeBatch(var7));
         }
      }

      private ArrayList<TransactionLog.SimpleTransaction> getAllSimple() throws SQLException {
         ArrayList var1 = new ArrayList();
         LocalDate var2 = LocalDate.now().minusDays(30L);
         TransactionLog.SimpleMonth var3 = TransactionLog.this.new SimpleMonth(var2.getMonthValue(), var2.getYear());

         for (TransactionLog.Table var5 : this.tables) {
            if (!var5.date.isOlderThen(var3)) {
               this.appendSimpleTransactions(var1, var5);
            }
         }

         return var1;
      }

      private void appendSimpleTransactions(ArrayList<TransactionLog.SimpleTransaction> var1, TransactionLog.Table var2) throws SQLException {
         Statement var3 = null;
         ResultSet var4 = null;

         try {
            var3 = this.conn.createStatement();
            if (var2.single_items != null) {
               var4 = var3.executeQuery("SELECT * FROM " + var2.single_items + ";");

               while (var4.next()) {
                  var1.add(TransactionLog.this.new SimpleTransaction(var4.getLong(1), var4.getString(2), var4.getString(3), var4.getInt(5), var4.getString(6)));
               }
            }

            if (var2.multiple_items != null) {
               var4 = var3.executeQuery("SELECT * FROM " + var2.multiple_items + ";");

               while (var4.next()) {
                  long var5 = var4.getLong(1);
                  String var7 = var4.getString(2);
                  String var8 = var4.getString(6);

                  for (TransactionLog.Item var12 : (TransactionLog.Item[])new Gson().fromJson(var4.getString(3), TransactionLog.Item[].class)) {
                     var1.add(TransactionLog.this.new SimpleTransaction(var5, var7, var12.itemPath, var12.amount, var8));
                  }
               }
            }
         } finally {
            if (var4 != null) {
               var4.close();
            }

            if (var3 != null) {
               var3.close();
            }
         }
      }

      private TransactionLog.Table getTableFrom(TransactionLog.SimpleMonth var1) {
         return this.tables.stream().filter(var1x -> var1x.date.equals(var1)).findFirst().get();
      }

      private ArrayList<TransactionLog.AdvancedTransaction> getAllAdvanced(TransactionLog.SimpleMonth var1) throws SQLException {
         TransactionLog.Table var2 = this.getTableFrom(var1);
         ArrayList var3 = new ArrayList();
         Statement var4 = null;
         ResultSet var5 = null;

         try {
            var4 = this.conn.createStatement();
            if (var2.single_items != null) {
               var5 = var4.executeQuery("SELECT * FROM " + var2.single_items + ";");

               while (var5.next()) {
                  var3.add(
                     TransactionLog.this.new AdvancedTransaction(
                        var5.getLong(1),
                        var5.getString(2),
                        TransactionLog.this.new SingleItem(var5.getString(3), true),
                        TransactionLog.this.deserializePrices(var5.getString(4)),
                        var5.getInt(5),
                        var5.getString(6),
                        var5.getString(7)
                     )
                  );
               }
            }

            if (var2.multiple_items != null) {
               var5 = var4.executeQuery("SELECT * FROM " + var2.multiple_items + ";");

               while (var5.next()) {
                  var3.add(
                     TransactionLog.this.new AdvancedTransaction(
                        var5.getLong(1),
                        var5.getString(2),
                        TransactionLog.this.deserializeItems(var5.getString(3)),
                        TransactionLog.this.deserializePrices(var5.getString(4)),
                        var5.getInt(5),
                        var5.getString(6),
                        var5.getString(7)
                     )
                  );
               }
            }
         } finally {
            if (var5 != null) {
               var5.close();
            }

            if (var4 != null) {
               var4.close();
            }
         }

         return var3;
      }

      public void saveUnsaved() {
         List<TransactionLog.PendingEntry> var1 = new ArrayList<>(this.multiple_item_transactions);
         this.multiple_item_transactions.clear();
         this.executeBatch(var1);
         List<TransactionLog.PendingEntry> var2 = new ArrayList<>(this.single_item_transactions);
         this.single_item_transactions.clear();
         this.executeBatch(var2);
      }
   }

   private final class Format {
      String start = "";
      String ending = "";

      public Format() {
      }

      public String complete(String var1) {
         String var2 = this.start + var1 + this.ending;
         this.start = "";
         this.ending = "";
         return var2;
      }
   }

   public final class GeneralStats {
      int transactions;
      List<TransactionLog.SimplePrice> cashIn;
      List<TransactionLog.SimplePrice> cashOut;

      public GeneralStats(ArrayList<TransactionLog.AdvancedTransaction> nullx) {
         this.transactions = nullx.size();
         this.cashIn = TransactionLog.this.getCashFlow(nullx, Transaction.Mode.BUY);
         this.cashOut = TransactionLog.this.getCashFlow(nullx, Transaction.Mode.SELL);
      }
   }

   private class Item {
      private int amount;
      private String itemPath;

      public Item(Entry<ShopItem, Integer> nullx) {
         this.amount = (Integer)nullx.getValue();
         this.itemPath = ((ShopItem)nullx.getKey()).getItemPath();
      }
   }

   private final class Line {
      String text = "";

      public Line() {
      }

      public String get() {
         String var1 = this.text;
         this.text = "";
         return var1;
      }
   }

   public final class MultipleItem {
      int amount;
      String name;
      String item;
      Material mat;

      public MultipleItem(int nullx, String nullxx) {
         this.amount = nullx;

         try {
            ShopItem var4 = TransactionLog.this.plugin.getShopItem(nullxx);
            this.name = TransactionLog.this.formatHTMLColors(var4.getDisplayname());
            this.mat = var4.getItemToGive().getType();
         } catch (NullPointerException var5) {
            this.name = "Unknown item";
            this.mat = Material.BARRIER;
         }

         this.item = nullxx;
      }
   }

   public final class Player {
      public String name;
      UUID uuid;

      public Player(UUID nullx) {
         try {
            this.name = Bukkit.getOfflinePlayer(nullx).getName();
         } catch (NullPointerException var4) {
            this.name = "Unknown";
         }

         this.uuid = nullx;
      }
   }

   public final class Price {
      String formatted;
      String ecoType;
      double amount;

      public Price(String nullx, Double nullxx) {
         try {
            this.formatted = TransactionLog.this.formatHTMLColors(
               TransactionLog.this.plugin.formatPrice(TransactionLog.this.plugin.getEcoHandler().getLoaded(nullx), nullxx)
            );
         } catch (NullPointerException var5) {
            this.formatted = nullxx + " " + nullx;
         }

         this.amount = nullxx;
         this.ecoType = nullx;
      }

      public String getEcoType() {
         return this.ecoType;
      }

      public double getAmount() {
         return this.amount;
      }
   }

   public class SimpleMonth implements Comparable<TransactionLog.SimpleMonth> {
      private final int month;
      private final int year;

      public SimpleMonth(String nullx) {
         String var3 = nullx.split("_ITEM_TRANSACTIONS_")[1];
         this.month = Integer.parseInt(var3.split("_")[0]);
         this.year = Integer.parseInt(var3.split("_")[1]);
      }

      public SimpleMonth(int nullx, int nullxx) {
         this.month = nullx;
         this.year = nullxx;
      }

      public boolean isLaterThen(TransactionLog.SimpleMonth var1) {
         return this.year > var1.year || this.year == var1.year && this.month > var1.month;
      }

      public boolean isOlderThen(TransactionLog.SimpleMonth var1) {
         return this.year < var1.year || this.year == var1.year && this.month < var1.month;
      }

      public int compareTo(@NotNull TransactionLog.SimpleMonth var1) {
         return this.isLaterThen(var1) ? 1 : (this.isOlderThen(var1) ? -1 : 0);
      }

      @Override
      public String toString() {
         return this.month + "/" + this.year;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            TransactionLog.SimpleMonth var2 = (TransactionLog.SimpleMonth)var1;
            return this.month == var2.month && this.year == var2.year;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.month, this.year);
      }
   }

   private final class SimplePrice {
      String formatted;

      public SimplePrice(String nullx) {
         this.formatted = nullx;
      }
   }

   public final class SimpleTransaction {
      public final long date;
      public final UUID uuid;
      public final int amount;
      public final String itemPath;
      public final Transaction.Mode action;

      public SimpleTransaction(long nullx, String nullxx, String nullxxx, int nullxxxx, String nullxxxxx) {
         this.date = nullx;
         this.uuid = UUID.fromString(nullxx);
         this.itemPath = nullxxx;
         this.amount = nullxxxx;
         this.action = Transaction.Mode.valueOf(nullxxxxx);
      }
   }

   public final class SingleItem {
      public String name;
      String item;
      Material mat;

      public SingleItem(String nullx, boolean nullxx) {
         try {
            ShopItem var4 = TransactionLog.this.plugin.getShopItem(nullx);
            this.name = nullxx ? TransactionLog.this.formatHTMLColors(var4.getDisplayname()) : var4.getDisplayname();
            this.mat = var4.getItemToGive().getType();
         } catch (NullPointerException var5) {
            this.name = "Unknown item";
            this.mat = Material.BARRIER;
         }

         this.item = nullx;
      }
   }

   public class Table {
      public TransactionLog.SimpleMonth date;
      public String single_items;
      public String multiple_items;

      public Table(TransactionLog.SimpleMonth nullx) {
         this.date = nullx;
      }

      public TransactionLog.Table withSingle(String var1) {
         this.single_items = var1;
         return this;
      }

      public TransactionLog.Table withMultiple(String var1) {
         this.multiple_items = var1;
         return this;
      }
   }

   public static enum TimeFrame {
      DAY,
      WEEK,
      MONTH;

      private TimeFrame() {
      }
   }

   private static final class PendingEntry {
      private final String table;
      private final String column;
      private final long date;
      private final String uuid;
      private final String payload;
      private final String prices;
      private final int amount;
      private final String action;
      private final String method;

      private PendingEntry(String var1, String var2, String var3, String var4, String var5, int var6, String var7, String var8) {
         this.table = var1;
         this.column = var2;
         this.date = Instant.now().toEpochMilli();
         this.uuid = var3;
         this.payload = var4;
         this.prices = var5;
         this.amount = var6;
         this.action = var7;
         this.method = var8;
      }
   }

   public class TopItem {
      public final TransactionLog.SingleItem item;
      public final int amount;

      public TopItem(String nullx, int nullxx, boolean nullxxx) {
         this.item = TransactionLog.this.new SingleItem(nullx, nullxxx);
         this.amount = nullxx;
      }
   }

   public class TopPlayer {
      public final TransactionLog.Player player;
      public final int amount;

      public TopPlayer(UUID nullx, int nullxx) {
         this.player = TransactionLog.this.new Player(nullx);
         this.amount = nullxx;
      }
   }
}
