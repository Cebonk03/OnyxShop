package org.quartz;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

public interface Trigger extends Serializable, Cloneable, Comparable<Trigger> {
   long serialVersionUID = -3904243490805975570L;
   int MISFIRE_INSTRUCTION_SMART_POLICY = 0;
   int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;
   int DEFAULT_PRIORITY = 5;

   TriggerKey getKey();

   JobKey getJobKey();

   String getDescription();

   String getCalendarName();

   JobDataMap getJobDataMap();

   int getPriority();

   boolean mayFireAgain();

   Date getStartTime();

   Date getEndTime();

   Date getNextFireTime();

   Date getPreviousFireTime();

   Date getFireTimeAfter(Date var1);

   Date getFinalFireTime();

   int getMisfireInstruction();

   TriggerBuilder<? extends Trigger> getTriggerBuilder();

   ScheduleBuilder<? extends Trigger> getScheduleBuilder();

   @Override
   boolean equals(Object var1);

   int compareTo(Trigger var1);

   public static enum CompletedExecutionInstruction {
      NOOP,
      RE_EXECUTE_JOB,
      SET_TRIGGER_COMPLETE,
      DELETE_TRIGGER,
      SET_ALL_JOB_TRIGGERS_COMPLETE,
      SET_TRIGGER_ERROR,
      SET_ALL_JOB_TRIGGERS_ERROR;

      private CompletedExecutionInstruction() {
      }
   }

   public static enum TriggerState {
      NONE,
      NORMAL,
      PAUSED,
      COMPLETE,
      ERROR,
      BLOCKED;

      private TriggerState() {
      }
   }

   public static class TriggerTimeComparator implements Comparator<Trigger>, Serializable {
      private static final long serialVersionUID = -3904243490805975570L;

      public TriggerTimeComparator() {
      }

      public static int compare(Date var0, int var1, TriggerKey var2, Date var3, int var4, TriggerKey var5) {
         if (var0 != null || var3 != null) {
            if (var0 == null) {
               return 1;
            }

            if (var3 == null) {
               return -1;
            }

            if (var0.before(var3)) {
               return -1;
            }

            if (var0.after(var3)) {
               return 1;
            }
         }

         int var6 = var4 - var1;
         return var6 != 0 ? var6 : var2.compareTo(var5);
      }

      public int compare(Trigger var1, Trigger var2) {
         return compare(var1.getNextFireTime(), var1.getPriority(), var1.getKey(), var2.getNextFireTime(), var2.getPriority(), var2.getKey());
      }
   }
}
