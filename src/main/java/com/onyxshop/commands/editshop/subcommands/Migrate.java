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

   public Migrate(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length <= 1) {
         SendMessage.sendMessage(value, this.getSyntax());
      } else if (!this.supported.containsKey(values[1].toLowerCase(Locale.ENGLISH))) {
         SendMessage.sendMessage(value, Lang.PLUGIN_NOT_FOUND.get());
      } else {
         this.pluginName = values[1].toLowerCase(Locale.ENGLISH);
         if (values.length <= 2) {
            SendMessage.sendMessage(value, this.getSyntax());
         } else {
            this.image = values[2].toLowerCase(Locale.ENGLISH);
            boolean flag = false;
            boolean flag2 = false;
            boolean flag3 = false;

            for (String text : values) {
               if (text.equalsIgnoreCase("--override")) {
                  flag = true;
               } else if (text.equalsIgnoreCase("--database")) {
                  flag2 = true;
               } else if (text.equalsIgnoreCase("--confirm")) {
                  flag3 = true;
               }
            }

            if ((flag || flag2) && !flag3) {
               SendMessage.warnMessage(
                  "This command will override any existing shop data. We suggest creating backups of your current shops.yml, sections.yml and/or database before using this command."
               );
               SendMessage.warnMessage(value, "We will not be responsible for any data loss of your current shop settings if no backups are made.");
               SendMessage.infoMessage(
                  value,
                  ChatColor.GREEN
                     + "Use this command with the "
                     + ChatColor.WHITE
                     + "--confirm "
                     + ChatColor.GREEN
                     + "flag to continue migration using this command."
               );
            } else {
               boolean flag4 = flag;
               boolean flag5 = flag2;
               this.plugin.runTaskAsync(() -> this.migrateData(value, flag4, flag5));
            }
         }
      }
   }

   private Map<String, List<String>> getSupportedFiles() {
      HashMap map = new HashMap();
      map.put("economyshopgui", Arrays.asList("V1toV2"));
      return map;
   }

   private void migrateData(Object value, boolean flag, boolean flag2) {
      if (this.pluginName.equalsIgnoreCase("economyshopgui")) {
         int value2 = 0;
         boolean flag3 = false;
         HashSet values = new HashSet();
         File file = new File(this.plugin.getDataFolder(), "shops.yml");
         if (file.exists()) {
            SendMessage.logDebugMessage("Started migrating data from shops.yml....");
            YamlConfiguration config = this.plugin.loadConfiguration(file, "shops.yml");
            if (flag) {
               File file2 = new File(this.plugin.getDataFolder() + File.separator + "shops");
               if (file2.exists() && file2.isDirectory()) {
                  for (File file3 : file2.listFiles()) {
                     try {
                        Files.delete(file3.toPath());
                        SendMessage.infoMessage("Deleted " + file2.getName() + " shops config while migrating using the --override flag.");
                     } catch (IOException exception) {
                        SendMessage.warnMessage(
                           "A unknown exception occurred while removing " + file2.getName() + " shops config while migrating using the --override flag."
                        );
                        exception.printStackTrace();
                     }
                  }
               }
            }

            for (String text : config.getKeys(false)) {
               if (ConfigManager.getShop(text) != null) {
                  if (!flag) {
                     values.add(text);
                     SendMessage.logDebugMessage("Skipping section " + text + " because it already exists...");
                     continue;
                  }

                  SendMessage.warnMessage(value, "Overriding " + text + " from shops.yml");
               }

               YamlConfiguration config2 = new YamlConfiguration();
               int value3 = 1;
               LinkedHashMap map = new LinkedHashMap();
               HashMap<Integer, Map<String, Object>> map2 = new HashMap<>();
               boolean flag4 = false;

               for (String sectionId : config.getConfigurationSection(text).getKeys(false)) {
                  int slot = config.getInt(text + "." + sectionId + ".slot");
                  if (slot != 0) {
                     flag4 = true;
                     int value4 = (int)Math.ceil(slot / 45.0);
                     if (map2.containsKey(value4)) {
                        ((Map)map2.get(value4)).put(sectionId, config.get(text + "." + sectionId));
                     } else {
                        for (int index = 1; index < value4; index++) {
                           if (!map2.containsKey(index)) {
                              map2.put(index, new HashMap());
                           }
                        }

                        LinkedHashMap map3 = new LinkedHashMap();
                        map3.put(sectionId, config.get(text + "." + sectionId));
                        map2.put(value4, map3);
                     }
                  }

                  if (map.size() == 45) {
                     map2.put(value3, map);
                     map = new LinkedHashMap();
                     map.put(sectionId, config.get(text + "." + sectionId));
                     value3++;
                  } else {
                     map.put(sectionId, config.get(text + "." + sectionId));
                  }
               }

               if (!map.isEmpty()) {
                  if (map2.containsKey(value3)) {
                     ((Map)map2.get(value3)).putAll(map);
                  } else {
                     map2.put(value3, map);
                  }
               }

               if (map2.isEmpty()) {
                  SendMessage.warnMessage("Skipping " + text + " from shops.yml because no data to migrate could be found.");
               } else {
                  flag3 = true;
                  SendMessage.logDebugMessage("Migrating section " + text + " with " + map2.values().stream().mapToInt(value4 -> value4.size()).sum() + " items.");

                  for (int size = 0; size < map2.size(); size++) {
                     String text2 = "page" + (size + 1);
                     config2.set("pages." + text2 + ".gui-rows", flag4 ? 6 : CalculateAmount.getSlots(((Map)map2.get(size + 1)).size()) / 9);
                     config2.set("pages." + text2 + ".items", map2.get(size + 1));
                     SendMessage.logDebugMessage("Created new page for section " + text + " with " + ((Map)map2.get(size + 1)).size() + " items.");
                  }

                  File file2 = new File(this.plugin.getDataFolder() + File.separator + "shops" + File.separator, text + ".yml");

                  try {
                     config2.save(file2);
                     value2++;
                     SendMessage.infoMessage(
                        "Migrated "
                           + text
                           + " with "
                           + map2.size()
                           + " pages and a total of "
                           + map2.values().stream().mapToInt(value4 -> value4.size()).sum()
                           + " items and saved to "
                           + file2.getAbsolutePath()
                     );
                  } catch (IOException exception) {
                     if (value instanceof Player) {
                        SendMessage.warnMessage("A unknown error occurred while migrating shop '" + text + "', look in your console for more details.");
                     }

                     SendMessage.warnMessage("A unknown error occurred while saving new shop file " + file2.getName());
                     exception.printStackTrace();
                  }
               }
            }
         }

         File file2 = new File(this.plugin.getDataFolder(), "sections.yml");
         if (file2.exists()) {
            SendMessage.logDebugMessage("Started migrating data from sections.yml....");
            YamlConfiguration config = this.plugin.loadConfiguration(file2, "sections.yml");
            if (flag) {
               File file3 = new File(this.plugin.getDataFolder() + File.separator + "sections");
               if (file3.exists() && file3.isDirectory()) {
                  for (File file4 : file3.listFiles()) {
                     try {
                        Files.delete(file4.toPath());
                        SendMessage.infoMessage("Deleted " + file3.getName() + " sections config while migrating using the --override flag.");
                     } catch (IOException exception) {
                        SendMessage.warnMessage(
                           "A unknown exception occurred while removing " + file3.getName() + " sections config while migrating using the --override flag."
                        );
                        exception.printStackTrace();
                     }
                  }
               }
            }

            if (!config.contains("ShopSections")) {
               SendMessage.warnMessage("No sections found in sections.yml");
            } else {
               for (String shopId : config.getConfigurationSection("ShopSections").getKeys(false)) {
                  if (ConfigManager.getSection(shopId) != null) {
                     if (!flag) {
                        values.add(shopId);
                        SendMessage.logDebugMessage("Skipping section " + shopId + " because it already exists...");
                        continue;
                     }

                     SendMessage.warnMessage(value, "Overriding " + shopId + " from sections.yml");
                  }

                  YamlConfiguration config2 = new YamlConfiguration();
                  SendMessage.logDebugMessage("Migrating section " + shopId + "...");
                  Map<String, Object> sectionData = this.getSectionData(config, shopId);
                  if (sectionData.isEmpty()) {
                     SendMessage.warnMessage("Skipping " + shopId + " from sections.yml because no data to migrate could be found.");
                  } else {
                     flag3 = true;

                     for (String key : sectionData.keySet()) {
                        config2.set(key, sectionData.get(key));
                     }

                     File file3 = new File(this.plugin.getDataFolder() + File.separator + "sections" + File.separator, shopId + ".yml");

                     try {
                        config2.save(file3);
                        value2++;
                        SendMessage.infoMessage("Migrated " + shopId + " and saved to " + file3.getAbsolutePath());
                     } catch (IOException exception) {
                        if (value instanceof Player) {
                           SendMessage.warnMessage("A unknown error occurred while migrating shop '" + shopId + "', look in your console for more details.");
                        }

                        SendMessage.warnMessage("A unknown error occurred while saving new shop file " + file3.getName());
                        exception.printStackTrace();
                     }
                  }
               }
            }
         }

         if (!flag && !values.isEmpty()) {
            SendMessage.warnMessage(
               value,
               "Failed to migrate data for shops "
                  + Arrays.toString(values.toArray())
                  + " because they already exist. If you wish to override them, you can use this command with the "
                  + ChatColor.WHITE
                  + "--override "
                  + ChatColor.RED
                  + "flag."
            );
         }

         if (!flag3) {
            SendMessage.warnMessage(value, "No changes found to migrate!");
            return;
         }

         if (!flag2) {
            SendMessage.infoMessage(
               value,
               "Completed migrating data for "
                  + value2
                  + " shops and sections which can now be found in "
                  + ChatColor.WHITE
                  + "/sections/<shopName>.yml"
                  + ChatColor.GREEN
                  + " and "
                  + ChatColor.WHITE
                  + "/shops/<shopName.yml>"
            );
            SendMessage.infoMessage(
               value,
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
            SendMessage.warnMessage(value, "Note that this will reset DynamicPricing data.");
            return;
         }

         if (this.plugin.getProviderManager().getDatabase() != null) {
            com.onyxshop.database.Database database = this.plugin.getProviderManager().getDatabase();
            SendMessage.infoMessage(value, "Starting migration of database... This can take some time, please let it complete or your database may go corrupt.");
            SendMessage.infoMessage(value, "See your console for any additional messages.");

            try {
               if (database instanceof MySQL) {
                  ((MySQL)database).migrateV4toV5();
               } else {
                  ConfigUtil.createBackup(Paths.get(this.plugin.getDataFolder() + File.separator + "database.db"));
                  ((SQLite)database).migrateV4toV5();
               }

               SendMessage.infoMessage(value, "Successfully migrated data from database.");
            } catch (Exception exception) {
               SendMessage.errorMessage(value, "A unknown error occurred while migrating the database, please look in your console for any errors.");
               SendMessage.errorMessage("Error while migrating database:");
               exception.printStackTrace();
            }
         }

         SendMessage.infoMessage(value, "Restart your server to reload all changes.");
      }
   }

   private Map<String, Object> getSectionData(YamlConfiguration config, String sectionId) {
      Set<String> keys = config.getConfigurationSection("ShopSections." + sectionId).getKeys(false);
      LinkedHashMap<String, Object> map = new LinkedHashMap<>();
      keys.remove("enable");
      map.put("enable", config.getBoolean("ShopSections." + sectionId + ".enable", true));
      keys.remove("slot");
      map.put("slot", config.getInt("ShopSections." + sectionId + ".slot"));
      keys.remove("title");
      map.put("title", config.getString("ShopSections." + sectionId + ".title", ""));
      keys.remove("hidden");
      map.put("hidden", config.getBoolean("ShopSections." + sectionId + ".hidden", false));
      keys.remove("subSection");
      map.put("sub-section", config.getBoolean("ShopSections." + sectionId + ".subSection", false));
      keys.remove("fill-item");
      map.put("display-item", config.getBoolean("ShopSections." + sectionId + ".fill-item", false));
      if (keys.remove("economy")) {
         map.put("economy", config.getString("ShopSections." + sectionId + ".economy"));
      }

      if (keys.remove("click-commands")) {
         map.put("click-commands", config.getStringList("ShopSections." + sectionId + ".click-commands"));
      }

      if (!config.getBoolean("ShopSections." + sectionId + ".dynamic-pricing", true)) {
         keys.remove("dynamic-pricing");
         map.put("dynamic-pricing.enabled", false);
      }

      if (!keys.contains("fillItem")) {
         map.put("fill-item.material", "AIR");
      } else {
         for (String shopId : config.getConfigurationSection("ShopSections." + sectionId + ".fillItem").getKeys(false)) {
            map.put("fill-item." + shopId, config.get("ShopSections." + sectionId + ".fillItem." + shopId));
         }
      }

      map.put("nav-bar.mode", "INHERIT");
      HashMap map2 = new HashMap();

      for (String text : keys) {
         if (text.equalsIgnoreCase("displayname")) {
            map2.put("name", config.getString("ShopSections." + sectionId + ".displayname"));
         }

         map2.put(text, config.get("ShopSections." + sectionId + "." + text));
      }

      map.put("item", map2);
      return map;
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 2:
            if (!values[1].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[1], this.supported.keySet(), values2);
               Collections.sort(values2);
               return values2;
            }

            return new ArrayList<>(this.supported.keySet());
         case 3:
            if (this.supported.containsKey(values[1].toLowerCase(Locale.ENGLISH))) {
               if (!values[2].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[2], this.supported.get(values[1].toLowerCase(Locale.ENGLISH)), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.supported.get(values[1].toLowerCase(Locale.ENGLISH));
            }

            return null;
         default:
            return null;
      }
   }
}
