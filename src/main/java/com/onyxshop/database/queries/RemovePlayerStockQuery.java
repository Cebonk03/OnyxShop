package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class RemovePlayerStockQuery implements Query {
   private final String playerUUID;
   private final String section;

   public RemovePlayerStockQuery(String playerName, String itemId) {
      this.playerUUID = playerName;
      this.section = itemId == null ? "%" : "%" + itemId + "%";
   }

   @Override
   public String format(String text) {
      return SQLQueries.REMOVE_PLAYER_STOCK.init(text, this.section, this.playerUUID);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         RemovePlayerStockQuery removePlayerStockQuery = (RemovePlayerStockQuery)value;
         return this.section.equals(removePlayerStockQuery.section) && this.playerUUID.equals(removePlayerStockQuery.playerUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.section, this.playerUUID);
   }
}
