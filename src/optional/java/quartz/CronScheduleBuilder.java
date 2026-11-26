package org.quartz;

import java.text.ParseException;
import java.util.TimeZone;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.MutableTrigger;

public class CronScheduleBuilder extends ScheduleBuilder<CronTrigger> {
   private CronExpression cronExpression;
   private int misfireInstruction = 0;

   protected CronScheduleBuilder(CronExpression var1) {
      if (var1 == null) {
         throw new NullPointerException("cronExpression cannot be null");
      } else {
         this.cronExpression = var1;
      }
   }

   @Override
   public MutableTrigger build() {
      CronTriggerImpl var1 = new CronTriggerImpl();
      var1.setCronExpression(this.cronExpression);
      var1.setTimeZone(this.cronExpression.getTimeZone());
      var1.setMisfireInstruction(this.misfireInstruction);
      return var1;
   }

   public static CronScheduleBuilder cronSchedule(String var0) {
      try {
         return cronSchedule(new CronExpression(var0));
      } catch (ParseException var2) {
         throw new RuntimeException("CronExpression '" + var0 + "' is invalid.", var2);
      }
   }

   public static CronScheduleBuilder cronScheduleNonvalidatedExpression(String var0) {
      return cronSchedule(new CronExpression(var0));
   }

   private static CronScheduleBuilder cronScheduleNoParseException(String var0) {
      try {
         return cronSchedule(new CronExpression(var0));
      } catch (ParseException var2) {
         throw new RuntimeException("CronExpression '" + var0 + "' is invalid, which should not be possible, please report bug to Quartz developers.", var2);
      }
   }

   public static CronScheduleBuilder cronSchedule(CronExpression var0) {
      return new CronScheduleBuilder(var0);
   }

   public static CronScheduleBuilder dailyAtHourAndMinute(int var0, int var1) {
      DateBuilder.validateHour(var0);
      DateBuilder.validateMinute(var1);
      String var2 = String.format("0 %d %d ? * *", var1, var0);
      return cronScheduleNoParseException(var2);
   }

   public static CronScheduleBuilder atHourAndMinuteOnGivenDaysOfWeek(int var0, int var1, Integer... var2) {
      if (var2 != null && var2.length != 0) {
         Integer[] var3 = var2;
         int var4 = var2.length;

         for (int var5 = 0; var5 < var4; var5++) {
            int var6 = var3[var5];
            DateBuilder.validateDayOfWeek(var6);
         }

         DateBuilder.validateHour(var0);
         DateBuilder.validateMinute(var1);
         String var7 = String.format("0 %d %d ? * %d", var1, var0, var2[0]);

         for (int var8 = 1; var8 < var2.length; var8++) {
            var7 = var7 + "," + var2[var8];
         }

         return cronScheduleNoParseException(var7);
      } else {
         throw new IllegalArgumentException("You must specify at least one day of week.");
      }
   }

   public static CronScheduleBuilder weeklyOnDayAndHourAndMinute(int var0, int var1, int var2) {
      DateBuilder.validateDayOfWeek(var0);
      DateBuilder.validateHour(var1);
      DateBuilder.validateMinute(var2);
      String var3 = String.format("0 %d %d ? * %d", var2, var1, var0);
      return cronScheduleNoParseException(var3);
   }

   public static CronScheduleBuilder monthlyOnDayAndHourAndMinute(int var0, int var1, int var2) {
      DateBuilder.validateDayOfMonth(var0);
      DateBuilder.validateHour(var1);
      DateBuilder.validateMinute(var2);
      String var3 = String.format("0 %d %d %d * ?", var2, var1, var0);
      return cronScheduleNoParseException(var3);
   }

   public CronScheduleBuilder inTimeZone(TimeZone var1) {
      this.cronExpression.setTimeZone(var1);
      return this;
   }

   public CronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
      this.misfireInstruction = -1;
      return this;
   }

   public CronScheduleBuilder withMisfireHandlingInstructionDoNothing() {
      this.misfireInstruction = 2;
      return this;
   }

   public CronScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
      this.misfireInstruction = 1;
      return this;
   }
}
