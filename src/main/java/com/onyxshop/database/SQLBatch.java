package com.onyxshop.database;

import java.util.LinkedHashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.queries.Query;

public class SQLBatch {
   private final Set<Query> batch = new LinkedHashSet<>();
   private final Database database;
   private final SQLBatch.Type type;
   private final SQLBatchQueue queue;

   public SQLBatch(OnyxShop plugin, Database database, SQLBatch.Type type) {
      this.database = database;
      this.type = type;
      this.queue = new SQLBatchQueue(plugin, database);
   }

   public void addQuery(Query query) {
      synchronized (this.batch) {
         this.batch.remove(query);
         this.batch.add(query);
         if (this.batch.size() >= 20) {
            LinkedHashSet values = new LinkedHashSet<>(this.batch);
            this.queue.addToQueue(this.type, values);
            this.batch.clear();
         }
      }
   }

   public void saveLast() {
      LinkedHashSet<Query> snapshot;
      synchronized (this.batch) {
         if (this.batch.isEmpty()) {
            return;
         }

         snapshot = new LinkedHashSet<>(this.batch);
         this.batch.clear();
      }

      this.database.executeBatch(this.type, snapshot);
   }

   public void drain() {
      this.queue.drain();
      this.saveLast();
   }

   public static enum Type {
      STOCK,
      SELL_LIMIT;

      private Type() {
      }
   }
}
