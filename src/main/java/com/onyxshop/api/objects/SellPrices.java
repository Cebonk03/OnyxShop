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

   public SellPrices(OfflinePlayer var1, Map<ShopItem, Integer> var2, Map<EcoType, Double> var3) {
      this.player = var1;
      this.items = var2;
      this.prices = var3;
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

   public double getPrice(@NotNull EcoType var1) {
      return this.prices.getOrDefault(var1, -1.0);
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public SellPrices updateLimits() {
      for (ShopItem var2 : this.items.keySet()) {
         if (var2.isRefillStock()) {
            OnyxShopHook.sellItemStock(var2, this.player.getUniqueId(), this.items.get(var2));
         }

         if (var2.getLimitedSellMode() != 0) {
            OnyxShopHook.sellItemLimit(var2, this.player.getUniqueId(), this.items.get(var2));
         }

         if (var2.isDynamicPricing()) {
            OnyxShopHook.sellItem(var2, this.items.get(var2));
         }
      }

      return this;
   }
}
