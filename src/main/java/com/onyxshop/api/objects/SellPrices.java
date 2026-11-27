package com.onyxshop.api.objects;

import java.util.Map;
import com.onyxshop.api.OnyxShopHook;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class SellPrices {
   private final OfflinePlayer player;
   private final Map<ShopItem, Integer> items;
   private final Map<EcoType, Double> prices;

   public SellPrices(OfflinePlayer offlinePlayer, Map<ShopItem, Integer> map, Map<EcoType, Double> map2) {
      this.player = offlinePlayer;
      this.items = map;
      this.prices = map2;
   }

   public OfflinePlayer getPlayer() {
      return this.player;
   }

   public Map<ShopItem, Integer> getItems() {
      return this.items;
   }

   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   public double getPrice(@NotNull EcoType ecoType) {
      return this.prices.getOrDefault(ecoType, -1.0);
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public SellPrices updateLimits() {
      for (ShopItem shopItem : this.items.keySet()) {
         if (shopItem.isRefillStock()) {
            OnyxShopHook.sellItemStock(shopItem, this.player.getUniqueId(), this.items.get(shopItem));
         }

         if (shopItem.getLimitedSellMode() != 0) {
            OnyxShopHook.sellItemLimit(shopItem, this.player.getUniqueId(), this.items.get(shopItem));
         }

         if (shopItem.isDynamicPricing()) {
            OnyxShopHook.sellItem(shopItem, this.items.get(shopItem));
         }
      }

      return this;
   }
}
