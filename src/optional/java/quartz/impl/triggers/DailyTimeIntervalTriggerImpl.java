package org.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.TimeOfDay;

public class DailyTimeIntervalTriggerImpl extends AbstractTrigger<DailyTimeIntervalTrigger> implements DailyTimeIntervalTrigger, CoreTrigger {
   private static final long serialVersionUID = -632667786771388749L;
   private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = Calendar.getInstance().get(1) + 100;
   private Date startTime = null;
   private Date endTime = null;
   private Date nextFireTime = null;
   private Date previousFireTime = null;
   private int repeatCount = -1;
   private int repeatInterval = 1;
   private DateBuilder.IntervalUnit repeatIntervalUnit = DateBuilder.IntervalUnit.MINUTE;
   private Set<Integer> daysOfWeek;
   private TimeOfDay startTimeOfDay;
   private TimeOfDay endTimeOfDay;
   private int timesTriggered = 0;
   private boolean complete = false;

   public DailyTimeIntervalTriggerImpl() {
   }

   public DailyTimeIntervalTriggerImpl(String var1, TimeOfDay var2, TimeOfDay var3, DateBuilder.IntervalUnit var4, int var5) {
      this(var1, null, var2, var3, var4, var5);
   }

   public DailyTimeIntervalTriggerImpl(String var1, String var2, TimeOfDay var3, TimeOfDay var4, DateBuilder.IntervalUnit var5, int var6) {
      this(var1, var2, new Date(), null, var3, var4, var5, var6);
   }

   public DailyTimeIntervalTriggerImpl(String var1, Date var2, Date var3, TimeOfDay var4, TimeOfDay var5, DateBuilder.IntervalUnit var6, int var7) {
      this(var1, null, var2, var3, var4, var5, var6, var7);
   }

   public DailyTimeIntervalTriggerImpl(String var1, String var2, Date var3, Date var4, TimeOfDay var5, TimeOfDay var6, DateBuilder.IntervalUnit var7, int var8) {
      super(var1, var2);
      this.setStartTime(var3);
      this.setEndTime(var4);
      this.setRepeatIntervalUnit(var7);
      this.setRepeatInterval(var8);
      this.setStartTimeOfDay(var5);
      this.setEndTimeOfDay(var6);
   }

   public DailyTimeIntervalTriggerImpl(
      String var1, String var2, String var3, String var4, Date var5, Date var6, TimeOfDay var7, TimeOfDay var8, DateBuilder.IntervalUnit var9, int var10
   ) {
      super(var1, var2, var3, var4);
      this.setStartTime(var5);
      this.setEndTime(var6);
      this.setRepeatIntervalUnit(var9);
      this.setRepeatInterval(var10);
      this.setStartTimeOfDay(var7);
      this.setEndTimeOfDay(var8);
   }

   @Override
   public Date getStartTime() {
      if (this.startTime == null) {
         this.startTime = new Date();
      }

      return this.startTime;
   }

   @Override
   public void setStartTime(Date var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Start time cannot be null");
      } else {
         Date var2 = this.getEndTime();
         if (var2 != null && var2.before(var1)) {
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
   public DateBuilder.IntervalUnit getRepeatIntervalUnit() {
      return this.repeatIntervalUnit;
   }

   public void setRepeatIntervalUnit(DateBuilder.IntervalUnit var1) {
      if (this.repeatIntervalUnit != null
         && (
            this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.SECOND)
               || this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.MINUTE)
               || this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.HOUR)
         )) {
         this.repeatIntervalUnit = var1;
      } else {
         throw new IllegalArgumentException("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
      }
   }

   @Override
   public int getRepeatInterval() {
      return this.repeatInterval;
   }

   public void setRepeatInterval(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("Repeat interval must be >= 1");
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
      return var1 >= -1 && var1 <= 2;
   }

