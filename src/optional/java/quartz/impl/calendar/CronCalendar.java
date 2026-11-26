package org.quartz.impl.calendar;

import java.util.Date;
import java.util.TimeZone;
import org.quartz.Calendar;
import org.quartz.CronExpression;

public class CronCalendar extends BaseCalendar {
   static final long serialVersionUID = -8172103999750856831L;
   CronExpression cronExpression;

   public CronCalendar(String var1) {
      this(null, var1, null);
   }

   public CronCalendar(Calendar var1, String var2) {
      this(var1, var2, null);
   }

   public CronCalendar(Calendar var1, String var2, TimeZone var3) {
      super(var1);
      this.cronExpression = new CronExpression(var2);
      this.cronExpression.setTimeZone(var3);
   }

   @Override
   public Object clone() {
      CronCalendar var1 = (CronCalendar)super.clone();
      var1.cronExpression = new CronExpression(this.cronExpression);
      return var1;
   }

   @Override
   public TimeZone getTimeZone() {
      return this.cronExpression.getTimeZone();
   }

   @Override
   public void setTimeZone(TimeZone var1) {
      this.cronExpression.setTimeZone(var1);
   }

   @Override
   public boolean isTimeIncluded(long var1) {
      return this.getBaseCalendar() != null && !this.getBaseCalendar().isTimeIncluded(var1) ? false : !this.cronExpression.isSatisfiedBy(new Date(var1));
   }

   @Override
   public long getNextIncludedTime(long var1) {
      long var3 = var1 + 1L;

      while (!this.isTimeIncluded(var3)) {
         if (this.cronExpression.isSatisfiedBy(new Date(var3))) {
            var3 = this.cronExpression.getNextInvalidTimeAfter(new Date(var3)).getTime();
         } else if (this.getBaseCalendar() != null && !this.getBaseCalendar().isTimeIncluded(var3)) {
            var3 = this.getBaseCalendar().getNextIncludedTime(var3);
         } else {
            var3++;
         }
      }

      return var3;
   }

   @Override
   public String toString() {
      StringBuffer var1 = new StringBuffer();
      var1.append("base calendar: [");
      if (this.getBaseCalendar() != null) {
         var1.append(this.getBaseCalendar().toString());
      } else {
         var1.append("null");
      }

      var1.append("], excluded cron expression: '");
      var1.append(this.cronExpression);
      var1.append("'");
      return var1.toString();
   }

   public CronExpression getCronExpression() {
      return this.cronExpression;
   }

   public void setCronExpression(String var1) {
      CronExpression var2 = new CronExpression(var1);
      this.cronExpression = var2;
   }

   public void setCronExpression(CronExpression var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("expression cannot be null");
      } else {
         this.cronExpression = var1;
      }
   }
}
