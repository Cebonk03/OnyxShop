package com.onyxshop.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.JsonUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ScheduleCache {
   private final OnyxShop plugin;
   private final File file;
   private final List<ScheduleCache.Schedule> schedules = new ArrayList<>();

   public ScheduleCache(OnyxShop plugin) {
      this.file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + "schedulecache.json");
      this.plugin = plugin;

      try {
         if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
         }

         if (!this.file.exists()) {
            this.file.createNewFile();
         }

         if (this.file.length() == 0L) {
            FileOutputStream fileOutputStream = new FileOutputStream(this.file);
            fileOutputStream.write("[]".getBytes());
            fileOutputStream.close();
         }
      } catch (IOException exception) {
         SendMessage.warnMessage("Failed to create 'schedulecache.json' cache file");
         exception.printStackTrace();
      }

      try {
         FileReader fileReader = new FileReader(this.file);

         try {
            this.loadCachedSchedules(JsonUtil.parseArray(fileReader));
         } catch (Throwable exception) {
            try {
               fileReader.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         fileReader.close();
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to load schedule cache from 'schedulecache.json'");
         exception.printStackTrace();
      }
   }

   private void loadCachedSchedules(JSONArray jsonArray) {
      for (Object value : jsonArray) {
         try {
            JSONObject jsonObject = (JSONObject)value;

            ScheduleCache.ScheduleType scheduleType;
            try {
               scheduleType = ScheduleCache.ScheduleType.valueOf(JsonUtil.getString(jsonObject, "type"));
            } catch (IllegalArgumentException exception) {
               throw new NullPointerException("Invalid schedule type for " + JsonUtil.getString(jsonObject, "type"));
            }

            long value2;
            try {
               value2 = (Long)jsonObject.get("nextRun");
            } catch (ClassCastException exception) {
               throw new NullPointerException("Invalid time for next run: " + JsonUtil.getString(jsonObject, "nextRun"));
            }

            switch (scheduleType) {
               case ROTATING_SHOP_INTERVAL:
                  String text = JsonUtil.getString(jsonObject, "shop");
                  ArrayList array = JsonUtil.getArray(jsonObject, "items");
                  if (this.plugin.getShopSections().contains(text) && this.plugin.getShopType(text) == ShopType.ROTATING) {
                     this.schedules.add(new ScheduleCache.RotatingShopInterval(value2, text, array));
                     break;
                  }

                  throw new NullPointerException("Rotating shop with name " + text + " does not exist");
               case DELAYED_COMMAND:
                  Map map = JsonUtil.getMap(jsonObject, "args");
                  String text2 = JsonUtil.getString(jsonObject, "task");
                  this.schedules.add(new ScheduleCache.DelayedCommand(value2, text2, map));
            }
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to load cached schedule from schedulecache.json: " + exception.getMessage());
         }
      }
   }

   public ScheduleCache.RotatingShopInterval getRotatingInterval(String shopId) {
      ScheduleCache.Schedule schedule = this.schedules
         .stream()
         .filter(element -> element.getType() == ScheduleCache.ScheduleType.ROTATING_SHOP_INTERVAL)
         .filter(element -> ((ScheduleCache.RotatingShopInterval)element).shop.equals(shopId))
         .findFirst()
         .orElse(null);
      return (ScheduleCache.RotatingShopInterval)schedule;
   }

   public List<ScheduleCache.Schedule> getDelayedCommands() {
      return this.schedules.stream().filter(element -> element.getType() == ScheduleCache.ScheduleType.DELAYED_COMMAND).collect(Collectors.toList());
   }

   public void removeDelayedCommand(ScheduleCache.DelayedCommand delayedCommand) {
      SendMessage.logDebugMessage("Removed delayed command '" + delayedCommand.task + "' from cache: " + this.schedules.remove(delayedCommand));
   }

   public void cacheDelayedCommands(List<ScheduleCache.DelayedCommand> values) {
      this.schedules.addAll(values);
   }

   public void cache(ScheduleCache.ScheduleType scheduleType, String type, long value, List<String> values) {
      for (ScheduleCache.Schedule schedule : this.schedules
         .stream()
         .filter(element -> element.getType() == scheduleType)
         .filter(element -> ((ScheduleCache.RotatingShopInterval)element).shop.equals(type))
         .collect(Collectors.toList())) {
         this.schedules.remove(schedule);
      }

      this.schedules.add(new ScheduleCache.RotatingShopInterval(value, type, values));
   }

   public void save() {
      this.saveToFile();
   }

   private void saveToFile() {
      JSONArray jsonArray = this.toJSON();
      SendMessage.logDebugMessage("Saving cached schedules...");

      try {
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(jsonArray.toJSONString());
            fileWriter.flush();
         } catch (Throwable exception) {
            try {
               fileWriter.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         fileWriter.close();
      } catch (IOException exception) {
         SendMessage.warnMessage("Failed to save schedule cache to 'schedulecache.json'");
         exception.printStackTrace();
      }
   }

   private JSONArray toJSON() {
      JSONArray jsonArray = new JSONArray();

      for (ScheduleCache.Schedule schedule : this.schedules) {
         jsonArray.add(schedule.toJSON());
      }

      return jsonArray;
   }

   public static class DelayedCommand implements ScheduleCache.Schedule {
      final ScheduleCache.ScheduleType type = ScheduleCache.ScheduleType.DELAYED_COMMAND;
      final String task;
      final long nextRun;
      final Map<String, String> args;

      public DelayedCommand(long value, String command, Map<String, String> map) {
         this.task = command;
         this.nextRun = value;
         this.args = map;
      }

      public long getNextRun() {
         return this.nextRun;
      }

      public String getTask() {
         return this.task;
      }

      public Map<String, String> getArgs() {
         return this.args;
      }

      @Override
      public ScheduleCache.ScheduleType getType() {
         return this.type;
      }

      @Override
      public JSONObject toJSON() {
         HashMap map = new HashMap();
         map.put("type", this.type.name());
         map.put("args", this.args);
         map.put("nextRun", this.nextRun);
         map.put("task", this.task);
         return new JSONObject(map);
      }
   }

   public class RotatingShopInterval implements ScheduleCache.Schedule {
      final ScheduleCache.ScheduleType type = ScheduleCache.ScheduleType.ROTATING_SHOP_INTERVAL;
      final long nextRun;
      final String shop;
      final LinkedList<String> items;

      public RotatingShopInterval(long value, String shopId, List<String> values) {
         this.nextRun = value;
         this.shop = shopId;
         this.items = new LinkedList<>(values);
      }

      public long getNextRun() {
         return this.nextRun;
      }

      public List<String> getItems() {
         return this.items;
      }

      @Override
      public ScheduleCache.ScheduleType getType() {
         return this.type;
      }

      @Override
      public JSONObject toJSON() {
         HashMap map = new HashMap();
         map.put("type", this.type.name());
         map.put("shop", this.shop);
         map.put("nextRun", this.nextRun);
         map.put("items", this.items);
         return new JSONObject(map);
      }
   }

   public interface Schedule {
      ScheduleCache.ScheduleType getType();

      JSONObject toJSON();
   }

   public static enum ScheduleType {
      ROTATING_SHOP_INTERVAL,
      DELAYED_COMMAND;

      private ScheduleType() {
      }
   }
}
