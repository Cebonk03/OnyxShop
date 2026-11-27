package com.onyxshop.util.scheduler;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface ServerScheduler {
   ScheduledTask runTask(OnyxShop plugin, Runnable task);

   ScheduledTask runTaskFor(OnyxShop plugin, Runnable task, Player player);

   ScheduledTask runTaskLater(OnyxShop plugin, Runnable task, long value);

   ScheduledTask runTaskLaterFor(OnyxShop plugin, Runnable task, Player player, long value);

   ScheduledTask runTaskTimer(OnyxShop plugin, Runnable task, long time, long value);

   ScheduledTask runTaskAsync(OnyxShop plugin, Runnable task);

   ScheduledTask runTaskLaterAsync(OnyxShop plugin, Runnable task, long value);

   ScheduledTask runTaskAsyncTimer(OnyxShop plugin, Runnable task, long time, long value);

   void runKillableTask(Runnable task);

   ScheduledTask runKillableTaskLater(Runnable task, long value);

   ScheduledTask runKillableTaskTimer(Runnable task, long time, long value);

   void cancelTasks(OnyxShop plugin);
}
