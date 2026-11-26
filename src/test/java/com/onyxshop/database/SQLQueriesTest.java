package com.onyxshop.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SQLQueriesTest {
   @Test
   void saveItemStockRendersExactSql() {
      assertEquals(
         "REPLACE INTO itemstock(item, amount) VALUES('Blocks.Grass', 5);",
         SQLQueries.SAVE_ITEM_STOCK.init("itemstock", "Blocks.Grass", 5)
      );
   }

   @Test
   void savePlayerStockRendersExactSql() {
      assertEquals(
         "REPLACE INTO playerstock(item,uuid,amount) VALUES('Blocks.Grass', '00000000-0000-0000-0000-000000000001', -1);",
         SQLQueries.SAVE_PLAYER_STOCK.init("playerstock", "Blocks.Grass", "00000000-0000-0000-0000-000000000001", -1)
      );
   }

   @Test
   void restockTemplatesUseExplicitColumnLists() {
      assertEquals(
         "REPLACE INTO itemstock(item, amount, restock) VALUES('Blocks.Grass', 0, '1700000000000');",
         SQLQueries.SAVE_ITEM_RESTOCK.init("itemstock", "Blocks.Grass", 1700000000000L)
      );
      assertEquals(
         "REPLACE INTO playerstock(item, uuid, amount, restock) VALUES('Blocks.Grass', 'uuid', 0, '1700000000000');",
         SQLQueries.SAVE_PLAYER_RESTOCK.init("playerstock", "Blocks.Grass", "uuid", 1700000000000L)
      );
   }

   @Test
   void removePlayerStockRendersLikePattern() {
      assertEquals(
         "DELETE FROM playerstock WHERE (item LIKE '%Food%' OR item IS NULL) AND uuid = 'uuid';",
         SQLQueries.REMOVE_PLAYER_STOCK.init("playerstock", "%Food%", "uuid")
      );
   }

   @Test
   void itemPathsAreStillInterpolatedVerbatim() {
      // Documents the remaining (config-controlled) interpolation: an apostrophe is NOT escaped.
      assertEquals(
         "REPLACE INTO itemstock(item, amount) VALUES('Chef's Hat', 1);",
         SQLQueries.SAVE_ITEM_STOCK.init("itemstock", "Chef's Hat", 1)
      );
   }
}
