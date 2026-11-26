package org.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;

public class SimpleTriggerImpl extends AbstractTrigger<SimpleTrigger> implements SimpleTrigger, CoreTrigger {
   private static final long serialVersionUID = -3735980074222850397L;
   private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = Calendar.getInstance().get(1) + 100;
   private Date startTime = null;
   private Date endTime = null;
   private Date nextFireTime = null;
   private Date previousFireTime = null;
   private int repeatCount = 0;
   private long repeatInterval = 0L;
   private int timesTriggered = 0;
   private boolean complete = false;

   public SimpleTriggerImpl() {
   }

   @Deprecated
   public SimpleTriggerImpl(String var1) {
      this(var1, (String)null);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, String var2) {
      this(var1, var2, new Date(), null, 0, 0L);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, int var2, long var3) {
      this(var1, null, var2, var3);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, String var2, int var3, long var4) {
      this(var1, var2, new Date(), null, var3, var4);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, Date var2) {
      this(var1, null, var2);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, String var2, Date var3) {
      this(var1, var2, var3, null, 0, 0L);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, Date var2, Date var3, int var4, long var5) {
      this(var1, null, var2, var3, var4, var5);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, String var2, Date var3, Date var4, int var5, long var6) {
      super(var1, var2);
      this.setStartTime(var3);
      this.setEndTime(var4);
      this.setRepeatCount(var5);
      this.setRepeatInterval(var6);
   }

   @Deprecated
   public SimpleTriggerImpl(String var1, String var2, String var3, String var4, Date var5, Date var6, int var7, long var8) {
      super(var1, var2, var3, var4);
      this.setStartTime(var5);
      this.setEndTime(var6);
      this.setRepeatCount(var7);
      this.setRepeatInterval(var8);
   }

   @Override
   public Date getStartTime() {
      return this.startTime;
   }

