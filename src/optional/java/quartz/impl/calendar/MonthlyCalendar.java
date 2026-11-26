package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.TimeZone;
import org.quartz.Calendar;

public class MonthlyCalendar extends BaseCalendar implements Calendar, Serializable {
   static final long serialVersionUID = 419164961091807944L;
   private static final int MAX_DAYS_IN_MONTH = 31;
   private boolean[] excludeDays = new boolean[31];
   private boolean excludeAll = false;

   public MonthlyCalendar() {
      this(null, null);
   }

   public MonthlyCalendar(Calendar var1) {
      this(var1, null);
   }

   public MonthlyCalendar(TimeZone var1) {
      this(null, var1);
   }

   public MonthlyCalendar(Calendar var1, TimeZone var2) {
      super(var1, var2);
      this.excludeAll = this.areAllDaysExcluded();
   }

   @Override
   public Object clone() {
      MonthlyCalendar var1 = (MonthlyCalendar)super.clone();
      var1.excludeDays = (boolean[])this.excludeDays.clone();
      return var1;
   }

   public boolean[] getDaysExcluded() {
      return this.excludeDays;
   }

   public boolean isDayExcluded(int var1) {
      if (var1 >= 1 && var1 <= 31) {
         return this.excludeDays[var1 - 1];
      } else {
         throw new IllegalArgumentException("The day parameter must be in the range of 1 to 31");
      }
   }

   public void setDaysExcluded(boolean[] var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("The days parameter cannot be null.");
      } else if (var1.length < 31) {
         throw new IllegalArgumentException("The days parameter must have a length of at least 31 elements.");
      } else {
         this.excludeDays = var1;
         this.excludeAll = this.areAllDaysExcluded();
      }
   }

   public void setDayExcluded(int var1, boolean var2) {
      if (var1 >= 1 && var1 <= 31) {
         this.excludeDays[var1 - 1] = var2;
         this.excludeAll = this.areAllDaysExcluded();
      } else {
         throw new IllegalArgumentException("The day parameter must be in the range of 1 to 31");
      }
   }

   public boolean areAllDaysExcluded() {
      for (int var1 = 1; var1 <= 31; var1++) {
         if (!this.isDayExcluded(var1)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (this.excludeAll) {
         return false;
      } else if (!super.isTimeIncluded(var1)) {
         return false;
      } else {
         java.util.Calendar var3 = this.createJavaCalendar(var1);
         int var4 = var3.get(5);
         return !this.isDayExcluded(var4);
      }
   }

   @Override
   public long getNextIncludedTime(long var1) {
      if (this.excludeAll) {
         return 0L;
      } else {
         long var3 = super.getNextIncludedTime(var1);
         if (var3 > 0L && var3 > var1) {
            var1 = var3;
         }

         java.util.Calendar var5 = this.getStartOfDayJavaCalendar(var1);
         int var6 = var5.get(5);
         if (!this.isDayExcluded(var6)) {
            return var1;
         } else {
            while (this.isDayExcluded(var6)) {
               var5.add(5, 1);
               var6 = var5.get(5);
            }

            return var5.getTime().getTime();
         }
      }
   }
}
