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

   public QueryBuilder(OnyxShop var1) {
      this.plugin = var1;
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

   public void rotate(RotatingShopProvider var1) {
      this.itemsByType.values().forEach(var1x -> var1x.removeIf(var1xx -> var1xx.section.equals(var1.getSection())));
      this.itemsByName.values().forEach(var1x -> var1x.removeIf(var1xx -> var1xx.section.equals(var1.getSection())));
      if (!this.bannedShops.contains(var1.getSection()) && (this.includeHidden || !var1.isHidden()) && (this.includeSubShops || !var1.isSubSection())) {
         for (ShopItem var3 : var1.getItemsInRotation()) {
            String var4 = var3.getShopItem().getType().name();
            this.itemsByType.computeIfAbsent(var4, var0 -> new ArrayList<>()).add(var3);
            this.itemsByName.computeIfAbsent(var3.getDisplayname().toLowerCase(Locale.ENGLISH), var0 -> new ArrayList<>()).add(var3);
         }
      }
   }

   private void cacheItemsByType() {
      for (String var2 : this.plugin.getShopSections().stream().filter(var1 -> !this.bannedShops.contains(var1)).collect(Collectors.toList())) {
         ShopSection var3 = this.plugin.getSection(var2);
         if ((var3.getType() != ShopType.ROTATING || ((RotatingShop)var3).getItemsInRotation() != null)
            && (this.includeHidden || !var3.isHidden())
            && (this.includeSubShops || !var3.isSubSection())
            && !this.bannedShops.contains(var3.getSection())) {
            for (ShopItem var5 : var3.getType() == ShopType.ROTATING ? ((RotatingShop)var3).getItemsInRotation() : var3.getShopItems()) {
               if (!var5.hasItemError() && !var5.isDisplayItem()) {
                  String var6 = var5.getShopItem().getType().name();
                  List var7 = this.itemsByType.computeIfAbsent(var6, var0 -> new ArrayList<>());
                  var7.add(var5);
               }
            }
         }
      }
   }

   private void cacheItemsByName() {
      for (String var2 : this.plugin.getShopSections().stream().filter(var1 -> !this.bannedShops.contains(var1)).collect(Collectors.toList())) {
         ShopSection var3 = this.plugin.getSection(var2);
         if ((var3.getType() != ShopType.ROTATING || ((RotatingShop)var3).getItemsInRotation() != null)
            && (this.includeHidden || !var3.isHidden())
            && (this.includeSubShops || !var3.isSubSection())
            && !this.bannedShops.contains(var3.getSection())) {
            for (ShopItem var5 : var3.getType() == ShopType.ROTATING ? ((RotatingShop)var3).getItemsInRotation() : var3.getShopItems()) {
               if (!var5.hasItemError() && !var5.isDisplayItem()) {
                  String var6 = var5.getDisplayname().toLowerCase(Locale.ENGLISH);
                  List var7 = this.itemsByName.computeIfAbsent(var6, var0 -> new ArrayList<>());
                  var7.add(var5);
                  ItemStack var8 = var5.getShopItem();
                  if (this.plugin.versionHandler.hasData(var8, NBTMaps.ENCHANTMENTS.getTag())
                     || this.plugin.versionHandler.hasData(var8, NBTMaps.STORED_ENCHANTMENTS.getTag())) {
                     for (SimpleEnchantment var10 : OnyxShop.getInstance().enchantsProvider.getEnchantsOfItem(var8)) {
                        if (var10.getProvider() == EnchantsProvider.Provider.VANILLA) {
                           for (String var12 : XEnchantment.matchXEnchantment((Enchantment)var10.getEnchant()).getNames(var8)) {
                              List var13 = this.itemsByName
                                 .computeIfAbsent(var12.toLowerCase(Locale.ENGLISH) + " " + var10.getLvl(), var0 -> new ArrayList<>());
                              var13.add(var5);
                           }
                        } else {
                           List var11 = this.itemsByName
                              .computeIfAbsent(var10.getName().toLowerCase(Locale.ENGLISH) + " " + var10.getLvl(), var0 -> new ArrayList<>());
                           var11.add(var5);
                        }
                     }
                  }

                  if (this.plugin.versionHandler.hasData(var8, NBTMaps.BASE_POTION.getTag().split("::")[0])) {
                     for (PotionTypes var15 : PotionTypes.getPotionTypes(var8)) {
                        for (String var18 : var15.getNames(var8)) {
                           List var19 = this.itemsByName.computeIfAbsent(var18.toLowerCase(Locale.ENGLISH), var0 -> new ArrayList<>());
                           var19.add(var5);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public LinkedList<ShopItem> queryByName(String var1) {
      var1 = var1.toLowerCase(Locale.ENGLISH);
      LinkedList var2 = new LinkedList();
      String[] var3 = var1.split(" ");

      for (Entry var5 : this.itemsByName.entrySet()) {
         if (var3.length == 1 ? ((String)var5.getKey()).contains(var1) : Arrays.stream(var3).anyMatch(var1x -> ((String)var5.getKey()).contains(var1x))) {
            var2.addAll((Collection)var5.getValue());
         }
      }

      return var2;
   }

   public LinkedList<ShopItem> queryByType(String var1) {
      var1 = var1.toUpperCase(Locale.ENGLISH);
      LinkedList var2 = new LinkedList();
      String[] var3 = var1.split(" ");

      for (Entry var5 : this.itemsByType.entrySet()) {
         if (var3.length == 1 ? ((String)var5.getKey()).contains(var1) : Arrays.stream(var3).anyMatch(var1x -> ((String)var5.getKey()).contains(var1x))) {
            var2.addAll((Collection)var5.getValue());
         }
      }

      return var2;
   }
}
