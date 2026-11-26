package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Collection;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ScheduledRestockJob;

public class PoolJob {
   private final int id;
   private final Collection<ScheduledRestockJob> jobs = new ArrayList<>();
   public boolean running = false;
   public boolean completed = false;

   public PoolJob(int var1) {
      this.id = var1;
   }

   public int addAndGetSize(ScheduledRestockJob var1) {
      this.jobs.add(var1);
      return this.jobs.size();
   }

   public void execute() {
      this.running = true;
      int var1 = 0;

      for (ScheduledRestockJob var3 : this.jobs) {
         try {
            var3.execute();
         } catch (Exception var5) {
            SendMessage.warnMessage("Failed to execute task " + var1 + " from pool job:");
            var5.printStackTrace();
         }

         var1++;
      }

      this.completed = true;
   }
}
