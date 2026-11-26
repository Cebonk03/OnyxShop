package com.onyxshop.api.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.onyxshop.api.OnyxShopHook;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class SellPrice {
   private final OfflinePlayer player;
   private final int amount;
   private final ShopItem shopItem;
   private final Map<EcoType, Double> prices;

   public SellPrice(OfflinePlayer var1, int var2, ShopItem var3, Map<EcoType, Double> var4) {
      this.player = var1;
      this.amount = var2;
      this.shopItem = var3;
      this.prices = var4;
   }

   public SellPrice(OfflinePlayer var1, int var2, ShopItem var3, EcoType var4, double var5) {
      this.player = var1;
      this.amount = var2;
      this.shopItem = var3;
      this.prices = new HashMap<>(Collections.singletonMap(var4, var5));
   }

   public OfflinePlayer getPlayer() {
      return this.player;
   }

   public int getAmount() {
      return this.amount;
   }

   public ShopItem getShopItem() {
      return this.shopItem;
   }

   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   public double getPrice(@NotNull EcoType var1) {
      return this.prices.getOrDefault(var1, -1.0);
   }

   public SellPrice updateLimits() {
      if (this.shopItem.isRefillStock()) {
         OnyxShopHook.sellItemStock(this.shopItem, this.player.getUniqueId(), this.amount);
      }

      if (this.shopItem.getLimitedSellMode() != 0) {
         OnyxShopHook.sellItemLimit(this.shopItem, this.player.getUniqueId(), this.amount);
      }

      if (this.shopItem.isDynamicPricing()) {
         OnyxShopHook.sellItem(this.shopItem, this.amount);
      }

      return this;
   }
}
