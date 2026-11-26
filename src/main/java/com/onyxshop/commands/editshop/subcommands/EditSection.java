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

   public EditSection(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
               this.action = var2[2].toLowerCase(Locale.ENGLISH);
               if (!this.actions.contains(this.action)) {
                  SendMessage.sendMessage(
                     var1, Lang.NO_VALID_ACTION.get().replace("%action%", var2[2]).replace("%actions%", Arrays.toString(this.actions.toArray()))
                  );
               } else if (var2.length <= 3) {
                  SendMessage.sendMessage(var1, this.getSyntax());
               } else {
                  this.key = var2[3];
                  if (this.getAvailableKeys().stream().noneMatch(this.key::equalsIgnoreCase)) {
                     SendMessage.sendMessage(
                        var1, Lang.NO_VALID_KEY.get().replace("%key%", var2[3]).replace("%keys%", Arrays.toString(this.getAvailableKeys().toArray()))
                     );
                  } else if (this.action.equalsIgnoreCase("remove") && this.listKeys.stream().noneMatch(this.key::equalsIgnoreCase)) {
                     this.value = null;
                     if (this.validate(var1, var2)) {
                        this.updateOption(var1);
                     }
                  } else if (var2.length <= 4) {
                     SendMessage.sendMessage(var1, this.getSyntax());
                  } else {
                     StringBuilder var3 = new StringBuilder();

                     for (int var4 = 4; var4 < var2.length; var4++) {
                        var3.append(var2[var4]).append(" ");
                     }

                     this.value = var3.substring(0, var3.length() - 1);
                     if (this.validate(var1, var2)) {
                        this.updateOption(var1);
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
               ArrayList var5 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var5);
               Collections.sort(var5);
               return var5;
            }

            return this.plugin.getShopSections();
         case 3:
            if (!var1[2].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], this.actions, var4);
               Collections.sort(var4);
               return var4;
            }

            return this.actions;
         case 4:
            if (this.isSection(var1[1]) && this.isAction(var1[2])) {
               if (!var1[3].isEmpty()) {
                  ArrayList var3 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[3], this.getAvailableKeys(), var3);
                  Collections.sort(var3);
                  return var3;
               }

               return this.getAvailableKeys();
            }

            return null;
         case 5:
            if (!var1[4].isEmpty()) {
               ArrayList var2 = new ArrayList();
               StringUtil.copyPartialMatches(var1[4], this.getTabcompletionForValue(var1[1], var1[2], var1[3]), var2);
               Collections.sort(var2);
               return var2;
            }

            return this.getTabcompletionForValue(var1[1], var1[2], var1[3]);
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

   private boolean isAction(String var1) {
      if (this.actions.contains(var1)) {
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

   private List<String> getExamplePlayerNames() {
      ArrayList var1 = new ArrayList();
      this.plugin.getServer().getOnlinePlayers().forEach(var1x -> var1.add(var1x.getName()));
      return var1;
   }

   private List<String> getTabcompletionForValue(String var1, String var2, String var3) {
      if (!this.isSection(var1) || !this.isAction(var2) || !this.isKey(var3)) {
         return new ArrayList<>();
      } else if (!var2.equals("remove") || !this.listKeys.contains(var3)) {
         switch (var3) {
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
               ArrayList var6 = new ArrayList();

               for (int var10 = 1; var10 < 64; var10++) {
                  var6.add(String.valueOf(var10));
               }

               return var6;
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
               if (var3.startsWith("pages")) {
                  String[] var7 = var3.split("\\.");
                  if (var7.length >= 2) {
                     String var8 = var7[2];
                     switch (var8) {
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
         return this.listKeys.contains(var3) ? this.getValues(var1, var3) : Collections.singletonList(ConfigManager.getSection(var1).get(var3, "").toString());
      }
   }

   private boolean validate(Object var1, String[] var2) {
      if (!this.action.equalsIgnoreCase("remove")
         || this.listKeys.contains(this.key)
         || !this.key.equalsIgnoreCase("item.material") && !this.key.equalsIgnoreCase("enable")) {
         if (this.value != null) {
            String var3 = ConfigManager.getSection(this.section).getString("item.material");
            if (var3 == null || var3.isEmpty()) {
               SendMessage.warnMessage(var1, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", var3));
               return false;
            }

            Optional var4 = XMaterial.matchXMaterial(var3);
            if (!var4.isPresent() || ((XMaterial)var4.get()).parseItem() == null) {
               SendMessage.warnMessage(var1, Lang.EDIT_ITEM_MATERIAL_INVALID.get().replace("%material%", var3));
               return false;
            }

            Material var5 = ((XMaterial)var4.get()).parseItem().getType();
            String var6 = this.key;
            switch (var6) {
               case "item.material":
                  return this.methods.getMaterial(var1, this.value.toString()) != null;
               case "item.name":
               case "item.displayname":
               case "item.lore":
               case "title":
               case "click-commands":
                  return true;
               case "slot":
                  this.value = this.methods.getMainMenuSlot(var1, this.value.toString());
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
                  } catch (IllegalArgumentException var18) {
                     SendMessage.warnMessage(var1, Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", this.key));
                     return false;
                  }
               case "item.stack-size":
                  try {
                     this.value = Integer.parseInt(this.value.toString());
                  } catch (NumberFormatException var17) {
                     SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
                     return false;
                  }

                  if (this.key.equalsIgnoreCase("item.stack-size") && !this.plugin.allowIllegalStacks && (Integer)this.value < var5.getMaxStackSize()) {
                     SendMessage.warnMessage(
                        var1,
                        "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                           .replace("%material%", var5.toString())
                           .replace("%stack-size%", this.value.toString())
                     );
                     return false;
                  }

                  return true;
               case "item.potiontypes":
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
               case "item.armor-trim.type":
                  if (!(new ItemStack(var5).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(var1, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimType(this.value.toString());
                     return true;
                  } catch (ItemLoadException var16) {
                     SendMessage.warnMessage(var1, var16.getMessage());
                     return false;
                  }
               case "item.armor-trim.pattern":
                  if (!(new ItemStack(var5).getItemMeta() instanceof ArmorMeta)) {
                     SendMessage.warnMessage(var1, "To add a armor trim to an item, the material needs to be a armor piece.");
                     return false;
                  }

                  try {
                     new CreateItemMethodes.ArmorTrim().getTrimPattern(this.value.toString());
                     return true;
                  } catch (ItemLoadException var15) {
                     SendMessage.warnMessage(var1, var15.getMessage());
                     return false;
                  }
               case "economy":
                  if (this.value.toString().equalsIgnoreCase("handItem")) {
                     if (var1 instanceof Player) {
                        Player var20 = (Player)var1;
                        if (var20.getItemInHand().getType() != Material.AIR) {
                           this.value = "ITEM:" + ItemFormatter.deserialize(var20.getItemInHand(), Arrays.asList(var2).contains("-unsafe"));
                           return true;
                        }

                        SendMessage.warnMessage(var1, "Item cannot be air!");
                        return false;
                     }

                     SendMessage.warnMessage(var1, Lang.REAL_PLAYER.get());
                     return false;
                  }

                  EcoType var9 = EconomyType.getFromString(this.value.toString());
                  if (var9 == null) {
                     SendMessage.warnMessage(
                        var1,
                        "Could not find a economy type such as '"
                           + this.value.toString()
                           + "', valid values are: "
                           + Arrays.stream(EconomyType.values()).map(Enum::name).collect(Collectors.joining(", "))
                     );
                     return false;
                  }

                  if (var9.getType() == EconomyType.ITEM) {
                     ItemFormat var21 = new ItemFormat(var9.getCurrency(), this.section + ".economy", 0);
                     if (var21.getItem() == null) {
                        SendMessage.warnMessage(var1, var21.getErrorMessage());
                        return false;
                     }
                  }

                  return true;
               case "item.skullowner":
                  if (var5 != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  return true;
               case "item.skull-texture":
                  if (var5 != XMaterial.PLAYER_HEAD.parseMaterial()) {
                     SendMessage.warnMessage(var1, Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                     return false;
                  }

                  try {
                     Base64.getDecoder().decode(this.value.toString());
                     return true;
                  } catch (IllegalArgumentException var14) {
                     SendMessage.warnMessage(var1, "Not a valid Base64 string");
                     return false;
                  }
               case "item.NBTData":
                  if (Character.toString(this.value.toString().charAt(0)).equals("{")
                     && Character.toString(this.value.toString().charAt(this.value.toString().length() - 1)).equals("}")) {
                     return true;
                  }

                  SendMessage.warnMessage(var1, Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
                  return false;
               default:
                  if (this.key.startsWith("pages")) {
                     String[] var10 = this.key.split("\\.");
                     if (var10.length >= 2) {
                        String var11 = var10[2];
                        switch (var11) {
                           case "gui-rows":
                              try {
                                 this.value = Integer.parseInt(this.value.toString());
                              } catch (NumberFormatException var19) {
                                 SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
                                 return false;
                              }

                              if ((Integer)this.value >= 1 && (Integer)this.value <= 6) {
                                 return true;
                              }

                              SendMessage.warnMessage(var1, "Invalid inventory rows amount, should be a number between 1-6");
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
         SendMessage.sendMessage(var1, Lang.ACTION_NOT_VALID_ON_KEY.get().replace("%action%", this.action).replace("%key%", this.key));
         return false;
      }
   }

   private List<String> getAvailableKeys() {
      try {
         ArrayList var1 = new ArrayList<>(this.allKeys);
         if (this.action.equalsIgnoreCase("add")) {
            var1.removeAll(this.getDefined());
            var1.addAll(this.listKeys);
            return var1;
         }

         if (this.action.equalsIgnoreCase("remove")) {
            List var4 = this.getDefined();
            var4.addAll(this.getPageKeys());
            return var4;
         }

         if (this.action.equalsIgnoreCase("set")) {
            ArrayList var2 = new ArrayList<>(this.allKeys);
            var2.addAll(this.getPageKeys());
            var2.addAll(this.listKeys);
            return var2;
         }
      } catch (NullPointerException var3) {
      }

      return new ArrayList<>();
   }

   private List<String> getDefined() {
      ArrayList var1 = new ArrayList(ConfigManager.getSection(this.section).getKeys(false));
      if (var1.contains("item")) {
         var1.addAll(ConfigManager.getSection(this.section).getConfigurationSection("item").getKeys(false));
      }

      return var1;
   }

   private List<String> getPageKeys() {
      ArrayList var1 = new ArrayList();

      for (String var3 : this.plugin.getConfigManager().getPages(this.section, true)) {
         var1.addAll(Arrays.asList("pages." + var3 + ".title", "pages." + var3 + ".gui-rows"));
      }

      return var1;
   }

   private List<String> getValues(String var1, String var2) {
      return ConfigManager.getSection(var1).getStringList(var2);
   }

   private List<String> getExampleNBTTags() {
      ArrayList var1 = new ArrayList();
      var1.add("{CanDestroy:[\"minecraft:diamond_block\",\"#minecraft:iron_block\"]}");
      var1.add("{PickupDelay:80}");
      var1.add("{CanPlaceOn:[\"minecraft:grass_block\",\"#minecraft:dirt\",\"#minecraft:sand\",\"#minecraft:cobblestone\",\"#minecraft:oak_log\"]}");
      var1.add("{Enchantments:[{id:\"9\",lvl:5},{id:\"17\",lvl:3},{id:\"12\",lvl:5},{id:\"13\",lvl:2},{id:\"11\",lvl:5}]}");
      return var1;
   }

   private void updateOption(Object var1) {
      SendMessage.sendMessage(var1, Lang.EDITSHOP_EDITING_SECTION.get());
      if (this.listKeys.contains(this.key)) {
         if (this.action.equals("add")) {
            List var2 = this.getValues(this.section, this.key);
            var2.add(this.value.toString());
            this.update(var2);
         } else if (this.action.equalsIgnoreCase("set")) {
            this.update(Collections.singletonList(this.value.toString()));
         } else if (this.action.equalsIgnoreCase("remove")) {
            List var3 = this.getValues(this.section, this.key);
            if (var3.size() == 1) {
               this.update(null);
            } else if (var3.size() >= 1) {
               var3.remove(this.value.toString());
               this.update(var3);
            }
         }
      } else {
         this.update(this.value);
      }

      SendMessage.sendMessage(var1, Lang.EDITSHOP_EDIT_ITEM_SUCCESSFUL.get().replace("%itemPath%", this.key));
      SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private <O> void update(O var1) {
      if (this.key.startsWith("pages")) {
         ConfigManager.getShop(this.section).set(this.key, var1);
         ConfigManager.saveShop(this.section);
      } else {
         ConfigManager.getSection(this.section).set(this.key, var1);
         ConfigManager.saveSection(this.section);
      }
   }
}
