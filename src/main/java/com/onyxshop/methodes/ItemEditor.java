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

   public ItemEditor(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public ItemStack getSelectedItem(ShopItem shopItem) {
      ItemStack itemStack = new ItemStack(shopItem.getShopItem());
      if (!shopItem.hasItemError() && !shopItem.isDisplayItem()) {
         ArrayList itemIds = new ArrayList();
         itemIds.add(Lang.BUY_PRICE.get().getLegacy().replace("%amounttopay%", this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice())));
         itemIds.add(Lang.SELL_PRICE.get().getLegacy().replace("%amounttopay%", this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice())));
         ItemMeta itemMeta = itemStack.getItemMeta();
         itemMeta.setLore(itemIds);
         itemStack.setItemMeta(itemMeta);
      }

      return itemStack;
   }

   public void saveFireworkOptions(Inventory inventory) {
      boolean flag = false;
      String item = ((FireworkOptions)inventory.getHolder()).getItem().section;
      String item2 = ((FireworkOptions)inventory.getHolder()).getItem().itemLoc;
      int value = Integer.parseInt(this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(0), "value"));
      if (value != ConfigManager.getShop(item).getInt("pages." + item2 + ".duration", 1)) {
         ConfigManager.getShop(item).set("pages." + item2 + ".duration", value);
         flag = true;
      }

      boolean flag2 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(3), "value"));
      if (flag2 != ConfigManager.getShop(item).getBoolean("pages." + item2 + ".flicker")) {
         ConfigManager.getShop(item).set("pages." + item2 + ".flicker", flag2);
         flag = true;
      }

      boolean flag3 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(4), "value"));
      if (flag3 != ConfigManager.getShop(item).getBoolean("pages." + item2 + ".trail")) {
         ConfigManager.getShop(item).set("pages." + item2 + ".trail", flag3);
         flag = true;
      }

      String onyxNbtString = this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(5), "value");
      if (!onyxNbtString.equals(ConfigManager.getShop(item).getString("pages." + item2 + ".shape", "BALL"))) {
         ConfigManager.getShop(item).set("pages." + item2 + ".shape", onyxNbtString);
         flag = true;
      }

      if (flag) {
         ConfigManager.saveShop(item);
         this.plugin.getSection(item).reloadItem(item + "." + item2);
      }
   }

   public void saveDynamicPricingOptions(Inventory inventory) {
      boolean flag = false;
      String item = ((DynamicPricingOptions)inventory.getHolder()).getItem().section;
      String item2 = ((DynamicPricingOptions)inventory.getHolder()).getItem().itemLoc;
      boolean flag2 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(0), "value"));
      if (flag2 != ConfigManager.getShop(item).getBoolean("pages." + item2 + "." + ItemEditor.Option.DYNAMIC_PRICING_ENABLE.getKey(), this.plugin.dynamicPricing)
         )
       {
         ConfigManager.getShop(item).set("pages." + item2 + "." + ItemEditor.Option.DYNAMIC_PRICING_ENABLE.getKey(), flag2);
         flag = true;
      }

      if (flag) {
         ConfigManager.saveShop(item);
         this.plugin.getSection(item).reloadItem(item + "." + item2);
      }
   }

   public void saveUnsavedOptions(Inventory inventory) {
      boolean flag = false;
      ShopItem shopItem = ((AvailableOptions)inventory.getHolder()).getItem();
      boolean flag2 = Boolean.parseBoolean(this.plugin.versionHandler.getOnyxNbtString(inventory.getItem(10), "value"));
      if (flag2 != ConfigManager.getShop(shopItem.section).getBoolean("pages." + shopItem.itemLoc + ".enchantment-glint")) {
         ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + ".enchantment-glint", flag2);
         flag = true;
      }

      if (flag) {
         ConfigManager.saveShop(shopItem.section);
         this.plugin.getSection(shopItem.section).reloadItem(shopItem.getItemPath());
      }
   }

   public ItemStack toggleOption(String itemId, ItemEditor.Option option) {
      ItemStack itemStack = null;
      switch (option) {
         case ENCHANTMENT_GLINT:
            itemId = itemId.equals("true") ? "false" : "true";
            ItemBuilder itemBuilder = new ItemBuilder(itemId.equals("true") ? XMaterial.ENCHANTED_BOOK.parseItem() : XMaterial.BOOK.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", option.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", option.getName())
                        .builder()
                        .append(itemId.equals("true") ? ChatColor.GREEN + itemId : ChatColor.RED + itemId)
                        .build()
                  )
               );
            itemStack = itemBuilder.build();
            break;
         case FIREWORK_DURATION:
            itemId = String.valueOf(itemId.equals("1") ? 2 : (itemId.equals("2") ? 3 : 1));
            ItemBuilder itemBuilder2 = new ItemBuilder(XMaterial.GUNPOWDER.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", option.getName()))
               .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", option.getName()) + ChatColor.GREEN + itemId)
               .setAmount(Integer.parseInt((String)itemId));
            itemStack = itemBuilder2.build();
            break;
         case FIREWORK_FLICKER:
            itemId = itemId.equals("true") ? "false" : "true";
            ItemBuilder itemBuilder3 = new ItemBuilder(itemId.equals("true") ? XMaterial.GLOWSTONE_DUST.parseItem() : XMaterial.GUNPOWDER.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", option.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", option.getName())
                        .builder()
                        .append(itemId.equals("true") ? ChatColor.GREEN + itemId : ChatColor.RED + itemId)
                        .build()
                  )
               );
            itemStack = itemBuilder3.build();
            break;
         case FIREWORK_TRAIL:
            itemId = itemId.equals("true") ? "false" : "true";
            ItemBuilder itemBuilder4 = new ItemBuilder(itemId.equals("true") ? XMaterial.DIAMOND_BLOCK.parseItem() : XMaterial.DIAMOND.parseItem())
               .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", option.getName()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", option.getName())
                        .builder()
                        .append(itemId.equals("true") ? ChatColor.GREEN + itemId : ChatColor.RED + itemId)
                        .build()
                  )
               );
            itemStack = itemBuilder4.build();
            break;
         case FIREWORK_SHAPE:
            itemId = itemId.equalsIgnoreCase("BALL_LARGE")
               ? "STAR"
               : (
                  itemId.equalsIgnoreCase("STAR")
                     ? "BURST"
                     : (itemId.equalsIgnoreCase("BURST") ? "CREEPER" : (itemId.equalsIgnoreCase("CREEPER") ? "BALL" : "BALL_LARGE"))
               );
            ItemBuilder itemBuilder5 = new ItemBuilder(
                  itemId.equalsIgnoreCase("BALL_LARGE")
                     ? XMaterial.FIRE_CHARGE.parseItem()
                     : (
                        itemId.equalsIgnoreCase("STAR")
                           ? XMaterial.GOLD_NUGGET.parseItem()
                           : (
                              itemId.equalsIgnoreCase("BURST")
                                 ? XMaterial.FEATHER.parseItem()
                                 : (itemId.equalsIgnoreCase("CREEPER") ? XMaterial.CREEPER_HEAD.parseItem() : XMaterial.FIREWORK_STAR.parseItem())
                           )
                     )
               )
               .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", option.getName()))
               .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", option.getName()) + ChatColor.GREEN + itemId);
            itemStack = itemBuilder5.build();
            break;
         case DYNAMIC_PRICING_ENABLE:
            itemId = itemId.equals("true") ? "false" : "true";
            ItemBuilder itemBuilder6 = new ItemBuilder(XMaterial.LEVER.parseItem())
               .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DYNAMIC_PRICING_ENABLE.get()))
               .withCLore(
                  Collections.singletonList(
                     Lang.CURRENT_VALUE
                        .get()
                        .replace("%option%", Lang.OPTION_DYNAMIC_PRICING.get())
                        .builder()
                        .append(itemId.equals("true") ? ChatColor.GREEN + itemId : ChatColor.RED + itemId)
                        .build()
                  )
               );
            if (itemId.equals("true")) {
               itemBuilder6.withEnchantGlint();
            }

            itemStack = itemBuilder6.build();
      }

      return this.setEditOption(this.plugin.versionHandler.setOnyxNbtString(itemStack, "value", (String)itemId), option);
   }

   public List<ItemStack> getAllKeys(String itemId, String text) {
      ShopItem shopItem = this.plugin.getShopItem(itemId, text);
      Config config = ConfigManager.getShop(itemId);
      String text2 = "pages." + text;
      ArrayList values = new ArrayList();
      ItemBuilder itemBuilder = new ItemBuilder(XMaterial.GRASS_BLOCK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_MATERIAL.get()));
      if (config.getString(text2 + ".material") != null && !config.getString(text2 + ".material").isEmpty()) {
         itemBuilder.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_MATERIAL.get()).builder().color('a').append(config.getString(text2 + ".material")).build()
            )
         );
      }

      values.add(this.setEditOption(itemBuilder.build(), ItemEditor.Option.MATERIAL));
      ItemBuilder itemBuilder2 = new ItemBuilder(XMaterial.NAME_TAG.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DISPLAYNAME.get()));
      if (config.getString(text2 + ".displayname") != null && !config.getString(text2 + ".displayname").isEmpty()) {
         itemBuilder2.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_DISPLAYNAME.get())
                  .builder()
                  .append(SendMessage.fromConfig(config.getString(text2 + ".displayname")))
                  .build()
            )
         );
      }

      values.add(this.setEditOption(itemBuilder2.build(), ItemEditor.Option.DISPLAYNAME));
      ItemBuilder itemBuilder3 = new ItemBuilder(XMaterial.NAME_TAG.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_NAME.get()));
      if (config.getString(text2 + ".name") != null && !config.getString(text2 + ".name").isEmpty()) {
         itemBuilder3.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_NAME.get())
                  .builder()
                  .append(SendMessage.fromConfig(config.getString(text2 + ".name")))
                  .build()
            )
         );
      }

      values.add(this.setEditOption(itemBuilder3.build(), ItemEditor.Option.NAME));
      ItemBuilder itemBuilder4 = new ItemBuilder(XMaterial.GOLD_NUGGET.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()));
      if (config.getString(text2 + ".buy") != null && !config.getString(text2 + ".buy").isEmpty()) {
         itemBuilder4.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()).builder().append(config.getString(text2 + ".buy")).build())
         );
      }

      values.add(this.setEditOption(itemBuilder4.build(), ItemEditor.Option.BUY_PRICE));
      ItemBuilder itemBuilder5 = new ItemBuilder(XMaterial.GOLD_NUGGET.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()));
      if (config.getString(text2 + ".sell") != null && !config.getString(text2 + ".sell").isEmpty()) {
         itemBuilder5.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()).builder().append(config.getString(text2 + ".sell")).build())
         );
      }

      values.add(this.setEditOption(itemBuilder5.build(), ItemEditor.Option.SELL_PRICE));
      ItemBuilder itemBuilder6 = new ItemBuilder(XMaterial.MOJANG_BANNER_PATTERN.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_LORE.get()))
         .hideFlags();
      if (!config.getStringList(text2 + ".lore").isEmpty()) {
         ArrayList values2 = new ArrayList();
         values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_LORE.get()));
         config.getStringList(text2 + ".lore").forEach(entry -> values2.add(SendMessage.fromConfig("- " + entry)));
         itemBuilder6.setCLore(values2);
      }

      values.add(this.setEditOption(itemBuilder6.build(), ItemEditor.Option.LORE));
      ItemBuilder itemBuilder7 = new ItemBuilder(XMaterial.MOJANG_BANNER_PATTERN.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DISPLAYLORE.get()))
         .hideFlags();
      if (!config.getStringList(text2 + ".displaylore").isEmpty()) {
         ArrayList values2 = new ArrayList();
         values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_DISPLAYLORE.get()));
         config.getStringList(text2 + ".displaylore").forEach(entry -> values2.add(SendMessage.fromConfig("- " + entry)));
         itemBuilder7.setCLore(values2);
      }

      values.add(this.setEditOption(itemBuilder7.build(), ItemEditor.Option.DISPLAYLORE));
      ItemBuilder itemBuilder8 = new ItemBuilder(XMaterial.TRIPWIRE_HOOK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SLOT.get()));
      if (config.getString(text2 + ".slot") != null && !config.getString(text2 + ".slot").isEmpty()) {
         itemBuilder8.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SLOT.get()).builder().append(config.getString(text2 + ".slot")).build())
         );
      }

      values.add(this.setEditOption(itemBuilder8.build(), ItemEditor.Option.SLOT));
      ItemBuilder itemBuilder9 = new ItemBuilder(XMaterial.FEATHER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_STACK_SIZE.get()));
      if (config.getString(text2 + ".stack-size") != null && !config.getString(text2 + ".stack-size").isEmpty()) {
         itemBuilder9.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_STACK_SIZE.get()).builder().append(config.getString(text2 + ".stack-size")).build()
            )
         );
         itemBuilder9.setAmount(config.getInt(text2 + ".stack-size", 16));
      }

      values.add(this.setEditOption(itemBuilder9.build(), ItemEditor.Option.STACK_SIZE));
      ItemBuilder itemBuilder10 = new ItemBuilder(XMaterial.ENCHANTED_BOOK.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ENCHANTMENT.get()));
      if (!config.getStringList(text2 + ".enchantments").isEmpty()) {
         ArrayList values2 = new ArrayList();
         values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_ENCHANTMENT.get()));
         config.getStringList(text2 + ".enchantments").forEach(entry -> values2.add(SendMessage.fromConfig("- " + entry)));
         itemBuilder10.setCLore(values2);
      }

      values.add(this.setEditOption(itemBuilder10.build(), ItemEditor.Option.ENCHANTMENTS));
      boolean flag = config.getBoolean(text2 + ".enchantment-glint");
      ItemBuilder itemBuilder11 = new ItemBuilder(flag ? XMaterial.ENCHANTED_BOOK.parseItem() : XMaterial.BOOK.parseItem())
         .withDisplayName(Lang.TOGGLE_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ENCHANTMENT_GLINT.get()))
         .withCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_ENCHANTMENT_GLINT.get())
                  .builder()
                  .append(flag ? ChatColor.GREEN + "true" : ChatColor.RED + "false")
                  .build()
            )
         );
      values.add(
         this.setEditOption(this.plugin.versionHandler.setOnyxNbtString(itemBuilder11.build(), "value", String.valueOf(flag)), ItemEditor.Option.ENCHANTMENT_GLINT)
      );
      if (this.isFirework(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
            .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.FIREWORK_OPTIONS.get()));
         values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.FIREWORK));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem());
         List multiple = Lang.DISABLED_FIREWORK_OPTIONS.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder12.build());
      }

      if (this.isPotion(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.POTION.parseItem())
            .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_POTIONTYPES.get()))
            .hideFlags();
         if (!config.getStringList(text2 + ".potiontypes").isEmpty()) {
            ArrayList values2 = new ArrayList();
            values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_POTIONTYPES.get()));
            config.getStringList(text2 + ".potiontypes").forEach(entry -> values2.add(SendMessage.fromConfig("- " + entry)));
            itemBuilder12.setCLore(values2);
         }

         values.add(this.setEditOption(this.plugin.versionHandler.setPotionType(itemBuilder12.build(), PotionTypes.HEALING), ItemEditor.Option.POTIONTYPES));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.POTION.parseItem());
         List multiple = Lang.DISABLED_POTIONTYPES_OPTION.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         itemBuilder12.hideFlags();
         values.add(itemBuilder12.build());
      }

      if (this.isSpawner(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.BLAZE_SPAWN_EGG.parseItem());
         itemBuilder12.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SPAWNER_TYPE.get()));
         if (config.getString(text2 + ".spawnertype") != null && !config.getString(text2 + ".spawnertype").isEmpty()) {
            itemBuilder12.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SPAWNER_TYPE.get()).builder().append(config.getString(text2 + ".spawnertype")).build()
               )
            );
         }

         values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.SPAWNERTYPE));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.BLAZE_SPAWN_EGG.parseItem());
         List multiple = Lang.DISABLED_SPAWNER_TYPE_OPTION.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder12.build());
      }

      if (this.isKnowledgeBook(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.KNOWLEDGE_BOOK.parseItem());
         itemBuilder12.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_RECIPE.get()));
         if (!config.getStringList(text2 + ".recipes").isEmpty()) {
            ArrayList values2 = new ArrayList();
            values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_RECIPE.get()));
            config.getStringList(text2 + ".recipes").forEach(entry -> values2.add(SendMessage.fromConfig("- " + entry)));
            itemBuilder12.setCLore(values2);
         }

         values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.RECIPES));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.KNOWLEDGE_BOOK.parseItem());
         List multiple = Lang.DISABLED_RECIPE_OPTION.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder12.build());
      }

      if (this.isPlayerHead(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem());
         itemBuilder12.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SKULLOWNER.get()));
         if (config.getString(text2 + ".skullowner") != null && !config.getString(text2 + ".skullowner").isEmpty()) {
            itemBuilder12.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE
                     .get()
                     .replace("%option%", Lang.OPTION_SKULLOWNER.get())
                     .builder()
                     .append(SendMessage.fromConfig(config.getString(text2 + ".skullowner")))
                     .build()
               )
            );
         }

         values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.SKULLOWNER));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem());
         List multiple = Lang.DISABLED_SKULLOWNER_OPTION.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder12.build());
      }

      if (this.isLeatherArmor(itemId, text2)) {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.LEATHER_CHESTPLATE.parseItem());
         itemBuilder12.setArmorColor(Color.fromRGB(39680));
         itemBuilder12.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_ARMORCOLOR.get()));
         if (config.getString(text2 + ".armorcolor") != null && !config.getString(text2 + ".armorcolor").isEmpty()) {
            itemBuilder12.setCLore(
               Arrays.asList(
                  Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_ARMORCOLOR.get()).builder().append(config.getString(text2 + ".armorcolor")).build()
               )
            );
         }

         if (this.plugin.version >= 117) {
            itemBuilder12.hideFlags();
         }

         values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.ARMORCOLOR));
      } else {
         ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.LEATHER_CHESTPLATE.parseItem());
         itemBuilder12.setArmorColor(Color.RED);
         List multiple = Lang.DISABLED_ARMORCOLOR_OPTION.getMultiple();
         itemBuilder12.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder12.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         if (this.plugin.version >= 117) {
            itemBuilder12.hideFlags();
         }

         values.add(itemBuilder12.build());
      }

      ItemBuilder itemBuilder12 = new ItemBuilder(XMaterial.WRITABLE_BOOK.parseItem());
      itemBuilder12.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_NBTDATA.get()));
      if (config.getString(text2 + ".NBTData") != null && !config.getString(text2 + ".NBTData").isEmpty()) {
         itemBuilder12.setCLore(
            Arrays.asList(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_NBTDATA.get()).builder().append(config.getString(text2 + ".NBTData")).build())
         );
      }

      values.add(this.setEditOption(itemBuilder12.build(), ItemEditor.Option.NBTDATA));
      ItemBuilder itemBuilder13 = new ItemBuilder(XMaterial.CHAIN_COMMAND_BLOCK.parseItem());
      itemBuilder13.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_BUY_COMMANDS.get()));
      if (!config.getStringList(text2 + ".buy-commands").isEmpty()) {
         ArrayList values2 = new ArrayList();
         values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_BUY_COMMANDS.get()));
         config.getStringList(text2 + ".buy-commands").forEach(entry -> values2.add(SendMessage.fromConfig("&f- " + entry)));
         itemBuilder13.setCLore(values2);
      }

      values.add(this.setEditOption(itemBuilder13.build(), ItemEditor.Option.BUY_COMMANDS));
      ItemBuilder itemBuilder14 = new ItemBuilder(XMaterial.CHAIN_COMMAND_BLOCK.parseItem());
      itemBuilder14.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SELL_COMMANDS.get()));
      if (!config.getStringList(text2 + ".sell-commands").isEmpty()) {
         ArrayList values2 = new ArrayList();
         values2.add(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SELL_COMMANDS.get()));
         config.getStringList(text2 + ".sell-commands").forEach(entry -> values2.add(SendMessage.fromConfig("&f- " + entry)));
         itemBuilder14.setCLore(values2);
      }

      values.add(this.setEditOption(itemBuilder14.build(), ItemEditor.Option.SELL_COMMANDS));
      ItemBuilder itemBuilder15 = new ItemBuilder(XMaterial.ENDER_CHEST.parseItem());
      if (config.contains(text2 + ".stock-limit-player")) {
         List<Translatable> multiple = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         multiple.forEach(entry -> entry.replace("%option%", Lang.GLOBAL_STOCK_OPTIONS.get()).replace("%option2%", Lang.OPTION_PLAYER_STOCK.get()));
         itemBuilder15.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder15.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder15.build());
      } else {
         itemBuilder15.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.GLOBAL_STOCK_OPTIONS.get()));
         values.add(this.setEditOption(itemBuilder15.build(), ItemEditor.Option.GLOBAL_STOCK));
      }

      ItemBuilder itemBuilder16 = new ItemBuilder(XMaterial.CHEST.parseItem());
      if (config.contains(text2 + ".stock-limit-global")) {
         List<Translatable> multiple = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         multiple.forEach(entry -> entry.replace("%option%", Lang.PLAYER_STOCK_OPTIONS.get()).replace("%option2%", Lang.OPTION_GLOBAL_STOCK.get()));
         itemBuilder16.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder16.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         values.add(itemBuilder16.build());
      } else {
         itemBuilder16.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.PLAYER_STOCK_OPTIONS.get()));
         values.add(this.setEditOption(itemBuilder16.build(), ItemEditor.Option.PLAYER_STOCK));
      }

      if (this.plugin.dynamicPricing) {
         ItemBuilder itemBuilder17 = new ItemBuilder(shopItem.isDynamicPricing() ? XMaterial.COMPARATOR.parseItem() : XMaterial.REPEATER.parseItem());
         itemBuilder17.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.DYNAMIC_PRICING_OPTIONS.get()));
         if (shopItem.isDynamicPricing()) {
            itemBuilder17.withEnchantGlint();
         }

         values.add(this.setEditOption(itemBuilder17.build(), ItemEditor.Option.DYNAMIC_PRICING));
      } else {
         ItemBuilder itemBuilder17 = new ItemBuilder(shopItem.isDynamicPricing() ? XMaterial.COMPARATOR.parseItem() : XMaterial.REPEATER.parseItem());
         itemBuilder17.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.DYNAMIC_PRICING_OPTIONS.get()));
         itemBuilder17.setCLore(Lang.DISABLED_DYNAMIC_PRICING_OPTION.getMultiple());
         values.add(itemBuilder17.build());
      }

      ItemBuilder itemBuilder17 = new ItemBuilder(shopItem.isLinked() ? XMaterial.ENDER_EYE.parseItem() : XMaterial.ENDER_PEARL.parseItem());
      itemBuilder17.setCDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_SUBSECTION.get()));
      if (shopItem.isLinked()) {
         itemBuilder17.setCLore(
            Arrays.asList(
               Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_SUBSECTION.get()).builder().append(ChatColor.WHITE + shopItem.getSubSection()).build()
            )
         );
      }

      values.add(this.setEditOption(itemBuilder17.build(), ItemEditor.Option.SUB_SECTION));
      return values;
   }

   private ItemStack setEditOption(ItemStack itemStack, ItemEditor.Option option) {
      return this.plugin.versionHandler.setEditOption(itemStack, option.getKey());
   }

   public List<ItemStack> getAvailableActions(String itemId, String text, String text2) {
      ArrayList values = new ArrayList();
      ItemBuilder itemBuilder = new ItemBuilder(XMaterial.BARRIER.parseItem());
      itemBuilder.setDisplayName(Lang.DISABLED_ACTION.get());
      ItemBuilder itemBuilder2 = new ItemBuilder(XMaterial.EMERALD_BLOCK.parseItem());
      itemBuilder2.setDisplayName(Lang.ADD_ACTION.get().replace("%key%", text2));
      ItemBuilder itemBuilder3 = new ItemBuilder(XMaterial.NAME_TAG.parseItem());
      itemBuilder3.setDisplayName(Lang.EDIT_ACTION.get().replace("%key%", text2));
      ItemBuilder itemBuilder4 = new ItemBuilder(XMaterial.REDSTONE_BLOCK.parseItem());
      itemBuilder4.setDisplayName(Lang.REMOVE_ACTION.get().replace("%key%", text2));
      text2 = text2.equals("global stock")
         ? "stock-limit-global"
         : (
            text2.equals("player stock")
               ? "stock-limit-player"
               : (text2.equals("global stock timer") ? "auto-restock-global" : (text2.equals("player stock timer") ? "auto-restock-player" : text2))
         );
      if (ConfigManager.getShop(itemId).get("pages." + text + "." + text2) == null) {
         values.add(0, this.plugin.versionHandler.setAction(itemBuilder2.build(), "ADD"));
         values.add(1, itemBuilder.build());
         values.add(2, itemBuilder.build());
      } else if (this.allListKeys.contains(text2)) {
         values.add(0, this.plugin.versionHandler.setAction(itemBuilder2.build(), "ADD"));
         if (ConfigManager.getShop(itemId).getConfigurationSection("pages." + text).getStringList(text2).isEmpty()) {
            values.add(1, itemBuilder.build());
            values.add(2, itemBuilder.build());
         } else {
            values.add(1, this.plugin.versionHandler.setAction(itemBuilder3.build(), "EDIT"));
            values.add(2, this.plugin.versionHandler.setAction(itemBuilder4.build(), "REMOVE"));
         }
      } else {
         values.add(0, itemBuilder.build());
         values.add(1, this.plugin.versionHandler.setAction(itemBuilder3.build(), "EDIT"));
         if (text2.equalsIgnoreCase("material")) {
            values.add(2, itemBuilder.build());
         } else {
            values.add(2, this.plugin.versionHandler.setAction(itemBuilder4.build(), "REMOVE"));
         }
      }

      return values;
   }

   public List<String> getMultipleItemLore(String... itemIds) {
      ArrayList lores = new ArrayList();

      for (String text : itemIds) {
         if (text.contains("\n")) {
            lores.addAll(Arrays.asList(text.split("\n")));
         } else {
            lores.add(text);
         }
      }

      return lores;
   }

   private Material getMaterial(String materialName, String text) {
      String text2 = ConfigManager.getShop(materialName).getString(text + ".material");
      if (text2 == null || text2.isEmpty()) {
         return null;
      } else if (!text2.contains(":")) {
         Optional optional = XMaterial.matchXMaterial(text2);
         if (!optional.isPresent()) {
            return null;
         } else {
            ItemStack itemStack = ((XMaterial)optional.get()).parseItem();
            return itemStack == null ? null : itemStack.getType();
         }
      } else {
         try {
            return OnyxShop.getInstance().getItemManager().getSPM(null, text2).getType();
         } catch (ItemLoadException exception) {
            SendMessage.warnMessage("Could not get material value of item: '" + text2 + "'");
            return null;
         }
      }
   }

   private boolean isFirework(String text, String text2) {
      Material material = this.getMaterial(text, text2);
      return material == null ? false : material.equals(XMaterial.FIREWORK_ROCKET.parseMaterial());
   }

   private boolean isLeatherArmor(String text, String text2) {
      Material material = this.getMaterial(text, text2);
      if (material == null) {
         return false;
      } else {
         ItemStack itemStack = new ItemStack(material);
         return itemStack.getItemMeta() instanceof LeatherArmorMeta;
      }
   }

   private boolean isPlayerHead(String playerName, String text) {
      Material material = this.getMaterial(playerName, text);
      return material == null ? false : XMaterial.matchXMaterial(material).equals(XMaterial.PLAYER_HEAD);
   }

   private boolean isKnowledgeBook(String text, String text2) {
      Material material = this.getMaterial(text, text2);
      return material == null ? false : XMaterial.matchXMaterial(material).equals(XMaterial.KNOWLEDGE_BOOK);
   }

   private boolean isSpawner(String text, String text2) {
      Material material = this.getMaterial(text, text2);
      return material == null ? false : XMaterial.matchXMaterial(material).equals(XMaterial.SPAWNER);
   }

   private boolean isPotion(String text, String text2) {
      Material material = this.getMaterial(text, text2);
      return material == null ? false : PotionTypes.canHaveEffects(material);
   }

   public ItemStack getPotion(String itemId, String text, String text2) {
      ItemBuilder itemBuilder = new ItemBuilder(Material.BARRIER);
      itemBuilder.setDisplayName(itemId);
      Material material = this.getMaterial(text, text2);
      if (material == null) {
         return itemBuilder.build();
      } else {
         Optional optional = PotionTypes.matchPotionType(itemId);
         if (optional.isPresent() && ((PotionTypes)optional.get()).isSupported()) {
            boolean flag = XMaterial.matchXMaterial(ConfigManager.getShop(text).getString(text2 + ".material")).get() == XMaterial.SPLASH_POTION;
            return ((PotionTypes)optional.get()).addEffect(new ItemStack(material), flag);
         } else {
            return itemBuilder.build();
         }
      }
   }

   public ItemStack getBook(String itemId) {
      try {
         return this.plugin.enchantsProvider.getEnchant(itemId).getBook();
      } catch (ItemLoadException exception) {
         ItemBuilder itemBuilder = new ItemBuilder(Material.BARRIER);
         itemBuilder.setDisplayName(itemId);
         return itemBuilder.build();
      }
   }

   public SimpleEnchantment getChosenEnchantment(String text, String text2, Integer value) {
      List enchantsOfItem = this.plugin.enchantsProvider.getEnchantsOfItem(this.plugin.getShopItem(text, text2).getItemToGive());
      if (enchantsOfItem.size() == 1) {
         value = 0;
      }

      return value >= 0 && value < enchantsOfItem.size() ? (SimpleEnchantment)enchantsOfItem.get(value) : null;
   }

   public PotionEffect getChosenPotionEffectType(String type, String text, int value) {
      if (ConfigManager.getShop(type).getStringList("pages." + text + ".potiontypes").size() == 1) {
         value = 0;
      }

      if (value >= 0 && value < ConfigManager.getShop(type).getStringList("pages." + text + ".potiontypes").size()) {
         Optional optional = PotionTypes.matchPotionType((String)ConfigManager.getShop(type).getStringList("pages." + text + ".potiontypes").get(value));
         if (optional.isPresent() && ((PotionTypes)optional.get()).isSupported()) {
            return ((PotionTypes)optional.get()).parsePotionEffect();
         }
      }

      return null;
   }

   public void loadItemChanges(ItemEditor.Action action, ItemEditor.Option option, String itemId) {
      label26:
      switch (action) {
         case EDIT:
         case ADD:
         case REMOVE:
            if (option == ItemEditor.Option.SLOT) {
               this.plugin.addSection(itemId.split("\\.")[0], new ShopCategory(this.plugin, itemId.split("\\.")[0]));
            } else {
               this.plugin.getSection(itemId.split("\\.")[0]).reloadItem(itemId);
               switch (option) {
                  case BUY_PRICE:
                  case SELL_PRICE:
                     if (this.plugin.getShopItem(itemId).isDynamicPricing()) {
                        SendMessage.logDebugMessage("Detected price change for item '" + itemId + "', reloading DynamicPricing...");
                     }
                  case DYNAMIC_PRICING_ENABLE:
                  case DYNAMIC_PRICING_MAX_STOCK:
                     if (this.plugin.getShopItem(itemId).isDynamicPricing()) {
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
            this.plugin.getSection(itemId.split("\\.")[0]).addNewItem(itemId);
            break;
         case REMOVE_ITEM:
            this.plugin.addSection(itemId.split("\\.")[0], new ShopCategory(this.plugin, itemId.split("\\.")[0]));
      }

      if (!this.plugin.shopItemsByMaterialName.isEmpty()) {
         this.plugin.shopItemsByMaterialName.clear();
      }

      this.plugin.startupReload.getShopItemsByMaterialName();
   }

   private String getNextAvailablePosition(String text) {
      Config config = ConfigManager.getShop(text);
      int size = config.getConfigurationSection("pages").getKeys(false).size();
      if (size == 0) {
         return "page1.items.1";
      } else {
         LinkedList sectionIds = new LinkedList(config.getConfigurationSection("pages").getKeys(false));
         String text2 = (String)sectionIds.get(size - 1);
         int value = config.getInt("pages." + text2 + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(text) ? 9 : 0);
         if (config.getConfigurationSection("pages." + text2 + ".items").getKeys(false).size() >= value) {
            return "page" + (sectionIds.size() + 1) + ".items.1";
         } else {
            Set keys = config.getConfigurationSection("pages." + text2 + ".items").getKeys(false);
            int value2 = keys.size() + 1;

            while (keys.contains(String.valueOf(value2))) {
               value2++;
            }

            return text2 + ".items." + value2;
         }
      }
   }

   public void openItemEditorCreateItem(Player player, ShopItem shopItem, String itemId, Double amount, Double amount2) {
      if (itemId == null) {
         new ChooseValue(player, shopItem, ItemEditor.Option.MATERIAL, ItemEditor.Action.ADD, -1, 1);
      } else if (amount == null) {
         new AnvilGUI.Builder()
            .onComplete((player2, input) -> {
               try {
                  Double amount3 = Double.parseDouble(input);
                  this.plugin.runTaskLaterFor(() -> this.openItemEditorCreateItem(player, shopItem, itemId, amount3, null), player, 1L);
                  return AnvilGUI.Response.close();
               } catch (NumberFormatException exception) {
                  return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
               }
            })
            .onClose(player2 -> this.plugin.runTaskLaterFor(() -> this.openItemEditorCreateItem(player2, shopItem, null, null, null), player2, 1L))
            .text(Lang.OPTION_BUY_PRICE.get().getLegacy())
            .itemLeft(new ItemStack(Material.PAPER))
            .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", Lang.OPTION_BUY_PRICE.get()).getLegacy())
            .plugin(this.plugin)
            .open(player);
      } else if (amount2 == null) {
         new AnvilGUI.Builder()
            .onComplete((player2, input) -> {
               try {
                  Double amount3 = Double.parseDouble(input);
                  this.plugin.runTaskLaterFor(() -> this.openItemEditorCreateItem(player, shopItem, itemId, amount, amount3), player, 1L);
                  return AnvilGUI.Response.close();
               } catch (NumberFormatException exception) {
                  return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
               }
            })
            .onClose(player2 -> this.plugin.runTaskLaterFor(() -> this.openItemEditorCreateItem(player2, shopItem, itemId, null, null), player2, 1L))
            .text(Lang.OPTION_SELL_PRICE.get().getLegacy())
            .itemLeft(new ItemStack(Material.PAPER))
            .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", Lang.OPTION_SELL_PRICE.get()).getLegacy())
            .plugin(this.plugin)
            .open(player);
      } else {
         String nextAvailablePosition = this.getNextAvailablePosition(shopItem.section);
         ConfigManager.getShop(shopItem.section).set("pages." + nextAvailablePosition + ".material", itemId);
         ConfigManager.getShop(shopItem.section).set("pages." + nextAvailablePosition + ".buy", amount);
         ConfigManager.getShop(shopItem.section).set("pages." + nextAvailablePosition + ".sell", amount2);
         ConfigManager.saveShop(shopItem.section);
         new ConclusionScreen(player, shopItem, null, ItemEditor.Action.ADD_ITEM, shopItem.section + "." + nextAvailablePosition);
      }
   }

   public void openAvailableResultsForOption(Player player, ItemEditor.Option option, ItemEditor.Action action, ShopItem shopItem, int value) {
      boolean flag = false;
      switch (option) {
         case MATERIAL:
            new ChooseValue(player, shopItem, option, action, value, 1);
            return;
         case BUY_PRICE:
         case SELL_PRICE:
         case SLOT:
         case STACK_SIZE:
         case DYNAMIC_PRICING_MAX_STOCK:
            if (action != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((player2, input) -> {
                     try {
                        Object value2;
                        if (option != ItemEditor.Option.BUY_PRICE && option != ItemEditor.Option.SELL_PRICE) {
                           value2 = Integer.parseInt(input);
                        } else {
                           value2 = Double.parseDouble(input);
                        }

                        ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), value2);
                        ConfigManager.saveShop(shopItem.section);
                        this.plugin.runTaskLaterFor(() -> new ConclusionScreen(player, shopItem, option, action, null), player, 1L);
                        return AnvilGUI.Response.close();
                     } catch (NumberFormatException exception) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(player2 -> this.plugin.runTaskLaterFor(() -> new AvailableActions(player, shopItem, option), player2, 1L))
                  .text(
                     action != ItemEditor.Action.ADD
                        ? (
                           option == ItemEditor.Option.BUY_PRICE
                              ? String.valueOf(shopItem.getBuyPrice())
                              : (
                                 option == ItemEditor.Option.SELL_PRICE
                                    ? String.valueOf(shopItem.getSellPrice())
                                    : (
                                       option == ItemEditor.Option.SLOT
                                          ? String.valueOf(this.plugin.getSection(shopItem.section).getItemLocs().indexOf(shopItem.itemLoc))
                                          : (option == ItemEditor.Option.STACK_SIZE ? String.valueOf(shopItem.getStackSize()) : option.getName())
                                    )
                              )
                        )
                        : option.getName()
                  )
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", option.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(player);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case NAME:
         case DISPLAYNAME:
         case NBTDATA:
         case PLAYER_STOCK_FREQUENCY:
         case GLOBAL_STOCK_FREQUENCY:
            if (action != ItemEditor.Action.REMOVE) {
               player.closeInventory();
               this.convFactory.startConversation(player, option, action, shopItem.getItemPath(), value);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case LORE:
         case DISPLAYLORE:
         case BUY_COMMANDS:
         case SELL_COMMANDS:
            if (action == ItemEditor.Action.ADD) {
               player.closeInventory();
               this.convFactory.startConversation(player, option, action, shopItem.getItemPath(), value);
               return;
            }

            if (action == ItemEditor.Action.EDIT) {
               if (ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey()).size() == 1) {
                  player.closeInventory();
                  this.convFactory.startConversation(player, option, action, shopItem.getItemPath(), value);
               } else if (value == -1) {
                  new IndexChooser(player, shopItem, option, action);
               } else {
                  player.closeInventory();
                  this.convFactory.startConversation(player, option, action, shopItem.getItemPath(), value);
               }

               return;
            }

            if (action == ItemEditor.Action.REMOVE) {
               if (ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey()).size() == 1) {
                  ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
                  flag = true;
               } else {
                  if (value == -1) {
                     new IndexChooser(player, shopItem, option, action);
                     return;
                  }

                  List stringList = ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey());
                  stringList.remove(value);
                  ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), stringList);
                  flag = true;
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
            if (action == ItemEditor.Action.ADD) {
               new ChooseValue(player, shopItem, option, action, value, 1);
               return;
            }

            if (action == ItemEditor.Action.EDIT) {
               if (ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey()).size() == 1) {
                  new ChooseValue(player, shopItem, option, action, value, 1);
               } else if (value == -1) {
                  new IndexChooser(player, shopItem, option, action);
               } else {
                  new ChooseValue(player, shopItem, option, action, value, 1);
               }

               return;
            }

            if (action == ItemEditor.Action.REMOVE) {
               if (ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey()).size() == 1) {
                  ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
                  flag = true;
               } else {
                  if (value == -1) {
                     new IndexChooser(player, shopItem, option, action);
                     return;
                  }

                  List stringList = ConfigManager.getShop(shopItem.section).getStringList("pages." + shopItem.itemLoc + "." + option.getKey());
                  stringList.remove(value);
                  ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), stringList);
                  flag = true;
               }
            }
            break;
         case SPAWNERTYPE:
            if (action != ItemEditor.Action.REMOVE) {
               new ChooseValue(player, shopItem, option, action, value, 1);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case SKULLOWNER:
            if (action != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((player2, input) -> {
                     ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), input);
                     ConfigManager.saveShop(shopItem.section);
                     this.plugin.runTaskLaterFor(() -> new ConclusionScreen(player, shopItem, option, action, null), player, 1L);
                     return AnvilGUI.Response.close();
                  })
                  .onClose(player2 -> this.plugin.runTaskLaterFor(() -> new AvailableActions(player, shopItem, option), player2, 1L))
                  .text(option.getName())
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", option.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(player);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case ARMORCOLOR:
            if (action != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((player2, input) -> {
                     input = input.replace("#", "");
                     if (input.startsWith("0x")) {
                        input = input.substring(2);
                     }

                     if (input.length() == 6) {
                        ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), input);
                        ConfigManager.saveShop(shopItem.section);
                        this.plugin.runTaskLaterFor(() -> new ConclusionScreen(player, shopItem, option, action, null), player, 1L);
                        return AnvilGUI.Response.close();
                     } else {
                        return AnvilGUI.Response.text("0x00FF00");
                     }
                  })
                  .onClose(player2 -> this.plugin.runTaskLaterFor(() -> new AvailableActions(player, shopItem, option), player2, 1L))
                  .text("0x00FF00")
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", option.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(player);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case PLAYER_STOCK_LIMIT:
         case GLOBAL_STOCK_LIMIT:
            if (action != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((player2, input) -> {
                     try {
                        ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), Integer.parseInt(input));
                        ConfigManager.saveShop(shopItem.section);
                        this.plugin.runTaskLaterFor(() -> new ConclusionScreen(player, shopItem, option, action, null), player, 1L);
                        return AnvilGUI.Response.close();
                     } catch (NumberFormatException exception) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(player2 -> this.plugin.runTaskLaterFor(() -> new AvailableActions(player, shopItem, option), player2, 1L))
                  .text(option.getName())
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", option.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(player);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
            break;
         case PLAYER_STOCK_TIMER:
         case GLOBAL_STOCK_TIMER:
            if (action != ItemEditor.Action.REMOVE) {
               new AnvilGUI.Builder()
                  .onComplete((player2, input) -> {
                     try {
                        TimeUtils.getTime(input);
                        ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), input);
                        ConfigManager.saveShop(shopItem.section);
                        this.plugin.runTaskLaterFor(() -> new ConclusionScreen(player, shopItem, option, action, null), player, 1L);
                        return AnvilGUI.Response.close();
                     } catch (ParseException exception) {
                        return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                     }
                  })
                  .onClose(player2 -> this.plugin.runTaskLaterFor(() -> new AvailableActions(player, shopItem, option), player2, 1L))
                  .text("5d 12h 30m 10s")
                  .itemLeft(new ItemStack(Material.PAPER))
                  .title(Lang.ANVILGUI_ENTER_VALUE.get().replace("%option%", option.getName()).getLegacy())
                  .plugin(this.plugin)
                  .open(player);
               return;
            }

            ConfigManager.getShop(shopItem.section).set("pages." + shopItem.itemLoc + "." + option.getKey(), null);
            flag = true;
      }

      if (flag) {
         ConfigManager.saveShop(shopItem.section);
         new ConclusionScreen(player, shopItem, option, action, null);
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

      private Option(String text, Translatable text2) {
         this.key = text;
         this.name = text2 != null ? text2.getLegacy() : null;
      }

      public static ItemEditor.Option getFromKey(String key) {
         for (ItemEditor.Option option : values()) {
            if (option.getKey().equals(key)) {
               return option;
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
