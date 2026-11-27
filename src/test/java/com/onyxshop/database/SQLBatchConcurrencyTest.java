package com.onyxshop.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.onyxshop.database.queries.ItemStockQuery;
import com.onyxshop.database.queries.Query;
import org.junit.jupiter.api.Test;

/**
 * Guards the two write-ordering guarantees of the SQLite batch path:
 * a concurrent addQuery must never corrupt or truncate the set handed to executeBatch,
 * and drain() must flush the older queued batches before the newer in-memory one.
 */
class SQLBatchConcurrencyTest {
   private static final Pattern STOCK_ROW = Pattern.compile("VALUES\\('([^']+)', (-?\\d+)\\)");

   @Test
   void saveLastKeepsWritesAddedWhileItRuns() throws Exception {
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      List<Query> executed = Collections.synchronizedList(new ArrayList<>());
      AtomicBoolean blockedOnce = new AtomicBoolean(false);

      Database database = recordingDatabase((batch) -> {
         for (Query query : batch) {
            executed.add(query);
            if (blockedOnce.compareAndSet(false, true)) {
               started.countDown();
               release.await(5, TimeUnit.SECONDS);
            }
         }
      });

      SQLBatch batch = new SQLBatch(null, database, SQLBatch.Type.STOCK);
      batch.addQuery(new ItemStockQuery("blocks.stone", 1));

      Thread writer = new Thread(batch::saveLast, "save-last");
      writer.start();
      assertTrue(started.await(5, TimeUnit.SECONDS), "executeBatch was never entered");
      batch.addQuery(new ItemStockQuery("blocks.dirt", 2));
      release.countDown();
      writer.join(5000);
      assertFalse(writer.isAlive(), "saveLast did not return");

      batch.saveLast();

      List<String> keys = new ArrayList<>();
      for (Query query : executed) {
         Matcher matcher = STOCK_ROW.matcher(query.format("itemstock"));
         assertTrue(matcher.find(), "unexpected query " + query);
         keys.add(matcher.group(1));
      }

      assertEquals(2, keys.size(), "a write was lost: " + keys);
      assertTrue(keys.contains("blocks.stone"), keys.toString());
      assertTrue(keys.contains("blocks.dirt"), keys.toString());
   }

   @Test
   void drainWritesQueuedBatchesBeforeTheNewestBatch() throws Exception {
      Map<String, Integer> lastValue = new LinkedHashMap<>();
      List<String> writeOrder = new ArrayList<>();
      Database database = recordingDatabase((batch) -> {
         for (Query query : batch) {
            Matcher matcher = STOCK_ROW.matcher(query.format("itemstock"));
            if (matcher.find()) {
               lastValue.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
               writeOrder.add(matcher.group(1));
            }
         }
      });

      SQLBatch batch = new SQLBatch(null, database, SQLBatch.Type.STOCK);
      for (int index = 0; index < 20; index++) {
         batch.addQuery(new ItemStockQuery("blocks.item" + index, index));
      }

      // The flush above schedules an async task through the (absent) plugin; the batch is queued regardless.
      batch.addQuery(new ItemStockQuery("blocks.item5", 999));
      batch.drain();

      assertEquals(Integer.valueOf(999), lastValue.get("blocks.item5"), "a stale queued value overwrote the newest one");
      assertEquals("blocks.item5", writeOrder.get(writeOrder.size() - 1), "newest batch was not written last: " + writeOrder);
   }

   private static Database recordingDatabase(BatchConsumer consumer) {
      return (Database)Proxy.newProxyInstance(
         Database.class.getClassLoader(),
         new Class<?>[]{Database.class},
         (proxy, method, args) -> {
            if ("executeBatch".equals(method.getName())) {
               consumer.accept((Set<Query>)args[1]);
               return null;
            }

            Class<?> type = method.getReturnType();
            if (type == boolean.class) {
               return false;
            } else if (type == int.class) {
               return 0;
            } else if (type == long.class) {
               return 0L;
            } else {
               return null;
            }
         }
      );
   }

   private interface BatchConsumer {
      void accept(Set<Query> batch) throws Exception;
   }
}
