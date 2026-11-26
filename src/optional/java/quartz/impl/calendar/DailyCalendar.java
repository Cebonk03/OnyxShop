package org.quartz.impl.calendar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TimeZone;
import org.quartz.Calendar;

public class DailyCalendar extends BaseCalendar {
   static final long serialVersionUID = -7561220099904944039L;
   private static final String invalidHourOfDay = "Invalid hour of day: ";
   private static final String invalidMinute = "Invalid minute: ";
   private static final String invalidSecond = "Invalid second: ";
   private static final String invalidMillis = "Invalid millis: ";
   private static final String invalidTimeRange = "Invalid time range: ";
   private static final String separator = " - ";
   private static final long oneMillis = 1L;
   private static final String colon = ":";
   private int rangeStartingHourOfDay;
   private int rangeStartingMinute;
   private int rangeStartingSecond;
   private int rangeStartingMillis;
   private int rangeEndingHourOfDay;
   private int rangeEndingMinute;
   private int rangeEndingSecond;
   private int rangeEndingMillis;
   private boolean invertTimeRange = false;

   public DailyCalendar(String var1, String var2) {
      this.setTimeRange(var1, var2);
   }

   public DailyCalendar(Calendar var1, String var2, String var3) {
      super(var1);
      this.setTimeRange(var2, var3);
   }

