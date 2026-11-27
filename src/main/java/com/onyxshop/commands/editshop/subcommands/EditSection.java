package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
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
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.ItemFormat;
import com.onyxshop.util.ItemFormatter;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.util.StringUtil;

public class EditSection extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private final List<String> allKeys = Arrays.asList(
      "enable",
      "item.material",
      "item.name",
      "item.displayname",
      "item.stack-size",
      "item.hideDefaultLore",
      "item.armor-trim.type",
      "item.armor-trim.pattern",
      "slot",
      "item.enchantment-glint",
      "item.skullowner",
      "item.armorcolor",
      "hidden",
      "title",
      "sub-section",
      "item.potion-glow",
      "economy",
      "item.skull-texture",
      "display-item",
      "item.NBTData"
   );
   private final List<String> listKeys = Arrays.asList("item.lore", "click-commands", "item.potiontypes");
   private final List<String> actions = Arrays.asList("add", "set", "remove");
   private String section;
   private String action;
   private String key;
   private Object value;

   public EditSection(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "editsection";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_EDIT_SECTION_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_EDIT_SECTION_SUBCOMMAND_SYNTAX.get().getLegacy();
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
               this.action = values[2].toLowerCase(Locale.ENGLISH);
               if (!this.actions.contains(this.action)) {
                  SendMessage.sendMessage(
                     value, Lang.NO_VALID_ACTION.get().replace("%action%", values[2]).replace("%actions%", Arrays.toString(this.actions.toArray()))
                  );
               } else if (values.length <= 3) {
                  SendMessage.sendMessage(value, this.getSyntax());
               } else {
                  this.key = values[3];
                  if (this.getAvailableKeys().stream().noneMatch(this.key::equalsIgnoreCase)) {
                     SendMessage.sendMessage(
                        value, Lang.NO_VALID_KEY.get().replace("%key%", values[3]).replace("%keys%", Arrays.toString(this.getAvailableKeys().toArray()))
                     );
                  } else if (this.action.equalsIgnoreCase("remove") && this.listKeys.stream().noneMatch(this.key::equalsIgnoreCase)) {
                     this.value = null;
                     if (this.validate(value, values)) {
                        this.updateOption(value);
                     }
                  } else if (values.length <= 4) {
                     SendMessage.sendMessage(value, this.getSyntax());
                  } else {
                     StringBuilder builder = new StringBuilder();

                     for (int index = 4; index < values.length; index++) {
                        builder.append(values[index]).append(" ");
                     }

                     this.value = builder.substring(0, builder.length() - 1);
                     if (this.validate(value, values)) {
                        this.updateOption(value);
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
            if (!values[2].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], this.actions, values2);
               Collections.sort(values2);
               return values2;
            }

            return this.actions;
         case 4:
            if (this.isSection(values[1]) && this.isAction(values[2])) {
               if (!values[3].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[3], this.getAvailableKeys(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.getAvailableKeys();
            }

            return null;
         case 5:
            if (!values[4].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[4], this.getTabcompletionForValue(values[1], values[2], values[3]), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.getTabcompletionForValue(values[1], values[2], values[3]);
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

   private boolean isAction(String text) {
      if (this.actions.contains(text)) {
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

   private List<String> getExamplePlayerNames() {
      ArrayList playerNames = new ArrayList();
      this.plugin.getServer().getOnlinePlayers().forEach(entry -> playerNames.add(entry.getName()));
      return playerNames;
   }

   private List<String> getTabcompletionForValue(String text, String text2, String text3) {
      if (!this.isSection(text) || !this.isAction(text2) || !this.isKey(text3)) {
         return new ArrayList<>();
      } else if (!text2.equals("remove") || !this.listKeys.contains(text3)) {
         switch (text3) {
            case "item.material":
               return this.plugin.getSupportedMatNames();
            case "item.name":
            case "item.displayname":
            case "title":
            case "item.lore":
               return Arrays.asList(
                  "#A1672D&lWood",
                  "&1&lArmor",
                  "#1B22E8&lF#1BAAE8&li#1BE8E1&lr#1BE896&le#1BE829&lw#E8DA1B&lo#E88F1B&lr#EB0909&lk#EB09DC&ls",
                  "&7&lStones",
                  "#09EBDC&lCustom#89EB09&lItems"
               );
            case "item.armorcolor":
               return this.methods.getExampleRGBColors();
            case "item.stack-size":
               ArrayList values = new ArrayList();

               for (int index = 1; index < 64; index++) {
                  values.add(String.valueOf(index));
               }

               return values;
            case "item.potiontypes":
               return PotionTypes.getNames();
            case "item.skullowner":
               return this.getExamplePlayerNames();
            case "item.enchantment-glint":
            case "item.potion-glow":
            case "item.hideDefaultLore":
            case "sub-section":
            case "display-item":
               return Arrays.asList("true", "false");
            case "slot":
               return this.methods.getAvailableMainMenuSlots();
            case "economy":
               return Arrays.asList(
                  "ITEM:Gold_Nugget",
                  "ITEM:Diamond",
                  "ULTRA_ECONOMY:Gems",
                  "ULTRA_ECONOMY:Crystals",
                  "VAULT",
                  "GEMS_ECONOMY:Sticks",
                  "PLAYER_POINTS",
                  "handItem"
               );
            case "item.skull-texture":
               return Arrays.asList(
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRjZjJjMTk4ZjYxZGY1YTZkMGJhOTdkYmY5MGMzMzc5OTU0MDVjMTczOTZjMDE2Yzg1ZjZmM2ZlYTUyYzkwNiJ9fX0=",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ5N2IxNDdjZmFlNTIyMDU1OTdmNzJlM2M0ZWY1MjUxMmU5Njc3MDIwZTRiNGZhNzUxMmMzYzZhY2RkOGMxIn19fQ==",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ5NjU4OWZiNWMxZjY5Mzg3YjdmYjE3ZDkyMzEyMDU4ZmY2ZThlYmViM2ViODllNGY3M2U3ODE5NjExM2IifX19"
               );
            case "item.NBTData":
               return this.getExampleNBTTags();
            case "click-commands":
               return Arrays.asList("minecraft:give %player% diamond", "/msg %player% You just clicked me", "o:/i cobblestone 64");
            case "item.enchantments":
               return this.plugin.enchantsProvider.getAvailableEnchantKeys();
            case "item.armor-trim.type":
               return new CreateItemMethodes.ArmorTrim().getTypes();
            case "item.armor-trim.pattern":
               return new CreateItemMethodes.ArmorTrim().getPatterns();
            default:
               if (text3.startsWith("pages")) {
                  String[] values2 = text3.split("\\.");
                  if (values2.length >= 2) {
                     String text4 = values2[2];
                     switch (text4) {
                        case "gui-rows":
                           return Arrays.asList("1", "2", "3", "4", "5", "6");
                        case "title":
                           return Arrays.asList("&8&lTools", "&d&lWools", "#34ebb4Utilities", "&f&lBuildingBlocks", "<blue><bold>TradingItems");
                     }
                  }
               }

               return new ArrayList<>();
         }
      } else {
         return this.listKeys.contains(text3) ? this.getValues(text, text3) : Collections.singletonList(ConfigManager.getSection(text).get(text3, "").toString());
      }
   }

   private boolean validate(Object value, String[] values) {
      if (!this.action.equalsIgnoreCase("remove")
         || this.listKeys.contains(this.key)
         || !this.key.equalsIgnoreCase("item.material") && !this.key.equalsIgnoreCase("enable")) {
         if (this.value != null) {
            String text = ConfigManager.getSection(this.section).getString("item.material");
            if (text == null || text.isEmpty()) {
               SendMessage.warnMessage(value, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", text));
               return false;
            }

            Optional optional = XMaterial.matchXMaterial(text);
            if (!optional.isPresent() || ((XMaterial)optional.get()).parseItem() == null) {
               SendMessage.warnMessage(value, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", text));
               return false;
            }

            Material material = ((XMaterial)optional.get()).parseItem().getType();
            String key = this.key;
            switch (key) {
               case "item.material":
                  return this.methods.getMaterial(value, this.value.toString()) != null;
               case "item.name":
               case "item.displayname":
               case "item.lore":
               case "title":
               case "click-commands":
                  return true;
               case "slot":
                  this.value = this.methods.getMainMenuSlot(value, this.value.toString());
                  return this.value != null;
               case "item.enchantment-glint":
               case "item.potion-glow":
               case "item.hideDefaultLore":
               case "sub-section":
               case "display-item":
               case "hidden":
               case "enable":
                  this.value = Boolean.valueOf(this.value.toString());
                  return true;
               case "item.armorcolor":
                  try {
                     Color.fromRGB(Integer.parseInt(this.value.toString()));
                     return true;
                  } catch (IllegalArgumentException exception) {
                     SendMessage.warnMessage(value, Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", this.key));
                     return false;
                  }
               case "item.stack-size":
                  try {
                     this.value = Integer.parseInt(this.value.toString());
                  } catch (NumberFormatException exception) {
                     SendMessage.warnMessage(value, Lang.NO_VALID_AMOUNT.get());
                     return false;
                  }

                  if (this.key.equalsIgnoreCase("item.stack-size") && !this.plugin.allowIllegalStacks && (Integer)this.value < material.getMaxStackSize()) {
                     SendMessage.warnMessage(
                        value,
                        "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                           .replace("%material%", material.toString())
                           .replace("%stack-size%", this.value.toString())
                     );
                     return false;
                  }

                  return true;
               case "item.potiontypes":
                  if (!PotionTypes.canHaveEffects(material)) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
                     return false;
                  }

                  Optional optional2 = PotionTypes.matchPotionType(this.value.toString());
                  if (!optional2.isPresent()) {
                     SendMessage.warnMessage(value, Lang.ITEM_POTIONTYPE_NULL.get());
                     return false;
                  }

                  if (!((PotionTypes)optional2.get()).isSupported()) {
                     SendMessage.warnMessage(value, Lang.POTIONTYPE_NOT_SUPPORTED.get());
                     return false;
                  }

                  return true;
               case "item.armor-trim.type":
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
               case "item.armor-trim.pattern":
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
               case "economy":
                  if (this.value.toString().equalsIgnoreCase("handItem")) {
                     if (value instanceof Player) {
                        Player player = (Player)value;
                        if (player.getItemInHand().getType() != Material.AIR) {
                           this.value = "ITEM:" + ItemFormatter.deserialize(player.getItemInHand(), Arrays.asList(values).contains("-unsafe"));
                           return true;
                        }

                        SendMessage.warnMessage(value, "Item cannot be air!");
                        return false;
                     }

                     SendMessage.warnMessage(value, Lang.REAL_PLAYER.get());
                     return false;
                  }

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
                     ItemFormat itemFormat = new ItemFormat(ecoType.getCurrency(), this.section + ".economy", 0);
                     if (itemFormat.getItem() == null) {
                        SendMessage.warnMessage(value, itemFormat.getErrorMessage());
                        return false;
                     }
                  }

                  return true;
               case "item.skullowner":
                  if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  return true;
               case "item.skull-texture":
                  if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(value, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  try {
                     Base64.getDecoder().decode(this.value.toString());
                     return true;
                  } catch (IllegalArgumentException exception) {
                     SendMessage.warnMessage(value, "Not a valid Base64 string");
                     return false;
                  }
               case "item.NBTData":
                  if (Character.toString(this.value.toString().charAt(0)).equals("{")
                     && Character.toString(this.value.toString().charAt(this.value.toString().length() - 1)).equals("}")) {
                     return true;
                  }

                  SendMessage.warnMessage(value, Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
                  return false;
               default:
                  if (this.key.startsWith("pages")) {
                     String[] keys = this.key.split("\\.");
                     if (keys.length >= 2) {
                        String text2 = keys[2];
                        switch (text2) {
                           case "gui-rows":
                              try {
                                 this.value = Integer.parseInt(this.value.toString());
                              } catch (NumberFormatException exception) {
                                 SendMessage.warnMessage(value, Lang.NO_VALID_AMOUNT.get());
                                 return false;
                              }

                              if ((Integer)this.value >= 1 && (Integer)this.value <= 6) {
                                 return true;
                              }

                              SendMessage.warnMessage(value, "Invalid inventory rows amount, should be a number between 1-6");
                              return false;
                           case "title":
                              return true;
                        }
                     }
                  }
            }
         }

         return true;
      } else {
         SendMessage.sendMessage(value, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
         return false;
      }
   }

   private List<String> getAvailableKeys() {
      try {
         ArrayList values = new ArrayList<>(this.allKeys);
         if (this.action.equalsIgnoreCase("add")) {
            values.removeAll(this.getDefined());
            values.addAll(this.listKeys);
            return values;
         }

         if (this.action.equalsIgnoreCase("remove")) {
            List defined = this.getDefined();
            defined.addAll(this.getPageKeys());
            return defined;
         }

         if (this.action.equalsIgnoreCase("set")) {
            ArrayList values2 = new ArrayList<>(this.allKeys);
            values2.addAll(this.getPageKeys());
            values2.addAll(this.listKeys);
            return values2;
         }
      } catch (NullPointerException exception) {
      }

      return new ArrayList<>();
   }

   private List<String> getDefined() {
      ArrayList sectionIds = new ArrayList(ConfigManager.getSection(this.section).getKeys(false));
      if (sectionIds.contains("item")) {
         sectionIds.addAll(ConfigManager.getSection(this.section).getConfigurationSection("item").getKeys(false));
      }

      return sectionIds;
   }

   private List<String> getPageKeys() {
      ArrayList values = new ArrayList();

      for (String sectionId : this.plugin.getConfigManager().getPages(this.section, true)) {
         values.addAll(Arrays.asList("pages." + sectionId + ".title", "pages." + sectionId + ".gui-rows"));
      }

      return values;
   }

   private List<String> getValues(String text, String text2) {
      return ConfigManager.getSection(text).getStringList(text2);
   }

   private List<String> getExampleNBTTags() {
      ArrayList values = new ArrayList();
      values.add("{CanDestroy:[\"minecraft:diamond_block\",\"#minecraft:iron_block\"]}");
      values.add("{PickupDelay:80}");
      values.add("{CanPlaceOn:[\"minecraft:grass_block\",\"#minecraft:dirt\",\"#minecraft:sand\",\"#minecraft:cobblestone\",\"#minecraft:oak_log\"]}");
      values.add("{Enchantments:[{id:\"9\",lvl:5},{id:\"17\",lvl:3},{id:\"12\",lvl:5},{id:\"13\",lvl:2},{id:\"11\",lvl:5}]}");
      return values;
   }

   private void updateOption(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_EDITING_SECTION.get());
      if (this.listKeys.contains(this.key)) {
         if (this.action.equals("add")) {
            List sectionIds = this.getValues(this.section, this.key);
            sectionIds.add(this.value.toString());
            this.update(sectionIds);
         } else if (this.action.equalsIgnoreCase("set")) {
            this.update(Collections.singletonList(this.value.toString()));
         } else if (this.action.equalsIgnoreCase("remove")) {
            List keys = this.getValues(this.section, this.key);
            if (keys.size() == 1) {
               this.update(null);
            } else if (keys.size() >= 1) {
               keys.remove(this.value.toString());
               this.update(keys);
            }
         }
      } else {
         this.update(this.value);
      }

      SendMessage.sendMessage(value, Lang.EDITSHOP_EDIT_ITEM_SUCCESSFUL.get().replace("%itemPath%", this.key));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private <O> void update(O value) {
      if (this.key.startsWith("pages")) {
         ConfigManager.getShop(this.section).set(this.key, value);
         ConfigManager.saveShop(this.section);
      } else {
         ConfigManager.getSection(this.section).set(this.key, value);
         ConfigManager.saveSection(this.section);
      }
   }
}
