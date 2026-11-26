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

   public Import(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length <= 1) {
         SendMessage.sendMessage(var1, this.getSyntax());
      } else if (this.supported.containsKey(var2[1].toLowerCase(Locale.ENGLISH))) {
         this.pluginName = var2[1].toLowerCase(Locale.ENGLISH);
         if (var2.length <= 2) {
            SendMessage.sendMessage(var1, this.getSyntax());
         } else {
            this.fileName = var2[2].toLowerCase(Locale.ENGLISH);
            this.file = this.getFile(this.pluginName, this.fileName);
            if (this.file != null && this.supported.get(this.pluginName).contains(this.fileName)) {
               if (var2.length > 3 && var2[3].equalsIgnoreCase("confirm") && this.pluginName.equalsIgnoreCase("shopguiplus")) {
                  this.completeImporting(var1);
               } else {
                  this.importChanges(var1);
               }
            } else {
               SendMessage.sendMessage(var1, Lang.FILE_NOT_FOUND.get().replace("%fileName%", this.fileName).replace("%pluginName%", this.pluginName));
            }
         }
      } else {
         SendMessage.sendMessage(var1, Lang.PLUGIN_NOT_FOUND.get());
      }
   }

   private Map<String, List<String>> getSupportedFiles() {
      HashMap var2 = new HashMap();
      File var1 = new File(this.plugin.getDataFolder().getParent() + "/OnyxShop/");
      if (var1.exists() && var1.isDirectory()) {
         var2.put("economyshopgui", Arrays.asList("all"));
      }

      var1 = new File(this.plugin.getDataFolder().getParent() + "/Essentials/");
      if (var1.exists() && var1.isDirectory()) {
         var2.put("essentials", Arrays.asList("worth.yml"));
      }

      var1 = new File(this.plugin.getDataFolder().getParent() + "/ShopGUIPlus/");
      if (var1.exists() && var1.isDirectory()) {
         var2.put("shopguiplus", Arrays.asList("all"));
      }

      return var2;
   }

   private File getFile(String var1) {
      return this.getFile(this.pluginName, var1);
   }

   private File getFile(String var1, String var2) {
      String var4 = var1.toLowerCase(Locale.ENGLISH);
      switch (var4) {
         case "essentials":
            File var7 = new File(this.plugin.getDataFolder().getParent() + "/Essentials/" + var2);
            return var7.exists() ? var7 : null;
         case "economyshopgui":
            File var6 = new File(this.plugin.getDataFolder().getParent() + "/OnyxShop/");
            return var6.exists() ? var6 : null;
         case "shopguiplus":
            File var3 = new File(this.plugin.getDataFolder().getParent() + "/ShopGUIPlus/");
            return var3.exists() ? var3 : null;
         default:
            return null;
      }
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

   private XMaterial getMaterial(String var1) {
      try {
         ItemStack var2 = this.ess.getItemDb().get(var1);
         return XMaterial.matchXMaterial(var2);
      } catch (Exception var3) {
         return null;
      }
   }

   private List<XMaterial> getMaterials(ConfigurationSection var1, String var2) {
      ArrayList var3 = new ArrayList();
      int var4 = 0;

      while (true) {
         Optional var5 = XMaterial.matchXMaterial(var2 + ":" + var4);
         if (!var5.isPresent() || ((XMaterial)var5.get()).parseItem() == null || var1.getDouble("worth." + var2 + ".*", -5.0) == -5.0) {
            return var3;
         }

         if (!var1.getConfigurationSection("worth." + var2).getKeys(false).contains(String.valueOf(var4))) {
            var3.add((XMaterial)var5.get());
         }

         var4++;
      }
   }

   private XMaterial getMaterial(ConfigurationSection var1, String var2, String var3) {
      Optional var4 = XMaterial.matchXMaterial(var2 + ":" + var3);
      return var4.isPresent() && ((XMaterial)var4.get()).parseItem() != null && var1.getDouble("worth." + var2 + "." + var3, -5.0) != -5.0
         ? (XMaterial)var4.get()
         : null;
   }

   private void importChanges(Object var1) {
      if (this.pluginName.equalsIgnoreCase("economyshopgui")) {
         try {
            Path var2 = this.createZipFile();
            SendMessage.infoMessage(
               var1, ChatColor.GREEN + "Creating backup of current shop layout, this can be found in " + var2.toFile().getAbsolutePath() + "..."
            );
            FileOutputStream var3 = new FileOutputStream(var2 + ".zip");
            ZipOutputStream var4 = new ZipOutputStream(var3);
            this.zipLayout(var4);
            var4.close();
            var3.close();
         } catch (IOException var45) {
            SendMessage.warnMessage(var1, "Failed to create backup of current shop layout.");
            var45.printStackTrace();
            return;
         }

         try {
            Files.copy(Paths.get(this.file.getPath(), "config.yml"), ConfigManager.getConfig().getFilePath(), StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException var44) {
            var44.printStackTrace();
         }

         YamlConfiguration var51 = this.plugin.loadConfiguration(new File(this.plugin.getDataFolder() + "/config.yml/"), this.fileName);
         var51.set("config-version", null);
         ConfigUtil.save(var51, new File(this.plugin.getDataFolder() + "/config.yml/"));
         File var58 = new File(this.plugin.getDataFolder() + File.separator + "sections");
         if (var58.exists() && var58.isDirectory()) {
            for (File var7 : var58.listFiles()) {
               try {
                  Files.delete(var7.toPath());
               } catch (IOException var43) {
               }
            }
         }

         File var66 = new File(this.file.getPath(), "sections");
         if (var66.exists() && var66.isDirectory() && var66.listFiles().length != 0) {
            for (File var8 : var66.listFiles()) {
               try {
                  Files.copy(var8.toPath(), Paths.get(this.plugin.getDataFolder() + "/sections/" + var8.getName()), StandardCopyOption.REPLACE_EXISTING);
               } catch (IOException var42) {
                  SendMessage.warnMessage("Error while importing 'sections/" + var8.getName() + "' from OnyxShop, skipping...");
                  var42.printStackTrace();
               }
            }
         }

         File var74 = new File(this.plugin.getDataFolder() + File.separator + "shops");
         if (var74.exists() && var74.isDirectory()) {
            for (File var9 : var74.listFiles()) {
               try {
                  Files.delete(var9.toPath());
               } catch (IOException var41) {
               }
            }
         }

         File var83 = new File(this.file.getPath(), "shops");
         if (var83.exists() && var83.isDirectory() && var83.listFiles().length != 0) {
            for (File var10 : var83.listFiles()) {
               try {
                  Files.copy(var10.toPath(), Paths.get(this.plugin.getDataFolder() + "/shops/" + var10.getName()), StandardCopyOption.REPLACE_EXISTING);
               } catch (IOException var40) {
                  SendMessage.warnMessage("Error while importing 'shops/" + var10.getName() + "' from OnyxShop, skipping...");
                  var40.printStackTrace();
               }
            }
         }

         File var93 = new File(this.file.getPath(), "stands.json");
         if (var93.exists()) {
            try {
               Files.copy(
                  Paths.get(this.file.getPath(), "stands.json"),
                  Paths.get(this.plugin.getDataFolder().getPath(), "stands.json"),
                  StandardCopyOption.REPLACE_EXISTING
               );
            } catch (IOException var39) {
               var39.printStackTrace();
            }
         }

         SendMessage.sendMessage(
            var1, ChatColor.GREEN + "Import complete, use command " + ChatColor.WHITE + "/sreload" + ChatColor.GREEN + " to reload the changes."
         );
      } else if (this.pluginName.equalsIgnoreCase("essentials")) {
         this.ess = (Essentials)Bukkit.getServer().getPluginManager().getPlugin("Essentials");
         if (this.ess == null || !this.ess.getItemDb().isReady()) {
            SendMessage.sendMessage(var1, ChatColor.RED + "Could not load essentials, please make sure EssentialsX is installed with the latest version.");
            return;
         }

         SendMessage.infoMessage(var1, ChatColor.GREEN + "Creating backup of current shop layout...");

         try {
            FileOutputStream var52 = new FileOutputStream(this.createZipFile() + ".zip");
            ZipOutputStream var59 = new ZipOutputStream(var52);
            this.zipLayout(var59);
            var59.close();
            var52.close();
         } catch (IOException var38) {
            SendMessage.warnMessage(var1, "Failed to create backup of current shop layout.");
            var38.printStackTrace();
            return;
         }

         String var53 = this.fileName;
         switch (var53) {
            case "worth.yml":
               YamlConfiguration var67 = this.plugin.loadConfiguration(this.file, this.fileName);
               HashMap var75 = new HashMap();

               for (String var94 : var67.getConfigurationSection("worth").getKeys(false)) {
                  if (var67.getDouble("worth." + var94, -5.0) == -5.0) {
                     for (String var107 : var67.getConfigurationSection("worth." + var94).getKeys(false)) {
                        if (var107.equals("*")) {
                           this.getMaterials(var67, var94).forEach(var3x -> var75.put(var3x, var67.getDouble("worth." + var94 + ".*")));
                        } else {
                           XMaterial var111 = this.getMaterial(var67, var94, var107);
                           if (var111 != null) {
                              var75.put(var111, var67.getDouble("worth." + var94 + "." + var107));
                           }
                        }
                     }
                  } else {
                     XMaterial var101 = this.getMaterial(var94);
                     if (var101 != null) {
                        var75.put(var101, var67.getDouble("worth." + var94));
                     }
                  }
               }

               int var85 = 0;
               HashSet<String> var95 = new HashSet<>();

               for (ShopSection var108 : this.plugin.getSections().values()) {
                  if (var108.getType() != ShopType.ROTATING) {
                     for (ShopItem var11 : var108.getShopItems()) {
                        if (!var11.hasItemError() && !var11.isDisplayItem() && var75.containsKey(XMaterial.matchXMaterial(var11.getItemToGive()))) {
                           ConfigManager.getShop(var108.getSection())
                              .set(
                                 "pages." + var11.itemLoc + ".buy",
                                 -ConfigManager.getShop(var108.getSection()).getDouble("pages." + var11.itemLoc + ".buy", 1.0)
                              );
                           ConfigManager.getShop(var108.getSection())
                              .set("pages." + var11.itemLoc + ".sell", var75.get(XMaterial.matchXMaterial(var11.getItemToGive())));
                           SendMessage.infoMessage(
                              "Imported item '"
                                 + XMaterial.matchXMaterial(var11.getItemToGive()).name()
                                 + "' from worth.yml with a sell price of '"
                                 + var75.get(XMaterial.matchXMaterial(var11.getItemToGive()))
                                 + "'"
                           );
                           var95.add(var108.getSection());
                           var85++;
                        }
                     }
                  }
               }

               var95.forEach(ConfigManager::saveShop);
               SendMessage.sendMessage(var1, ChatColor.GREEN + "Import success, " + var85 + " items where imported. See the console for more details.");
               break;
            default:
               SendMessage.sendMessage(var1, Lang.FILE_NOT_FOUND.get());
         }

         SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
      } else if (this.pluginName.equalsIgnoreCase("guishop")) {
         if (this.CACHE.isEmpty()) {
            YamlConfiguration var56 = this.plugin.loadConfiguration(this.file, this.fileName);
            int var64 = 0;
            int var71 = 0;
            int var79 = 0;
            HashMap var88 = new HashMap();

            for (String var104 : var56.getKeys(false)) {
               HashMap var109 = new HashMap();

               for (String var115 : var56.getConfigurationSection(var104 + ".pages").getKeys(false)) {
                  for (String var13 : var56.getConfigurationSection(var104 + "." + var115 + ".items").getKeys(false)) {
                     ConfigurationSection var14 = var56.getConfigurationSection(var104 + "." + var115 + ".items." + var13);

                     try {
                        var109.put("pages." + var115 + ".items." + var13, this.importGUIShopItem(var14));
                        var71++;
                     } catch (Exception var37) {
                        var79++;
                        SendMessage.warnMessage("A error occurred while loading item '" + var14.getCurrentPath() + "' from GUIShop, skipping...");
                        var37.printStackTrace();
                     }
                  }
               }

               if (!var109.isEmpty()) {
                  SendMessage.infoMessage("Successfully loaded " + var109.size() + " items from shop section " + var104);
                  var88.put(var104, var109);
                  var64++;
               } else {
                  SendMessage.infoMessage("Couldn't find any item inside section " + var104 + ", skipping...");
               }
            }

            if (var79 == 0) {
               SendMessage.sendMessage(var1, ChatColor.GREEN + "Completed, " + var71 + " item(s) where loaded from " + var64 + " sections.");
            } else {
               SendMessage.sendMessage(
                  var1,
                  ChatColor.GREEN
                     + "Successfully loaded "
                     + var71
                     + " item(s) from "
                     + var64
                     + " sections.\n"
                     + ChatColor.RED
                     + var79
                     + " item(s) failed to load, see the console for more info."
               );
            }

            SendMessage.sendMessage(
               var1,
               ChatColor.GREEN
                  + "You have 60 seconds to complete the import using "
                  + ChatColor.WHITE
                  + "/editshop import guishop"
                  + ChatColor.GREEN
                  + " again which will replace the existing shops.yml."
            );
            this.emptyCache();
         } else {
            SendMessage.infoMessage(var1, ChatColor.GREEN + "Creating backup of current shop layout...");

            try {
               FileOutputStream var54 = new FileOutputStream(this.createZipFile() + ".zip");
               ZipOutputStream var61 = new ZipOutputStream(var54);
               this.zipLayout(var61);
               var61.close();
               var54.close();
            } catch (IOException var36) {
               SendMessage.warnMessage(var1, "Failed to create backup of current shop layout.");
               var36.printStackTrace();
               return;
            }

            File var55 = new File(this.plugin.getDataFolder() + File.separator + "sections");
            if (var55.exists() && var55.isDirectory()) {
               for (File var86 : var55.listFiles()) {
                  try {
                     Files.delete(var86.toPath());
                  } catch (IOException var35) {
                  }
               }
            }

            File var63 = new File(this.plugin.getDataFolder() + File.separator + "shops");
            if (var63.exists() && var63.isDirectory()) {
               for (File var96 : var63.listFiles()) {
                  try {
                     Files.delete(var96.toPath());
                  } catch (IOException var34) {
                  }
               }
            }

            for (String var78 : this.CACHE.keySet()) {
               this.plugin.getConfigManager().createSectionConfig(var78);
            }
         }

         SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
      } else if (this.pluginName.equalsIgnoreCase("shopguiplus")) {
         long var57 = System.currentTimeMillis();
         Import.ShopGUIPlusData var72 = new Import.ShopGUIPlusData();
         int var80 = 0;
         int var89 = 0;
         int var98 = 0;
         int var105 = 0;
         int var110 = 0;
         int var114 = 0;
         int var116 = 0;
         int var117 = 0;
         SendMessage.sendMessage(
            var1,
            "§cPlease note that this command might not import every setting correctly as its supposed to work for ShopGUI+(most items will be imported), so we recommend going over the config.yml of OnyxShop manually after importing has completed."
         );
         SendMessage.sendMessage(var1, "§aStarted importing of ShopGUI+, this might take some time depending on how many shops and items there are...");
         File var118 = new File(this.file.getPath(), "/config.yml");
         HashMap<String, Map<String, Object>> var119 = new HashMap<>();
         if (var118.exists()) {
            YamlConfiguration var15 = this.plugin.loadConfiguration(var118, this.fileName);
            if (var15.get("specialElements") != null) {
               for (String var17 : var15.getConfigurationSection("specialElements").getKeys(false)) {
                  try {
                     Map var18 = this.importShopGUIPlusSpecialItem(var15.getConfigurationSection("specialElements." + var17));
                     if (!var18.isEmpty()) {
                        var72.addSpecial(var17, var18);
                        var114++;
                     }
                  } catch (Exception var50) {
                     if (var50 instanceof UnsupportedItemFlagException) {
                        var110++;
                     } else {
                        var116++;
                     }

                     SendMessage.warnMessage(var50.getMessage());
                     var50.printStackTrace();
                     SendMessage.warnMessage("Item at location 'specialElements." + var17 + "'");
                  }
               }
            }

            if (var15.get("shopMenuItems") != null) {
               for (String var127 : var15.getConfigurationSection("shopMenuItems").getKeys(false)) {
                  try {
                     Map var133 = this.importShopGUIPlusSectionItem(var72, var15.getConfigurationSection("shopMenuItems." + var127));
                     if (!var133.isEmpty()) {
                        if (!var133.containsKey("section")) {
                           var133.put("display-item", true);
                        }

                        var119.put(var133.containsKey("section") ? (String)var133.remove("section") : var127, var133);
                        var80++;
                     }
                  } catch (Exception var49) {
                     if (var49 instanceof UnsupportedItemFlagException) {
                        var110++;
                     } else {
                        var116++;
                     }

                     SendMessage.warnMessage(var49.getMessage());
                     var49.printStackTrace();
                     SendMessage.warnMessage("Item at location 'shopMenuItems." + var127 + "'");
                  }
               }
            }

            if (var15.get("shopMenuSize") != null) {
               try {
                  var72.addConfigData("main-menu.gui-rows", (int)Math.floor(Integer.parseInt(var15.getString("shopMenuSize")) / 9.0));
                  var114++;
               } catch (NumberFormatException var33) {
                  SendMessage.errorMessage("Failed to import main shop size for '" + var15.getString("shopMenuSize") + "', using 54 as default...");
                  var72.addConfigData("main-menu.gui-rows", 6);
               }
            }

            if (var15.get("shopMenuFillItem") != null) {
               try {
                  Map var123 = this.importShopGUIPlusFillItem(var15.getConfigurationSection("shopMenuFillItem"));
                  if (!var123.isEmpty()) {
                     var72.addConfigData("main-menu.fill-item", var123);
                     var114++;
                  }
               } catch (Exception var32) {
                  var116++;
                  SendMessage.warnMessage(var32.getMessage());
                  var32.printStackTrace();
                  SendMessage.warnMessage("Item at location 'shopMenuFillItem'");
               }
            }

            HashMap<Integer, Object> var124 = new HashMap<>();

            for (String var134 : var72.getSpecials().keySet()) {
               if (var124.containsKey(9)) {
                  if (var124.containsKey(1)) {
                     if (var124.containsKey(8)) {
                        if (var124.containsKey(2)) {
                           if (var124.containsKey(7)) {
                              if (var124.containsKey(3)) {
                                 if (var124.containsKey(6)) {
                                    if (var124.containsKey(4)) {
                                       if (var124.containsKey(5)) {
                                          SendMessage.errorMessage(
                                             "Cannot store special item inside main menu navigation bar. Reason: Navigation bar already contains 9 items."
                                          );
                                       } else {
                                          var124.put(5, var72.getSpecial(var134));
                                       }
                                    } else {
                                       var124.put(4, var72.getSpecial(var134));
                                    }
                                 } else {
                                    var124.put(6, var72.getSpecial(var134));
                                 }
                              } else {
                                 var124.put(3, var72.getSpecial(var134));
                              }
                           } else {
                              var124.put(7, var72.getSpecial(var134));
                           }
                        } else {
                           var124.put(2, var72.getSpecial(var134));
                        }
                     } else {
                        var124.put(8, var72.getSpecial(var134));
                     }
                  } else {
                     var124.put(1, var72.getSpecial(var134));
                  }
               } else {
                  var124.put(9, var72.getSpecial(var134));
               }
            }

            if (!var124.isEmpty()) {
               for (int var129 = 1; var129 < 10; var129++) {
                  if (!var124.containsKey(var129)) {
                     var124.put(
                        var129,
                        var72.getConfig().containsKey("main-menu.fill-item")
                           ? var72.getConfig().get("main-menu.fill-item")
                           : Collections.singletonMap("material", "air")
                     );
                  }
               }

               for (Integer var135 : var124.keySet()) {
                  var72.addConfigData("main-menu-nav-bar.items." + var135, var124.get(var135));
               }
            }

            HashMap<Integer, Object> var131 = new HashMap<>();

            for (String var19 : var72.getSpecials().keySet()) {
               if (var131.containsKey(9)) {
                  if (var131.containsKey(1)) {
                     if (var131.containsKey(8)) {
                        if (var131.containsKey(2)) {
                           if (var131.containsKey(7)) {
                              if (var131.containsKey(3)) {
                                 if (var131.containsKey(6)) {
                                    if (var131.containsKey(4)) {
                                       if (var131.containsKey(5)) {
                                          SendMessage.errorMessage(
                                             "Cannot store special item inside transaction navigation bar. Reason: Navigation bar already contains 9 items."
                                          );
                                       } else {
                                          var131.put(5, var72.getSpecial(var19));
                                       }
                                    } else {
                                       var131.put(4, var72.getSpecial(var19));
                                    }
                                 } else {
                                    var131.put(6, var72.getSpecial(var19));
                                 }
                              } else {
                                 var131.put(3, var72.getSpecial(var19));
                              }
                           } else {
                              var131.put(7, var72.getSpecial(var19));
                           }
                        } else {
                           var131.put(2, var72.getSpecial(var19));
                        }
                     } else {
                        var131.put(8, var72.getSpecial(var19));
                     }
                  } else {
                     var131.put(1, var72.getSpecial(var19));
                  }
               } else {
                  var131.put(9, var72.getSpecial(var19));
               }
            }

            if (!var131.isEmpty()) {
               for (int var137 = 1; var137 < 10; var137++) {
                  if (!var131.containsKey(var137)) {
                     var131.put(var137, Collections.singletonMap("material", "air"));
                  }
               }

               for (Integer var143 : var131.keySet()) {
                  var72.addConfigData("transaction-screens-nav-bar.items." + var143, var131.get(var143));
               }
            }

            if (var15.get("buttons") != null) {
               HashMap var139 = new HashMap();

               for (String var20 : var15.getConfigurationSection("buttons").getKeys(false)) {
                  if (var15.get("buttons." + var20 + ".slot") != null) {
                     try {
                        Map var21 = this.importShopGUIPlusSpecialItem(var15.getConfigurationSection("buttons." + var20));
                        if (!var21.isEmpty()) {
                           var21.put(
                              "action",
                              var20.equalsIgnoreCase("goBack")
                                 ? "BACK"
                                 : (var20.equalsIgnoreCase("previousPage") ? "PAGE_BACK" : (var20.equalsIgnoreCase("nextPage") ? "PAGE_NEXT" : "NONE"))
                           );
                           var139.put((int)Math.ceil(var15.getInt("buttons." + var20 + ".slot") % 9) + 1, var21);
                           var114++;
                        }
                     } catch (Exception var31) {
                        var116++;
                        SendMessage.warnMessage(var31.getMessage());
                        var31.printStackTrace();
                        SendMessage.warnMessage("Item at location 'buttons." + var20 + "'");
                     }
                  }
               }

               for (String var148 : var72.getSpecials().keySet()) {
                  if (var139.containsKey(9)) {
                     if (var139.containsKey(1)) {
                        if (var139.containsKey(8)) {
                           if (var139.containsKey(2)) {
                              if (var139.containsKey(7)) {
                                 if (var139.containsKey(3)) {
                                    if (var139.containsKey(6)) {
                                       if (var139.containsKey(4)) {
                                          if (var139.containsKey(5)) {
                                             SendMessage.errorMessage(
                                                "Cannot store special item inside shops navigation bar. Reason: Navigation bar already contains 9 items."
                                             );
                                          } else {
                                             var139.put(5, var72.getSpecial(var148));
                                          }
                                       } else {
                                          var139.put(4, var72.getSpecial(var148));
                                       }
                                    } else {
                                       var139.put(6, var72.getSpecial(var148));
                                    }
                                 } else {
                                    var139.put(3, var72.getSpecial(var148));
                                 }
                              } else {
                                 var139.put(7, var72.getSpecial(var148));
                              }
                           } else {
                              var139.put(2, var72.getSpecial(var148));
                           }
                        } else {
                           var139.put(8, var72.getSpecial(var148));
                        }
                     } else {
                        var139.put(1, var72.getSpecial(var148));
                     }
                  } else {
                     var139.put(9, var72.getSpecial(var148));
                  }
               }

               if (!var139.isEmpty()) {
                  for (int var146 = 1; var146 < 10; var146++) {
                     if (!var139.containsKey(var146)) {
                        var139.put(
                           var146,
                           var72.getConfig().containsKey("main-menu.fill-item")
                              ? var72.getConfig().get("main-menu.fill-item")
                              : Collections.singletonMap("material", "air")
                        );
                     }
                  }

                  var72.addConfigData("shops-nav-bar.items", var139);
               }
            }

            if (var15.get("amountSelectionGUI") != null) {
               Import.TransactionMenuData var140 = this.importShopGUIPlusTransScreen(var15.getConfigurationSection("amountSelectionGUI"));
               var72.addConfigData("sell-screen", var140.data);
               var72.addConfigData("buy-screen", var140.data);
               var114 += var140.itemsSuccess;
               var116 += var140.itemsFailed;
            }

            if (var15.get("amountSelectionGUIBulkBuy") != null) {
               Import.TransactionMenuData var141 = this.importShopGUIPlusTransScreen(var15.getConfigurationSection("amountSelectionGUIBulkBuy"));
               var72.addConfigData("buy-stacks-screen", var141.data);
               var114 += var141.itemsSuccess;
               var116 += var141.itemsFailed;
            }
         }

         SendMessage.sendMessage(var1, "§aCompleted loading main menu items, navigation bar items and transaction menu items, loading shops and items...");

         label674:
         for (File var142 : new File(this.file.getPath(), "/shops/").listFiles()) {
            try {
               SendMessage.infoMessage("Started importing of shop '" + var142.getName() + "'");
               YamlConfiguration var147 = this.plugin.loadConfiguration(var142, var142.getName());
               Iterator var149 = var147.getKeys(false).iterator();

               HashMap<String, Object> var22;
               boolean var23;
               Map var24;
               String var150;
               while (true) {
                  if (!var149.hasNext()) {
                     continue label674;
                  }

                  var150 = (String)var149.next();
                  var22 = new HashMap<>();
                  if (!var119.containsKey(var150)) {
                     throw new NullPointerException("DISABLED");
                  }

                  var23 = false;
                  var24 = (Map)var119.get(var150);
                  if (var147.get(var150 + ".fillItem") == null) {
                     break;
                  }

                  try {
                     Map var25 = this.importShopGUIPlusFillItem(var147.getConfigurationSection(var150 + ".fillItem"));
                     if (!var25.isEmpty()) {
                        var24.put("fill-item", var25);
                        var23 = true;
                        var114++;
                        break;
                     }
                  } catch (Exception var47) {
                     var116++;
                     SendMessage.warnMessage(var47.getMessage());
                     var47.printStackTrace();
                     SendMessage.warnMessage("Item from shop file: '" + var142.getName() + "' at location '" + var150 + ".fillItem'");
                     break;
                  }
               }

               if (var147.getString(var150 + ".name") != null) {
                  var24.put("title", var147.getString(var150 + ".name"));
                  var23 = true;
               }

               if (var147.getString(var150 + ".economy") != null) {
                  String var155 = var147.getString(var150 + ".economy");
                  if (!var155.equalsIgnoreCase("MYSQL_TOKENS") && !var155.equalsIgnoreCase("TOKEN_MANAGER") && !var155.equalsIgnoreCase("CUSTOM")) {
                     var24.put("economy", var155.toUpperCase(Locale.ENGLISH));
                  } else {
                     var24.put("enable", false);
                     var117++;
                  }

                  var23 = true;
               }

               if (var24.containsKey("fill-item")) {
                  Map var156 = (Map)var72.getConfig().get("shops-nav-bar.items");
                  HashMap var26 = new HashMap();
                  HashMap var27 = new HashMap();
                  String var28 = var72.getConfig().containsKey("main-menu.fill-item")
                     ? (String)((Map)var72.getConfig().get("main-menu.fill-item")).get("material")
                     : "air";

                  for (int var29 = 1; var29 < 10; var29++) {
                     if (!var22.containsKey(var29)
                        && var156.containsKey(var29)
                        && !((String)((Map)var156.get(var29)).get("material")).equalsIgnoreCase(var28)
                        && !((String)((Map)var156.get(var29)).get("material")).equalsIgnoreCase("air")) {
                        var27.put(var29, var156.get(var29));
                     }
                  }

                  if (!var27.isEmpty()) {
                     var26.put("mode", "SELF");
                     var26.put("fill-item", var24.get("fill-item"));
                     var26.put("items", var27);
                     var24.put("nav-bar", var26);
                  }
               }

               if (var23) {
                  var119.put(var150, var24);
               }

               for (String var153 : var147.getConfigurationSection(var150 + ".items").getKeys(false)) {
                  try {
                     Map var157 = this.importShopGUIPlusItem(var72, var147.getConfigurationSection(var150 + ".items." + var153));
                     if (!var157.isEmpty()) {
                        if (var147.getBoolean(var150 + ".enablePerItemPermissions")) {
                           var157.put("permission", var153);
                        }

                        var22.put(var153, var157);
                        var98++;
                     }
                  } catch (Exception var46) {
                     if (var46 instanceof UnsupportedItemFlagException) {
                        var110++;
                     } else {
                        var105++;
                     }

                     SendMessage.warnMessage(var46.getMessage());
                     var46.printStackTrace();
                     SendMessage.warnMessage("Item from shop file: '" + var142.getName() + "' at location '" + var150 + ".items." + var153 + "'");
                  }
               }

               if (var22.isEmpty()) {
                  SendMessage.infoMessage("§cCouldn't find any item inside section " + var150 + ", skipping...");
               } else {
                  LinkedHashMap var152 = new LinkedHashMap();
                  int var154 = 6;
                  if (var147.contains(var150 + ".size")) {
                     var154 = (int)Math.ceil(var147.getInt(var150 + ".size", 54) / 9.0);
                     if (var154 > 6) {
                        var154 = 6;
                     }
                  }

                  int var158 = 0;

                  for (String var161 : var22.keySet()) {
                     Map var163 = (Map)var22.get(var161);
                     if (var163.containsKey("page") && var158 < (Integer)var163.get("page")) {
                        var158 = (Integer)var163.get("page");
                     }
                  }

                  int var160 = (int)Math.ceil(var22.size() / (var154 * 9 - 9.0));
                  if (var160 < var158) {
                     var160 = var158;
                  }

                  for (int var162 = 1; var162 < var160 + 1; var162++) {
                     var152.put("pages.page" + var162 + ".gui-rows", var154);

                     for (String var165 : var22.keySet()) {
                        Map var30 = (Map)var22.get(var165);
                        if ((!var30.containsKey("page") || (Integer)var30.get("page") == var162) && (var30.containsKey("page") || var162 == 1)) {
                           var152.put("pages.page" + var162 + ".items." + var165, var22.get(var165));
                        }
                     }
                  }

                  var72.addShopsData(var150, var152);
                  var89++;
                  SendMessage.infoMessage("§aSuccessfully loaded " + var22.size() + " items across " + var160 + " pages from shop shop " + var150);
               }
            } catch (NullPointerException var48) {
               if (var119.containsKey(var142.getName())) {
                  ((Map)var119.get(var142.getName())).put("display-item", true);
               }
            }
         }

         for (String var126 : var119.keySet()) {
            var72.addSectionsData(var126, (Map<String, Object>)var119.get(var126));
         }

         SendMessage.sendMessage(
            var1,
            "§aCompleted loading data from ShopGUI+ in §f"
               + (System.currentTimeMillis() - var57)
               + "§ams, summary:\n§f <---------- §aSuccess§f  ---------->\n§f- §aConfig data imported successfully(Navigation bar items, transaction items, fill items, special items): §f"
               + var114
               + "\n§f- §aSection items imported successfully: §f"
               + var80
               + "\n§f- §aSections imported successfully: §f"
               + var89
               + "\n§f- §aShop items imported successfully: §f"
               + var98
               + "\n§f <---------- §cFailed§f  ---------->\n§f- §cConfig items failed to import: §f"
               + var116
               + "\n§f- §cShop items failed to import: §f"
               + var105
               + "\n§f <---------- §6Unsupported§f  ---------->\n§f- §6Section with unsupported economy type(These sections will be imported but will be disabled): §f"
               + var117
               + "\n§f- §6Shop items which aren't supported(You can add these items manually by '/onyx addhanditem' or using NBTData): §f"
               + var110
         );
         SendMessage.sendMessage(var1, "§aComplete the import using §f/onyx import shopguiplus all confirm §aonce you are satisfied with the results.");
         this.data = var72;
      } else {
         SendMessage.sendMessage(var1, Lang.PLUGIN_NOT_FOUND.get());
      }
   }

   private Import.TransactionMenuData importShopGUIPlusTransScreen(ConfigurationSection var1) {
      HashMap var2 = new HashMap();
      int var3 = 0;
      int var4 = 0;
      int var5 = 54;
      if (var1.get("size") != null) {
         try {
            var2.put("menu-size", var5 = (int)Math.floor(Integer.parseInt(var1.getString("size")) / 9.0));
         } catch (NumberFormatException var14) {
            SendMessage.errorMessage("Failed to import screen size for '" + var1.getCurrentPath() + "', using 54 as default...");
            var2.put("menu-size", var5 / 9);
         }
      }

      if (var1.get("itemSlot") != null) {
         HashMap var6 = new HashMap();
         var6.put("type", "SELECTED_ITEM");
         var6.put("slot", var1.getInt("itemSlot"));
         var2.put("items.selected-item", var6);
         var3++;
      }

      for (String var7 : var1.getConfigurationSection("buttons").getKeys(false)) {
         try {
            ConfigurationSection var8 = var1.getConfigurationSection("buttons." + var7);
            HashMap var9 = new HashMap();
            String var10 = "";
            if (var8.get("item.material") != null) {
               var10 = var8.getString("item.material");
            }

            if (var8.get("item.damage") != null) {
               var10 = var10 + ":" + var8.get("item.damage");
            }

            if (var10.isEmpty()) {
               throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
            }

            var9.put("material", var10);
            if (var8.get("item.name") != null) {
               var9.put("name", var8.getString("item.name"));
            }

            if (!var8.getStringList("item.lore").isEmpty()) {
               var9.put(
                  "lore",
                  var8.getStringList("item.lore")
                     .stream()
                     .map(var0 -> var0.replaceAll("%buy%", "%formattedPrice%").replaceAll("%sell%", "%formattedPrice%"))
                     .collect(Collectors.toList())
               );
            }

            if (var1.getCurrentPath().equalsIgnoreCase("amountSelectionGUIBulkBuy") && var8.get("value") != null) {
               var9.put("stack-size", var8.getInt("value"));
            }

            if (!var9.containsKey("stack-size") && var8.get("item.quantity") != null) {
               var9.put("stack-size", var8.getInt("item.quantity"));
            }

            if (var8.get("slot") != null) {
               int var11;
               try {
                  var11 = Integer.parseInt(var8.getString("slot"));
                  if (var11 > var5 * 9 - 9) {
                     var11 -= 9;
                  }
               } catch (NumberFormatException var13) {
                  var11 = var2.size();
               }

               var9.put("slot", var11);
            }

            HashMap var17 = new HashMap();
            if (!var8.getStringList("flags").isEmpty()) {
               var17.put("HideFlags", this.flagsToByte(var8.getStringList("flags"), var8.getCurrentPath()));
            }

            if (!var17.isEmpty()) {
               var9.put("NBTData", new Gson().toJson(var17).toString());
            }

            if (var1.getCurrentPath().equalsIgnoreCase("amountSelectionGUI")) {
               String var12 = "NONE";
               if (var7.equalsIgnoreCase("set1")) {
                  var12 = "REMOVE32";
               } else if (var7.equalsIgnoreCase("remove10")) {
                  var12 = "REMOVE16";
               } else if (var7.equalsIgnoreCase("remove1")) {
                  var12 = "REMOVE1";
               } else if (var7.equalsIgnoreCase("add1")) {
                  var12 = "ADD1";
               } else if (var7.equalsIgnoreCase("add10") || var7.equalsIgnoreCase("set16")) {
                  var12 = "ADD16";
               } else if (var7.equalsIgnoreCase("set64")) {
                  var12 = "ADD32";
               } else if (var7.equalsIgnoreCase("confirm")) {
                  var9.put("type", "CONFIRM");
                  var12 = "CONFIRM_TRANSACTION";
               } else if (var7.equalsIgnoreCase("sellAll")) {
                  var12 = "SELL_ALL";
               } else if (var7.equalsIgnoreCase("buyMore")) {
                  var12 = "OPEN_BUY_STACKS";
               } else if (var7.equalsIgnoreCase("cancel")) {
                  var12 = "BACK";
               } else if (var7.equalsIgnoreCase("sellMore")) {
                  continue;
               }

               var9.put("action", var12);
            } else if (var1.getCurrentPath().equalsIgnoreCase("amountSelectionGUIBulkBuy")) {
               if (var7.equalsIgnoreCase("cancel")) {
                  var9.put("action", "BACK");
               } else {
                  var9.put("action", "INSTA_BUY");
               }
            }

            var2.put("items." + var7, var9);
            var3++;
         } catch (Exception var15) {
            SendMessage.errorMessage(
               "Failed to import transaction screen button for '"
                  + var1.getCurrentPath()
                  + ".buttons."
                  + var7
                  + "' inside the config.yml, see reason below, skipping item..."
            );
            var15.printStackTrace();
            var4++;
         }
      }

      return new Import.TransactionMenuData(var2, var3, var4);
   }

   private Map<String, Object> importShopGUIPlusFillItem(ConfigurationSection var1) throws ItemImportException {
      HashMap var2 = new HashMap();
      String var3 = "";
      if (var1.get("material") != null) {
         var3 = var1.getString("material");
      }

      if (var1.get("damage") != null) {
         var3 = var3 + ":" + var1.get("damage");
      }

      if (!var3.isEmpty()) {
         var2.put("material", var3);
         if (var1.get("name") != null) {
            var2.put("name", var1.getString("name"));
         }

         if (!var1.getStringList("lore").isEmpty()) {
            var2.put("lore", var1.getStringList("lore"));
         }

         if (var1.get("quantity") != null) {
            var2.put("stack-size", var1.getInt("quantity"));
         }

         if (var1.get("amount") != null) {
            var2.put("stack-size", var1.getInt("amount"));
         }

         HashMap var4 = new HashMap();
         if (!var1.getStringList("flags").isEmpty()) {
            var4.put("HideFlags", this.flagsToByte(var1.getStringList("flags"), var1.getCurrentPath()));
         }

         if (!var4.isEmpty()) {
            var2.put("NBTData", new Gson().toJson(var4).toString());
         }

         return var2;
      } else {
         throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
      }
   }

   private Map<String, Object> importShopGUIPlusSectionItem(Import.ShopGUIPlusData var1, ConfigurationSection var2) throws ItemImportException {
      HashMap var3 = new HashMap();
      if (var2.get("type") != null && var2.getString("type").equalsIgnoreCase("special")) {
         if (var2.getString("type").equalsIgnoreCase("special")) {
         }
      } else {
         String var4 = "";
         if (var2.get("item.material") != null) {
            var4 = var2.getString("item.material");
         }

         if (var2.get("item.damage") != null) {
            var4 = var4 + ":" + var2.get("item.damage");
         }

         if (var4.isEmpty()) {
            throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
         }

         var3.put("item.material", var4);
         if (var2.get("item.name") != null) {
            var3.put("item.name", var2.getString("item.name"));
         }

         if (!var2.getStringList("item.lore").isEmpty()) {
            var3.put("item.lore", var2.getStringList("item.lore"));
         }

         if (var2.get("item.quantity") != null) {
            var3.put("item.stack-size", var2.getInt("item.quantity"));
         }

         if (var2.get("item.amount") != null) {
            var3.put("stack-size", var2.getInt("item.amount"));
         }

         if (var2.get("item.skin") != null) {
            var3.put("item.skull-texture", var2.getString("item.skin"));
         }

         if (var2.get("shop") != null) {
            if (var2.getString("shop").isEmpty()) {
               var3.put("section", "fill-" + var1.fill++);
               var3.put("display-item", true);
            } else {
               var3.put("section", var2.getString("shop"));
            }
         }

         if (var2.get("slot") != null) {
            var3.put("slot", var2.getInt("slot") + 1);
         }

         if (!var2.getStringList("item.flags").isEmpty()) {
            var3.put("item-flags", var2.getStringList("item.flags"));
         }
      }

      return var3;
   }

   private Map<String, Object> importShopGUIPlusSpecialItem(ConfigurationSection var1) throws ItemImportException {
      HashMap var2 = new HashMap();
      String var3 = "";
      if (var1.get("item.material") != null) {
         var3 = var1.getString("item.material");
      }

      if (var1.get("item.damage") != null) {
         var3 = var3 + ":" + var1.get("item.damage");
      }

      if (!var3.isEmpty()) {
         var2.put("material", var3);
         if (var1.get("item.name") != null) {
            var2.put("name", var1.getString("item.name"));
         }

         if (!var1.getStringList("item.lore").isEmpty()) {
            List var4 = var1.getStringList("item.lore")
               .stream()
               .map(var0 -> var0.contains("%balance%") ? var0.replace("%balance%", "%player_balance%").replace("$", "").replace("€", "") : var0)
               .collect(Collectors.toList());
            var2.put("lore", var4);
         }

         if (var1.get("item.quantity") != null) {
            var2.put("stack-size", var1.getInt("item.quantity"));
         }

         if (var1.get("item.amount") != null) {
            var2.put("stack-size", var1.getInt("item.amount"));
         }

         if (var1.get("item.skin") != null) {
            var2.put("skull-texture", var1.getString("item.skin"));
         }

         if (var1.get("item.skullOwner") != null) {
            var2.put("skullowner", var1.getString("item.skullOwner"));
         }

         if (!var1.getStringList("item.flags").isEmpty()) {
            var2.put("item-flags", var1.getStringList("item.flags"));
         }

         return var2;
      } else {
         throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
      }
   }

   private Map<String, Object> importShopGUIPlusItem(Import.ShopGUIPlusData var1, ConfigurationSection var2) throws UnsupportedItemFlagException , ItemImportException{
      HashMap var3 = new HashMap();
      if (var2.get("type") != null && var2.getString("type").equalsIgnoreCase("special")) {
         if (var2.getString("type").equalsIgnoreCase("special")) {
         }
      } else {
         String var4 = "";
         if (var2.get("item.material") != null) {
            var4 = var2.getString("item.material");
         }

         if (var2.get("item.damage") != null) {
            var4 = var4 + ":" + var2.get("item.damage");
         }

         if (var2.get("item.headDatabase") != null) {
            var4 = "hdb:" + var2.getString("item.headDatabase");
         }

         if (var2.get("item.nexo") != null) {
            var4 = "nexo:" + var2.getString("item.nexo");
         }

         if (var2.get("item.mmoItems.type") != null && var2.get("item.mmoItems.id") != null) {
            var4 = "MMOItem:" + var2.getString("item.mmoItems.type") + var2.getString("item.mmoItems.id");
         }

         if (var2.get("item.crackShot") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.crackShot', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (var2.get("item.oraxen") != null) {
            var4 = "oraxen:" + var2.getString("item.oraxen");
         }

         if (var2.get("item.customItems") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.customItems', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (var2.get("item.qualityArmory") != null) {
            throw new UnsupportedItemFlagException(
               "Unsupported item type found while importing item from ShopGUI+: 'item.customItems', please manually add this item using the '/onyx addhanditem' command, skipping item..."
            );
         }

         if (var2.get("item.brewery.recipe") != null) {
            String var5 = var2.getString("item.brewery.recipe");
            if (var2.get("item.brewery.quality") != null) {
               var5 = var5 + "." + var2.getInt("item.brewery.quality");
            }

            var4 = "brewery:" + var5;
         }

         if (var2.get("item.executableItems") != null) {
            var4 = "ei:" + var2.getString("item.executableItems");
         }

         if (var2.get("item.itemsAdder") != null) {
            var4 = "ia:" + var2.getString("item.itemsAdder");
         }

         if (var2.get("item.slimefun") != null) {
            var4 = "sf:" + var2.getString("item.slimefun");
         }

         if (var2.get("item.mythicMobs") != null) {
            var4 = "mythicmobs:" + var2.getString("item.mythicMobs");
         }

         if (var4.isEmpty()) {
            throw new ItemImportException("Failed to find item material, every item requires one, skipping...");
         }

         var3.put("material", var4);
         if (var2.get("page") != null) {
            var3.put("page", var2.getInt("page"));
         }

         Object var17 = null;
         if (var2.get("slot") != null) {
            var17 = var2.getInt("slot") + 1;
         }

         if (var2.get("slots") != null) {
            var17 = var2.getIntegerList("slots").stream().map(var0 -> String.valueOf(var0 + 1)).collect(Collectors.joining(","));
         }

         if (var17 != null) {
            var3.put("slot", var17);
         }

         if (var2.get("shop") != null) {
            var3.put("section", var2.getString("shop"));
         }

         if (var2.get("item.name") != null) {
            var3.put("name", var2.getString("item.name"));
         }

         if (var2.get("buyPrice") != null) {
            var3.put("buy", var2.getDouble("buyPrice"));
         }

         if (var2.get("sellPrice") != null) {
            var3.put("sell", var2.getDouble("sellPrice"));
         }

         if (!var2.getStringList("item.enchantments").isEmpty()) {
            var3.put("enchantments", var2.getStringList("item.enchantments"));
         }

         if (!var2.getStringList("item.lore").isEmpty()) {
            var3.put("lore", var2.getStringList("item.lore"));
         }

         if (var2.get("item.model") != null) {
            var3.put("CustomModelData", var2.getString("item.model"));
         }

         if (!var2.getStringList("item.flags").isEmpty()) {
            var3.put("item-flags", var2.getStringList("item.flags"));
         }

         if (!var2.getStringList("item.musicInstrument").isEmpty()) {
            var3.put("instrument", var2.getString("item.musicInstrument").replace("_GOAT_HORN", ""));
         }

         if (var2.get("item.mob") != null) {
            if (var4.startsWith("MONSTER_EGG")) {
               String var6 = null;

               try {
                  var6 = EntityType.fromId(Integer.parseInt(var2.getString("item.mob"))).name();
               } catch (NumberFormatException var14) {
                  var6 = EntityType.valueOf(var2.getString("item.mob").toUpperCase(Locale.ENGLISH)).name();
               } catch (NullPointerException var15) {
               }

               if (var6 == null || !XMaterial.matchXMaterial(var6 + "_SPAWN_EGG").isPresent()) {
                  throw new ItemImportException("Failed to import spawn egg data '" + var2.get("item.mob") + "' from ShopGUI+, skipping item...");
               }

               var3.put("material", XMaterial.matchXMaterial(var6 + "_SPAWN_EGG").get().name());
            } else {
               if (!var4.startsWith("SPAWNER") && !var4.startsWith("MOB_SPAWNER")) {
                  throw new ItemImportException("Failed to import mob type '" + var2.get("item.mob") + "' from ShopGUI+, skipping item...");
               }

               var3.put("spawnertype", var2.getString("item.mob"));
            }
         }

         HashMap var18 = new HashMap();
         if (var2.getConfigurationSection("item.potion") != null) {
            ConfigurationSection var7 = var2.getConfigurationSection("item.potion");
            if (var7.getString("type") != null) {
               try {
                  Optional<PotionTypes> var8 = PotionTypes.matchPotionDataLegacy(
                     PotionType.valueOf(var7.getString("type")),
                     var7.getInt("level") == 1 && var7.getBoolean("extended"),
                     var7.getInt("level") > 1 && !var7.getBoolean("extended")
                  );
                  var8.ifPresent(var1x -> var3.put("potiontypes", Collections.singletonList(var1x.name())));
                  var8.orElseThrow(() -> new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get()));
               } catch (ItemLoadException | IllegalArgumentException var16) {
               }

               if (!var3.containsKey("potiontypes")) {
                  var18.put(
                     "CustomPotionEffects",
                     Arrays.<String>asList(
                           "{Id:" + PotionEffectType.getByKey(NamespacedKey.minecraft(var7.getString("type"))).getId(),
                           "Amplifier:" + var7.getInt("level"),
                           "Duration:" + PotionTypes.getFromType(var7.getString("type")).get().getDuration()
                        )
                        + "}"
                  );
               }
            }

            if (var7.getString("color") != null) {
               var18.put("CustomPotionColor", var7.getString("color").toUpperCase(Locale.ENGLISH));
            }
         }

         if (var2.getBoolean("item.unbreakable")) {
            var18.put("Unbreakable", 1);
         }

         if (var2.get("item.nbt") != null) {
            for (String var23 : var2.getConfigurationSection("item.nbt").getKeys(false)) {
               if (var2.get("item.nbt." + var23 + ".type") != null && var2.get("item.nbt." + var23 + ".key") != null) {
                  if (var2.getString("item.nbt." + var23 + ".type").equalsIgnoreCase("COMPOUND")) {
                     HashMap var9 = new HashMap();
                     if (var2.get("item.nbt." + var23 + ".children") != null && var2.getString("item.nbt." + var23 + ".key") != null) {
                        for (String var11 : var2.getConfigurationSection("item.nbt." + var23 + ".children").getKeys(false)) {
                           if (var2.getString("item.nbt." + var23 + ".children." + var11 + ".key") != null
                              && var2.get("item.nbt." + var23 + ".children." + var11 + ".value") != null) {
                              var9.put(
                                 var2.getString("item.nbt." + var23 + ".children." + var11 + ".key"),
                                 var2.get("item.nbt." + var23 + ".children." + var11 + ".value")
                              );
                           }
                        }

                        var18.put(var2.getString("item.nbt." + var23 + ".key"), new Gson().toJson(var9).toString());
                     }
                  }
               } else if (var2.getString("item.nbt." + var23 + ".key") != null && var2.get("item.nbt." + var23 + ".value") != null) {
                  var18.put(var2.getString("item.nbt." + var23 + ".key"), var2.get("item.nbt." + var23 + ".value"));
               }
            }
         }

         if (!var18.isEmpty()) {
            var3.put("NBTData", new Gson().toJson(var18).toString());
         }

         if (!var18.isEmpty()) {
            System.out.println(var3.get("NBTData"));
         }

         if (var2.get("item.quantity") != null) {
            var3.put("stack-size", var2.getInt("item.quantity"));
         }

         if (var2.get("item.amount") != null) {
            var3.put("stack-size", var2.getInt("item.amount"));
         }

         if (var2.getBoolean("item.glow")) {
            var3.put("enchantment-glint", true);
         }

         if (var2.getString("item.color") != null
            && (
               var4.equalsIgnoreCase("LEATHER_HELMET")
                  || var4.equalsIgnoreCase("LEATHER_BOOTS")
                  || var4.equalsIgnoreCase("LEATHER_CHESTPLATE")
                  || var4.equalsIgnoreCase("LEATHER_LEGGINGS")
            )) {
            try {
               var3.put("armorcolor", this.getColor(var2.getString("item.color")).asARGB());
            } catch (IllegalAccessException | NoSuchFieldException var13) {
               try {
                  List var24 = Arrays.stream(var2.getString("item.color").replace(" ", "").split(",")).map(Integer::parseInt).collect(Collectors.toList());
                  var3.put("armorcolor", Color.fromRGB((Integer)var24.get(0), (Integer)var24.get(1), (Integer)var24.get(2)).asARGB());
               } catch (NumberFormatException var12) {
                  throw new ItemImportException("Failed to import armor color '" + var2.getString("item.color") + "' from ShopGUI+, skipping item...");
               }
            }
         }

         if (var2.get("item.fireworkPower") != null) {
            var3.put("duration", var2.get("item.fireworkPower"));
         }

         if (var2.get("item.fireworkEffects") != null) {
            Iterator var20 = var2.getConfigurationSection("item.fireworkEffects").getKeys(false).iterator();
            if (var20.hasNext()) {
               String var25 = (String)var20.next();
               if (var2.get("item.fireworkEffects." + var25 + ".type") != null) {
                  var3.put("shape", var2.getString("item.fireworkEffects." + var25 + ".type"));
               }

               if (var2.get("item.fireworkEffects." + var25 + ".flicker") != null) {
                  var3.put("flicker", var2.getBoolean("item.fireworkEffects." + var25 + ".flicker"));
               }

               if (var2.get("item.fireworkEffects." + var25 + ".trail") != null) {
                  var3.put("trail", var2.getBoolean("item.fireworkEffects." + var25 + ".trail"));
               }

               if (!var2.getStringList("item.fireworkEffects." + var25 + ".colors").isEmpty()) {
                  var3.put("colors", var2.getStringList("item.fireworkEffects." + var25 + ".colors"));
               }

               if (!var2.getStringList("item.fireworkEffects." + var25 + ".fadeColors").isEmpty()) {
                  var3.put("colors", var2.getStringList("item.fireworkEffects." + var25 + ".fadeColors"));
               }
            }
         }

         if (var2.get("item.skullOwner") != null) {
            var3.put("skullowner", var2.getString("item.skullOwner"));
         }

         if (var2.get("item.skin") != null) {
            var3.put("skull-texture", var2.getString("item.skin"));
         }

         if (var2.get("placeholder") != null) {
            String var21 = "";
            if (var2.get("item.material") != null) {
               var21 = var2.getString("item.material");
            }

            if (var2.get("item.damage") != null) {
               var21 = var4 + ":" + var2.get("item.damage");
            }

            if (!var21.isEmpty()) {
               var3.put("display-item.material", var21);
            }

            if (var2.get("placeholder.name") != null) {
               var3.put("display-item.name", var2.getString("placeholder.name"));
            }

            if (var2.get("placeholder.quantity") != null) {
               var3.put("display-item.stack-size", var2.getString("placeholder.stack-size"));
            }

            if (var2.get("placeholder.lore") != null) {
               var3.put("display-item.lore", var2.getStringList("placeholder.lore"));
            }
         }

         ArrayList var22 = new ArrayList();
         if (var2.get("permission") != null) {
            if (this.getPermCommand(var2.getString("permission")) == null) {
               throw new ItemImportException(
                  "Failed to import permission '"
                     + var2.getString("permission")
                     + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
               );
            }

            var22.add(this.getPermCommand(var2.getString("permission")));
         }

         if (!var2.getStringList("permissions").isEmpty()) {
            for (String var29 : var2.getStringList("permissions")) {
               if (this.getPermCommand(var29) == null) {
                  throw new ItemImportException(
                     "Failed to import permission '" + var29 + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                  );
               }

               var22.add(this.getPermCommand(var29));
            }
         }

         if (var2.get("permissions") != null) {
            for (String var30 : var2.getConfigurationSection("permissions").getKeys(false)) {
               if (var2.get("permissions." + var30 + ".permission") != null) {
                  if (var2.get("permissions." + var30 + ".world") != null) {
                     if (this.getPermCommand(var2.getString("permissions." + var30 + ".permission"), var2.getString("permissions." + var30 + ".world")) == null
                        )
                      {
                        throw new ItemImportException(
                           "Failed to import per world permission 'permissions."
                              + var30
                              + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                        );
                     }

                     var22.add(this.getPermCommand(var2.getString("permissions." + var30 + ".permission"), var2.getString("permissions." + var30 + ".world")));
                  } else {
                     if (this.getPermCommand(var30) == null) {
                        throw new ItemImportException(
                           "Failed to import permission '" + var30 + "' from ShopGUI+ because no supported permissions plugin could be found, skipping item..."
                        );
                     }

                     var22.add(this.getPermCommand(var30));
                  }
               }
            }
         }

         if (!var2.getStringList("commands").isEmpty()) {
            var22.addAll(
               var2.getStringList("commands")
                  .stream()
                  .map(var1x -> var2.getBoolean("runAsBuyer") ? "p:" : "" + var1x.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var2.getStringList("commandsOnBuy").isEmpty()) {
            var22.addAll(
               var2.getStringList("commandsOnBuy")
                  .stream()
                  .map(var0 -> "p:" + var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var2.getStringList("commandsOnBuyConsole").isEmpty()) {
            var22.addAll(
               var2.getStringList("commandsOnBuyConsole")
                  .stream()
                  .map(var0 -> var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var22.isEmpty()) {
            var3.put("buy-commands", var22);
         }

         ArrayList var28 = new ArrayList();
         if (!var2.getStringList("commandsOnSell").isEmpty()) {
            var28.addAll(
               var2.getStringList("commandsOnSell")
                  .stream()
                  .map(var0 -> "p:" + var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var2.getStringList("commandsOnSellConsole").isEmpty()) {
            var28.addAll(
               var2.getStringList("commandsOnSellConsole")
                  .stream()
                  .map(var0 -> var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var28.isEmpty()) {
            var3.put("sell-commands", var22);
         }

         ArrayList var31 = new ArrayList();
         if (!var2.getStringList("commandsOnClick").isEmpty()) {
            var31.addAll(
               var2.getStringList("commandsOnClick")
                  .stream()
                  .map(var0 -> "p:" + var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var2.getStringList("commandsOnClickConsole").isEmpty()) {
            var31.addAll(
               var2.getStringList("commandsOnClickConsole")
                  .stream()
                  .map(var0 -> var0.replace("%PLAYER%", "%player%").replace("%AMOUNT%", "%quantity%"))
                  .collect(Collectors.toList())
            );
         }

         if (!var31.isEmpty()) {
            var3.put("click-commands", var22);
         }

         if (var2.get("commandsLimit") != null) {
            var3.put("stock-limit-player", var2.getInt("commandsLimit"));
         }

         if (var2.getBoolean("runSingleCommand")) {
            var3.put("max-buy", -1);
         }
      }

      return var3;
   }

   private void completeImporting(Object var1) {
      String var2 = this.pluginName;
      byte var3 = -1;
      switch (var2.hashCode()) {
         case -217641569:
            if (var2.equals("shopguiplus")) {
               var3 = 0;
            }
         default:
            switch (var3) {
               case 0:
                  if (this.data == null) {
                     SendMessage.sendMessage(
                        var1,
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
                        Path var4 = this.createZipFile();
                        SendMessage.infoMessage(
                           var1, ChatColor.GREEN + "Creating backup of current shop layout, this can be found in " + var4.toFile().getAbsolutePath() + "..."
                        );
                        FileOutputStream var5 = new FileOutputStream(var4 + ".zip");
                        ZipOutputStream var6 = new ZipOutputStream(var5);
                        this.zipLayout(var6);
                        var6.close();
                        var5.close();
                     } catch (IOException var13) {
                        SendMessage.warnMessage(var1, "Failed to create backup of current shop layout.");
                        var13.printStackTrace();
                        return;
                     }

                     try {
                        if (new File(this.plugin.getDataFolder() + File.separator + "config.yml").exists()) {
                           Files.delete(ConfigManager.getConfig().getFilePath());
                        }

                        File var16 = new File(this.plugin.getDataFolder() + File.separator + "sections");
                        if (var16.exists() && var16.isDirectory()) {
                           for (File var8 : var16.listFiles()) {
                              try {
                                 Files.delete(var8.toPath());
                              } catch (IOException var12) {
                              }
                           }
                        }

                        File var21 = new File(this.plugin.getDataFolder() + File.separator + "shops");
                        if (var21.exists() && var21.isDirectory()) {
                           for (File var9 : var21.listFiles()) {
                              try {
                                 Files.delete(var9.toPath());
                              } catch (IOException var11) {
                              }
                           }
                        }
                     } catch (IOException var15) {
                        SendMessage.sendMessage(
                           var1,
                           ChatColor.RED
                              + "Failed to delete the existing configurations, you will need to delete the files manually(shops folder, sections folder, config.yml) and perform this command again."
                        );
                        var15.printStackTrace();
                        return;
                     }

                     SendMessage.infoMessage("Exporting cached data...");

                     try {
                        for (String var22 : this.data.getShops().keySet()) {
                           Config var27 = this.plugin.getConfigManager().createShopConfig(var22);

                           for (Entry var34 : this.data.getShops().get(var22).entrySet()) {
                              var27.set((String)var34.getKey(), var34.getValue());
                           }

                           var27.save(new File(this.plugin.getDataFolder() + "/shops/" + var22 + ".yml"));
                        }

                        for (String var23 : this.data.getSections().keySet()) {
                           Config var28 = this.plugin.getConfigManager().createSectionConfig(var23);

                           for (Entry var35 : this.data.getSections().get(var23).entrySet()) {
                              var28.set((String)var35.getKey(), var35.getValue());
                           }

                           var28.save(new File(this.plugin.getDataFolder() + "/sections/" + var23 + ".yml"));
                        }

                        YamlConfiguration var19 = new YamlConfiguration();

                        for (String var29 : this.data.getConfig().keySet()) {
                           var19.set(var29, this.data.getConfig().get(var29));
                        }

                        var19.save(new File(this.plugin.getDataFolder() + File.separator + "config.yml"));
                        this.plugin.getConfigManager().loadConfig();
                        SendMessage.sendMessage(var1, "§aImport success, the existing shop layout was replaced with the loaded data.");
                        if (this.data.isRestartRequired()) {
                           SendMessage.sendMessage(var1, "§aA server restart is required to load all the imported data.");
                        } else {
                           SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
                        }

                        return;
                     } catch (IllegalArgumentException | IOException var14) {
                        SendMessage.sendMessage(var1, "§cFailed to save configurations because an IOE error occurred, see console for error stacktrace.");
                        var14.printStackTrace();
                        return;
                     }
                  }
               default:
                  SendMessage.sendMessage(var1, Lang.PLUGIN_NOT_FOUND.get());
            }
      }
   }

   private void setSection(Config var1, Object var2, String var3) {
      if (var2 instanceof Map) {
         for (Entry<?, ?> var5 : ((Map<?, ?>)var2).entrySet()) {
            if (!(var5.getValue() instanceof MemorySection)) {
               var1.set(var3 + "." + var5.getKey(), var5.getValue());
            }
         }
      } else {
         var1.set(var3, var2);
      }
   }

   private Map<String, Object> importGUIShopItem(ConfigurationSection var1) throws ItemLoadException {
      HashMap var2 = new HashMap();
      if (var1.get("id") != null) {
         var2.put("material", var1.getString("id"));
      }

      if (var1.get("target-shop") != null) {
         var2.put("section", var1.getString("target-shop"));
      }

      if (var1.get("name") != null) {
         var2.put("displayname", var1.getString("name"));
      }

      if (var1.get("buy-name") != null) {
         var2.put("name", var1.getString("buy-name"));
      }

      if (var1.get("buy-price") != null) {
         var2.put("buy", var1.getDouble("buy-price"));
      }

      if (var1.get("sell-price") != null) {
         var2.put("sell", var1.getDouble("sell-price"));
      }

      if (var1.get("enchantments") != null) {
         var2.put("enchantments", Arrays.stream(var1.getString("enchantments").split(" ")).collect(Collectors.toList()));
      }

      if (!var1.getStringList("shop-lore").isEmpty()) {
         var2.put("displaylore", var1.getStringList("shop-lore").stream().map(var0 -> var0.split("\n")).collect(Collectors.toList()));
      }

      if (!var1.getStringList("lore").isEmpty()) {
         var2.put("lore", var1.getStringList("lore").stream().map(var0 -> var0.split("\n")).collect(Collectors.toList()));
      }

      if (!var1.getStringList("buy-lore").isEmpty()) {
         var2.put("lore", var1.getStringList("buy-lore").stream().map(var0 -> var0.split("\n")).collect(Collectors.toList()));
      }

      HashMap var3 = new HashMap();
      if (var1.get("custom-nbt") != null) {
         var3.putAll(this.getNBTFromString(var1.getString("custom-nbt"), var1.getCurrentPath()));
      }

      if (var1.get("custom-model") != null) {
         var3.put("CustomModelData", var1.getString("custom-model"));
      }

      if (var1.get("item-flags") != null) {
         var3.put("HideFlags", this.flagsToByte(Arrays.stream(var1.getString("item-flags").split(" ")).collect(Collectors.toList()), var1.getCurrentPath()));
      }

      if (!var3.isEmpty()) {
         var2.put("NBTData", new Gson().toJson(var3).toString());
      }

      if (!var3.isEmpty()) {
         System.out.println(var2.get("NBTData"));
      }

      if (var1.get("quantity") != null) {
         var2.put("stack-size", var1.getString("quantity"));
      }

      if (!var1.getStringList("commands").isEmpty()) {
         var2.put("commands", var1.getStringList("commands").stream().map(var0 -> var0.replace("%player_name%", "%player%")).collect(Collectors.toList()));
      }

      if (var1.get("skull-uuid") != null) {
         var2.put("skull-texture", var1.getString("skull-uuid"));
      }

      if (var1.get("mob-type") != null) {
         var2.put("spawnertype", var1.getString("mob-type"));
      }

      if (var1.get("potion-info") != null) {
         Map var4 = var1.getConfigurationSection("potion-info").getValues(true);
         if (Boolean.parseBoolean(var4.get("splash").toString()) && var2.containsKey("material")) {
            Optional var5 = XMaterial.matchXMaterial(var2.get("material").toString());
            if (var5.isPresent() && !((XMaterial)var5.get()).equals(XMaterial.SPLASH_POTION)) {
               var2.put("material", XMaterial.SPLASH_POTION.toString());
            }
         }

         if (var4.containsKey("type")) {
            var2.put(
               "potiontypes",
               PotionTypes.matchPotionDataLegacy(
                     PotionType.valueOf(var4.get("type").toString()),
                     Boolean.parseBoolean(var4.get("extended").toString()),
                     Boolean.parseBoolean(var4.get("upgraded").toString())
                  )
                  .orElseThrow(() -> new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get()))
                  .name()
            );
         }
      }

      return var2;
   }

   private String getPermCommand(String var1) {
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
         return "/lp user %player% permission set " + var1 + " true";
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("PowerRanks")) {
         return "/pr addplayerperm %player% " + var1;
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("GroupManager")) {
         return "/manuaddp %player% " + var1;
      } else {
         return this.plugin.getServer().getPluginManager().isPluginEnabled("UltraPermissions") ? "/upc AddPlayerPermission %player% " + var1 : null;
      }
   }

   private String getPermCommand(String var1, String var2) {
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
         return "/lp user %player% permission set " + var1 + " true world=" + var2;
      } else {
         return this.plugin.getServer().getPluginManager().isPluginEnabled("UltraPermissions")
            ? "/upc AddPlayerPermission %player% " + var1 + " " + var2
            : null;
      }
   }

   private Color getColor(String var1) throws NoSuchFieldException , IllegalAccessException{
      Field var2 = Color.class.getField(var1);
      var2.setAccessible(true);
      return (Color)var2.get(null);
   }

   private int flagsToByte(List<String> var1, String var2) {
      byte var3 = 0;

      for (String var5 : var1) {
         try {
            var3 |= (byte)(1 << ItemFlag.valueOf(var5).ordinal());
         } catch (NullPointerException | IllegalArgumentException var7) {
            SendMessage.warnMessage("&cInvalid item flag found '" + var5 + "' while importing item '" + var2 + "' from GUIShop, skipping...");
         }
      }

      return var3;
   }

   private Map<String, Object> getNBTFromString(String var1, String var2) {
      try {
         return (Map<String, Object>)new Gson().fromJson(var1, Map.class);
      } catch (JsonSyntaxException var4) {
         SendMessage.warnMessage("&cInvalid nbt tag found '" + var1 + "' while importing item '" + var2 + "' from GUIShop, skipping...");
         return new HashMap<>();
      }
   }

   private void emptyCache() {
      this.plugin.runTaskLater(() -> this.CACHE.clear(), 6000L);
   }

   private Map<String, Object> getSectionData(String var1, int var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("enable", true);
      var3.put("slot", var2);
      var3.put("title", "&8&l" + var1);
      var3.put("hidden", false);
      var3.put("sub-section", false);
      var3.put("display-item", false);
      var3.put("economy", ConfigManager.getConfig().getString("ShopSections." + var1 + ".economy"));
      var3.put("nav-bar.mode", "INHERIT");
      HashMap var4 = new HashMap();
      var4.put("material", "COOKED_BEEF");
      var3.put("item", var4);
      return var3;
   }

   private Path createZipFile() {
      DateTimeFormatter var1 = DateTimeFormatter.ofPattern("MM-dd-yyyy");
      LocalDateTime var2 = LocalDateTime.now();
      File var3 = new File(this.plugin.getDataFolder() + "/backups");
      if (!var3.exists()) {
         var3.mkdirs();
      }

      String var4 = this.plugin.getDataFolder() + "/backups/layout - " + var1.format(var2);
      int var5 = 0;

      while (new File(var4 + (var5 > 0 ? "(" + var5 + ")" : "")).exists()) {
         var5++;
      }

      return Paths.get(var4 + (var5 > 0 ? "(" + var5 + ")" : ""));
   }

   private void zipLayout(ZipOutputStream var1) throws FileNotFoundException, IOException {
      for (String var3 : Arrays.asList("shops", "sections", "config.yml", "stands.json", "cache")) {
         String var4 = this.plugin.getDataFolder().getPath() + File.separator + var3;
         if (var4.contains("\\.")) {
            FileInputStream var5 = new FileInputStream(var4);
            var1.putNextEntry(new ZipEntry(var3));
            byte[] var6 = new byte[1024];

            int var7;
            while ((var7 = var5.read(var6)) >= 0) {
               var1.write(var6, 0, var7);
            }

            var5.close();
         } else {
            this.zipFiles(var1, var3);
         }
      }
   }

   private void zipFiles(ZipOutputStream var1, String var2) throws FileNotFoundException, IOException {
      File var3 = new File(this.plugin.getDataFolder() + File.separator + var2);
      if (var3.exists() && var3.isDirectory()) {
         for (File var7 : var3.listFiles()) {
            if (!var7.isDirectory()) {
               String var8 = var7.getPath();
               FileInputStream var9 = new FileInputStream(var8);
               var1.putNextEntry(new ZipEntry(var2 + "/" + var7.toPath().getFileName().toString()));
               byte[] var10 = new byte[1024];

               int var11;
               while ((var11 = var9.read(var10)) >= 0) {
                  var1.write(var10, 0, var11);
               }

               var9.close();
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

      public void addShopsData(String var1, Map<String, Object> var2) {
         this.shops.put(var1, var2);
      }

      public void addSectionsData(String var1, Map<String, Object> var2) {
         this.sections.put(var1, var2);
      }

      public void addConfigData(String var1, Object var2) {
         this.config.put(var1, var2);
      }

      public void addSpecial(String var1, Map<String, Object> var2) {
         this.specials.put(var1, var2);
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

      public Map<String, Object> getSpecial(String var1) {
         return this.specials.get(var1);
      }

      public Map<String, Map<String, Object>> getSpecials() {
         return this.specials;
      }

      public void setRestartRequired(boolean var1) {
         this.restartRequired = var1;
      }

      public boolean isRestartRequired() {
         return this.restartRequired;
      }
   }

   private class TransactionMenuData {
      public final Map<String, Object> data;
      public final int itemsSuccess;
      public final int itemsFailed;

      public TransactionMenuData(Map<String, Object> nullx, int nullxx, int nullxxx) {
         this.data = nullx;
         this.itemsSuccess = nullxx;
         this.itemsFailed = nullxxx;
      }
   }
}
