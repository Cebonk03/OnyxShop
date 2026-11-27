package com.onyxshop.database.queries;

import com.onyxshop.database.SQLQueries;

public class ClearPlayerStockQuery implements Query {
   public ClearPlayerStockQuery() {
   }

   @Override
   public String format(String text) {
      return SQLQueries.CLEAR_TABLE.init(text);
   }

   @Override
   public boolean equals(Object value) {
      return this == value ? true : value != null && this.getClass() == value.getClass();
   }
}
