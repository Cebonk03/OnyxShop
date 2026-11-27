package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class RemoveStockQuery implements Query {
   private final String section;

   public RemoveStockQuery(String itemId) {
      this.section = itemId == null ? "%" : "%" + itemId + "%";
   }

   @Override
   public String format(String text) {
      return SQLQueries.REMOVE_STOCK.init(text, this.section);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         RemoveStockQuery removeStockQuery = (RemoveStockQuery)value;
         return this.section.equals(removeStockQuery.section);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.section);
   }
}
