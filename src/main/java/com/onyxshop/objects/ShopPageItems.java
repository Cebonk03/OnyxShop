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

   public String getItem(int value) {
      return this.itemOrder.get(value);
   }

   public int getSize() {
      return this.size;
   }

   public void addItem(String itemId) {
      for (int size = 0; size < this.itemOrder.size(); size++) {
         String text = this.itemOrder.get(size);
         if (text == null) {
            this.itemOrder.set(size, itemId);
            return;
         }
      }
   }

   public void setItem(String itemId, ItemStack itemStack) {
      for (int size = 0; size < this.itemOrder.size(); size++) {
         String text = this.itemOrder.get(size);
         if (text != null && text.equals(itemId)) {
            this.items.put(size, itemStack);
         }
      }
   }

   public List<String> getItemOrder() {
      return this.itemOrder;
   }

   public Map<Integer, ItemStack> getItems() {
      return new LinkedHashMap<>(this.items);
   }

   public ShopPageItems(Map<Integer, ItemStack> map, List<String> shopIds, String itemId, int page) {
      this.items = map;
      this.itemOrder = shopIds;
      this.section = itemId;
      this.size = page;
   }

   public ShopPageItems(String shopId) {
      this.items = new HashMap<>();
      this.itemOrder = new ArrayList<>();
      this.section = shopId;
      this.size = 54;
   }
}
