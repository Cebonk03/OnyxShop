package com.onyxshop.commands.editshop.subcommands;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CreateItemMethodes;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemFormat;
import com.onyxshop.util.ItemFormatter;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.util.StringUtil;

public class EditItem extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private final List<String> allKeys = this.getAllKeys();
   private final List<String> actionKeys = Arrays.asList(
      "material",
      "displayname",
      "name",
      "slot",
      "stack-size",
      "action",
      "skull-texture",
      "skullowner",
      "armorcolor",
      "enchantment-glint",
      "stack-size",
      "CustomModelData",
      "NBTData",
      "components"
   );
   private final List<String> availableActions = this.getAvailableActions();
   String section;
   String itemLoc;
   String action;
   String key;
   Object value;
   private final List<String> listKeys = this.getListKeys();

   public EditItem(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "edititem";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_EDIT_ITEM_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_EDIT_ITEM_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length <= 1) {
         SendMessage.sendMessage(var1, this.getSyntax());
      } else {
         this.section = this.methods.getSection(var1, var2[1]);
         if (this.section != null) {
            if (var2.length <= 2) {
               SendMessage.sendMessage(var1, this.getSyntax());
            } else {
               this.itemLoc = this.methods.getItemLoc(var1, this.section, var2[2]);
               if (this.itemLoc != null) {
                  if (var2.length <= 3) {
                     SendMessage.sendMessage(var1, this.getSyntax());
                  } else {
                     boolean var3 = false;

                     for (String var5 : this.availableActions) {
                        if (var2[3].equalsIgnoreCase(var5)) {
                           this.action = var5;
                           var3 = true;
                           break;
                        }
                     }

                     if (!var3) {
                        SendMessage.sendMessage(
                           var1, Lang.NO_VALID_ACTION.get().replace("%action%", var2[3]).replace("%actions%", Arrays.toString(this.availableActions.toArray()))
                        );
                     } else if (var2.length > 4) {
                        this.key = var2[4];
                        if (this.getAvailableKeys().stream().noneMatch(this.key::equalsIgnoreCase)) {
                           SendMessage.sendMessage(
                              var1, Lang.NO_VALID_KEY.get().replace("%key%", var2[4]).replace("%keys%", Arrays.toString(this.getAvailableKeys().toArray()))
                           );
                        } else if (this.action.equalsIgnoreCase("remove") && this.listKeys.stream().noneMatch(this.key::equalsIgnoreCase)) {
                           this.value = null;
                           if (this.validate(var1, var2)) {
                              this.updateOption(var1);
                           }
                        } else if (var2.length <= 5) {
                           SendMessage.sendMessage(var1, this.getSyntax());
                        } else {
                           if (var2.length > 6) {
                              StringBuilder var6 = new StringBuilder();

                              for (int var7 = 5; var7 < var2.length; var7++) {
                                 if (!var2[var7].equals("-unsafe")) {
                                    var6.append(var2[var7]).append(" ");
                                 }
                              }

                              this.value = var6.substring(0, var6.length() - 1);
                           } else {
                              this.value = var2[5];
                           }

                           if (this.validate(var1, var2)) {
                              this.updateOption(var1);
                           }
                        }
                     } else {
                        SendMessage.sendMessage(var1, this.getSyntax());
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var6 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var6);
               Collections.sort(var6);
               return var6;
            }

            return this.plugin.getShopSections();
         case 3:
            if (!this.isSection(var1[1])) {
               return null;
            } else {
               if (!var1[2].isEmpty()) {
                  ArrayList var5 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[2], this.plugin.getSection(var1[1]).getItemLocs(), var5);
                  Collections.sort(var5);
                  return var5;
               }

               return this.plugin.getSection(var1[1]).getItemLocs();
            }
         case 4:
            if (!var1[3].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], this.getAvailableActions(), var4);
               Collections.sort(var4);
               return var4;
            }

            return this.getAvailableActions();
         case 5:
            if (this.isSection(var1[1]) && this.isItemLoc(var1[2]) && this.isAction(var1[3])) {
               if (!var1[4].isEmpty()) {
                  ArrayList var3 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[4], this.getAvailableKeys(), var3);
                  Collections.sort(var3);
                  return var3;
               }

               return this.getAvailableKeys();
            }

            return null;
         case 6:
            if (!this.isKey(var1[4])) {
               return null;
            } else {
               if (!var1[5].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[5], this.getTabcompletionForValue(var1[1], var1[2], var1[3], var1[4]), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return this.getTabcompletionForValue(var1[1], var1[2], var1[3], var1[4]);
            }
         default:
            return null;
      }
   }

   private boolean isSection(String var1) {
      if (this.plugin.getShopSections().contains(var1)) {
         this.section = var1;
         return true;
      } else {
         return false;
      }
   }

   private boolean isItemLoc(String var1) {
      if (this.plugin.getSection(this.section).getItemLocs().contains(var1)) {
         this.itemLoc = var1;
         return true;
      } else {
         return false;
      }
   }

   private boolean isAction(String var1) {
      if (this.availableActions.contains(var1)) {
         this.action = var1;
         return true;
      } else {
         return false;
      }
   }

   private boolean isKey(String var1) {
      if (this.getAvailableKeys().contains(var1)) {
         this.key = var1;
         return true;
      } else {
         return false;
      }
   }

   private boolean validate(Object var1, String[] var2) {
      if (this.action.equalsIgnoreCase("add")) {
         if (!this.getAvailableKeys().contains(this.key)) {
            SendMessage.sendMessage(var1, Lang.KEY_ALREADY_DEFINED.get().replace("%key%", this.key));
            return false;
         }
      } else if (this.action.equalsIgnoreCase("remove") && !this.getAvailableKeys().contains(this.key)) {
         SendMessage.sendMessage(var1, Lang.KEY_IS_NOT_FOUND.get().replace("%key%", this.key));
         return false;
      }

      if ((this.action.equalsIgnoreCase("remove") || this.action.equalsIgnoreCase("add"))
         && !this.listKeys.contains(this.key)
         && this.key.equalsIgnoreCase("material")) {
         SendMessage.sendMessage(var1, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
         return false;
      } else {
         if (this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)) {
            ArrayList var3 = new ArrayList<>(this.actionKeys);
            var3.addAll(Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags"));
            if (var3.stream().noneMatch(var1x -> var1x.equals(this.key))) {
               SendMessage.sendMessage(var1, Lang.EDIT_KEY_UNAVAILABLE.get().replace("%key%", this.key));
               return false;
            }
         }

         if (this.value != null) {
            String var29 = ConfigManager.getShop(this.section).getString(this.getItemLoc(this.section, this.itemLoc) + ".material");
            if (var29 == null || var29.isEmpty()) {
               SendMessage.sendMessage(var1, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", var29));
               return false;
            }

            ItemStack var4 = null;

            try {
               var4 = CreateItemMethodes.createItemMaterialFromString(null, var29);
            } catch (ItemLoadException var27) {
            }

            if (var4 == null) {
               SendMessage.sendMessage(var1, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", var29));
               return false;
            }

            Material var5 = var4.getType();
            String var6 = this.key;
            switch (var6) {
               case "material":
                  if (!this.action.equalsIgnoreCase("remove") && !this.action.equalsIgnoreCase("add")) {
                     return this.methods.getMaterial(var1, this.value.toString()) != null;
                  }

                  SendMessage.sendMessage(var1, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
                  return false;
               case "displayname":
               case "name":
               case "lore":
               case "displaylore":
               case "buy-commands":
               case "sell-commands":
               case "click-commands":
                  return true;
               case "ignored-tags":
                  if (this.plugin.getShopItem(this.section, this.itemLoc) != null) {
                     if (this.value.toString().split("::").length > 2) {
                        SendMessage.errorMessage(var1, "OnyxShop does not support deeper nested tags as one deep!");
                        return false;
                     }

                     if (this.plugin
                        .versionHandler
                        .hasData(
                           this.plugin.getShopItem(this.section, this.itemLoc).getItemToGive(),
                           NBTMaps.getTag(this.value.toString(), this.section, this.itemLoc)
                        )) {
                        SendMessage.warnMessage(
                           var1,
                           "The item configured at path '"
                              + this.section
                              + "."
                              + this.itemLoc
                              + "' does not contain any NBTTags like '"
                              + this.value.toString()
                              + "', ignoring..."
                        );
                     }
                  }

                  return true;
               case "buy":
               case "sell":
                  this.value = this.methods.getPrice(var1, this.value);
                  return this.value != null;
               case "slot":
                  try {
                     int var31 = Integer.parseInt(this.value.toString());
                     if (this.plugin.maxShopSize * 45 >= var31 && 1 <= var31) {
                        this.value = var31;
                        return true;
                     }

                     throw new NumberFormatException();
                  } catch (NumberFormatException var26) {
                     SendMessage.sendMessage(
                        var1,
                        Lang.NO_VALID_SLOT_FOR_ITEM
                           .get()
                           .replace("%slot%", this.value.toString())
                           .replace("%maxShopSize%", String.valueOf(this.plugin.maxShopSize * 45))
                     );
                     return false;
                  }
               case "ominous-strength":
                  try {
                     int var30 = Integer.parseInt(this.value.toString());
                     if (5 > var30 && 1 < var30) {
                        this.value = var30;
                        return true;
                     }

                     throw new NumberFormatException();
                  } catch (NumberFormatException var25) {
                     SendMessage.sendMessage(var1, Lang.INVALID_OMINOUS_BOTTLE_STRENGTH.get().replace("%strength%", this.value.toString()));
                     return false;
                  }
               case "enchantments":
                  try {
                     this.plugin.enchantsProvider.getEnchant(this.value.toString());
                     return true;
                  } catch (ItemLoadException var24) {
                     SendMessage.sendMessage(var1, var24.getMessage());
                     return false;
                  }
               case "enchantment-glint":
               case "hidePricingLore":
               case "hidden":
               case "requireItem":
               case "giveItem":
               case "refill-stock":
               case "prior-lore":
               case "close-menu":
               case "hideUtilLore":
               case "buy-prices.require-all":
               case "sell-prices.give-all":
                  this.value = Boolean.valueOf(this.value.toString());
                  return true;
               case "flicker":
               case "trail":
                  if (ConfigManager.getShop(this.section).getStringList(this.getItemLoc(this.section, this.itemLoc) + ".colors").isEmpty()) {
                     SendMessage.sendMessage(
                        var1,
                        ChatColor.RED
                           + "To apply a flicker, trail or firework shape you need to give the firework at least one color using the 'colors' option"
                     );
                     return false;
                  }

                  this.value = Boolean.valueOf(this.value.toString());
                  return true;
               case "potiontypes":
                  if (!PotionTypes.canHaveEffects(var5)) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
                     return false;
                  }

                  Optional var8 = PotionTypes.matchPotionType(this.value.toString());
                  if (!var8.isPresent()) {
                     SendMessage.warnMessage(var1, Lang.ITEM_POTIONTYPE_NULL.get());
                     return false;
                  }

                  if (!((PotionTypes)var8.get()).isSupported()) {
                     SendMessage.warnMessage(var1, Lang.POTIONTYPE_NOT_SUPPORTED.get());
                     return false;
                  }

                  return true;
               case "spawnertype":
                  if (var5 != XMaterial.SPAWNER.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_SPAWNER.get());
                     return false;
                  }

                  for (EntityType var38 : EntityType.values()) {
                     if (this.value.toString().equalsIgnoreCase(var38.toString())) {
                        return true;
                     }
                  }

                  SendMessage.warnMessage(var1, Lang.ITEM_SPAWNERTYPE_NULL.get());
                  return false;
               case "armorcolor":
                  try {
                     Color.fromRGB(Integer.parseInt(this.value.toString()));
                  } catch (IllegalArgumentException var23) {
                     SendMessage.warnMessage(var1, Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", this.section + "." + this.itemLoc + "." + this.key));
                  }

                  return true;
               case "recipes":
                  if (var5 != XMaterial.KNOWLEDGE_BOOK.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_BOOK.get());
                     return false;
                  }

                  if (XMaterial.matchXMaterial(this.value.toString()).isPresent()
                     && XMaterial.matchXMaterial(this.value.toString()).get().parseMaterial() != null) {
                     return true;
                  }

                  SendMessage.warnMessage(var1, Lang.CANNOT_GET_RECIPE_MATERIAL.get());
                  return false;
               case "skullowner":
                  if (var5 != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  return true;
               case "NBTData":
                  if (Character.toString(this.value.toString().charAt(0)).equals("{")
                     && Character.toString(this.value.toString().charAt(this.value.toString().length() - 1)).equals("}")) {
                     return true;
                  }

                  SendMessage.warnMessage(var1, Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
                  return false;
               case "colors":
               case "fade-colors":
                  if (!Arrays.stream(FireworkUtil.FireworkColor.values())
                     .map(var0 -> var0.name())
                     .collect(Collectors.toList())
                     .contains(this.value.toString().toUpperCase(Locale.ENGLISH))) {
                     SendMessage.warnMessage(var1, "Could not get the color '" + this.value.toString() + "'");
                     return false;
                  }

                  return true;
               case "shape":
                  if (ConfigManager.getShop(this.section).getStringList(this.getItemLoc(this.section, this.itemLoc) + ".colors").isEmpty()) {
                     SendMessage.warnMessage(
                        var1, "To apply a flicker, trail or firework shape you need to give the firework at least one color using the 'colors' option"
                     );
                     return false;
                  }

                  try {
                     Type.valueOf(this.value.toString().toUpperCase(Locale.ENGLISH));
                     return true;
                  } catch (IllegalArgumentException var22) {
                     SendMessage.warnMessage(var1, "Could not get the firework shape '" + this.value.toString() + "'");
                     return false;
                  }
               case "duration":
                  try {
                     Integer.parseInt(this.value.toString());
                     return true;
                  } catch (NumberFormatException var21) {
                     SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
                     return false;
                  }
               case "max-buy":
               case "max-sell":
               case "min-buy":
               case "min-sell":
               case "stack-size":
               case "level-requirement":
               case "stock-limit-global":
               case "stock-limit-player":
               case "sell-limit-global":
               case "sell-limit-player":
                  try {
                     this.value = Integer.parseInt(this.value.toString());
                  } catch (NumberFormatException var20) {
                     SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
                     return false;
                  }

                  if (this.key.equals("stack-size") && !this.plugin.allowIllegalStacks) {
                     if ((Integer)this.value < var5.getMaxStackSize()) {
                        SendMessage.warnMessage(
                           var1,
                           ChatColor.RED
                              + "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                                 .replace("%material%", var5.toString())
                                 .replace("%stack-size%", this.value.toString())
                        );
                     }
                  } else if (!this.key.equals("min-buy") && !this.key.equals("min-sell")) {
                     if (this.key.equals("max-buy") || this.key.equals("max-sell")) {
                        if (this.key.equals("max-buy")) {
                           if (this.plugin.getShopItem(this.section, this.itemLoc).isMinBuy((Integer)this.value)) {
                              SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " should be larger as the min-buy option of this item.");
                              return false;
                           }
                        } else if (this.key.equals("max-sell") && this.plugin.getShopItem(this.section, this.itemLoc).isMinSell((Integer)this.value)) {
                           SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " should be larger as the min-sell option of this item.");
                           return false;
                        }
                     }
                  } else {
                     if ((Integer)this.value < 0) {
                        SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " needs to be a positive value!");
                        return false;
                     }

                     if (this.key.equals("min-buy")) {
                        if ((Integer)this.value < this.plugin.getShopItem(this.section, this.itemLoc).getStackSize()) {
                           SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " should be larger as the stack size of this shop item.");
                           return false;
                        }

                        if (this.plugin.getShopItem(this.section, this.itemLoc).isMaxBuy((Integer)this.value)) {
                           SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " should be smaller as the max-buy option of this item.");
                           return false;
                        }
                     } else if (this.key.equals("min-sell") && this.plugin.getShopItem(this.section, this.itemLoc).isMaxSell((Integer)this.value)) {
                        SendMessage.warnMessage(var1, ChatColor.RED + "The " + this.key + " should be smaller as the max-sell option of this item.");
                        return false;
                     }
                  }

                  return true;
               case "instrument":
                  if (var5 != XMaterial.GOAT_HORN.parseMaterial()) {
                     SendMessage.warnMessage(var1, ChatColor.RED + "To apply a horn sound type, the material needs to be a goat horn");
                     return false;
                  }

                  try {
                     CreateItemMethodes.Horn.setInstrument(new ItemStack(Material.GOAT_HORN), this.value.toString());
                     return true;
                  } catch (ItemLoadException var19) {
                     SendMessage.warnMessage(
                        var1,
                        "Could not find a goat horn instrument like '"
                           + this.value.toString()
                           + "', valid values are:\n"
                           + ChatColor.RED
                           + CreateItemMethodes.Horn.getValues()
                     );
                     return false;
                  }
               case "section":
                  return this.plugin.getShopSections().contains(this.value.toString());
               case "stew-effect":
                  if (var5 != XMaterial.MUSHROOM_STEW.parseMaterial()) {
                     SendMessage.warnMessage(var1, "To add a stew effect to an item, the material needs to be a 'SUSPICIOUS_STEW'.");
                     return false;
                  }

                  CreateItemMethodes.Stew var9 = CreateItemMethodes.Stew.get(this.value.toString());
                  if (var9 == null) {
                     SendMessage.warnMessage(
                        var1,
                        "Could not find a stew effect like '"
                           + this.value.toString()
                           + "', valid values are:\n"
                           + ChatColor.RED
                           + Arrays.stream(CreateItemMethodes.Stew.values()).map(Enum::name).collect(Collectors.toList())
                     );
                     return false;
                  }

                  return true;
               case "armor-trim.type":
                  if (!(new ItemStack(var5).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(var1, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimType(this.value.toString());
                     return true;
                  } catch (ItemLoadException var18) {
                     SendMessage.warnMessage(var1, var18.getMessage());
                     return false;
                  }
               case "armor-trim.pattern":
                  if (!(new ItemStack(var5).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(var1, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimPattern(this.value.toString());
                     return true;
                  } catch (ItemLoadException var17) {
                     SendMessage.warnMessage(var1, var17.getMessage());
                     return false;
                  }
               case "permission":
                  if (!this.value.toString().toLowerCase(Locale.ENGLISH).contains("onyxshop")) {
                     return true;
                  }

                  SendMessage.warnMessage(
                     var1, "The permission is already prefixed using 'OnyxShop.shop." + this.section + ".item.<permission>', no need to prefix it again."
                  );
                  return false;
               case "buy-prices.prices":
               case "sell-prices.prices":
               case "economy":
                  Double var10 = null;
                  if (this.key.equals("buy-prices.prices") || this.key.equals("sell-prices.prices")) {
                     try {
                        var10 = this.methods.getPrice(var1, this.value.toString().split("::")[1]);
                        if (var10 == null) {
                           return false;
                        }

                        this.value = this.value.toString().split("::")[0];
                     } catch (ArrayIndexOutOfBoundsException var16) {
                        SendMessage.warnMessage(var1, "A price needs to be specified.");
                        return false;
                     }
                  }

                  if (this.value.toString().equalsIgnoreCase("handItem")) {
                     if (!(var1 instanceof Player)) {
                        SendMessage.warnMessage(var1, Lang.REAL_PLAYER.get());
                        return false;
                     }

                     Player var34 = (Player)var1;
                     if (var34.getItemInHand().getType() == Material.AIR) {
                        SendMessage.warnMessage(var1, "Item cannot be air");
                        return false;
                     }

                     this.value = "ITEM:" + ItemFormatter.deserialize(var34.getItemInHand(), Arrays.asList(var2).contains("-unsafe"));
                  } else {
                     EcoType var35 = EconomyType.getFromString(this.value.toString());
                     if (var35 == null) {
                        SendMessage.warnMessage(
                           var1,
                           "Could not find a economy type such as '"
                              + this.value.toString()
                              + "', valid values are: "
                              + Arrays.stream(EconomyType.values()).map(Enum::name).collect(Collectors.joining(", "))
                        );
                        return false;
                     }

                     if (var35.getType() == EconomyType.ITEM) {
                        ItemFormat var37 = new ItemFormat(var35.getCurrency(), this.section + ".pages." + this.itemLoc + ".economy", 0);
                        if (var37.getItem() == null) {
                           SendMessage.warnMessage(var1, var37.getErrorMessage());
                           return false;
                        }
                     }
                  }

                  if (var10 != null) {
                     this.value = this.value + "::" + var10;
                  }

                  return true;
               case "skull-texture":
                  if (var5 != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  try {
                     Base64.getDecoder().decode(this.value.toString());
                     return true;
                  } catch (IllegalArgumentException var15) {
                     SendMessage.warnMessage(var1, ChatColor.RED + "Not a valid Base64 string");
                     return false;
                  }
               case "auto-restock-global":
               case "auto-restock-player":
               case "auto-restock-global-sell":
               case "auto-restock-player-sell":
                  try {
                     TimeUtils.getTime(this.value.toString());
                     return true;
                  } catch (ParseException var14) {
                     SendMessage.warnMessage(var1, var14.getMessage());
                     return false;
                  }
               case "auto-restock-player-frequency":
               case "auto-restock-global-frequency":
               case "auto-restock-player-sell-frequency":
               case "auto-restock-global-sell-frequency":
                  try {
                     LinkedList var11 = this.plugin.getScheduleManager().validate(this.value.toString());
                     if (var11.isEmpty()) {
                        SendMessage.warnMessage(var1, "Invalid cron expression, this schedule will not fire again.");
                        return false;
                     }

                     StringBuilder var12 = new StringBuilder();

                     for (int var13 = 0; var13 < var11.size(); var13++) {
                        var12.append(
                           ChatColor.GRAY
                              + "-"
                              + ChatColor.GREEN
                              + " "
                              + TimeUtils.getDateTime(((Date)var11.get(var13)).getTime())
                              + (var13 != var11.size() ? "\n" : "")
                        );
                     }

                     SendMessage.infoMessage(var1, "Loaded cron job with following schedule dates loaded(up to 5):\n" + var12.toString());
                     return true;
                  } catch (RuntimeException var28) {
                     if (!(var1 instanceof ConsoleCommandSender)) {
                        SendMessage.warnMessage(var1, "Invalid cron expression format with reason(See console for more details): " + var28.getMessage());
                     }

                     SendMessage.warnMessage("Invalid cron expression format specified while executing command");
                     var28.printStackTrace();
                     return false;
                  }
               case "action":
                  if (Arrays.stream(ActionItem.Action.values()).noneMatch(var1x -> var1x.name().equalsIgnoreCase(this.value.toString()))) {
                     SendMessage.warnMessage(var1, Lang.INVALID_ITEM_ACTION.get().replace("%action%", this.value.toString()));
                     return false;
                  }

                  return true;
            }
         }

         return true;
      }
   }

   private List<String> getTabcompletionForValue(String var1, String var2, String var3, String var4) {
      if (!this.isSection(var1) || !this.isItemLoc(var2) || !this.isAction(var3)) {
         return new ArrayList<>();
      } else if (var3.equalsIgnoreCase("remove") && this.listKeys.contains(var4)) {
         return ConfigManager.getShop(var1).getStringList(this.getItemLoc(var1, var2) + "." + var4);
      } else if (var3.equalsIgnoreCase("remove")) {
         ArrayList var5 = new ArrayList();
         var5.add(ConfigManager.getShop(var1).get(this.getItemLoc(var1, var2) + "." + var4, "").toString());
         return var5;
      } else {
         switch (var4) {
            case "material":
            case "recipes":
               return this.plugin.getSupportedMatNames();
            case "displayname":
            case "name":
            case "lore":
            case "displaylore":
               return this.methods.getExampleItemNames();
            case "buy":
            case "sell":
               return this.plugin.getExamplePrices();
            case "enchantments":
               return this.plugin.enchantsProvider.getAvailableEnchantKeys();
            case "potiontypes":
               return PotionTypes.getNames();
            case "armorcolor":
               return this.methods.getExampleRGBColors();
            case "spawnertype":
               return this.getEntityNames();
            case "skullowner":
               return this.getExamplePlayerNames();
            case "hidePricingLore":
            case "enchantment-glint":
            case "hidden":
            case "flicker":
            case "trail":
            case "requireItem":
            case "giveItem":
            case "refill-stock":
            case "buy-prices.require-all":
            case "sell-prices.give-all":
               return Arrays.asList("true", "false");
            case "buy-prices.prices":
            case "sell-prices.prices":
            case "economy":
               List<String> var7 = this.plugin.getEcoHandler().getRegistered().stream().map(var0 -> {
                  String var1x = var0.toString();
                  if (var0.getType() == EconomyType.ITEM) {
                     var1x = var0.getCurrency();
                  }

                  return var1x;
               }).collect(Collectors.toList());
               var7.add("ITEM:material:EMERALD name:&a&lGem Stone id:Gems");
               var7.add("ITEM:material:GOLD_NUGGET");
               var7.add("handItem");
               if (!this.key.equals("buy-prices.prices") && !this.key.equals("sell-prices.prices")) {
                  return var7;
               }

               return var7.stream().map(var0 -> var0 + "::130").collect(Collectors.toList());
            case "max-buy":
            case "max-sell":
            case "min-buy":
            case "min-sell":
               return Arrays.asList("1", "2", "8", "16", "32", "64", "128");
            case "slot":
               return Arrays.asList("1", "9", "73", "108");
            case "ominous-strength":
               return Arrays.asList("1", "2", "3", "4", "5");
            case "NBTData":
               return this.getExampleNBTTags();
            case "buy-commands":
            case "sell-commands":
            case "click-commands":
               return this.getExampleCommands();
            case "ignored-tags":
               return Arrays.asList("display::Name", "Damage", "Enchantments", "HideTags");
            case "colors":
            case "fade-colors":
               return Arrays.stream(FireworkUtil.FireworkColor.values()).map(var0 -> var0.name()).collect(Collectors.toList());
            case "shape":
               return Arrays.stream(Type.values()).map(var0 -> var0.name()).collect(Collectors.toList());
            case "duration":
               return Arrays.asList("1", "2", "3");
            case "stack-size":
               ArrayList var8 = new ArrayList();

               for (int var9 = 1; var9 < 64; var9++) {
                  var8.add(String.valueOf(var9));
               }

               return var8;
            case "instrument":
               return CreateItemMethodes.Horn.getValues();
            case "section":
               return this.plugin.getShopSections();
            case "stew-effect":
               return Arrays.stream(CreateItemMethodes.Stew.values()).map(Enum::name).collect(Collectors.toList());
            case "armor-trim.type":
               return new CreateItemMethodes.ArmorTrim().getTypes();
            case "armor-trim.pattern":
               return new CreateItemMethodes.ArmorTrim().getPatterns();
            case "level-requirement":
               return Arrays.asList("5", "10", "20", "35");
            case "skull-texture":
               return Arrays.asList(
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRjZjJjMTk4ZjYxZGY1YTZkMGJhOTdkYmY5MGMzMzc5OTU0MDVjMTczOTZjMDE2Yzg1ZjZmM2ZlYTUyYzkwNiJ9fX0=",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ5N2IxNDdjZmFlNTIyMDU1OTdmNzJlM2M0ZWY1MjUxMmU5Njc3MDIwZTRiNGZhNzUxMmMzYzZhY2RkOGMxIn19fQ==",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ5NjU4OWZiNWMxZjY5Mzg3YjdmYjE3ZDkyMzEyMDU4ZmY2ZThlYmViM2ViODllNGY3M2U3ODE5NjExM2IifX19"
               );
            case "permission":
               return Arrays.asList("gods-gift", "donor", "special", "apple", "crazy", "builder");
            case "stock-limit-global":
            case "stock-limit-player":
            case "sell-limit-global":
            case "sell-limit-player":
               return Arrays.asList("10", "50", "100", "120", "19000");
            case "auto-restock-global":
            case "auto-restock-player":
            case "auto-restock-global-sell":
            case "auto-restock-player-sell":
               return Arrays.asList("1h 30m", "48h", "120s", "7d 12h 30m 10s", "1440m");
            case "auto-restock-player-frequency":
            case "auto-restock-global-frequency":
            case "auto-restock-player-sell-frequency":
            case "auto-restock-global-sell-frequency":
               return Arrays.asList("0 0 0 ? * *", "0 0 12 * * ?", "0 30 11 ? * MON-FRI", "0 30 16 ? * FRI", "59 29 * ? * *", "0 0 12 * * ? 2023-2025");
            case "action":
               return Arrays.stream(ActionItem.Action.values()).map(Enum::name).collect(Collectors.toList());
            default:
               return new ArrayList<>();
         }
      }
   }

   private List<String> getAvailableKeys() {
      try {
         ArrayList var1 = new ArrayList<>(this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc) ? this.actionKeys : this.allKeys);
         ArrayList var2 = new ArrayList(ConfigManager.getShop(this.section).getConfigurationSection(this.getItemLoc(this.section, this.itemLoc)).getKeys(false));
         if (this.action.equalsIgnoreCase("add")) {
            var1.removeAll(var2);
            var1.addAll(
               this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)
                  ? Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags")
                  : this.listKeys
            );
            return var1;
         }

         if (this.action.equalsIgnoreCase("remove")) {
            return var2;
         }

         if (this.action.equalsIgnoreCase("set")) {
            var1.addAll(
               this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)
                  ? Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags")
                  : this.listKeys
            );
            return var1;
         }
      } catch (NullPointerException var3) {
      }

      return new ArrayList<>();
   }

   private List<String> getAllKeys() {
      ArrayList var1 = new ArrayList();
      var1.add("material");
      var1.add("buy");
      var1.add("sell");
      var1.add("name");
      var1.add("displayname");
      var1.add("hidePricingLore");
      var1.add("slot");
      var1.add("stack-size");
      var1.add("enchantment-glint");
      var1.add("spawnertype");
      var1.add("skullowner");
      var1.add("armorcolor");
      var1.add("duration");
      var1.add("armor-trim.pattern");
      var1.add("armor-trim.type");
      var1.add("flicker");
      var1.add("trail");
      var1.add("shape");
      var1.add("instrument");
      var1.add("section");
      var1.add("stew-effect");
      var1.add("ominous-strength");
      var1.add("hidden");
      var1.add("max-buy");
      var1.add("max-sell");
      var1.add("min-buy");
      var1.add("min-sell");
      var1.add("prior-lore");
      var1.add("close-menu");
      var1.add("NBTData");
      var1.add("buy-prices.require-all");
      var1.add("sell-prices.give-all");
      var1.add("requireItem");
      var1.add("giveItem");
      var1.add("level-requirement");
      var1.add("skull-texture");
      var1.add("economy");
      var1.add("permission");
      var1.add("hideUtilLore");
      var1.add("refill-stock");
      var1.add("stock-limit-global");
      var1.add("auto-restock-global");
      var1.add("auto-restock-global-frequency");
      var1.add("stock-limit-player");
      var1.add("auto-restock-player");
      var1.add("auto-restock-player-frequency");
      var1.add("sell-limit-global");
      var1.add("auto-restock-global-sell");
      var1.add("auto-restock-global-sell-frequency");
      var1.add("sell-limit-player");
      var1.add("auto-restock-player-sell");
      var1.add("auto-restock-player-sell-frequency");
      var1.add("action");
      return var1;
   }

   private List<String> getListKeys() {
      ArrayList var1 = new ArrayList();
      var1.add("enchantments");
      var1.add("potiontypes");
      var1.add("lore");
      var1.add("displaylore");
      var1.add("recipes");
      var1.add("colors");
      var1.add("fade-colors");
      var1.add("buy-commands");
      var1.add("sell-commands");
      var1.add("click-commands");
      var1.add("buy-prices.prices");
      var1.add("sell-prices.prices");
      var1.add("ignored-tags");
      return var1;
   }

   private List<String> getAvailableActions() {
      ArrayList var1 = new ArrayList();
      var1.add("add");
      var1.add("set");
      var1.add("remove");
      return var1;
   }

   private List<String> getExampleNBTTags() {
      ArrayList var1 = new ArrayList();
      var1.add("{CanDestroy:[\"minecraft:diamond_block\",\"#minecraft:iron_block\"]}");
      var1.add("{PickupDelay:80}");
      var1.add("{CanPlaceOn:[\"minecraft:grass_block\",\"#minecraft:dirt\",\"#minecraft:sand\",\"#minecraft:cobblestone\",\"#minecraft:oak_log\"]}");
      var1.add("{Enchantments:[{id:\"9\",lvl:5},{id:\"17\",lvl:3},{id:\"12\",lvl:5},{id:\"13\",lvl:2},{id:\"11\",lvl:5}]}");
      return var1;
   }

   private List<String> getExampleCommands() {
      ArrayList var1 = new ArrayList();
      var1.add("/tp <player> <player>");
      var1.add("weather clear");
      var1.add("time set day");
      var1.add("/summon llama ~ ~1 ~");
      return var1;
   }

   private List<String> getExamplePlayerNames() {
      ArrayList var1 = new ArrayList();

      for (Player var3 : this.plugin.getServer().getOnlinePlayers()) {
         var1.add(var3.getName());
      }

      return var1;
   }

   private List<String> getEntityNames() {
      ArrayList var1 = new ArrayList();

      for (EntityType var5 : EntityType.values()) {
         if (var5.isAlive()) {
            try {
               EntityType.valueOf(var5.name());
               var1.add(var5.name());
            } catch (IllegalArgumentException var7) {
            }
         }
      }

      return var1;
   }

   private List<String> getValues(String var1, String var2, String var3) {
      return ConfigManager.getShop(var1).getStringList(this.getItemLoc(var1, var2) + "." + var3);
   }

   private void updateOption(Object var1) {
      SendMessage.sendMessage(var1, Lang.EDITSHOP_EDITING_ITEM.get());
      String var2 = this.getItemLoc(this.section, this.itemLoc);
      if (this.listKeys.contains(this.key)) {
         if (this.action.equalsIgnoreCase("add")) {
            List var3 = this.getValues(this.section, this.itemLoc, this.key);
            var3.add(this.value.toString());
            ConfigManager.getShop(this.section).set(var2 + "." + this.key, var3);
         } else if (this.action.equalsIgnoreCase("set")) {
            ConfigManager.getShop(this.section).set(var2 + "." + this.key, Collections.singletonList(this.value.toString()));
         } else if (this.action.equalsIgnoreCase("remove")) {
            List var4 = this.getValues(this.section, this.itemLoc, this.key);
            if (var4.size() == 1) {
               ConfigManager.getShop(this.section).set(var2 + "." + this.key, null);
            } else if (var4.size() >= 1) {
               var4.remove(this.value.toString());
               ConfigManager.getShop(this.section).set(var2 + "." + this.key, var4);
            }
         }
      } else {
         ConfigManager.getShop(this.section).set(var2 + "." + this.key, this.value);
      }

      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(var1, Lang.EDITSHOP_EDIT_ITEM_SUCCESSFUL.get().replace("%itemPath%", var2));
      SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getItemLoc(String var1, String var2) {
      return (this.plugin.getShopType(var1) == ShopType.CATEGORY ? "pages." : "") + var2;
   }
}
