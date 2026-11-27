package com.onyxshop.files.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.exceptions.ConfigSaveException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.yaml.snakeyaml.Yaml;

public class CommentedConfig extends Config {
   private final Set<String> specialKeys = new HashSet<>(
      Arrays.asList(
         "display-lore-layout", "dynamic-pricing", "visual-transaction-limits", "disable-back", "click-mappings", "sound-effects", "placeholders", "mysql"
      )
   );
   private final Map<String, String> comments;
   private boolean reload = false;

   public CommentedConfig(OnyxShop plugin, File file, InputStream inputStream) throws ConfigLoadException {
      super(file);
      Object value = new HashMap();

      try {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
         value = this.getComments(bufferedReader.lines().collect(Collectors.toList()));
         bufferedReader.close();
      } catch (FileNotFoundException exception) {
      } catch (IOException exception) {
         throw new ConfigLoadException("A unknown IOE Exception occurred while loading " + file.getName() + "\n" + this.stackTraceToString(exception));
      }

      this.comments = (Map<String, String>)value;
      this.setDef(plugin.loadConfiguration(new BufferedReader(new InputStreamReader(plugin.getResource(file.getName()), StandardCharsets.UTF_8)), file.getName()));
   }

   private Set<String> getKeys() {
      LinkedHashSet values = new LinkedHashSet();
      Set keys = this.getKeys(false);
      keys.removeAll(values);

      for (String text : this.defaults.getKeys(false)) {
         values.add(text);
         Object value = this.get(text);
         if (value instanceof MemorySection) {
            for (String sectionId : (this.specialKeys.contains(text) ? this.getDef() : this).getConfigurationSection(text).getKeys(true)) {
               values.add(text + "." + sectionId);
            }
         } else if (value == null) {
            ConfigurationSection section = this.getDef().getConfigurationSection(text);
            if (section != null) {
               section.getKeys(true).forEach(entry -> values.add(text + "." + entry));
            }

            this.reload = true;
         }
      }

      values.addAll(keys);
      return values;
   }

   @Override
   public void save() throws ConfigSaveException {
      try {
         File file = new File(this.getFilePath().toString() + ".tmp");
         BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
         Yaml yaml = new Yaml();

         for (String text : this.getKeys()) {
            String[] values = text.split("\\.");
            String text2 = values[values.length - 1];
            String text3 = this.comments.get(text);
            StringBuilder builder = new StringBuilder();
            int value = values.length - 1;
            this.appendPrefixSpaces(builder, value);
            String text4 = builder.toString();
            if (text3 != null) {
               bufferedWriter.write(text3);
            }

            Object value2 = this.get(text, this.getDef().get(text));
            if (value2 instanceof ConfigurationSerializable) {
               bufferedWriter.write(text4 + text2 + ": " + yaml.dump(((ConfigurationSerializable)value2).serialize()));
            } else if (!(value2 instanceof String) && !(value2 instanceof Character)) {
               if (value2 instanceof List) {
                  bufferedWriter.write(this.getListAsString((List)value2, text2, text4));
               } else if (value2 instanceof MemorySection) {
                  bufferedWriter.write(text4 + text2 + ":\n");
               } else {
                  bufferedWriter.write(text4 + text2 + ": " + yaml.dump(value2));
               }
            } else if (value2 instanceof String && ((String)value2).contains("\n")) {
               bufferedWriter.write(text4 + text2 + ": \"" + ((String)value2).replace("\n", "\\n") + "\"\n");
            } else {
               String text5 = yaml.dump(value2);
               if (!text5.startsWith("|")) {
                  bufferedWriter.write(text4 + text2 + ": " + text5);
               } else {
                  String[] values2 = text5.substring(0, text5.length() - 1).split("\n");
                  bufferedWriter.write(text4 + text2 + ": " + values2[0] + "\n");

                  for (int index = 1; index < values2.length; index++) {
                     bufferedWriter.write(text4 + "  " + values2[index] + "\n");
                  }
               }
            }
         }

         String text = this.comments.get(null);
         if (text != null) {
            bufferedWriter.write(text);
         }

         bufferedWriter.close();
         Files.move(
            file.toPath(),
            this.getFilePath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE
         );
         if (this.reload) {
            this.reload = false;
            this.reload();
         }
      } catch (IOException | ConfigLoadException exception) {
         throw new ConfigSaveException(
            Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", this.getFilePath().getFileName().toString()) + "\n" + this.stackTraceToString(exception)
         );
      }
   }

   private String stackTraceToString(Exception exception) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      return stringWriter.toString();
   }

   private Map<String, String> getComments(List<String> values) {
      HashMap map = new HashMap();
      StringBuilder builder = new StringBuilder();
      StringBuilder builder2 = new StringBuilder();
      int value = 0;

      for (String text : values) {
         if (text == null || !text.trim().startsWith("-")) {
            if (text != null && !text.trim().equals("") && !text.trim().startsWith("#")) {
               value = this.setFullKey(builder2, text, value);
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

   private String getListAsString(List values, String text, String text2) {
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

   private int setFullKey(StringBuilder builder, String key, int value) {
      int amount = this.countIndents(key);
      String text = key.trim().split(":")[0];
      if (builder.length() == 0) {
         builder.append(text);
      } else if (amount == value) {
         this.removeLastKey(builder);
         if (builder.length() > 0) {
            builder.append(".");
         }

         builder.append(text);
      } else if (amount > value) {
         builder.append(".").append(text);
      } else {
         int value2 = value - amount;

         for (int index = 0; index < value2 + 1; index++) {
            this.removeLastKey(builder);
         }

         if (builder.length() > 0) {
            builder.append(".");
         }

         builder.append(text);
      }

      return amount;
   }

   private int countIndents(String text) {
      int amount = 0;

      for (char character : text.toCharArray()) {
         if (character != ' ') {
            break;
         }

         amount++;
      }

      return amount / 2;
   }

   private void removeLastKey(StringBuilder builder) {
      String key = builder.toString();
      String[] values = key.split("\\.");
      if (values.length == 1) {
         builder.setLength(0);
      } else {
         key = key.substring(0, key.length() - values[values.length - 1].length() - 1);
         builder.setLength(key.length());
      }
   }

   private String getPrefixSpaces(int value) {
      StringBuilder builder = new StringBuilder();

      for (int index = 0; index < value; index++) {
         builder.append("  ");
      }

      return builder.toString();
   }

   private void appendPrefixSpaces(StringBuilder builder, int value) {
      builder.append(this.getPrefixSpaces(value));
   }

   private final class ConfigurationDefaults {
      public Configuration defaults;

      public ConfigurationDefaults(Configuration configuration) {
         this.defaults = configuration;
      }
   }
}
