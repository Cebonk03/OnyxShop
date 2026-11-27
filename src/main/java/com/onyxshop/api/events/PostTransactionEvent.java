package com.onyxshop.api.events;

import java.util.HashMap;
import java.util.Map;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PostTransactionEvent extends CustomEvent {
   private final int amount;
   private final double price;
   private Map<EcoType, Double> prices;
   private final ShopItem shopItem;
   private final Map<ShopItem, Integer> items;
   private final Player player;
   private final Transaction.Type type;
   private final Transaction.Result result;

   public PostTransactionEvent(ShopItem shopItem, Player player, int value, double amount, Transaction.Type type, Transaction.Result result) {
      this.amount = value;
      this.shopItem = shopItem;
      this.player = player;
      this.price = amount;
      this.type = type;
      this.result = result;
      this.items = new HashMap<>();
      this.prices = new HashMap<>();
   }

   public PostTransactionEvent(Map<ShopItem, Integer> map, Map<EcoType, Double> map2, Player player, int value, Transaction.Type type, Transaction.Result result) {
      this.amount = value;
      this.shopItem = map.size() >= 1 ? (ShopItem)map.keySet().toArray()[0] : null;
      this.player = player;
      this.price = map2 != null && !map2.isEmpty() ? (Double)map2.values().toArray()[0] : 0.0;
      this.type = type;
      this.result = result;
      this.items = map;
      this.prices = map2;
   }

   public int getAmount() {
      return this.amount;
   }

   public double getPrice() {
      return this.price;
   }

   @Nullable
   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   @Deprecated
   public ItemStack getItemStack() {
      return this.shopItem.getItemToGive();
   }

   @Nullable
   public ShopItem getShopItem() {
      return this.shopItem;
   }

   @Nullable
   public Map<ShopItem, Integer> getItems() {
      return this.items;
   }

   public Player getPlayer() {
      return this.player;
   }

   public Transaction.Type getTransactionType() {
      return this.type;
   }

   public Transaction.Result getTransactionResult() {
      return this.result;
   }
}
