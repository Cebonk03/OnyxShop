package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.RotatingShopProvider;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class QueryBuilder {
   private final OnyxShop plugin;
   private final Map<String, List<ShopItem>> itemsByType = new HashMap<>();
   private final Map<String, List<ShopItem>> itemsByName = new TreeMap<>();
   private List<String> bannedShops = new ArrayList<>();
   private boolean includeSubShops = true;
   private boolean includeHidden = false;

   public QueryBuilder(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void reload() {
      if (!this.itemsByType.isEmpty()) {
         this.itemsByType.clear();
      }

      if (!this.itemsByName.isEmpty()) {
         this.itemsByName.clear();
      }

      if (this.plugin.shopSearch) {
         this.bannedShops = ConfigManager.getConfig().getStringList("shop-search.excluded-shops");
         this.includeSubShops = ConfigManager.getConfig().getBoolean("shop-search.sub-shops");
         this.includeHidden = ConfigManager.getConfig().getBoolean("shop-search.hidden-shops");
         this.cacheItemsByType();
         this.cacheItemsByName();
      }
   }

   public void rotate(RotatingShopProvider rotatingShopProvider) {
      this.itemsByType.values().forEach(entry -> entry.removeIf(element -> element.section.equals(rotatingShopProvider.getSection())));
      this.itemsByName.values().forEach(entry -> entry.removeIf(element -> element.section.equals(rotatingShopProvider.getSection())));
      if (!this.bannedShops.contains(rotatingShopProvider.getSection()) && (this.includeHidden || !rotatingShopProvider.isHidden()) && (this.includeSubShops || !rotatingShopProvider.isSubSection())) {
         for (ShopItem shopItem : rotatingShopProvider.getItemsInRotation()) {
            String shopId = shopItem.getShopItem().getType().name();
            this.itemsByType.computeIfAbsent(shopId, key -> new ArrayList<>()).add(shopItem);
            this.itemsByName.computeIfAbsent(shopItem.getDisplayname().toLowerCase(Locale.ENGLISH), key -> new ArrayList<>()).add(shopItem);
         }
      }
   }

   private void cacheItemsByType() {
      for (String type : this.plugin.getShopSections().stream().filter(element -> !this.bannedShops.contains(element)).collect(Collectors.toList())) {
         ShopSection section = this.plugin.getSection(type);
         if ((section.getType() != ShopType.ROTATING || ((RotatingShop)section).getItemsInRotation() != null)
            && (this.includeHidden || !section.isHidden())
            && (this.includeSubShops || !section.isSubSection())
            && !this.bannedShops.contains(section.getSection())) {
            for (ShopItem shopItem : section.getType() == ShopType.ROTATING ? ((RotatingShop)section).getItemsInRotation() : section.getShopItems()) {
               if (!shopItem.hasItemError() && !shopItem.isDisplayItem()) {
                  String shopId = shopItem.getShopItem().getType().name();
                  List values = this.itemsByType.computeIfAbsent(shopId, key -> new ArrayList<>());
                  values.add(shopItem);
               }
            }
         }
      }
   }

   private void cacheItemsByName() {
      for (String name : this.plugin.getShopSections().stream().filter(element -> !this.bannedShops.contains(element)).collect(Collectors.toList())) {
         ShopSection section = this.plugin.getSection(name);
         if ((section.getType() != ShopType.ROTATING || ((RotatingShop)section).getItemsInRotation() != null)
            && (this.includeHidden || !section.isHidden())
            && (this.includeSubShops || !section.isSubSection())
            && !this.bannedShops.contains(section.getSection())) {
            for (ShopItem shopItem : section.getType() == ShopType.ROTATING ? ((RotatingShop)section).getItemsInRotation() : section.getShopItems()) {
               if (!shopItem.hasItemError() && !shopItem.isDisplayItem()) {
                  String text = shopItem.getDisplayname().toLowerCase(Locale.ENGLISH);
                  List values = this.itemsByName.computeIfAbsent(text, key -> new ArrayList<>());
                  values.add(shopItem);
                  ItemStack itemStack = shopItem.getShopItem();
                  if (this.plugin.versionHandler.hasData(itemStack, NBTMaps.ENCHANTMENTS.getTag())
                     || this.plugin.versionHandler.hasData(itemStack, NBTMaps.STORED_ENCHANTMENTS.getTag())) {
                     for (SimpleEnchantment simpleEnchantment : OnyxShop.getInstance().enchantsProvider.getEnchantsOfItem(itemStack)) {
                        if (simpleEnchantment.getProvider() == EnchantsProvider.Provider.VANILLA) {
                           for (String text2 : XEnchantment.matchXEnchantment((Enchantment)simpleEnchantment.getEnchant()).getNames(itemStack)) {
                              List values2 = this.itemsByName
                                 .computeIfAbsent(text2.toLowerCase(Locale.ENGLISH) + " " + simpleEnchantment.getLvl(), key -> new ArrayList<>());
                              values2.add(shopItem);
                           }
                        } else {
                           List values2 = this.itemsByName
                              .computeIfAbsent(simpleEnchantment.getName().toLowerCase(Locale.ENGLISH) + " " + simpleEnchantment.getLvl(), key -> new ArrayList<>());
                           values2.add(shopItem);
                        }
                     }
                  }

                  if (this.plugin.versionHandler.hasData(itemStack, NBTMaps.BASE_POTION.getTag().split("::")[0])) {
                     for (PotionTypes potionTypes : PotionTypes.getPotionTypes(itemStack)) {
                        for (String text2 : potionTypes.getNames(itemStack)) {
                           List values2 = this.itemsByName.computeIfAbsent(text2.toLowerCase(Locale.ENGLISH), key -> new ArrayList<>());
                           values2.add(shopItem);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public LinkedList<ShopItem> queryByName(String name) {
      name = name.toLowerCase(Locale.ENGLISH);
      LinkedList values = new LinkedList();
      String[] values2 = name.split(" ");

      for (Entry entry : this.itemsByName.entrySet()) {
         if (values2.length == 1 ? ((String)entry.getKey()).contains(name) : Arrays.stream(values2).anyMatch(element -> ((String)entry.getKey()).contains(element))) {
            values.addAll((Collection)entry.getValue());
         }
      }

      return values;
   }

   public LinkedList<ShopItem> queryByType(String type) {
      type = type.toUpperCase(Locale.ENGLISH);
      LinkedList values = new LinkedList();
      String[] values2 = type.split(" ");

      for (Entry entry : this.itemsByType.entrySet()) {
         if (values2.length == 1 ? ((String)entry.getKey()).contains(type) : Arrays.stream(values2).anyMatch(element -> ((String)entry.getKey()).contains(element))) {
            values.addAll((Collection)entry.getValue());
         }
      }

      return values;
   }
}
