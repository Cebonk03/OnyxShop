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

   public SQLBatch(OnyxShop var1, Database var2, SQLBatch.Type var3) {
      this.database = var2;
      this.type = var3;
      this.queue = new SQLBatchQueue(var1, var2);
   }

   public void addQuery(Query var1) {
      this.batch.remove(var1);
      this.batch.add(var1);
      if (this.batch.size() == 20) {
         LinkedHashSet var2 = new LinkedHashSet<>(this.batch);
         this.queue.addToQueue(this.type, var2);
         this.batch.clear();
      }
   }

   public void saveLast() {
      this.database.executeBatch(this.type, this.batch);
      this.batch.clear();
   }

   public void drain() {
      this.saveLast();
      this.queue.drain();
   }

   public static enum Type {
      STOCK,
      SELL_LIMIT;

      private Type() {
      }
   }
}
