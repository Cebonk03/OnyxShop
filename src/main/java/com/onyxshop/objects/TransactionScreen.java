package com.onyxshop.objects;

import java.util.List;

public class TransactionScreen {
   private final List<TransactionItem> items;
   private final int size;

   public TransactionScreen(List<TransactionItem> var1, int var2) {
      this.items = var1;
      this.size = var2;
   }

   public List<TransactionItem> getItems() {
      return this.items;
   }

   public int getSize() {
      return this.size;
   }
}
