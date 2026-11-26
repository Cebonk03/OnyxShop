package com.onyxshop.util.scheduler;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface ServerScheduler {
   ScheduledTask runTask(OnyxShop var1, Runnable var2);

   ScheduledTask runTaskFor(OnyxShop var1, Runnable var2, Player var3);

   ScheduledTask runTaskLater(OnyxShop var1, Runnable var2, long var3);

   ScheduledTask runTaskTimer(OnyxShop var1, Runnable var2, long var3, long var5);

   ScheduledTask runTaskAsync(OnyxShop var1, Runnable var2);

   ScheduledTask runTaskLaterAsync(OnyxShop var1, Runnable var2, long var3);

   ScheduledTask runTaskAsyncTimer(OnyxShop var1, Runnable var2, long var3, long var5);

   void runKillableTask(Runnable var1);

   ScheduledTask runKillableTaskLater(Runnable var1, long var2);

   ScheduledTask runKillableTaskTimer(Runnable var1, long var2, long var4);

   void cancelTasks(OnyxShop var1);
}
