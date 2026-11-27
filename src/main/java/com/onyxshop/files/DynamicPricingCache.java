package com.onyxshop.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.JsonUtil;
import org.json.simple.JSONObject;

public class DynamicPricingCache {
   private final OnyxShop plugin;
   private final File file;
   private JSONObject settins;

   public DynamicPricingCache(OnyxShop plugin) {
      this.file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + "dp_cache.json");
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
            fileOutputStream.write("{}".getBytes());
            fileOutputStream.close();
         }
      } catch (IOException exception) {
         SendMessage.warnMessage("Failed to create 'dp_cache.json' cache file");
         exception.printStackTrace();
      }

      this.loadSettings();
   }

   private void loadSettings() {
      try {
         FileReader fileReader = new FileReader(this.file);

         try {
            this.settins = JsonUtil.parseJson(fileReader);
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
         SendMessage.warnMessage("Failed to load DynamicPricing cache from 'dp_cache.json'");
         exception.printStackTrace();
      }
   }

   public Integer getCachedMaxPrice(String price) {
      try {
         Object value = this.settins.get("cachedMaxPrices");
         if (value == null) {
            return null;
         } else {
            JSONObject jsonObject = (JSONObject)value;
            Object value2 = jsonObject.get(price);
            return value2 == null ? null : ((Long)value2).intValue();
         }
      } catch (Exception exception) {
         return null;
      }
   }

   public Integer getCachedMaxStock(String itemId) {
      try {
         if (itemId == null) {
            Object value = this.settins.get("cachedGlobalMaxStock");
            return value == null ? null : ((Long)value).intValue();
         } else {
            Object value = this.settins.get("cachedSectionsMaxStock");
            if (value == null) {
               return null;
            } else {
               JSONObject jsonObject = (JSONObject)value;
               Object value2 = jsonObject.get(itemId);
               return value2 == null ? null : ((Long)value2).intValue();
            }
         }
      } catch (Exception exception) {
         return ConfigManager.getConfig().getInt("dynamic-pricing.max-stock", 250);
      }
   }

   public void saveCachedMaxPrice(String price, int value) {
      Object value2 = this.settins.get("cachedMaxPrices");
      if (value2 == null) {
         value2 = new JSONObject();
      }

      JSONObject jsonObject = (JSONObject)value2;
      jsonObject.put(price, (long)value);
      this.settins.put("cachedMaxPrices", jsonObject);
   }

   public void saveCachedMaxStock(String itemId, int value) {
      if (itemId == null) {
         this.settins.put("cachedGlobalMaxStock", (long)value);
      } else {
         Object value2 = this.settins.get("cachedSectionsMaxStock");
         if (value2 == null) {
            value2 = new JSONObject();
         }

         JSONObject jsonObject = (JSONObject)value2;
         jsonObject.put(itemId, (long)value);
         this.settins.put("cachedSectionsMaxStock", jsonObject);
      }
   }

   public void removeCachedMaxPrice(String price) {
      Object value = this.settins.get("cachedMaxPrices");
      if (value == null) {
         value = new JSONObject();
      }

      JSONObject jsonObject = (JSONObject)value;
      if (jsonObject.containsKey(price)) {
         jsonObject.remove(price);
      }

      this.settins.put("cachedMaxPrices", jsonObject);
   }

   public void removeCachedMaxStock(String itemId) {
      Object value = this.settins.get("cachedSectionsMaxStock");
      if (value == null) {
         value = new JSONObject();
      }

      JSONObject jsonObject = (JSONObject)value;
      if (jsonObject.containsKey(itemId)) {
         jsonObject.remove(itemId);
      }

      this.settins.put("cachedSectionsMaxStock", jsonObject);
   }

   public void write() {
      try {
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(this.settins.toJSONString());
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
         SendMessage.warnMessage("Failed to save DynamicPricing cache to 'dp_cache.json'");
         exception.printStackTrace();
      }
   }
}
