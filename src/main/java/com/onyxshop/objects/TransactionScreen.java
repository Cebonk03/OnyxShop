package com.onyxshop.objects;

import java.util.List;

public class TransactionScreen {
   private final List<TransactionItem> items;
   private final int size;

   public TransactionScreen(List<TransactionItem> values, int value) {
      this.items = values;
      this.size = value;
   }

   public List<TransactionItem> getItems() {
      return this.items;
   }

   public int getSize() {
      return this.size;
   }
}
