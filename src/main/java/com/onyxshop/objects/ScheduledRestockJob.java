package com.onyxshop.objects;

import com.onyxshop.methodes.SendMessage;

public class ScheduledRestockJob {
   private final Runnable onExecute;
   private final String item;
   private final int mode;
   private final long nextFire;

   public ScheduledRestockJob(Runnable task, String text, int restockTime, long value) {
      this.onExecute = task;
      this.item = text;
      this.mode = restockTime;
      this.nextFire = value;
   }

   public void execute() {
      try {
         this.onExecute.run();
         if (this.mode == 1) {
            SendMessage.logDebugMessage(
               "Item '" + this.item + "' has been restocked successfully for global restock and will next schedule at " + this.nextFire
            );
         } else if (this.mode == 2) {
            SendMessage.logDebugMessage("Item '" + this.item + "' has been restocked successfully for every player and will next schedule at " + this.nextFire);
         }
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to execute restock job for shop item '" + this.item + "' and stock mode " + this.mode);
      }
   }
}
