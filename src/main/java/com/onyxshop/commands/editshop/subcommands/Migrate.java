package com.onyxshop.commands.editshop.subcommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.SQLite;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ConfigUtil;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class Migrate extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private final Map<String, List<String>> supported = this.getSupportedFiles();
   String pluginName;
   String image;

   public Migrate(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "migrate";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_MIGRATE_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_MIGRATE_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length <= 1) {
         SendMessage.sendMessage(var1, this.getSyntax());
      } else if (!this.supported.containsKey(var2[1].toLowerCase(Locale.ENGLISH))) {
         SendMessage.sendMessage(var1, Lang.PLUGIN_NOT_FOUND.get());
      } else {
         this.pluginName = var2[1].toLowerCase(Locale.ENGLISH);
         if (var2.length <= 2) {
            SendMessage.sendMessage(var1, this.getSyntax());
         } else {
            this.image = var2[2].toLowerCase(Locale.ENGLISH);
            boolean var3 = false;
            boolean var4 = false;
            boolean var5 = false;

            for (String var9 : var2) {
               if (var9.equalsIgnoreCase("--override")) {
                  var3 = true;
               } else if (var9.equalsIgnoreCase("--database")) {
                  var4 = true;
               } else if (var9.equalsIgnoreCase("--confirm")) {
                  var5 = true;
               }
            }

            if ((var3 || var4) && !var5) {
               SendMessage.warnMessage(
                  "This command will override any existing shop data. We suggest creating backups of your current shops.yml, sections.yml and/or database before using this command."
               );
               SendMessage.warnMessage(var1, "We will not be responsible for any data loss of your current shop settings if no backups are made.");
               SendMessage.infoMessage(
                  var1,
                  ChatColor.GREEN
                     + "Use this command with the "
                     + ChatColor.WHITE
                     + "--confirm "
                     + ChatColor.GREEN
                     + "flag to continue migration using this command."
               );
            } else {
               boolean var10 = var3;
               boolean var11 = var4;
               this.plugin.runTaskAsync(() -> this.migrateData(var1, var10, var11));
            }
         }
      }
   }

   private Map<String, List<String>> getSupportedFiles() {
      HashMap var1 = new HashMap();
      var1.put("economyshopgui", Arrays.asList("V1toV2"));
      return var1;
   }

   private void migrateData(Object var1, boolean var2, boolean var3) {
      if (this.pluginName.equalsIgnoreCase("economyshopgui")) {
         int var4 = 0;
         boolean var5 = false;
         HashSet var6 = new HashSet();
         File var7 = new File(this.plugin.getDataFolder(), "shops.yml");
         if (var7.exists()) {
            SendMessage.logDebugMessage("Started migrating data from shops.yml....");
            YamlConfiguration var8 = this.plugin.loadConfiguration(var7, "shops.yml");
            if (var2) {
               File var9 = new File(this.plugin.getDataFolder() + File.separator + "shops");
               if (var9.exists() && var9.isDirectory()) {
                  for (File var13 : var9.listFiles()) {
                     try {
                        Files.delete(var13.toPath());
                        SendMessage.infoMessage("Deleted " + var9.getName() + " shops config while migrating using the --override flag.");
                     } catch (IOException var23) {
                        SendMessage.warnMessage(
                           "A unknown exception occurred while removing " + var9.getName() + " shops config while migrating using the --override flag."
                        );
                        var23.printStackTrace();
                     }
                  }
               }
            }

            for (String var30 : var8.getKeys(false)) {
               if (ConfigManager.getShop(var30) != null) {
                  if (!var2) {
                     var6.add(var30);
                     SendMessage.logDebugMessage("Skipping section " + var30 + " because it already exists...");
                     continue;
                  }

                  SendMessage.warnMessage(var1, "Overriding " + var30 + " from shops.yml");
               }

               YamlConfiguration var33 = new YamlConfiguration();
               int var36 = 1;
               LinkedHashMap var39 = new LinkedHashMap();
               HashMap<Integer, Map<String, Object>> var14 = new HashMap<>();
               boolean var15 = false;

               for (String var17 : var8.getConfigurationSection(var30).getKeys(false)) {
                  int var18 = var8.getInt(var30 + "." + var17 + ".slot");
                  if (var18 != 0) {
                     var15 = true;
                     int var19 = (int)Math.ceil(var18 / 45.0);
                     if (var14.containsKey(var19)) {
                        ((Map)var14.get(var19)).put(var17, var8.get(var30 + "." + var17));
                     } else {
                        for (int var20 = 1; var20 < var19; var20++) {
                           if (!var14.containsKey(var20)) {
                              var14.put(var20, new HashMap());
                           }
                        }

                        LinkedHashMap var49 = new LinkedHashMap();
                        var49.put(var17, var8.get(var30 + "." + var17));
                        var14.put(var19, var49);
                     }
                  }

                  if (var39.size() == 45) {
                     var14.put(var36, var39);
                     var39 = new LinkedHashMap();
                     var39.put(var17, var8.get(var30 + "." + var17));
                     var36++;
                  } else {
                     var39.put(var17, var8.get(var30 + "." + var17));
                  }
               }

               if (!var39.isEmpty()) {
                  if (var14.containsKey(var36)) {
                     ((Map)var14.get(var36)).putAll(var39);
                  } else {
                     var14.put(var36, var39);
                  }
               }

               if (var14.isEmpty()) {
                  SendMessage.warnMessage("Skipping " + var30 + " from shops.yml because no data to migrate could be found.");
               } else {
                  var5 = true;
                  SendMessage.logDebugMessage("Migrating section " + var30 + " with " + var14.values().stream().mapToInt(var0 -> var0.size()).sum() + " items.");

                  for (int var46 = 0; var46 < var14.size(); var46++) {
                     String var48 = "page" + (var46 + 1);
                     var33.set("pages." + var48 + ".gui-rows", var15 ? 6 : CalculateAmount.getSlots(((Map)var14.get(var46 + 1)).size()) / 9);
                     var33.set("pages." + var48 + ".items", var14.get(var46 + 1));
                     SendMessage.logDebugMessage("Created new page for section " + var30 + " with " + ((Map)var14.get(var46 + 1)).size() + " items.");
                  }

                  File var47 = new File(this.plugin.getDataFolder() + File.separator + "shops" + File.separator, var30 + ".yml");

                  try {
                     var33.save(var47);
                     var4++;
                     SendMessage.infoMessage(
                        "Migrated "
                           + var30
                           + " with "
                           + var14.size()
                           + " pages and a total of "
                           + var14.values().stream().mapToInt(var0 -> var0.size()).sum()
                           + " items and saved to "
                           + var47.getAbsolutePath()
                     );
                  } catch (IOException var25) {
                     if (var1 instanceof Player) {
                        SendMessage.warnMessage("A unknown error occurred while migrating shop '" + var30 + "', look in your console for more details.");
                     }

                     SendMessage.warnMessage("A unknown error occurred while saving new shop file " + var47.getName());
                     var25.printStackTrace();
                  }
               }
            }
         }

         File var26 = new File(this.plugin.getDataFolder(), "sections.yml");
         if (var26.exists()) {
            SendMessage.logDebugMessage("Started migrating data from sections.yml....");
            YamlConfiguration var28 = this.plugin.loadConfiguration(var26, "sections.yml");
            if (var2) {
               File var31 = new File(this.plugin.getDataFolder() + File.separator + "sections");
               if (var31.exists() && var31.isDirectory()) {
                  for (File var42 : var31.listFiles()) {
                     try {
                        Files.delete(var42.toPath());
                        SendMessage.infoMessage("Deleted " + var31.getName() + " sections config while migrating using the --override flag.");
                     } catch (IOException var22) {
                        SendMessage.warnMessage(
                           "A unknown exception occurred while removing " + var31.getName() + " sections config while migrating using the --override flag."
                        );
                        var22.printStackTrace();
                     }
                  }
               }
            }

            if (!var28.contains("ShopSections")) {
               SendMessage.warnMessage("No sections found in sections.yml");
            } else {
               for (String var35 : var28.getConfigurationSection("ShopSections").getKeys(false)) {
                  if (ConfigManager.getSection(var35) != null) {
                     if (!var2) {
                        var6.add(var35);
                        SendMessage.logDebugMessage("Skipping section " + var35 + " because it already exists...");
                        continue;
                     }

                     SendMessage.warnMessage(var1, "Overriding " + var35 + " from sections.yml");
                  }

                  YamlConfiguration var38 = new YamlConfiguration();
                  SendMessage.logDebugMessage("Migrating section " + var35 + "...");
                  Map<String, Object> var41 = this.getSectionData(var28, var35);
                  if (var41.isEmpty()) {
                     SendMessage.warnMessage("Skipping " + var35 + " from sections.yml because no data to migrate could be found.");
                  } else {
                     var5 = true;

                     for (String var45 : var41.keySet()) {
                        var38.set(var45, var41.get(var45));
                     }

                     File var44 = new File(this.plugin.getDataFolder() + File.separator + "sections" + File.separator, var35 + ".yml");

                     try {
                        var38.save(var44);
                        var4++;
                        SendMessage.infoMessage("Migrated " + var35 + " and saved to " + var44.getAbsolutePath());
                     } catch (IOException var24) {
                        if (var1 instanceof Player) {
                           SendMessage.warnMessage("A unknown error occurred while migrating shop '" + var35 + "', look in your console for more details.");
                        }

                        SendMessage.warnMessage("A unknown error occurred while saving new shop file " + var44.getName());
                        var24.printStackTrace();
                     }
                  }
               }
            }
         }

         if (!var2 && !var6.isEmpty()) {
            SendMessage.warnMessage(
               var1,
               "Failed to migrate data for shops "
                  + Arrays.toString(var6.toArray())
                  + " because they already exist. If you wish to override them, you can use this command with the "
                  + ChatColor.WHITE
                  + "--override "
                  + ChatColor.RED
                  + "flag."
            );
         }

         if (!var5) {
            SendMessage.warnMessage(var1, "No changes found to migrate!");
            return;
         }

         if (!var3) {
            SendMessage.infoMessage(
               var1,
               "Completed migrating data for "
                  + var4
                  + " shops and sections which can now be found in "
                  + ChatColor.WHITE
                  + "/sections/<shopName>.yml"
                  + ChatColor.GREEN
                  + " and "
                  + ChatColor.WHITE
                  + "/shops/<shopName.yml>"
            );
            SendMessage.infoMessage(
               var1,
               "Use command "
                  + ChatColor.WHITE
                  + "/sreload"
                  + ChatColor.GREEN
                  + " to load the migrated shops, followed by this command with the "
                  + ChatColor.WHITE
                  + "--database "
                  + ChatColor.GREEN
                  + "flag to migrate item stocks/item sell limits."
            );
            SendMessage.warnMessage(var1, "Note that this will reset DynamicPricing data.");
            return;
         }

         if (this.plugin.getProviderManager().getDatabase() != null) {
            com.onyxshop.database.Database var29 = this.plugin.getProviderManager().getDatabase();
            SendMessage.infoMessage(var1, "Starting migration of database... This can take some time, please let it complete or your database may go corrupt.");
            SendMessage.infoMessage(var1, "See your console for any additional messages.");

            try {
               if (var29 instanceof MySQL) {
                  ((MySQL)var29).migrateV4toV5();
               } else {
                  ConfigUtil.createBackup(Paths.get(this.plugin.getDataFolder() + File.separator + "database.db"));
                  ((SQLite)var29).migrateV4toV5();
               }

               SendMessage.infoMessage(var1, "Successfully migrated data from database.");
            } catch (Exception var21) {
               SendMessage.errorMessage(var1, "A unknown error occurred while migrating the database, please look in your console for any errors.");
               SendMessage.errorMessage("Error while migrating database:");
               var21.printStackTrace();
            }
         }

         SendMessage.infoMessage(var1, "Restart your server to reload all changes.");
      }
   }

   private Map<String, Object> getSectionData(YamlConfiguration var1, String var2) {
      Set<String> var3 = var1.getConfigurationSection("ShopSections." + var2).getKeys(false);
      LinkedHashMap<String, Object> var4 = new LinkedHashMap<>();
      var3.remove("enable");
      var4.put("enable", var1.getBoolean("ShopSections." + var2 + ".enable", true));
      var3.remove("slot");
      var4.put("slot", var1.getInt("ShopSections." + var2 + ".slot"));
      var3.remove("title");
      var4.put("title", var1.getString("ShopSections." + var2 + ".title", ""));
      var3.remove("hidden");
      var4.put("hidden", var1.getBoolean("ShopSections." + var2 + ".hidden", false));
      var3.remove("subSection");
      var4.put("sub-section", var1.getBoolean("ShopSections." + var2 + ".subSection", false));
      var3.remove("fill-item");
      var4.put("display-item", var1.getBoolean("ShopSections." + var2 + ".fill-item", false));
      if (var3.remove("economy")) {
         var4.put("economy", var1.getString("ShopSections." + var2 + ".economy"));
      }

      if (var3.remove("click-commands")) {
         var4.put("click-commands", var1.getStringList("ShopSections." + var2 + ".click-commands"));
      }

      if (!var1.getBoolean("ShopSections." + var2 + ".dynamic-pricing", true)) {
         var3.remove("dynamic-pricing");
         var4.put("dynamic-pricing.enabled", false);
      }

      if (!var3.contains("fillItem")) {
         var4.put("fill-item.material", "AIR");
      } else {
         for (String var6 : var1.getConfigurationSection("ShopSections." + var2 + ".fillItem").getKeys(false)) {
            var4.put("fill-item." + var6, var1.get("ShopSections." + var2 + ".fillItem." + var6));
         }
      }

      var4.put("nav-bar.mode", "INHERIT");
      HashMap var8 = new HashMap();

      for (String var7 : var3) {
         if (var7.equalsIgnoreCase("displayname")) {
            var8.put("name", var1.getString("ShopSections." + var2 + ".displayname"));
         }

         var8.put(var7, var1.get("ShopSections." + var2 + "." + var7));
      }

      var4.put("item", var8);
      return var4;
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.supported.keySet(), var3);
               Collections.sort(var3);
               return var3;
            }

            return new ArrayList<>(this.supported.keySet());
         case 3:
            if (this.supported.containsKey(var1[1].toLowerCase(Locale.ENGLISH))) {
               if (!var1[2].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[2], this.supported.get(var1[1].toLowerCase(Locale.ENGLISH)), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return this.supported.get(var1[1].toLowerCase(Locale.ENGLISH));
            }

            return null;
         default:
            return null;
      }
   }
}
