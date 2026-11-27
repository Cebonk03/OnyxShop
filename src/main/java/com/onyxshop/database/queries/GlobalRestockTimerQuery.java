package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class GlobalRestockTimerQuery implements Query {
   private final String itemPath;
   private final long timer;

   public GlobalRestockTimerQuery(String text, long restockTime) {
      this.itemPath = text;
      this.timer = restockTime;
   }

   @Override
   public String format(String text) {
      return SQLQueries.SAVE_ITEM_RESTOCK.init(text, this.itemPath, this.timer);
   }

   @Override
   public String formatPostgres(String text) {
      return SQLQueries.SAVE_ITEM_RESTOCK_PG.init(text, this.itemPath, this.timer);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         GlobalRestockTimerQuery globalRestockTimerQuery = (GlobalRestockTimerQuery)value;
         return this.itemPath.equals(globalRestockTimerQuery.itemPath);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.itemPath);
   }
}