   @Override
   public void updateAfterMisfire(org.quartz.Calendar var1) {
      int var2 = this.getMisfireInstruction();
      if (var2 != -1) {
         if (var2 == 0) {
            var2 = 1;
         }

         if (var2 == 2) {
            Date var3 = this.getFireTimeAfter(new Date());

            while (var3 != null && var1 != null && !var1.isTimeIncluded(var3.getTime())) {
               var3 = this.getFireTimeAfter(var3);
            }

            this.setNextFireTime(var3);
         } else if (var2 == 1) {
            this.setNextFireTime(new Date());
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

      if (this.nextFireTime == null) {
         this.complete = true;
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
      this.nextFireTime = this.getFireTimeAfter(new Date(this.getStartTime().getTime() - 1000L));

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

   private Calendar createCalendarTime(Date var1) {
      Calendar var2 = Calendar.getInstance();
      var2.setTime(var1);
      return var2;
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
      } else if (this.repeatCount != -1 && this.timesTriggered > this.repeatCount) {
         return null;
      } else {
         if (var1 == null) {
            var1 = new Date(System.currentTimeMillis() + 1000L);
         } else {
            var1 = new Date(var1.getTime() + 1000L);
         }

         if (var1.before(this.startTime)) {
            var1 = this.startTime;
         }

         boolean var2 = false;
         if (this.endTimeOfDay != null) {
            var2 = var1.getTime() > this.endTimeOfDay.getTimeOfDayForDate(var1).getTime();
         }

         Date var3 = this.advanceToNextDayOfWeekIfNecessary(var1, var2);
         if (var3 == null) {
            return null;
         } else {
            Object var4 = null;
            if (this.endTimeOfDay == null) {
               var4 = new TimeOfDay(23, 59, 59).getTimeOfDayForDate(var3);
            } else {
               var4 = this.endTimeOfDay.getTimeOfDayForDate(var3);
            }

            Date var5 = this.startTimeOfDay.getTimeOfDayForDate(var3);
            if (var3.before(var5)) {
               return var5;
            } else {
               long var6 = var3.getTime();
               long var8 = var5.getTime();
               long var10 = (var6 - var8) / 1000L;
               long var12 = this.getRepeatInterval();
               Calendar var14 = this.createCalendarTime(var5);
               DateBuilder.IntervalUnit var15 = this.getRepeatIntervalUnit();
               if (var15.equals(DateBuilder.IntervalUnit.SECOND)) {
                  long var16 = var10 / var12;
                  if (var10 % var12 != 0L) {
                     var16++;
                  }

                  var14.add(13, this.getRepeatInterval() * (int)var16);
                  var3 = var14.getTime();
               } else if (var15.equals(DateBuilder.IntervalUnit.MINUTE)) {
                  long var21 = var10 / (var12 * 60L);
                  if (var10 % (var12 * 60L) != 0L) {
                     var21++;
                  }

                  var14.add(12, this.getRepeatInterval() * (int)var21);
                  var3 = var14.getTime();
               } else if (var15.equals(DateBuilder.IntervalUnit.HOUR)) {
                  long var22 = var10 / (var12 * 60L * 60L);
                  if (var10 % (var12 * 60L * 60L) != 0L) {
                     var22++;
                  }

                  var14.add(11, this.getRepeatInterval() * (int)var22);
                  var3 = var14.getTime();
               }

               if (var3.after((Date)var4)) {
                  var3 = this.advanceToNextDayOfWeekIfNecessary(var3, this.isSameDay(var3, (Date)var4));
                  var3 = this.startTimeOfDay.getTimeOfDayForDate(var3);
               }

               return var3;
            }
         }
      }
   }

   private boolean isSameDay(Date var1, Date var2) {
      Calendar var3 = this.createCalendarTime(var1);
      Calendar var4 = this.createCalendarTime(var2);
      return var3.get(1) == var4.get(1) && var3.get(6) == var4.get(6);
   }

   private Date advanceToNextDayOfWeekIfNecessary(Date var1, boolean var2) {
      TimeOfDay var3 = this.getStartTimeOfDay();
      Date var4 = var3.getTimeOfDayForDate(var1);
      Calendar var5 = this.createCalendarTime(var4);
      int var6 = var5.get(7);
      Set var7 = this.getDaysOfWeek();
      if (var2 || !var7.contains(var6)) {
         for (int var8 = 1; var8 <= 7; var8++) {
            var5.add(5, 1);
            var6 = var5.get(7);
            if (var7.contains(var6)) {
               var1 = var5.getTime();
               break;
            }
         }
      }

      Date var10 = this.getEndTime();
      return var10 != null && var1.getTime() > var10.getTime() ? null : var1;
   }

   @Override
   public Date getFinalFireTime() {
      if (!this.complete && this.getEndTime() != null) {
         Date var1 = this.getEndTime();
         if (this.endTimeOfDay != null) {
            Date var2 = this.endTimeOfDay.getTimeOfDayForDate(var1);
            if (var1.getTime() < var2.getTime()) {
               var1 = var2;
            }
         }

         return var1;
      } else {
         return null;
      }
   }

   @Override
   public boolean mayFireAgain() {
      return this.getNextFireTime() != null;
   }

   @Override
   public void validate() throws SchedulerException {
      super.validate();
      if (this.repeatIntervalUnit != null
         && (
            this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.SECOND)
               || this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.MINUTE)
               || this.repeatIntervalUnit.equals(DateBuilder.IntervalUnit.HOUR)
         )) {
         if (this.repeatInterval < 1) {
            throw new SchedulerException("Repeat Interval cannot be zero.");
         } else {
            long var1 = 86400L;
            if (this.repeatIntervalUnit == DateBuilder.IntervalUnit.SECOND && this.repeatInterval > var1) {
               throw new SchedulerException("repeatInterval can not exceed 24 hours (" + var1 + " seconds). Given " + this.repeatInterval);
            } else if (this.repeatIntervalUnit == DateBuilder.IntervalUnit.MINUTE && this.repeatInterval > var1 / 60L) {
               throw new SchedulerException("repeatInterval can not exceed 24 hours (" + var1 / 60L + " minutes). Given " + this.repeatInterval);
            } else if (this.repeatIntervalUnit == DateBuilder.IntervalUnit.HOUR && this.repeatInterval > 24) {
               throw new SchedulerException("repeatInterval can not exceed 24 hours. Given " + this.repeatInterval + " hours.");
            } else if (this.getEndTimeOfDay() != null && !this.getStartTimeOfDay().before(this.getEndTimeOfDay())) {
               throw new SchedulerException("StartTimeOfDay " + this.startTimeOfDay + " should not come after endTimeOfDay " + this.endTimeOfDay);
            }
         }
      } else {
         throw new SchedulerException("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
      }
   }

   @Override
   public Set<Integer> getDaysOfWeek() {
      if (this.daysOfWeek == null) {
         this.daysOfWeek = DailyTimeIntervalScheduleBuilder.ALL_DAYS_OF_THE_WEEK;
      }

      return this.daysOfWeek;
   }

   public void setDaysOfWeek(Set<Integer> var1) {
      if (var1 == null || var1.size() == 0) {
         throw new IllegalArgumentException("DaysOfWeek set must be a set that contains at least one day.");
      } else if (var1.size() == 0) {
         throw new IllegalArgumentException("DaysOfWeek set must contain at least one day.");
      } else {
         this.daysOfWeek = var1;
      }
   }

   @Override
   public TimeOfDay getStartTimeOfDay() {
      if (this.startTimeOfDay == null) {
         this.startTimeOfDay = new TimeOfDay(0, 0, 0);
      }

      return this.startTimeOfDay;
   }

   public void setStartTimeOfDay(TimeOfDay var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Start time of day cannot be null");
      } else {
         TimeOfDay var2 = this.getEndTimeOfDay();
         if (var2 != null && var2.before(var1)) {
            throw new IllegalArgumentException("End time of day cannot be before start time of day");
         } else {
            this.startTimeOfDay = var1;
         }
      }
   }

   @Override
   public TimeOfDay getEndTimeOfDay() {
      return this.endTimeOfDay;
   }

   public void setEndTimeOfDay(TimeOfDay var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("End time of day cannot be null");
      } else {
         TimeOfDay var2 = this.getStartTimeOfDay();
         if (var2 != null && var1.before(var1)) {
            throw new IllegalArgumentException("End time of day cannot be before start time of day");
         } else {
            this.endTimeOfDay = var1;
         }
      }
   }

   @Override
   public ScheduleBuilder<DailyTimeIntervalTrigger> getScheduleBuilder() {
      DailyTimeIntervalScheduleBuilder var1 = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
         .withInterval(this.getRepeatInterval(), this.getRepeatIntervalUnit())
         .onDaysOfTheWeek(this.getDaysOfWeek())
         .startingDailyAt(this.getStartTimeOfDay())
         .endingDailyAt(this.getEndTimeOfDay());
      switch (this.getMisfireInstruction()) {
         case 1:
            var1.withMisfireHandlingInstructionFireAndProceed();
            break;
         case 2:
            var1.withMisfireHandlingInstructionDoNothing();
      }

      return var1;
   }

   @Override
   public boolean hasAdditionalProperties() {
      return false;
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
}
