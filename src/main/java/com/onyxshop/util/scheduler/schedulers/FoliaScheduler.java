package com.onyxshop.util.scheduler.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
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
   private final RegionScheduler regionScheduler;
   private final AsyncScheduler asyncScheduler;
   private final ScheduledExecutorService thread = Executors.newScheduledThreadPool(
      2, new ThreadFactoryBuilder().setNameFormat("ONYX_UTIL_THREAD #%d").build()
   );

   public FoliaScheduler(OnyxShop var1) {
      this.globalRegionScheduler = var1.getServer().getGlobalRegionScheduler();
      this.regionScheduler = var1.getServer().getRegionScheduler();
      this.asyncScheduler = var1.getServer().getAsyncScheduler();
   }

   @Override
   public ScheduledTask runTask(OnyxShop var1, Runnable var2) {
      return new FoliaTask(this.globalRegionScheduler.run(var1, var1x -> var2.run()));
   }

   @Override
   public ScheduledTask runTaskFor(OnyxShop var1, Runnable var2, Player var3) {
      return new FoliaTask(this.regionScheduler.run(var1, var3.getLocation(), var1x -> var2.run()));
   }

   @Override
   public ScheduledTask runTaskLater(OnyxShop var1, Runnable var2, long var3) {
      return (ScheduledTask)(var3 <= 0L ? this.runTask(var1, var2) : new FoliaTask(this.globalRegionScheduler.runDelayed(var1, var1x -> var2.run(), var3)));
   }

   @Override
   public ScheduledTask runTaskTimer(OnyxShop var1, Runnable var2, long var3, long var5) {
      if (var3 <= 0L) {
         var3 = 1L;
      }

      return new FoliaTask(this.globalRegionScheduler.runAtFixedRate(var1, var1x -> var2.run(), var3, var5));
   }

   @Override
   public ScheduledTask runTaskAsync(OnyxShop var1, Runnable var2) {
      return new FoliaTask(this.asyncScheduler.runNow(var1, var1x -> var2.run()));
   }

   @Override
   public ScheduledTask runTaskLaterAsync(OnyxShop var1, Runnable var2, long var3) {
      return (ScheduledTask)(var3 <= 0L
         ? this.runTaskAsync(var1, var2)
         : new FoliaTask(this.asyncScheduler.runDelayed(var1, var1x -> var2.run(), var3 * 50L, TimeUnit.MILLISECONDS)));
   }

   @Override
   public ScheduledTask runTaskAsyncTimer(OnyxShop var1, Runnable var2, long var3, long var5) {
      if (var3 <= 0L) {
         var3 = 1L;
      }

      return new FoliaTask(this.asyncScheduler.runAtFixedRate(var1, var1x -> var2.run(), var3 * 50L, var5 * 50L, TimeUnit.MILLISECONDS));
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
      this.asyncScheduler.cancelTasks(var1);
      this.globalRegionScheduler.cancelTasks(var1);
      this.thread.shutdownNow();
   }
}
