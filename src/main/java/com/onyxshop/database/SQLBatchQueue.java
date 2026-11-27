package com.onyxshop.database;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.queries.Query;
import com.onyxshop.methodes.SendMessage;

public class SQLBatchQueue {
   private final OnyxShop plugin;
   private final Database database;
   private final ConcurrentLinkedQueue<SQLBatchQueue.Batch> queue = new ConcurrentLinkedQueue<>();
   private final AtomicBoolean running = new AtomicBoolean(false);

   public SQLBatchQueue(OnyxShop plugin, Database database) {
      this.plugin = plugin;
      this.database = database;
   }

   public void addToQueue(SQLBatch.Type type, Set<Query> values) {
      this.queue.add(new SQLBatchQueue.Batch(type, values));
      if (this.running.compareAndSet(false, true)) {
         this.plugin.runTaskAsync(this::execute);
      }
   }

   public void drain() {
      SQLBatchQueue.Batch batch;
      while ((batch = this.queue.poll()) != null) {
         try {
            this.database.executeBatch(batch.type, batch.batch);
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to drain a queued SQL batch:");
            exception.printStackTrace();
         }
      }

      this.running.set(false);
   }

   private void execute() {
      while (true) {
         SQLBatchQueue.Batch batch;
         if ((batch = this.queue.poll()) != null) {
            try {
               this.database.executeBatch(batch.type, batch.batch);
            } catch (Exception exception) {
               SendMessage.warnMessage("Failed to execute a queued SQL batch:");
               exception.printStackTrace();
            }
         } else {
            this.running.set(false);
            if (this.queue.isEmpty() || !this.running.compareAndSet(false, true)) {
               return;
            }
         }
      }
   }

   private static class Batch {
      private final SQLBatch.Type type;
      private final Set<Query> batch;

      private Batch(SQLBatch.Type type, Set<Query> values) {
         this.type = type;
         this.batch = values;
      }
   }
}
