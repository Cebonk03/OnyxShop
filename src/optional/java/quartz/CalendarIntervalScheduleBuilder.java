package org.quartz;

import java.util.TimeZone;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.spi.MutableTrigger;

public class CalendarIntervalScheduleBuilder extends ScheduleBuilder<CalendarIntervalTrigger> {
   private int interval = 1;
   private DateBuilder.IntervalUnit intervalUnit = DateBuilder.IntervalUnit.DAY;
   private int misfireInstruction = 0;
   private TimeZone timeZone;
   private boolean preserveHourOfDayAcrossDaylightSavings;
   private boolean skipDayIfHourDoesNotExist;

   protected CalendarIntervalScheduleBuilder() {
   }

   public static CalendarIntervalScheduleBuilder calendarIntervalSchedule() {
      return new CalendarIntervalScheduleBuilder();
   }

   @Override
   public MutableTrigger build() {
      CalendarIntervalTriggerImpl var1 = new CalendarIntervalTriggerImpl();
      var1.setRepeatInterval(this.interval);
      var1.setRepeatIntervalUnit(this.intervalUnit);
      var1.setMisfireInstruction(this.misfireInstruction);
      var1.setTimeZone(this.timeZone);
      var1.setPreserveHourOfDayAcrossDaylightSavings(this.preserveHourOfDayAcrossDaylightSavings);
      var1.setSkipDayIfHourDoesNotExist(this.skipDayIfHourDoesNotExist);
      return var1;
   }

   public CalendarIntervalScheduleBuilder withInterval(int var1, DateBuilder.IntervalUnit var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("TimeUnit must be specified.");
      } else {
         this.validateInterval(var1);
         this.interval = var1;
         this.intervalUnit = var2;
         return this;
      }
   }

   public CalendarIntervalScheduleBuilder withIntervalInSeconds(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.SECOND;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInMinutes(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.MINUTE;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInHours(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.HOUR;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInDays(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.DAY;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInWeeks(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.WEEK;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInMonths(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.MONTH;
      return this;
   }

   public CalendarIntervalScheduleBuilder withIntervalInYears(int var1) {
      this.validateInterval(var1);
      this.interval = var1;
      this.intervalUnit = DateBuilder.IntervalUnit.YEAR;
      return this;
   }

   public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
      this.misfireInstruction = -1;
      return this;
   }

   public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing() {
      this.misfireInstruction = 2;
      return this;
   }

   public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
      this.misfireInstruction = 1;
      return this;
   }

   public CalendarIntervalScheduleBuilder inTimeZone(TimeZone var1) {
      this.timeZone = var1;
      return this;
   }

   public CalendarIntervalScheduleBuilder preserveHourOfDayAcrossDaylightSavings(boolean var1) {
      this.preserveHourOfDayAcrossDaylightSavings = var1;
      return this;
   }

   public CalendarIntervalScheduleBuilder skipDayIfHourDoesNotExist(boolean var1) {
      this.skipDayIfHourDoesNotExist = var1;
      return this;
   }

   private void validateInterval(int var1) {
      if (var1 <= 0) {
         throw new IllegalArgumentException("Interval must be a positive value.");
      }
   }
}
