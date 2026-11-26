package com.onyxshop.util.scheduler.tasks;

import com.onyxshop.util.scheduler.ScheduledTask;

public class FoliaTask implements ScheduledTask {
   private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

   public FoliaTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask var1) {
      this.task = var1;
   }

   @Override
   public void cancel() {
      this.task.cancel();
   }
}
