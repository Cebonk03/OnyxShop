package com.onyxshop.database.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PlayerStockQueryTest {
   @Test
   void sameItemAndPlayerAreEqualEvenWhenAmountDiffers() {
      // SQLBatch keeps a LinkedHashSet and removes-then-adds; equality must therefore ignore the
      // amount so the latest amount replaces the older queued one instead of accumulating rows.
      PlayerStockQuery older = new PlayerStockQuery("Blocks.Grass", "uuid", 10);
      PlayerStockQuery newer = new PlayerStockQuery("Blocks.Grass", "uuid", 3);

      assertEquals(older, newer);
      assertEquals(older.hashCode(), newer.hashCode());
   }

   @Test
   void differentPlayersAreNotEqual() {
      assertNotEquals(new PlayerStockQuery("Blocks.Grass", "a", 1), new PlayerStockQuery("Blocks.Grass", "b", 1));
   }

   @Test
   void removeQueryBuildsWildcardPattern() {
      assertEquals("DELETE FROM playerstock WHERE (item LIKE '%Food%' OR item IS NULL) AND uuid = 'uuid';",
         new RemovePlayerStockQuery("uuid", "Food").format("playerstock"));
      assertEquals("DELETE FROM playerstock WHERE (item LIKE '%' OR item IS NULL) AND uuid = 'uuid';",
         new RemovePlayerStockQuery("uuid", null).format("playerstock"));
   }
}
