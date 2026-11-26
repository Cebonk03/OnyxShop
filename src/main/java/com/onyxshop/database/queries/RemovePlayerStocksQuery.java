package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class RemovePlayerStocksQuery implements Query {
   private final String section;

   public RemovePlayerStocksQuery(String var1) {
      this.section = var1 == null ? "%" : "%" + var1 + "%";
   }

   @Override
   public String format(String var1) {
      return SQLQueries.REMOVE_STOCK.init(var1, this.section);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         RemovePlayerStocksQuery var2 = (RemovePlayerStocksQuery)var1;
         return this.section.equals(var2.section);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.section);
   }
}
