package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class ItemStockQuery implements Query {
   private final String itemPath;
   private final int amount;

   public ItemStockQuery(String itemId, int stock) {
      this.itemPath = itemId;
      this.amount = stock;
   }

   @Override
   public String format(String text) {
      return SQLQueries.SAVE_ITEM_STOCK.init(text, this.itemPath, this.amount);
   }

   @Override
   public String formatPostgres(String text) {
      return SQLQueries.SAVE_ITEM_STOCK_PG.init(text, this.itemPath, this.amount);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         ItemStockQuery itemStockQuery = (ItemStockQuery)value;
         return this.itemPath.equals(itemStockQuery.itemPath);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath);
   }
}
