package com.onyxshop.methodes;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.onyxshop.util.exceptions.ConfigSaveException;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.config.Config;
import com.onyxshop.inventorys.editor.items.AvailableActions;
import com.onyxshop.inventorys.editor.items.AvailableOptions;
import com.onyxshop.inventorys.editor.items.ChooseValue;
import com.onyxshop.inventorys.editor.items.ConclusionScreen;
import com.onyxshop.inventorys.editor.items.DynamicPricingOptions;
import com.onyxshop.inventorys.editor.items.FireworkOptions;
import com.onyxshop.inventorys.editor.items.IndexChooser;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.types.ShopCategory;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.ConvFactory;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

public class ItemEditor {
   private final ConvFactory convFactory = new ConvFactory();
   private final OnyxShop plugin;
   public final List<String> allListKeys = Arrays.asList(
      "enchantments", "potiontypes", "buy-commands", "sell-commands", "recipes", "lore", "displaylore", "colors", "fade-colors"
   );

   public ItemEditor(OnyxShop var1) {
      this.plugin = var1;
   }

   public ItemStack getSelectedItem(ShopItem var1) {
      ItemStack var2 = new ItemStack(var1.getShopItem());
      if (!var1.hasItemError() && !var1.isDisplayItem()) {
         ArrayList var3 = new ArrayList();
         var3.add(Lang.BUY_PRICE.get().getLegacy().replace("%amounttopay%", this.plugin.formatPrice(var1.getEcoType(), var1.getBuyPrice())));
         var3.add(Lang.SELL_PRICE.get().getLegacy().replace("%amounttopay%", this.plugin.formatPrice(var1.getEcoType(), var1.getSellPrice())));
         ItemMeta var4 = var2.getItemMeta();
         var4.setLore(var3);
         var2.setItemMeta(var4);
      }

      return var2;
   }

   public void saveFireworkOptions(Inventory var1) {
      boolean var2 = false;
      String var3 = ((FireworkOptions)var1.getHolder()).getItem().section;
      String var4 = ((FireworkOptions)var1.getHolder()).getItem().itemLoc;
      int var5 = Integer.parseInt(this.plugin.versionHandler.getOnyxNbtString(var1.getItem(0), "value"));
      if (var5 != ConfigManager.getShop(var3).getInt("pages." + var4 + ".duration", 1)) {
         ConfigManager.getShop(var3).set("pages." + var4 + ".duration", var5);
         var2 = true;
      }

      boolean var6 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(var1.getItem(3), "value"));
      if (var6 != ConfigManager.getShop(var3).getBoolean("pages." + var4 + ".flicker")) {
         ConfigManager.getShop(var3).set("pages." + var4 + ".flicker", var6);
         var2 = true;
      }

