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

   public PostTransactionEvent(ShopItem var1, Player var2, int var3, double var4, Transaction.Type var6, Transaction.Result var7) {
      this.amount = var3;
      this.shopItem = var1;
      this.player = var2;
      this.price = var4;
      this.type = var6;
      this.result = var7;
      this.items = new HashMap<>();
      this.prices = new HashMap<>();
   }

   public PostTransactionEvent(Map<ShopItem, Integer> var1, Map<EcoType, Double> var2, Player var3, int var4, Transaction.Type var5, Transaction.Result var6) {
      this.amount = var4;
      this.shopItem = var1.size() >= 1 ? (ShopItem)var1.keySet().toArray()[0] : null;
      this.player = var3;
      this.price = var2 != null && !var2.isEmpty() ? (Double)var2.values().toArray()[0] : 0.0;
      this.type = var5;
      this.result = var6;
      this.items = var1;
      this.prices = var2;
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
