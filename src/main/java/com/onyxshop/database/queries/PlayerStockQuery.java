package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class PlayerStockQuery implements Query {
   private final String itemPath;
   private final String playerUUID;
   private final int amount;

   public PlayerStockQuery(String itemId, String playerId, int amount) {
      this.itemPath = itemId;
      this.playerUUID = playerId;
      this.amount = amount;
   }

   @Override
   public String format(String text) {
      return SQLQueries.SAVE_PLAYER_STOCK.init(text, this.itemPath, this.playerUUID, this.amount);
   }

   @Override
   public String formatPostgres(String text) {
      return SQLQueries.SAVE_PLAYER_STOCK_PG.init(text, this.itemPath, this.playerUUID, this.amount);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         PlayerStockQuery playerStockQuery = (PlayerStockQuery)value;
         return this.itemPath.equals(playerStockQuery.itemPath) && this.playerUUID.equals(playerStockQuery.playerUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath, this.playerUUID);
   }
}
