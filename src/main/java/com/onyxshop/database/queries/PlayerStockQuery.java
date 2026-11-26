package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class PlayerStockQuery implements Query {
   private final String itemPath;
   private final String playerUUID;
   private final int amount;

   public PlayerStockQuery(String var1, String var2, int var3) {
      this.itemPath = var1;
      this.playerUUID = var2;
      this.amount = var3;
   }

   @Override
   public String format(String var1) {
      return SQLQueries.SAVE_PLAYER_STOCK.init(var1, this.itemPath, this.playerUUID, this.amount);
   }

   @Override
   public String formatPostgres(String var1) {
      return SQLQueries.SAVE_PLAYER_STOCK_PG.init(var1, this.itemPath, this.playerUUID, this.amount);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         PlayerStockQuery var2 = (PlayerStockQuery)var1;
         return this.itemPath.equals(var2.itemPath) && this.playerUUID.equals(var2.playerUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath, this.playerUUID);
   }
}
