package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;
import com.onyxshop.providers.ScheduleManager;

public class RestockScheduleQuery implements Query {
   private final ScheduleManager.Item item;
   private final long next;

   public RestockScheduleQuery(ScheduleManager.Item var1, long var2) {
      this.item = var1;
      this.next = var2;
   }

   @Override
   public String format(String var1) {
      return SQLQueries.SAVE_RESTOCK_SCHEDULE.init(var1, this.item.getItemPath(), this.item.mode, this.next);
   }

   @Override
   public String formatPostgres(String var1) {
      return SQLQueries.SAVE_RESTOCK_SCHEDULE_PG.init(var1, this.item.getItemPath(), this.item.mode, this.next);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         RestockScheduleQuery var2 = (RestockScheduleQuery)var1;
         return this.item.equals(var2.item);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.item);
   }
}
