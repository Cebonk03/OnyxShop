package org.quartz;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateBuilder {
   public static final int SUNDAY = 1;
   public static final int MONDAY = 2;
   public static final int TUESDAY = 3;
   public static final int WEDNESDAY = 4;
   public static final int THURSDAY = 5;
   public static final int FRIDAY = 6;
   public static final int SATURDAY = 7;
   public static final int JANUARY = 1;
   public static final int FEBRUARY = 2;
   public static final int MARCH = 3;
   public static final int APRIL = 4;
   public static final int MAY = 5;
   public static final int JUNE = 6;
   public static final int JULY = 7;
   public static final int AUGUST = 8;
   public static final int SEPTEMBER = 9;
   public static final int OCTOBER = 10;
   public static final int NOVEMBER = 11;
   public static final int DECEMBER = 12;
   public static final long MILLISECONDS_IN_MINUTE = 60000L;
   public static final long MILLISECONDS_IN_HOUR = 3600000L;
   public static final long SECONDS_IN_MOST_DAYS = 86400L;
   public static final long MILLISECONDS_IN_DAY = 86400000L;
   private int month;
   private int day;
   private int year;
   private int hour;
   private int minute;
   private int second;
   private TimeZone tz;
   private Locale lc;
   private static final int MAX_YEAR = java.util.Calendar.getInstance().get(1) + 100;

   private DateBuilder() {
      java.util.Calendar var1 = java.util.Calendar.getInstance();
      this.month = var1.get(2) + 1;
      this.day = var1.get(5);
      this.year = var1.get(1);
      this.hour = var1.get(11);
      this.minute = var1.get(12);
      this.second = var1.get(13);
   }

   private DateBuilder(TimeZone var1) {
      java.util.Calendar var2 = java.util.Calendar.getInstance(var1);
      this.tz = var1;
      this.month = var2.get(2) + 1;
      this.day = var2.get(5);
      this.year = var2.get(1);
      this.hour = var2.get(11);
      this.minute = var2.get(12);
      this.second = var2.get(13);
   }

   private DateBuilder(Locale var1) {
      java.util.Calendar var2 = java.util.Calendar.getInstance(var1);
      this.lc = var1;
      this.month = var2.get(2) + 1;
      this.day = var2.get(5);
      this.year = var2.get(1);
      this.hour = var2.get(11);
      this.minute = var2.get(12);
      this.second = var2.get(13);
   }

   private DateBuilder(TimeZone var1, Locale var2) {
      java.util.Calendar var3 = java.util.Calendar.getInstance(var1, var2);
      this.tz = var1;
      this.lc = var2;
      this.month = var3.get(2) + 1;
      this.day = var3.get(5);
      this.year = var3.get(1);
      this.hour = var3.get(11);
      this.minute = var3.get(12);
      this.second = var3.get(13);
   }

   public static DateBuilder newDate() {
      return new DateBuilder();
   }

   public static DateBuilder newDateInTimezone(TimeZone var0) {
      return new DateBuilder(var0);
   }

   public static DateBuilder newDateInLocale(Locale var0) {
      return new DateBuilder(var0);
   }

   public static DateBuilder newDateInTimeZoneAndLocale(TimeZone var0, Locale var1) {
      return new DateBuilder(var0, var1);
   }

   public Date build() {
      java.util.Calendar var1;
      if (this.tz != null && this.lc != null) {
         var1 = java.util.Calendar.getInstance(this.tz, this.lc);
      } else if (this.tz != null) {
         var1 = java.util.Calendar.getInstance(this.tz);
      } else if (this.lc != null) {
         var1 = java.util.Calendar.getInstance(this.lc);
      } else {
         var1 = java.util.Calendar.getInstance();
      }

      var1.set(1, this.year);
      var1.set(2, this.month - 1);
      var1.set(5, this.day);
      var1.set(11, this.hour);
      var1.set(12, this.minute);
      var1.set(13, this.second);
      var1.set(14, 0);
      return var1.getTime();
   }

   public DateBuilder atHourOfDay(int var1) {
      validateHour(var1);
      this.hour = var1;
      return this;
   }

   public DateBuilder atMinute(int var1) {
      validateMinute(var1);
      this.minute = var1;
      return this;
   }

   public DateBuilder atSecond(int var1) {
      validateSecond(var1);
      this.second = var1;
      return this;
   }

   public DateBuilder atHourMinuteAndSecond(int var1, int var2, int var3) {
      validateHour(var1);
      validateMinute(var2);
      validateSecond(var3);
      this.hour = var1;
      this.second = var3;
      this.minute = var2;
      return this;
   }

   public DateBuilder onDay(int var1) {
      validateDayOfMonth(var1);
      this.day = var1;
      return this;
   }

   public DateBuilder inMonth(int var1) {
      validateMonth(var1);
      this.month = var1;
      return this;
   }

   public DateBuilder inMonthOnDay(int var1, int var2) {
      validateMonth(var1);
      validateDayOfMonth(var2);
      this.month = var1;
      this.day = var2;
      return this;
   }

   public DateBuilder inYear(int var1) {
      validateYear(var1);
      this.year = var1;
      return this;
   }

   public DateBuilder inTimeZone(TimeZone var1) {
      this.tz = var1;
      return this;
   }

   public DateBuilder inLocale(Locale var1) {
      this.lc = var1;
      return this;
   }

   public static Date futureDate(int var0, DateBuilder.IntervalUnit var1) {
      java.util.Calendar var2 = java.util.Calendar.getInstance();
      var2.setTime(new Date());
      var2.setLenient(true);
      var2.add(translate(var1), var0);
      return var2.getTime();
   }

   private static int translate(DateBuilder.IntervalUnit var0) {
      switch (var0) {
         case DAY:
            return 6;
         case HOUR:
            return 11;
         case MINUTE:
            return 12;
         case MONTH:
            return 2;
         case SECOND:
            return 13;
         case MILLISECOND:
            return 14;
         case WEEK:
            return 3;
         case YEAR:
            return 1;
         default:
            throw new IllegalArgumentException("Unknown IntervalUnit");
      }
   }

   public static Date tomorrowAt(int var0, int var1, int var2) {
      validateSecond(var2);
      validateMinute(var1);
      validateHour(var0);
      Date var3 = new Date();
      java.util.Calendar var4 = java.util.Calendar.getInstance();
      var4.setTime(var3);
      var4.setLenient(true);
      var4.add(6, 1);
      var4.set(11, var0);
      var4.set(12, var1);
      var4.set(13, var2);
      var4.set(14, 0);
      return var4.getTime();
   }

   public static Date todayAt(int var0, int var1, int var2) {
      return dateOf(var0, var1, var2);
   }

   public static Date dateOf(int var0, int var1, int var2) {
      validateSecond(var2);
      validateMinute(var1);
      validateHour(var0);
      Date var3 = new Date();
      java.util.Calendar var4 = java.util.Calendar.getInstance();
      var4.setTime(var3);
      var4.setLenient(true);
      var4.set(11, var0);
      var4.set(12, var1);
      var4.set(13, var2);
      var4.set(14, 0);
      return var4.getTime();
   }

   public static Date dateOf(int var0, int var1, int var2, int var3, int var4) {
      validateSecond(var2);
      validateMinute(var1);
      validateHour(var0);
      validateDayOfMonth(var3);
      validateMonth(var4);
      Date var5 = new Date();
      java.util.Calendar var6 = java.util.Calendar.getInstance();
      var6.setTime(var5);
      var6.set(2, var4 - 1);
      var6.set(5, var3);
      var6.set(11, var0);
      var6.set(12, var1);
      var6.set(13, var2);
      var6.set(14, 0);
      return var6.getTime();
   }

   public static Date dateOf(int var0, int var1, int var2, int var3, int var4, int var5) {
      validateSecond(var2);
      validateMinute(var1);
      validateHour(var0);
      validateDayOfMonth(var3);
      validateMonth(var4);
      validateYear(var5);
      Date var6 = new Date();
      java.util.Calendar var7 = java.util.Calendar.getInstance();
      var7.setTime(var6);
      var7.set(1, var5);
      var7.set(2, var4 - 1);
      var7.set(5, var3);
      var7.set(11, var0);
      var7.set(12, var1);
      var7.set(13, var2);
      var7.set(14, 0);
      return var7.getTime();
   }

   public static Date evenHourDateAfterNow() {
      return evenHourDate(null);
   }

   public static Date evenHourDate(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.setLenient(true);
      var1.set(11, var1.get(11) + 1);
      var1.set(12, 0);
      var1.set(13, 0);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date evenHourDateBefore(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.set(12, 0);
      var1.set(13, 0);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date evenMinuteDateAfterNow() {
      return evenMinuteDate(null);
   }

   public static Date evenMinuteDate(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.setLenient(true);
      var1.set(12, var1.get(12) + 1);
      var1.set(13, 0);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date evenMinuteDateBefore(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.set(13, 0);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date evenSecondDateAfterNow() {
      return evenSecondDate(null);
   }

   public static Date evenSecondDate(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.setLenient(true);
      var1.set(13, var1.get(13) + 1);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date evenSecondDateBefore(Date var0) {
      if (var0 == null) {
         var0 = new Date();
      }

      java.util.Calendar var1 = java.util.Calendar.getInstance();
      var1.setTime(var0);
      var1.set(14, 0);
      return var1.getTime();
   }

   public static Date nextGivenMinuteDate(Date var0, int var1) {
      if (var1 >= 0 && var1 <= 59) {
         if (var0 == null) {
            var0 = new Date();
         }

         java.util.Calendar var2 = java.util.Calendar.getInstance();
         var2.setTime(var0);
         var2.setLenient(true);
         if (var1 == 0) {
            var2.set(11, var2.get(11) + 1);
            var2.set(12, 0);
            var2.set(13, 0);
            var2.set(14, 0);
            return var2.getTime();
         } else {
            int var3 = var2.get(12);
            int var4 = var3 / var1;
            int var5 = var1 * (var4 + 1);
            if (var5 < 60) {
               var2.set(12, var5);
               var2.set(13, 0);
               var2.set(14, 0);
               return var2.getTime();
            } else {
               var2.set(11, var2.get(11) + 1);
               var2.set(12, 0);
               var2.set(13, 0);
               var2.set(14, 0);
               return var2.getTime();
            }
         }
      } else {
         throw new IllegalArgumentException("minuteBase must be >=0 and <= 59");
      }
   }

   public static Date nextGivenSecondDate(Date var0, int var1) {
      if (var1 >= 0 && var1 <= 59) {
         if (var0 == null) {
            var0 = new Date();
         }

         java.util.Calendar var2 = java.util.Calendar.getInstance();
         var2.setTime(var0);
         var2.setLenient(true);
         if (var1 == 0) {
            var2.set(12, var2.get(12) + 1);
            var2.set(13, 0);
            var2.set(14, 0);
            return var2.getTime();
         } else {
            int var3 = var2.get(13);
            int var4 = var3 / var1;
            int var5 = var1 * (var4 + 1);
            if (var5 < 60) {
               var2.set(13, var5);
               var2.set(14, 0);
               return var2.getTime();
            } else {
               var2.set(12, var2.get(12) + 1);
               var2.set(13, 0);
               var2.set(14, 0);
               return var2.getTime();
            }
         }
      } else {
         throw new IllegalArgumentException("secondBase must be >=0 and <= 59");
      }
   }

   public static Date translateTime(Date var0, TimeZone var1, TimeZone var2) {
      Date var3 = new Date();
      int var4 = var2.getOffset(var0.getTime()) - var1.getOffset(var0.getTime());
      var3.setTime(var0.getTime() - var4);
      return var3;
   }

   public static void validateDayOfWeek(int var0) {
      if (var0 < 1 || var0 > 7) {
         throw new IllegalArgumentException("Invalid day of week.");
      }
   }

   public static void validateHour(int var0) {
      if (var0 < 0 || var0 > 23) {
         throw new IllegalArgumentException("Invalid hour (must be >= 0 and <= 23).");
      }
   }

   public static void validateMinute(int var0) {
      if (var0 < 0 || var0 > 59) {
         throw new IllegalArgumentException("Invalid minute (must be >= 0 and <= 59).");
      }
   }

   public static void validateSecond(int var0) {
      if (var0 < 0 || var0 > 59) {
         throw new IllegalArgumentException("Invalid second (must be >= 0 and <= 59).");
      }
   }

   public static void validateDayOfMonth(int var0) {
      if (var0 < 1 || var0 > 31) {
         throw new IllegalArgumentException("Invalid day of month.");
      }
   }

   public static void validateMonth(int var0) {
      if (var0 < 1 || var0 > 12) {
         throw new IllegalArgumentException("Invalid month (must be >= 1 and <= 12.");
      }
   }

   public static void validateYear(int var0) {
      if (var0 < 0 || var0 > MAX_YEAR) {
         throw new IllegalArgumentException("Invalid year (must be >= 0 and <= " + MAX_YEAR);
      }
   }

   public static enum IntervalUnit {
      MILLISECOND,
      SECOND,
      MINUTE,
      HOUR,
      DAY,
      WEEK,
      MONTH,
      YEAR;

      private IntervalUnit() {
      }
   }
}