   @Override
   public void setStartTime(Date var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Start time cannot be null");
      } else {
         Date var2 = this.getEndTime();
         if (var2 != null && var1 != null && var2.before(var1)) {
            throw new IllegalArgumentException("End time cannot be before start time");
         } else {
            this.startTime = var1;
         }
      }
   }

   @Override
   public Date getEndTime() {
      return this.endTime;
   }

   @Override
   public void setEndTime(Date var1) {
      Date var2 = this.getStartTime();
      if (var2 != null && var1 != null && var2.after(var1)) {
         throw new IllegalArgumentException("End time cannot be before start time");
      } else {
         this.endTime = var1;
      }
   }

   @Override
   public int getRepeatCount() {
      return this.repeatCount;
   }

   public void setRepeatCount(int var1) {
      if (var1 < 0 && var1 != -1) {
         throw new IllegalArgumentException("Repeat count must be >= 0, use the constant REPEAT_INDEFINITELY for infinite.");
      } else {
         this.repeatCount = var1;
      }
   }

   @Override
   public long getRepeatInterval() {
      return this.repeatInterval;
   }

   public void setRepeatInterval(long var1) {
      if (var1 < 0L) {
         throw new IllegalArgumentException("Repeat interval must be >= 0");
      } else {
         this.repeatInterval = var1;
      }
   }

   @Override
   public int getTimesTriggered() {
      return this.timesTriggered;
   }

   public void setTimesTriggered(int var1) {
      this.timesTriggered = var1;
   }

   @Override
   protected boolean validateMisfireInstruction(int var1) {
      return var1 < -1 ? false : var1 <= 5;
   }

   @Override
   public void updateAfterMisfire(org.quartz.Calendar var1) {
      int var2 = this.getMisfireInstruction();
      if (var2 != -1) {
         if (var2 == 0) {
            if (this.getRepeatCount() == 0) {
               var2 = 1;
            } else if (this.getRepeatCount() == -1) {
               var2 = 4;
            } else {
               var2 = 2;
            }
         } else if (var2 == 1 && this.getRepeatCount() != 0) {
            var2 = 3;
         }

         if (var2 == 1) {
            this.setNextFireTime(new Date());
         } else if (var2 == 5) {
            Date var3 = this.getFireTimeAfter(new Date());

            while (var3 != null && var1 != null && !var1.isTimeIncluded(var3.getTime())) {
               var3 = this.getFireTimeAfter(var3);
               if (var3 == null) {
                  break;
               }

               Calendar var4 = Calendar.getInstance();
               var4.setTime(var3);
               if (var4.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                  var3 = null;
               }
            }

            this.setNextFireTime(var3);
         } else if (var2 == 4) {
            Date var6 = this.getFireTimeAfter(new Date());

            while (var6 != null && var1 != null && !var1.isTimeIncluded(var6.getTime())) {
               var6 = this.getFireTimeAfter(var6);
               if (var6 == null) {
                  break;
               }

               Calendar var9 = Calendar.getInstance();
               var9.setTime(var6);
               if (var9.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                  var6 = null;
               }
            }

            if (var6 != null) {
               int var10 = this.computeNumTimesFiredBetween(this.nextFireTime, var6);
               this.setTimesTriggered(this.getTimesTriggered() + var10);
            }

            this.setNextFireTime(var6);
         } else if (var2 == 2) {
            Date var7 = new Date();
            if (this.repeatCount != 0 && this.repeatCount != -1) {
               this.setRepeatCount(this.getRepeatCount() - this.getTimesTriggered());
               this.setTimesTriggered(0);
            }

            if (this.getEndTime() != null && this.getEndTime().before(var7)) {
               this.setNextFireTime(null);
            } else {
               this.setStartTime(var7);
               this.setNextFireTime(var7);
            }
         } else if (var2 == 3) {
            Date var8 = new Date();
            int var11 = this.computeNumTimesFiredBetween(this.nextFireTime, var8);
            if (this.repeatCount != 0 && this.repeatCount != -1) {
               int var5 = this.getRepeatCount() - (this.getTimesTriggered() + var11);
               if (var5 <= 0) {
                  var5 = 0;
               }

               this.setRepeatCount(var5);
               this.setTimesTriggered(0);
            }

            if (this.getEndTime() != null && this.getEndTime().before(var8)) {
               this.setNextFireTime(null);
            } else {
               this.setStartTime(var8);
               this.setNextFireTime(var8);
            }
         }
      }
   }

   @Override
   public void triggered(org.quartz.Calendar var1) {
      this.timesTriggered++;
      this.previousFireTime = this.nextFireTime;
      this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
         if (this.nextFireTime == null) {
            break;
         }

         Calendar var2 = Calendar.getInstance();
         var2.setTime(this.nextFireTime);
         if (var2.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
            this.nextFireTime = null;
         }
      }
   }

   @Override
   public void updateWithNewCalendar(org.quartz.Calendar var1, long var2) {
      this.nextFireTime = this.getFireTimeAfter(this.previousFireTime);
      if (this.nextFireTime != null && var1 != null) {
         Date var4 = new Date();

         while (this.nextFireTime != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
            this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
            if (this.nextFireTime == null) {
               break;
            }

            Calendar var5 = Calendar.getInstance();
            var5.setTime(this.nextFireTime);
            if (var5.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
               this.nextFireTime = null;
            }

            if (this.nextFireTime != null && this.nextFireTime.before(var4)) {
               long var6 = var4.getTime() - this.nextFireTime.getTime();
               if (var6 >= var2) {
                  this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
               }
            }
         }
      }
   }

   @Override
   public Date computeFirstFireTime(org.quartz.Calendar var1) {
      this.nextFireTime = this.getStartTime();

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
         if (this.nextFireTime == null) {
            break;
         }

         Calendar var2 = Calendar.getInstance();
         var2.setTime(this.nextFireTime);
         if (var2.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
            return null;
         }
      }

      return this.nextFireTime;
   }

   @Override
   public Date getNextFireTime() {
      return this.nextFireTime;
   }

   @Override
   public Date getPreviousFireTime() {
      return this.previousFireTime;
   }

   @Override
   public void setNextFireTime(Date var1) {
      this.nextFireTime = var1;
   }

   @Override
   public void setPreviousFireTime(Date var1) {
      this.previousFireTime = var1;
   }

   @Override
   public Date getFireTimeAfter(Date var1) {
      if (this.complete) {
         return null;
      } else if (this.timesTriggered > this.repeatCount && this.repeatCount != -1) {
         return null;
      } else {
         if (var1 == null) {
            var1 = new Date();
         }

         if (this.repeatCount == 0 && var1.compareTo(this.getStartTime()) >= 0) {
            return null;
         } else {
            long var2 = this.getStartTime().getTime();
            long var4 = var1.getTime();
            long var6 = this.getEndTime() == null ? Long.MAX_VALUE : this.getEndTime().getTime();
            if (var6 <= var4) {
               return null;
            } else if (var4 < var2) {
               return new Date(var2);
            } else {
               long var8 = (var4 - var2) / this.repeatInterval + 1L;
               if (var8 > this.repeatCount && this.repeatCount != -1) {
                  return null;
               } else {
                  Date var10 = new Date(var2 + var8 * this.repeatInterval);
                  return var6 <= var10.getTime() ? null : var10;
               }
            }
         }
      }
   }

   public Date getFireTimeBefore(Date var1) {
      if (var1.getTime() < this.getStartTime().getTime()) {
         return null;
      } else {
         int var2 = this.computeNumTimesFiredBetween(this.getStartTime(), var1);
         return new Date(this.getStartTime().getTime() + var2 * this.repeatInterval);
      }
   }

   public int computeNumTimesFiredBetween(Date var1, Date var2) {
      if (this.repeatInterval < 1L) {
         return 0;
      } else {
         long var3 = var2.getTime() - var1.getTime();
         return (int)(var3 / this.repeatInterval);
      }
   }

   @Override
   public Date getFinalFireTime() {
      if (this.repeatCount == 0) {
         return this.startTime;
      } else if (this.repeatCount == -1) {
         return this.getEndTime() == null ? null : this.getFireTimeBefore(this.getEndTime());
      } else {
         long var1 = this.startTime.getTime() + this.repeatCount * this.repeatInterval;
         return this.getEndTime() != null && var1 >= this.getEndTime().getTime() ? this.getFireTimeBefore(this.getEndTime()) : new Date(var1);
      }
   }

   @Override
   public boolean mayFireAgain() {
      return this.getNextFireTime() != null;
   }

   @Override
   public void validate() throws SchedulerException {
      super.validate();
      if (this.repeatCount != 0 && this.repeatInterval < 1L) {
         throw new SchedulerException("Repeat Interval cannot be zero.");
      }
   }

   @Override
   public boolean hasAdditionalProperties() {
      return false;
   }

   @Override
   public ScheduleBuilder<SimpleTrigger> getScheduleBuilder() {
      SimpleScheduleBuilder var1 = SimpleScheduleBuilder.simpleSchedule()
         .withIntervalInMilliseconds(this.getRepeatInterval())
         .withRepeatCount(this.getRepeatCount());
      switch (this.getMisfireInstruction()) {
         case 1:
            var1.withMisfireHandlingInstructionFireNow();
            break;
         case 2:
            var1.withMisfireHandlingInstructionNowWithExistingCount();
            break;
         case 3:
            var1.withMisfireHandlingInstructionNowWithRemainingCount();
            break;
         case 4:
            var1.withMisfireHandlingInstructionNextWithRemainingCount();
            break;
         case 5:
            var1.withMisfireHandlingInstructionNextWithExistingCount();
      }

      return var1;
   }
}
