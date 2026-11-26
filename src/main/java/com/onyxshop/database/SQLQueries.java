package com.onyxshop.database;

public enum SQLQueries {
   SAVE_ITEM_STOCK("REPLACE INTO %1$s(item, amount) VALUES('%2$s', %3$d);"),
   SAVE_PLAYER_STOCK("REPLACE INTO %1$s(item,uuid,amount) VALUES('%2$s', '%3$s', %4$d);"),
   UPDATE_PLAYER_STOCK("UPDATE %1$s SET amount = %3$d WHERE item = '%2$s';"),
   REMOVE_STOCK("DELETE FROM %1$s WHERE (item LIKE '%2$s' OR item IS NULL);"),
   CLEAR_TABLE("DELETE FROM %1$s;"),
   REMOVE_PLAYER_STOCK("DELETE FROM %1$s WHERE (item LIKE '%2$s' OR item IS NULL) AND uuid = '%3$s';"),
   SAVE_ITEM_RESTOCK("REPLACE INTO %1$s(item, amount, restock) VALUES('%2$s', 0, '%3$s');"),
   SAVE_PLAYER_RESTOCK("REPLACE INTO %1$s(item, uuid, amount, restock) VALUES('%2$s', '%3$s', 0, '%4$s');"),
   SAVE_RESTOCK_SCHEDULE("REPLACE INTO %1$s VALUES('%2$s',%3$d,'%4$s');"),
   SAVE_ITEM_STOCK_PG("INSERT INTO %1$s(item, amount) VALUES('%2$s', %3$d) ON CONFLICT(item) DO UPDATE SET amount = EXCLUDED.amount, restock = NULL;"),
   SAVE_PLAYER_STOCK_PG(
      "INSERT INTO %1$s(item,uuid,amount) VALUES('%2$s', '%3$s', %4$d) ON CONFLICT(item,uuid) DO UPDATE SET amount = EXCLUDED.amount, restock = NULL;"
   ),
   SAVE_ITEM_RESTOCK_PG(
      "INSERT INTO %1$s(item,amount,restock) VALUES('%2$s', 0, '%3$s') ON CONFLICT(item) DO UPDATE SET amount = EXCLUDED.amount, restock = EXCLUDED.restock;"
   ),
   SAVE_PLAYER_RESTOCK_PG(
      "INSERT INTO %1$s(item,uuid,amount,restock) VALUES('%2$s', '%3$s', 0, '%4$s') ON CONFLICT(item,uuid) DO UPDATE SET amount = EXCLUDED.amount, restock = EXCLUDED.restock;"
   ),
   SAVE_RESTOCK_SCHEDULE_PG("INSERT INTO %1$s(item,mode,restock) VALUES('%2$s',%3$d,'%4$s') ON CONFLICT(item,mode) DO UPDATE SET restock = EXCLUDED.restock;");

   private final String query;

   private SQLQueries(String nullxx) {
      this.query = nullxx;
   }

   public String init(Object... var1) {
      return String.format(this.query, var1);
   }
}
