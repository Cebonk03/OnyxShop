package com.onyxshop.util.scheduler.tasks;

import com.onyxshop.util.scheduler.ScheduledTask;

public class BukkitTask implements ScheduledTask {
   private final org.bukkit.scheduler.BukkitTask task;

   public BukkitTask(org.bukkit.scheduler.BukkitTask var1) {
      this.task = var1;
   }

   @Override
   public void cancel() {
      this.task.cancel();
   }
}
