package org.terracotta.quartz.collections;

import java.io.Serializable;
import java.util.Date;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

public class TimeTrigger implements Comparable<TimeTrigger>, Serializable {
   private final TriggerKey triggerKey;
   private final Long nextFireTime;
   private final int priority;

   TimeTrigger(TriggerKey var1, Date var2, int var3) {
      this.triggerKey = var1;
      this.nextFireTime = var2 == null ? null : var2.getTime();
      this.priority = var3;
   }

   TriggerKey getTriggerKey() {
      return this.triggerKey;
   }

   int getPriority() {
      return this.priority;
   }

   Date getNextFireTime() {
      return this.nextFireTime == null ? null : new Date(this.nextFireTime);
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 instanceof TimeTrigger) {
         TimeTrigger var2 = (TimeTrigger)var1;
         return this.triggerKey.equals(var2.triggerKey);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.triggerKey.hashCode();
   }

   @Override
   public String toString() {
      return "TimeTrigger [triggerKey=" + this.triggerKey + ", nextFireTime=" + new Date(this.nextFireTime) + ", priority=" + this.priority + "]";
   }

   public int compareTo(TimeTrigger var1) {
      return Trigger.TriggerTimeComparator.compare(
         this.getNextFireTime(), this.getPriority(), this.getTriggerKey(), var1.getNextFireTime(), var1.getPriority(), var1.getTriggerKey()
      );
   }
}
