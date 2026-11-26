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

   public SQLBatchQueue(OnyxShop var1, Database var2) {
      this.plugin = var1;
      this.database = var2;
   }

   public void addToQueue(SQLBatch.Type var1, Set<Query> var2) {
      this.queue.add(new SQLBatchQueue.Batch(var1, var2));
      if (this.running.compareAndSet(false, true)) {
         this.plugin.runTaskAsync(this::execute);
      }
   }

   public void drain() {
      SQLBatchQueue.Batch var1;
      while ((var1 = this.queue.poll()) != null) {
         try {
            this.database.executeBatch(var1.type, var1.batch);
         } catch (Exception var3) {
            SendMessage.warnMessage("Failed to drain a queued SQL batch:");
            var3.printStackTrace();
         }
      }

      this.running.set(false);
   }

   private void execute() {
      while (true) {
         SQLBatchQueue.Batch var1;
         if ((var1 = this.queue.poll()) != null) {
            try {
               this.database.executeBatch(var1.type, var1.batch);
            } catch (Exception var3) {
               SendMessage.warnMessage("Failed to execute a queued SQL batch:");
               var3.printStackTrace();
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

      private Batch(SQLBatch.Type var1, Set<Query> var2) {
         this.type = var1;
         this.batch = var2;
      }
   }
}
