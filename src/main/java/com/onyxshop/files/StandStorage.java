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

   public StandStorage(OnyxShop plugin) {
      this.file = new File(plugin.getDataFolder() + File.separator + "stands.json");

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
         SendMessage.warnMessage(Lang.ERROR_CREATING_STANDS_STORAGE.get());
         exception.printStackTrace();
      }
   }

   public Set<Stand> getStoredStands() {
      HashSet values = new HashSet();

      try {
         FileReader fileReader = new FileReader(this.file);

         try {
            for (Object value : JsonUtil.parseArray(fileReader)) {
               try {
                  JSONObject jsonObject = (JSONObject)value;
                  int id = JsonUtil.getLong(jsonObject, "stand_id").intValue();
                  SimpleLocation simpleLocation = new SimpleLocation(JsonUtil.getString(jsonObject, "location"), id);
                  StandType standType = StandType.fromName(JsonUtil.getString(jsonObject, "type"));
                  String text = JsonUtil.getString(jsonObject, "item");
                  StandSettings standSettings = new StandSettings(JsonUtil.getString(jsonObject, "settings"));
                  values.add(new Stand(id, simpleLocation, text, standType, standSettings));
               } catch (Exception exception) {
                  SendMessage.errorMessage(Lang.ERROR_LOADING_SHOP_STAND.get().replace("%stand%", value.toString()).replace("%reason%", exception.getMessage()));
               }
            }
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
         SendMessage.warnMessage(Lang.ERROR_LOADING_SHOP_STANDS.get());
         exception.printStackTrace();
      }

      return values;
   }

   public void save(Set<Stand> values) {
      this.saveToFile(values);
   }

   private void saveToFile(Set<Stand> values) {
      JSONArray jsonArray = this.toJSON(values);
      if (jsonArray != null) {
         SendMessage.logDebugMessage(Lang.SAVING_SHOP_STANDS.get().replace("%count%", String.valueOf(values.size())));

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
            SendMessage.warnMessage(Lang.ERROR_SAVING_SHOP_STANDS.get());
            exception.printStackTrace();
         }
      }
   }

   private JSONArray toJSON(Set<Stand> values) {
      if (values.isEmpty()) {
         return null;
      } else {
         JSONArray jsonArray = new JSONArray();

         for (Stand stand : values) {
            jsonArray.add(stand.serialize());
         }

         return jsonArray;
      }
   }
}
