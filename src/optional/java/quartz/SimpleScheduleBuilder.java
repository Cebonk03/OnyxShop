package org.quartz;

import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.MutableTrigger;

public class SimpleScheduleBuilder extends ScheduleBuilder<SimpleTrigger> {
   private long interval = 0L;
   private int repeatCount = 0;
   private int misfireInstruction = 0;

   protected SimpleScheduleBuilder() {
   }

   public static SimpleScheduleBuilder simpleSchedule() {
      return new SimpleScheduleBuilder();
   }

   public static SimpleScheduleBuilder repeatMinutelyForever() {
      return simpleSchedule().withIntervalInMinutes(1).repeatForever();
   }

   public static SimpleScheduleBuilder repeatMinutelyForever(int var0) {
      return simpleSchedule().withIntervalInMinutes(var0).repeatForever();
   }

   public static SimpleScheduleBuilder repeatSecondlyForever() {
      return simpleSchedule().withIntervalInSeconds(1).repeatForever();
   }

   public static SimpleScheduleBuilder repeatSecondlyForever(int var0) {
      return simpleSchedule().withIntervalInSeconds(var0).repeatForever();
   }

   public static SimpleScheduleBuilder repeatHourlyForever() {
      return simpleSchedule().withIntervalInHours(1).repeatForever();
   }

   public static SimpleScheduleBuilder repeatHourlyForever(int var0) {
      return simpleSchedule().withIntervalInHours(var0).repeatForever();
   }

   public static SimpleScheduleBuilder repeatMinutelyForTotalCount(int var0) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInMinutes(1).withRepeatCount(var0 - 1);
      }
   }

   public static SimpleScheduleBuilder repeatMinutelyForTotalCount(int var0, int var1) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInMinutes(var1).withRepeatCount(var0 - 1);
      }
   }

   public static SimpleScheduleBuilder repeatSecondlyForTotalCount(int var0) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInSeconds(1).withRepeatCount(var0 - 1);
      }
   }

   public static SimpleScheduleBuilder repeatSecondlyForTotalCount(int var0, int var1) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInSeconds(var1).withRepeatCount(var0 - 1);
      }
   }

   public static SimpleScheduleBuilder repeatHourlyForTotalCount(int var0) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInHours(1).withRepeatCount(var0 - 1);
      }
   }

   public static SimpleScheduleBuilder repeatHourlyForTotalCount(int var0, int var1) {
      if (var0 < 1) {
         throw new IllegalArgumentException("Total count of firings must be at least one! Given count: " + var0);
      } else {
         return simpleSchedule().withIntervalInHours(var1).withRepeatCount(var0 - 1);
      }
   }

   @Override
   public MutableTrigger build() {
      SimpleTriggerImpl var1 = new SimpleTriggerImpl();
      var1.setRepeatInterval(this.interval);
      var1.setRepeatCount(this.repeatCount);
      var1.setMisfireInstruction(this.misfireInstruction);
      return var1;
   }

   public SimpleScheduleBuilder withIntervalInMilliseconds(long var1) {
      this.interval = var1;
      return this;
   }

   public SimpleScheduleBuilder withIntervalInSeconds(int var1) {
      this.interval = var1 * 1000L;
      return this;
   }

   public SimpleScheduleBuilder withIntervalInMinutes(int var1) {
      this.interval = var1 * 60000L;
      return this;
   }

   public SimpleScheduleBuilder withIntervalInHours(int var1) {
      this.interval = var1 * 3600000L;
      return this;
   }

   public SimpleScheduleBuilder withRepeatCount(int var1) {
      this.repeatCount = var1;
      return this;
   }

   public SimpleScheduleBuilder repeatForever() {
      this.repeatCount = -1;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
      this.misfireInstruction = -1;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionFireNow() {
      this.misfireInstruction = 1;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionNextWithExistingCount() {
      this.misfireInstruction = 5;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionNextWithRemainingCount() {
      this.misfireInstruction = 4;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionNowWithExistingCount() {
      this.misfireInstruction = 2;
      return this;
   }

   public SimpleScheduleBuilder withMisfireHandlingInstructionNowWithRemainingCount() {
      this.misfireInstruction = 3;
      return this;
   }
}
