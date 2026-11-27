package com.onyxshop.util.scheduler.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.onyxshop.OnyxShop;
import com.onyxshop.util.scheduler.ScheduledTask;
import com.onyxshop.util.scheduler.ServerScheduler;
import com.onyxshop.util.scheduler.tasks.FoliaTask;
import com.onyxshop.util.scheduler.tasks.KillableTask;
import org.bukkit.entity.Player;

public class FoliaScheduler implements ServerScheduler {
   private final GlobalRegionScheduler globalRegionScheduler;
   private final AsyncScheduler asyncScheduler;
   private final ScheduledExecutorService thread = Executors.newScheduledThreadPool(
      2, new ThreadFactoryBuilder().setNameFormat("ONYX_UTIL_THREAD #%d").build()
   );

   public FoliaScheduler(OnyxShop plugin) {
      this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
      this.asyncScheduler = plugin.getServer().getAsyncScheduler();
   }

   @Override
   public ScheduledTask runTask(OnyxShop plugin, Runnable task) {
      return new FoliaTask(this.globalRegionScheduler.run(plugin, element -> task.run()));
   }

   @Override
   public ScheduledTask runTaskFor(OnyxShop plugin, Runnable task, Player player) {
      io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = player.getScheduler().run(plugin, element -> task.run(), null);
      return scheduledTask != null ? new FoliaTask(scheduledTask) : () -> {
      };
   }

   @Override
   public ScheduledTask runTaskLaterFor(OnyxShop plugin, Runnable task, Player player, long value) {
      if (value <= 0L) {
         return this.runTaskFor(plugin, task, player);
      }

      io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = player.getScheduler()
         .runDelayed(plugin, element -> task.run(), null, value);
      return scheduledTask != null ? new FoliaTask(scheduledTask) : () -> {
      };
   }

   @Override
   public ScheduledTask runTaskLater(OnyxShop plugin, Runnable task, long value) {
      return (ScheduledTask)(value <= 0L ? this.runTask(plugin, task) : new FoliaTask(this.globalRegionScheduler.runDelayed(plugin, element -> task.run(), value)));
   }

   @Override
   public ScheduledTask runTaskTimer(OnyxShop plugin, Runnable task, long time, long value) {
      if (time <= 0L) {
         time = 1L;
      }

      return new FoliaTask(this.globalRegionScheduler.runAtFixedRate(plugin, element -> task.run(), time, value));
   }

   @Override
   public ScheduledTask runTaskAsync(OnyxShop plugin, Runnable task) {
      return new FoliaTask(this.asyncScheduler.runNow(plugin, element -> task.run()));
   }

   @Override
   public ScheduledTask runTaskLaterAsync(OnyxShop plugin, Runnable task, long value) {
      return (ScheduledTask)(value <= 0L
         ? this.runTaskAsync(plugin, task)
         : new FoliaTask(this.asyncScheduler.runDelayed(plugin, element -> task.run(), value * 50L, TimeUnit.MILLISECONDS)));
   }

   @Override
   public ScheduledTask runTaskAsyncTimer(OnyxShop plugin, Runnable task, long time, long value) {
      if (time <= 0L) {
         time = 1L;
      }

      return new FoliaTask(this.asyncScheduler.runAtFixedRate(plugin, element -> task.run(), time * 50L, value * 50L, TimeUnit.MILLISECONDS));
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
      this.asyncScheduler.cancelTasks(plugin);
      this.globalRegionScheduler.cancelTasks(plugin);
      this.thread.shutdownNow();
   }
}
