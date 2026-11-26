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

   public ScheduleCache(OnyxShop var1) {
      this.file = new File(var1.getDataFolder() + File.separator + "cache" + File.separator + "schedulecache.json");
      this.plugin = var1;

      try {
         if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
         }

         if (!this.file.exists()) {
            this.file.createNewFile();
         }

         if (this.file.length() == 0L) {
            FileOutputStream var2 = new FileOutputStream(this.file);
            var2.write("[]".getBytes());
            var2.close();
         }
      } catch (IOException var8) {
         SendMessage.warnMessage("Failed to create 'schedulecache.json' cache file");
         var8.printStackTrace();
      }

      try {
         FileReader var9 = new FileReader(this.file);

         try {
            this.loadCachedSchedules(JsonUtil.parseArray(var9));
         } catch (Throwable var6) {
            try {
               var9.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         var9.close();
      } catch (Exception var7) {
         SendMessage.warnMessage("Failed to load schedule cache from 'schedulecache.json'");
         var7.printStackTrace();
      }
   }

   private void loadCachedSchedules(JSONArray var1) {
      for (Object var3 : var1) {
         try {
            JSONObject var4 = (JSONObject)var3;

            ScheduleCache.ScheduleType var5;
            try {
               var5 = ScheduleCache.ScheduleType.valueOf(JsonUtil.getString(var4, "type"));
            } catch (IllegalArgumentException var13) {
               throw new NullPointerException("Invalid schedule type for " + JsonUtil.getString(var4, "type"));
            }

            long var6;
            try {
               var6 = (Long)var4.get("nextRun");
            } catch (ClassCastException var12) {
               throw new NullPointerException("Invalid time for next run: " + JsonUtil.getString(var4, "nextRun"));
            }

            switch (var5) {
               case ROTATING_SHOP_INTERVAL:
                  String var8 = JsonUtil.getString(var4, "shop");
                  ArrayList var9 = JsonUtil.getArray(var4, "items");
                  if (this.plugin.getShopSections().contains(var8) && this.plugin.getShopType(var8) == ShopType.ROTATING) {
                     this.schedules.add(new ScheduleCache.RotatingShopInterval(var6, var8, var9));
                     break;
                  }

                  throw new NullPointerException("Rotating shop with name " + var8 + " does not exist");
               case DELAYED_COMMAND:
                  Map var10 = JsonUtil.getMap(var4, "args");
                  String var11 = JsonUtil.getString(var4, "task");
                  this.schedules.add(new ScheduleCache.DelayedCommand(var6, var11, var10));
            }
         } catch (Exception var14) {
            SendMessage.errorMessage("Failed to load cached schedule from schedulecache.json: " + var14.getMessage());
         }
      }
   }

   public ScheduleCache.RotatingShopInterval getRotatingInterval(String var1) {
      ScheduleCache.Schedule var2 = this.schedules
         .stream()
         .filter(var0 -> var0.getType() == ScheduleCache.ScheduleType.ROTATING_SHOP_INTERVAL)
         .filter(var1x -> ((ScheduleCache.RotatingShopInterval)var1x).shop.equals(var1))
         .findFirst()
         .orElse(null);
      return (ScheduleCache.RotatingShopInterval)var2;
   }

   public List<ScheduleCache.Schedule> getDelayedCommands() {
      return this.schedules.stream().filter(var0 -> var0.getType() == ScheduleCache.ScheduleType.DELAYED_COMMAND).collect(Collectors.toList());
   }

   public void removeDelayedCommand(ScheduleCache.DelayedCommand var1) {
      SendMessage.logDebugMessage("Removed delayed command '" + var1.task + "' from cache: " + this.schedules.remove(var1));
   }

   public void cacheDelayedCommands(List<ScheduleCache.DelayedCommand> var1) {
      this.schedules.addAll(var1);
   }

   public void cache(ScheduleCache.ScheduleType var1, String var2, long var3, List<String> var5) {
      for (ScheduleCache.Schedule var7 : this.schedules
         .stream()
         .filter(var1x -> var1x.getType() == var1)
         .filter(var1x -> ((ScheduleCache.RotatingShopInterval)var1x).shop.equals(var2))
         .collect(Collectors.toList())) {
         this.schedules.remove(var7);
      }

      this.schedules.add(new ScheduleCache.RotatingShopInterval(var3, var2, var5));
   }

   public void save() {
      this.saveToFile();
   }

   private void saveToFile() {
      JSONArray var1 = this.toJSON();
      SendMessage.logDebugMessage("Saving cached schedules...");

      try {
         FileWriter var2 = new FileWriter(this.file);

         try {
            var2.write(var1.toJSONString());
            var2.flush();
         } catch (Throwable var6) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         var2.close();
      } catch (IOException var7) {
         SendMessage.warnMessage("Failed to save schedule cache to 'schedulecache.json'");
         var7.printStackTrace();
      }
   }

   private JSONArray toJSON() {
      JSONArray var1 = new JSONArray();

      for (ScheduleCache.Schedule var3 : this.schedules) {
         var1.add(var3.toJSON());
      }

      return var1;
   }

   public static class DelayedCommand implements ScheduleCache.Schedule {
      final ScheduleCache.ScheduleType type = ScheduleCache.ScheduleType.DELAYED_COMMAND;
      final String task;
      final long nextRun;
      final Map<String, String> args;

      public DelayedCommand(long var1, String var3, Map<String, String> var4) {
         this.task = var3;
         this.nextRun = var1;
         this.args = var4;
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
         HashMap var1 = new HashMap();
         var1.put("type", this.type.name());
         var1.put("args", this.args);
         var1.put("nextRun", this.nextRun);
         var1.put("task", this.task);
         return new JSONObject(var1);
      }
   }

   public class RotatingShopInterval implements ScheduleCache.Schedule {
      final ScheduleCache.ScheduleType type = ScheduleCache.ScheduleType.ROTATING_SHOP_INTERVAL;
      final long nextRun;
      final String shop;
      final LinkedList<String> items;

      public RotatingShopInterval(long nullx, String nullxx, List<String> nullxxx) {
         this.nextRun = nullx;
         this.shop = nullxx;
         this.items = new LinkedList<>(nullxxx);
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
         HashMap var1 = new HashMap();
         var1.put("type", this.type.name());
         var1.put("shop", this.shop);
         var1.put("nextRun", this.nextRun);
         var1.put("items", this.items);
         return new JSONObject(var1);
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
