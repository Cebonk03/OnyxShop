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

   public EditItem(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length <= 1) {
         SendMessage.sendMessage(value, this.getSyntax());
      } else {
         this.section = this.methods.getSection(value, values[1]);
         if (this.section != null) {
            if (values.length <= 2) {
               SendMessage.sendMessage(value, this.getSyntax());
            } else {
               this.itemLoc = this.methods.getItemLoc(value, this.section, values[2]);
               if (this.itemLoc != null) {
                  if (values.length <= 3) {
                     SendMessage.sendMessage(value, this.getSyntax());
                  } else {
                     boolean flag = false;

                     for (String text : this.availableActions) {
                        if (values[3].equalsIgnoreCase(text)) {
                           this.action = text;
                           flag = true;
                           break;
                        }
                     }

                     if (!flag) {
                        SendMessage.sendMessage(
                           value, Lang.NO_VALID_ACTION.get().replace("%action%", values[3]).replace("%actions%", Arrays.toString(this.availableActions.toArray()))
                        );
                     } else if (values.length > 4) {
                        this.key = values[4];
                        if (this.getAvailableKeys().stream().noneMatch(this.key::equalsIgnoreCase)) {
                           SendMessage.sendMessage(
                              value, Lang.NO_VALID_KEY.get().replace("%key%", values[4]).replace("%keys%", Arrays.toString(this.getAvailableKeys().toArray()))
                           );
                        } else if (this.action.equalsIgnoreCase("remove") && this.listKeys.stream().noneMatch(this.key::equalsIgnoreCase)) {
                           this.value = null;
                           if (this.validate(value, values)) {
                              this.updateOption(value);
                           }
                        } else if (values.length <= 5) {
                           SendMessage.sendMessage(value, this.getSyntax());
                        } else {
                           if (values.length > 6) {
                              StringBuilder builder = new StringBuilder();

                              for (int index = 5; index < values.length; index++) {
                                 if (!values[index].equals("-unsafe")) {
                                    builder.append(values[index]).append(" ");
                                 }
                              }

                              this.value = builder.substring(0, builder.length() - 1);
                           } else {
                              this.value = values[5];
                           }

                           if (this.validate(value, values)) {
                              this.updateOption(value);
                           }
                        }
                     } else {
                        SendMessage.sendMessage(value, this.getSyntax());
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 2:
            if (!values[1].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[1], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 3:
            if (!this.isSection(values[1])) {
               return null;
            } else {
               if (!values[2].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[2], this.plugin.getSection(values[1]).getItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.plugin.getSection(values[1]).getItemLocs();
            }
         case 4:
            if (!values[3].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[3], this.getAvailableActions(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.getAvailableActions();
         case 5:
            if (this.isSection(values[1]) && this.isItemLoc(values[2]) && this.isAction(values[3])) {
               if (!values[4].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[4], this.getAvailableKeys(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.getAvailableKeys();
            }

            return null;
         case 6:
            if (!this.isKey(values[4])) {
               return null;
            } else {
               if (!values[5].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[5], this.getTabcompletionForValue(values[1], values[2], values[3], values[4]), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.getTabcompletionForValue(values[1], values[2], values[3], values[4]);
            }
         default:
            return null;
      }
   }

   private boolean isSection(String sectionId) {
      if (this.plugin.getShopSections().contains(sectionId)) {
         this.section = sectionId;
         return true;
      } else {
         return false;
      }
   }

   private boolean isItemLoc(String itemId) {
      if (this.plugin.getSection(this.section).getItemLocs().contains(itemId)) {
         this.itemLoc = itemId;
         return true;
      } else {
         return false;
      }
   }

   private boolean isAction(String text) {
      if (this.availableActions.contains(text)) {
         this.action = text;
         return true;
      } else {
         return false;
      }
   }

   private boolean isKey(String key) {
      if (this.getAvailableKeys().contains(key)) {
         this.key = key;
         return true;
      } else {
         return false;
      }
   }

   private boolean validate(Object value, String[] values) {
      if (this.action.equalsIgnoreCase("add")) {
         if (!this.getAvailableKeys().contains(this.key)) {
            SendMessage.sendMessage(value, Lang.KEY_ALREADY_DEFINED.get().replace("%key%", this.key));
            return false;
         }
      } else if (this.action.equalsIgnoreCase("remove") && !this.getAvailableKeys().contains(this.key)) {
         SendMessage.sendMessage(value, Lang.KEY_IS_NOT_FOUND.get().replace("%key%", this.key));
         return false;
      }

      if ((this.action.equalsIgnoreCase("remove") || this.action.equalsIgnoreCase("add"))
         && !this.listKeys.contains(this.key)
         && this.key.equalsIgnoreCase("material")) {
         SendMessage.sendMessage(value, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
         return false;
      } else {
         if (this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)) {
            ArrayList values2 = new ArrayList<>(this.actionKeys);
            values2.addAll(Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags"));
            if (values2.stream().noneMatch(element -> element.equals(this.key))) {
               SendMessage.sendMessage(value, Lang.EDIT_KEY_UNAVAILABLE.get().replace("%key%", this.key));
               return false;
            }
         }

         if (this.value != null) {
            String text = ConfigManager.getShop(this.section).getString(this.getItemLoc(this.section, this.itemLoc) + ".material");
            if (text == null || text.isEmpty()) {
               SendMessage.sendMessage(value, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", text));
               return false;
            }

            ItemStack itemStack = null;

            try {
               itemStack = CreateItemMethodes.createItemMaterialFromString(null, text);
            } catch (ItemLoadException exception) {
            }

            if (itemStack == null) {
               SendMessage.sendMessage(value, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", text));
               return false;
            }

            Material material = itemStack.getType();
            String key = this.key;
            switch (key) {
               case "material":
                  if (!this.action.equalsIgnoreCase("remove") && !this.action.equalsIgnoreCase("add")) {
                     return this.methods.getMaterial(value, this.value.toString()) != null;
                  }

                  SendMessage.sendMessage(value, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
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
                        SendMessage.errorMessage(value, "OnyxShop does not support deeper nested tags as one deep!");
                        return false;
                     }

                     if (this.plugin
                        .versionHandler
                        .hasData(
                           this.plugin.getShopItem(this.section, this.itemLoc).getItemToGive(),
                           NBTMaps.getTag(this.value.toString(), this.section, this.itemLoc)
                        )) {
                        SendMessage.warnMessage(
                           value,
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
                  this.value = this.methods.getPrice(value, this.value);
                  return this.value != null;
               case "slot":
                  try {
                     int value2 = Integer.parseInt(this.value.toString());
                     if (this.plugin.maxShopSize * 45 >= value2 && 1 <= value2) {
                        this.value = value2;
                        return true;
                     }

                     throw new NumberFormatException();
                  } catch (NumberFormatException exception) {
                     SendMessage.sendMessage(
                        value,
                        Lang.NO_VALID_SLOT_FOR_ITEM
                           .get()
                           .replace("%slot%", this.value.toString())
                           .replace("%maxShopSize%", String.valueOf(this.plugin.maxShopSize * 45))
                     );
                     return false;
                  }
               case "ominous-strength":
                  try {
                     int value2 = Integer.parseInt(this.value.toString());
                     if (5 > value2 && 1 < value2) {
                        this.value = value2;
                        return true;
                     }

                     throw new NumberFormatException();
                  } catch (NumberFormatException exception) {
                     SendMessage.sendMessage(value, Lang.INVALID_OMINOUS_BOTTLE_STRENGTH.get().replace("%strength%", this.value.toString()));
                     return false;
                  }
               case "enchantments":
                  try {
                     this.plugin.enchantsProvider.getEnchant(this.value.toString());
                     return true;
                  } catch (ItemLoadException exception) {
                     SendMessage.sendMessage(value, exception.getMessage());
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
                        value,
                        ChatColor.RED
                           + "To apply a flicker, trail or firework shape you need to give the firework at least one color using the 'colors' option"
                     );
                     return false;
                  }

                  this.value = Boolean.valueOf(this.value.toString());
                  return true;
               case "potiontypes":
                  if (!PotionTypes.canHaveEffects(material)) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
                     return false;
                  }

                  Optional optional = PotionTypes.matchPotionType(this.value.toString());
                  if (!optional.isPresent()) {
                     SendMessage.warnMessage(value, Lang.ITEM_POTIONTYPE_NULL.get());
                     return false;
                  }

                  if (!((PotionTypes)optional.get()).isSupported()) {
                     SendMessage.warnMessage(value, Lang.POTIONTYPE_NOT_SUPPORTED.get());
                     return false;
                  }

                  return true;
               case "spawnertype":
                  if (material != XMaterial.SPAWNER.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_SPAWNER.get());
                     return false;
                  }

                  for (EntityType entityType : EntityType.values()) {
                     if (this.value.toString().equalsIgnoreCase(entityType.toString())) {
                        return true;
                     }
                  }

                  SendMessage.warnMessage(value, Lang.ITEM_SPAWNERTYPE_NULL.get());
                  return false;
               case "armorcolor":
                  try {
                     Color.fromRGB(Integer.parseInt(this.value.toString()));
                  } catch (IllegalArgumentException exception) {
                     SendMessage.warnMessage(value, Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", this.section + "." + this.itemLoc + "." + this.key));
                  }

                  return true;
               case "recipes":
                  if (material != XMaterial.KNOWLEDGE_BOOK.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_BOOK.get());
                     return false;
                  }

                  if (XMaterial.matchXMaterial(this.value.toString()).isPresent()
                     && XMaterial.matchXMaterial(this.value.toString()).get().parseMaterial() != null) {
                     return true;
                  }

                  SendMessage.warnMessage(value, Lang.CANNOT_GET_RECIPE_MATERIAL.get());
                  return false;
               case "skullowner":
                  if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  return true;
               case "NBTData":
                  if (Character.toString(this.value.toString().charAt(0)).equals("{")
                     && Character.toString(this.value.toString().charAt(this.value.toString().length() - 1)).equals("}")) {
                     return true;
                  }

                  SendMessage.warnMessage(value, Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
                  return false;
               case "colors":
               case "fade-colors":
                  if (!Arrays.stream(FireworkUtil.FireworkColor.values())
                     .map(element -> element.name())
                     .collect(Collectors.toList())
                     .contains(this.value.toString().toUpperCase(Locale.ENGLISH))) {
                     SendMessage.warnMessage(value, "Could not get the color '" + this.value.toString() + "'");
                     return false;
                  }

                  return true;
               case "shape":
                  if (ConfigManager.getShop(this.section).getStringList(this.getItemLoc(this.section, this.itemLoc) + ".colors").isEmpty()) {
                     SendMessage.warnMessage(
                        value, "To apply a flicker, trail or firework shape you need to give the firework at least one color using the 'colors' option"
                     );
                     return false;
                  }

                  try {
                     Type.valueOf(this.value.toString().toUpperCase(Locale.ENGLISH));
                     return true;
                  } catch (IllegalArgumentException exception) {
                     SendMessage.warnMessage(value, "Could not get the firework shape '" + this.value.toString() + "'");
                     return false;
                  }
               case "duration":
                  try {
                     Integer.parseInt(this.value.toString());
                     return true;
                  } catch (NumberFormatException exception) {
                     SendMessage.warnMessage(value, Lang.NO_VALID_AMOUNT.get());
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
                  } catch (NumberFormatException exception) {
                     SendMessage.warnMessage(value, Lang.NO_VALID_AMOUNT.get());
                     return false;
                  }

                  if (this.key.equals("stack-size") && !this.plugin.allowIllegalStacks) {
                     if ((Integer)this.value < material.getMaxStackSize()) {
                        SendMessage.warnMessage(
                           value,
                           ChatColor.RED
                              + "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                                 .replace("%material%", material.toString())
                                 .replace("%stack-size%", this.value.toString())
                        );
                     }
                  } else if (!this.key.equals("min-buy") && !this.key.equals("min-sell")) {
                     if (this.key.equals("max-buy") || this.key.equals("max-sell")) {
                        if (this.key.equals("max-buy")) {
                           if (this.plugin.getShopItem(this.section, this.itemLoc).isMinBuy((Integer)this.value)) {
                              SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " should be larger as the min-buy option of this item.");
                              return false;
                           }
                        } else if (this.key.equals("max-sell") && this.plugin.getShopItem(this.section, this.itemLoc).isMinSell((Integer)this.value)) {
                           SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " should be larger as the min-sell option of this item.");
                           return false;
                        }
                     }
                  } else {
                     if ((Integer)this.value < 0) {
                        SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " needs to be a positive value!");
                        return false;
                     }

                     if (this.key.equals("min-buy")) {
                        if ((Integer)this.value < this.plugin.getShopItem(this.section, this.itemLoc).getStackSize()) {
                           SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " should be larger as the stack size of this shop item.");
                           return false;
                        }

                        if (this.plugin.getShopItem(this.section, this.itemLoc).isMaxBuy((Integer)this.value)) {
                           SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " should be smaller as the max-buy option of this item.");
                           return false;
                        }
                     } else if (this.key.equals("min-sell") && this.plugin.getShopItem(this.section, this.itemLoc).isMaxSell((Integer)this.value)) {
                        SendMessage.warnMessage(value, ChatColor.RED + "The " + this.key + " should be smaller as the max-sell option of this item.");
                        return false;
                     }
                  }

                  return true;
               case "instrument":
                  if (material != XMaterial.GOAT_HORN.parseMaterial()) {
                     SendMessage.warnMessage(value, ChatColor.RED + "To apply a horn sound type, the material needs to be a goat horn");
                     return false;
                  }

                  try {
                     CreateItemMethodes.Horn.setInstrument(new ItemStack(Material.GOAT_HORN), this.value.toString());
                     return true;
                  } catch (ItemLoadException exception) {
                     SendMessage.warnMessage(
                        value,
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
                  if (material != XMaterial.MUSHROOM_STEW.parseMaterial()) {
                     SendMessage.warnMessage(value, "To add a stew effect to an item, the material needs to be a 'SUSPICIOUS_STEW'.");
                     return false;
                  }

                  CreateItemMethodes.Stew stew = CreateItemMethodes.Stew.get(this.value.toString());
                  if (stew == null) {
                     SendMessage.warnMessage(
                        value,
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
                  if (!(new ItemStack(material).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(value, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimType(this.value.toString());
                     return true;
                  } catch (ItemLoadException exception) {
                     SendMessage.warnMessage(value, exception.getMessage());
                     return false;
                  }
               case "armor-trim.pattern":
                  if (!(new ItemStack(material).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(value, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimPattern(this.value.toString());
                     return true;
                  } catch (ItemLoadException exception) {
                     SendMessage.warnMessage(value, exception.getMessage());
                     return false;
                  }
               case "permission":
                  if (!this.value.toString().toLowerCase(Locale.ENGLISH).contains("onyxshop")) {
                     return true;
                  }

                  SendMessage.warnMessage(
                     value, "The permission is already prefixed using 'OnyxShop.shop." + this.section + ".item.<permission>', no need to prefix it again."
                  );
                  return false;
               case "buy-prices.prices":
               case "sell-prices.prices":
               case "economy":
                  Double amount = null;
                  if (this.key.equals("buy-prices.prices") || this.key.equals("sell-prices.prices")) {
                     try {
                        amount = this.methods.getPrice(value, this.value.toString().split("::")[1]);
                        if (amount == null) {
                           return false;
                        }

                        this.value = this.value.toString().split("::")[0];
                     } catch (ArrayIndexOutOfBoundsException exception) {
                        SendMessage.warnMessage(value, "A price needs to be specified.");
                        return false;
                     }
                  }

                  if (this.value.toString().equalsIgnoreCase("handItem")) {
                     if (!(value instanceof Player)) {
                        SendMessage.warnMessage(value, Lang.REAL_PLAYER.get());
                        return false;
                     }

                     Player player = (Player)value;
                     if (player.getItemInHand().getType() == Material.AIR) {
                        SendMessage.warnMessage(value, "Item cannot be air");
                        return false;
                     }

                     this.value = "ITEM:" + ItemFormatter.deserialize(player.getItemInHand(), Arrays.asList(values).contains("-unsafe"));
                  } else {
                     EcoType ecoType = EconomyType.getFromString(this.value.toString());
                     if (ecoType == null) {
                        SendMessage.warnMessage(
                           value,
                           "Could not find a economy type such as '"
                              + this.value.toString()
                              + "', valid values are: "
                              + Arrays.stream(EconomyType.values()).map(Enum::name).collect(Collectors.joining(", "))
                        );
                        return false;
                     }

                     if (ecoType.getType() == EconomyType.ITEM) {
                        ItemFormat itemFormat = new ItemFormat(ecoType.getCurrency(), this.section + ".pages." + this.itemLoc + ".economy", 0);
                        if (itemFormat.getItem() == null) {
                           SendMessage.warnMessage(value, itemFormat.getErrorMessage());
                           return false;
                        }
                     }
                  }

                  if (amount != null) {
                     this.value = this.value + "::" + amount;
                  }

                  return true;
               case "skull-texture":
                  if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  try {
                     Base64.getDecoder().decode(this.value.toString());
                     return true;
                  } catch (IllegalArgumentException exception) {
                     SendMessage.warnMessage(value, ChatColor.RED + "Not a valid Base64 string");
                     return false;
                  }
               case "auto-restock-global":
               case "auto-restock-player":
               case "auto-restock-global-sell":
               case "auto-restock-player-sell":
                  try {
                     TimeUtils.getTime(this.value.toString());
                     return true;
                  } catch (ParseException exception) {
                     SendMessage.warnMessage(value, exception.getMessage());
                     return false;
                  }
               case "auto-restock-player-frequency":
               case "auto-restock-global-frequency":
               case "auto-restock-player-sell-frequency":
               case "auto-restock-global-sell-frequency":
                  try {
                     LinkedList values2 = this.plugin.getScheduleManager().validate(this.value.toString());
                     if (values2.isEmpty()) {
                        SendMessage.warnMessage(value, "Invalid cron expression, this schedule will not fire again.");
                        return false;
                     }

                     StringBuilder builder = new StringBuilder();

                     for (int size = 0; size < values2.size(); size++) {
                        builder.append(
                           ChatColor.GRAY
                              + "-"
                              + ChatColor.GREEN
                              + " "
                              + TimeUtils.getDateTime(((Date)values2.get(size)).getTime())
                              + (size != values2.size() ? "\n" : "")
                        );
                     }

                     SendMessage.infoMessage(value, "Loaded cron job with following schedule dates loaded(up to 5):\n" + builder.toString());
                     return true;
                  } catch (RuntimeException exception) {
                     if (!(value instanceof ConsoleCommandSender)) {
                        SendMessage.warnMessage(value, "Invalid cron expression format with reason(See console for more details): " + exception.getMessage());
                     }

                     SendMessage.warnMessage("Invalid cron expression format specified while executing command");
                     exception.printStackTrace();
                     return false;
                  }
               case "action":
                  if (Arrays.stream(ActionItem.Action.values()).noneMatch(element -> element.name().equalsIgnoreCase(this.value.toString()))) {
                     SendMessage.warnMessage(value, Lang.INVALID_ITEM_ACTION.get().replace("%action%", this.value.toString()));
                     return false;
                  }

                  return true;
            }
         }

         return true;
      }
   }

   private List<String> getTabcompletionForValue(String text, String text2, String text3, String text4) {
      if (!this.isSection(text) || !this.isItemLoc(text2) || !this.isAction(text3)) {
         return new ArrayList<>();
      } else if (text3.equalsIgnoreCase("remove") && this.listKeys.contains(text4)) {
         return ConfigManager.getShop(text).getStringList(this.getItemLoc(text, text2) + "." + text4);
      } else if (text3.equalsIgnoreCase("remove")) {
         ArrayList values = new ArrayList();
         values.add(ConfigManager.getShop(text).get(this.getItemLoc(text, text2) + "." + text4, "").toString());
         return values;
      } else {
         switch (text4) {
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
               List<String> values = this.plugin.getEcoHandler().getRegistered().stream().map(element -> {
                  String text5 = element.toString();
                  if (element.getType() == EconomyType.ITEM) {
                     text5 = element.getCurrency();
                  }

                  return text5;
               }).collect(Collectors.toList());
               values.add("ITEM:material:EMERALD name:&a&lGem Stone id:Gems");
               values.add("ITEM:material:GOLD_NUGGET");
               values.add("handItem");
               if (!this.key.equals("buy-prices.prices") && !this.key.equals("sell-prices.prices")) {
                  return values;
               }

               return values.stream().map(element -> element + "::130").collect(Collectors.toList());
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
               return Arrays.stream(FireworkUtil.FireworkColor.values()).map(element -> element.name()).collect(Collectors.toList());
            case "shape":
               return Arrays.stream(Type.values()).map(element -> element.name()).collect(Collectors.toList());
            case "duration":
               return Arrays.asList("1", "2", "3");
            case "stack-size":
               ArrayList values2 = new ArrayList();

               for (int index = 1; index < 64; index++) {
                  values2.add(String.valueOf(index));
               }

               return values2;
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
         ArrayList values = new ArrayList<>(this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc) ? this.actionKeys : this.allKeys);
         ArrayList shopIds = new ArrayList(ConfigManager.getShop(this.section).getConfigurationSection(this.getItemLoc(this.section, this.itemLoc)).getKeys(false));
         if (this.action.equalsIgnoreCase("add")) {
            values.removeAll(shopIds);
            values.addAll(
               this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)
                  ? Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags")
                  : this.listKeys
            );
            return values;
         }

         if (this.action.equalsIgnoreCase("remove")) {
            return shopIds;
         }

         if (this.action.equalsIgnoreCase("set")) {
            values.addAll(
               this.plugin.getSection(this.section).isActionItem(this.section, this.itemLoc)
                  ? Arrays.asList("lore", "displaylore", "enchantments", "potiontypes", "item-flags")
                  : this.listKeys
            );
            return values;
         }
      } catch (NullPointerException exception) {
      }

      return new ArrayList<>();
   }

   private List<String> getAllKeys() {
      ArrayList values = new ArrayList();
      values.add("material");
      values.add("buy");
      values.add("sell");
      values.add("name");
      values.add("displayname");
      values.add("hidePricingLore");
      values.add("slot");
      values.add("stack-size");
      values.add("enchantment-glint");
      values.add("spawnertype");
      values.add("skullowner");
      values.add("armorcolor");
      values.add("duration");
      values.add("armor-trim.pattern");
      values.add("armor-trim.type");
      values.add("flicker");
      values.add("trail");
      values.add("shape");
      values.add("instrument");
      values.add("section");
      values.add("stew-effect");
      values.add("ominous-strength");
      values.add("hidden");
      values.add("max-buy");
      values.add("max-sell");
      values.add("min-buy");
      values.add("min-sell");
      values.add("prior-lore");
      values.add("close-menu");
      values.add("NBTData");
      values.add("buy-prices.require-all");
      values.add("sell-prices.give-all");
      values.add("requireItem");
      values.add("giveItem");
      values.add("level-requirement");
      values.add("skull-texture");
      values.add("economy");
      values.add("permission");
      values.add("hideUtilLore");
      values.add("refill-stock");
      values.add("stock-limit-global");
      values.add("auto-restock-global");
      values.add("auto-restock-global-frequency");
      values.add("stock-limit-player");
      values.add("auto-restock-player");
      values.add("auto-restock-player-frequency");
      values.add("sell-limit-global");
      values.add("auto-restock-global-sell");
      values.add("auto-restock-global-sell-frequency");
      values.add("sell-limit-player");
      values.add("auto-restock-player-sell");
      values.add("auto-restock-player-sell-frequency");
      values.add("action");
      return values;
   }

   private List<String> getListKeys() {
      ArrayList values = new ArrayList();
      values.add("enchantments");
      values.add("potiontypes");
      values.add("lore");
      values.add("displaylore");
      values.add("recipes");
      values.add("colors");
      values.add("fade-colors");
      values.add("buy-commands");
      values.add("sell-commands");
      values.add("click-commands");
      values.add("buy-prices.prices");
      values.add("sell-prices.prices");
      values.add("ignored-tags");
      return values;
   }

   private List<String> getAvailableActions() {
      ArrayList values = new ArrayList();
      values.add("add");
      values.add("set");
      values.add("remove");
      return values;
   }

   private List<String> getExampleNBTTags() {
      ArrayList values = new ArrayList();
      values.add("{CanDestroy:[\"minecraft:diamond_block\",\"#minecraft:iron_block\"]}");
      values.add("{PickupDelay:80}");
      values.add("{CanPlaceOn:[\"minecraft:grass_block\",\"#minecraft:dirt\",\"#minecraft:sand\",\"#minecraft:cobblestone\",\"#minecraft:oak_log\"]}");
      values.add("{Enchantments:[{id:\"9\",lvl:5},{id:\"17\",lvl:3},{id:\"12\",lvl:5},{id:\"13\",lvl:2},{id:\"11\",lvl:5}]}");
      return values;
   }

   private List<String> getExampleCommands() {
      ArrayList values = new ArrayList();
      values.add("/tp <player> <player>");
      values.add("weather clear");
      values.add("time set day");
      values.add("/summon llama ~ ~1 ~");
      return values;
   }

   private List<String> getExamplePlayerNames() {
      ArrayList playerNames = new ArrayList();

      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         playerNames.add(player.getName());
      }

      return playerNames;
   }

   private List<String> getEntityNames() {
      ArrayList values = new ArrayList();

      for (EntityType entityType : EntityType.values()) {
         if (entityType.isAlive()) {
            try {
               EntityType.valueOf(entityType.name());
               values.add(entityType.name());
            } catch (IllegalArgumentException exception) {
            }
         }
      }

      return values;
   }

   private List<String> getValues(String text, String text2, String text3) {
      return ConfigManager.getShop(text).getStringList(this.getItemLoc(text, text2) + "." + text3);
   }

   private void updateOption(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_EDITING_ITEM.get());
      String itemLoc = this.getItemLoc(this.section, this.itemLoc);
      if (this.listKeys.contains(this.key)) {
         if (this.action.equalsIgnoreCase("add")) {
            List sectionIds = this.getValues(this.section, this.itemLoc, this.key);
            sectionIds.add(this.value.toString());
            ConfigManager.getShop(this.section).set(itemLoc + "." + this.key, sectionIds);
         } else if (this.action.equalsIgnoreCase("set")) {
            ConfigManager.getShop(this.section).set(itemLoc + "." + this.key, Collections.singletonList(this.value.toString()));
         } else if (this.action.equalsIgnoreCase("remove")) {
            List itemIds = this.getValues(this.section, this.itemLoc, this.key);
            if (itemIds.size() == 1) {
               ConfigManager.getShop(this.section).set(itemLoc + "." + this.key, null);
            } else if (itemIds.size() >= 1) {
               itemIds.remove(this.value.toString());
               ConfigManager.getShop(this.section).set(itemLoc + "." + this.key, itemIds);
            }
         }
      } else {
         ConfigManager.getShop(this.section).set(itemLoc + "." + this.key, this.value);
      }

      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(value, Lang.EDITSHOP_EDIT_ITEM_SUCCESSFUL.get().replace("%itemPath%", itemLoc));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getItemLoc(String itemId, String text) {
      return (this.plugin.getShopType(itemId) == ShopType.CATEGORY ? "pages." : "") + text;
   }
}
