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

   public SkullCache(OnyxShop plugin) {
      this.file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + "skullcache.json");

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
         SendMessage.warnMessage("Failed to create 'skullcache.json' cache file");
         exception.printStackTrace();
      }

      try {
         FileReader fileReader = new FileReader(this.file);

         try {
            this.skullCache = JsonUtil.parseJson(fileReader);
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
         SendMessage.warnMessage("Failed to load skullcache from 'skullcache.json'");
         exception.printStackTrace();
      }
   }

   public synchronized String getSkull(String text) {
      return (String)this.skullCache.get(text);
   }

   public synchronized SkullCache.PlayerTexture getTexture(String playerName) {
      Object value = this.skullCache.get(playerName);
      return value == null ? null : new SkullCache.PlayerTexture((String)value);
   }

   public synchronized SkullCache.PlayerTexture cacheSkull(String playerName, SkullCache.PlayerTexture playerTexture) {
      this.skullCache.put(playerName, new Gson().toJson(playerTexture.getTexture()));
      return playerTexture;
   }

   public synchronized String cacheSkull(String text, String text2) {
      this.skullCache.put(text, text2);
      return text2;
   }

   public synchronized void write() {
      try {
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(this.skullCache.toJSONString());
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
         SendMessage.warnMessage("Failed to save skull cache to 'skullcache.json'");
         exception.printStackTrace();
      }
   }

   public static class PlayerTexture {
      private final String texture;

      public PlayerTexture(String playerName) {
         this.texture = playerName;
      }

      public String getTexture() {
         return this.texture;
      }
   }
}
