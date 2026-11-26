package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class ShopPageItems {
   private final int size;
   private final String section;
   private final Map<Integer, ItemStack> items;
   private final List<String> itemOrder;

   public String getItem(int var1) {
      return this.itemOrder.get(var1);
   }

   public int getSize() {
      return this.size;
   }

   public void addItem(String var1) {
      for (int var2 = 0; var2 < this.itemOrder.size(); var2++) {
         String var3 = this.itemOrder.get(var2);
         if (var3 == null) {
            this.itemOrder.set(var2, var1);
            return;
         }
      }
   }

   public void setItem(String var1, ItemStack var2) {
      for (int var3 = 0; var3 < this.itemOrder.size(); var3++) {
         String var4 = this.itemOrder.get(var3);
         if (var4 != null && var4.equals(var1)) {
            this.items.put(var3, var2);
         }
      }
   }

   public List<String> getItemOrder() {
      return this.itemOrder;
   }

   public Map<Integer, ItemStack> getItems() {
      return new LinkedHashMap<>(this.items);
   }

   public ShopPageItems(Map<Integer, ItemStack> var1, List<String> var2, String var3, int var4) {
      this.items = var1;
      this.itemOrder = var2;
      this.section = var3;
      this.size = var4;
   }

   public ShopPageItems(String var1) {
      this.items = new HashMap<>();
      this.itemOrder = new ArrayList<>();
      this.section = var1;
      this.size = 54;
   }
}
