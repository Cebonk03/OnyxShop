package com.onyxshop.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ScheduleCache;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.TimeUtils;
import org.bukkit.entity.Player;

public class DelayedCommandProvider {
   private final OnyxShop plugin;
   private final List<ScheduleCache.DelayedCommand> delayedCommands = new ArrayList<>();

   public DelayedCommandProvider(OnyxShop var1) {
      this.plugin = var1;
   }

   public void loadDelayedCommands() {
      int var1 = 0;

      for (ScheduleCache.Schedule var3 : this.plugin.getScheduleCache().getDelayedCommands()) {
         ScheduleCache.DelayedCommand var4 = (ScheduleCache.DelayedCommand)var3;
         var1++;
         long var5 = new Date().toInstant().toEpochMilli();
         if (var4.getNextRun() < var5) {
            SendMessage.logDebugMessage(
               "Delayed command('"
                  + var4.getTask()
                  + "') for player '"
                  + this.getPlayer(var4)
                  + "' ended at "
                  + TimeUtils.getDateTime(var4.getNextRun())
                  + " when server was offline, executing command when server finishes loading..."
            );
            this.plugin.runTaskLater(() -> {
               try {
                  CommandHelper.executeDelayedCommand(var4);
               } finally {
                  this.plugin.getScheduleCache().removeDelayedCommand(var4);
               }
            }, 40L);
         } else {
            this.scheduleDelayedCommand(var4, var4.getNextRun() - var5);
         }
      }

      if (var1 != 0) {
         SendMessage.infoMessage("Completed loading " + var1 + " cached scheduled command(s)");
      }
   }

   private void scheduleDelayedCommand(ScheduleCache.DelayedCommand var1, long var2) {
      SendMessage.logDebugMessage(
         "Scheduling delayed command('"
            + var1.getTask()
            + "') for player '"
            + this.getPlayer(var1)
            + "' in "
            + TimeUtils.getReadableTime(var2)
            + "("
            + TimeUtils.getDateTime(var1.getNextRun())
            + ")"
      );
      this.plugin.runTaskLater(() -> {
         try {
            CommandHelper.executeDelayedCommand(var1);
         } finally {
            this.plugin.getScheduleCache().removeDelayedCommand(var1);
         }
      }, var2 / 1000L * 20L);
   }

   private String getPlayer(ScheduleCache.DelayedCommand var1) {
      Map var2 = var1.getArgs();
      return var2.keySet().contains("player") ? (String)var2.get("player") : (String)var2.get("uuid");
   }

   public void addDelayedCommand(String var1, long var2, final ShopItem var4, final Player var5, final String var6, final int var7) {
      this.delayedCommands.add(new ScheduleCache.DelayedCommand(new Date().toInstant().toEpochMilli() + var2, var1, new HashMap<String, String>() {
         {
            this.put("item", var4.getItemPath());
            this.put("price", var6);
            this.put("amount", String.valueOf(var7));
            this.put("player", var5.getName());
            this.put("uuid", var5.getUniqueId().toString());
         }
      }));
   }

   public void addDelayedCommand(String var1, long var2, final DisplayItem var4, final Player var5) {
      this.delayedCommands.add(new ScheduleCache.DelayedCommand(new Date().toInstant().toEpochMilli() + var2, var1, new HashMap<String, String>() {
         {
            if (var4 != null) {
               this.put("item", var4.getItemPath());
            }

            this.put("player", var5.getName());
            this.put("uuid", var5.getUniqueId().toString());
         }
      }));
   }

   public void cacheDelayedCommands() {
      long var1 = new Date().toInstant().toEpochMilli();
      ArrayList var3 = new ArrayList();

      for (ScheduleCache.DelayedCommand var5 : this.delayedCommands) {
         if (var5.getNextRun() > var1) {
            var3.add(var5);
         }
      }

      this.plugin.getScheduleCache().cacheDelayedCommands(var3);
   }
}
