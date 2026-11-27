package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class UpdatePlayerStockQuery implements Query {
   private final String itemPath;
   private final int amount;

   public UpdatePlayerStockQuery(String itemId, int amount) {
      this.itemPath = itemId;
      this.amount = amount;
   }

   @Override
   public String format(String text) {
      return SQLQueries.UPDATE_PLAYER_STOCK.init(text, this.itemPath, this.amount);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         UpdatePlayerStockQuery updatePlayerStockQuery = (UpdatePlayerStockQuery)value;
         return this.itemPath.equals(updatePlayerStockQuery.itemPath);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath);
   }
}
