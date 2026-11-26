package org.quartz;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;
import org.quartz.spi.MutableTrigger;

public class DailyTimeIntervalScheduleBuilder extends ScheduleBuilder<DailyTimeIntervalTrigger> {
   private int interval = 1;
   private DateBuilder.IntervalUnit intervalUnit = DateBuilder.IntervalUnit.MINUTE;
   private Set<Integer> daysOfWeek;
   private TimeOfDay startTimeOfDay;
   private TimeOfDay endTimeOfDay;
   private int repeatCount = -1;
   private int misfireInstruction = 0;
   public static final Set<Integer> ALL_DAYS_OF_THE_WEEK;
   public static final Set<Integer> MONDAY_THROUGH_FRIDAY;
   public static final Set<Integer> SATURDAY_AND_SUNDAY;

   protected DailyTimeIntervalScheduleBuilder() {
   }

   public static DailyTimeIntervalScheduleBuilder dailyTimeIntervalSchedule() {
      return new DailyTimeIntervalScheduleBuilder();
   }

   @Override
   public MutableTrigger build() {
      DailyTimeIntervalTriggerImpl var1 = new DailyTimeIntervalTriggerImpl();
      var1.setRepeatInterval(this.interval);
      var1.setRepeatIntervalUnit(this.intervalUnit);
      var1.setMisfireInstruction(this.misfireInstruction);
      var1.setRepeatCount(this.repeatCount);
      if (this.daysOfWeek != null) {
         var1.setDaysOfWeek(this.daysOfWeek);
      } else {
         var1.setDaysOfWeek(ALL_DAYS_OF_THE_WEEK);
      }

      if (this.startTimeOfDay != null) {
         var1.setStartTimeOfDay(this.startTimeOfDay);
      } else {
         var1.setStartTimeOfDay(TimeOfDay.hourAndMinuteOfDay(0, 0));
      }

      if (this.endTimeOfDay != null) {
         var1.setEndTimeOfDay(this.endTimeOfDay);
      } else {
         var1.setEndTimeOfDay(TimeOfDay.hourMinuteAndSecondOfDay(23, 59, 59));
      }

      return var1;
   }

