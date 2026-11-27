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

   public DelayedCommandProvider(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void loadDelayedCommands() {
      int value = 0;

      for (ScheduleCache.Schedule schedule : this.plugin.getScheduleCache().getDelayedCommands()) {
         ScheduleCache.DelayedCommand delayedCommand = (ScheduleCache.DelayedCommand)schedule;
         value++;
         long value2 = new Date().toInstant().toEpochMilli();
         if (delayedCommand.getNextRun() < value2) {
            SendMessage.logDebugMessage(
               "Delayed command('"
                  + delayedCommand.getTask()
                  + "') for player '"
                  + this.getPlayer(delayedCommand)
                  + "' ended at "
                  + TimeUtils.getDateTime(delayedCommand.getNextRun())
                  + " when server was offline, executing command when server finishes loading..."
            );
            this.plugin.runTaskLater(() -> {
               try {
                  CommandHelper.executeDelayedCommand(delayedCommand);
               } finally {
                  this.plugin.getScheduleCache().removeDelayedCommand(delayedCommand);
               }
            }, 40L);
         } else {
            this.scheduleDelayedCommand(delayedCommand, delayedCommand.getNextRun() - value2);
         }
      }

      if (value != 0) {
         SendMessage.infoMessage("Completed loading " + value + " cached scheduled command(s)");
      }
   }

   private void scheduleDelayedCommand(ScheduleCache.DelayedCommand delayedCommand, long value) {
      SendMessage.logDebugMessage(
         "Scheduling delayed command('"
            + delayedCommand.getTask()
            + "') for player '"
            + this.getPlayer(delayedCommand)
            + "' in "
            + TimeUtils.getReadableTime(value)
            + "("
            + TimeUtils.getDateTime(delayedCommand.getNextRun())
            + ")"
      );
      this.plugin.runTaskLater(() -> {
         try {
            CommandHelper.executeDelayedCommand(delayedCommand);
         } finally {
            this.plugin.getScheduleCache().removeDelayedCommand(delayedCommand);
         }
      }, value / 1000L * 20L);
   }

   private String getPlayer(ScheduleCache.DelayedCommand delayedCommand) {
      Map args = delayedCommand.getArgs();
      return args.keySet().contains("player") ? (String)args.get("player") : (String)args.get("uuid");
   }

   public void addDelayedCommand(String command, long value, final ShopItem shopItem, final Player player, final String shopId, final int value2) {
      this.delayedCommands.add(new ScheduleCache.DelayedCommand(new Date().toInstant().toEpochMilli() + value, command, new HashMap<String, String>() {
         {
            this.put("item", shopItem.getItemPath());
            this.put("price", shopId);
            this.put("amount", String.valueOf(value2));
            this.put("player", player.getName());
            this.put("uuid", player.getUniqueId().toString());
         }
      }));
   }

   public void addDelayedCommand(String command, long value, final DisplayItem displayItem, final Player player) {
      this.delayedCommands.add(new ScheduleCache.DelayedCommand(new Date().toInstant().toEpochMilli() + value, command, new HashMap<String, String>() {
         {
            if (displayItem != null) {
               this.put("item", displayItem.getItemPath());
            }

            this.put("player", player.getName());
            this.put("uuid", player.getUniqueId().toString());
         }
      }));
   }

   public void cacheDelayedCommands() {
      long value = new Date().toInstant().toEpochMilli();
      ArrayList values = new ArrayList();

      for (ScheduleCache.DelayedCommand delayedCommand : this.delayedCommands) {
         if (delayedCommand.getNextRun() > value) {
            values.add(delayedCommand);
         }
      }

      this.plugin.getScheduleCache().cacheDelayedCommands(values);
   }
}
