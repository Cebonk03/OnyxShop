package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class RemovePlayerStockQuery implements Query {
   private final String playerUUID;
   private final String section;

   public RemovePlayerStockQuery(String var1, String var2) {
      this.playerUUID = var1;
      this.section = var2 == null ? "%" : "%" + var2 + "%";
   }

   @Override
   public String format(String var1) {
      return SQLQueries.REMOVE_PLAYER_STOCK.init(var1, this.section, this.playerUUID);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         RemovePlayerStockQuery var2 = (RemovePlayerStockQuery)var1;
         return this.section.equals(var2.section) && this.playerUUID.equals(var2.playerUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.section, this.playerUUID);
   }
}
