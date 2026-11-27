package com.onyxshop.database.queries;

public interface Query {
   String format(String text);

   default String formatPostgres(String table) {
      return this.format(table);
   }

   @Override
   boolean equals(Object value);

   @Override
   int hashCode();
}
