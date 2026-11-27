package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class PlayerRestockTimerQuery implements Query {
   private final String itemPath;
   private final String playerUUID;
   private final long timer;

   public PlayerRestockTimerQuery(String playerName, String text, long restockTime) {
      this.itemPath = playerName;
      this.playerUUID = text;
      this.timer = restockTime;
   }

   @Override
   public String format(String text) {
      return SQLQueries.SAVE_PLAYER_RESTOCK.init(text, this.itemPath, this.playerUUID, this.timer);
   }

   @Override
   public String formatPostgres(String text) {
      return SQLQueries.SAVE_PLAYER_RESTOCK_PG.init(text, this.itemPath, this.playerUUID, this.timer);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         PlayerRestockTimerQuery playerRestockTimerQuery = (PlayerRestockTimerQuery)value;
         return this.itemPath.equals(playerRestockTimerQuery.itemPath) && this.playerUUID.equals(playerRestockTimerQuery.playerUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath, this.playerUUID);
   }
}
