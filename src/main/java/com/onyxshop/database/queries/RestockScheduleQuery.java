package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;
import com.onyxshop.providers.ScheduleManager;

public class RestockScheduleQuery implements Query {
   private final ScheduleManager.Item item;
   private final long next;

   public RestockScheduleQuery(ScheduleManager.Item item, long restockTime) {
      this.item = item;
      this.next = restockTime;
   }

   @Override
   public String format(String text) {
      return SQLQueries.SAVE_RESTOCK_SCHEDULE.init(text, this.item.getItemPath(), this.item.mode, this.next);
   }

   @Override
   public String formatPostgres(String text) {
      return SQLQueries.SAVE_RESTOCK_SCHEDULE_PG.init(text, this.item.getItemPath(), this.item.mode, this.next);
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         RestockScheduleQuery restockScheduleQuery = (RestockScheduleQuery)value;
         return this.item.equals(restockScheduleQuery.item);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.item);
   }
}
