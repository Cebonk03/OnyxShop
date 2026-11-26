package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class UpdatePlayerStockQuery implements Query {
   private final String itemPath;
   private final int amount;

   public UpdatePlayerStockQuery(String var1, int var2) {
      this.itemPath = var1;
      this.amount = var2;
   }

   @Override
   public String format(String var1) {
      return SQLQueries.UPDATE_PLAYER_STOCK.init(var1, this.itemPath, this.amount);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         UpdatePlayerStockQuery var2 = (UpdatePlayerStockQuery)var1;
         return this.itemPath.equals(var2.itemPath);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath);
   }
}
