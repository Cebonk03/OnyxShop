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

   public PreTransactionEvent(ShopItem shopItem, Player player, int value, double amount, Transaction.Type type) {
      this.amount = value;
      this.shopItem = shopItem;
      this.player = player;
      this.price = amount;
      this.originalPrice = this.price;
      this.prices = new HashMap<>();
      this.originalPrices = this.prices;
      this.items = new HashMap<>();
      this.transactionType = type;
   }

   public PreTransactionEvent(Map<ShopItem, Integer> map, Map<EcoType, Double> map2, Player player, int value, Transaction.Type type) {
      this.amount = value;
      this.shopItem = map.size() >= 1 ? (ShopItem)map.keySet().toArray()[0] : null;
      this.player = player;
      this.price = map2 != null && !map2.isEmpty() ? (Double)map2.values().toArray()[0] : 0.0;
      this.originalPrice = this.price;
      this.prices = map2;
      this.originalPrices = Collections.unmodifiableMap(this.prices);
      this.items = map;
      this.transactionType = type;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean flag) {
      this.cancelled = flag;
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

   public void setPrice(double price) {
      this.price = price;
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
