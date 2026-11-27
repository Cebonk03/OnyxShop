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

   public TransactionLog(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public boolean setup() {
      if (this.db != null) {
         this.saveUnsaved();
      }

      this.file = new File(this.plugin.getDataFolder(), "transaction-log.txt");
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException exception) {
            SendMessage.errorMessage("Failed to create the transactions log file");
            return false;
         }
      }

      try {
         this.db = new TransactionLog.Database();
         return true;
      } catch (IOException exception) {
         SendMessage.warnMessage("Failed to create transaction directory...");
         return false;
      }
   }

   public void log(String text) {
      this.cache.add("[" + this.dateFormatter.format(new Date().getTime()) + "] - " + text);
   }

   public void log(UUID uniqueId, ShopItem shopItem, Map<EcoType, Double> map, int value, Transaction.Mode mode, Transaction.Type type) {
      this.db.cacheSingle(uniqueId.toString(), shopItem.getItemPath(), new Gson().toJson(map), value, type, mode);
      if (this.TRANSACTION_CACHE != null) {
         this.TRANSACTION_CACHE.add(new TransactionLog.SimpleTransaction(Instant.now().toEpochMilli(), uniqueId.toString(), shopItem.getItemPath(), value, mode.name()));
      }
   }

   public void log(UUID uniqueId, Map<ShopItem, Integer> map, Map<EcoType, Double> map2, int value, Transaction.Mode mode, Transaction.Type type) {
      String text = new Gson()
         .toJson(map.entrySet().stream().map(element -> new TransactionLog.Item((Entry<ShopItem, Integer>)element)).collect(Collectors.toList()));
      String text2 = new Gson().toJson(map2);
      this.db.cacheMultiple(uniqueId.toString(), text, text2, value, type, mode);
      if (this.TRANSACTION_CACHE != null) {
         long value2 = Instant.now().toEpochMilli();

         for (Entry entry : map.entrySet()) {
            this.TRANSACTION_CACHE
               .add(
                  new TransactionLog.SimpleTransaction(value2, uniqueId.toString(), ((ShopItem)entry.getKey()).getItemPath(), (Integer)entry.getValue(), mode.name())
               );
         }
      }
   }

   public Collection<TransactionLog.SimpleTransaction> getTransactions(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode) {
      long[] timeFrameRange = this.getTimeFrameRange(timeFrame);
      return this.TRANSACTION_CACHE
         .stream()
         .filter(element -> element.action == mode && element.date >= timeFrameRange[0] && element.date <= timeFrameRange[1])
         .collect(Collectors.toList());
   }

   private long[] getTimeFrameRange(TransactionLog.TimeFrame timeFrame) {
      long time = Instant.now().toEpochMilli();
      if (timeFrame == TransactionLog.TimeFrame.MONTH) {
         return new long[]{time - TimeUnit.DAYS.toMillis(30L), time};
      } else if (timeFrame == TransactionLog.TimeFrame.WEEK) {
         return new long[]{time - TimeUnit.DAYS.toMillis(7L), time};
      } else {
         long time2 = TimeUnit.DAYS.toMillis(1L);
         long startOfDay = this.getStartOfDay();
         return new long[]{startOfDay, startOfDay + time2};
      }
   }

   private long getStartOfDay() {
      LocalDateTime localDateTime = LocalDateTime.now();
      LocalDateTime localDateTime2 = localDateTime.toLocalDate().atTime(LocalTime.MIN);
      Instant instant = localDateTime2.atZone(ZoneId.systemDefault()).toInstant();
      return instant.toEpochMilli();
   }

   private void save() {
      if (!this.cache.isEmpty()) {
         try {
            Files.write(this.file.toPath(), this.cache, StandardOpenOption.APPEND);
            this.cache.clear();
         } catch (IOException exception) {
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
         } catch (Exception exception) {
            SendMessage.warnMessage("A unknown error occurred while loading transaction cache from database:\n" + this.stackTraceToString(exception));
         }
      }
   }

   public String exportLogs() {
      this.loadCache();
      ArrayList values = this.plugin.getTransactionLog().retrieveAllTransactions();
      Gson gson = new Gson();
      StringBuilder builder = new StringBuilder();
      builder.append("{\"generalStats\":" + gson.toJson(new TransactionLog.GeneralStats(values)) + "},");
      StringBuilder builder2 = new StringBuilder();
      builder2.append("{\"topPlayers\":[{\"PBM\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY)) + "},");
      builder2.append("{\"PBW\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY)) + "},");
      builder2.append("{\"PBD\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY)) + "},");
      builder2.append("{\"PSM\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL)) + "},");
      builder2.append("{\"PSW\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL)) + "},");
      builder2.append("{\"PSD\":" + gson.toJson(this.getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL)) + "}]},");
      StringBuilder builder3 = new StringBuilder();
      builder3.append("{\"topItems\":[{\"IBM\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY, true)) + "},");
      builder3.append("{\"IBW\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY, true)) + "},");
      builder3.append("{\"IBD\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY, true)) + "},");
      builder3.append("{\"ISM\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL, true)) + "},");
      builder3.append("{\"ISW\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL, true)) + "},");
      builder3.append("{\"ISD\":" + gson.toJson(this.getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL, true)) + "}]}");
      String text = gson.toJson(values, ArrayList.class);
      return "{\"stats\":["
         + builder
         + builder2
         + builder3
         + "],\"dates\":"
         + Arrays.toString(this.db.getAllLoaded().stream().map(element -> "\"" + element.toString() + "\"").toArray())
         + ",\"logs\":"
         + text
         + "}";
   }

   private List<TransactionLog.SimplePrice> getCashFlow(ArrayList<TransactionLog.AdvancedTransaction> advancedTransactions, Transaction.Mode mode) {
      Map<String, Double> map = advancedTransactions.stream()
         .filter(element -> element.action == mode)
         .flatMap(element -> element.prices.stream())
         .collect(Collectors.groupingBy(TransactionLog.Price::getEcoType, Collectors.summingDouble(TransactionLog.Price::getAmount)));
      ArrayList values = new ArrayList();

      for (String key : map.keySet()) {
         String price = this.plugin.formatNullablePrice(key, (Double)map.get(key));
         if (price != null) {
            values.add(new TransactionLog.SimplePrice(this.formatHTMLColors(price)));
         }
      }

      return values;
   }

   public ArrayList<TransactionLog.TopPlayer> getTopThreePlayers(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode) {
      HashMap<UUID, Integer> map = new HashMap<>();

      for (TransactionLog.SimpleTransaction simpleTransaction : this.getTransactions(timeFrame, mode)) {
         UUID uniqueId = simpleTransaction.uuid;
         int amount = simpleTransaction.amount;
         map.put(uniqueId, map.getOrDefault(uniqueId, 0) + amount);
      }

      ArrayList values = new ArrayList(map.entrySet());
      values.sort(Entry.comparingByValue().reversed());
      ArrayList values2 = new ArrayList();

      for (int index = 0; index < 10; index++) {
         try {
            Entry entry = (Entry)values.get(index);
            values2.add(new TransactionLog.TopPlayer((UUID)entry.getKey(), (Integer)entry.getValue()));
         } catch (IndexOutOfBoundsException exception) {
            break;
         }
      }

      return values2;
   }

   public ArrayList<TransactionLog.TopItem> getTopThreeItems(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode) {
      return this.getTopThreeItems(timeFrame, mode, false);
   }

   private ArrayList<TransactionLog.TopItem> getTopThreeItems(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode, boolean flag) {
      HashMap<String, Integer> map = new HashMap<>();

      for (TransactionLog.SimpleTransaction simpleTransaction : this.getTransactions(timeFrame, mode)) {
         String itemId = simpleTransaction.itemPath;
         int amount = simpleTransaction.amount;
         map.put(itemId, map.getOrDefault(itemId, 0) + amount);
      }

      ArrayList values = new ArrayList(map.entrySet());
      values.sort(Entry.comparingByValue().reversed());
      ArrayList values2 = new ArrayList();

      for (int index = 0; index < 10; index++) {
         try {
            Entry entry = (Entry)values.get(index);
            values2.add(new TransactionLog.TopItem((String)entry.getKey(), (Integer)entry.getValue(), flag));
         } catch (IndexOutOfBoundsException exception) {
            break;
         }
      }

      return values2;
   }

   public int getItemTransactions(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, String itemId, String text, UUID uniqueId) {
      long[] timeFrameRange = this.getTimeFrameRange(timeFrame);
      int value = 0;

      for (TransactionLog.SimpleTransaction simpleTransaction : this.TRANSACTION_CACHE) {
         if (simpleTransaction.action == mode
            && simpleTransaction.date >= timeFrameRange[0]
            && simpleTransaction.date <= timeFrameRange[1]
            && (uniqueId == null || simpleTransaction.uuid.equals(uniqueId))
            && (itemId == null || (text == null ? simpleTransaction.itemPath.startsWith(itemId + ".") : simpleTransaction.itemPath.equals(itemId + "." + text)))) {
            value += simpleTransaction.amount;
         }
      }

      return value;
   }

   public ArrayList<TransactionLog.AdvancedTransaction> retrieveAllTransactions() {
      ArrayList values = new ArrayList();

      for (TransactionLog.SimpleMonth simpleMonth : this.db.getAllLoaded()) {
         try {
            values.addAll(this.db.getAllAdvanced(simpleMonth));
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to retrieve logged transactions from " + simpleMonth.month + "/" + simpleMonth.year);
            exception.printStackTrace();
         }
      }

      Collections.sort(values, new Comparator<TransactionLog.AdvancedTransaction>() {
         public int compare(TransactionLog.AdvancedTransaction advancedTransaction, TransactionLog.AdvancedTransaction advancedTransaction2) {
            return Long.compare(advancedTransaction2.date, advancedTransaction.date);
         }
      });
      return values;
   }

   private String stackTraceToString(Exception exception) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      return stringWriter.toString();
   }

   private String formatHTMLColors(String text) {
      StringBuilder builder = new StringBuilder();
      TransactionLog.Line line = new TransactionLog.Line();
      TransactionLog.Color color = new TransactionLog.Color();
      TransactionLog.Format format = new TransactionLog.Format();
      boolean flag = false;

      try {
         for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == 167) {
               char character2 = text.charAt(index + 1);
               if (character2 == 'x' && text.charAt(index + 8) == 167) {
                  if (!color.c.isEmpty()) {
                     if (!format.start.isEmpty()) {
                        builder.append(format.complete(line.get()));
                     } else {
                        builder.append(line.get());
                     }

                     builder.append(color.get());
                  }

                  builder.append("<a style=\"color:" + this.getRGB(text, index) + "\">");
                  color.c = "</a>";
                  index += 13;
               } else {
                  ChatColor chatColor = ChatColor.getByChar(character2);
                  if (chatColor != null) {
                     if (chatColor.isColor()) {
                        if (!color.c.isEmpty()) {
                           if (!format.start.isEmpty()) {
                              builder.append(format.complete(line.get()));
                           } else {
                              builder.append(line.get());
                           }

                           builder.append(color.get());
                        }

                        builder.append("<a style=\"color:" + chatColor.name().toLowerCase(Locale.ROOT).replace("_", "") + "\">");
                        color.c = "</a>";
                        index++;
                     } else if (chatColor.isFormat()) {
                        String text2 = this.getFormat(chatColor);
                        if (text2 != null) {
                           if (!format.start.isEmpty()) {
                              builder.append(format.complete(line.get()));
                           } else {
                              builder.append(line.get());
                           }

                           format.start = format.start + "<" + text2 + ">";
                           format.ending = "</" + text2 + ">" + format.ending;
                           index++;
                        } else {
                           line.text = line.text + character;
                        }
                     } else if (chatColor == ChatColor.RESET) {
                        this.closeElement(builder, color, format, line);
                        index++;
                     }
                  }
               }
            } else {
               line.text = line.text + character;
            }
         }

         if (!format.start.isEmpty()) {
            builder.append(format.complete(line.get()));
         } else {
            builder.append(line.get());
         }

         if (!color.c.isEmpty()) {
            builder.append(color.get());
         }
      } catch (IndexOutOfBoundsException exception) {
      }

      return builder.toString();
   }

   private void closeElement(StringBuilder builder, TransactionLog.Color color, TransactionLog.Format format, TransactionLog.Line line) {
      if (!color.c.isEmpty()) {
         if (!format.start.isEmpty()) {
            builder.append(format.complete(line.get()));
         } else {
            builder.append(line.get());
         }

         builder.append(color.get());
      } else {
         if (!format.start.isEmpty()) {
            builder.append(format.complete(line.get()));
         } else {
            builder.append(line.get());
         }

         builder.append(line.get());
      }
   }

   private String getFormat(ChatColor chatColor) {
      switch (chatColor) {
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

   private String getRGB(String text, int value) {
      return text.substring(value, value + 14).replace("§", "").replace("x", "#");
   }

   private ArrayList<TransactionLog.Price> deserializePrices(String price) {
      JsonObject jsonObject = (JsonObject)new Gson().fromJson(price, JsonObject.class);
      return jsonObject.entrySet()
         .stream()
         .map(entry -> new TransactionLog.Price((String)entry.getKey(), Double.parseDouble(((JsonElement)entry.getValue()).getAsString())))
         .collect(Collectors.toCollection(ArrayList::new));
   }

   private List<TransactionLog.MultipleItem> deserializeItems(String itemId) {
      try {
         JSONArray jsonArray = JsonUtil.parseArray(itemId);
         return ((Stream<Object>)jsonArray.stream()).map(element -> {
            try {
               JSONObject jsonObject = JsonUtil.parseJson(element.toString());
               return new TransactionLog.MultipleItem(Integer.valueOf(jsonObject.get("amount").toString()), jsonObject.get("itemPath").toString());
            } catch (ParseException exception) {
               SendMessage.warnMessage("Failed to parse item '" + itemId + "' for transaction, skipping...");
               exception.printStackTrace();
               return new TransactionLog.MultipleItem(0, "unknown");
            }
         }).collect(Collectors.toList());
      } catch (ParseException exception) {
         SendMessage.errorMessage("Failed to parse item(s) '" + itemId + "' for transaction, skipping...");
         exception.printStackTrace();
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
         long price, String text, Object value, ArrayList<TransactionLog.Price> prices, int value2, String text2, String text3
      ) {
         this.date = price;
         this.player = TransactionLog.this.new Player(UUID.fromString(text));
         this.items = value;
         this.prices = prices;
         this.amount = value2;
         this.action = Transaction.Mode.valueOf(text2);
         this.type = text3;
      }
   }

   private final class Color {
      String c = "";

      public Color() {
      }

      public String get() {
         String c = this.c;
         this.c = "";
         return c;
      }

      public void addColor(String text) {
         this.c = text + this.c;
      }
   }

   private class Database {
      private Connection conn;
      private final String table;
      private final LinkedList<TransactionLog.Table> tables = new LinkedList<>();
      private final ArrayList<TransactionLog.PendingEntry> multiple_item_transactions = new ArrayList<>();
      private final ArrayList<TransactionLog.PendingEntry> single_item_transactions = new ArrayList<>();

      public List<TransactionLog.SimpleMonth> getAllLoaded() {
         return this.tables.stream().map(element -> element.date).collect(Collectors.toList());
      }

      public List<TransactionLog.SimpleMonth> getAllSince(TransactionLog.SimpleMonth simpleMonth) {
         return this.getAllLoaded().stream().filter(element -> element.equals(simpleMonth) || element.isLaterThen(simpleMonth)).collect(Collectors.toList());
      }

      public Database() throws IOException {
         Calendar calendar = Calendar.getInstance();
         this.table = "TRANSACTIONS_" + (calendar.get(2) + 1) + "_" + calendar.get(1);

         try {
            Files.createDirectory(Paths.get(TransactionLog.this.plugin.getDataFolder() + File.separator + "transactions"));
         } catch (FileAlreadyExistsException exception) {
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
         } catch (Exception exception) {
            SendMessage.errorMessage(Lang.ERROR_OCCURRED_WHILE_CONNECTING_TO_DATABASE.get());
            exception.printStackTrace();
            return false;
         }
      }

      private void checkTableConstraints() {
         try {
            boolean flag = false;
            Statement statement = this.conn.createStatement();
            ResultSet resultSet = statement.executeQuery("PRAGMA index_list('MULTIPLE_ITEM_" + this.table + "');");
            if (resultSet.next()) {
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS MULTIPLE_ITEM_"
                     + this.table
                     + "_NEW (date REAL NOT NULL,uuid TEXT NOT NULL,items TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
               );
               statement.executeUpdate(
                  "INSERT INTO MULTIPLE_ITEM_"
                     + this.table
                     + "_NEW (date, uuid, items, prices, amount, action, method) SELECT date, uuid, items, prices, amount, action, method FROM MULTIPLE_ITEM_"
                     + this.table
                     + ";"
               );
               statement.executeUpdate("DROP TABLE MULTIPLE_ITEM_" + this.table + ";");
               statement.executeUpdate("ALTER TABLE MULTIPLE_ITEM_" + this.table + "_NEW RENAME TO MULTIPLE_ITEM_" + this.table + ";");
               flag = true;
            }

            ResultSet resultSet2 = statement.executeQuery("PRAGMA index_list('SINGLE_ITEM_" + this.table + "');");
            if (resultSet2.next()) {
               statement.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS SINGLE_ITEM_"
                     + this.table
                     + "_NEW (date REAL NOT NULL,uuid TEXT NOT NULL,item TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
               );
               statement.executeUpdate(
                  "INSERT INTO SINGLE_ITEM_"
                     + this.table
                     + "_NEW (date, uuid, item, prices, amount, action, method) SELECT date, uuid, item, prices, amount, action, method FROM SINGLE_ITEM_"
                     + this.table
                     + ";"
               );
               statement.executeUpdate("DROP TABLE SINGLE_ITEM_" + this.table + ";");
               statement.executeUpdate("ALTER TABLE SINGLE_ITEM_" + this.table + "_NEW RENAME TO SINGLE_ITEM_" + this.table + ";");
               flag = true;
            }

            if (flag) {
               SendMessage.infoMessage("Successfully removed unique constraint from transaction db tables");
            }
         } catch (SQLException exception) {
            SendMessage.warnMessage("Failed to migrate database to new format for " + this.table);
            exception.printStackTrace();
         }
      }

      private void createTable() {
         try {
            Statement statement = this.conn.createStatement();
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS MULTIPLE_ITEM_"
                  + this.table
                  + " (date REAL NOT NULL,uuid TEXT NOT NULL,items TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
            );
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS SINGLE_ITEM_"
                  + this.table
                  + " (date REAL NOT NULL,uuid TEXT NOT NULL,item TEXT NOT NULL,prices TEXT NOT NULL,amount INTEGER NOT NULL,action TEXT NOT NULL,method TEXT NOT NULL);"
            );
            statement.close();
         } catch (SQLException exception) {
            exception.printStackTrace();
         }
      }

      private void loadTables() {
         HashSet<String> values = new HashSet<>();

         try {
            Statement statement = this.conn.createStatement();

            try {
               ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table';");

               try {
                  while (resultSet.next()) {
                     values.add(resultSet.getString(1));
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
         } catch (SQLException exception) {
            SendMessage.errorMessage("Failed to load transaction tables");
            exception.printStackTrace();
         }

         for (String text : values) {
            try {
               TransactionLog.SimpleMonth simpleMonth = TransactionLog.this.new SimpleMonth(text);
               TransactionLog.Table table = this.tables
                  .stream()
                  .filter(element -> element.date.equals(simpleMonth))
                  .findFirst()
                  .orElse(TransactionLog.this.new Table(simpleMonth));
               if (text.contains("MULTIPLE")) {
                  table.multiple_items = text;
               } else if (text.contains("SINGLE")) {
                  table.single_items = text;
               }

               this.tables.remove(table);
               this.tables.add(table);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            }
         }

         Collections.sort(this.tables, new Comparator<TransactionLog.Table>() {
            public int compare(TransactionLog.Table table, TransactionLog.Table table2) {
               return table.date.compareTo(table2.date);
            }
         });
      }

      private synchronized void executeBatch(List<TransactionLog.PendingEntry> values) {
         if (values.isEmpty()) {
            return;
         }

         try {
            for (TransactionLog.PendingEntry pendingEntry : values) {
               PreparedStatement statement = this.conn
                  .prepareStatement("INSERT INTO " + pendingEntry.table + " (date, uuid, " + pendingEntry.column + ", prices, amount, action, method) VALUES(?,?,?,?,?,?,?);");

               try {
                  statement.setLong(1, pendingEntry.date);
                  statement.setString(2, pendingEntry.uuid);
                  statement.setString(3, pendingEntry.payload);
                  statement.setString(4, pendingEntry.prices);
                  statement.setInt(5, pendingEntry.amount);
                  statement.setString(6, pendingEntry.action);
                  statement.setString(7, pendingEntry.method);
                  statement.executeUpdate();
               } finally {
                  statement.close();
               }
            }
         } catch (SQLException exception) {
            SendMessage.errorMessage("Failed to write " + values.size() + " transaction log entries: " + exception.getMessage());
            exception.printStackTrace();
         }
      }

      private void cacheMultiple(String type, String text, String text2, int value, Transaction.Type type2, Transaction.Mode mode) {
         this.multiple_item_transactions
            .add(new TransactionLog.PendingEntry("MULTIPLE_ITEM_" + this.table, "items", type, text, text2, value, mode.name(), type2.enumName()));
         if (this.multiple_item_transactions.size() >= 10) {
            List<TransactionLog.PendingEntry> values = new ArrayList<>(this.multiple_item_transactions);
            this.multiple_item_transactions.clear();
            TransactionLog.this.plugin.runTaskAsync(() -> this.executeBatch(values));
         }
      }

      private void cacheSingle(String type, String text, String text2, int value, Transaction.Type type2, Transaction.Mode mode) {
         this.single_item_transactions
            .add(new TransactionLog.PendingEntry("SINGLE_ITEM_" + this.table, "item", type, text, text2, value, mode.name(), type2.enumName()));
         if (this.single_item_transactions.size() >= 10) {
            List<TransactionLog.PendingEntry> values = new ArrayList<>(this.single_item_transactions);
            this.single_item_transactions.clear();
            TransactionLog.this.plugin.runTaskAsync(() -> this.executeBatch(values));
         }
      }

      private ArrayList<TransactionLog.SimpleTransaction> getAllSimple() throws SQLException {
         ArrayList values = new ArrayList();
         LocalDate localDate = LocalDate.now().minusDays(30L);
         TransactionLog.SimpleMonth simpleMonth = TransactionLog.this.new SimpleMonth(localDate.getMonthValue(), localDate.getYear());

         for (TransactionLog.Table table : this.tables) {
            if (!table.date.isOlderThen(simpleMonth)) {
               this.appendSimpleTransactions(values, table);
            }
         }

         return values;
      }

      private void appendSimpleTransactions(ArrayList<TransactionLog.SimpleTransaction> simpleTransactions, TransactionLog.Table table) throws SQLException {
         Statement statement = null;
         ResultSet resultSet = null;

         try {
            statement = this.conn.createStatement();
            if (table.single_items != null) {
               resultSet = statement.executeQuery("SELECT * FROM " + table.single_items + ";");

               while (resultSet.next()) {
                  simpleTransactions.add(TransactionLog.this.new SimpleTransaction(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getInt(5), resultSet.getString(6)));
               }
            }

            if (table.multiple_items != null) {
               resultSet = statement.executeQuery("SELECT * FROM " + table.multiple_items + ";");

               while (resultSet.next()) {
                  long value = resultSet.getLong(1);
                  String text = resultSet.getString(2);
                  String text2 = resultSet.getString(6);

                  for (TransactionLog.Item item : (TransactionLog.Item[])new Gson().fromJson(resultSet.getString(3), TransactionLog.Item[].class)) {
                     simpleTransactions.add(TransactionLog.this.new SimpleTransaction(value, text, item.itemPath, item.amount, text2));
                  }
               }
            }
         } finally {
            if (resultSet != null) {
               resultSet.close();
            }

            if (statement != null) {
               statement.close();
            }
         }
      }

      private TransactionLog.Table getTableFrom(TransactionLog.SimpleMonth simpleMonth) {
         return this.tables.stream().filter(element -> element.date.equals(simpleMonth)).findFirst().get();
      }

      private ArrayList<TransactionLog.AdvancedTransaction> getAllAdvanced(TransactionLog.SimpleMonth simpleMonth) throws SQLException {
         TransactionLog.Table table = this.getTableFrom(simpleMonth);
         ArrayList values = new ArrayList();
         Statement statement = null;
         ResultSet resultSet = null;

         try {
            statement = this.conn.createStatement();
            if (table.single_items != null) {
               resultSet = statement.executeQuery("SELECT * FROM " + table.single_items + ";");

               while (resultSet.next()) {
                  values.add(
                     TransactionLog.this.new AdvancedTransaction(
                        resultSet.getLong(1),
                        resultSet.getString(2),
                        TransactionLog.this.new SingleItem(resultSet.getString(3), true),
                        TransactionLog.this.deserializePrices(resultSet.getString(4)),
                        resultSet.getInt(5),
                        resultSet.getString(6),
                        resultSet.getString(7)
                     )
                  );
               }
            }

            if (table.multiple_items != null) {
               resultSet = statement.executeQuery("SELECT * FROM " + table.multiple_items + ";");

               while (resultSet.next()) {
                  values.add(
                     TransactionLog.this.new AdvancedTransaction(
                        resultSet.getLong(1),
                        resultSet.getString(2),
                        TransactionLog.this.deserializeItems(resultSet.getString(3)),
                        TransactionLog.this.deserializePrices(resultSet.getString(4)),
                        resultSet.getInt(5),
                        resultSet.getString(6),
                        resultSet.getString(7)
                     )
                  );
               }
            }
         } finally {
            if (resultSet != null) {
               resultSet.close();
            }

            if (statement != null) {
               statement.close();
            }
         }

         return values;
      }

      public void saveUnsaved() {
         List<TransactionLog.PendingEntry> values = new ArrayList<>(this.multiple_item_transactions);
         this.multiple_item_transactions.clear();
         this.executeBatch(values);
         List<TransactionLog.PendingEntry> values2 = new ArrayList<>(this.single_item_transactions);
         this.single_item_transactions.clear();
         this.executeBatch(values2);
      }
   }

   private final class Format {
      String start = "";
      String ending = "";

      public Format() {
      }

      public String complete(String text) {
         String text2 = this.start + text + this.ending;
         this.start = "";
         this.ending = "";
         return text2;
      }
   }

   public final class GeneralStats {
      int transactions;
      List<TransactionLog.SimplePrice> cashIn;
      List<TransactionLog.SimplePrice> cashOut;

      public GeneralStats(ArrayList<TransactionLog.AdvancedTransaction> advancedTransactions) {
         this.transactions = advancedTransactions.size();
         this.cashIn = TransactionLog.this.getCashFlow(advancedTransactions, Transaction.Mode.BUY);
         this.cashOut = TransactionLog.this.getCashFlow(advancedTransactions, Transaction.Mode.SELL);
      }
   }

   private class Item {
      private int amount;
      private String itemPath;

      public Item(Entry<ShopItem, Integer> entry) {
         this.amount = (Integer)entry.getValue();
         this.itemPath = ((ShopItem)entry.getKey()).getItemPath();
      }
   }

   private final class Line {
      String text = "";

      public Line() {
      }

      public String get() {
         String text = this.text;
         this.text = "";
         return text;
      }
   }

   public final class MultipleItem {
      int amount;
      String name;
      String item;
      Material mat;

      public MultipleItem(int value, String itemId) {
         this.amount = value;

         try {
            ShopItem shopItem = TransactionLog.this.plugin.getShopItem(itemId);
            this.name = TransactionLog.this.formatHTMLColors(shopItem.getDisplayname());
            this.mat = shopItem.getItemToGive().getType();
         } catch (NullPointerException exception) {
            this.name = "Unknown item";
            this.mat = Material.BARRIER;
         }

         this.item = itemId;
      }
   }

   public final class Player {
      public String name;
      UUID uuid;

      public Player(UUID playerId) {
         try {
            this.name = Bukkit.getOfflinePlayer(playerId).getName();
         } catch (NullPointerException exception) {
            this.name = "Unknown";
         }

         this.uuid = playerId;
      }
   }

   public final class Price {
      String formatted;
      String ecoType;
      double amount;

      public Price(String price, Double amount) {
         try {
            this.formatted = TransactionLog.this.formatHTMLColors(
               TransactionLog.this.plugin.formatPrice(TransactionLog.this.plugin.getEcoHandler().getLoaded(price), amount)
            );
         } catch (NullPointerException exception) {
            this.formatted = amount + " " + price;
         }

         this.amount = amount;
         this.ecoType = price;
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

      public SimpleMonth(String text) {
         String text2 = text.split("_ITEM_TRANSACTIONS_")[1];
         this.month = Integer.parseInt(text2.split("_")[0]);
         this.year = Integer.parseInt(text2.split("_")[1]);
      }

      public SimpleMonth(int value, int value2) {
         this.month = value;
         this.year = value2;
      }

      public boolean isLaterThen(TransactionLog.SimpleMonth simpleMonth) {
         return this.year > simpleMonth.year || this.year == simpleMonth.year && this.month > simpleMonth.month;
      }

      public boolean isOlderThen(TransactionLog.SimpleMonth simpleMonth) {
         return this.year < simpleMonth.year || this.year == simpleMonth.year && this.month < simpleMonth.month;
      }

      public int compareTo(@NotNull TransactionLog.SimpleMonth simpleMonth) {
         return this.isLaterThen(simpleMonth) ? 1 : (this.isOlderThen(simpleMonth) ? -1 : 0);
      }

      @Override
      public String toString() {
         return this.month + "/" + this.year;
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (value != null && this.getClass() == value.getClass()) {
            TransactionLog.SimpleMonth simpleMonth = (TransactionLog.SimpleMonth)value;
            return this.month == simpleMonth.month && this.year == simpleMonth.year;
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

      public SimplePrice(String price) {
         this.formatted = price;
      }
   }

   public final class SimpleTransaction {
      public final long date;
      public final UUID uuid;
      public final int amount;
      public final String itemPath;
      public final Transaction.Mode action;

      public SimpleTransaction(long value, String text, String text2, int value2, String text3) {
         this.date = value;
         this.uuid = UUID.fromString(text);
         this.itemPath = text2;
         this.amount = value2;
         this.action = Transaction.Mode.valueOf(text3);
      }
   }

   public final class SingleItem {
      public String name;
      String item;
      Material mat;

      public SingleItem(String itemId, boolean flag) {
         try {
            ShopItem shopItem = TransactionLog.this.plugin.getShopItem(itemId);
            this.name = flag ? TransactionLog.this.formatHTMLColors(shopItem.getDisplayname()) : shopItem.getDisplayname();
            this.mat = shopItem.getItemToGive().getType();
         } catch (NullPointerException exception) {
            this.name = "Unknown item";
            this.mat = Material.BARRIER;
         }

         this.item = itemId;
      }
   }

   public class Table {
      public TransactionLog.SimpleMonth date;
      public String single_items;
      public String multiple_items;

      public Table(TransactionLog.SimpleMonth simpleMonth) {
         this.date = simpleMonth;
      }

      public TransactionLog.Table withSingle(String text) {
         this.single_items = text;
         return this;
      }

      public TransactionLog.Table withMultiple(String text) {
         this.multiple_items = text;
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

      private PendingEntry(String text, String text2, String text3, String text4, String text5, int value, String text6, String text7) {
         this.table = text;
         this.column = text2;
         this.date = Instant.now().toEpochMilli();
         this.uuid = text3;
         this.payload = text4;
         this.prices = text5;
         this.amount = value;
         this.action = text6;
         this.method = text7;
      }
   }

   public class TopItem {
      public final TransactionLog.SingleItem item;
      public final int amount;

      public TopItem(String itemId, int value, boolean flag) {
         this.item = TransactionLog.this.new SingleItem(itemId, flag);
         this.amount = value;
      }
   }

   public class TopPlayer {
      public final TransactionLog.Player player;
      public final int amount;

      public TopPlayer(UUID playerId, int value) {
         this.player = TransactionLog.this.new Player(playerId);
         this.amount = value;
      }
   }
}
