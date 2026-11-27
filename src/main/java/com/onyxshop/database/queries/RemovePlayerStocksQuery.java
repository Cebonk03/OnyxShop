package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class RemovePlayerStocksQuery implements Query {
   private final String section;

   public RemovePlayerStocksQuery(String section) {
      this.section = section == null ? "%" : "%" + section + "%";
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
         RemovePlayerStocksQuery removePlayerStocksQuery = (RemovePlayerStocksQuery)value;
         return this.section.equals(removePlayerStocksQuery.section);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.section);
   }
}
