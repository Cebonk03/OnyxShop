package org.quartz;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

public class TimeOfDay implements Serializable {
   private static final long serialVersionUID = 2964774315889061771L;
   private final int hour;
   private final int minute;
   private final int second;

   public TimeOfDay(int var1, int var2, int var3) {
      this.hour = var1;
      this.minute = var2;
      this.second = var3;
      this.validate();
   }

   public TimeOfDay(int var1, int var2) {
      this.hour = var1;
      this.minute = var2;
      this.second = 0;
      this.validate();
   }

   private void validate() {
      if (this.hour < 0 || this.hour > 23) {
         throw new IllegalArgumentException("Hour must be from 0 to 23");
      } else if (this.minute < 0 || this.minute > 59) {
         throw new IllegalArgumentException("Minute must be from 0 to 59");
      } else if (this.second < 0 || this.second > 59) {
         throw new IllegalArgumentException("Second must be from 0 to 59");
      }
   }

   public static TimeOfDay hourMinuteAndSecondOfDay(int var0, int var1, int var2) {
      return new TimeOfDay(var0, var1, var2);
   }

   public static TimeOfDay hourAndMinuteOfDay(int var0, int var1) {
      return new TimeOfDay(var0, var1);
   }

   public int getHour() {
      return this.hour;
   }

   public int getMinute() {
      return this.minute;
   }

   public int getSecond() {
      return this.second;
   }

   public boolean before(TimeOfDay var1) {
      if (var1.hour > this.hour) {
         return true;
      } else if (var1.hour < this.hour) {
         return false;
      } else if (var1.minute > this.minute) {
         return true;
      } else if (var1.minute < this.minute) {
         return false;
      } else if (var1.second > this.second) {
         return true;
      } else {
         return var1.second < this.second ? false : false;
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof TimeOfDay)) {
         return false;
      } else {
         TimeOfDay var2 = (TimeOfDay)var1;
         return var2.hour == this.hour && var2.minute == this.minute && var2.second == this.second;
      }
   }

   @Override
   public int hashCode() {
      return this.hour + 1 ^ this.minute + 1 ^ this.second + 1;
   }

   public Date getTimeOfDayForDate(Date var1) {
      if (var1 == null) {
         return null;
      } else {
         java.util.Calendar var2 = java.util.Calendar.getInstance();
         var2.setTime(var1);
         var2.set(11, this.hour);
         var2.set(12, this.minute);
         var2.set(13, this.second);
         var2.clear(14);
         return var2.getTime();
      }
   }

   public static TimeOfDay hourAndMinuteAndSecondFromDate(Date var0) {
      return hourAndMinuteAndSecondFromDate(var0, null);
   }

   public static TimeOfDay hourAndMinuteAndSecondFromDate(Date var0, TimeZone var1) {
      if (var0 == null) {
         return null;
      } else {
         java.util.Calendar var2 = java.util.Calendar.getInstance();
         var2.setTime(var0);
         if (var1 != null) {
            var2.setTimeZone(var1);
         }

         return new TimeOfDay(var2.get(11), var2.get(12), var2.get(13));
      }
   }

   public static TimeOfDay hourAndMinuteFromDate(Date var0) {
      return hourAndMinuteFromDate(var0, null);
   }

   public static TimeOfDay hourAndMinuteFromDate(Date var0, TimeZone var1) {
      if (var0 == null) {
         return null;
      } else {
         java.util.Calendar var2 = java.util.Calendar.getInstance();
         var2.setTime(var0);
         if (var1 != null) {
            var2.setTimeZone(var1);
         }

         return new TimeOfDay(var2.get(11), var2.get(12));
      }
   }

   @Override
   public String toString() {
      return "TimeOfDay[" + this.hour + ":" + this.minute + ":" + this.second + "]";
   }
}
