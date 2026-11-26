package org.terracotta.quartz.wrappers;

import java.io.Serializable;
import java.util.Date;
import org.quartz.TriggerKey;

public class FiredTrigger implements Serializable {
   private final String clientId;
   private final TriggerKey triggerKey;
   private final long scheduledFireTime;
   private final long fireTime;

   public FiredTrigger(String var1, TriggerKey var2, long var3) {
      this(var1, var2, var3, System.currentTimeMillis());
   }

   public FiredTrigger(String var1, TriggerKey var2, long var3, long var5) {
      this.clientId = var1;
      this.triggerKey = var2;
      this.scheduledFireTime = var3;
      this.fireTime = var5;
   }

   public String getClientId() {
      return this.clientId;
   }

   public TriggerKey getTriggerKey() {
      return this.triggerKey;
   }

   public long getScheduledFireTime() {
      return this.scheduledFireTime;
   }

   public long getFireTime() {
      return this.fireTime;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "(" + this.triggerKey + ", " + this.getClientId() + ", " + new Date(this.fireTime) + ")";
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      int var2 = 1;
      var2 = 31 * var2 + (this.clientId == null ? 0 : this.clientId.hashCode());
      var2 = 31 * var2 + (int)(this.scheduledFireTime ^ this.scheduledFireTime >>> 32);
      var2 = 31 * var2 + (int)(this.fireTime ^ this.fireTime >>> 32);
      return 31 * var2 + (this.triggerKey == null ? 0 : this.triggerKey.hashCode());
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         FiredTrigger var2 = (FiredTrigger)var1;
         if (this.clientId == null) {
            if (var2.clientId != null) {
               return false;
            }
         } else if (!this.clientId.equals(var2.clientId)) {
            return false;
         }

         if (this.scheduledFireTime != var2.scheduledFireTime) {
            return false;
         } else if (this.fireTime != var2.fireTime) {
            return false;
         } else {
            if (this.triggerKey == null) {
               if (var2.triggerKey != null) {
                  return false;
               }
            } else if (!this.triggerKey.equals(var2.triggerKey)) {
               return false;
            }

            return true;
         }
      }
   }
}
