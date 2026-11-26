package com.onyxshop.api.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

public class PreTransactionEvent extends CustomEvent implements Cancellable {
   private boolean cancelled;
   private final int amount;
   private double price;
   private final double originalPrice;
   private final Map<EcoType, Double> prices;
   private final Map<EcoType, Double> originalPrices;
   private final ShopItem shopItem;
   private final Map<ShopItem, Integer> items;
   private final Player player;
   private final Transaction.Type transactionType;

   public PreTransactionEvent(ShopItem var1, Player var2, int var3, double var4, Transaction.Type var6) {
      this.amount = var3;
      this.shopItem = var1;
      this.player = var2;
      this.price = var4;
      this.originalPrice = this.price;
      this.prices = new HashMap<>();
      this.originalPrices = this.prices;
      this.items = new HashMap<>();
      this.transactionType = var6;
   }

   public PreTransactionEvent(Map<ShopItem, Integer> var1, Map<EcoType, Double> var2, Player var3, int var4, Transaction.Type var5) {
      this.amount = var4;
      this.shopItem = var1.size() >= 1 ? (ShopItem)var1.keySet().toArray()[0] : null;
      this.player = var3;
      this.price = var2 != null && !var2.isEmpty() ? (Double)var2.values().toArray()[0] : 0.0;
      this.originalPrice = this.price;
      this.prices = var2;
      this.originalPrices = Collections.unmodifiableMap(this.prices);
      this.items = var1;
      this.transactionType = var5;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean var1) {
      this.cancelled = var1;
   }

   public int getAmount() {
      return this.amount;
   }

   public double getPrice() {
      return this.price;
   }

   public final double getOriginalPrice() {
      return this.originalPrice;
   }

   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   public final Map<EcoType, Double> getOriginalPrices() {
      return this.originalPrices;
   }

   public void setPrice(double var1) {
      this.price = var1;
   }

   @Deprecated
   public ItemStack getItemStack() {
      return this.shopItem.getItemToGive();
   }

   public ShopItem getShopItem() {
      return this.shopItem;
   }

   public Map<ShopItem, Integer> getItems() {
      return this.items;
   }

   public Player getPlayer() {
      return this.player;
   }

   public Transaction.Type getTransactionType() {
      return this.transactionType;
   }
}
