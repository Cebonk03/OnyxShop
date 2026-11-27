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

   public static void createBackup(String text, boolean flag) {
      createBackup(Paths.get(OnyxShop.getInstance().getDataFolder().toURI().getPath(), text), flag);
   }

   public static void createBackup(Path path) {
      createBackup(path, false);
   }

   public static void createBackup(Path path, boolean flag) {
      String name = path.getFileName().toString();
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime localDateTime = LocalDateTime.now();
      Path path2 = Paths.get(OnyxShop.getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(path2) || !Files.isDirectory(path2)) {
         try {
            Files.createDirectory(path2);
         } catch (FileAlreadyExistsException exception) {
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }

      try {
         try {
            Files.copy(path, Paths.get(path2.toString(), name.split("\\.")[0] + " " + dateTimeFormatter.format(localDateTime) + ".yml"));
            SendMessage.infoMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", name));
         } catch (FileAlreadyExistsException exception) {
            if (flag) {
               return;
            }

            int value = 1;
            String text = name.split("\\.")[0] + " " + dateTimeFormatter.format(localDateTime) + " " + value + "." + name.split("\\.")[1];

            while (new File(path2.toString(), text).exists()) {
               value++;
            }

            Files.copy(path, Paths.get(path2.toString(), text));
         }
      } catch (Exception exception) {
         exception.printStackTrace();
      }
   }

   public static boolean loadConfig() {
      if (new File(OnyxShop.getInstance().getDataFolder(), "config.yml").exists()) {
         try {
            YamlConfiguration config = OnyxShop.getInstance()
               .loadConfiguration(new File(OnyxShop.getInstance().getDataFolder(), "config.yml"), "config.yml");
            if (config == null) {
               return false;
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(OnyxShop.getInstance().getResource("config.yml"), StandardCharsets.UTF_8));
            List<String> values = bufferedReader.lines().collect(Collectors.toList());
            bufferedReader.close();
            StringBuilder builder = new StringBuilder();

            for (String text : values) {
               builder.append(text).append("\n");
            }

            Files.write(
               Paths.get(OnyxShop.getInstance().getDataFolder() + "/config.yml"),
               builder.toString().getBytes(),
               StandardOpenOption.WRITE,
               StandardOpenOption.TRUNCATE_EXISTING
            );
            File file = new File(OnyxShop.getInstance().getDataFolder(), "config.yml");
            YamlConfiguration config2 = OnyxShop.getInstance().loadConfiguration(file, "config.yml");

            for (String text : config.getKeys(false)) {
               config2.set(text, config.get(text));
            }

            save(config2, file);
            OnyxShop.getInstance().reloadConfig();
            if (ConfigManager.badYaml != null && ConfigManager.badYaml.equals("config.yml")) {
               ConfigManager.badYaml = null;
            }
         } catch (IOException exception) {
            SendMessage.errorMessage(
               "Cannot read config.yml config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it. If you cannot find the cause yourself, join our discord support server that can be found at a plugin page of OnyxShop."
            );
            exception.printStackTrace();
            ConfigManager.badYaml = "config.yml";
            return false;
         }
      } else {
         OnyxShop.getInstance().saveDefaultConfig();
      }

      return true;
   }

   public static void save(FileConfiguration fileConfiguration, File file) {
      if (fileConfiguration == null) {
         SendMessage.errorMessage(Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", file.getName()));
      } else {
         try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            Map comments = getComments(bufferedReader.lines().collect(Collectors.toList()));
            bufferedReader.close();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            for (String text : fileConfiguration.getKeys(true)) {
               String[] values = text.split("\\.");
               String text2 = values[values.length - 1];
               String text3 = (String)comments.remove(text);
               StringBuilder builder = new StringBuilder();
               int value = values.length - 1;
               appendPrefixSpaces(builder, value);
               String text4 = builder.toString();
               if (text3 != null) {
                  bufferedWriter.write(text3);
               }

               Object value2 = fileConfiguration.get(text);
               if (value2 instanceof ConfigurationSerializable) {
                  bufferedWriter.write(text4 + text2 + ": " + new Yaml().dump(((ConfigurationSerializable)value2).serialize()));
               } else if (!(value2 instanceof String) && !(value2 instanceof Character)) {
                  if (value2 instanceof List) {
                     bufferedWriter.write(getListAsString((List)value2, text2, text4));
                  } else if (value2 instanceof MemorySection) {
                     bufferedWriter.write(text4 + text2 + ":\n");
                  } else {
                     bufferedWriter.write(text4 + text2 + ": " + new Yaml().dump(value2));
                  }
               } else {
                  if (value2 instanceof String) {
                     String text5 = (String)value2;
                     value2 = text5.replace("\n", "\\n");
                  }

                  bufferedWriter.write(text4 + text2 + ": " + new Yaml().dump(value2).replace("\n ", ""));
               }
            }

            String text = (String)comments.get(null);
            if (text != null) {
               bufferedWriter.write(text);
            }

            bufferedWriter.close();
         } catch (IOException exception) {
            SendMessage.errorMessage(Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", file.getName()));
            exception.printStackTrace();
         }
      }
   }

   private static Map<String, String> getComments(List<String> values) {
      HashMap map = new HashMap();
      StringBuilder builder = new StringBuilder();
      StringBuilder builder2 = new StringBuilder();
      int value = 0;

      for (String text : values) {
         if (text == null || !text.trim().startsWith("-")) {
            if (text != null && !text.trim().equals("") && !text.trim().startsWith("#")) {
               value = setFullKey(builder2, text, value);
               if (builder2.length() > 0) {
                  map.put(builder2.toString(), builder.toString());
                  builder.setLength(0);
               }
            } else {
               builder.append(text).append("\n");
            }
         }
      }

      if (builder.length() > 0) {
         map.put(null, builder.toString());
      }

      return map;
   }

   private static int setFullKey(StringBuilder builder, String key, int value) {
      int amount = countIndents(key);
      String text = key.trim().split(":")[0];
      if (builder.length() == 0) {
         builder.append(text);
      } else if (amount == value) {
         removeLastKey(builder);
         if (builder.length() > 0) {
            builder.append(".");
         }

         builder.append(text);
      } else if (amount > value) {
         builder.append(".").append(text);
      } else {
         int value2 = value - amount;

         for (int index = 0; index < value2 + 1; index++) {
            removeLastKey(builder);
         }

         if (builder.length() > 0) {
            builder.append(".");
         }

         builder.append(text);
      }

      return amount;
   }

   private static String getListAsString(List values, String text, String text2) {
      StringBuilder builder = new StringBuilder(text2).append(text).append(":");
      if (values.isEmpty()) {
         builder.append(" []\n");
         return builder.toString();
      } else {
         builder.append("\n");

         for (int size = 0; size < values.size(); size++) {
            Object value = values.get(size);
            if (value instanceof String || value instanceof Character) {
               String text3 = value.toString();
               if (text3.contains("\"") || text3.contains("'")) {
                  text3 = text3.replace("'", "''");
               }

               builder.append(text2).append("- '").append(text3).append("'");
            } else if (value instanceof List) {
               builder.append(text2).append("- ").append(new Yaml().dump(value));
            } else {
               builder.append(text2).append("- ").append(value);
            }

            if (size != values.size()) {
               builder.append("\n");
            }
         }

         return builder.toString();
      }
   }

   private static int countIndents(String text) {
      int amount = 0;

      for (char character : text.toCharArray()) {
         if (character != ' ') {
            break;
         }

         amount++;
      }

      return amount / 2;
   }

   private static void removeLastKey(StringBuilder builder) {
      String key = builder.toString();
      String[] values = key.split("\\.");
      if (values.length == 1) {
         builder.setLength(0);
      } else {
         key = key.substring(0, key.length() - values[values.length - 1].length() - 1);
         builder.setLength(key.length());
      }
   }

   private static String getPrefixSpaces(int value) {
      StringBuilder builder = new StringBuilder();

      for (int index = 0; index < value; index++) {
         builder.append("  ");
      }

      return builder.toString();
   }

   private static void appendPrefixSpaces(StringBuilder builder, int value) {
      builder.append(getPrefixSpaces(value));
   }
}
