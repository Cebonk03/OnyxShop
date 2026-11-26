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

   public static String getNBT(ItemStack var0, ConfigurationSection var1, ConfigManager.CFile var2, String var3) throws ItemLoadException {
      return getNBT(var0, var1, var2, var3, false);
   }

   public static String getNBT(ItemStack var0, ConfigurationSection var1, ConfigManager.CFile var2, String var3, boolean var4) throws ItemLoadException {
      if (var1.contains("components") && ServerInfo.supportsComponents()) {
         return var4 && autoUpdate && fixer != null ? update(var0, var1, var2, var3, var1.getString("components")) : var1.getString("components");
      } else if (var1.contains("NBTData")) {
         if (ServerInfo.supportsComponents()) {
            if (var4) {
               SendMessage.logDebugMessage(
                  "Found NBTData option at %path% in ".replace("%path%", var3) + var2.getName() + ", automatically converting to components format..."
               );
               return convert(var0, var1, var2, var1.getString("NBTData"));
            } else {
               throw new ItemLoadException("Server version does not support the NBTData item option, should be components option");
            }
         } else {
            return var1.getString("NBTData");
         }
      } else {
         return null;
      }
   }

   public static String getNBT(ItemStack var0, Map<String, String> var1, String var2, int var3, boolean var4) throws ItemLoadException {
      if (var1.containsKey("components") && ServerInfo.supportsComponents()) {
         return var4 && autoUpdate && fixer != null
            ? update(var0, var1, getFileFromPath(var2), var2, var3, (String)var1.get("components"))
            : (String)var1.get("components");
      } else if (var1.containsKey("NBTData")) {
         if (ServerInfo.supportsComponents()) {
            if (var4) {
               ConfigManager.CFile var5 = getFileFromPath(var2);
               SendMessage.logDebugMessage(
                  "Found NBTData option at %path% in ".replace("%path%", var2) + var5.getName() + ", automatically converting to components format..."
               );
               return convert(var0, var1, var5, var2, var3, (String)var1.get("NBTData"));
            } else {
               throw new ItemLoadException("Server version does not support the NBTData item option, should be components option");
            }
         } else {
            return (String)var1.get("NBTData");
         }
      } else {
         return null;
      }
   }

   private static String convert(ItemStack var0, ConfigurationSection var1, ConfigManager.CFile var2, String var3) throws ItemLoadException {
      try {
         String var4 = OnyxShop.getInstance().versionHandler.getClass().getName();
         String var5 = (String)ReflectUtil.invokeLegacyVersionMethod("convert", var4 + "$NBTSerializer", var0, var3);
         var1.set("components", var5);
         if (var2.type != ConfigManager.CFile.Type.SHOP) {
            var2.save();
         } else {
            SaveTask.addSaveTask(var2);
         }

         return var5;
      } catch (Exception var6) {
         var6.printStackTrace();
         throw new ItemLoadException("Failed to convert NBT to components data for " + var1.getCurrentPath() + " at " + var2.getName() + ".yml");
      }
   }

   private static String convert(ItemStack var0, Map<String, String> var1, ConfigManager.CFile var2, String var3, int var4, String var5) throws ItemLoadException {
      try {
         String var6 = OnyxShop.getInstance().versionHandler.getClass().getName();
         String var7 = (String)ReflectUtil.invokeLegacyVersionMethod("convert", var6 + "$NBTSerializer", var0, var5);
         var1.put("components", var7);
         saveData(
            var2,
            var3.contains(".") ? var3.split("\\.", 2)[1] : var3,
            var4,
            "ITEM:" + var1.entrySet().stream().map(var0x -> (String)var0x.getKey() + ":" + (String)var0x.getValue()).collect(Collectors.joining(" "))
         );
         if (var2.type != ConfigManager.CFile.Type.SHOP) {
            var2.save();
         } else {
            SaveTask.addSaveTask(var2);
         }

         return var7;
      } catch (Exception var8) {
         var8.printStackTrace();
         throw new ItemLoadException(
            "Failed to convert NBT to components data for " + var3 + " at " + (var3.contains(".") ? var3.split("\\.") : "config") + ".yml"
         );
      }
   }

   private static String update(ItemStack var0, ConfigurationSection var1, ConfigManager.CFile var2, String var3, String var4) throws ItemLoadException {
      try {
         SendMessage.logDebugMessage("Updating item component data at %path% in ".replace("%path%", var3) + var2.getName() + " to latest data version");
         String var5 = OnyxShop.getInstance().versionHandler.getClass().getName();
         String var6 = (String)ReflectUtil.invokeLegacyVersionMethod("update", var5 + "$NBTSerializer", fixer, var0, var4);
         var1.set("components", var6);
         if (var2.type != ConfigManager.CFile.Type.SHOP) {
            var2.save();
         } else {
            SaveTask.addSaveTask(var2);
         }

         SaveTask.addBackupTask(var2);
         return var6;
      } catch (Exception var7) {
         var7.printStackTrace();
         throw new ItemLoadException("Failed to update components data for item " + var1.getCurrentPath() + " at " + var2.getName() + ".yml");
      }
   }

   private static String update(ItemStack var0, Map<String, String> var1, ConfigManager.CFile var2, String var3, int var4, String var5) throws ItemLoadException {
      try {
         SendMessage.logDebugMessage("Updating item component data at %path% in ".replace("%path%", var3) + var2.getName() + " to latest data version");
         String var6 = OnyxShop.getInstance().versionHandler.getClass().getName();
         String var7 = (String)ReflectUtil.invokeLegacyVersionMethod("update", var6 + "$NBTSerializer", fixer, var0, var5);
         var1.put("components", var7);
         saveData(
            var2,
            var3.contains(".") ? var3.split("\\.", 2)[1] : var3,
            var4,
            "ITEM:" + var1.entrySet().stream().map(var0x -> (String)var0x.getKey() + ":" + (String)var0x.getValue()).collect(Collectors.joining(" "))
         );
         if (var2.type != ConfigManager.CFile.Type.SHOP) {
            var2.save();
         } else {
            SaveTask.addSaveTask(var2);
         }

         SaveTask.addBackupTask(var2);
         return var7;
      } catch (Exception var8) {
         var8.printStackTrace();
         throw new ItemLoadException(
            "Failed to convert NBT to components data for " + var3 + " at " + (var3.contains(".") ? var3.split("\\.") : "config") + ".yml"
         );
      }
   }

   private static void saveData(ConfigManager.CFile var0, String var1, int var2, String var3) {
      switch (var0.type) {
         case CONFIG:
            var0.get().set("economy-provider", var3);
         case SECTION:
            var0.get().set(var1, var3);
            break;
         case SHOP:
            if (var1.endsWith("economy")) {
               var0.get().set(var1, var3);
            } else if (var1.endsWith("prices")) {
               ConfigUtil.createBackup(var0.getName(), true);
               List var4 = var0.get().getStringList(var1);
               String var5 = (String)var4.get(var2);
               var4.set(var2, var3 + "::" + var5.split("::")[1]);
               var0.get().set(var1, var4);
            }
      }
   }

   private static ConfigManager.CFile getFileFromPath(String var0) throws ItemLoadException {
      String var1 = var0.contains(".") ? var0.split("\\.", 2)[1] : var0;
      switch (var1) {
         case "economy-provider":
            return new ConfigManager.CFile(ConfigManager.CFile.Type.CONFIG, "config");
         case "economy":
            String var3 = var0.split("\\.")[0];
            if (ConfigManager.getSection(var3) != null) {
               return new ConfigManager.CFile(ConfigManager.CFile.Type.SECTION, var3);
            }
            break;
         default:
            if (var0.endsWith("economy") || var0.endsWith("prices")) {
               String var4 = var0.split("\\.")[0];
               if (ConfigManager.getShop(var4) != null) {
                  return new ConfigManager.CFile(ConfigManager.CFile.Type.SHOP, var4);
               }
            }
      }

      throw new ItemLoadException("Cannot find config file for path " + var0);
   }

   static {
      if (ServerInfo.getVersion() != ServerInfo.Version.UNKOWN_VERSION && ServerInfo.supportsComponents()) {
         NBTUtils.NBTVersion var0 = NBTUtils.NBTVersion.getFromConfig();
         NBTUtils.NBTVersion var1 = NBTUtils.NBTVersion.getLatest();
         if (var0 != var1) {
            if (autoUpdate) {
               SendMessage.logDebugMessage("Detected new component version for " + var1.version + ", updating from " + var0.version);
            }

            fixer = new NBTUtils.NBTFixer(var0.dataVersion, var1.dataVersion);
            var1.saveToConfig();
         } else {
            var0.saveToConfig();
         }
      }
   }

   public static class NBTFixer {
      public final int from;
      public final int to;

      public NBTFixer(int var1, int var2) {
         this.from = var1;
         this.to = var2;
      }
   }

   public static enum NBTVersion {
      v2_0("1.20.5", 3837),
      v2_2("1.21.4", 4189),
      v2_3("1.21.5", 4323);

      public static final NBTUtils.NBTFixer compFixer;
      final String version;
      final int dataVersion;

      private NBTVersion(String nullxx, int nullxxx) {
         this.version = nullxx;
         this.dataVersion = nullxxx;
      }

      public static NBTUtils.NBTVersion getLatest() {
         NBTUtils.NBTVersion var0 = v2_0;

         for (NBTUtils.NBTVersion var4 : values()) {
            if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.getFromSimple(var4.version))) {
               var0 = var4;
            }
         }

         return var0;
      }

      public static NBTUtils.NBTVersion getFromConfig() {
         String var0 = ConfigManager.getConfig().getString("config-version");
         if (var0 != null && var0.contains("-")) {
            try {
               double var1 = Double.parseDouble(var0.split("-")[1]);
               return getFromSimple(String.valueOf(var1));
            } catch (NumberFormatException var3) {
               return v2_0;
            }
         } else {
            return getLatest();
         }
      }

      public static NBTUtils.NBTVersion getFromSimple(String var0) {
         for (NBTUtils.NBTVersion var4 : values()) {
            if (var4.toSimple().equals(var0)) {
               return var4;
            }
         }

         return v2_0;
      }

      public void saveToConfig() {
         String var1 = ConfigManager.getConfig().getString("config-version");
         if (var1 != null) {
            ConfigManager.getConfig().set("config-version", var1.split("-")[0] + "-" + this.toSimple());
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
