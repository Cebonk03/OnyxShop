package com.onyxshop.providers;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.Database;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.ScheduleCache;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.RotateHistory;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPageItems;
import com.onyxshop.objects.jobs.RotationJob;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.objects.titles.ShopTitle;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.SimplePair;
import com.onyxshop.util.SortOrder;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.exceptions.ShopLoadException;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

public class RotatingShopProvider extends RotatingShop {
   private final Map<Integer, ShopPageItems> shopPages = new HashMap<>();
   private final Map<Integer, List<Integer>> displaySlots;
   private final Random random = new Random();
   private final RotatingShopProvider.RotationTask task;
   private final int totalSlots;
   private final List<RotatingShopProvider.Item> displayChances;
   private final RotateHistory history;
   private final List<String> pages;
   private List<String> currentItems;
   private boolean repeatedRotation;
   private boolean canDuplicate = false;
   private boolean broadcast;

   public RotatingShopProvider(OnyxShop var1, String var2) throws ShopLoadException {
      super(var1, var2);
      super.setProvider(this);
      this.pages = this.initPages();
      this.displaySlots = this.getDisplaySlots();
      this.totalSlots = this.displaySlots.values().stream().mapToInt(List::size).sum();
      this.history = new RotateHistory((int)Math.ceil(this.getDisplayItems().size() / this.totalSlots));
      this.repeatedRotation = ConfigManager.getSection(this.getSection()).getBoolean("rotating-shop-settings.can-repeat", true);
      this.broadcast = ConfigManager.getSection(this.getSection()).getBoolean("rotating-shop-settings.broadcast", true);
      this.displayChances = this.getDisplayChances();
      if (this.totalSlots > this.getShopItems().size()) {
         SendMessage.warnMessage("RotatingShop " + this.getSection() + " has more display slots than there are shop items, disabling item chances...");
         this.repeatedRotation = false;
         this.canDuplicate = true;
      }

      try {
         this.task = new RotatingShopProvider.RotationTask();
         if (this.task.schedule instanceof RotatingShopProvider.ScheduleTypeInterval) {
            SendMessage.infoMessage(
               Lang.ENABLED_ROTATING_SHOP
                  .get()
                  .replace("%shop%", this.getSection())
                  .replace("%interval%", ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.interval"))
            );
         } else if (this.task.schedule instanceof RotatingShopProvider.ScheduleTypeFrequency) {
            SendMessage.infoMessage(
               Lang.ENABLED_ROTATING_SHOP_FREQUENCY
                  .get()
                  .replace("%shop%", this.getSection())
                  .replace("%frequency%", ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.frequency"))
                  .replace("%nextRotation%", Date.from(Instant.ofEpochMilli(this.task.schedule.getNextRotation())).toString())
            );
         } else if (this.task.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync) {
            SendMessage.infoMessage(Lang.ENABLED_ROTATING_SHOP_SYNC.get().replace("%shop%", this.getSection()));
         }
      } catch (IndexOutOfBoundsException var4) {
         throw new ShopLoadException(
            "Failed to load next rotation for shop %shop%, there are more display slots than there are shop items".replace("%shop%", this.getSection())
         );
      }
   }

   public List<String> getCurrentItems() {
      return this.currentItems;
   }

   public ShopPageItems getPage(int var1) {
      return this.shopPages.get(var1);
   }

   public int getPageSize() {
      return this.pages.size();
   }

   @Override
   public int getPageForShopItem(String var1) {
      int var2 = 1;

      for (ShopPageItems var4 : this.shopPages.values()) {
         if (var4.getItemOrder().contains(var1)) {
            return var2;
         }

         var2++;
      }

      return 1;
   }

   @Override
   public List<ShopItem> getPageItems(int var1) {
      return this.shopPages
         .get(var1)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(var1x -> this.getDisplayItem(this.getSection() + "." + var1x))
         .filter(var0 -> var0 instanceof ShopItem)
         .map(var0 -> (ShopItem)var0)
         .collect(Collectors.toList());
   }

   @Override
   public void stopCurrent() {
      this.task.end();
   }

   @Override
   public String getNextRotation() {
      return TimeUtils.getReadableTime(this.task.schedule.getNextRotation() - System.currentTimeMillis());
   }

   public void loadNextRotation(boolean var1) {
      if (this.task.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync) {
         SendMessage.warnMessage(Lang.ROTATING_SHOP_FORCE_ROTATE_BLOCKED.get().replace("%shop%", this.getSection()));
      } else {
         try {
            if (!var1) {
               SendMessage.logDebugMessage(Lang.LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
            }

            if (this.currentItems != null) {
               this.processItems(false);
            }

            if (this.plugin.rotatingShops && !this.pages.isEmpty()) {
               this.loadPages();
            } else {
               this.shopPages.put(1, new ShopPageItems(this.getSection()));
            }

            this.processItems(true);
            this.task.rotate();
            this.reopen();
            Database var2 = this.plugin.getProviderManager().getDatabase();
            if (var2 instanceof MySQL || var2 instanceof PostgreSQL) {
               var2.setRotatingShopItems(this.getSection(), this.currentItems, this.task.schedule.getNextRotation());
            }

            if (!var1 && this.broadcast) {
               SendMessage.broadcastMessage(
                  Lang.NEW_ITEMS_GLOBAL_MESSAGE
                     .get()
                     .replace("%shop%", this.getSection())
                     .replace("%shopName%", this.getShopName())
                     .replace("%interval%", this.getNextRotation())
               );
            }
         } catch (NullPointerException var3) {
            SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
            var3.printStackTrace();
         }
      }
   }

   private String getShopName() {
      return ChatUtil.formatColors(
         ConfigManager.getSection(this.getSection())
            .getString("item.name", ConfigManager.getSection(this.getSection()).getString("item.displayname", this.getSection()))
      );
   }

   private void loadPages() {
      try {
         this.currentItems = this.getNextRandomItems();
         int var1 = 1;
         int var2 = 0;

         for (String var4 : this.pages) {
            int var5 = this.getPageSize(var4);
            ArrayList var6 = new ArrayList();
            List var7 = this.displaySlots.get(var1);
            LinkedHashMap var8 = new LinkedHashMap();

            for (int var9 = 0; var9 < var5 + 1; var9++) {
               if (var7.contains(var9)) {
                  String var10 = this.currentItems.get(var2++);
                  var8.put(var9, this.getDisplayItem(var10).getShopItem());
                  var6.add(var10.split("\\.")[1] + "." + var10.split("\\.")[2]);
               } else {
                  var6.add(null);
               }
            }

            this.shopPages.put(var1, new ShopPageItems(var8, var6, this.getSection(), var5));
            var1++;
         }
      } catch (Exception var11) {
         SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
         var11.printStackTrace();
      }
   }

   private void loadPages(List<String> var1) {
      try {
         this.currentItems = var1;
         this.processItems(true);
         int var2 = 1;
         int var3 = 0;

         for (String var5 : this.pages) {
            int var6 = this.getPageSize(var5);
            ArrayList var7 = new ArrayList();
            List var8 = this.displaySlots.get(var2);
            LinkedHashMap var9 = new LinkedHashMap();

            for (int var10 = 0; var10 < var6 + 1; var10++) {
               if (var8.contains(var10)) {
                  String var11 = (String)var1.get(var3++);
                  var9.put(var10, this.getDisplayItem(var11).getShopItem());
                  var7.add(var11.split("\\.")[1] + "." + var11.split("\\.")[2]);
               } else {
                  var7.add(null);
               }
            }

            this.shopPages.put(var2, new ShopPageItems(var9, var7, this.getSection(), var6));
            var2++;
         }
      } catch (Exception var12) {
         SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
         var12.printStackTrace();
      }
   }

   private List<String> getNextRandomItems() {
      if (!this.repeatedRotation) {
         return this.getNextItems();
      } else {
         HashSet var1 = new HashSet();
         ArrayList var2 = new ArrayList();

         for (RotatingShopProvider.Item var4 : this.displayChances) {
            double var5 = this.random.nextDouble() * 100.0;
            if (var5 < var4.chance) {
               var1.add(var4.item);
            } else {
               var2.add(var4);
            }
         }

         List<String> var7 = new ArrayList<>(var1);
         if (var7.size() < this.totalSlots) {
            Collections.shuffle(var2, this.random);

            for (int var8 = 0; var8 < var2.size() && var7.size() < this.totalSlots; var8++) {
               var7.add(((RotatingShopProvider.Item)var2.get(var8)).item);
            }
         }

         if (var7.size() > this.totalSlots) {
            Collections.shuffle((List<?>)var7, this.random);
            var7 = var7.subList(0, this.totalSlots);
         }

         return (List<String>)var7;
      }
   }

   private String getRandom(List<RotatingShopProvider.Item> var1, double var2) {
      double var4 = 0.0;

      for (RotatingShopProvider.Item var7 : var1) {
         var4 += var7.chance;
         if (var2 <= var4) {
            return var7.item;
         }
      }

      throw new IllegalStateException();
   }

   private int chooseIndex(double[] var1, double var2) {
      double var4 = 0.0;

      for (int var6 = 0; var6 < var1.length; var6++) {
         var4 += var1[var6];
         if (var2 <= var4) {
            return var6;
         }
      }

      throw new IllegalStateException();
   }

   private List<String> getNextItems() {
      ArrayList var1 = new ArrayList();
      Random var2 = new Random();

      for (int var3 = 0; var3 < this.totalSlots; var3++) {
         String var4 = this.itemLocs.get(var2.nextInt(this.itemLocs.size()));

         while (!this.canDuplicate && (this.history.getHistory().contains(this.getSection() + "." + var4) || var1.contains(this.getSection() + "." + var4))) {
            var4 = this.itemLocs.get(var2.nextInt(this.itemLocs.size()));
         }

         var1.add(this.getSection() + "." + var4);
      }

      this.history.rotate(var1);
      return var1;
   }

   private void processItems(boolean var1) {
      for (String var3 : this.currentItems) {
         ShopItem var4 = this.getShopItem(var3);
         if (var4 != null && !var4.hasItemError() && !var4.isDisplayItem()) {
            ItemStack var8 = var4.getItemToGive();
            if (var8 == null) {
               continue;
            }

            boolean var5 = ConfigManager.getConfig().getBoolean("prioritize-rotating-shop-items", true);
            String var6 = var8.getType().name();
            List<ShopItem> var7 = this.plugin.shopItemsByMaterialName.getOrDefault(var6, new ArrayList<>());
            if (var1) {
               if (var5) {
                  var7.add(0, var4);
               } else {
                  var7.add(var4);
               }
            } else {
               var7.remove(var4);
            }

            if (!var5) {
               switch (SortOrder.fromConfig()) {
                  case DESCENDING_SELL_PRICE:
                     var7.sort((var0, var1x) -> Double.compare(var1x.getSellPrice(), var0.getSellPrice()));
                     break;
                  case ASCENDING_SELL_PRICE:
                     var7.sort((var0, var1x) -> Double.compare(var0.getSellPrice(), var1x.getSellPrice()));
               }
            }

            this.plugin.shopItemsByMaterialName.put(var6, var7);
         }
      }

      if (var1) {
         this.plugin.getQueryBuilder().rotate(this);
      }
   }

   private int getPageSize(String var1) {
      Config var2 = ConfigManager.getSection(this.getSection());
      if (var2.contains("rotating-shop-settings.pages." + var1 + ".display-slots")) {
         try {
            int var5 = Integer.parseInt(var2.getString("rotating-shop-settings.pages." + var1 + ".gui-rows"));
            if (var5 >= 1 && var5 <= 6) {
               return var5 * 9;
            }

            SendMessage.warnMessage(
               "Inventory size for page '" + var1 + "' from rotating shop '" + this.getSection() + "' has to be in range from 1-6, using default..."
            );
         } catch (NumberFormatException var4) {
            SendMessage.warnMessage("Invalid inventory size for page '" + var1 + "' from rotating shop '" + this.getSection() + "', using default...");
         }

         return 45;
      } else {
         int var3 = this.displaySlots.get(this.pages.indexOf(var1) + 1).size();
         return var3 <= 45 && var3 >= 1 ? CalculateAmount.getSlots(var3) : 45;
      }
   }

   private List<String> initPages() {
      LinkedList var1 = new LinkedList();
      Config var2 = ConfigManager.getSection(this.getSection());
      if (var2.contains("rotating-shop-settings.pages")) {
         int var3 = 1;
         ConfigurationSection var4 = var2.getConfigurationSection("rotating-shop-settings.pages");

         for (String var6 : var4.getKeys(false)) {
            if (var4.contains(var6 + ".display-slots")) {
               var1.add(var6);
            }

            this.pageTitles.put(var3, this.getPageTitle(var4, var6));
            var3++;
         }
      }

      return var1;
   }

   private ShopTitle getPageTitle(ConfigurationSection var1, String var2) {
      if (var1.contains(var2 + ".title")) {
         String var3 = var1.getString(var2 + ".title");
         Translatable var4 = null;
         if (var1.contains(var2 + ".bedrock-title")) {
            var4 = SendMessage.fromConfig(this.plugin.getInvTitle(var1.getString(var2 + ".bedrock-title")));
         }

         return new ShopTitle(SendMessage.fromConfig(this.plugin.getInvTitle(var3)), var4);
      } else {
         return this.title;
      }
   }

   private Map<Integer, List<Integer>> getDisplaySlots() {
      HashMap var1 = new HashMap();
      Config var2 = ConfigManager.getSection(this.getSection());

      for (int var3 = 0; var3 < this.pages.size(); var3++) {
         String var4 = this.pages.get(var3);
         List var5 = var2.getIntegerList("rotating-shop-settings.pages." + var4 + ".display-slots");

         for (int var6 = 0; var6 < var5.size(); var6++) {
            int var7 = (Integer)var5.get(var6);
            if (super.getStaticItems(var3 + 1) != null && super.getStaticItems(var3 + 1).containsKey(var7)) {
               SendMessage.warnMessage(
                  Lang.CANNOT_LOAD_DISPLAY_SLOT
                     .get()
                     .replace("%slot%", String.valueOf(var7))
                     .replace("%page%", String.valueOf(var3 + 1))
                     .replace("%shop%", this.getSection())
               );
               var5.remove(var6);
            }
         }

         var1.put(var3 + 1, var5);
      }

      return var1;
   }

   private List<RotatingShopProvider.Item> getDisplayChances() {
      ArrayList var1 = new ArrayList();
      ArrayList<String> var2 = new ArrayList<>(this.itemLocs);
      double var3 = 0.0;

      for (String var6 : this.itemLocs) {
         Double var7 = this.getDisplayChance(var6);
         if (var7 != null) {
            if (var7 < 0.01) {
               SendMessage.warnMessage(
                  "Invalid chance for item " + var6 + " from shop " + this.getSection() + ", should be greater then 0.01 but got " + var7 + ". Using 0.01..."
               );
               var7 = 0.01;
            }

            if (var7 > 100.0 - var3) {
               SendMessage.warnMessage(
                  "Invalid chances for items from shop "
                     + this.getSection()
                     + ", should be a sum of 100 but got "
                     + (var3 + var7)
                     + ". Trying to automatically correct value..."
               );
               if (var3 + var2.size() * 0.01 > 100.0) {
                  SendMessage.errorMessage(
                     "Failed to correct display chance automatically, disabling repeated rotations for shop "
                        + this.getSection()
                        + ". Please adjust the values manually for a total sum of 100(or nearly 100) to prevent this issue"
                  );
                  this.repeatedRotation = false;
                  return null;
               }

               double var15 = (100.0 - var3) / var2.size();
               SendMessage.warnMessage(
                  "Using "
                     + var15
                     + "% display chance for all left over items... Please adjust the values manually for a total sum of 100(or nearly 100) to prevent this issue"
               );

               for (String var11 : var2) {
                  RotatingShopProvider.Item var12 = new RotatingShopProvider.Item(this.getSection() + "." + var11, var15);
                  var1.add(var12);
               }

               return var1;
            }

            RotatingShopProvider.Item var8 = new RotatingShopProvider.Item(this.getSection() + "." + var6, var7);
            var1.add(var8);
            var2.remove(var6);
            var3 += var8.chance;
         }
      }

      if (!var2.isEmpty()) {
         double var13 = (100.0 - var3) / var2.size();
         if (var13 < 0.01) {
            SendMessage.errorMessage(
               "Failed to automatically find a display chance for the items without a display chance set, because the sum of all display chances would be larger then 100. Disabling repeated rotations for shop "
                  + this.getSection()
                  + ".\nTo prevent this issue, please adjust the values manually to reach a total sum(or smaller) as "
                  + (100.0 - 0.01 * var2.size())
                  + ", current sum: "
                  + (var3 + 0.01 * var2.size())
            );
            this.repeatedRotation = false;
            return null;
         }

         for (String var16 : var2) {
            RotatingShopProvider.Item var9 = new RotatingShopProvider.Item(this.getSection() + "." + var16, var13);
            var1.add(var9);
         }
      }

      return var1;
   }

   private Double getDisplayChance(String var1) {
      Config var2 = ConfigManager.getShop(this.getSection());
      if (!var2.contains(var1 + ".display-chance")) {
         return null;
      } else {
         try {
            return Double.parseDouble(var2.getString(var1 + ".display-chance"));
         } catch (NumberFormatException var4) {
            return null;
         }
      }
   }

   private Long getInterval() throws ShopLoadException {
      String var1 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.interval");
      if (var1 == null) {
         return null;
      } else {
         try {
            return TimeUtils.getTime(var1);
         } catch (ParseException var3) {
            throw new ShopLoadException(Lang.INVALID_ROTATION_INTERVAL.get().getLegacy().replace("%shop%", this.getSection()).replace("%interval%", var1));
         }
      }
   }

   private RotatingShopProvider.RotationSchedule getSchedule() throws ShopLoadException {
      String var1 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.interval");
      String var2 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.frequency");
      if (!"SYNC".equalsIgnoreCase(var1) && !"SYNC".equalsIgnoreCase(var2)) {
         Long var3 = this.getInterval();
         if (var3 != null) {
            return new RotatingShopProvider.ScheduleTypeInterval(var3);
         } else {
            String var4 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.frequency");
            if (var4 == null) {
               throw new ShopLoadException("No rotation interval or frequency specified for rotating shop");
            } else {
               JobDetail var5 = JobBuilder.newJob(RotationJob.class)
                  .setJobData(new JobDataMap(ImmutableMap.of("section", this.getSection())))
                  .withIdentity(this.getSection() + "_JOB", "ONYX_ROTATING_SHOP")
                  .build();
               Trigger var6 = TriggerBuilder.newTrigger()
                  .withIdentity(this.getSection() + "_TRIGGER", "ONYX_ROTATING_SHOP")
                  .withSchedule(CronScheduleBuilder.cronSchedule(var4))
                  .build();
               ScheduleManager var7 = this.plugin.getScheduleManager();
               if (!var7.hasJob(var5.getKey())) {
                  try {
                     var7.schedule(var5, var6);
                     if (var6.getNextFireTime() == null) {
                        throw new ShopLoadException("Failed to schedule cron expression as it will never fire again for " + var4);
                     }
                  } catch (SchedulerException var9) {
                     var9.printStackTrace();
                     throw new ShopLoadException("A unknown error occurred while creating a new cron schedule for " + var4);
                  }
               } else {
                  var6 = var7.getJob(var6.getKey());
               }

               return new RotatingShopProvider.ScheduleTypeFrequency(var6);
            }
         }
      } else {
         return new RotatingShopProvider.ScheduleTypeDatabaseSync();
      }
   }

   private class Item implements Comparable<RotatingShopProvider.Item> {
      String item;
      double chance;
      double randomKey;

      public Item(String nullx, double nullxx) {
         this.item = nullx;
         this.chance = nullxx;
      }

      public int compareTo(RotatingShopProvider.Item var1) {
         return Double.compare(this.randomKey, var1.randomKey);
      }
   }

   private interface RotationSchedule {
      long getNextRotation();

      void init(long var1);

      void rotate();

      void stop();
   }

   private class RotationTask {
      final RotatingShopProvider.RotationSchedule schedule = RotatingShopProvider.this.getSchedule();

      public RotationTask() throws ShopLoadException {
         if (!(this.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync)) {
            this.rotate();
            long var8 = 0L;
            long var4 = new Date().toInstant().toEpochMilli();
            ScheduleCache.RotatingShopInterval var6 = RotatingShopProvider.this.plugin
               .getScheduleCache()
               .getRotatingInterval(RotatingShopProvider.this.getSection());
            if (var6 != null) {
               if (var6.getNextRun() <= var4) {
                  SendMessage.infoMessage(
                     "Rotating items inside shop "
                        + RotatingShopProvider.this.getSection()
                        + " now because the rotation interval ended at "
                        + Date.from(Instant.ofEpochMilli(var6.getNextRun()))
                        + " when server was offline..."
                  );
                  RotatingShopProvider.this.plugin.runTask(() -> RotatingShopProvider.this.loadNextRotation(false));
                  var8 = -1L;
               } else {
                  if (this.schedule instanceof RotatingShopProvider.ScheduleTypeInterval) {
                     if (var6.getNextRun() - var4 > ((RotatingShopProvider.ScheduleTypeInterval)this.schedule).interval) {
                        SendMessage.logDebugMessage(
                           "Found cached interval for rotating shop "
                              + RotatingShopProvider.this.getSection()
                              + " with next rotation in "
                              + TimeUtils.getReadableTime(var6.getNextRun() - var4)
                              + ", but current interval does not match for "
                              + TimeUtils.getReadableTime(((RotatingShopProvider.ScheduleTypeInterval)this.schedule).interval)
                              + ", updating cache..."
                        );
                        var8 = -1L;
                     } else if (var6.getNextRun() != 0L) {
                        SendMessage.infoMessage(
                           "Loaded cached interval for rotating shop "
                              + RotatingShopProvider.this.getSection()
                              + " with next rotation in "
                              + TimeUtils.getReadableTime(var6.getNextRun() - var4)
                              + "..."
                        );
                        var8 = var6.getNextRun() - var4;
                     }
                  } else if (this.schedule instanceof RotatingShopProvider.ScheduleTypeFrequency && var6.getNextRun() != this.schedule.getNextRotation()) {
                     SendMessage.logDebugMessage(
                        "Found cached cron schedule for rotating shop "
                           + RotatingShopProvider.this.getSection()
                           + " with next rotation in "
                           + TimeUtils.getReadableTime(var6.getNextRun() - var4)
                           + ", but current interval does not match for "
                           + TimeUtils.getReadableTime(this.schedule.getNextRotation() - var4)
                           + ", updating cache..."
                     );
                     this.schedule.rotate();
                  }

                  List var7 = var6.getItems().stream().filter(var1 -> RotatingShopProvider.this.getShopItem(var1) != null).collect(Collectors.toList());
                  if (var7.isEmpty()) {
                     RotatingShopProvider.this.loadPages();
                  } else if (var7.size() != RotatingShopProvider.this.totalSlots) {
                     SendMessage.logDebugMessage(
                        "Size of items from cached rotation for "
                           + RotatingShopProvider.this.getSection()
                           + " does not match current display slots, loading new random items..."
                     );
                     RotatingShopProvider.this.loadPages(RotatingShopProvider.this.getNextRandomItems());
                     var8 = -1L;
                  } else {
                     RotatingShopProvider.this.loadPages(var7);
                  }
               }
            } else if (this.schedule instanceof RotatingShopProvider.ScheduleTypeFrequency) {
               RotatingShopProvider.this.loadPages(RotatingShopProvider.this.getNextRandomItems());
            }

            if (this.schedule instanceof RotatingShopProvider.ScheduleTypeInterval) {
               this.schedule.init(var8);
            }
         } else {
            for (int var2 = 1; var2 <= Math.max(1, RotatingShopProvider.this.pages.size()); var2++) {
               RotatingShopProvider.this.shopPages.put(var2, new ShopPageItems(RotatingShopProvider.this.getSection()));
            }

            this.schedule.init(0L);
         }
      }

      public void rotate() {
         this.schedule.rotate();
      }

      public void end() {
         this.schedule.stop();
         if (!(this.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync)) {
            if (RotatingShopProvider.this.plugin.getScheduleCache() != null) {
               RotatingShopProvider.this.plugin
                  .getScheduleCache()
                  .cache(
                     ScheduleCache.ScheduleType.ROTATING_SHOP_INTERVAL,
                     RotatingShopProvider.this.getSection(),
                     this.schedule.getNextRotation(),
                     RotatingShopProvider.this.currentItems
                  );
            } else {
               SendMessage.errorMessage("Failed to cache next rotation interval for rotating shop " + RotatingShopProvider.this.getSection());
            }
         }
      }
   }

   private class ScheduleTypeDatabaseSync implements RotatingShopProvider.RotationSchedule {
      private ScheduledTask task;
      private long next;

      public ScheduleTypeDatabaseSync() throws ShopLoadException {
         Database var2 = RotatingShopProvider.this.plugin.getProviderManager().getDatabase();
         if (!(var2 instanceof MySQL) && !(var2 instanceof PostgreSQL)) {
            throw new ShopLoadException(
               "Setting rotating-shop-settings.interval/frequency option to SYNC requires MySQL or PostgreSQL as the storage-type, for shop "
                  + RotatingShopProvider.this.getSection()
            );
         }
      }

      @Override
      public long getNextRotation() {
         return this.next;
      }

      @Override
      public void rotate() {
         this.poll();
      }

      @Override
      public void init(long var1) {
         this.task = RotatingShopProvider.this.plugin.runTaskAsyncTimer(this::poll, 0L, 200L);
      }

      @Override
      public void stop() {
         if (this.task != null) {
            this.task.cancel();
         }
      }

      private void poll() {
         try {
            SimplePair var1 = RotatingShopProvider.this.plugin.getProviderManager().getDatabase().getRotatingShopItems(RotatingShopProvider.this.getSection());
            if (var1 == null || (Long)var1.value == this.next) {
               return;
            }

            List<String> var2 = ((List<String>)var1.key).stream().filter(var1x -> RotatingShopProvider.this.getShopItem(var1x) != null).collect(Collectors.toList());
            if (var2.size() != RotatingShopProvider.this.totalSlots) {
               SendMessage.logDebugMessage(
                  "Size of synced rotation items for " + RotatingShopProvider.this.getSection() + " does not match current display slots, skipping..."
               );
               return;
            }

            RotatingShopProvider.this.plugin
               .runTask(
                  () -> {
                     RotatingShopProvider.this.loadPages(var2);
                     this.next = (Long)var1.value;
                     RotatingShopProvider.this.reopen();
                     if (RotatingShopProvider.this.broadcast) {
                        SendMessage.broadcastMessage(
                           Lang.NEW_ITEMS_GLOBAL_MESSAGE
                              .get()
                              .replace("%shop%", RotatingShopProvider.this.getSection())
                              .replace("%shopName%", RotatingShopProvider.this.getShopName())
                              .replace("%interval%", RotatingShopProvider.this.getNextRotation())
                        );
                     }
                  }
               );
         } catch (SQLException var3) {
            SendMessage.warnMessage("Failed to sync rotating shop state for " + RotatingShopProvider.this.getSection() + " from database");
            var3.printStackTrace();
         }
      }
   }

   private class ScheduleTypeFrequency implements RotatingShopProvider.RotationSchedule {
      private final TriggerKey key;
      private long next;

      public ScheduleTypeFrequency(Trigger nullx) {
         this.key = nullx.getKey();
         this.next = nullx.getNextFireTime().toInstant().toEpochMilli();
      }

      @Override
      public long getNextRotation() {
         return this.next;
      }

      @Override
      public void init(long var1) {
      }

      @Override
      public void rotate() {
         this.next = RotatingShopProvider.this.plugin.getScheduleManager().getNextFire(this.key);
      }

      @Override
      public void stop() {
      }
   }

   private class ScheduleTypeInterval implements RotatingShopProvider.RotationSchedule {
      private ScheduledTask task;
      private final long interval;
      private long next;

      public ScheduleTypeInterval(long nullx) {
         this.interval = nullx;
      }

      @Override
      public long getNextRotation() {
         return this.next;
      }

      @Override
      public void rotate() {
         this.next = new Date().toInstant().toEpochMilli() + this.interval;
      }

      @Override
      public void init(long var1) {
         this.task = RotatingShopProvider.this.plugin
            .runTaskTimer(
               () -> RotatingShopProvider.this.loadNextRotation(false), (var1 < 0L ? this.interval : var1) / 1000L * 20L, this.interval / 1000L * 20L
            );
         this.next = new Date().toInstant().toEpochMilli() + (var1 < 0L ? this.interval : var1);
      }

      @Override
      public void stop() {
         this.task.cancel();
      }
   }
}
