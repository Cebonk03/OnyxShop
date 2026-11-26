package com.onyxshop.util.scheduler.tasks;

import java.util.concurrent.Future;
import com.onyxshop.util.scheduler.ScheduledTask;

public class KillableTask implements ScheduledTask {
   private final Future<?> task;

   public KillableTask(Future<?> var1) {
      this.task = var1;
   }

   @Override
   public void cancel() {
      this.task.cancel(true);
   }
}
