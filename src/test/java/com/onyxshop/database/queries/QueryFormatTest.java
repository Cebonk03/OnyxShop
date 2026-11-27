package com.onyxshop.database.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Pins the placeholder order of the player-stock statements: the item path always binds before the player's
 * UUID. The descriptive-name pass renamed these constructor parameters, so this is the guard that the two
 * same-typed strings were not swapped.
 */
class QueryFormatTest {
   @Test
   void playerStockQueryBindsItemThenUuid() {
      assertEquals(
         "REPLACE INTO playerstock(item,uuid,amount) VALUES('a.b', 'uuid-1', 3);",
         new PlayerStockQuery("a.b", "uuid-1", 3).format("playerstock")
      );
   }
}
