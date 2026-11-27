package com.onyxshop.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class NBTUtils {
   public static NBTUtils.NBTFixer fixer;
   private static final boolean autoUpdate = ConfigManager.getConfig().getBoolean("auto-update-components", true);

   public NBTUtils() {
   }

   public static String getNBT(ItemStack itemStack, ConfigurationSection section, ConfigManager.CFile cFile, String itemId) throws ItemLoadException {
      return getNBT(itemStack, section, cFile, itemId, false);
   }

   public static String getNBT(ItemStack itemStack, ConfigurationSection section, ConfigManager.CFile cFile, String itemId, boolean flag) throws ItemLoadException {
      if (section.contains("components") && ServerInfo.supportsComponents()) {
         return flag && autoUpdate && fixer != null ? update(itemStack, section, cFile, itemId, section.getString("components")) : section.getString("components");
      } else if (section.contains("NBTData")) {
         if (ServerInfo.supportsComponents()) {
            if (flag) {
               SendMessage.logDebugMessage(
                  "Found NBTData option at %path% in ".replace("%path%", itemId) + cFile.getName() + ", automatically converting to components format..."
               );
               return convert(itemStack, section, cFile, section.getString("NBTData"));
            } else {
               throw new ItemLoadException("Server version does not support the NBTData item option, should be components option");
            }
         } else {
            return section.getString("NBTData");
         }
      } else {
         return null;
      }
   }

   public static String getNBT(ItemStack itemStack, Map<String, String> itemIds, String text, int value, boolean flag) throws ItemLoadException {
      if (itemIds.containsKey("components") && ServerInfo.supportsComponents()) {
         return flag && autoUpdate && fixer != null
            ? update(itemStack, itemIds, getFileFromPath(text), text, value, (String)itemIds.get("components"))
            : (String)itemIds.get("components");
      } else if (itemIds.containsKey("NBTData")) {
         if (ServerInfo.supportsComponents()) {
            if (flag) {
               ConfigManager.CFile cFile = getFileFromPath(text);
               SendMessage.logDebugMessage(
                  "Found NBTData option at %path% in ".replace("%path%", text) + cFile.getName() + ", automatically converting to components format..."
               );
               return convert(itemStack, itemIds, cFile, text, value, (String)itemIds.get("NBTData"));
            } else {
               throw new ItemLoadException("Server version does not support the NBTData item option, should be components option");
            }
         } else {
            return (String)itemIds.get("NBTData");
         }
      } else {
         return null;
      }
   }

   private static String convert(ItemStack itemStack, ConfigurationSection section, ConfigManager.CFile cFile, String itemId) throws ItemLoadException {
      try {
         String shopId = OnyxShop.getInstance().versionHandler.getClass().getName();
         String text = (String)ReflectUtil.invokeLegacyVersionMethod("convert", shopId + "$NBTSerializer", itemStack, itemId);
         section.set("components", text);
         if (cFile.type != ConfigManager.CFile.Type.SHOP) {
            cFile.save();
         } else {
            SaveTask.addSaveTask(cFile);
         }

         return text;
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new ItemLoadException("Failed to convert NBT to components data for " + section.getCurrentPath() + " at " + cFile.getName() + ".yml");
      }
   }

   private static String convert(ItemStack itemStack, Map<String, String> itemIds, ConfigManager.CFile cFile, String text, int value, String text2) throws ItemLoadException {
      try {
         String shopId = OnyxShop.getInstance().versionHandler.getClass().getName();
         String text3 = (String)ReflectUtil.invokeLegacyVersionMethod("convert", shopId + "$NBTSerializer", itemStack, text2);
         itemIds.put("components", text3);
         saveData(
            cFile,
            text.contains(".") ? text.split("\\.", 2)[1] : text,
            value,
            "ITEM:" + itemIds.entrySet().stream().map(entry -> (String)entry.getKey() + ":" + (String)entry.getValue()).collect(Collectors.joining(" "))
         );
         if (cFile.type != ConfigManager.CFile.Type.SHOP) {
            cFile.save();
         } else {
            SaveTask.addSaveTask(cFile);
         }

         return text3;
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new ItemLoadException(
            "Failed to convert NBT to components data for " + text + " at " + (text.contains(".") ? text.split("\\.") : "config") + ".yml"
         );
      }
   }

   private static String update(ItemStack itemStack, ConfigurationSection section, ConfigManager.CFile cFile, String itemId, String sectionId) throws ItemLoadException {
      try {
         SendMessage.logDebugMessage("Updating item component data at %path% in ".replace("%path%", itemId) + cFile.getName() + " to latest data version");
         String shopId = OnyxShop.getInstance().versionHandler.getClass().getName();
         String text = (String)ReflectUtil.invokeLegacyVersionMethod("update", shopId + "$NBTSerializer", fixer, itemStack, sectionId);
         section.set("components", text);
         if (cFile.type != ConfigManager.CFile.Type.SHOP) {
            cFile.save();
         } else {
            SaveTask.addSaveTask(cFile);
         }

         SaveTask.addBackupTask(cFile);
         return text;
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new ItemLoadException("Failed to update components data for item " + section.getCurrentPath() + " at " + cFile.getName() + ".yml");
      }
   }

   private static String update(ItemStack itemStack, Map<String, String> itemIds, ConfigManager.CFile cFile, String text, int value, String text2) throws ItemLoadException {
      try {
         SendMessage.logDebugMessage("Updating item component data at %path% in ".replace("%path%", text) + cFile.getName() + " to latest data version");
         String shopId = OnyxShop.getInstance().versionHandler.getClass().getName();
         String text3 = (String)ReflectUtil.invokeLegacyVersionMethod("update", shopId + "$NBTSerializer", fixer, itemStack, text2);
         itemIds.put("components", text3);
         saveData(
            cFile,
            text.contains(".") ? text.split("\\.", 2)[1] : text,
            value,
            "ITEM:" + itemIds.entrySet().stream().map(entry -> (String)entry.getKey() + ":" + (String)entry.getValue()).collect(Collectors.joining(" "))
         );
         if (cFile.type != ConfigManager.CFile.Type.SHOP) {
            cFile.save();
         } else {
            SaveTask.addSaveTask(cFile);
         }

         SaveTask.addBackupTask(cFile);
         return text3;
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new ItemLoadException(
            "Failed to convert NBT to components data for " + text + " at " + (text.contains(".") ? text.split("\\.") : "config") + ".yml"
         );
      }
   }

   private static void saveData(ConfigManager.CFile cFile, String text, int value, String text2) {
      switch (cFile.type) {
         case CONFIG:
            cFile.get().set("economy-provider", text2);
         case SECTION:
            cFile.get().set(text, text2);
            break;
         case SHOP:
            if (text.endsWith("economy")) {
               cFile.get().set(text, text2);
            } else if (text.endsWith("prices")) {
               ConfigUtil.createBackup(cFile.getName(), true);
               List stringList = cFile.get().getStringList(text);
               String text3 = (String)stringList.get(value);
               stringList.set(value, text2 + "::" + text3.split("::")[1]);
               cFile.get().set(text, stringList);
            }
      }
   }

   private static ConfigManager.CFile getFileFromPath(String path) throws ItemLoadException {
      String text = path.contains(".") ? path.split("\\.", 2)[1] : path;
      switch (text) {
         case "economy-provider":
            return new ConfigManager.CFile(ConfigManager.CFile.Type.CONFIG, "config");
         case "economy":
            String text2 = path.split("\\.")[0];
            if (ConfigManager.getSection(text2) != null) {
               return new ConfigManager.CFile(ConfigManager.CFile.Type.SECTION, text2);
            }
            break;
         default:
            if (path.endsWith("economy") || path.endsWith("prices")) {
               String text3 = path.split("\\.")[0];
               if (ConfigManager.getShop(text3) != null) {
                  return new ConfigManager.CFile(ConfigManager.CFile.Type.SHOP, text3);
               }
            }
      }

      throw new ItemLoadException("Cannot find config file for path " + path);
   }

   static {
      if (ServerInfo.getVersion() != ServerInfo.Version.UNKOWN_VERSION && ServerInfo.supportsComponents()) {
         NBTUtils.NBTVersion nbtVersion = NBTUtils.NBTVersion.getFromConfig();
         NBTUtils.NBTVersion nbtVersion2 = NBTUtils.NBTVersion.getLatest();
         if (nbtVersion != nbtVersion2) {
            if (autoUpdate) {
               SendMessage.logDebugMessage("Detected new component version for " + nbtVersion2.version + ", updating from " + nbtVersion.version);
            }

            fixer = new NBTUtils.NBTFixer(nbtVersion.dataVersion, nbtVersion2.dataVersion);
            nbtVersion2.saveToConfig();
         } else {
            nbtVersion.saveToConfig();
         }
      }
   }

   public static class NBTFixer {
      public final int from;
      public final int to;

      public NBTFixer(int value, int value2) {
         this.from = value;
         this.to = value2;
      }
   }

   public static enum NBTVersion {
      v2_0("1.20.5", 3837),
      v2_2("1.21.4", 4189),
      v2_3("1.21.5", 4323);

      public static final NBTUtils.NBTFixer compFixer;
      final String version;
      final int dataVersion;

      private NBTVersion(String text, int value) {
         this.version = text;
         this.dataVersion = value;
      }

      public static NBTUtils.NBTVersion getLatest() {
         NBTUtils.NBTVersion nbtVersion = v2_0;

         for (NBTUtils.NBTVersion nbtVersion2 : values()) {
            if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.getFromSimple(nbtVersion2.version))) {
               nbtVersion = nbtVersion2;
            }
         }

         return nbtVersion;
      }

      public static NBTUtils.NBTVersion getFromConfig() {
         String text = ConfigManager.getConfig().getString("config-version");
         if (text != null && text.contains("-")) {
            try {
               double amount = Double.parseDouble(text.split("-")[1]);
               return getFromSimple(String.valueOf(amount));
            } catch (NumberFormatException exception) {
               return v2_0;
            }
         } else {
            return getLatest();
         }
      }

      public static NBTUtils.NBTVersion getFromSimple(String text) {
         for (NBTUtils.NBTVersion nbtVersion : values()) {
            if (nbtVersion.toSimple().equals(text)) {
               return nbtVersion;
            }
         }

         return v2_0;
      }

      public void saveToConfig() {
         String text = ConfigManager.getConfig().getString("config-version");
         if (text != null) {
            ConfigManager.getConfig().set("config-version", text.split("-")[0] + "-" + this.toSimple());
            ConfigManager.saveConfig();
         }
      }

      private String toSimple() {
         return this.name().substring(1).replace("_", ".");
      }

      static {
         compFixer = new NBTUtils.NBTFixer(1343, getLatest().dataVersion);
      }
   }
}
