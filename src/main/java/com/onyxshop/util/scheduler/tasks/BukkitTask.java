package com.onyxshop.util.scheduler.tasks;

import com.onyxshop.util.scheduler.ScheduledTask;

public class BukkitTask implements ScheduledTask {
   private final org.bukkit.scheduler.BukkitTask task;

   public BukkitTask(org.bukkit.scheduler.BukkitTask bukkitTask) {
      this.task = bukkitTask;
   }

   @Override
   public void cancel() {
      this.task.cancel();
   }
}
