package com.onyxshop.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.yaml.snakeyaml.Yaml;

public class ConfigUtil {
   public ConfigUtil() {
   }

   public static void createBackup(String var0, boolean var1) {
      createBackup(Paths.get(OnyxShop.getInstance().getDataFolder().toURI().getPath(), var0), var1);
   }

   public static void createBackup(Path var0) {
      createBackup(var0, false);
   }

   public static void createBackup(Path var0, boolean var1) {
      String var2 = var0.getFileName().toString();
      DateTimeFormatter var3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime var4 = LocalDateTime.now();
      Path var5 = Paths.get(OnyxShop.getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(var5) || !Files.isDirectory(var5)) {
         try {
            Files.createDirectory(var5);
         } catch (FileAlreadyExistsException var9) {
         } catch (IOException var10) {
            var10.printStackTrace();
         }
      }

      try {
         try {
            Files.copy(var0, Paths.get(var5.toString(), var2.split("\\.")[0] + " " + var3.format(var4) + ".yml"));
            SendMessage.infoMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", var2));
         } catch (FileAlreadyExistsException var11) {
            if (var1) {
               return;
            }

            int var7 = 1;
            String var8 = var2.split("\\.")[0] + " " + var3.format(var4) + " " + var7 + "." + var2.split("\\.")[1];

            while (new File(var5.toString(), var8).exists()) {
               var7++;
            }

            Files.copy(var0, Paths.get(var5.toString(), var8));
         }
      } catch (Exception var12) {
         var12.printStackTrace();
      }
   }

   public static boolean loadConfig() {
      if (new File(OnyxShop.getInstance().getDataFolder(), "config.yml").exists()) {
         try {
            YamlConfiguration var0 = OnyxShop.getInstance()
               .loadConfiguration(new File(OnyxShop.getInstance().getDataFolder(), "config.yml"), "config.yml");
            if (var0 == null) {
               return false;
            }

            BufferedReader var1 = new BufferedReader(new InputStreamReader(OnyxShop.getInstance().getResource("config.yml"), StandardCharsets.UTF_8));
            List<String> var2 = var1.lines().collect(Collectors.toList());
            var1.close();
            StringBuilder var3 = new StringBuilder();

            for (String var5 : var2) {
               var3.append(var5).append("\n");
            }

            Files.write(
               Paths.get(OnyxShop.getInstance().getDataFolder() + "/config.yml"),
               var3.toString().getBytes(),
               StandardOpenOption.WRITE,
               StandardOpenOption.TRUNCATE_EXISTING
            );
            File var9 = new File(OnyxShop.getInstance().getDataFolder(), "config.yml");
            YamlConfiguration var10 = OnyxShop.getInstance().loadConfiguration(var9, "config.yml");

            for (String var7 : var0.getKeys(false)) {
               var10.set(var7, var0.get(var7));
            }

            save(var10, var9);
            OnyxShop.getInstance().reloadConfig();
            if (ConfigManager.badYaml != null && ConfigManager.badYaml.equals("config.yml")) {
               ConfigManager.badYaml = null;
            }
         } catch (IOException var8) {
            SendMessage.errorMessage(
               "Cannot read config.yml config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it. If you cannot find the cause yourself, join our discord support server that can be found at a plugin page of OnyxShop."
            );
            var8.printStackTrace();
            ConfigManager.badYaml = "config.yml";
            return false;
         }
      } else {
         OnyxShop.getInstance().saveDefaultConfig();
      }

      return true;
   }

