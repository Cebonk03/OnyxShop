package com.onyxshop.database.queries;

public interface Query {
   String format(String var1);

   default String formatPostgres(String table) {
      return this.format(table);
   }

   @Override
   boolean equals(Object var1);

   @Override
   int hashCode();
}
