package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;
import org.quartz.Calendar;

public class AnnualCalendar extends BaseCalendar implements Calendar, Serializable {
   static final long serialVersionUID = 7346867105876610961L;
   private ArrayList<java.util.Calendar> excludeDays = new ArrayList<>();
   private boolean dataSorted = false;

   public AnnualCalendar() {
   }

   public AnnualCalendar(Calendar var1) {
      super(var1);
   }

   public AnnualCalendar(TimeZone var1) {
      super(var1);
   }

   public AnnualCalendar(Calendar var1, TimeZone var2) {
      super(var1, var2);
   }

   @Override
   public Object clone() {
      AnnualCalendar var1 = (AnnualCalendar)super.clone();
      var1.excludeDays = new ArrayList<>(this.excludeDays);
      return var1;
   }

   public ArrayList<java.util.Calendar> getDaysExcluded() {
      return this.excludeDays;
   }

   public boolean isDayExcluded(java.util.Calendar var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Parameter day must not be null");
      } else if (!super.isTimeIncluded(var1.getTime().getTime())) {
         return true;
      } else {
         int var2 = var1.get(2);
         int var3 = var1.get(5);
         if (!this.dataSorted) {
            Collections.sort(this.excludeDays, new CalendarComparator());
            this.dataSorted = true;
         }

         for (java.util.Calendar var5 : this.excludeDays) {
            if (var2 < var5.get(2)) {
               return false;
            }

            if (var3 == var5.get(5) && var2 == var5.get(2)) {
               return true;
            }
         }

         return false;
      }
   }

   public void setDaysExcluded(ArrayList<java.util.Calendar> var1) {
      if (var1 == null) {
         this.excludeDays = new ArrayList<>();
      } else {
         this.excludeDays = var1;
      }

      this.dataSorted = false;
   }

   public void setDayExcluded(java.util.Calendar var1, boolean var2) {
      if (var2) {
         if (this.isDayExcluded(var1)) {
            return;
         }

         this.excludeDays.add(var1);
         this.dataSorted = false;
      } else {
         if (!this.isDayExcluded(var1)) {
            return;
         }

         this.removeExcludedDay(var1, true);
      }
   }

   public void removeExcludedDay(java.util.Calendar var1) {
      this.removeExcludedDay(var1, false);
   }

   private void removeExcludedDay(java.util.Calendar var1, boolean var2) {
      if (var2 || this.isDayExcluded(var1)) {
         if (!this.excludeDays.remove(var1)) {
            int var3 = var1.get(2);
            int var4 = var1.get(5);

            for (java.util.Calendar var6 : this.excludeDays) {
               if (var3 == var6.get(2) && var4 == var6.get(5)) {
                  var1 = var6;
                  break;
               }
            }

            this.excludeDays.remove(var1);
         }
      }
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (!super.isTimeIncluded(var1)) {
         return false;
      } else {
         java.util.Calendar var3 = this.createJavaCalendar(var1);
         return !this.isDayExcluded(var3);
      }
   }

   @Override
   public long getNextIncludedTime(long var1) {
      long var3 = super.getNextIncludedTime(var1);
      if (var3 > 0L && var3 > var1) {
         var1 = var3;
      }

      java.util.Calendar var5 = this.getStartOfDayJavaCalendar(var1);
      if (!this.isDayExcluded(var5)) {
         return var1;
      } else {
         while (this.isDayExcluded(var5)) {
            var5.add(5, 1);
         }

         return var5.getTime().getTime();
      }
   }
}
