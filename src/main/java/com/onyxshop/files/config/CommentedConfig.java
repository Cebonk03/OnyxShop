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

   public CommentedConfig(OnyxShop var1, File var2, InputStream var3) throws ConfigLoadException {
      super(var2);
      Object var4 = new HashMap();

      try {
         BufferedReader var5 = new BufferedReader(new InputStreamReader(var3, StandardCharsets.UTF_8));
         var4 = this.getComments(var5.lines().collect(Collectors.toList()));
         var5.close();
      } catch (FileNotFoundException var6) {
      } catch (IOException var7) {
         throw new ConfigLoadException("A unknown IOE Exception occurred while loading " + var2.getName() + "\n" + this.stackTraceToString(var7));
      }

      this.comments = (Map<String, String>)var4;
      this.setDef(var1.loadConfiguration(new BufferedReader(new InputStreamReader(var1.getResource(var2.getName()), StandardCharsets.UTF_8)), var2.getName()));
   }

   private Set<String> getKeys() {
      LinkedHashSet var1 = new LinkedHashSet();
      Set var2 = this.getKeys(false);
      var2.removeAll(var1);

      for (String var4 : this.defaults.getKeys(false)) {
         var1.add(var4);
         Object var5 = this.get(var4);
         if (var5 instanceof MemorySection) {
            for (String var7 : (this.specialKeys.contains(var4) ? this.getDef() : this).getConfigurationSection(var4).getKeys(true)) {
               var1.add(var4 + "." + var7);
            }
         } else if (var5 == null) {
            ConfigurationSection var6 = this.getDef().getConfigurationSection(var4);
            if (var6 != null) {
               var6.getKeys(true).forEach(var2x -> var1.add(var4 + "." + var2x));
            }

            this.reload = true;
         }
      }

      var1.addAll(var2);
      return var1;
   }

   @Override
   public void save() throws ConfigSaveException {
      try {
         File var17 = new File(this.getFilePath().toString() + ".tmp");
         BufferedWriter var1 = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(var17.toPath()), StandardCharsets.UTF_8));
         Yaml var2 = new Yaml();

         for (String var4 : this.getKeys()) {
            String[] var5 = var4.split("\\.");
            String var6 = var5[var5.length - 1];
            String var7 = this.comments.get(var4);
            StringBuilder var8 = new StringBuilder();
            int var9 = var5.length - 1;
            this.appendPrefixSpaces(var8, var9);
            String var10 = var8.toString();
            if (var7 != null) {
               var1.write(var7);
            }

            Object var11 = this.get(var4, this.getDef().get(var4));
            if (var11 instanceof ConfigurationSerializable) {
               var1.write(var10 + var6 + ": " + var2.dump(((ConfigurationSerializable)var11).serialize()));
            } else if (!(var11 instanceof String) && !(var11 instanceof Character)) {
               if (var11 instanceof List) {
                  var1.write(this.getListAsString((List)var11, var6, var10));
               } else if (var11 instanceof MemorySection) {
                  var1.write(var10 + var6 + ":\n");
               } else {
                  var1.write(var10 + var6 + ": " + var2.dump(var11));
               }
            } else if (var11 instanceof String && ((String)var11).contains("\n")) {
               var1.write(var10 + var6 + ": \"" + ((String)var11).replace("\n", "\\n") + "\"\n");
            } else {
               String var12 = var2.dump(var11);
               if (!var12.startsWith("|")) {
                  var1.write(var10 + var6 + ": " + var12);
               } else {
                  String[] var13 = var12.substring(0, var12.length() - 1).split("\n");
                  var1.write(var10 + var6 + ": " + var13[0] + "\n");

                  for (int var14 = 1; var14 < var13.length; var14++) {
                     var1.write(var10 + "  " + var13[var14] + "\n");
                  }
               }
            }
         }

         String var16 = this.comments.get(null);
         if (var16 != null) {
            var1.write(var16);
         }

         var1.close();
         Files.move(
            var17.toPath(),
            this.getFilePath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE
         );
         if (this.reload) {
            this.reload = false;
            this.reload();
         }
      } catch (IOException | ConfigLoadException var15) {
         throw new ConfigSaveException(
            Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", this.getFilePath().getFileName().toString()) + "\n" + this.stackTraceToString(var15)
         );
      }
   }

   private String stackTraceToString(Exception var1) {
      StringWriter var2 = new StringWriter();
      PrintWriter var3 = new PrintWriter(var2);
      var1.printStackTrace(var3);
      return var2.toString();
   }

   private Map<String, String> getComments(List<String> var1) {
      HashMap var2 = new HashMap();
      StringBuilder var3 = new StringBuilder();
      StringBuilder var4 = new StringBuilder();
      int var5 = 0;

      for (String var7 : var1) {
         if (var7 == null || !var7.trim().startsWith("-")) {
            if (var7 != null && !var7.trim().equals("") && !var7.trim().startsWith("#")) {
               var5 = this.setFullKey(var4, var7, var5);
               if (var4.length() > 0) {
                  var2.put(var4.toString(), var3.toString());
                  var3.setLength(0);
               }
            } else {
               var3.append(var7).append("\n");
            }
         }
      }

      if (var3.length() > 0) {
         var2.put(null, var3.toString());
      }

      return var2;
   }

   private String getListAsString(List var1, String var2, String var3) {
      StringBuilder var4 = new StringBuilder(var3).append(var2).append(":");
      if (var1.isEmpty()) {
         var4.append(" []\n");
         return var4.toString();
      } else {
         var4.append("\n");

         for (int var5 = 0; var5 < var1.size(); var5++) {
            Object var6 = var1.get(var5);
            if (var6 instanceof String || var6 instanceof Character) {
               String var7 = var6.toString();
               if (var7.contains("\"") || var7.contains("'")) {
                  var7 = var7.replace("'", "''");
               }

               var4.append(var3).append("- '").append(var7).append("'");
            } else if (var6 instanceof List) {
               var4.append(var3).append("- ").append(new Yaml().dump(var6));
            } else {
               var4.append(var3).append("- ").append(var6);
            }

            if (var5 != var1.size()) {
               var4.append("\n");
            }
         }

         return var4.toString();
      }
   }

   private int setFullKey(StringBuilder var1, String var2, int var3) {
      int var4 = this.countIndents(var2);
      String var5 = var2.trim().split(":")[0];
      if (var1.length() == 0) {
         var1.append(var5);
      } else if (var4 == var3) {
         this.removeLastKey(var1);
         if (var1.length() > 0) {
            var1.append(".");
         }

         var1.append(var5);
      } else if (var4 > var3) {
         var1.append(".").append(var5);
      } else {
         int var6 = var3 - var4;

         for (int var7 = 0; var7 < var6 + 1; var7++) {
            this.removeLastKey(var1);
         }

         if (var1.length() > 0) {
            var1.append(".");
         }

         var1.append(var5);
      }

      return var4;
   }

   private int countIndents(String var1) {
      int var2 = 0;

      for (char var6 : var1.toCharArray()) {
         if (var6 != ' ') {
            break;
         }

         var2++;
      }

      return var2 / 2;
   }

   private void removeLastKey(StringBuilder var1) {
      String var2 = var1.toString();
      String[] var3 = var2.split("\\.");
      if (var3.length == 1) {
         var1.setLength(0);
      } else {
         var2 = var2.substring(0, var2.length() - var3[var3.length - 1].length() - 1);
         var1.setLength(var2.length());
      }
   }

   private String getPrefixSpaces(int var1) {
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1; var3++) {
         var2.append("  ");
      }

      return var2.toString();
   }

   private void appendPrefixSpaces(StringBuilder var1, int var2) {
      var1.append(this.getPrefixSpaces(var2));
   }

   private final class ConfigurationDefaults {
      public Configuration defaults;

      public ConfigurationDefaults(Configuration nullx) {
         this.defaults = nullx;
      }
   }
}
