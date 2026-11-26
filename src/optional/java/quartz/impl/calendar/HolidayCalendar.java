package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.quartz.Calendar;

public class HolidayCalendar extends BaseCalendar implements Calendar, Serializable {
   static final long serialVersionUID = -7590908752291814693L;
   private TreeSet<Date> dates = new TreeSet<>();

   public HolidayCalendar() {
   }

   public HolidayCalendar(Calendar var1) {
      super(var1);
   }

   public HolidayCalendar(TimeZone var1) {
      super(var1);
   }

   public HolidayCalendar(Calendar var1, TimeZone var2) {
      super(var1, var2);
   }

   @Override
   public Object clone() {
      HolidayCalendar var1 = (HolidayCalendar)super.clone();
      var1.dates = new TreeSet<>(this.dates);
      return var1;
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (!super.isTimeIncluded(var1)) {
         return false;
      } else {
         Date var3 = this.getStartOfDayJavaCalendar(var1).getTime();
         return !this.dates.contains(var3);
      }
   }

   @Override
   public long getNextIncludedTime(long var1) {
      long var3 = super.getNextIncludedTime(var1);
      if (var3 > 0L && var3 > var1) {
         var1 = var3;
      }

      java.util.Calendar var5 = this.getStartOfDayJavaCalendar(var1);

      while (!this.isTimeIncluded(var5.getTime().getTime())) {
         var5.add(5, 1);
      }

      return var5.getTime().getTime();
   }

   public void addExcludedDate(Date var1) {
      Date var2 = this.getStartOfDayJavaCalendar(var1.getTime()).getTime();
      this.dates.add(var2);
   }

   public void removeExcludedDate(Date var1) {
      Date var2 = this.getStartOfDayJavaCalendar(var1.getTime()).getTime();
      this.dates.remove(var2);
   }

   public SortedSet<Date> getExcludedDates() {
      return Collections.unmodifiableSortedSet(this.dates);
   }
}