   public DailyCalendar(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      this.setTimeRange(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public DailyCalendar(Calendar var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      super(var1);
      this.setTimeRange(var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public DailyCalendar(java.util.Calendar var1, java.util.Calendar var2) {
      this.setTimeRange(var1, var2);
   }

   public DailyCalendar(Calendar var1, java.util.Calendar var2, java.util.Calendar var3) {
      super(var1);
      this.setTimeRange(var2, var3);
   }

   public DailyCalendar(long var1, long var3) {
      this.setTimeRange(var1, var3);
   }

   public DailyCalendar(Calendar var1, long var2, long var4) {
      super(var1);
      this.setTimeRange(var2, var4);
   }

   public DailyCalendar(TimeZone var1, long var2, long var4) {
      super(var1);
      this.setTimeRange(var2, var4);
   }

   public DailyCalendar(Calendar var1, TimeZone var2, long var3, long var5) {
      super(var1, var2);
      this.setTimeRange(var3, var5);
   }

   @Override
   public Object clone() {
      return (DailyCalendar)super.clone();
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (this.getBaseCalendar() != null && !this.getBaseCalendar().isTimeIncluded(var1)) {
         return false;
      } else {
         long var3 = this.getStartOfDayJavaCalendar(var1).getTime().getTime();
         long var5 = this.getEndOfDayJavaCalendar(var1).getTime().getTime();
         long var7 = this.getTimeRangeStartingTimeInMillis(var1);
         long var9 = this.getTimeRangeEndingTimeInMillis(var1);
         return !this.invertTimeRange ? var1 > var3 && var1 < var7 || var1 > var9 && var1 < var5 : var1 >= var7 && var1 <= var9;
      }
   }

   @Override
   public long getNextIncludedTime(long var1) {
      long var5 = var1 + 1L;

      while (!this.isTimeIncluded(var5)) {
         if (!this.invertTimeRange) {
            if (var5 >= this.getTimeRangeStartingTimeInMillis(var5) && var5 <= this.getTimeRangeEndingTimeInMillis(var5)) {
               var5 = this.getTimeRangeEndingTimeInMillis(var5) + 1L;
            } else if (this.getBaseCalendar() != null && !this.getBaseCalendar().isTimeIncluded(var5)) {
               var5 = this.getBaseCalendar().getNextIncludedTime(var5);
            } else {
               var5++;
            }
         } else if (var5 < this.getTimeRangeStartingTimeInMillis(var5)) {
            var5 = this.getTimeRangeStartingTimeInMillis(var5);
         } else if (var5 > this.getTimeRangeEndingTimeInMillis(var5)) {
            var5 = this.getEndOfDayJavaCalendar(var5).getTime().getTime();
            var5++;
         } else if (this.getBaseCalendar() != null && !this.getBaseCalendar().isTimeIncluded(var5)) {
            var5 = this.getBaseCalendar().getNextIncludedTime(var5);
         } else {
            var5++;
         }
      }

      return var5;
   }

   public long getTimeRangeStartingTimeInMillis(long var1) {
      java.util.Calendar var3 = this.createJavaCalendar(var1);
      var3.set(11, this.rangeStartingHourOfDay);
      var3.set(12, this.rangeStartingMinute);
      var3.set(13, this.rangeStartingSecond);
      var3.set(14, this.rangeStartingMillis);
      return var3.getTime().getTime();
   }

   public long getTimeRangeEndingTimeInMillis(long var1) {
      java.util.Calendar var3 = this.createJavaCalendar(var1);
      var3.set(11, this.rangeEndingHourOfDay);
      var3.set(12, this.rangeEndingMinute);
      var3.set(13, this.rangeEndingSecond);
      var3.set(14, this.rangeEndingMillis);
      return var3.getTime().getTime();
   }

   public boolean getInvertTimeRange() {
      return this.invertTimeRange;
   }

   public void setInvertTimeRange(boolean var1) {
      this.invertTimeRange = var1;
   }

   @Override
   public String toString() {
      NumberFormat var1 = NumberFormat.getNumberInstance();
      var1.setMaximumFractionDigits(0);
      var1.setMinimumIntegerDigits(2);
      StringBuffer var2 = new StringBuffer();
      var2.append("base calendar: [");
      if (this.getBaseCalendar() != null) {
         var2.append(this.getBaseCalendar().toString());
      } else {
         var2.append("null");
      }

      var2.append("], time range: '");
      var2.append(var1.format((long)this.rangeStartingHourOfDay));
      var2.append(":");
      var2.append(var1.format((long)this.rangeStartingMinute));
      var2.append(":");
      var2.append(var1.format((long)this.rangeStartingSecond));
      var2.append(":");
      var1.setMinimumIntegerDigits(3);
      var2.append(var1.format((long)this.rangeStartingMillis));
      var1.setMinimumIntegerDigits(2);
      var2.append(" - ");
      var2.append(var1.format((long)this.rangeEndingHourOfDay));
      var2.append(":");
      var2.append(var1.format((long)this.rangeEndingMinute));
      var2.append(":");
      var2.append(var1.format((long)this.rangeEndingSecond));
      var2.append(":");
      var1.setMinimumIntegerDigits(3);
      var2.append(var1.format((long)this.rangeEndingMillis));
      var2.append("', inverted: " + this.invertTimeRange + "]");
      return var2.toString();
   }

   private String[] split(String var1, String var2) {
      ArrayList<String> var3 = new ArrayList();
      StringTokenizer var4 = new StringTokenizer(var1, var2);

      while (var4.hasMoreTokens()) {
         var3.add(var4.nextToken());
      }

      return var3.toArray(new String[var3.size()]);
   }

   public void setTimeRange(String var1, String var2) {
      String[] var3 = this.split(var1, ":");
      if (var3.length >= 2 && var3.length <= 4) {
         int var4 = Integer.parseInt(var3[0]);
         int var5 = Integer.parseInt(var3[1]);
         int var6;
         if (var3.length > 2) {
            var6 = Integer.parseInt(var3[2]);
         } else {
            var6 = 0;
         }

         int var7;
         if (var3.length == 4) {
            var7 = Integer.parseInt(var3[3]);
         } else {
            var7 = 0;
         }

         String[] var8 = this.split(var2, ":");
         if (var8.length >= 2 && var8.length <= 4) {
            int var9 = Integer.parseInt(var8[0]);
            int var10 = Integer.parseInt(var8[1]);
            int var11;
            if (var8.length > 2) {
               var11 = Integer.parseInt(var8[2]);
            } else {
               var11 = 0;
            }

            int var12;
            if (var8.length == 4) {
               var12 = Integer.parseInt(var8[3]);
            } else {
               var12 = 0;
            }

            this.setTimeRange(var4, var5, var6, var7, var9, var10, var11, var12);
         } else {
            throw new IllegalArgumentException("Invalid time string '" + var2 + "'");
         }
      } else {
         throw new IllegalArgumentException("Invalid time string '" + var1 + "'");
      }
   }

   public void setTimeRange(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      this.validate(var1, var2, var3, var4);
      this.validate(var5, var6, var7, var8);
      java.util.Calendar var9 = this.createJavaCalendar();
      var9.set(11, var1);
      var9.set(12, var2);
      var9.set(13, var3);
      var9.set(14, var4);
      java.util.Calendar var10 = this.createJavaCalendar();
      var10.set(11, var5);
      var10.set(12, var6);
      var10.set(13, var7);
      var10.set(14, var8);
      if (!var9.before(var10)) {
         throw new IllegalArgumentException(
            "Invalid time range: " + var1 + ":" + var2 + ":" + var3 + ":" + var4 + " - " + var5 + ":" + var6 + ":" + var7 + ":" + var8
         );
      } else {
         this.rangeStartingHourOfDay = var1;
         this.rangeStartingMinute = var2;
         this.rangeStartingSecond = var3;
         this.rangeStartingMillis = var4;
         this.rangeEndingHourOfDay = var5;
         this.rangeEndingMinute = var6;
         this.rangeEndingSecond = var7;
         this.rangeEndingMillis = var8;
      }
   }

   public void setTimeRange(java.util.Calendar var1, java.util.Calendar var2) {
      this.setTimeRange(var1.get(11), var1.get(12), var1.get(13), var1.get(14), var2.get(11), var2.get(12), var2.get(13), var2.get(14));
   }

   public void setTimeRange(long var1, long var3) {
      this.setTimeRange(this.createJavaCalendar(var1), this.createJavaCalendar(var3));
   }

   private void validate(int var1, int var2, int var3, int var4) {
      if (var1 < 0 || var1 > 23) {
         throw new IllegalArgumentException("Invalid hour of day: " + var1);
      } else if (var2 < 0 || var2 > 59) {
         throw new IllegalArgumentException("Invalid minute: " + var2);
      } else if (var3 < 0 || var3 > 59) {
         throw new IllegalArgumentException("Invalid second: " + var3);
      } else if (var4 < 0 || var4 > 999) {
         throw new IllegalArgumentException("Invalid millis: " + var4);
      }
   }
}
