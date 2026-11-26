package com.onyxshop.database.queries;

import com.onyxshop.database.SQLQueries;

public class ClearPlayerStockQuery implements Query {
   public ClearPlayerStockQuery() {
   }

   @Override
   public String format(String var1) {
      return SQLQueries.CLEAR_TABLE.init(var1);
   }

   @Override
   public boolean equals(Object var1) {
      return this == var1 ? true : var1 != null && this.getClass() == var1.getClass();
   }
}
