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

   public BukkitScheduler(OnyxShop var1) {
      this.scheduler = var1.getServer().getScheduler();
   }

   @Override
   public ScheduledTask runTask(OnyxShop var1, Runnable var2) {
      return new BukkitTask(this.scheduler.runTask(var1, var2));
   }

   @Override
   public ScheduledTask runTaskFor(OnyxShop var1, Runnable var2, Player var3) {
      return new BukkitTask(this.scheduler.runTask(var1, var2));
   }

   @Override
   public ScheduledTask runTaskLater(OnyxShop var1, Runnable var2, long var3) {
      return new BukkitTask(this.scheduler.runTaskLater(var1, var2, var3));
   }

   @Override
   public ScheduledTask runTaskTimer(OnyxShop var1, Runnable var2, long var3, long var5) {
      return new BukkitTask(this.scheduler.runTaskTimer(var1, var2, var3, var5));
   }

   @Override
   public ScheduledTask runTaskAsync(OnyxShop var1, Runnable var2) {
      return new BukkitTask(this.scheduler.runTaskAsynchronously(var1, var2));
   }

   @Override
   public ScheduledTask runTaskLaterAsync(OnyxShop var1, Runnable var2, long var3) {
      return new BukkitTask(this.scheduler.runTaskLaterAsynchronously(var1, var2, var3));
   }

   @Override
   public ScheduledTask runTaskAsyncTimer(OnyxShop var1, Runnable var2, long var3, long var5) {
      return new BukkitTask(this.scheduler.runTaskTimerAsynchronously(var1, var2, var3, var5));
   }

   @Override
   public void runKillableTask(Runnable var1) {
      this.thread.execute(var1);
   }

   @Override
   public ScheduledTask runKillableTaskLater(Runnable var1, long var2) {
      return new KillableTask(this.thread.schedule(var1, var2 * 50L, TimeUnit.MILLISECONDS));
   }

   @Override
   public ScheduledTask runKillableTaskTimer(Runnable var1, long var2, long var4) {
      return new KillableTask(this.thread.scheduleAtFixedRate(var1, var2 * 50L, var4 * 50L, TimeUnit.MILLISECONDS));
   }

   @Override
   public void cancelTasks(OnyxShop var1) {
      this.scheduler.cancelTasks(var1);
      this.thread.shutdownNow();
   }
}