      boolean var7 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(var1.getItem(4), "value"));
      if (var7 != ConfigManager.getShop(var3).getBoolean("pages." + var4 + ".trail")) {
         ConfigManager.getShop(var3).set("pages." + var4 + ".trail", var7);
         var2 = true;
      }

      String var8 = this.plugin.versionHandler.getOnyxNbtString(var1.getItem(5), "value");
      if (!var8.equals(ConfigManager.getShop(var3).getString("pages." + var4 + ".shape", "BALL"))) {
         ConfigManager.getShop(var3).set("pages." + var4 + ".shape", var8);
         var2 = true;
      }

      if (var2) {
         ConfigManager.saveShop(var3);
         this.plugin.getSection(var3).reloadItem(var3 + "." + var4);
      }
   }

   public void saveDynamicPricingOptions(Inventory var1) {
      boolean var2 = false;
      String var3 = ((DynamicPricingOptions)var1.getHolder()).getItem().section;
      String var4 = ((DynamicPricingOptions)var1.getHolder()).getItem().itemLoc;
      boolean var5 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(var1.getItem(0), "value"));
      if (var5 != ConfigManager.getShop(var3).getBoolean("pages." + var4 + "." + ItemEditor.Option.DYNAMIC_PRICING_ENABLE.getKey(), this.plugin.dynamicPricing)
         )
       {
         ConfigManager.getShop(var3).set("pages." + var4 + "." + ItemEditor.Option.DYNAMIC_PRICING_ENABLE.getKey(), var5);
         var2 = true;
      }

      if (var2) {
         ConfigManager.saveShop(var3);
         this.plugin.getSection(var3).reloadItem(var3 + "." + var4);
      }
   }

   public void saveUnsavedOptions(Inventory var1) {
      boolean var2 = false;
      ShopItem var3 = ((AvailableOptions)var1.getHolder()).getItem();
      boolean var4 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(var1.getItem(10), "value"));
      if (var4 != ConfigManager.getShop(var3.section).getBoolean("pages." + var3.itemLoc + ".enchantment-glint")) {
         ConfigManager.getShop(var3.section).set("pages." + var3.itemLoc + ".enchantment-glint", var4);
         var2 = true;
      }

      if (var2) {
         ConfigManager.saveShop(var3.section);
         this.plugin.getSection(var3.section).reloadItem(var3.getItemPath());
      }
   }

   public ItemStack toggleOption(String var1, ItemEditor.Option var2) {
      ItemStack var3 = null;
      switch (var2) {
         case ENCHANTMENT_GLINT:
            var1 = var1.equals("true") ? "false" : "true";
            ItemBuilder var4 = new ItemBuilder(var1.equals("true") ? XMaterial.ENCHANTED_BOOK.parseItem() : XMaterial.BOOK.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", var2.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", var2.getName())
                        .builder()
                        .append(var1.equals("true") ? ChatColor.GREEN + var1 : ChatColor.RED + var1)
                        .build()
                  )
               );
            var3 = var4.build();
            break;
         case FIREWORK_DURATION:
            var1 = String.valueOf(var1.equals("1") ? 2 : (var1.equals("2") ? 3 : 1));
            ItemBuilder var8 = new ItemBuilder(XMaterial.GUNPOWDER.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", var2.getName()))
               .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", var2.getName()) + ChatColor.GREEN + var1)
               .setAmount(Integer.parseInt((String)var1));
            var3 = var8.build();
            break;
         case FIREWORK_FLICKER:
            var1 = var1.equals("true") ? "false" : "true";
            ItemBuilder var6 = new ItemBuilder(var1.equals("true") ? XMaterial.GLOWSTONE_DUST.parseItem() : XMaterial.GUNPOWDER.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", var2.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", var2.getName())
                        .builder()
                        .append(var1.equals("true") ? ChatColor.GREEN + var1 : ChatColor.RED + var1)
                        .build()
                  )
               );
            var3 = var6.build();
            break;
         case FIREWORK_TRAIL:
            var1 = var1.equals("true") ? "false" : "true";
            ItemBuilder var7 = new ItemBuilder(var1.equals("true") ? XMaterial.DIAMOND_BLOCK.parseItem() : XMaterial.DIAMOND.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", var2.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", var2.getName())
                        .builder()
                        .append(var1.equals("true") ? ChatColor.GREEN + var1 : ChatColor.RED + var1)
                        .build()
                  )
               );
            var3 = var7.build();
            break;
         case FIREWORK_SHAPE:
            var1 = var1.equalsIgnoreCase("BALL_LARGE")
               ? "STAR"
               : (
                  var1.equalsIgnoreCase("STAR")
                     ? "BURST"
                     : (var1.equalsIgnoreCase("BURST") ? "CREEPER" : (var1.equalsIgnoreCase("CREEPER") ? "BALL" : "BALL_LARGE"))
               );
            ItemBuilder var9 = new ItemBuilder(
                  var1.equalsIgnoreCase("BALL_LARGE")
                     ? XMaterial.FIRE_CHARGE.parseItem()
                     : (
                        var1.equalsIgnoreCase("STAR")
                           ? XMaterial.GOLD_NUGGET.parseItem()
                           : (
                              var1.equalsIgnoreCase("BURST")
                                 ? XMaterial.FEATHER.parseItem()
                                 : (var1.equalsIgnoreCase("CREEPER") ? XMaterial.CREEPER_HEAD.parseItem() : XMaterial.FIREWORK_STAR.parseItem())
                           )
                     )
               )
               .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", var2.getName()))
               .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", var2.getName()) + ChatColor.GREEN + var1);
            var3 = var9.build();
            break;
         case DYNAMIC_PRICING_ENABLE:
            var1 = var1.equals("true") ? "false" : "true";
            ItemBuilder var5 = new ItemBuilder(XMaterial.LEVER.parseItem())
               .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DYNAMIC_PRICING_ENABLE.get()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", Lang.OPTION_DYNAMIC_PRICING.get())
                        .builder()
                        .append(var1.equals("true") ? ChatColor.GREEN + var1 : ChatColor.RED + var1)
                        .build()
                  )
               );
            if (var1.equals("true")) {
               var5.withEnchantGlint();
            }

            var3 = var5.build();
      }

      return this.setEditOption(this.plugin.versionHandler.setOnyxNbtString(var3, "value", (String)var1), var2);
   }

   public List<ItemStack> getAllKeys(String var1, String var2) {
      ShopItem var3 = this.plugin.getShopItem(var1, var2);
      Config var4 = ConfigManager.getShop(var1);
      String var5 = "pages." + var2;
      ArrayList var6 = new ArrayList();
      ItemBuilder var7 = new ItemBuilder(XMaterial.GRASS_BLOCK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_MATERIAL.get()));
      if (var4.getString(var5 + ".material") != null && !var4.getString(var5 + ".material").isEmpty()) {
         var7.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_MATERIAL.get()).builder().color('a').append(var4.getString(var5 + ".material")).build()
            )
         );
      }

      var6.add(this.setEditOption(var7.build(), ItemEditor.Option.MATERIAL));
      ItemBuilder var8 = new ItemBuilder(XMaterial.NAME_TAG.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DISPLAYNAME.get()));
      if (var4.getString(var5 + ".displayname") != null && !var4.getString(var5 + ".displayname").isEmpty()) {
         var8.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_DISPLAYNAME.get())
                  .builder()
                  .append(SendMessage.fromConfig(var4.getString(var5 + ".displayname")))
                  .build()
            )
         );
      }

      var6.add(this.setEditOption(var8.build(), ItemEditor.Option.DISPLAYNAME));
      ItemBuilder var9 = new ItemBuilder(XMaterial.NAME_TAG.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_NAME.get()));
      if (var4.getString(var5 + ".name") != null && !var4.getString(var5 + ".name").isEmpty()) {
         var9.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_NAME.get())
                  .builder()
                  .append(SendMessage.fromConfig(var4.getString(var5 + ".name")))
                  .build()
            )
         );
      }

      var6.add(this.setEditOption(var9.build(), ItemEditor.Option.NAME));
      ItemBuilder var10 = new ItemBuilder(XMaterial.GOLD_NUGGET.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()));
      if (var4.getString(var5 + ".buy") != null && !var4.getString(var5 + ".buy").isEmpty()) {
         var10.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()).builder().append(var4.getString(var5 + ".buy")).build())
         );
      }

      var6.add(this.setEditOption(var10.build(), ItemEditor.Option.BUY_PRICE));
      ItemBuilder var11 = new ItemBuilder(XMaterial.GOLD_NUGGET.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()));
      if (var4.getString(var5 + ".sell") != null && !var4.getString(var5 + ".sell").isEmpty()) {
         var11.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()).builder().append(var4.getString(var5 + ".sell")).build())
         );
      }

      var6.add(this.setEditOption(var11.build(), ItemEditor.Option.SELL_PRICE));
      ItemBuilder var12 = new ItemBuilder(XMaterial.MOJANG_BANNER_PATTERN.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_LORE.get()))
         .hideFlags();
      if (!var4.getStringList(var5 + ".lore").isEmpty()) {
         ArrayList var13 = new ArrayList();
         var13.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_LORE.get()));
         var4.getStringList(var5 + ".lore").forEach(var1x -> var13.add(SendMessage.fromConfig("- " + var1x)));
         var12.setCLore(var13);
      }

      var6.add(this.setEditOption(var12.build(), ItemEditor.Option.LORE));
      ItemBuilder var25 = new ItemBuilder(XMaterial.MOJANG_BANNER_PATTERN.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DISPLAYLORE.get()))
         .hideFlags();
      if (!var4.getStringList(var5 + ".displaylore").isEmpty()) {
         ArrayList var14 = new ArrayList();
         var14.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_DISPLAYLORE.get()));
         var4.getStringList(var5 + ".displaylore").forEach(var1x -> var14.add(SendMessage.fromConfig("- " + var1x)));
         var25.setCLore(var14);
      }

      var6.add(this.setEditOption(var25.build(), ItemEditor.Option.DISPLAYLORE));
      ItemBuilder var26 = new ItemBuilder(XMaterial.TRIPWIRE_HOOK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SLOT.get()));
      if (var4.getString(var5 + ".slot") != null && !var4.getString(var5 + ".slot").isEmpty()) {
         var26.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SLOT.get()).builder().append(var4.getString(var5 + ".slot")).build())
         );
      }

      var6.add(this.setEditOption(var26.build(), ItemEditor.Option.SLOT));
      ItemBuilder var15 = new ItemBuilder(XMaterial.FEATHER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_STACK_SIZE.get()));
      if (var4.getString(var5 + ".stack-size") != null && !var4.getString(var5 + ".stack-size").isEmpty()) {
         var15.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_STACK_SIZE.get()).builder().append(var4.getString(var5 + ".stack-size")).build()
            )
         );
         var15.setAmount(var4.getInt(var5 + ".stack-size", 16));
      }

      var6.add(this.setEditOption(var15.build(), ItemEditor.Option.STACK_SIZE));
      ItemBuilder var16 = new ItemBuilder(XMaterial.ENCHANTED_BOOK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ENCHANTMENT.get()));
      if (!var4.getStringList(var5 + ".enchantments").isEmpty()) {
         ArrayList var17 = new ArrayList();
         var17.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_ENCHANTMENT.get()));
         var4.getStringList(var5 + ".enchantments").forEach(var1x -> var17.add(SendMessage.fromConfig("- " + var1x)));
         var16.setCLore(var17);
      }

      var6.add(this.setEditOption(var16.build(), ItemEditor.Option.ENCHANTMENTS));
      boolean var27 = var4.getBoolean(var5 + ".enchantment-glint");
      ItemBuilder var18 = new ItemBuilder(var27 ? XMaterial.ENCHANTED_BOOK.parseItem() : XMaterial.BOOK.parseItem())
         .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ENCHANTMENT_GLINT.get()))
         .withCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_ENCHANTMENT_GLINT.get())
                  .builder()
                  .append(var27 ? ChatColor.GREEN + "true" : ChatColor.RED + "false")
                  .build()
            )
         );
      var6.add(
         this.setEditOption(this.plugin.versionHandler.setOnyxNbtString(var18.build(), "value", String.valueOf(var27)), ItemEditor.Option.ENCHANTMENT_GLINT)
      );
      if (this.isFirework(var1, var5)) {
         ItemBuilder var19 = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
            .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.FIREWORK_OPTIONS.get()));
         var6.add(this.setEditOption(var19.build(), ItemEditor.Option.FIREWORK));
      } else {
         ItemBuilder var28 = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem());
         List var20 = Lang.DISABLED_FIREWORK_OPTIONS.getMultiple();
         var28.setCDisplayName((Translatable)var20.get(0));
         var28.setCLore(var20.size() > 0 ? var20.subList(1, var20.size()) : var20);
         var6.add(var28.build());
      }

      if (this.isPotion(var1, var5)) {
         ItemBuilder var29 = new ItemBuilder(XMaterial.POTION.parseItem())
            .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_POTIONTYPES.get()))
            .hideFlags();
         if (!var4.getStringList(var5 + ".potiontypes").isEmpty()) {
            ArrayList var40 = new ArrayList();
            var40.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_POTIONTYPES.get()));
            var4.getStringList(var5 + ".potiontypes").forEach(var1x -> var40.add(SendMessage.fromConfig("- " + var1x)));
            var29.setCLore(var40);
         }

         var6.add(this.setEditOption(this.plugin.versionHandler.setPotionType(var29.build(), PotionTypes.HEALING), ItemEditor.Option.POTIONTYPES));
      } else {
         ItemBuilder var30 = new ItemBuilder(XMaterial.POTION.parseItem());
         List var41 = Lang.DISABLED_POTIONTYPES_OPTION.getMultiple();
         var30.setCDisplayName((Translatable)var41.get(0));
         var30.setCLore(var41.size() > 0 ? var41.subList(1, var41.size()) : var41);
         var30.hideFlags();
         var6.add(var30.build());
      }

      if (this.isSpawner(var1, var5)) {
         ItemBuilder var31 = new ItemBuilder(XMaterial.BLAZE_SPAWN_EGG.parseItem());
         var31.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SPAWNER_TYPE.get()));
         if (var4.getString(var5 + ".spawnertype") != null && !var4.getString(var5 + ".spawnertype").isEmpty()) {
            var31.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SPAWNER_TYPE.get()).builder().append(var4.getString(var5 + ".spawnertype")).build()
               )
            );
         }

         var6.add(this.setEditOption(var31.build(), ItemEditor.Option.SPAWNERTYPE));
      } else {
         ItemBuilder var32 = new ItemBuilder(XMaterial.BLAZE_SPAWN_EGG.parseItem());
         List var42 = Lang.DISABLED_SPAWNER_TYPE_OPTION.getMultiple();
         var32.setCDisplayName((Translatable)var42.get(0));
         var32.setCLore(var42.size() > 0 ? var42.subList(1, var42.size()) : var42);
         var6.add(var32.build());
      }

      if (this.isKnowledgeBook(var1, var5)) {
         ItemBuilder var33 = new ItemBuilder(XMaterial.KNOWLEDGE_BOOK.parseItem());
         var33.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_RECIPE.get()));
         if (!var4.getStringList(var5 + ".recipes").isEmpty()) {
            ArrayList var43 = new ArrayList();
            var43.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_RECIPE.get()));
            var4.getStringList(var5 + ".recipes").forEach(var1x -> var43.add(SendMessage.fromConfig("- " + var1x)));
            var33.setCLore(var43);
         }

         var6.add(this.setEditOption(var33.build(), ItemEditor.Option.RECIPES));
      } else {
         ItemBuilder var34 = new ItemBuilder(XMaterial.KNOWLEDGE_BOOK.parseItem());
         List var44 = Lang.DISABLED_RECIPE_OPTION.getMultiple();
         var34.setCDisplayName((Translatable)var44.get(0));
         var34.setCLore(var44.size() > 0 ? var44.subList(1, var44.size()) : var44);
         var6.add(var34.build());
      }

      if (this.isPlayerHead(var1, var5)) {
         ItemBuilder var35 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem());
         var35.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SKULLOWNER.get()));
         if (var4.getString(var5 + ".skullowner") != null && !var4.getString(var5 + ".skullowner").isEmpty()) {
            var35.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE
                     .get()
                     .replace("%option%", Lang.OPTION_SKULLOWNER.get())
                     .builder()
                     .append(SendMessage.fromConfig(var4.getString(var5 + ".skullowner")))
                     .build()
               )
            );
         }

         var6.add(this.setEditOption(var35.build(), ItemEditor.Option.SKULLOWNER));
      } else {
         ItemBuilder var36 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem());
         List var45 = Lang.DISABLED_SKULLOWNER_OPTION.getMultiple();
         var36.setCDisplayName((Translatable)var45.get(0));
         var36.setCLore(var45.size() > 0 ? var45.subList(1, var45.size()) : var45);
         var6.add(var36.build());
      }

      if (this.isLeatherArmor(var1, var5)) {
         ItemBuilder var37 = new ItemBuilder(XMaterial.LEATHER_CHESTPLATE.parseItem());
         var37.setArmorColor(Color.fromRGB(39680));
         var37.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ARMORCOLOR.get()));
         if (var4.getString(var5 + ".armorcolor") != null && !var4.getString(var5 + ".armorcolor").isEmpty()) {
            var37.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_ARMORCOLOR.get()).builder().append(var4.getString(var5 + ".armorcolor")).build()
               )
            );
         }

         if (this.plugin.version >= 117) {
            var37.hideFlags();
         }

         var6.add(this.setEditOption(var37.build(), ItemEditor.Option.ARMORCOLOR));
      } else {
         ItemBuilder var38 = new ItemBuilder(XMaterial.LEATHER_CHESTPLATE.parseItem());
         var38.setArmorColor(Color.RED);
         List var46 = Lang.DISABLED_ARMORCOLOR_OPTION.getMultiple();
         var38.setCDisplayName((Translatable)var46.get(0));
         var38.setCLore(var46.size() > 0 ? var46.subList(1, var46.size()) : var46);
         if (this.plugin.version >= 117) {
            var38.hideFlags();
         }

         var6.add(var38.build());
      }

      ItemBuilder var39 = new ItemBuilder(XMaterial.WRITABLE_BOOK.parseItem());
      var39.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_NBTDATA.get()));
      if (var4.getString(var5 + ".NBTData") != null && !var4.getString(var5 + ".NBTData").isEmpty()) {
         var39.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_NBTDATA.get()).builder().append(var4.getString(var5 + ".NBTData")).build())
         );
      }

      var6.add(this.setEditOption(var39.build(), ItemEditor.Option.NBTDATA));
      ItemBuilder var47 = new ItemBuilder(XMaterial.CHAIN_COMMAND_BLOCK.parseItem());
      var47.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_BUY_COMMANDS.get()));
      if (!var4.getStringList(var5 + ".buy-commands").isEmpty()) {
         ArrayList var21 = new ArrayList();
         var21.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_BUY_COMMANDS.get()));
         var4.getStringList(var5 + ".buy-commands").forEach(var1x -> var21.add(SendMessage.fromConfig("&f- " + var1x)));
         var47.setCLore(var21);
      }

      var6.add(this.setEditOption(var47.build(), ItemEditor.Option.BUY_COMMANDS));
      ItemBuilder var48 = new ItemBuilder(XMaterial.CHAIN_COMMAND_BLOCK.parseItem());
      var48.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SELL_COMMANDS.get()));
      if (!var4.getStringList(var5 + ".sell-commands").isEmpty()) {
         ArrayList var22 = new ArrayList();
         var22.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SELL_COMMANDS.get()));
         var4.getStringList(var5 + ".sell-commands").forEach(var1x -> var22.add(SendMessage.fromConfig("&f- " + var1x)));
         var48.setCLore(var22);
      }

      var6.add(this.setEditOption(var48.build(), ItemEditor.Option.SELL_COMMANDS));
      ItemBuilder var49 = new ItemBuilder(XMaterial.ENDER_CHEST.parseItem());
      if (var4.contains(var5 + ".stock-limit-player")) {
         List<Translatable> var23 = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         var23.forEach(var0 -> var0.replace("%option%", Lang.GLOBAL_STOCK_OPTIONS.get()).replace("%option2%", Lang.OPTION_PLAYER_STOCK.get()));
         var49.setCDisplayName((Translatable)var23.get(0));
         var49.setCLore(var23.size() > 0 ? var23.subList(1, var23.size()) : var23);
         var6.add(var49.build());
      } else {
         var49.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.GLOBAL_STOCK_OPTIONS.get()));
         var6.add(this.setEditOption(var49.build(), ItemEditor.Option.GLOBAL_STOCK));
      }

      ItemBuilder var50 = new ItemBuilder(XMaterial.CHEST.parseItem());
      if (var4.contains(var5 + ".stock-limit-global")) {
         List<Translatable> var24 = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         var24.forEach(var0 -> var0.replace("%option%", Lang.PLAYER_STOCK_OPTIONS.get()).replace("%option2%", Lang.OPTION_GLOBAL_STOCK.get()));
         var50.setCDisplayName((Translatable)var24.get(0));
         var50.setCLore(var24.size() > 0 ? var24.subList(1, var24.size()) : var24);
         var6.add(var50.build());
      } else {
         var50.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.PLAYER_STOCK_OPTIONS.get()));
         var6.add(this.setEditOption(var50.build(), ItemEditor.Option.PLAYER_STOCK));
      }

      if (this.plugin.dynamicPricing) {
         ItemBuilder var51 = new ItemBuilder(var3.isDynamicPricing() ? XMaterial.COMPARATOR.parseItem() : XMaterial.REPEATER.parseItem());
         var51.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.DYNAMIC_PRICING_OPTIONS.get()));
         if (var3.isDynamicPricing()) {
            var51.withEnchantGlint();
         }

         var6.add(this.setEditOption(var51.build(), ItemEditor.Option.DYNAMIC_PRICING));
      } else {
         ItemBuilder var52 = new ItemBuilder(var3.isDynamicPricing() ? XMaterial.COMPARATOR.parseItem() : XMaterial.REPEATER.parseItem());
         var52.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.DYNAMIC_PRICING_OPTIONS.get()));
         var52.setCLore(Lang.DISABLED_DYNAMIC_PRICING_OPTION.getMultiple());
         var6.add(var52.build());
      }

      ItemBuilder var53 = new ItemBuilder(var3.isLinked() ? XMaterial.ENDER_EYE.parseItem() : XMaterial.ENDER_PEARL.parseItem());
      var53.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SUBSECTION.get()));
      if (var3.isLinked()) {
         var53.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SUBSECTION.get()).builder().append(ChatColor.WHITE + var3.getSubSection()).build()
            )
         );
      }

      var6.add(this.setEditOption(var53.build(), ItemEditor.Option.SUB_SECTION));
      return var6;
   }

   private ItemStack setEditOption(ItemStack var1, ItemEditor.Option var2) {
      return this.plugin.versionHandler.setEditOption(var1, var2.getKey());
   }

   public List<ItemStack> getAvailableActions(String var1, String var2, String var3) {
      ArrayList var4 = new ArrayList();
      ItemBuilder var5 = new ItemBuilder(XMaterial.BARRIER.parseItem());
      var5.setDisplayName(Lang.DISABLED_ACTION.get());
      ItemBuilder var6 = new ItemBuilder(XMaterial.EMERALD_BLOCK.parseItem());
      var6.setDisplayName(Lang.ADD_ACTION.get().replace("%key%", var3));
      ItemBuilder var7 = new ItemBuilder(XMaterial.NAME_TAG.parseItem());
      var7.setDisplayName(Lang.EDIT_ACTION.get().replace("%key%", var3));
      ItemBuilder var8 = new ItemBuilder(XMaterial.REDSTONE_BLOCK.parseItem());
      var8.setDisplayName(Lang.REMOVE_ACTION.get().replace("%key%", var3));
      var3 = var3.equals("global stock")
         ? "stock-limit-global"
         : (
            var3.equals("player stock")
               ? "stock-limit-player"
               : (var3.equals("global stock timer") ? "auto-restock-global" : (var3.equals("player stock timer") ? "auto-restock-player" : var3))
         );
      if (ConfigManager.getShop(var1).get("pages." + var2 + "." + var3) == null) {
         var4.add(0, this.plugin.versionHandler.setAction(var6.build(), "ADD"));
         var4.add(1, var5.build());
         var4.add(2, var5.build());
      } else if (this.allListKeys.contains(var3)) {
         var4.add(0, this.plugin.versionHandler.setAction(var6.build(), "ADD"));
         if (ConfigManager.getShop(var1).getConfigurationSection("pages." + var2).getStringList(var3).isEmpty()) {
            var4.add(1, var5.build());
            var4.add(2, var5.build());
         } else {
            var4.add(1, this.plugin.versionHandler.setAction(var7.build(), "EDIT"));
            var4.add(2, this.plugin.versionHandler.setAction(var8.build(), "REMOVE"));
         }
      } else {
         var4.add(0, var5.build());
         var4.add(1, this.plugin.versionHandler.setAction(var7.build(), "EDIT"));
         if (var3.equalsIgnoreCase("material")) {
            var4.add(2, var5.build());
         } else {
            var4.add(2, this.plugin.versionHandler.setAction(var8.build(), "REMOVE"));
         }
      }

      return var4;
   }

   public List<String> getMultipleItemLore(String... var1) {
      ArrayList var2 = new ArrayList();

      for (String var6 : var1) {
         if (var6.contains("\n")) {
            var2.addAll(Arrays.asList(var6.split("\n")));
         } else {
            var2.add(var6);
         }
      }

      return var2;
   }

   private Material getMaterial(String var1, String var2) {
      String var4 = ConfigManager.getShop(var1).getString(var2 + ".material");
      if (var4 == null || var4.isEmpty()) {
         return null;
      } else if (!var4.contains(":")) {
         Optional var5 = XMaterial.matchXMaterial(var4);
         if (!var5.isPresent()) {
            return null;
         } else {
            ItemStack var3 = ((XMaterial)var5.get()).parseItem();
            return var3 == null ? null : var3.getType();
         }
      } else {
         try {
            return OnyxShop.getInstance().getItemManager().getSPM(null, var4).getType();
         } catch (ItemLoadException var6) {
            SendMessage.warnMessage("Could not get material value of item: '" + var4 + "'");
            return null;
         }
      }
   }

   private boolean isFirework(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      return var3 == null ? false : var3.equals(XMaterial.FIREWORK_ROCKET.parseMaterial());
   }

   private boolean isLeatherArmor(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      if (var3 == null) {
         return false;
      } else {
         ItemStack var4 = new ItemStack(var3);
         return var4.getItemMeta() instanceof LeatherArmorMeta;
      }
   }

   private boolean isPlayerHead(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      return var3 == null ? false : XMaterial.matchXMaterial(var3).equals(XMaterial.PLAYER_HEAD);
   }

   private boolean isKnowledgeBook(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      return var3 == null ? false : XMaterial.matchXMaterial(var3).equals(XMaterial.KNOWLEDGE_BOOK);
   }

   private boolean isSpawner(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      return var3 == null ? false : XMaterial.matchXMaterial(var3).equals(XMaterial.SPAWNER);
   }

   private boolean isPotion(String var1, String var2) {
      Material var3 = this.getMaterial(var1, var2);
      return var3 == null ? false : PotionTypes.canHaveEffects(var3);
   }

   public ItemStack getPotion(String var1, String var2, String var3) {
      ItemBuilder var4 = new ItemBuilder(Material.BARRIER);
      var4.setDisplayName(var1);
      Material var5 = this.getMaterial(var2, var3);
      if (var5 == null) {
         return var4.build();
      } else {
         Optional var6 = PotionTypes.matchPotionType(var1);
         if (var6.isPresent() && ((PotionTypes)var6.get()).isSupported()) {
            boolean var7 = XMaterial.matchXMaterial(ConfigManager.getShop(var2).getString(var3 + ".material")).get() == XMaterial.SPLASH_POTION;
            return ((PotionTypes)var6.get()).addEffect(new ItemStack(var5), var7);
         } else {
            return var4.build();
         }
      }
   }

   public ItemStack getBook(String var1) {
      try {
         return this.plugin.enchantsProvider.getEnchant(var1).getBook();
      } catch (ItemLoadException var4) {
         ItemBuilder var3 = new ItemBuilder(Material.BARRIER);
         var3.setDisplayName(var1);
         return var3.build();
      }
   }

   public SimpleEnchantment getChosenEnchantment(String var1, String var2, Integer var3) {
      List var4 = this.plugin.enchantsProvider.getEnchantsOfItem(this.plugin.getShopItem(var1, var2).getItemToGive());
      if (var4.size() == 1) {
         var3 = 0;
      }

      return var3 >= 0 && var3 < var4.size() ? (SimpleEnchantment)var4.get(var3) : null;
   }

   public PotionEffect getChosenPotionEffectType(String var1, String var2, int var3) {
      if (ConfigManager.getShop(var1).getStringList("pages." + var2 + ".potiontypes").size() == 1) {
         var3 = 0;
      }

      if (var3 >= 0 && var3 < ConfigManager.getShop(var1).getStringList("pages." + var2 + ".potiontypes").size()) {
         Optional var4 = PotionTypes.matchPotionType((String)ConfigManager.getShop(var1).getStringList("pages." + var2 + ".potiontypes").get(var3));
         if (var4.isPresent() && ((PotionTypes)var4.get()).isSupported()) {
            return ((PotionTypes)var4.get()).parsePotionEffect();
         }
      }

      return null;
   }

   public void loadItemChanges(ItemEditor.Action var1, ItemEditor.Option var2, String var3) {
      label26:
      switch (var1) {
         case EDIT:
         case ADD:
         case REMOVE:
            if (var2 == ItemEditor.Option.SLOT) {
               this.plugin.addSection(var3.split("\\.")[0], new ShopCategory(this.plugin, var3.split("\\.")[0]));
            } else {
               this.plugin.getSection(var3.split("\\.")[0]).reloadItem(var3);
               switch (var2) {
                  case BUY_PRICE:
                  case SELL_PRICE:
                     if (this.plugin.getShopItem(var3).isDynamicPricing()) {
                        SendMessage.logDebugMessage("Detected price change for item '" + var3 + "', reloading DynamicPricing...");
                     }
                  case DYNAMIC_PRICING_ENABLE:
                  case DYNAMIC_PRICING_MAX_STOCK:
                     if (this.plugin.getShopItem(var3).isDynamicPricing()) {
                        this.plugin.getDynamicPricingProvider().reload();
                     }
                     break label26;
                  case PLAYER_STOCK_LIMIT:
                  case PLAYER_STOCK_TIMER:
                  case PLAYER_STOCK_FREQUENCY:
                  case GLOBAL_STOCK_LIMIT:
                  case GLOBAL_STOCK_TIMER:
                  case GLOBAL_STOCK_FREQUENCY:
                     this.plugin.getLimitedStockProvider().reload();
               }
            }
            break;
         case ADD_ITEM:
            this.plugin.getSection(var3.split("\\.")[0]).addNewItem(var3);
            break;
         case REMOVE_ITEM:
            this.plugin.addSection(var3.split("\\.")[0], new ShopCategory(this.plugin, var3.split("\\.")[0]));
      }

      if (!this.plugin.shopItemsByMaterialName.isEmpty()) {
         this.plugin.shopItemsByMaterialName.clear();
      }

      this.plugin.startupReload.getShopItemsByMaterialName();
   }

   private String getNextAvailablePosition(String var1) {
      Config var2 = ConfigManager.getShop(var1);
      int var3 = var2.getConfigurationSection("pages").getKeys(false).size();
      if (var3 == 0) {
         return "page1.items.1";
      } else {
         LinkedList var4 = new LinkedList(var2.getConfigurationSection("pages").getKeys(false));
         String var5 = (String)var4.get(var3 - 1);
         int var6 = var2.getInt("pages." + var5 + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(var1) ? 9 : 0);
         if (var2.getConfigurationSection("pages." + var5 + ".items").getKeys(false).size() >= var6) {
            return "page" + (var4.size() + 1) + ".items.1";
         } else {
            Set var7 = var2.getConfigurationSection("pages." + var5 + ".items").getKeys(false);
            int var8 = var7.size() + 1;

            while (var7.contains(String.valueOf(var8))) {
               var8++;
            }

            return var5 + ".items." + var8;
         }
      }
   }

   public void openItemEditorCreateItem(Player var1, ShopItem var2, String var3, Double var4, Double var5) {
      if (var3 == null) {
         new ChooseValue(var1, var2, ItemEditor.Option.MATERIAL, ItemEditor.Action.ADD, -1, 1);
      } else if (var4 == null) {
         new AnvilGUI.Builder()
            .onComplete((var4x, var5x) -> {
               try {
                  Double var6x = Double.parseDouble(var5x);
                  this.plugin.runTaskLater(() -> this.openItemEditorCreateItem(var1, var2, var3, var6x, null), 1L);
                  return AnvilGUI.Response.close();
               } catch (NumberFormatException var7) {
                  return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
               }
            })
            .onClose(var2x -> this.plugin.runTaskLater(() -> this.openItemEditorCreateItem(var2x, var2, null, null, null), 1L))
            .text(Lang.OPTION_BUY_PRICE.get().getLegacy())
            .itemLeft(new ItemStack(Material.PAPER))
            .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()).getLegacy())
            .plugin(this.plugin)
            .open(var1);
      } else if (var5 == null) {
         new AnvilGUI.Builder()
            .onComplete((var5x, var6x) -> {
               try {
                  Double var7 = Double.parseDouble(var6x);
                  this.plugin.runTaskLater(() -> this.openItemEditorCreateItem(var1, var2, var3, var4, var7), 1L);
                  return AnvilGUI.Response.close();
               } catch (NumberFormatException var8) {
                  return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
               }
            })
            .onClose(var3x -> this.plugin.runTaskLater(() -> this.openItemEditorCreateItem(var3x, var2, var3, null, null), 1L))
            .text(Lang.OPTION_SELL_PRICE.get().getLegacy())
            .itemLeft(new ItemStack(Material.PAPER))
            .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()).getLegacy())
            .plugin(this.plugin)
            .open(var1);
      } else {
         String var6 = this.getNextAvailablePosition(var2.section);
         ConfigManager.getShop(var2.section).set("pages." + var6 + ".material", var3);
         ConfigManager.getShop(var2.section).set("pages." + var6 + ".buy", var4);
         ConfigManager.getShop(var2.section).set("pages." + var6 + ".sell", var5);
         ConfigManager.saveShop(var2.section);
         new ConclusionScreen(var1, var2, null, ItemEditor.Action.ADD_ITEM, var2.section + "." + var6);
      }
   }

   public void openAvailableResultsForOption(Player var1, ItemEditor.Option var2, ItemEditor.Action var3, ShopItem var4, int var5) {
      boolean var6 = false;
      switch (var2) {
         case MATERIAL:
            new ChooseValue(var1, var4, var2, var3, var5, 1);
            return;
         case BUY_PRICE:
         case SELL_PRICE:
         case SLOT:
         case STACK_SIZE:
         case DYNAMIC_PRICING_MAX_STOCK:
            if (var3 != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((var5x, var6x) -> {
                     try {
                        Object var7x;
                        if (var2 != ItemEditor.Option.BUY_PRICE && var2 != ItemEditor.Option.SELL_PRICE) {
                           var7x = Integer.parseInt(var6x);
                        } else {
                           var7x = Double.parseDouble(var6x);
                        }

                        ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var7x);
                        ConfigManager.saveShop(var4.section);
                        this.plugin.runTaskLater(() -> new ConclusionScreen(var1, var4, var2, var3, null), 1L);
                        return AnvilGUI.Response.close();
                     } catch (NumberFormatException var8x) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(var4x -> this.plugin.runTaskLater(() -> new AvailableActions(var1, var4, var2), 1L))
                  .text(
                     var3 != ItemEditor.Action.ADD
                        ? (
                           var2 == ItemEditor.Option.BUY_PRICE
                              ? String.valueOf(var4.getBuyPrice())
                              : (
                                 var2 == ItemEditor.Option.SELL_PRICE
                                    ? String.valueOf(var4.getSellPrice())
                                    : (
                                       var2 == ItemEditor.Option.SLOT
                                          ? String.valueOf(this.plugin.getSection(var4.section).getItemLocs().indexOf(var4.itemLoc))
                                          : (var2 == ItemEditor.Option.STACK_SIZE ? String.valueOf(var4.getStackSize()) : var2.getName())
                                    )
                              )
                        )
                        : var2.getName()
                  )
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", var2.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(var1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case NAME:
         case DISPLAYNAME:
         case NBTDATA:
         case PLAYER_STOCK_FREQUENCY:
         case GLOBAL_STOCK_FREQUENCY:
            if (var3 != ItemEditor.Action.REMOVE) {
               var1.closeInventory();
               this.convFactory.startConversation(var1, var2, var3, var4.getItemPath(), var5);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case LORE:
         case DISPLAYLORE:
         case BUY_COMMANDS:
         case SELL_COMMANDS:
            if (var3 == ItemEditor.Action.ADD) {
               var1.closeInventory();
               this.convFactory.startConversation(var1, var2, var3, var4.getItemPath(), var5);
               return;
            }

            if (var3 == ItemEditor.Action.EDIT) {
               if (ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey()).size() == 1) {
                  var1.closeInventory();
                  this.convFactory.startConversation(var1, var2, var3, var4.getItemPath(), var5);
               } else if (var5 == -1) {
                  new IndexChooser(var1, var4, var2, var3);
               } else {
                  var1.closeInventory();
                  this.convFactory.startConversation(var1, var2, var3, var4.getItemPath(), var5);
               }

               return;
            }

            if (var3 == ItemEditor.Action.REMOVE) {
               if (ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey()).size() == 1) {
                  ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
                  var6 = true;
               } else {
                  if (var5 == -1) {
                     new IndexChooser(var1, var4, var2, var3);
                     return;
                  }

                  List var8 = ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey());
                  var8.remove(var5);
                  ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var8);
                  var6 = true;
               }
            }
         case HIDE_PRICING_LORE:
         case ENCHANTMENT_GLINT:
         case FIREWORK:
         case FIREWORK_DURATION:
         case FIREWORK_FLICKER:
         case FIREWORK_TRAIL:
         case FIREWORK_SHAPE:
         case COMPONENTS:
         case PLAYER_STOCK:
         case GLOBAL_STOCK:
         case LEVEL_REQUIREMENT:
         case SKULL_TEXTURE:
         case DYNAMIC_PRICING:
         case DYNAMIC_PRICING_ENABLE:
         default:
            break;
         case ENCHANTMENTS:
         case POTIONTYPES:
         case RECIPES:
         case FIREWORK_COLORS:
         case FIREWORK_FADE_COLORS:
         case SUB_SECTION:
            if (var3 == ItemEditor.Action.ADD) {
               new ChooseValue(var1, var4, var2, var3, var5, 1);
               return;
            }

            if (var3 == ItemEditor.Action.EDIT) {
               if (ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey()).size() == 1) {
                  new ChooseValue(var1, var4, var2, var3, var5, 1);
               } else if (var5 == -1) {
                  new IndexChooser(var1, var4, var2, var3);
               } else {
                  new ChooseValue(var1, var4, var2, var3, var5, 1);
               }

               return;
            }

            if (var3 == ItemEditor.Action.REMOVE) {
               if (ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey()).size() == 1) {
                  ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
                  var6 = true;
               } else {
                  if (var5 == -1) {
                     new IndexChooser(var1, var4, var2, var3);
                     return;
                  }

                  List var7 = ConfigManager.getShop(var4.section).getStringList("pages." + var4.itemLoc + "." + var2.getKey());
                  var7.remove(var5);
                  ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var7);
                  var6 = true;
               }
            }
            break;
         case SPAWNERTYPE:
            if (var3 != ItemEditor.Action.REMOVE) {
               new ChooseValue(var1, var4, var2, var3, var5, 1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case SKULLOWNER:
            if (var3 != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((var5x, var6x) -> {
                     ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var6x);
                     ConfigManager.saveShop(var4.section);
                     this.plugin.runTaskLater(() -> new ConclusionScreen(var1, var4, var2, var3, null), 1L);
                     return AnvilGUI.Response.close();
                  })
                  .onClose(var4x -> this.plugin.runTaskLater(() -> new AvailableActions(var1, var4, var2), 1L))
                  .text(var2.getName())
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", var2.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(var1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case ARMORCOLOR:
            if (var3 != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((var5x, var6x) -> {
                     var6x = var6x.replace("#", "");
                     if (var6x.startsWith("0x")) {
                        var6x = var6x.substring(2);
                     }

                     if (var6x.length() == 6) {
                        ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var6x);
                        ConfigManager.saveShop(var4.section);
                        this.plugin.runTaskLater(() -> new ConclusionScreen(var1, var4, var2, var3, null), 1L);
                        return AnvilGUI.Response.close();
                     } else {
                        return AnvilGUI.Response.text("0x00FF00");
                     }
                  })
                  .onClose(var4x -> this.plugin.runTaskLater(() -> new AvailableActions(var1, var4, var2), 1L))
                  .text("0x00FF00")
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", var2.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(var1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case PLAYER_STOCK_LIMIT:
         case GLOBAL_STOCK_LIMIT:
            if (var3 != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((var5x, var6x) -> {
                     try {
                        ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), Integer.parseInt(var6x));
                        ConfigManager.saveShop(var4.section);
                        this.plugin.runTaskLater(() -> new ConclusionScreen(var1, var4, var2, var3, null), 1L);
                        return AnvilGUI.Response.close();
                     } catch (NumberFormatException var8x) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(var4x -> this.plugin.runTaskLater(() -> new AvailableActions(var1, var4, var2), 1L))
                  .text(var2.getName())
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", var2.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(var1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
            break;
         case PLAYER_STOCK_TIMER:
         case GLOBAL_STOCK_TIMER:
            if (var3 != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((var5x, var6x) -> {
                     try {
                        TimeUtils.getTime(var6x);
                        ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), var6x);
                        ConfigManager.saveShop(var4.section);
                        this.plugin.runTaskLater(() -> new ConclusionScreen(var1, var4, var2, var3, null), 1L);
                        return AnvilGUI.Response.close();
                     } catch (ParseException var8x) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(var4x -> this.plugin.runTaskLater(() -> new AvailableActions(var1, var4, var2), 1L))
                  .text("5d 12h 30m 10s")
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", var2.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(var1);
               return;
            }

            ConfigManager.getShop(var4.section).set("pages." + var4.itemLoc + "." + var2.getKey(), null);
            var6 = true;
      }

      if (var6) {
         ConfigManager.saveShop(var4.section);
         new ConclusionScreen(var1, var4, var2, var3, null);
      }
   }

   public static enum Action {
      EDIT,
      ADD,
      REMOVE,
      ADD_ITEM,
      REMOVE_ITEM;

      private Action() {
      }
   }

   public static enum Option {
      MATERIAL("material", Lang.OPTION_MATERIAL.get()),
      BUY_PRICE("buy", Lang.OPTION_BUY_PRICE.get()),
      SELL_PRICE("sell", Lang.OPTION_SELL_PRICE.get()),
      NAME("name", Lang.OPTION_NAME.get()),
      DISPLAYNAME("displayname", Lang.OPTION_DISPLAYNAME.get()),
      LORE("lore", Lang.OPTION_LORE.get()),
      DISPLAYLORE("displaylore", Lang.OPTION_DISPLAYLORE.get()),
      HIDE_PRICING_LORE("hidePricingLore", Lang.OPTION_PRICING_LORE.get()),
      SLOT("slot", Lang.OPTION_SLOT.get()),
      STACK_SIZE("stack-size", Lang.OPTION_STACK_SIZE.get()),
      ENCHANTMENT_GLINT("enchantment-glint", Lang.OPTION_ENCHANTMENT_GLINT.get()),
      ENCHANTMENTS("enchantments", Lang.OPTION_ENCHANTMENT.get()),
      POTIONTYPES("potiontypes", Lang.OPTION_POTIONTYPES.get()),
      SPAWNERTYPE("spawnertype", Lang.OPTION_SPAWNER_TYPE.get()),
      RECIPES("recipes", Lang.OPTION_RECIPE.get()),
      SKULLOWNER("skullowner", Lang.OPTION_SKULLOWNER.get()),
      ARMORCOLOR("armorcolor", Lang.OPTION_ARMORCOLOR.get()),
      FIREWORK("firework", null),
      FIREWORK_DURATION("duration", Lang.OPTION_FIREWORK_DURATION.get()),
      FIREWORK_COLORS("colors", Lang.OPTION_FIREWORK_COLORS.get()),
      FIREWORK_FADE_COLORS("fade-colors", Lang.OPTION_FIREWORK_FADE_COLORS.get()),
      FIREWORK_FLICKER("flicker", Lang.OPTION_FIREWORK_FLICKER.get()),
      FIREWORK_TRAIL("trail", Lang.OPTION_FIREWORK_TRAIL.get()),
      FIREWORK_SHAPE("shape", Lang.OPTION_FIREWORK_SHAPE.get()),
      SUB_SECTION("section", Lang.OPTION_SUBSECTION.get()),
      NBTDATA("NBTData", Lang.OPTION_NBTDATA.get()),
      COMPONENTS("components", null),
      BUY_COMMANDS("buy-commands", Lang.OPTION_BUY_COMMANDS.get()),
      SELL_COMMANDS("sell-commands", Lang.OPTION_SELL_COMMANDS.get()),
      PLAYER_STOCK("player-stock", null),
      PLAYER_STOCK_LIMIT("stock-limit-player", Lang.OPTION_PLAYER_STOCK_LIMIT.get()),
      PLAYER_STOCK_TIMER("auto-restock-player", Lang.OPTION_PLAYER_STOCK_RESTOCK_TIME.get()),
      PLAYER_STOCK_FREQUENCY("auto-restock-player-frequency", Lang.OPTION_PLAYER_STOCK_RESTOCK_FREQUENCY.get()),
      GLOBAL_STOCK("global-stock", null),
      GLOBAL_STOCK_LIMIT("stock-limit-global", Lang.OPTION_GLOBAL_STOCK_LIMIT.get()),
      GLOBAL_STOCK_TIMER("auto-restock-global", Lang.OPTION_GLOBAL_STOCK_RESTOCK_TIME.get()),
      GLOBAL_STOCK_FREQUENCY("auto-restock-global-frequency", Lang.OPTION_GLOBAL_STOCK_RESTOCK_FREQUENCY.get()),
      LEVEL_REQUIREMENT("level-requirement", Lang.OPTION_LEVEL_REQUIREMENT.get()),
      SKULL_TEXTURE("skull-texture", Lang.OPTION_SKULL_TEXTURE.get()),
      DYNAMIC_PRICING("dynamic-pricing", Lang.OPTION_DYNAMIC_PRICING.get()),
      DYNAMIC_PRICING_ENABLE("dynamic-pricing.enabled", Lang.OPTION_DYNAMIC_PRICING_ENABLE.get()),
      DYNAMIC_PRICING_MAX_STOCK("dynamic-pricing.max-stock", Lang.OPTION_DYNAMIC_PRICING_MAX_STOCK.get());

      private final String key;
      private final String name;

      private Option(String nullxx, Translatable nullxxx) {
         this.key = nullxx;
         this.name = nullxxx != null ? nullxxx.getLegacy() : null;
      }

      public static ItemEditor.Option getFromKey(String var0) {
         for (ItemEditor.Option var4 : values()) {
            if (var4.getKey().equals(var0)) {
               return var4;
            }
         }

         return null;
      }

      public String getKey() {
         return this.key;
      }

      public String getName() {
         return this.name;
      }
   }
}
