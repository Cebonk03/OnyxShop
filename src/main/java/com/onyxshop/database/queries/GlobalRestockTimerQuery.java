package com.onyxshop.database.queries;

import java.util.Objects;
import com.onyxshop.database.SQLQueries;

public class GlobalRestockTimerQuery implements Query {
   private final String itemPath;
   private final long timer;

   public GlobalRestockTimerQuery(String var1, long var2) {
      this.itemPath = var1;
      this.timer = var2;
   }

   @Override
   public String format(String var1) {
      return SQLQueries.SAVE_ITEM_RESTOCK.init(var1, this.itemPath, this.timer);
   }

   @Override
   public String formatPostgres(String var1) {
      return SQLQueries.SAVE_ITEM_RESTOCK_PG.init(var1, this.itemPath, this.timer);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         GlobalRestockTimerQuery var2 = (GlobalRestockTimerQuery)var1;
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
