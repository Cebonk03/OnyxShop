package com.onyxshop.util;

import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class SmartStack {
   private final ItemStack item;

   public SmartStack(ItemStack itemStack) {
      this.item = itemStack;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public static class Shulker extends SmartStack {
      private final Map<Integer, ItemStack> items;

      public Shulker(ItemStack itemStack, Map<Integer, ItemStack> map) {
         super(itemStack);
         this.items = map;
      }

      public Map<Integer, ItemStack> getItems() {
         return this.items;
      }
   }
}
