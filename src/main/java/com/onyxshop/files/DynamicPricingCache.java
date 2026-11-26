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

   public DynamicPricingCache(OnyxShop var1) {
      this.file = new File(var1.getDataFolder() + File.separator + "cache" + File.separator + "dp_cache.json");
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
            var2.write("{}".getBytes());
            var2.close();
         }
      } catch (IOException var3) {
         SendMessage.warnMessage("Failed to create 'dp_cache.json' cache file");
         var3.printStackTrace();
      }

      this.loadSettings();
   }

   private void loadSettings() {
      try {
         FileReader var1 = new FileReader(this.file);

         try {
            this.settins = JsonUtil.parseJson(var1);
         } catch (Throwable var5) {
            try {
               var1.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         var1.close();
      } catch (Exception var6) {
         SendMessage.warnMessage("Failed to load DynamicPricing cache from 'dp_cache.json'");
         var6.printStackTrace();
      }
   }

   public Integer getCachedMaxPrice(String var1) {
      try {
         Object var2 = this.settins.get("cachedMaxPrices");
         if (var2 == null) {
            return null;
         } else {
            JSONObject var3 = (JSONObject)var2;
            Object var4 = var3.get(var1);
            return var4 == null ? null : ((Long)var4).intValue();
         }
      } catch (Exception var5) {
         return null;
      }
   }

   public Integer getCachedMaxStock(String var1) {
      try {
         if (var1 == null) {
            Object var6 = this.settins.get("cachedGlobalMaxStock");
            return var6 == null ? null : ((Long)var6).intValue();
         } else {
            Object var2 = this.settins.get("cachedSectionsMaxStock");
            if (var2 == null) {
               return null;
            } else {
               JSONObject var3 = (JSONObject)var2;
               Object var4 = var3.get(var1);
               return var4 == null ? null : ((Long)var4).intValue();
            }
         }
      } catch (Exception var5) {
         return ConfigManager.getConfig().getInt("dynamic-pricing.max-stock", 250);
      }
   }

   public void saveCachedMaxPrice(String var1, int var2) {
      Object var3 = this.settins.get("cachedMaxPrices");
      if (var3 == null) {
         var3 = new JSONObject();
      }

      JSONObject var4 = (JSONObject)var3;
      var4.put(var1, (long)var2);
      this.settins.put("cachedMaxPrices", var4);
   }

   public void saveCachedMaxStock(String var1, int var2) {
      if (var1 == null) {
         this.settins.put("cachedGlobalMaxStock", (long)var2);
      } else {
         Object var3 = this.settins.get("cachedSectionsMaxStock");
         if (var3 == null) {
            var3 = new JSONObject();
         }

         JSONObject var4 = (JSONObject)var3;
         var4.put(var1, (long)var2);
         this.settins.put("cachedSectionsMaxStock", var4);
      }
   }

   public void removeCachedMaxPrice(String var1) {
      Object var2 = this.settins.get("cachedMaxPrices");
      if (var2 == null) {
         var2 = new JSONObject();
      }

      JSONObject var3 = (JSONObject)var2;
      if (var3.containsKey(var1)) {
         var3.remove(var1);
      }

      this.settins.put("cachedMaxPrices", var3);
   }

   public void removeCachedMaxStock(String var1) {
      Object var2 = this.settins.get("cachedSectionsMaxStock");
      if (var2 == null) {
         var2 = new JSONObject();
      }

      JSONObject var3 = (JSONObject)var2;
      if (var3.containsKey(var1)) {
         var3.remove(var1);
      }

      this.settins.put("cachedSectionsMaxStock", var3);
   }

   public void write() {
      try {
         FileWriter var1 = new FileWriter(this.file);

         try {
            var1.write(this.settins.toJSONString());
            var1.flush();
         } catch (Throwable var5) {
            try {
               var1.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         var1.close();
      } catch (IOException var6) {
         SendMessage.warnMessage("Failed to save DynamicPricing cache to 'dp_cache.json'");
         var6.printStackTrace();
      }
   }
}
