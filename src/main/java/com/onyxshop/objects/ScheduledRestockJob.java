package com.onyxshop.objects;

import com.onyxshop.methodes.SendMessage;

public class ScheduledRestockJob {
   private final Runnable onExecute;
   private final String item;
   private final int mode;
   private final long nextFire;

   public ScheduledRestockJob(Runnable var1, String var2, int var3, long var4) {
      this.onExecute = var1;
      this.item = var2;
      this.mode = var3;
      this.nextFire = var4;
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
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to execute restock job for shop item '" + this.item + "' and stock mode " + this.mode);
      }
   }
}
