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

   public PoolJob(int value) {
      this.id = value;
   }

   public int addAndGetSize(ScheduledRestockJob scheduledRestockJob) {
      this.jobs.add(scheduledRestockJob);
      return this.jobs.size();
   }

   public void execute() {
      this.running = true;
      int value = 0;

      for (ScheduledRestockJob scheduledRestockJob : this.jobs) {
         try {
            scheduledRestockJob.execute();
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to execute task " + value + " from pool job:");
            exception.printStackTrace();
         }

         value++;
      }

      this.completed = true;
   }
}
