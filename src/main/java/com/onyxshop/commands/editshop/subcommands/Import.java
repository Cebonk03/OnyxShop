package com.onyxshop.commands.editshop.subcommands;

import com.earth2me.essentials.Essentials;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.ConfigUtil;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemImportException;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.exceptions.UnsupportedItemFlagException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.StringUtil;

public class Import extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private Essentials ess;
   private final Map<String, List<String>> supported;
   private Map<String, Object> CACHE;
   private Import.ShopGUIPlusData data;
   String pluginName;
   String fileName;
   File file;

   public Import(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
      this.supported = this.getSupportedFiles();
   }

   @Override
   public String getName() {
      return "import";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_IMPORT_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_IMPORT_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length <= 1) {
         SendMessage.sendMessage(value, this.getSyntax());
      } else if (this.supported.containsKey(values[1].toLowerCase(Locale.ENGLISH))) {
         this.pluginName = values[1].toLowerCase(Locale.ENGLISH);
         if (values.length <= 2) {
            SendMessage.sendMessage(value, this.getSyntax());
         } else {
            this.fileName = values[2].toLowerCase(Locale.ENGLISH);
            this.file = this.getFile(this.pluginName, this.fileName);
            if (this.file != null && this.supported.get(this.pluginName).contains(this.fileName)) {
               if (values.length > 3 && values[3].equalsIgnoreCase("confirm") && this.pluginName.equalsIgnoreCase("shopguiplus")) {
                  this.completeImporting(value);
               } else {
                  this.importChanges(value);
               }
            } else {
               SendMessage.sendMessage(value, Lang.FILE_NOT_FOUND.get().replace("%fileName%", this.fileName).replace("%pluginName%", this.pluginName));
            }
         }
      } else {
         SendMessage.sendMessage(value, Lang.PLUGIN_NOT_FOUND.get());
      }
   }

   private Map<String, List<String>> getSupportedFiles() {
      HashMap map = new HashMap();
      File file = new File(this.plugin.getDataFolder().getParent() + "/OnyxShop/");
      if (file.exists() && file.isDirectory()) {
         map.put("economyshopgui", Arrays.asList("all"));
      }

      file = new File(this.plugin.getDataFolder().getParent() + "/Essentials/");
      if (file.exists() && file.isDirectory()) {
         map.put("essentials", Arrays.asList("worth.yml"));
      }

      file = new File(this.plugin.getDataFolder().getParent() + "/ShopGUIPlus/");
      if (file.exists() && file.isDirectory()) {
         map.put("shopguiplus", Arrays.asList("all"));
      }

      return map;
   }

   private File getFile(String fileName) {
      return this.getFile(this.pluginName, fileName);
   }

   private File getFile(String pluginName, String fileName) {
      String pluginKey = pluginName.toLowerCase(Locale.ENGLISH);
      switch (pluginKey) {
         case "essentials":
            File file = new File(this.plugin.getDataFolder().getParent() + "/Essentials/" + fileName);
            return file.exists() ? file : null;
         case "economyshopgui":
            File file2 = new File(this.plugin.getDataFolder().getParent() + "/OnyxShop/");
            return file2.exists() ? file2 : null;
         case "shopguiplus":
            File file3 = new File(this.plugin.getDataFolder().getParent() + "/ShopGUIPlus/");
            return file3.exists() ? file3 : null;
         default:
            return null;
      }
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

   private XMaterial getMaterial(String materialName) {
      try {
         ItemStack itemStack = this.ess.getItemDb().get(materialName);
         return XMaterial.matchXMaterial(itemStack);
      } catch (Exception exception) {
         return null;
      }
   }

   private List<XMaterial> getMaterials(ConfigurationSection section, String materialName) {
      ArrayList values = new ArrayList();
      int value = 0;

      while (true) {
         Optional optional = XMaterial.matchXMaterial(materialName + ":" + value);
         if (!optional.isPresent() || ((XMaterial)optional.get()).parseItem() == null || section.getDouble("worth." + materialName + ".*", -5.0) == -5.0) {
            return values;
         }

         if (!section.getConfigurationSection("worth." + materialName).getKeys(false).contains(String.valueOf(value))) {
            values.add((XMaterial)optional.get());
         }

         value++;
      }
   }

   private XMaterial getMaterial(ConfigurationSection section, String materialName, String sectionId) {
      Optional optional = XMaterial.matchXMaterial(materialName + ":" + sectionId);
      return optional.isPresent() && ((XMaterial)optional.get()).parseItem() != null && section.getDouble("worth." + materialName + "." + sectionId, -5.0) != -5.0
         ? (XMaterial)optional.get()
         : null;
   }

   private void importChanges(Object value) {
      if (this.pluginName.equalsIgnoreCase("economyshopgui")) {
         try {
            Path path = this.createZipFile();
            SendMessage.infoMessage(
               value, ChatColor.GREEN + "Creating backup of current shop layout, this can be found in " + path.toFile().getAbsolutePath() + "..."
            );
            FileOutputStream fileOutputStream = new FileOutputStream(path + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            this.zipLayout(zipOutputStream);
            zipOutputStream.close();
            fileOutputStream.close();
         } catch (IOException exception) {
            SendMessage.warnMessage(value, "Failed to create backup of current shop layout.");
            exception.printStackTrace();
            return;
         }

         try {
            Files.copy(Paths.get(this.file.getPath(), "config.yml"), ConfigManager.getConfig().getFilePath(), StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException exception) {
            exception.printStackTrace();
         }

         YamlConfiguration config = this.plugin.loadConfiguration(new File(this.plugin.getDataFolder() + "/config.yml/"), this.fileName);
         config.set("config-version", null);
         ConfigUtil.save(config, new File(this.plugin.getDataFolder() + "/config.yml/"));
         File file = new File(this.plugin.getDataFolder() + File.separator + "sections");
         if (file.exists() && file.isDirectory()) {
            for (File file2 : file.listFiles()) {
               try {
                  Files.delete(file2.toPath());
               } catch (IOException exception) {
               }
            }
         }

         File file2 = new File(this.file.getPath(), "sections");
         if (file2.exists() && file2.isDirectory() && file2.listFiles().length != 0) {
            for (File file3 : file2.listFiles()) {
               try {
                  Files.copy(file3.toPath(), Paths.get(this.plugin.getDataFolder() + "/sections/" + file3.getName()), StandardCopyOption.REPLACE_EXISTING);
               } catch (IOException exception) {
                  SendMessage.warnMessage("Error while importing 'sections/" + file3.getName() + "' from OnyxShop, skipping...");
                  exception.printStackTrace();
               }
            }
         }

         File file3 = new File(this.plugin.getDataFolder() + File.separator + "shops");
         if (file3.exists() && file3.isDirectory()) {
            for (File file4 : file3.listFiles()) {
               try {
                  Files.delete(file4.toPath());
               } catch (IOException exception) {
               }
            }
         }

         File file4 = new File(this.file.getPath(), "shops");
         if (file4.exists() && file4.isDirectory() && file4.listFiles().length != 0) {
            for (File file5 : file4.listFiles()) {
               try {
                  Files.copy(file5.toPath(), Paths.get(this.plugin.getDataFolder() + "/shops/" + file5.getName()), StandardCopyOption.REPLACE_EXISTING);
               } catch (IOException exception) {
                  SendMessage.warnMessage("Error while importing 'shops/" + file5.getName() + "' from OnyxShop, skipping...");
                  exception.printStackTrace();
               }
            }
         }

         File file5 = new File(this.file.getPath(), "stands.json");
         if (file5.exists()) {
            try {
               Files.copy(
                  Paths.get(this.file.getPath(), "stands.json"),
                  Paths.get(this.plugin.getDataFolder().getPath(), "stands.json"),
                  StandardCopyOption.REPLACE_EXISTING
               );
            } catch (IOException exception) {
               exception.printStackTrace();
            }
         }

         SendMessage.sendMessage(
            value, ChatColor.GREEN + "Import complete, use command " + ChatColor.WHITE + "/sreload" + ChatColor.GREEN + " to reload the changes."
         );
      } else if (this.pluginName.equalsIgnoreCase("essentials")) {
         this.ess = (Essentials)Bukkit.getServer().getPluginManager().getPlugin("Essentials");
         if (this.ess == null || !this.ess.getItemDb().isReady()) {
            SendMessage.sendMessage(value, ChatColor.RED + "Could not load essentials, please make sure EssentialsX is installed with the latest version.");
            return;
         }

         SendMessage.infoMessage(value, ChatColor.GREEN + "Creating backup of current shop layout...");

         try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.createZipFile() + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            this.zipLayout(zipOutputStream);
            zipOutputStream.close();
            fileOutputStream.close();
         } catch (IOException exception) {
            SendMessage.warnMessage(value, "Failed to create backup of current shop layout.");
            exception.printStackTrace();
            return;
         }

         String fileName = this.fileName;
         switch (fileName) {
            case "worth.yml":
               YamlConfiguration config = this.plugin.loadConfiguration(this.file, this.fileName);
               HashMap map = new HashMap();

               for (String sectionId : config.getConfigurationSection("worth").getKeys(false)) {
                  if (config.getDouble("worth." + sectionId, -5.0) == -5.0) {
                     for (String text : config.getConfigurationSection("worth." + sectionId).getKeys(false)) {
                        if (text.equals("*")) {
                           this.getMaterials(config, sectionId).forEach(entry -> map.put(entry, config.getDouble("worth." + sectionId + ".*")));
                        } else {
                           XMaterial xMaterial = this.getMaterial(config, sectionId, text);
                           if (xMaterial != null) {
                              map.put(xMaterial, config.getDouble("worth." + sectionId + "." + text));
                           }
                        }
                     }
                  } else {
                     XMaterial xMaterial = this.getMaterial(sectionId);
                     if (xMaterial != null) {
                        map.put(xMaterial, config.getDouble("worth." + sectionId));
                     }
                  }
               }

               int value2 = 0;
               HashSet<String> values = new HashSet<>();

               for (ShopSection section : this.plugin.getSections().values()) {
                  if (section.getType() != ShopType.ROTATING) {
                     for (ShopItem shopItem : section.getShopItems()) {
                        if (!shopItem.hasItemError() && !shopItem.isDisplayItem() && map.containsKey(XMaterial.matchXMaterial(shopItem.getItemToGive()))) {
                           ConfigManager.getShop(section.getSection())
                              .set(
                                 "pages." + shopItem.itemLoc + ".buy",
                                 -ConfigManager.getShop(section.getSection()).getDouble("pages." + shopItem.itemLoc + ".buy", 1.0)
                              );
                           ConfigManager.getShop(section.getSection())
                              .set("pages." + shopItem.itemLoc + ".sell", map.get(XMaterial.matchXMaterial(shopItem.getItemToGive())));
                           SendMessage.infoMessage(
                              "Imported item '"
                                 + XMaterial.matchXMaterial(shopItem.getItemToGive()).name()
                                 + "' from worth.yml with a sell price of '"
                                 + map.get(XMaterial.matchXMaterial(shopItem.getItemToGive()))
                                 + "'"
                           );
                           values.add(section.getSection());
                           value2++;
                        }
                     }
                  }
               }

               values.forEach(ConfigManager::saveShop);
               SendMessage.sendMessage(value, ChatColor.GREEN + "Import success, " + value2 + " items where imported. See the console for more details.");
               break;
            default:
               SendMessage.sendMessage(value, Lang.FILE_NOT_FOUND.get());
         }

         SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
      } else if (this.pluginName.equalsIgnoreCase("guishop")) {
         if (this.CACHE.isEmpty()) {
            YamlConfiguration config = this.plugin.loadConfiguration(this.file, this.fileName);
            int value2 = 0;
            int value3 = 0;
            int value4 = 0;
            HashMap map = new HashMap();

            for (String text : config.getKeys(false)) {
               HashMap map2 = new HashMap();

               for (String text2 : config.getConfigurationSection(text + ".pages").getKeys(false)) {
                  for (String text3 : config.getConfigurationSection(text + "." + text2 + ".items").getKeys(false)) {
                     ConfigurationSection section = config.getConfigurationSection(text + "." + text2 + ".items." + text3);

                     try {
                        map2.put("pages." + text2 + ".items." + text3, this.importGUIShopItem(section));
                        value3++;
                     } catch (Exception exception) {
                        value4++;
                        SendMessage.warnMessage("A error occurred while loading item '" + section.getCurrentPath() + "' from GUIShop, skipping...");
                        exception.printStackTrace();
                     }
                  }
               }

               if (!map2.isEmpty()) {
                  SendMessage.infoMessage("Successfully loaded " + map2.size() + " items from shop section " + text);
                  map.put(text, map2);
                  value2++;
               } else {
                  SendMessage.infoMessage("Couldn't find any item inside section " + text + ", skipping...");
               }
            }

            if (value4 == 0) {
               SendMessage.sendMessage(value, ChatColor.GREEN + "Completed, " + value3 + " item(s) where loaded from " + value2 + " sections.");
            } else {
               SendMessage.sendMessage(
                  value,
                  ChatColor.GREEN
                     + "Successfully loaded "
                     + value3
                     + " item(s) from "
                     + value2
                     + " sections.\n"
                     + ChatColor.RED
                     + value4
                     + " item(s) failed to load, see the console for more info."
               );
            }

            SendMessage.sendMessage(
               value,
               ChatColor.GREEN
                  + "You have 60 seconds to complete the import using "
                  + ChatColor.WHITE
                  + "/editshop import guishop"
                  + ChatColor.GREEN
                  + " again which will replace the existing shops.yml."
            );
            this.emptyCache();
         } else {
            SendMessage.infoMessage(value, ChatColor.GREEN + "Creating backup of current shop layout...");

            try {
               FileOutputStream fileOutputStream = new FileOutputStream(this.createZipFile() + ".zip");
               ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
               this.zipLayout(zipOutputStream);
               zipOutputStream.close();
               fileOutputStream.close();
            } catch (IOException exception) {
               SendMessage.warnMessage(value, "Failed to create backup of current shop layout.");
               exception.printStackTrace();
               return;
            }

            File file = new File(this.plugin.getDataFolder() + File.separator + "sections");
            if (file.exists() && file.isDirectory()) {
               for (File file2 : file.listFiles()) {
                  try {
                     Files.delete(file2.toPath());
                  } catch (IOException exception) {
                  }
               }
            }

            File file2 = new File(this.plugin.getDataFolder() + File.separator + "shops");
            if (file2.exists() && file2.isDirectory()) {
               for (File file3 : file2.listFiles()) {
                  try {
                     Files.delete(file3.toPath());
                  } catch (IOException exception) {
                  }
               }
            }

            for (String key : this.CACHE.keySet()) {
               this.plugin.getConfigManager().createSectionConfig(key);
            }
         }

         SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
      } else if (this.pluginName.equalsIgnoreCase("shopguiplus")) {
         long startTime = System.currentTimeMillis();
         Import.ShopGUIPlusData shopGUIPlusData = new Import.ShopGUIPlusData();
         int value2 = 0;
         int value3 = 0;
         int value4 = 0;
         int value5 = 0;
         int value6 = 0;
         int value7 = 0;
         int value8 = 0;
         int value9 = 0;
         SendMessage.sendMessage(
            value,
            "§cPlease note that this command might not import every setting correctly as its supposed to work for ShopGUI+(most items will be imported), so we recommend going over the config.yml of OnyxShop manually after importing has completed."
         );
         SendMessage.sendMessage(value, "§aStarted importing of ShopGUI+, this might take some time depending on how many shops and items there are...");
         File file = new File(this.file.getPath(), "/config.yml");
         HashMap<String, Map<String, Object>> map = new HashMap<>();
         if (file.exists()) {
            YamlConfiguration config = this.plugin.loadConfiguration(file, this.fileName);
            if (config.get("specialElements") != null) {
               for (String text : config.getConfigurationSection("specialElements").getKeys(false)) {
                  try {
                     Map shopIds = this.importShopGUIPlusSpecialItem(config.getConfigurationSection("specialElements." + text));
                     if (!shopIds.isEmpty()) {
                        shopGUIPlusData.addSpecial(text, shopIds);
                        value7++;
                     }
                  } catch (Exception exception) {
                     if (exception instanceof UnsupportedItemFlagException) {
                        value6++;
                     } else {
                        value8++;
                     }

                     SendMessage.warnMessage(exception.getMessage());
                     exception.printStackTrace();
                     SendMessage.warnMessage("Item at location 'specialElements." + text + "'");
                  }
               }
            }

            if (config.get("shopMenuItems") != null) {
               for (String text : config.getConfigurationSection("shopMenuItems").getKeys(false)) {
                  try {
                     Map itemIds = this.importShopGUIPlusSectionItem(shopGUIPlusData, config.getConfigurationSection("shopMenuItems." + text));
                     if (!itemIds.isEmpty()) {
                        if (!itemIds.containsKey("section")) {
                           itemIds.put("display-item", true);
                        }

                        map.put(itemIds.containsKey("section") ? (String)itemIds.remove("section") : text, itemIds);
                        value2++;
                     }
                  } catch (Exception exception) {
                     if (exception instanceof UnsupportedItemFlagException) {
                        value6++;
                     } else {
                        value8++;
                     }

                     SendMessage.warnMessage(exception.getMessage());
                     exception.printStackTrace();
                     SendMessage.warnMessage("Item at location 'shopMenuItems." + text + "'");
                  }
               }
            }

            if (config.get("shopMenuSize") != null) {
               try {
                  shopGUIPlusData.addConfigData("main-menu.gui-rows", (int)Math.floor(Integer.parseInt(config.getString("shopMenuSize")) / 9.0));
                  value7++;
               } catch (NumberFormatException exception) {
                  SendMessage.errorMessage("Failed to import main shop size for '" + config.getString("shopMenuSize") + "', using 54 as default...");
                  shopGUIPlusData.addConfigData("main-menu.gui-rows", 6);
               }
            }

            if (config.get("shopMenuFillItem") != null) {
               try {
                  Map map2 = this.importShopGUIPlusFillItem(config.getConfigurationSection("shopMenuFillItem"));
                  if (!map2.isEmpty()) {
                     shopGUIPlusData.addConfigData("main-menu.fill-item", map2);
                     value7++;
                  }
               } catch (Exception exception) {
                  value8++;
                  SendMessage.warnMessage(exception.getMessage());
                  exception.printStackTrace();
                  SendMessage.warnMessage("Item at location 'shopMenuFillItem'");
               }
            }

            HashMap<Integer, Object> map2 = new HashMap<>();

            for (String text : shopGUIPlusData.getSpecials().keySet()) {
               if (map2.containsKey(9)) {
                  if (map2.containsKey(1)) {
                     if (map2.containsKey(8)) {
                        if (map2.containsKey(2)) {
                           if (map2.containsKey(7)) {
                              if (map2.containsKey(3)) {
                                 if (map2.containsKey(6)) {
                                    if (map2.containsKey(4)) {
                                       if (map2.containsKey(5)) {
                                          SendMessage.errorMessage(
                                             "Cannot store special item inside main menu navigation bar. Reason: Navigation bar already contains 9 items."
                                          );
                                       } else {
                                          map2.put(5, shopGUIPlusData.getSpecial(text));
                                       }
                                    } else {
                                       map2.put(4, shopGUIPlusData.getSpecial(text));
                                    }
                                 } else {
                                    map2.put(6, shopGUIPlusData.getSpecial(text));
                                 }
                              } else {
                                 map2.put(3, shopGUIPlusData.getSpecial(text));
                              }
                           } else {
                              map2.put(7, shopGUIPlusData.getSpecial(text));
                           }
                        } else {
                           map2.put(2, shopGUIPlusData.getSpecial(text));
                        }
                     } else {
                        map2.put(8, shopGUIPlusData.getSpecial(text));
                     }
                  } else {
                     map2.put(1, shopGUIPlusData.getSpecial(text));
                  }
               } else {
                  map2.put(9, shopGUIPlusData.getSpecial(text));
               }
            }

            if (!map2.isEmpty()) {
               for (int index = 1; index < 10; index++) {
                  if (!map2.containsKey(index)) {
                     map2.put(
                        index,
                        shopGUIPlusData.getConfig().containsKey("main-menu.fill-item")
                           ? shopGUIPlusData.getConfig().get("main-menu.fill-item")
                           : Collections.singletonMap("material", "air")
                     );
                  }
               }

               for (Integer value10 : map2.keySet()) {
                  shopGUIPlusData.addConfigData("main-menu-nav-bar.items." + value10, map2.get(value10));
               }
            }

            HashMap<Integer, Object> map3 = new HashMap<>();

            for (String text : shopGUIPlusData.getSpecials().keySet()) {
               if (map3.containsKey(9)) {
                  if (map3.containsKey(1)) {
                     if (map3.containsKey(8)) {
                        if (map3.containsKey(2)) {
                           if (map3.containsKey(7)) {
                              if (map3.containsKey(3)) {
                                 if (map3.containsKey(6)) {
                                    if (map3.containsKey(4)) {
                                       if (map3.containsKey(5)) {
                                          SendMessage.errorMessage(
                                             "Cannot store special item inside transaction navigation bar. Reason: Navigation bar already contains 9 items."
                                          );
                                       } else {
                                          map3.put(5, shopGUIPlusData.getSpecial(text));
                                       }
                                    } else {
                                       map3.put(4, shopGUIPlusData.getSpecial(text));
                                    }
                                 } else {
                                    map3.put(6, shopGUIPlusData.getSpecial(text));
                                 }
                              } else {
                                 map3.put(3, shopGUIPlusData.getSpecial(text));
                              }
                           } else {
                              map3.put(7, shopGUIPlusData.getSpecial(text));
                           }
                        } else {
                           map3.put(2, shopGUIPlusData.getSpecial(text));
                        }
                     } else {
                        map3.put(8, shopGUIPlusData.getSpecial(text));
                     }
                  } else {
                     map3.put(1, shopGUIPlusData.getSpecial(text));
                  }
               } else {
                  map3.put(9, shopGUIPlusData.getSpecial(text));
               }
            }

            if (!map3.isEmpty()) {
               for (int index = 1; index < 10; index++) {
                  if (!map3.containsKey(index)) {
                     map3.put(index, Collections.singletonMap("material", "air"));
                  }
               }

               for (Integer value10 : map3.keySet()) {
                  shopGUIPlusData.addConfigData("transaction-screens-nav-bar.items." + value10, map3.get(value10));
               }
            }

            if (config.get("buttons") != null) {
               HashMap map4 = new HashMap();

               for (String text : config.getConfigurationSection("buttons").getKeys(false)) {
                  if (config.get("buttons." + text + ".slot") != null) {
                     try {
                        Map map5 = this.importShopGUIPlusSpecialItem(config.getConfigurationSection("buttons." + text));
                        if (!map5.isEmpty()) {
                           map5.put(
                              "action",
                              text.equalsIgnoreCase("goBack")
                                 ? "BACK"
                                 : (text.equalsIgnoreCase("previousPage") ? "PAGE_BACK" : (text.equalsIgnoreCase("nextPage") ? "PAGE_NEXT" : "NONE"))
                           );
                           map4.put((int)Math.ceil(config.getInt("buttons." + text + ".slot") % 9) + 1, map5);
                           value7++;
                        }
                     } catch (Exception exception) {
                        value8++;
                        SendMessage.warnMessage(exception.getMessage());
                        exception.printStackTrace();
                        SendMessage.warnMessage("Item at location 'buttons." + text + "'");
                     }
                  }
               }

               for (String text : shopGUIPlusData.getSpecials().keySet()) {
                  if (map4.containsKey(9)) {
                     if (map4.containsKey(1)) {
                        if (map4.containsKey(8)) {
                           if (map4.containsKey(2)) {
                              if (map4.containsKey(7)) {
                                 if (map4.containsKey(3)) {
                                    if (map4.containsKey(6)) {
                                       if (map4.containsKey(4)) {
                                          if (map4.containsKey(5)) {
                                             SendMessage.errorMessage(
                                                "Cannot store special item inside shops navigation bar. Reason: Navigation bar already contains 9 items."
                                             );
                                          } else {
                                             map4.put(5, shopGUIPlusData.getSpecial(text));
                                          }
                                       } else {
                                          map4.put(4, shopGUIPlusData.getSpecial(text));
                                       }
                                    } else {
                                       map4.put(6, shopGUIPlusData.getSpecial(text));
                                    }
                                 } else {
                                    map4.put(3, shopGUIPlusData.getSpecial(text));
                                 }
                              } else {
                                 map4.put(7, shopGUIPlusData.getSpecial(text));
                              }
                           } else {
                              map4.put(2, shopGUIPlusData.getSpecial(text));
                           }
                        } else {
                           map4.put(8, shopGUIPlusData.getSpecial(text));
                        }
                     } else {
                        map4.put(1, shopGUIPlusData.getSpecial(text));
                     }
                  } else {
                     map4.put(9, shopGUIPlusData.getSpecial(text));
                  }
               }

               if (!map4.isEmpty()) {
                  for (int index = 1; index < 10; index++) {
                     if (!map4.containsKey(index)) {
                        map4.put(
                           index,
                           shopGUIPlusData.getConfig().containsKey("main-menu.fill-item")
                              ? shopGUIPlusData.getConfig().get("main-menu.fill-item")
                              : Collections.singletonMap("material", "air")
                        );
                     }
                  }

                  shopGUIPlusData.addConfigData("shops-nav-bar.items", map4);
               }
            }

            if (config.get("amountSelectionGUI") != null) {
               Import.TransactionMenuData transactionMenuData = this.importShopGUIPlusTransScreen(config.getConfigurationSection("amountSelectionGUI"));
               shopGUIPlusData.addConfigData("sell-screen", transactionMenuData.data);
               shopGUIPlusData.addConfigData("buy-screen", transactionMenuData.data);
               value7 += transactionMenuData.itemsSuccess;
               value8 += transactionMenuData.itemsFailed;
            }

            if (config.get("amountSelectionGUIBulkBuy") != null) {
               Import.TransactionMenuData transactionMenuData = this.importShopGUIPlusTransScreen(config.getConfigurationSection("amountSelectionGUIBulkBuy"));
               shopGUIPlusData.addConfigData("buy-stacks-screen", transactionMenuData.data);
               value7 += transactionMenuData.itemsSuccess;
               value8 += transactionMenuData.itemsFailed;
            }
         }

         SendMessage.sendMessage(value, "§aCompleted loading main menu items, navigation bar items and transaction menu items, loading shops and items...");

         label674:
         for (File file2 : new File(this.file.getPath(), "/shops/").listFiles()) {
            try {
               SendMessage.infoMessage("Started importing of shop '" + file2.getName() + "'");
               YamlConfiguration config = this.plugin.loadConfiguration(file2, file2.getName());
               Iterator iterator = config.getKeys(false).iterator();

               HashMap<String, Object> map2;
               boolean flag;
               Map map3;
               String text;
               while (true) {
                  if (!iterator.hasNext()) {
                     continue label674;
                  }

                  text = (String)iterator.next();
                  map2 = new HashMap<>();
                  if (!map.containsKey(text)) {
                     throw new NullPointerException("DISABLED");
                  }

                  flag = false;
                  map3 = (Map)map.get(text);
                  if (config.get(text + ".fillItem") == null) {
                     break;
                  }

                  try {
                     Map map4 = this.importShopGUIPlusFillItem(config.getConfigurationSection(text + ".fillItem"));
                     if (!map4.isEmpty()) {
                        map3.put("fill-item", map4);
                        flag = true;
                        value7++;
                        break;
                     }
                  } catch (Exception exception) {
                     value8++;
                     SendMessage.warnMessage(exception.getMessage());
                     exception.printStackTrace();
                     SendMessage.warnMessage("Item from shop file: '" + file2.getName() + "' at location '" + text + ".fillItem'");
                     break;
                  }
               }

               if (config.getString(text + ".name") != null) {
                  map3.put("title", config.getString(text + ".name"));
                  flag = true;
               }

               if (config.getString(text + ".economy") != null) {
                  String text2 = config.getString(text + ".economy");
                  if (!text2.equalsIgnoreCase("MYSQL_TOKENS") && !text2.equalsIgnoreCase("TOKEN_MANAGER") && !text2.equalsIgnoreCase("CUSTOM")) {
                     map3.put("economy", text2.toUpperCase(Locale.ENGLISH));
                  } else {
                     map3.put("enable", false);
                     value9++;
                  }

                  flag = true;
               }

               if (map3.containsKey("fill-item")) {
                  Map map4 = (Map)shopGUIPlusData.getConfig().get("shops-nav-bar.items");
                  HashMap map5 = new HashMap();
                  HashMap map6 = new HashMap();
                  String text2 = shopGUIPlusData.getConfig().containsKey("main-menu.fill-item")
                     ? (String)((Map)shopGUIPlusData.getConfig().get("main-menu.fill-item")).get("material")
                     : "air";

                  for (int index = 1; index < 10; index++) {
                     if (!map2.containsKey(index)
                        && map4.containsKey(index)
                        && !((String)((Map)map4.get(index)).get("material")).equalsIgnoreCase(text2)
                        && !((String)((Map)map4.get(index)).get("material")).equalsIgnoreCase("air")) {
                        map6.put(index, map4.get(index));
                     }
                  }

                  if (!map6.isEmpty()) {
                     map5.put("mode", "SELF");
                     map5.put("fill-item", map3.get("fill-item"));
                     map5.put("items", map6);
                     map3.put("nav-bar", map5);
                  }
               }

               if (flag) {
                  map.put(text, map3);
               }

               for (String text2 : config.getConfigurationSection(text + ".items").getKeys(false)) {
                  try {
                     Map map4 = this.importShopGUIPlusItem(shopGUIPlusData, config.getConfigurationSection(text + ".items." + text2));
                     if (!map4.isEmpty()) {
                        if (config.getBoolean(text + ".enablePerItemPermissions")) {
                           map4.put("permission", text2);
                        }

                        map2.put(text2, map4);
                        value4++;
                     }
                  } catch (Exception exception) {
                     if (exception instanceof UnsupportedItemFlagException) {
                        value6++;
                     } else {
                        value5++;
                     }

                     SendMessage.warnMessage(exception.getMessage());
                     exception.printStackTrace();
                     SendMessage.warnMessage("Item from shop file: '" + file2.getName() + "' at location '" + text + ".items." + text2 + "'");
                  }
               }

               if (map2.isEmpty()) {
                  SendMessage.infoMessage("§cCouldn't find any item inside section " + text + ", skipping...");
               } else {
                  LinkedHashMap map4 = new LinkedHashMap();
                  int value10 = 6;
                  if (config.contains(text + ".size")) {
                     value10 = (int)Math.ceil(config.getInt(text + ".size", 54) / 9.0);
                     if (value10 > 6) {
                        value10 = 6;
                     }
                  }

                  int value11 = 0;

                  for (String text2 : map2.keySet()) {
                     Map map5 = (Map)map2.get(text2);
                     if (map5.containsKey("page") && value11 < (Integer)map5.get("page")) {
                        value11 = (Integer)map5.get("page");
                     }
                  }

                  int value12 = (int)Math.ceil(map2.size() / (value10 * 9 - 9.0));
                  if (value12 < value11) {
                     value12 = value11;
                  }

                  for (int index = 1; index < value12 + 1; index++) {
                     map4.put("pages.page" + index + ".gui-rows", value10);

                     for (String text2 : map2.keySet()) {
                        Map map5 = (Map)map2.get(text2);
                        if ((!map5.containsKey("page") || (Integer)map5.get("page") == index) && (map5.containsKey("page") || index == 1)) {
                           map4.put("pages.page" + index + ".items." + text2, map2.get(text2));
                        }
                     }
                  }

                  shopGUIPlusData.addShopsData(text, map4);
                  value3++;
                  SendMessage.infoMessage("§aSuccessfully loaded " + map2.size() + " items across " + value12 + " pages from shop shop " + text);
               }
            } catch (NullPointerException exception) {
               if (map.containsKey(file2.getName())) {
                  ((Map)map.get(file2.getName())).put("display-item", true);
               }
            }
         }

         for (String text : map.keySet()) {
            shopGUIPlusData.addSectionsData(text, (Map<String, Object>)map.get(text));
         }

         SendMessage.sendMessage(
            value,
            "§aCompleted loading data from ShopGUI+ in §f"
               + (System.currentTimeMillis() - startTime)
               + "§ams, summary:\n§f <---------- §aSuccess§f  ---------->\n§f- §aConfig data imported successfully(Navigation bar items, transaction items, fill items, special items): §f"
               + value7
               + "\n§f- §aSection items imported successfully: §f"
               + value2
               + "\n§f- §aSections imported successfully: §f"
               + value3
               + "\n§f- §aShop items imported successfully: §f"
               + value4
               + "\n§f <---------- §cFailed§f  ---------->\n§f- §cConfig items failed to import: §f"
               + value8
               + "\n§f- §cShop items failed to import: §f"
               + value5
               + "\n§f <---------- §6Unsupported§f  ---------->\n§f- §6Section with unsupported economy type(These sections will be imported but will be disabled): §f"
               + value9
               + "\n§f- §6Shop items which aren't supported(You can add these items manually by '/onyx addhanditem' or using NBTData): §f"
               + value6
         );
         SendMessage.sendMessage(value, "§aComplete the import using §f/onyx import shopguiplus all confirm §aonce you are satisfied with the results.");
         this.data = shopGUIPlusData;
      } else {
         SendMessage.sendMessage(value, Lang.PLUGIN_NOT_FOUND.get());
      }
   }

   private Import.TransactionMenuData importShopGUIPlusTransScreen(ConfigurationSection section) {
      HashMap shopIds = new HashMap();
      int value = 0;
      int value2 = 0;
      int value3 = 54;
      if (section.get("size") != null) {
         try {
            shopIds.put("menu-size", value3 = (int)Math.floor(Integer.parseInt(section.getString("size")) / 9.0));
         } catch (NumberFormatException exception) {
            SendMessage.errorMessage("Failed to import screen size for '" + section.getCurrentPath() + "', using 54 as default...");
            shopIds.put("menu-size", value3 / 9);
         }
      }

      if (section.get("itemSlot") != null) {
         HashMap map = new HashMap();
         map.put("type", "SELECTED_ITEM");
         map.put("slot", section.getInt("itemSlot"));
         shopIds.put("items.selected-item", map);
         value++;
      }

      for (String sectionId : section.getConfigurationSection("buttons").getKeys(false)) {
         try {
            ConfigurationSection section2 = section.getConfigurationSection("buttons." + sectionId);
            HashMap map = new HashMap();
            String text = "";
            if (section2.get("item.material") != null) {
               text = section2.getString("item.material");
            }

            if (section2.get("item.damage") != null) {
               text = text + ":" + section2.get("item.damage");
            }

            if (text.isEmpty()) {
               throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
            }

            map.put("material", text);
            if (section2.get("item.name") != null) {
               map.put("name", section2.getString("item.name"));
            }

            if (!section2.getStringList("item.lore").isEmpty()) {
               map.put(
                  "lore",
                  section2.getStringList("item.lore")
                     .stream()
                     .map(element -> element.replaceAll("%buy%", "%formattedPrice%").replaceAll("%sell%", "%formattedPrice%"))
                     .collect(Collectors.toList())
               );
            }

            if (section.getCurrentPath().equalsIgnoreCase("amountSelectionGUIBulkBuy") && section2.get("value") != null) {
               map.put("stack-size", section2.getInt("value"));
            }

            if (!map.containsKey("stack-size") && section2.get("item.quantity") != null) {
               map.put("stack-size", section2.getInt("item.quantity"));
            }

            if (section2.get("slot") != null) {
               int value4;
               try {
                  value4 = Integer.parseInt(section2.getString("slot"));
                  if (value4 > value3 * 9 - 9) {
                     value4 -= 9;
                  }
               } catch (NumberFormatException exception) {
                  value4 = shopIds.size();
               }

               map.put("slot", value4);
            }

            HashMap map2 = new HashMap();
            if (!section2.getStringList("flags").isEmpty()) {
               map2.put("HideFlags", this.flagsToByte(section2.getStringList("flags"), section2.getCurrentPath()));
            }

            if (!map2.isEmpty()) {
               map.put("NBTData", new Gson().toJson(map2).toString());
            }

            if (section.getCurrentPath().equalsIgnoreCase("amountSelectionGUI")) {
               String text2 = "NONE";
               if (sectionId.equalsIgnoreCase("set1")) {
                  text2 = "REMOVE32";
               } else if (sectionId.equalsIgnoreCase("remove10")) {
                  text2 = "REMOVE16";
               } else if (sectionId.equalsIgnoreCase("remove1")) {
                  text2 = "REMOVE1";
               } else if (sectionId.equalsIgnoreCase("add1")) {
                  text2 = "ADD1";
               } else if (sectionId.equalsIgnoreCase("add10") || sectionId.equalsIgnoreCase("set16")) {
                  text2 = "ADD16";
               } else if (sectionId.equalsIgnoreCase("set64")) {
                  text2 = "ADD32";
               } else if (sectionId.equalsIgnoreCase("confirm")) {
                  map.put("type", "CONFIRM");
                  text2 = "CONFIRM_TRANSACTION";
               } else if (sectionId.equalsIgnoreCase("sellAll")) {
                  text2 = "SELL_ALL";
               } else if (sectionId.equalsIgnoreCase("buyMore")) {
                  text2 = "OPEN_BUY_STACKS";
               } else if (sectionId.equalsIgnoreCase("cancel")) {
                  text2 = "BACK";
               } else if (sectionId.equalsIgnoreCase("sellMore")) {
                  continue;
               }

               map.put("action", text2);
            } else if (section.getCurrentPath().equalsIgnoreCase("amountSelectionGUIBulkBuy")) {
               if (sectionId.equalsIgnoreCase("cancel")) {
                  map.put("action", "BACK");
               } else {
                  map.put("action", "INSTA_BUY");
               }
            }

            shopIds.put("items." + sectionId, map);
            value++;
         } catch (Exception exception) {
            SendMessage.errorMessage(
               "Failed to import transaction screen button for '"
                  + section.getCurrentPath()
                  + ".buttons."
                  + sectionId
                  + "' inside the config.yml, see reason below, skipping item..."
            );
            exception.printStackTrace();
            value2++;
         }
      }

      return new Import.TransactionMenuData(shopIds, value, value2);
   }

   private Map<String, Object> importShopGUIPlusFillItem(ConfigurationSection section) throws ItemImportException {
      HashMap shopIds = new HashMap();
      String itemId = "";
      if (section.get("material") != null) {
         itemId = section.getString("material");
      }

      if (section.get("damage") != null) {
         itemId = itemId + ":" + section.get("damage");
      }

      if (!itemId.isEmpty()) {
         shopIds.put("material", itemId);
         if (section.get("name") != null) {
            shopIds.put("name", section.getString("name"));
         }

         if (!section.getStringList("lore").isEmpty()) {
            shopIds.put("lore", section.getStringList("lore"));
         }

         if (section.get("quantity") != null) {
            shopIds.put("stack-size", section.getInt("quantity"));
         }

         if (section.get("amount") != null) {
            shopIds.put("stack-size", section.getInt("amount"));
         }

         HashMap map = new HashMap();
         if (!section.getStringList("flags").isEmpty()) {
            map.put("HideFlags", this.flagsToByte(section.getStringList("flags"), section.getCurrentPath()));
         }

         if (!map.isEmpty()) {
            shopIds.put("NBTData", new Gson().toJson(map).toString());
         }

         return shopIds;
      } else {
         throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
      }
   }

   private Map<String, Object> importShopGUIPlusSectionItem(Import.ShopGUIPlusData shopGUIPlusData, ConfigurationSection section) throws ItemImportException {
      HashMap shopIds = new HashMap();
      if (section.get("type") != null && section.getString("type").equalsIgnoreCase("special")) {
         if (section.getString("type").equalsIgnoreCase("special")) {
         }
      } else {
         String sectionId = "";
         if (section.get("item.material") != null) {
            sectionId = section.getString("item.material");
         }

         if (section.get("item.damage") != null) {
            sectionId = sectionId + ":" + section.get("item.damage");
         }

         if (sectionId.isEmpty()) {
            throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
         }

         shopIds.put("item.material", sectionId);
         if (section.get("item.name") != null) {
            shopIds.put("item.name", section.getString("item.name"));
         }

         if (!section.getStringList("item.lore").isEmpty()) {
            shopIds.put("item.lore", section.getStringList("item.lore"));
         }

         if (section.get("item.quantity") != null) {
            shopIds.put("item.stack-size", section.getInt("item.quantity"));
         }

         if (section.get("item.amount") != null) {
            shopIds.put("stack-size", section.getInt("item.amount"));
         }

         if (section.get("item.skin") != null) {
            shopIds.put("item.skull-texture", section.getString("item.skin"));
         }

         if (section.get("shop") != null) {
            if (section.getString("shop").isEmpty()) {
               shopIds.put("section", "fill-" + shopGUIPlusData.fill++);
               shopIds.put("display-item", true);
            } else {
               shopIds.put("section", section.getString("shop"));
            }
         }

         if (section.get("slot") != null) {
            shopIds.put("slot", section.getInt("slot") + 1);
         }

         if (!section.getStringList("item.flags").isEmpty()) {
            shopIds.put("item-flags", section.getStringList("item.flags"));
         }
      }

      return shopIds;
   }

   private Map<String, Object> importShopGUIPlusSpecialItem(ConfigurationSection section) throws ItemImportException {
      HashMap shopIds = new HashMap();
      String itemId = "";
      if (section.get("item.material") != null) {
         itemId = section.getString("item.material");
      }

      if (section.get("item.damage") != null) {
         itemId = itemId + ":" + section.get("item.damage");
      }

      if (!itemId.isEmpty()) {
         shopIds.put("material", itemId);
         if (section.get("item.name") != null) {
            shopIds.put("name", section.getString("item.name"));
         }

         if (!section.getStringList("item.lore").isEmpty()) {
            List lores = section.getStringList("item.lore")
               .stream()
               .map(element -> element.contains("%balance%") ? element.replace("%balance%", "%player_balance%").replace("$", "").replace("€", "") : element)
               .collect(Collectors.toList());
            shopIds.put("lore", lores);
         }

         if (section.get("item.quantity") != null) {
            shopIds.put("stack-size", section.getInt("item.quantity"));
         }

         if (section.get("item.amount") != null) {
            shopIds.put("stack-size", section.getInt("item.amount"));
         }

         if (section.get("item.skin") != null) {
            shopIds.put("skull-texture", section.getString("item.skin"));
         }

         if (section.get("item.skullOwner") != null) {
            shopIds.put("skullowner", section.getString("item.skullOwner"));
         }

         if (!section.getStringList("item.flags").isEmpty()) {
            shopIds.put("item-flags", section.getStringList("item.flags"));
         }

         return shopIds;
      } else {
         throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
      }
   }

   private Map<String, Object> importShopGUIPlusItem(Import.ShopGUIPlusData shopGUIPlusData, ConfigurationSection section) throws UnsupportedItemFlagException , ItemImportException{
      HashMap shopIds = new HashMap();
      if (section.get("type") != null && section.getString("type").equalsIgnoreCase("special")) {
         if (section.getString("type").equalsIgnoreCase("special")) {
         }
      } else {
         String itemId = "";
         if (section.get("item.material") != null) {
            itemId = section.getString("item.material");
         }

         if (section.get("item.damage") != null) {
            itemId = itemId + ":" + section.get("item.damage");
         }

         if (section.get("item.headDatabase") != null) {
            itemId = "hdb:" + section.getString("item.headDatabase");
         }

         if (section.get("item.nexo") != null) {
            itemId = "nexo:" + section.getString("item.nexo");
         }

         if (section.get("item.mmoItems.type") != null && section.get("item.mmoItems.id") != null) {
            itemId = "MMOItem:" + section.getString("item.mmoItems.type") + section.getString("item.mmoItems.id");
         }

         if (section.get("item.crackShot") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.crackShot', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (section.get("item.oraxen") != null) {
            itemId = "oraxen:" + section.getString("item.oraxen");
         }

         if (section.get("item.customItems") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.customItems', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (section.get("item.qualityArmory") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.customItems', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (section.get("item.brewery.recipe") != null) {
            String text = section.getString("item.brewery.recipe");
            if (section.get("item.brewery.quality") != null) {
               text = text + "." + section.getInt("item.brewery.quality");
            }

            itemId = "brewery:" + text;
         }

         if (section.get("item.executableItems") != null) {
            itemId = "ei:" + section.getString("item.executableItems");
         }

         if (section.get("item.itemsAdder") != null) {
            itemId = "ia:" + section.getString("item.itemsAdder");
         }

         if (section.get("item.slimefun") != null) {
            itemId = "sf:" + section.getString("item.slimefun");
         }

         if (section.get("item.mythicMobs") != null) {
            itemId = "mythicmobs:" + section.getString("item.mythicMobs");
         }

         if (itemId.isEmpty()) {
            throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
         }

         shopIds.put("material", itemId);
         if (section.get("page") != null) {
            shopIds.put("page", section.getInt("page"));
         }

         Object value = null;
         if (section.get("slot") != null) {
            value = section.getInt("slot") + 1;
         }

         if (section.get("slots") != null) {
            value = section.getIntegerList("slots").stream().map(element -> String.valueOf(element + 1)).collect(Collectors.joining(","));
         }

         if (value != null) {
            shopIds.put("slot", value);
         }

         if (section.get("shop") != null) {
            shopIds.put("section", section.getString("shop"));
         }

         if (section.get("item.name") != null) {
            shopIds.put("name", section.getString("item.name"));
         }

         if (section.get("buyPrice") != null) {
            shopIds.put("buy", section.getDouble("buyPrice"));
         }

         if (section.get("sellPrice") != null) {
            shopIds.put("sell", section.getDouble("sellPrice"));
         }

         if (!section.getStringList("item.enchantments").isEmpty()) {
            shopIds.put("enchantments", section.getStringList("item.enchantments"));
         }

         if (!section.getStringList("item.lore").isEmpty()) {
            shopIds.put("lore", section.getStringList("item.lore"));
         }

         if (section.get("item.model") != null) {
            shopIds.put("CustomModelData", section.getString("item.model"));
         }

         if (!section.getStringList("item.flags").isEmpty()) {
            shopIds.put("item-flags", section.getStringList("item.flags"));
         }

         if (!section.getStringList("item.musicInstrument").isEmpty()) {
            shopIds.put("instrument", section.getString("item.musicInstrument").replace("_GOAT_HORN", ""));
         }

         if (section.get("item.mob") != null) {
            if (itemId.startsWith("MONSTER_EGG")) {
               String text = null;

               try {
                  text = EntityType.fromId(Integer.parseInt(section.getString("item.mob"))).name();
               } catch (NumberFormatException exception) {
                  text = EntityType.valueOf(section.getString("item.mob").toUpperCase(Locale.ENGLISH)).name();
               } catch (NullPointerException exception) {
               }

               if (text == null || !XMaterial.matchXMaterial(text + "_SPAWN_EGG").isPresent()) {
                  throw new ItemImportException("Failed to import spawn egg data '" + section.get("item.mob") + "' from ShopGUI+, skipping item...");
               }

               shopIds.put("material", XMaterial.matchXMaterial(text + "_SPAWN_EGG").get().name());
            } else {
               if (!itemId.startsWith("SPAWNER") && !itemId.startsWith("MOB_SPAWNER")) {
                  throw new ItemImportException("Failed to import mob type '" + section.get("item.mob") + "' from ShopGUI+, skipping item...");
               }

               shopIds.put("spawnertype", section.getString("item.mob"));
            }
         }

         HashMap map = new HashMap();
         if (section.getConfigurationSection("item.potion") != null) {
            ConfigurationSection section2 = section.getConfigurationSection("item.potion");
            if (section2.getString("type") != null) {
               try {
                  Optional<PotionTypes> optional = PotionTypes.matchPotionDataLegacy(
                     PotionType.valueOf(section2.getString("type")),
                     section2.getInt("level") == 1 && section2.getBoolean("extended"),
                     section2.getInt("level") > 1 && !section2.getBoolean("extended")
                  );
                  optional.ifPresent(element -> shopIds.put("potiontypes", Collections.singletonList(element.name())));
                  optional.orElseThrow(() -> new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get()));
               } catch (ItemLoadException | IllegalArgumentException exception) {
               }

               if (!shopIds.containsKey("potiontypes")) {
                  map.put(
                     "CustomPotionEffects",
                     Arrays.<String>asList(
                           "{Id:" + PotionEffectType.getByKey(NamespacedKey.minecraft(section2.getString("type"))).getId(),
                           "Amplifier:" + section2.getInt("level"),
                           "Duration:" + PotionTypes.getFromType(section2.getString("type")).get().getDuration()
                        )
                        + "}"
                  );
               }
            }

            if (section2.getString("color") != null) {
               map.put("CustomPotionColor", section2.getString("color").toUpperCase(Locale.ENGLISH));
            }
         }

         if (section.getBoolean("item.unbreakable")) {
            map.put("Unbreakable", 1);
         }

         if (section.get("item.nbt") != null) {
            for (String sectionId : section.getConfigurationSection("item.nbt").getKeys(false)) {
               if (section.get("item.nbt." + sectionId + ".type") != null && section.get("item.nbt." + sectionId + ".key") != null) {
                  if (section.getString("item.nbt." + sectionId + ".type").equalsIgnoreCase("COMPOUND")) {
                     HashMap map2 = new HashMap();
                     if (section.get("item.nbt." + sectionId + ".children") != null && section.getString("item.nbt." + sectionId + ".key") != null) {
                        for (String text : section.getConfigurationSection("item.nbt." + sectionId + ".children").getKeys(false)) {
                           if (section.getString("item.nbt." + sectionId + ".children." + text + ".key") != null
                              && section.get("item.nbt." + sectionId + ".children." + text + ".value") != null) {
                              map2.put(
                                 section.getString("item.nbt." + sectionId + ".children." + text + ".key"),
                                 section.get("item.nbt." + sectionId + ".children." + text + ".value")
                              );
                           }
                        }

                        map.put(section.getString("item.nbt." + sectionId + ".key"), new Gson().toJson(map2).toString());
                     }
                  }
               } else if (section.getString("item.nbt." + sectionId + ".key") != null && section.get("item.nbt." + sectionId + ".value") != null) {
                  map.put(section.getString("item.nbt." + sectionId + ".key"), section.get("item.nbt." + sectionId + ".value"));
               }
            }
         }

         if (!map.isEmpty()) {
            shopIds.put("NBTData", new Gson().toJson(map).toString());
         }

         if (!map.isEmpty()) {
            System.out.println(shopIds.get("NBTData"));
         }

         if (section.get("item.quantity") != null) {
            shopIds.put("stack-size", section.getInt("item.quantity"));
         }

         if (section.get("item.amount") != null) {
            shopIds.put("stack-size", section.getInt("item.amount"));
         }

         if (section.getBoolean("item.glow")) {
            shopIds.put("enchantment-glint", true);
         }

         if (section.getString("item.color") != null
            && (
               itemId.equalsIgnoreCase("LEATHER_HELMET")
                  || itemId.equalsIgnoreCase("LEATHER_BOOTS")
                  || itemId.equalsIgnoreCase("LEATHER_CHESTPLATE")
                  || itemId.equalsIgnoreCase("LEATHER_LEGGINGS")
            )) {
            try {
               shopIds.put("armorcolor", this.getColor(section.getString("item.color")).asARGB());
            } catch (IllegalAccessException | NoSuchFieldException exception) {
               try {
                  List values = Arrays.stream(section.getString("item.color").replace(" ", "").split(",")).map(Integer::parseInt).collect(Collectors.toList());
                  shopIds.put("armorcolor", Color.fromRGB((Integer)values.get(0), (Integer)values.get(1), (Integer)values.get(2)).asARGB());
               } catch (NumberFormatException exception2) {
                  throw new ItemImportException("Failed to import armor color '" + section.getString("item.color") + "' from ShopGUI+, skipping item...");
               }
            }
         }

         if (section.get("item.fireworkPower") != null) {
            shopIds.put("duration", section.get("item.fireworkPower"));
         }

         if (section.get("item.fireworkEffects") != null) {
            Iterator iterator = section.getConfigurationSection("item.fireworkEffects").getKeys(false).iterator();
            if (iterator.hasNext()) {
               String text = (String)iterator.next();
               if (section.get("item.fireworkEffects." + text + ".type") != null) {
                  shopIds.put("shape", section.getString("item.fireworkEffects." + text + ".type"));
               }

               if (section.get("item.fireworkEffects." + text + ".flicker") != null) {
                  shopIds.put("flicker", section.getBoolean("item.fireworkEffects." + text + ".flicker"));
               }

               if (section.get("item.fireworkEffects." + text + ".trail") != null) {
                  shopIds.put("trail", section.getBoolean("item.fireworkEffects." + text + ".trail"));
               }

               if (!section.getStringList("item.fireworkEffects." + text + ".colors").isEmpty()) {
                  shopIds.put("colors", section.getStringList("item.fireworkEffects." + text + ".colors"));
               }

               if (!section.getStringList("item.fireworkEffects." + text + ".fadeColors").isEmpty()) {
                  shopIds.put("colors", section.getStringList("item.fireworkEffects." + text + ".fadeColors"));
               }
            }
         }

         if (section.get("item.skullOwner") != null) {
            shopIds.put("skullowner", section.getString("item.skullOwner"));
         }

         if (section.get("item.skin") != null) {
            shopIds.put("skull-texture", section.getString("item.skin"));
         }

         if (section.get("placeholder") != null) {
            String text = "";
            if (section.get("item.material") != null) {
               text = section.getString("item.material");
            }

            if (section.get("item.damage") != null) {
               text = itemId + ":" + section.get("item.damage");
            }

            if (!text.isEmpty()) {
               shopIds.put("display-item.material", text);
            }

            if (section.get("placeholder.name") != null) {
               shopIds.put("display-item.name", section.getString("placeholder.name"));
            }

            if (section.get("placeholder.quantity") != null) {
               shopIds.put("display-item.stack-size", section.getString("placeholder.stack-size"));
            }

            if (section.get("placeholder.lore") != null) {
               shopIds.put("display-item.lore", section.getStringList("placeholder.lore"));
            }
         }

         ArrayList values = new ArrayList();
         if (section.get("permission") != null) {
            if (this.getPermCommand(section.getString("permission")) == null) {
               throw new ItemImportException(
                  "Failed to import permission '"
                     + section.getString("permission")
                     + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
               );
            }

            values.add(this.getPermCommand(section.getString("permission")));
         }

         if (!section.getStringList("permissions").isEmpty()) {
            for (String text : section.getStringList("permissions")) {
               if (this.getPermCommand(text) == null) {
                  throw new ItemImportException(
                     "Failed to import permission '" + text + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                  );
               }

               values.add(this.getPermCommand(text));
            }
         }

         if (section.get("permissions") != null) {
            for (String text : section.getConfigurationSection("permissions").getKeys(false)) {
               if (section.get("permissions." + text + ".permission") != null) {
                  if (section.get("permissions." + text + ".world") != null) {
                     if (this.getPermCommand(section.getString("permissions." + text + ".permission"), section.getString("permissions." + text + ".world")) == null
                        )
                      {
                        throw new ItemImportException(
                           "Failed to import per world permission 'permissions."
                              + text
                              + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                        );
                     }

                     values.add(this.getPermCommand(section.getString("permissions." + text + ".permission"), section.getString("permissions." + text + ".world")));
                  } else {
                     if (this.getPermCommand(text) == null) {
                        throw new ItemImportException(
                           "Failed to import permission '" + text + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                        );
                     }

                     values.add(this.getPermCommand(text));
                  }
               }
            }
         }

         if (!section.getStringList("commands").isEmpty()) {
            values.addAll(
               section.getStringList("commands")
                  .stream()
                  .map(element -> section.getBoolean("runAsBuyer") ? "p:" : "" + element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!section.getStringList("commandsOnBuy").isEmpty()) {
            values.addAll(
               section.getStringList("commandsOnBuy")
                  .stream()
                  .map(element -> "p:" + element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!section.getStringList("commandsOnBuyConsole").isEmpty()) {
            values.addAll(
               section.getStringList("commandsOnBuyConsole")
                  .stream()
                  .map(element -> element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!values.isEmpty()) {
            shopIds.put("buy-commands", values);
         }

         ArrayList values2 = new ArrayList();
         if (!section.getStringList("commandsOnSell").isEmpty()) {
            values2.addAll(
               section.getStringList("commandsOnSell")
                  .stream()
                  .map(element -> "p:" + element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!section.getStringList("commandsOnSellConsole").isEmpty()) {
            values2.addAll(
               section.getStringList("commandsOnSellConsole")
                  .stream()
                  .map(element -> element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!values2.isEmpty()) {
            shopIds.put("sell-commands", values);
         }

         ArrayList values3 = new ArrayList();
         if (!section.getStringList("commandsOnClick").isEmpty()) {
            values3.addAll(
               section.getStringList("commandsOnClick")
                  .stream()
                  .map(element -> "p:" + element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!section.getStringList("commandsOnClickConsole").isEmpty()) {
            values3.addAll(
               section.getStringList("commandsOnClickConsole")
                  .stream()
                  .map(element -> element.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!values3.isEmpty()) {
            shopIds.put("click-commands", values);
         }

         if (section.get("commandsLimit") != null) {
            shopIds.put("stock-limit-player", section.getInt("commandsLimit"));
         }

         if (section.getBoolean("runSingleCommand")) {
            shopIds.put("max-buy", -1);
         }
      }

      return shopIds;
   }

   private void completeImporting(Object value) {
      String pluginName = this.pluginName;
      byte value2 = -1;
      switch (pluginName.hashCode()) {
         case -217641569:
            if (pluginName.equals("shopguiplus")) {
               value2 = 0;
            }
         default:
            switch (value2) {
               case 0:
                  if (this.data == null) {
                     SendMessage.sendMessage(
                        value,
                        ChatColor.RED
                           + "No data ready for importing yet, please use "
                           + ChatColor.WHITE
                           + "/onyx import shopguiplus"
                           + ChatColor.RED
                           + "first."
                     );
                     return;
                  } else {
                     try {
                        Path path = this.createZipFile();
                        SendMessage.infoMessage(
                           value, ChatColor.GREEN + "Creating backup of current shop layout, this can be found in " + path.toFile().getAbsolutePath() + "..."
                        );
                        FileOutputStream fileOutputStream = new FileOutputStream(path + ".zip");
                        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
                        this.zipLayout(zipOutputStream);
                        zipOutputStream.close();
                        fileOutputStream.close();
                     } catch (IOException exception) {
                        SendMessage.warnMessage(value, "Failed to create backup of current shop layout.");
                        exception.printStackTrace();
                        return;
                     }

                     try {
                        if (new File(this.plugin.getDataFolder() + File.separator + "config.yml").exists()) {
                           Files.delete(ConfigManager.getConfig().getFilePath());
                        }

                        File file = new File(this.plugin.getDataFolder() + File.separator + "sections");
                        if (file.exists() && file.isDirectory()) {
                           for (File file2 : file.listFiles()) {
                              try {
                                 Files.delete(file2.toPath());
                              } catch (IOException exception) {
                              }
                           }
                        }

                        File file2 = new File(this.plugin.getDataFolder() + File.separator + "shops");
                        if (file2.exists() && file2.isDirectory()) {
                           for (File file3 : file2.listFiles()) {
                              try {
                                 Files.delete(file3.toPath());
                              } catch (IOException exception) {
                              }
                           }
                        }
                     } catch (IOException exception) {
                        SendMessage.sendMessage(
                           value,
                           ChatColor.RED
                              + "Failed to delete the existing configurations, you will need to delete the files manually(shops folder, sections folder, config.yml) and perform this command again."
                        );
                        exception.printStackTrace();
                        return;
                     }

                     SendMessage.infoMessage("Exporting cached data...");

                     try {
                        for (String key : this.data.getShops().keySet()) {
                           Config config = this.plugin.getConfigManager().createShopConfig(key);

                           for (Entry entry : this.data.getShops().get(key).entrySet()) {
                              config.set((String)entry.getKey(), entry.getValue());
                           }

                           config.save(new File(this.plugin.getDataFolder() + "/shops/" + key + ".yml"));
                        }

                        for (String text : this.data.getSections().keySet()) {
                           Config config = this.plugin.getConfigManager().createSectionConfig(text);

                           for (Entry entry : this.data.getSections().get(text).entrySet()) {
                              config.set((String)entry.getKey(), entry.getValue());
                           }

                           config.save(new File(this.plugin.getDataFolder() + "/sections/" + text + ".yml"));
                        }

                        YamlConfiguration config = new YamlConfiguration();

                        for (String text : this.data.getConfig().keySet()) {
                           config.set(text, this.data.getConfig().get(text));
                        }

                        config.save(new File(this.plugin.getDataFolder() + File.separator + "config.yml"));
                        this.plugin.getConfigManager().loadConfig();
                        SendMessage.sendMessage(value, "§aImport success, the existing shop layout was replaced with the loaded data.");
                        if (this.data.isRestartRequired()) {
                           SendMessage.sendMessage(value, "§aA server restart is required to load all the imported data.");
                        } else {
                           SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
                        }

                        return;
                     } catch (IllegalArgumentException | IOException exception) {
                        SendMessage.sendMessage(value, "§cFailed to save configurations because an IOE error occurred, see console for error stacktrace.");
                        exception.printStackTrace();
                        return;
                     }
                  }
               default:
                  SendMessage.sendMessage(value, Lang.PLUGIN_NOT_FOUND.get());
            }
      }
   }

   private void setSection(Config config, Object value, String sectionId) {
      if (value instanceof Map) {
         for (Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
            if (!(entry.getValue() instanceof MemorySection)) {
               config.set(sectionId + "." + entry.getKey(), entry.getValue());
            }
         }
      } else {
         config.set(sectionId, value);
      }
   }

   private Map<String, Object> importGUIShopItem(ConfigurationSection section) throws ItemLoadException {
      HashMap shopIds = new HashMap();
      if (section.get("id") != null) {
         shopIds.put("material", section.getString("id"));
      }

      if (section.get("target-shop") != null) {
         shopIds.put("section", section.getString("target-shop"));
      }

      if (section.get("name") != null) {
         shopIds.put("displayname", section.getString("name"));
      }

      if (section.get("buy-name") != null) {
         shopIds.put("name", section.getString("buy-name"));
      }

      if (section.get("buy-price") != null) {
         shopIds.put("buy", section.getDouble("buy-price"));
      }

      if (section.get("sell-price") != null) {
         shopIds.put("sell", section.getDouble("sell-price"));
      }

      if (section.get("enchantments") != null) {
         shopIds.put("enchantments", Arrays.stream(section.getString("enchantments").split(" ")).collect(Collectors.toList()));
      }

      if (!section.getStringList("shop-lore").isEmpty()) {
         shopIds.put("displaylore", section.getStringList("shop-lore").stream().map(element -> element.split("\n")).collect(Collectors.toList()));
      }

      if (!section.getStringList("lore").isEmpty()) {
         shopIds.put("lore", section.getStringList("lore").stream().map(element -> element.split("\n")).collect(Collectors.toList()));
      }

      if (!section.getStringList("buy-lore").isEmpty()) {
         shopIds.put("lore", section.getStringList("buy-lore").stream().map(element -> element.split("\n")).collect(Collectors.toList()));
      }

      HashMap itemIds = new HashMap();
      if (section.get("custom-nbt") != null) {
         itemIds.putAll(this.getNBTFromString(section.getString("custom-nbt"), section.getCurrentPath()));
      }

      if (section.get("custom-model") != null) {
         itemIds.put("CustomModelData", section.getString("custom-model"));
      }

      if (section.get("item-flags") != null) {
         itemIds.put("HideFlags", this.flagsToByte(Arrays.stream(section.getString("item-flags").split(" ")).collect(Collectors.toList()), section.getCurrentPath()));
      }

      if (!itemIds.isEmpty()) {
         shopIds.put("NBTData", new Gson().toJson(itemIds).toString());
      }

      if (!itemIds.isEmpty()) {
         System.out.println(shopIds.get("NBTData"));
      }

      if (section.get("quantity") != null) {
         shopIds.put("stack-size", section.getString("quantity"));
      }

      if (!section.getStringList("commands").isEmpty()) {
         shopIds.put("commands", section.getStringList("commands").stream().map(element -> element.replace("%player_name%", "%player%")).collect(Collectors.toList()));
      }

      if (section.get("skull-uuid") != null) {
         shopIds.put("skull-texture", section.getString("skull-uuid"));
      }

      if (section.get("mob-type") != null) {
         shopIds.put("spawnertype", section.getString("mob-type"));
      }

      if (section.get("potion-info") != null) {
         Map sectionIds = section.getConfigurationSection("potion-info").getValues(true);
         if (Boolean.parseBoolean(sectionIds.get("splash").toString()) && shopIds.containsKey("material")) {
            Optional optional = XMaterial.matchXMaterial(shopIds.get("material").toString());
            if (optional.isPresent() && !((XMaterial)optional.get()).equals(XMaterial.SPLASH_POTION)) {
               shopIds.put("material", XMaterial.SPLASH_POTION.toString());
            }
         }

         if (sectionIds.containsKey("type")) {
            shopIds.put(
               "potiontypes",
               PotionTypes.matchPotionDataLegacy(
                     PotionType.valueOf(sectionIds.get("type").toString()),
                     Boolean.parseBoolean(sectionIds.get("extended").toString()),
                     Boolean.parseBoolean(sectionIds.get("upgraded").toString())
                  )
                  .orElseThrow(() -> new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get()))
                  .name()
            );
         }
      }

      return shopIds;
   }

   private String getPermCommand(String permission) {
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
         return "/lp user %player% permission set " + permission + " true";
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("PowerRanks")) {
         return "/pr addplayerperm %player% " + permission;
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("GroupManager")) {
         return "/manuaddp %player% " + permission;
      } else {
         return this.plugin.getServer().getPluginManager().isPluginEnabled("UltraPermissions") ? "/upc AddPlayerPermission %player% " + permission : null;
      }
   }

   private String getPermCommand(String permission, String command) {
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
         return "/lp user %player% permission set " + permission + " true world=" + command;
      } else {
         return this.plugin.getServer().getPluginManager().isPluginEnabled("UltraPermissions")
            ? "/upc AddPlayerPermission %player% " + permission + " " + command
            : null;
      }
   }

   private Color getColor(String text) throws NoSuchFieldException , IllegalAccessException{
      Field field = Color.class.getField(text);
      field.setAccessible(true);
      return (Color)field.get(null);
   }

   private int flagsToByte(List<String> values, String text) {
      byte value = 0;

      for (String text2 : values) {
         try {
            value |= (byte)(1 << ItemFlag.valueOf(text2).ordinal());
         } catch (NullPointerException | IllegalArgumentException exception) {
            SendMessage.warnMessage("&cInvalid item flag found '" + text2 + "' while importing item '" + text + "' from GUIShop, skipping...");
         }
      }

      return value;
   }

   private Map<String, Object> getNBTFromString(String text, String text2) {
      try {
         return (Map<String, Object>)new Gson().fromJson(text, Map.class);
      } catch (JsonSyntaxException exception) {
         SendMessage.warnMessage("&cInvalid nbt tag found '" + text + "' while importing item '" + text2 + "' from GUIShop, skipping...");
         return new HashMap<>();
      }
   }

   private void emptyCache() {
      this.plugin.runTaskLater(() -> this.CACHE.clear(), 6000L);
   }

   private Map<String, Object> getSectionData(String sectionId, int value) {
      LinkedHashMap map = new LinkedHashMap();
      map.put("enable", true);
      map.put("slot", value);
      map.put("title", "&8&l" + sectionId);
      map.put("hidden", false);
      map.put("sub-section", false);
      map.put("display-item", false);
      map.put("economy", ConfigManager.getConfig().getString("ShopSections." + sectionId + ".economy"));
      map.put("nav-bar.mode", "INHERIT");
      HashMap map2 = new HashMap();
      map2.put("material", "COOKED_BEEF");
      map.put("item", map2);
      return map;
   }

   private Path createZipFile() {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
      LocalDateTime localDateTime = LocalDateTime.now();
      File file = new File(this.plugin.getDataFolder() + "/backups");
      if (!file.exists()) {
         file.mkdirs();
      }

      String text = this.plugin.getDataFolder() + "/backups/layout - " + dateTimeFormatter.format(localDateTime);
      int value = 0;

      while (new File(text + (value > 0 ? "(" + value + ")" : "")).exists()) {
         value++;
      }

      return Paths.get(text + (value > 0 ? "(" + value + ")" : ""));
   }

   private void zipLayout(ZipOutputStream zipOutputStream) throws FileNotFoundException, IOException {
      for (String text : Arrays.asList("shops", "sections", "config.yml", "stands.json", "cache")) {
         String path = this.plugin.getDataFolder().getPath() + File.separator + text;
         if (path.contains("\\.")) {
            FileInputStream fileInputStream = new FileInputStream(path);
            zipOutputStream.putNextEntry(new ZipEntry(text));
            byte[] value = new byte[1024];

            int value2;
            while ((value2 = fileInputStream.read(value)) >= 0) {
               zipOutputStream.write(value, 0, value2);
            }

            fileInputStream.close();
         } else {
            this.zipFiles(zipOutputStream, text);
         }
      }
   }

   private void zipFiles(ZipOutputStream zipOutputStream, String text) throws FileNotFoundException, IOException {
      File file = new File(this.plugin.getDataFolder() + File.separator + text);
      if (file.exists() && file.isDirectory()) {
         for (File file2 : file.listFiles()) {
            if (!file2.isDirectory()) {
               String path = file2.getPath();
               FileInputStream fileInputStream = new FileInputStream(path);
               zipOutputStream.putNextEntry(new ZipEntry(text + "/" + file2.toPath().getFileName().toString()));
               byte[] value = new byte[1024];

               int value2;
               while ((value2 = fileInputStream.read(value)) >= 0) {
                  zipOutputStream.write(value, 0, value2);
               }

               fileInputStream.close();
            }
         }
      }
   }

   private class ShopGUIPlusData {
      private final Map<String, Map<String, Object>> shops = new HashMap<>();
      private final Map<String, Map<String, Object>> sections = new HashMap<>();
      private final Map<String, Object> config = new HashMap<>();
      private final Map<String, Map<String, Object>> specials = new HashMap<>();
      private boolean restartRequired;
      public int fill = 0;

      public ShopGUIPlusData() {
      }

      public void addShopsData(String text, Map<String, Object> map) {
         this.shops.put(text, map);
      }

      public void addSectionsData(String text, Map<String, Object> map) {
         this.sections.put(text, map);
      }

      public void addConfigData(String text, Object value) {
         this.config.put(text, value);
      }

      public void addSpecial(String text, Map<String, Object> map) {
         this.specials.put(text, map);
      }

      public Map<String, Map<String, Object>> getShops() {
         return this.shops;
      }

      public Map<String, Map<String, Object>> getSections() {
         return this.sections;
      }

      public Map<String, Object> getConfig() {
         return this.config;
      }

      public Map<String, Object> getSpecial(String text) {
         return this.specials.get(text);
      }

      public Map<String, Map<String, Object>> getSpecials() {
         return this.specials;
      }

      public void setRestartRequired(boolean flag) {
         this.restartRequired = flag;
      }

      public boolean isRestartRequired() {
         return this.restartRequired;
      }
   }

   private class TransactionMenuData {
      public final Map<String, Object> data;
      public final int itemsSuccess;
      public final int itemsFailed;

      public TransactionMenuData(Map<String, Object> data, int itemsSuccess, int itemsFailed) {
         this.data = data;
         this.itemsSuccess = itemsSuccess;
         this.itemsFailed = itemsFailed;
      }
   }
}
