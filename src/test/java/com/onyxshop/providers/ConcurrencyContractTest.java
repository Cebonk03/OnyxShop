package com.onyxshop.providers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/**
 * Guards the cross-thread contract the Folia scheduler depends on: display/pricing caches are read and
 * written from async scheduler threads while region threads transact, and player-bound work must have an
 * entity-scoped scheduling entry point.
 */
class ConcurrencyContractTest {
   @Test
   void stockCachesAreConcurrent() throws Exception {
      assertConcurrentMap(LimitedStockProvider.class, "itemStocks");
      assertConcurrentMap(LimitedStockProvider.class, "CASHED_GLOBAL_STOCK");
      assertConcurrentMap(LimitedStockProvider.class, "CASHED_PLAYER_STOCK");
      assertConcurrentMap(LimitedStockProvider.class, "GLOBAL_RESTOCK_TIMER");
      assertConcurrentMap(LimitedStockProvider.class, "PLAYER_RESTOCK_TIMER");
   }

   @Test
   void pricingCachesAreConcurrentAndVisible() throws Exception {
      assertConcurrentMap(DynamicPricingProvider.class, "CASHED_STOCK");
      assertConcurrentMap(DynamicPricingProvider.class, "EXPIRED_STOCK");

      Field expired = DynamicPricingProvider.class.getDeclaredField("EXPIRED_STOCK");
      assertTrue(Modifier.isVolatile(expired.getModifiers()), "EXPIRED_STOCK must be volatile");
   }

   @Test
   void schedulerExposesEntityScopedDelayedHop() throws Exception {
      Method hop = Class.forName("com.onyxshop.OnyxShop").getMethod("runTaskLaterFor", Runnable.class, Player.class, long.class);
      assertNotNull(hop);
   }

   private static void assertConcurrentMap(Class<?> owner, String name) throws Exception {
      Field field = owner.getDeclaredField(name);
      assertTrue(Map.class.isAssignableFrom(field.getType()), name + " must be a Map");
      assertTrue(ConcurrentHashMap.class.isAssignableFrom(field.getType()), name + " must be a ConcurrentHashMap");
   }
}
