package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;
import org.quartz.Calendar;

public class BaseCalendar implements Calendar, Serializable, Cloneable {
   static final long serialVersionUID = 3106623404629760239L;
   private Calendar baseCalendar;
   private String description;
   private TimeZone timeZone;

   public BaseCalendar() {
   }

   public BaseCalendar(Calendar var1) {
      this.setBaseCalendar(var1);
   }

   public BaseCalendar(TimeZone var1) {
      this.setTimeZone(var1);
   }

   public BaseCalendar(Calendar var1, TimeZone var2) {
      this.setBaseCalendar(var1);
      this.setTimeZone(var2);
   }

   @Override
   public Object clone() {
      try {
         BaseCalendar var1 = (BaseCalendar)super.clone();
         if (this.getBaseCalendar() != null) {
            var1.baseCalendar = (Calendar)this.getBaseCalendar().clone();
         }

         if (this.getTimeZone() != null) {
            var1.timeZone = (TimeZone)this.getTimeZone().clone();
         }

         return var1;
      } catch (CloneNotSupportedException var2) {
         throw new IncompatibleClassChangeError("Not Cloneable.");
      }
   }

   @Override
   public void setBaseCalendar(Calendar var1) {
      this.baseCalendar = var1;
   }

   @Override
   public Calendar getBaseCalendar() {
      return this.baseCalendar;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   @Override
   public void setDescription(String var1) {
      this.description = var1;
   }

   public TimeZone getTimeZone() {
      return this.timeZone;
   }

   public void setTimeZone(TimeZone var1) {
      this.timeZone = var1;
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      if (var1 <= 0L) {
         throw new IllegalArgumentException("timeStamp must be greater 0");
      } else {
         return this.baseCalendar == null || this.baseCalendar.isTimeIncluded(var1);
      }
   }

   @Override
   public long getNextIncludedTime(long var1) {
      if (var1 <= 0L) {
         throw new IllegalArgumentException("timeStamp must be greater 0");
      } else {
         return this.baseCalendar != null ? this.baseCalendar.getNextIncludedTime(var1) : var1;
      }
   }

   protected java.util.Calendar createJavaCalendar(long var1) {
      java.util.Calendar var3 = this.createJavaCalendar();
      var3.setTime(new Date(var1));
      return var3;
   }

   protected java.util.Calendar createJavaCalendar() {
      return this.getTimeZone() == null ? java.util.Calendar.getInstance() : java.util.Calendar.getInstance(this.getTimeZone());
   }

   protected java.util.Calendar getStartOfDayJavaCalendar(long var1) {
      java.util.Calendar var3 = this.createJavaCalendar(var1);
      var3.set(11, 0);
      var3.set(12, 0);
      var3.set(13, 0);
      var3.set(14, 0);
      return var3;
   }

   protected java.util.Calendar getEndOfDayJavaCalendar(long var1) {
      java.util.Calendar var3 = this.createJavaCalendar(var1);
      var3.set(11, 23);
      var3.set(12, 59);
      var3.set(13, 59);
      var3.set(14, 999);
      return var3;
   }
}
