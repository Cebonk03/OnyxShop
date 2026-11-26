package com.onyxshop.util;

import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class SmartStack {
   private final ItemStack item;

   public SmartStack(ItemStack var1) {
      this.item = var1;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public static class Shulker extends SmartStack {
      private final Map<Integer, ItemStack> items;

      public Shulker(ItemStack var1, Map<Integer, ItemStack> var2) {
         super(var1);
         this.items = var2;
      }

      public Map<Integer, ItemStack> getItems() {
         return this.items;
      }
   }
}
