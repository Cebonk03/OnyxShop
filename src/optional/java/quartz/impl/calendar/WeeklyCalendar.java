package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.TimeZone;
import org.quartz.Calendar;

public class WeeklyCalendar extends BaseCalendar implements Calendar, Serializable {
   static final long serialVersionUID = -6809298821229007586L;
   private boolean[] excludeDays = new boolean[8];
   private boolean excludeAll = false;

   public WeeklyCalendar() {
      this(null, null);
   }

   public WeeklyCalendar(Calendar var1) {
      this(var1, null);
   }

   public WeeklyCalendar(TimeZone var1) {
      super(null, var1);
   }

   public WeeklyCalendar(Calendar var1, TimeZone var2) {
      super(var1, var2);
      this.excludeDays[1] = true;
      this.excludeDays[7] = true;
      this.excludeAll = this.areAllDaysExcluded();
   }

   @Override
   public Object clone() {
      WeeklyCalendar var1 = (WeeklyCalendar)super.clone();
      var1.excludeDays = (boolean[])this.excludeDays.clone();
      return var1;
   }

   public boolean[] getDaysExcluded() {
      return this.excludeDays;
   }

   public boolean isDayExcluded(int var1) {
      return this.excludeDays[var1];
   }

   public void setDaysExcluded(boolean[] var1) {
      if (var1 != null) {
         this.excludeDays = var1;
         this.excludeAll = this.areAllDaysExcluded();
      }
   }

   public void setDayExcluded(int var1, boolean var2) {
      this.excludeDays[var1] = var2;
      this.excludeAll = this.areAllDaysExcluded();
   }

   public boolean areAllDaysExcluded() {
      return this.isDayExcluded(1)
         && this.isDayExcluded(2)
         && this.isDayExcluded(3)
         && this.isDayExcluded(4)
         && this.isDayExcluded(5)
         && this.isDayExcluded(6)
         && this.isDayExcluded(7);
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (this.excludeAll) {
         return false;
      } else if (!super.isTimeIncluded(var1)) {
         return false;
      } else {
         java.util.Calendar var3 = this.createJavaCalendar(var1);
         int var4 = var3.get(7);
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
         int var6 = var5.get(7);
         if (!this.isDayExcluded(var6)) {
            return var1;
         } else {
            while (this.isDayExcluded(var6)) {
               var5.add(5, 1);
               var6 = var5.get(7);
            }

            return var5.getTime().getTime();
         }
      }
   }
}
