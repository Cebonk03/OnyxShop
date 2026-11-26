package com.onyxshop.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.stands.Stand;
import com.onyxshop.objects.stands.StandSettings;
import com.onyxshop.objects.stands.StandType;
import com.onyxshop.util.JsonUtil;
import com.onyxshop.util.SimpleLocation;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StandStorage {
   private final File file;

   public StandStorage(OnyxShop var1) {
      this.file = new File(var1.getDataFolder() + File.separator + "stands.json");

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
      } catch (IOException var3) {
         SendMessage.warnMessage(Lang.ERROR_CREATING_STANDS_STORAGE.get());
         var3.printStackTrace();
      }
   }

   public Set<Stand> getStoredStands() {
      HashSet var1 = new HashSet();

      try {
         FileReader var2 = new FileReader(this.file);

         try {
            for (Object var4 : JsonUtil.parseArray(var2)) {
               try {
                  JSONObject var5 = (JSONObject)var4;
                  int var6 = JsonUtil.getLong(var5, "stand_id").intValue();
                  SimpleLocation var7 = new SimpleLocation(JsonUtil.getString(var5, "location"), var6);
                  StandType var8 = StandType.fromName(JsonUtil.getString(var5, "type"));
                  String var9 = JsonUtil.getString(var5, "item");
                  StandSettings var10 = new StandSettings(JsonUtil.getString(var5, "settings"));
                  var1.add(new Stand(var6, var7, var9, var8, var10));
               } catch (Exception var12) {
                  SendMessage.errorMessage(Lang.ERROR_LOADING_SHOP_STAND.get().replace("%stand%", var4.toString()).replace("%reason%", var12.getMessage()));
               }
            }
         } catch (Throwable var13) {
            try {
               var2.close();
            } catch (Throwable var11) {
               var13.addSuppressed(var11);
            }

            throw var13;
         }

         var2.close();
      } catch (Exception var14) {
         SendMessage.warnMessage(Lang.ERROR_LOADING_SHOP_STANDS.get());
         var14.printStackTrace();
      }

      return var1;
   }

   public void save(Set<Stand> var1) {
      this.saveToFile(var1);
   }

   private void saveToFile(Set<Stand> var1) {
      JSONArray var2 = this.toJSON(var1);
      if (var2 != null) {
         SendMessage.logDebugMessage(Lang.SAVING_SHOP_STANDS.get().replace("%count%", String.valueOf(var1.size())));

         try {
            FileWriter var3 = new FileWriter(this.file);

            try {
               var3.write(var2.toJSONString());
               var3.flush();
            } catch (Throwable var7) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            var3.close();
         } catch (IOException var8) {
            SendMessage.warnMessage(Lang.ERROR_SAVING_SHOP_STANDS.get());
            var8.printStackTrace();
         }
      }
   }

   private JSONArray toJSON(Set<Stand> var1) {
      if (var1.isEmpty()) {
         return null;
      } else {
         JSONArray var2 = new JSONArray();

         for (Stand var4 : var1) {
            var2.add(var4.serialize());
         }

         return var2;
      }
   }
}