   public DailyTimeIntervalScheduleBuilder withInterval(int var1, DateBuilder.IntervalUnit var2) {
      if (var2 != null
         && (var2.equals(DateBuilder.IntervalUnit.SECOND) || var2.equals(DateBuilder.IntervalUnit.MINUTE) || var2.equals(DateBuilder.IntervalUnit.HOUR))) {
         this.validateInterval(var1);
         this.interval = var1;
         this.intervalUnit = var2;
         return this;
      } else {
         throw new IllegalArgumentException("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
      }
   }

   public DailyTimeIntervalScheduleBuilder withIntervalInSeconds(int var1) {
      this.withInterval(var1, DateBuilder.IntervalUnit.SECOND);
      return this;
   }

   public DailyTimeIntervalScheduleBuilder withIntervalInMinutes(int var1) {
      this.withInterval(var1, DateBuilder.IntervalUnit.MINUTE);
      return this;
   }

   public DailyTimeIntervalScheduleBuilder withIntervalInHours(int var1) {
      this.withInterval(var1, DateBuilder.IntervalUnit.HOUR);
      return this;
   }

   public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek(Set<Integer> var1) {
      if (var1 != null && var1.size() != 0) {
         for (Integer var3 : var1) {
            if (!ALL_DAYS_OF_THE_WEEK.contains(var3)) {
               throw new IllegalArgumentException("Invalid value for day of week: " + var3);
            }
         }

         this.daysOfWeek = var1;
         return this;
      } else {
         throw new IllegalArgumentException("Days of week must be an non-empty set.");
      }
   }

   public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek(Integer... var1) {
      HashSet var2 = new HashSet(12);
      Collections.addAll(var2, var1);
      return this.onDaysOfTheWeek(var2);
   }

   public DailyTimeIntervalScheduleBuilder onMondayThroughFriday() {
      this.daysOfWeek = MONDAY_THROUGH_FRIDAY;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder onSaturdayAndSunday() {
      this.daysOfWeek = SATURDAY_AND_SUNDAY;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder onEveryDay() {
      this.daysOfWeek = ALL_DAYS_OF_THE_WEEK;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder startingDailyAt(TimeOfDay var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Start time of day cannot be null!");
      } else {
         this.startTimeOfDay = var1;
         return this;
      }
   }

   public DailyTimeIntervalScheduleBuilder endingDailyAt(TimeOfDay var1) {
      this.endTimeOfDay = var1;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder endingDailyAfterCount(int var1) {
      if (var1 <= 0) {
         throw new IllegalArgumentException("Ending daily after count must be a positive number!");
      } else if (this.startTimeOfDay == null) {
         throw new IllegalArgumentException("You must set the startDailyAt() before calling this endingDailyAfterCount()!");
      } else {
         Date var2 = new Date();
         Date var3 = this.startTimeOfDay.getTimeOfDayForDate(var2);
         Date var4 = TimeOfDay.hourMinuteAndSecondOfDay(23, 59, 59).getTimeOfDayForDate(var2);
         long var5 = var4.getTime() - var3.getTime();
         long var7;
         if (this.intervalUnit == DateBuilder.IntervalUnit.SECOND) {
            var7 = this.interval * 1000L;
         } else if (this.intervalUnit == DateBuilder.IntervalUnit.MINUTE) {
            var7 = this.interval * 1000L * 60L;
         } else {
            if (this.intervalUnit != DateBuilder.IntervalUnit.HOUR) {
               throw new IllegalArgumentException("The IntervalUnit: " + this.intervalUnit + " is invalid for this trigger.");
            }

            var7 = this.interval * 1000L * 60L * 24L;
         }

         if (var5 - var7 <= 0L) {
            throw new IllegalArgumentException("The startTimeOfDay is too late with given Interval and IntervalUnit values.");
         } else {
            long var9 = var5 / var7;
            if (var1 > var9) {
               throw new IllegalArgumentException("The given count " + var1 + " is too large! The max you can set is " + var9);
            } else {
               long var11 = (var1 - 1) * var7;
               Date var13 = new Date(var3.getTime() + var11);
               if (var13.getTime() > var4.getTime()) {
                  throw new IllegalArgumentException("The given count " + var1 + " is too large! The max you can set is " + var9);
               } else {
                  java.util.Calendar var14 = java.util.Calendar.getInstance();
                  var14.setTime(var13);
                  int var15 = var14.get(11);
                  int var16 = var14.get(12);
                  int var17 = var14.get(13);
                  this.endTimeOfDay = TimeOfDay.hourMinuteAndSecondOfDay(var15, var16, var17);
                  return this;
               }
            }
         }
      }
   }

   public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
      this.misfireInstruction = -1;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing() {
      this.misfireInstruction = 2;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
      this.misfireInstruction = 1;
      return this;
   }

   public DailyTimeIntervalScheduleBuilder withRepeatCount(int var1) {
      this.repeatCount = var1;
      return this;
   }

   private void validateInterval(int var1) {
      if (var1 <= 0) {
         throw new IllegalArgumentException("Interval must be a positive value.");
      }
   }

   static {
      HashSet var0 = new HashSet(7);

      for (int var1 = 1; var1 <= 7; var1++) {
         var0.add(var1);
      }

      ALL_DAYS_OF_THE_WEEK = Collections.unmodifiableSet(var0);
      var0 = new HashSet(5);

      for (int var4 = 2; var4 <= 6; var4++) {
         var0.add(var4);
      }

      MONDAY_THROUGH_FRIDAY = Collections.unmodifiableSet(var0);
      var0 = new HashSet(2);
      var0.add(1);
      var0.add(7);
      SATURDAY_AND_SUNDAY = Collections.unmodifiableSet(var0);
   }
}
