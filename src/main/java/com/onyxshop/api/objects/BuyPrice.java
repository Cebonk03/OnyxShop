package com.onyxshop.api.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.onyxshop.api.OnyxShopHook;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BuyPrice {
   private final OfflinePlayer player;
   private final int amount;
   private final ShopItem shopItem;
   private final Map<EcoType, Double> prices;

   public BuyPrice(OfflinePlayer offlinePlayer, int price, ShopItem shopItem, Map<EcoType, Double> map) {
      this.player = offlinePlayer;
      this.amount = price;
      this.shopItem = shopItem;
      this.prices = map;
   }

   public BuyPrice(OfflinePlayer offlinePlayer, int price, ShopItem shopItem, EcoType ecoType, double amount) {
      this.player = offlinePlayer;
      this.amount = price;
      this.shopItem = shopItem;
      this.prices = new HashMap<>(Collections.singletonMap(ecoType, amount));
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

   public double getPrice(@NotNull EcoType ecoType) {
      return this.prices.getOrDefault(ecoType, -1.0);
   }

   public BuyPrice updateLimits() {
      if (this.shopItem.getLimitedStockMode() != 0) {
         OnyxShopHook.buyItemStock(this.shopItem, this.player.getUniqueId(), this.amount);
      }

      if (this.shopItem.isDynamicPricing()) {
         OnyxShopHook.buyItem(this.shopItem, this.amount);
      }

      return this;
   }
}
