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

   public RotatingShopProvider(OnyxShop plugin, String shopId) throws ShopLoadException {
      super(plugin, shopId);
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
      } catch (IndexOutOfBoundsException exception) {
         throw new ShopLoadException(
            "Failed to load next rotation for shop %shop%, there are more display slots than there are shop items".replace("%shop%", this.getSection())
         );
      }
   }

   public List<String> getCurrentItems() {
      return this.currentItems;
   }

   public ShopPageItems getPage(int page) {
      return this.shopPages.get(page);
   }

   public int getPageSize() {
      return this.pages.size();
   }

   @Override
   public int getPageForShopItem(String shopId) {
      int page = 1;

      for (ShopPageItems shopPageItems : this.shopPages.values()) {
         if (shopPageItems.getItemOrder().contains(shopId)) {
            return page;
         }

         page++;
      }

      return 1;
   }

   @Override
   public List<ShopItem> getPageItems(int page) {
      return this.shopPages
         .get(page)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(element -> this.getDisplayItem(this.getSection() + "." + element))
         .filter(element -> element instanceof ShopItem)
         .map(element -> (ShopItem)element)
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

   public void loadNextRotation(boolean flag) {
      if (this.task.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync) {
         SendMessage.warnMessage(Lang.ROTATING_SHOP_FORCE_ROTATE_BLOCKED.get().replace("%shop%", this.getSection()));
      } else {
         try {
            if (!flag) {
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
            Database database = this.plugin.getProviderManager().getDatabase();
            if (database instanceof MySQL || database instanceof PostgreSQL) {
               database.setRotatingShopItems(this.getSection(), this.currentItems, this.task.schedule.getNextRotation());
            }

            if (!flag && this.broadcast) {
               SendMessage.broadcastMessage(
                  Lang.NEW_ITEMS_GLOBAL_MESSAGE
                     .get()
                     .replace("%shop%", this.getSection())
                     .replace("%shopName%", this.getShopName())
                     .replace("%interval%", this.getNextRotation())
               );
            }
         } catch (NullPointerException exception) {
            SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
            exception.printStackTrace();
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
         int value = 1;
         int value2 = 0;

         for (String text : this.pages) {
            int pageSize = this.getPageSize(text);
            ArrayList values = new ArrayList();
            List values2 = this.displaySlots.get(value);
            LinkedHashMap map = new LinkedHashMap();

            for (int index = 0; index < pageSize + 1; index++) {
               if (values2.contains(index)) {
                  String text2 = this.currentItems.get(value2++);
                  map.put(index, this.getDisplayItem(text2).getShopItem());
                  values.add(text2.split("\\.")[1] + "." + text2.split("\\.")[2]);
               } else {
                  values.add(null);
               }
            }

            this.shopPages.put(value, new ShopPageItems(map, values, this.getSection(), pageSize));
            value++;
         }
      } catch (Exception exception) {
         SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
         exception.printStackTrace();
      }
   }

   private void loadPages(List<String> values) {
      try {
         this.currentItems = values;
         this.processItems(true);
         int value = 1;
         int value2 = 0;

         for (String text : this.pages) {
            int pageSize = this.getPageSize(text);
            ArrayList values2 = new ArrayList();
            List values3 = this.displaySlots.get(value);
            LinkedHashMap map = new LinkedHashMap();

            for (int index = 0; index < pageSize + 1; index++) {
               if (values3.contains(index)) {
                  String text2 = (String)values.get(value2++);
                  map.put(index, this.getDisplayItem(text2).getShopItem());
                  values2.add(text2.split("\\.")[1] + "." + text2.split("\\.")[2]);
               } else {
                  values2.add(null);
               }
            }

            this.shopPages.put(value, new ShopPageItems(map, values2, this.getSection(), pageSize));
            value++;
         }
      } catch (Exception exception) {
         SendMessage.warnMessage(Lang.FAILED_LOADING_NEXT_ROTATION.get().replace("%shop%", this.getSection()));
         exception.printStackTrace();
      }
   }

   private List<String> getNextRandomItems() {
      if (!this.repeatedRotation) {
         return this.getNextItems();
      } else {
         HashSet values = new HashSet();
         ArrayList values2 = new ArrayList();

         for (RotatingShopProvider.Item item : this.displayChances) {
            double amount = this.random.nextDouble() * 100.0;
            if (amount < item.chance) {
               values.add(item.item);
            } else {
               values2.add(item);
            }
         }

         List<String> values3 = new ArrayList<>(values);
         if (values3.size() < this.totalSlots) {
            Collections.shuffle(values2, this.random);

            for (int size = 0; size < values2.size() && values3.size() < this.totalSlots; size++) {
               values3.add(((RotatingShopProvider.Item)values2.get(size)).item);
            }
         }

         if (values3.size() > this.totalSlots) {
            Collections.shuffle((List<?>)values3, this.random);
            values3 = values3.subList(0, this.totalSlots);
         }

         return (List<String>)values3;
      }
   }

   private String getRandom(List<RotatingShopProvider.Item> values, double amount) {
      double amount2 = 0.0;

      for (RotatingShopProvider.Item item : values) {
         amount2 += item.chance;
         if (amount <= amount2) {
            return item.item;
         }
      }

      throw new IllegalStateException();
   }

   private int chooseIndex(double[] values, double index) {
      double amount = 0.0;

      for (int index2 = 0; index2 < values.length; index2++) {
         amount += values[index2];
         if (index <= amount) {
            return index2;
         }
      }

      throw new IllegalStateException();
   }

   private List<String> getNextItems() {
      ArrayList values = new ArrayList();
      Random random = new Random();

      for (int index = 0; index < this.totalSlots; index++) {
         String itemId = this.itemLocs.get(random.nextInt(this.itemLocs.size()));

         while (!this.canDuplicate && (this.history.getHistory().contains(this.getSection() + "." + itemId) || values.contains(this.getSection() + "." + itemId))) {
            itemId = this.itemLocs.get(random.nextInt(this.itemLocs.size()));
         }

         values.add(this.getSection() + "." + itemId);
      }

      this.history.rotate(values);
      return values;
   }

   private void processItems(boolean flag) {
      for (String text : this.currentItems) {
         ShopItem shopItem = this.getShopItem(text);
         if (shopItem != null && !shopItem.hasItemError() && !shopItem.isDisplayItem()) {
            ItemStack itemStack = shopItem.getItemToGive();
            if (itemStack == null) {
               continue;
            }

            boolean flag2 = ConfigManager.getConfig().getBoolean("prioritize-rotating-shop-items", true);
            String type = itemStack.getType().name();
            List<ShopItem> orDefault = this.plugin.shopItemsByMaterialName.getOrDefault(type, new ArrayList<>());
            if (flag) {
               if (flag2) {
                  orDefault.add(0, shopItem);
               } else {
                  orDefault.add(shopItem);
               }
            } else {
               orDefault.remove(shopItem);
            }

            if (!flag2) {
               switch (SortOrder.fromConfig()) {
                  case DESCENDING_SELL_PRICE:
                     orDefault.sort((element, arg2) -> Double.compare(arg2.getSellPrice(), element.getSellPrice()));
                     break;
                  case ASCENDING_SELL_PRICE:
                     orDefault.sort((element, arg2) -> Double.compare(element.getSellPrice(), arg2.getSellPrice()));
               }
            }

            this.plugin.shopItemsByMaterialName.put(type, orDefault);
         }
      }

      if (flag) {
         this.plugin.getQueryBuilder().rotate(this);
      }
   }

   private int getPageSize(String text) {
      Config config = ConfigManager.getSection(this.getSection());
      if (config.contains("rotating-shop-settings.pages." + text + ".display-slots")) {
         try {
            int page = Integer.parseInt(config.getString("rotating-shop-settings.pages." + text + ".gui-rows"));
            if (page >= 1 && page <= 6) {
               return page * 9;
            }

            SendMessage.warnMessage(
               "Inventory size for page '" + text + "' from rotating shop '" + this.getSection() + "' has to be in range from 1-6, using default..."
            );
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage("Invalid inventory size for page '" + text + "' from rotating shop '" + this.getSection() + "', using default...");
         }

         return 45;
      } else {
         int size = this.displaySlots.get(this.pages.indexOf(text) + 1).size();
         return size <= 45 && size >= 1 ? CalculateAmount.getSlots(size) : 45;
      }
   }

   private List<String> initPages() {
      LinkedList values = new LinkedList();
      Config config = ConfigManager.getSection(this.getSection());
      if (config.contains("rotating-shop-settings.pages")) {
         int value = 1;
         ConfigurationSection section = config.getConfigurationSection("rotating-shop-settings.pages");

         for (String text : section.getKeys(false)) {
            if (section.contains(text + ".display-slots")) {
               values.add(text);
            }

            this.pageTitles.put(value, this.getPageTitle(section, text));
            value++;
         }
      }

      return values;
   }

   private ShopTitle getPageTitle(ConfigurationSection section, String title) {
      if (section.contains(title + ".title")) {
         String text = section.getString(title + ".title");
         Translatable text2 = null;
         if (section.contains(title + ".bedrock-title")) {
            text2 = SendMessage.fromConfig(this.plugin.getInvTitle(section.getString(title + ".bedrock-title")));
         }

         return new ShopTitle(SendMessage.fromConfig(this.plugin.getInvTitle(text)), text2);
      } else {
         return this.title;
      }
   }

   private Map<Integer, List<Integer>> getDisplaySlots() {
      HashMap map = new HashMap();
      Config config = ConfigManager.getSection(this.getSection());

      for (int size = 0; size < this.pages.size(); size++) {
         String text = this.pages.get(size);
         List integerList = config.getIntegerList("rotating-shop-settings.pages." + text + ".display-slots");

         for (int index = 0; index < integerList.size(); index++) {
            int value2 = (Integer)integerList.get(index);
            if (super.getStaticItems(size + 1) != null && super.getStaticItems(size + 1).containsKey(value2)) {
               SendMessage.warnMessage(
                  Lang.CANNOT_LOAD_DISPLAY_SLOT
                     .get()
                     .replace("%slot%", String.valueOf(value2))
                     .replace("%page%", String.valueOf(size + 1))
                     .replace("%shop%", this.getSection())
               );
               integerList.remove(index);
            }
         }

         map.put(size + 1, integerList);
      }

      return map;
   }

   private List<RotatingShopProvider.Item> getDisplayChances() {
      ArrayList values = new ArrayList();
      ArrayList<String> values2 = new ArrayList<>(this.itemLocs);
      double amount = 0.0;

      for (String itemId : this.itemLocs) {
         Double displayChance = this.getDisplayChance(itemId);
         if (displayChance != null) {
            if (displayChance < 0.01) {
               SendMessage.warnMessage(
                  "Invalid chance for item " + itemId + " from shop " + this.getSection() + ", should be greater then 0.01 but got " + displayChance + ". Using 0.01..."
               );
               displayChance = 0.01;
            }

            if (displayChance > 100.0 - amount) {
               SendMessage.warnMessage(
                  "Invalid chances for items from shop "
                     + this.getSection()
                     + ", should be a sum of 100 but got "
                     + (amount + displayChance)
                     + ". Trying to automatically correct value..."
               );
               if (amount + values2.size() * 0.01 > 100.0) {
                  SendMessage.errorMessage(
                     "Failed to correct display chance automatically, disabling repeated rotations for shop "
                        + this.getSection()
                        + ". Please adjust the values manually for a total sum of 100(or nearly 100) to prevent this issue"
                  );
                  this.repeatedRotation = false;
                  return null;
               }

               double size = (100.0 - amount) / values2.size();
               SendMessage.warnMessage(
                  "Using "
                     + size
                     + "% display chance for all left over items... Please adjust the values manually for a total sum of 100(or nearly 100) to prevent this issue"
               );

               for (String text : values2) {
                  RotatingShopProvider.Item item = new RotatingShopProvider.Item(this.getSection() + "." + text, size);
                  values.add(item);
               }

               return values;
            }

            RotatingShopProvider.Item item = new RotatingShopProvider.Item(this.getSection() + "." + itemId, displayChance);
            values.add(item);
            values2.remove(itemId);
            amount += item.chance;
         }
      }

      if (!values2.isEmpty()) {
         double amount2 = (100.0 - amount) / values2.size();
         if (amount2 < 0.01) {
            SendMessage.errorMessage(
               "Failed to automatically find a display chance for the items without a display chance set, because the sum of all display chances would be larger then 100. Disabling repeated rotations for shop "
                  + this.getSection()
                  + ".\nTo prevent this issue, please adjust the values manually to reach a total sum(or smaller) as "
                  + (100.0 - 0.01 * values2.size())
                  + ", current sum: "
                  + (amount + 0.01 * values2.size())
            );
            this.repeatedRotation = false;
            return null;
         }

         for (String text : values2) {
            RotatingShopProvider.Item item = new RotatingShopProvider.Item(this.getSection() + "." + text, amount2);
            values.add(item);
         }
      }

      return values;
   }

   private Double getDisplayChance(String text) {
      Config config = ConfigManager.getShop(this.getSection());
      if (!config.contains(text + ".display-chance")) {
         return null;
      } else {
         try {
            return Double.parseDouble(config.getString(text + ".display-chance"));
         } catch (NumberFormatException exception) {
            return null;
         }
      }
   }

   private Long getInterval() throws ShopLoadException {
      String text = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.interval");
      if (text == null) {
         return null;
      } else {
         try {
            return TimeUtils.getTime(text);
         } catch (ParseException exception) {
            throw new ShopLoadException(Lang.INVALID_ROTATION_INTERVAL.get().getLegacy().replace("%shop%", this.getSection()).replace("%interval%", text));
         }
      }
   }

   private RotatingShopProvider.RotationSchedule getSchedule() throws ShopLoadException {
      String text = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.interval");
      String text2 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.frequency");
      if (!"SYNC".equalsIgnoreCase(text) && !"SYNC".equalsIgnoreCase(text2)) {
         Long interval = this.getInterval();
         if (interval != null) {
            return new RotatingShopProvider.ScheduleTypeInterval(interval);
         } else {
            String text3 = ConfigManager.getSection(this.getSection()).getString("rotating-shop-settings.frequency");
            if (text3 == null) {
               throw new ShopLoadException("No rotation interval or frequency specified for rotating shop");
            } else {
               JobDetail jobDetail = JobBuilder.newJob(RotationJob.class)
                  .setJobData(new JobDataMap(ImmutableMap.of("section", this.getSection())))
                  .withIdentity(this.getSection() + "_JOB", "ONYX_ROTATING_SHOP")
                  .build();
               Trigger trigger = TriggerBuilder.newTrigger()
                  .withIdentity(this.getSection() + "_TRIGGER", "ONYX_ROTATING_SHOP")
                  .withSchedule(CronScheduleBuilder.cronSchedule(text3))
                  .build();
               ScheduleManager scheduleManager = this.plugin.getScheduleManager();
               if (!scheduleManager.hasJob(jobDetail.getKey())) {
                  try {
                     scheduleManager.schedule(jobDetail, trigger);
                     if (trigger.getNextFireTime() == null) {
                        throw new ShopLoadException("Failed to schedule cron expression as it will never fire again for " + text3);
                     }
                  } catch (SchedulerException exception) {
                     exception.printStackTrace();
                     throw new ShopLoadException("A unknown error occurred while creating a new cron schedule for " + text3);
                  }
               } else {
                  trigger = scheduleManager.getJob(trigger.getKey());
               }

               return new RotatingShopProvider.ScheduleTypeFrequency(trigger);
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

      public Item(String itemId, double amount) {
         this.item = itemId;
         this.chance = amount;
      }

      public int compareTo(RotatingShopProvider.Item item) {
         return Double.compare(this.randomKey, item.randomKey);
      }
   }

   private interface RotationSchedule {
      long getNextRotation();

      void init(long value);

      void rotate();

      void stop();
   }

   private class RotationTask {
      final RotatingShopProvider.RotationSchedule schedule = RotatingShopProvider.this.getSchedule();

      public RotationTask() throws ShopLoadException {
         if (!(this.schedule instanceof RotatingShopProvider.ScheduleTypeDatabaseSync)) {
            this.rotate();
            long value = 0L;
            long value2 = new Date().toInstant().toEpochMilli();
            ScheduleCache.RotatingShopInterval rotatingShopInterval = RotatingShopProvider.this.plugin
               .getScheduleCache()
               .getRotatingInterval(RotatingShopProvider.this.getSection());
            if (rotatingShopInterval != null) {
               if (rotatingShopInterval.getNextRun() <= value2) {
                  SendMessage.infoMessage(
                     "Rotating items inside shop "
                        + RotatingShopProvider.this.getSection()
                        + " now because the rotation interval ended at "
                        + Date.from(Instant.ofEpochMilli(rotatingShopInterval.getNextRun()))
                        + " when server was offline..."
                  );
                  RotatingShopProvider.this.plugin.runTask(() -> RotatingShopProvider.this.loadNextRotation(false));
                  value = -1L;
               } else {
                  if (this.schedule instanceof RotatingShopProvider.ScheduleTypeInterval) {
                     if (rotatingShopInterval.getNextRun() - value2 > ((RotatingShopProvider.ScheduleTypeInterval)this.schedule).interval) {
                        SendMessage.logDebugMessage(
                           "Found cached interval for rotating shop "
                              + RotatingShopProvider.this.getSection()
                              + " with next rotation in "
                              + TimeUtils.getReadableTime(rotatingShopInterval.getNextRun() - value2)
                              + ", but current interval does not match for "
                              + TimeUtils.getReadableTime(((RotatingShopProvider.ScheduleTypeInterval)this.schedule).interval)
                              + ", updating cache..."
                        );
                        value = -1L;
                     } else if (rotatingShopInterval.getNextRun() != 0L) {
                        SendMessage.infoMessage(
                           "Loaded cached interval for rotating shop "
                              + RotatingShopProvider.this.getSection()
                              + " with next rotation in "
                              + TimeUtils.getReadableTime(rotatingShopInterval.getNextRun() - value2)
                              + "..."
                        );
                        value = rotatingShopInterval.getNextRun() - value2;
                     }
                  } else if (this.schedule instanceof RotatingShopProvider.ScheduleTypeFrequency && rotatingShopInterval.getNextRun() != this.schedule.getNextRotation()) {
                     SendMessage.logDebugMessage(
                        "Found cached cron schedule for rotating shop "
                           + RotatingShopProvider.this.getSection()
                           + " with next rotation in "
                           + TimeUtils.getReadableTime(rotatingShopInterval.getNextRun() - value2)
                           + ", but current interval does not match for "
                           + TimeUtils.getReadableTime(this.schedule.getNextRotation() - value2)
                           + ", updating cache..."
                     );
                     this.schedule.rotate();
                  }

                  List shopIds = rotatingShopInterval.getItems().stream().filter(element -> RotatingShopProvider.this.getShopItem(element) != null).collect(Collectors.toList());
                  if (shopIds.isEmpty()) {
                     RotatingShopProvider.this.loadPages();
                  } else if (shopIds.size() != RotatingShopProvider.this.totalSlots) {
                     SendMessage.logDebugMessage(
                        "Size of items from cached rotation for "
                           + RotatingShopProvider.this.getSection()
                           + " does not match current display slots, loading new random items..."
                     );
                     RotatingShopProvider.this.loadPages(RotatingShopProvider.this.getNextRandomItems());
                     value = -1L;
                  } else {
                     RotatingShopProvider.this.loadPages(shopIds);
                  }
               }
            } else if (this.schedule instanceof RotatingShopProvider.ScheduleTypeFrequency) {
               RotatingShopProvider.this.loadPages(RotatingShopProvider.this.getNextRandomItems());
            }

            if (this.schedule instanceof RotatingShopProvider.ScheduleTypeInterval) {
               this.schedule.init(value);
            }
         } else {
            for (int size = 1; size <= Math.max(1, RotatingShopProvider.this.pages.size()); size++) {
               RotatingShopProvider.this.shopPages.put(size, new ShopPageItems(RotatingShopProvider.this.getSection()));
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
         Database database = RotatingShopProvider.this.plugin.getProviderManager().getDatabase();
         if (!(database instanceof MySQL) && !(database instanceof PostgreSQL)) {
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
      public void init(long value) {
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
            SimplePair simplePair = RotatingShopProvider.this.plugin.getProviderManager().getDatabase().getRotatingShopItems(RotatingShopProvider.this.getSection());
            if (simplePair == null || (Long)simplePair.value == this.next) {
               return;
            }

            List<String> keies = ((List<String>)simplePair.key).stream().filter(element -> RotatingShopProvider.this.getShopItem(element) != null).collect(Collectors.toList());
            if (keies.size() != RotatingShopProvider.this.totalSlots) {
               SendMessage.logDebugMessage(
                  "Size of synced rotation items for " + RotatingShopProvider.this.getSection() + " does not match current display slots, skipping..."
               );
               return;
            }

            RotatingShopProvider.this.plugin
               .runTask(
                  () -> {
                     RotatingShopProvider.this.loadPages(keies);
                     this.next = (Long)simplePair.value;
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
         } catch (SQLException exception) {
            SendMessage.warnMessage("Failed to sync rotating shop state for " + RotatingShopProvider.this.getSection() + " from database");
            exception.printStackTrace();
         }
      }
   }

   private class ScheduleTypeFrequency implements RotatingShopProvider.RotationSchedule {
      private final TriggerKey key;
      private long next;

      public ScheduleTypeFrequency(Trigger trigger) {
         this.key = trigger.getKey();
         this.next = trigger.getNextFireTime().toInstant().toEpochMilli();
      }

      @Override
      public long getNextRotation() {
         return this.next;
      }

      @Override
      public void init(long value) {
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

      public ScheduleTypeInterval(long value) {
         this.interval = value;
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
      public void init(long value) {
         this.task = RotatingShopProvider.this.plugin
            .runTaskTimer(
               () -> RotatingShopProvider.this.loadNextRotation(false), (value < 0L ? this.interval : value) / 1000L * 20L, this.interval / 1000L * 20L
            );
         this.next = new Date().toInstant().toEpochMilli() + (value < 0L ? this.interval : value);
      }

      @Override
      public void stop() {
         this.task.cancel();
      }
   }
}
