package com.onyxshop.files;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.JsonUtil;
import org.json.simple.JSONObject;

public class SkullCache {
   private final File file;
   private JSONObject skullCache;

   public SkullCache(OnyxShop var1) {
      this.file = new File(var1.getDataFolder() + File.separator + "cache" + File.separator + "skullcache.json");

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
      } catch (IOException var8) {
         SendMessage.warnMessage("Failed to create 'skullcache.json' cache file");
         var8.printStackTrace();
      }

      try {
         FileReader var9 = new FileReader(this.file);

         try {
            this.skullCache = JsonUtil.parseJson(var9);
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
         SendMessage.warnMessage("Failed to load skullcache from 'skullcache.json'");
         var7.printStackTrace();
      }
   }

   public String getSkull(String var1) {
      return (String)this.skullCache.get(var1);
   }

   public SkullCache.PlayerTexture getTexture(String var1) {
      Object var2 = this.skullCache.get(var1);
      return var2 == null ? null : new SkullCache.PlayerTexture((String)var2);
   }

   public SkullCache.PlayerTexture cacheSkull(String var1, SkullCache.PlayerTexture var2) {
      this.skullCache.put(var1, new Gson().toJson(var2.getTexture()));
      return var2;
   }

   public String cacheSkull(String var1, String var2) {
      this.skullCache.put(var1, var2);
      return var2;
   }

   public void write() {
      try {
         FileWriter var1 = new FileWriter(this.file);

         try {
            var1.write(this.skullCache.toJSONString());
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
         SendMessage.warnMessage("Failed to save skull cache to 'skullcache.json'");
         var6.printStackTrace();
      }
   }

   public static class PlayerTexture {
      private final String texture;

      public PlayerTexture(String var1) {
         this.texture = var1;
      }

      public String getTexture() {
         return this.texture;
      }
   }
}
