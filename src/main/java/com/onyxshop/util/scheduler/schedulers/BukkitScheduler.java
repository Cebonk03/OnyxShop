package com.onyxshop.util.scheduler.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.onyxshop.OnyxShop;
import com.onyxshop.util.scheduler.ScheduledTask;
import com.onyxshop.util.scheduler.ServerScheduler;
import com.onyxshop.util.scheduler.tasks.BukkitTask;
import com.onyxshop.util.scheduler.tasks.KillableTask;
import org.bukkit.entity.Player;

public class BukkitScheduler implements ServerScheduler {
   private final org.bukkit.scheduler.BukkitScheduler scheduler;
   private final ScheduledExecutorService thread = Executors.newScheduledThreadPool(
      2, new ThreadFactoryBuilder().setNameFormat("ONYX_UTIL_THREAD #%d").build()
   );

   public BukkitScheduler(OnyxShop plugin) {
      this.scheduler = plugin.getServer().getScheduler();
   }

   @Override
   public ScheduledTask runTask(OnyxShop plugin, Runnable task) {
      return new BukkitTask(this.scheduler.runTask(plugin, task));
   }

   @Override
   public ScheduledTask runTaskFor(OnyxShop plugin, Runnable task, Player player) {
      return new BukkitTask(this.scheduler.runTask(plugin, task));
   }

   @Override
   public ScheduledTask runTaskLater(OnyxShop plugin, Runnable task, long value) {
      return new BukkitTask(this.scheduler.runTaskLater(plugin, task, value));
   }

   @Override
   public ScheduledTask runTaskLaterFor(OnyxShop plugin, Runnable task, Player player, long value) {
      return new BukkitTask(this.scheduler.runTaskLater(plugin, task, value));
   }

   @Override
   public ScheduledTask runTaskTimer(OnyxShop plugin, Runnable task, long time, long value) {
      return new BukkitTask(this.scheduler.runTaskTimer(plugin, task, time, value));
   }

   @Override
   public ScheduledTask runTaskAsync(OnyxShop plugin, Runnable task) {
      return new BukkitTask(this.scheduler.runTaskAsynchronously(plugin, task));
   }

   @Override
   public ScheduledTask runTaskLaterAsync(OnyxShop plugin, Runnable task, long value) {
      return new BukkitTask(this.scheduler.runTaskLaterAsynchronously(plugin, task, value));
   }

   @Override
   public ScheduledTask runTaskAsyncTimer(OnyxShop plugin, Runnable task, long time, long value) {
      return new BukkitTask(this.scheduler.runTaskTimerAsynchronously(plugin, task, time, value));
   }

   @Override
   public void runKillableTask(Runnable task) {
      this.thread.execute(task);
   }

   @Override
   public ScheduledTask runKillableTaskLater(Runnable task, long value) {
      return new KillableTask(this.thread.schedule(task, value * 50L, TimeUnit.MILLISECONDS));
   }

   @Override
   public ScheduledTask runKillableTaskTimer(Runnable task, long time, long value) {
      return new KillableTask(this.thread.scheduleAtFixedRate(task, time * 50L, value * 50L, TimeUnit.MILLISECONDS));
   }

   @Override
   public void cancelTasks(OnyxShop plugin) {
      this.scheduler.cancelTasks(plugin);
      this.thread.shutdownNow();
   }
}