   public static void save(FileConfiguration var0, File var1) {
      if (var0 == null) {
         SendMessage.errorMessage(Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", var1.getName()));
      } else {
         try {
            BufferedReader var2 = new BufferedReader(new InputStreamReader(new FileInputStream(var1), StandardCharsets.UTF_8));
            Map var3 = getComments(var2.lines().collect(Collectors.toList()));
            var2.close();
            BufferedWriter var4 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var1), StandardCharsets.UTF_8));

            for (String var6 : var0.getKeys(true)) {
               String[] var7 = var6.split("\\.");
               String var8 = var7[var7.length - 1];
               String var9 = (String)var3.remove(var6);
               StringBuilder var10 = new StringBuilder();
               int var11 = var7.length - 1;
               appendPrefixSpaces(var10, var11);
               String var12 = var10.toString();
               if (var9 != null) {
                  var4.write(var9);
               }

               Object var13 = var0.get(var6);
               if (var13 instanceof ConfigurationSerializable) {
                  var4.write(var12 + var8 + ": " + new Yaml().dump(((ConfigurationSerializable)var13).serialize()));
               } else if (!(var13 instanceof String) && !(var13 instanceof Character)) {
                  if (var13 instanceof List) {
                     var4.write(getListAsString((List)var13, var8, var12));
                  } else if (var13 instanceof MemorySection) {
                     var4.write(var12 + var8 + ":\n");
                  } else {
                     var4.write(var12 + var8 + ": " + new Yaml().dump(var13));
                  }
               } else {
                  if (var13 instanceof String) {
                     String var14 = (String)var13;
                     var13 = var14.replace("\n", "\\n");
                  }

                  var4.write(var12 + var8 + ": " + new Yaml().dump(var13).replace("\n ", ""));
               }
            }

            String var16 = (String)var3.get(null);
            if (var16 != null) {
               var4.write(var16);
            }

            var4.close();
         } catch (IOException var15) {
            SendMessage.errorMessage(Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", var1.getName()));
            var15.printStackTrace();
         }
      }
   }

   private static Map<String, String> getComments(List<String> var0) {
      HashMap var1 = new HashMap();
      StringBuilder var2 = new StringBuilder();
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;

      for (String var6 : var0) {
         if (var6 == null || !var6.trim().startsWith("-")) {
            if (var6 != null && !var6.trim().equals("") && !var6.trim().startsWith("#")) {
               var4 = setFullKey(var3, var6, var4);
               if (var3.length() > 0) {
                  var1.put(var3.toString(), var2.toString());
                  var2.setLength(0);
               }
            } else {
               var2.append(var6).append("\n");
            }
         }
      }

      if (var2.length() > 0) {
         var1.put(null, var2.toString());
      }

      return var1;
   }

   private static int setFullKey(StringBuilder var0, String var1, int var2) {
      int var3 = countIndents(var1);
      String var4 = var1.trim().split(":")[0];
      if (var0.length() == 0) {
         var0.append(var4);
      } else if (var3 == var2) {
         removeLastKey(var0);
         if (var0.length() > 0) {
            var0.append(".");
         }

         var0.append(var4);
      } else if (var3 > var2) {
         var0.append(".").append(var4);
      } else {
         int var5 = var2 - var3;

         for (int var6 = 0; var6 < var5 + 1; var6++) {
            removeLastKey(var0);
         }

         if (var0.length() > 0) {
            var0.append(".");
         }

         var0.append(var4);
      }

      return var3;
   }

   private static String getListAsString(List var0, String var1, String var2) {
      StringBuilder var3 = new StringBuilder(var2).append(var1).append(":");
      if (var0.isEmpty()) {
         var3.append(" []\n");
         return var3.toString();
      } else {
         var3.append("\n");

         for (int var4 = 0; var4 < var0.size(); var4++) {
            Object var5 = var0.get(var4);
            if (var5 instanceof String || var5 instanceof Character) {
               String var6 = var5.toString();
               if (var6.contains("\"") || var6.contains("'")) {
                  var6 = var6.replace("'", "''");
               }

               var3.append(var2).append("- '").append(var6).append("'");
            } else if (var5 instanceof List) {
               var3.append(var2).append("- ").append(new Yaml().dump(var5));
            } else {
               var3.append(var2).append("- ").append(var5);
            }

            if (var4 != var0.size()) {
               var3.append("\n");
            }
         }

         return var3.toString();
      }
   }

   private static int countIndents(String var0) {
      int var1 = 0;

      for (char var5 : var0.toCharArray()) {
         if (var5 != ' ') {
            break;
         }

         var1++;
      }

      return var1 / 2;
   }

   private static void removeLastKey(StringBuilder var0) {
      String var1 = var0.toString();
      String[] var2 = var1.split("\\.");
      if (var2.length == 1) {
         var0.setLength(0);
      } else {
         var1 = var1.substring(0, var1.length() - var2[var2.length - 1].length() - 1);
         var0.setLength(var1.length());
      }
   }

   private static String getPrefixSpaces(int var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0; var2++) {
         var1.append("  ");
      }

      return var1.toString();
   }

   private static void appendPrefixSpaces(StringBuilder var0, int var1) {
      var0.append(getPrefixSpaces(var1));
   }
}
