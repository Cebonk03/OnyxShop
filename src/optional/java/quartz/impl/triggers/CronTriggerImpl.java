package org.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.ScheduleBuilder;

public class CronTriggerImpl extends AbstractTrigger<CronTrigger> implements CronTrigger, CoreTrigger {
   private static final long serialVersionUID = -8644953146451592766L;
   protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;
   private CronExpression cronEx = null;
   private Date startTime = null;
   private Date endTime = null;
   private Date nextFireTime = null;
   private Date previousFireTime = null;
   private transient TimeZone timeZone = null;

   public CronTriggerImpl() {
      this.setStartTime(new Date());
      this.setTimeZone(TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1) {
      this(var1, null);
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2) {
      super(var1, var2);
      this.setStartTime(new Date());
      this.setTimeZone(TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3) {
      super(var1, var2);
      this.setCronExpression(var3);
      this.setStartTime(new Date());
      this.setTimeZone(TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      this.setStartTime(new Date());
      this.setTimeZone(TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3, String var4, String var5) {
      this(var1, var2, var3, var4, null, null, var5, TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3, String var4, String var5, TimeZone var6) {
      this(var1, var2, var3, var4, null, null, var5, var6);
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3, String var4, Date var5, Date var6, String var7) {
      super(var1, var2, var3, var4);
      this.setCronExpression(var7);
      if (var5 == null) {
         var5 = new Date();
      }

      this.setStartTime(var5);
      if (var6 != null) {
         this.setEndTime(var6);
      }

      this.setTimeZone(TimeZone.getDefault());
   }

   @Deprecated
   public CronTriggerImpl(String var1, String var2, String var3, String var4, Date var5, Date var6, String var7, TimeZone var8) {
      super(var1, var2, var3, var4);
      this.setCronExpression(var7);
      if (var5 == null) {
         var5 = new Date();
      }

      this.setStartTime(var5);
      if (var6 != null) {
         this.setEndTime(var6);
      }

      if (var8 == null) {
         this.setTimeZone(TimeZone.getDefault());
      } else {
         this.setTimeZone(var8);
      }
   }

   @Override
   public Object clone() {
      CronTriggerImpl var1 = (CronTriggerImpl)super.clone();
      if (this.cronEx != null) {
         var1.setCronExpression(new CronExpression(this.cronEx));
      }

      return var1;
   }

   public void setCronExpression(String var1) {
      TimeZone var2 = this.getTimeZone();
      this.cronEx = new CronExpression(var1);
      this.cronEx.setTimeZone(var2);
   }

   @Override
   public String getCronExpression() {
      return this.cronEx == null ? null : this.cronEx.getCronExpression();
   }

   public void setCronExpression(CronExpression var1) {
      this.cronEx = var1;
      this.timeZone = var1.getTimeZone();
   }

   @Override
   public Date getStartTime() {
      return this.startTime;
   }

   @Override
   public void setStartTime(Date var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Start time cannot be null");
      } else {
         Date var2 = this.getEndTime();
         if (var2 != null && var2.before(var1)) {
            throw new IllegalArgumentException("End time cannot be before start time");
         } else {
            Calendar var3 = Calendar.getInstance();
            var3.setTime(var1);
            var3.set(14, 0);
            this.startTime = var3.getTime();
         }
      }
   }

   @Override
   public Date getEndTime() {
      return this.endTime;
   }

   @Override
   public void setEndTime(Date var1) {
      Date var2 = this.getStartTime();
      if (var2 != null && var1 != null && var2.after(var1)) {
         throw new IllegalArgumentException("End time cannot be before start time");
      } else {
         this.endTime = var1;
      }
   }

   @Override
   public Date getNextFireTime() {
      return this.nextFireTime;
   }

   @Override
   public Date getPreviousFireTime() {
      return this.previousFireTime;
   }

   @Override
   public void setNextFireTime(Date var1) {
      this.nextFireTime = var1;
   }

   @Override
   public void setPreviousFireTime(Date var1) {
      this.previousFireTime = var1;
   }

   @Override
   public TimeZone getTimeZone() {
      if (this.cronEx != null) {
         return this.cronEx.getTimeZone();
      } else {
         if (this.timeZone == null) {
            this.timeZone = TimeZone.getDefault();
         }

         return this.timeZone;
      }
   }

   public void setTimeZone(TimeZone var1) {
      if (this.cronEx != null) {
         this.cronEx.setTimeZone(var1);
      }

      this.timeZone = var1;
   }

   @Override
   public Date getFireTimeAfter(Date var1) {
      if (var1 == null) {
         var1 = new Date();
      }

      if (this.getStartTime().after(var1)) {
         var1 = new Date(this.getStartTime().getTime() - 1000L);
      }

      if (this.getEndTime() != null && var1.compareTo(this.getEndTime()) >= 0) {
         return null;
      } else {
         Date var2 = this.getTimeAfter(var1);
         return this.getEndTime() != null && var2 != null && var2.after(this.getEndTime()) ? null : var2;
      }
   }

   @Override
   public Date getFinalFireTime() {
      Date var1;
      if (this.getEndTime() != null) {
         var1 = this.getTimeBefore(new Date(this.getEndTime().getTime() + 1000L));
      } else {
         var1 = this.cronEx == null ? null : this.cronEx.getFinalFireTime();
      }

      return var1 != null && this.getStartTime() != null && var1.before(this.getStartTime()) ? null : var1;
   }

   @Override
   public boolean mayFireAgain() {
      return this.getNextFireTime() != null;
   }

   @Override
   protected boolean validateMisfireInstruction(int var1) {
      return var1 >= -1 && var1 <= 2;
   }

   @Override
   public void updateAfterMisfire(org.quartz.Calendar var1) {
      int var2 = this.getMisfireInstruction();
      if (var2 != -1) {
         if (var2 == 0) {
            var2 = 1;
         }

         if (var2 == 2) {
            Date var3 = this.getFireTimeAfter(new Date());

            while (var3 != null && var1 != null && !var1.isTimeIncluded(var3.getTime())) {
               var3 = this.getFireTimeAfter(var3);
            }

            this.setNextFireTime(var3);
         } else if (var2 == 1) {
            this.setNextFireTime(new Date());
         }
      }
   }

   public boolean willFireOn(Calendar var1) {
      return this.willFireOn(var1, false);
   }

   public boolean willFireOn(Calendar var1, boolean var2) {
      var1 = (Calendar)var1.clone();
      var1.set(14, 0);
      if (var2) {
         var1.set(11, 0);
         var1.set(12, 0);
         var1.set(13, 0);
      }

      Date var3 = var1.getTime();
      Date var4 = this.getFireTimeAfter(new Date(var1.getTime().getTime() - 1000L));
      if (var4 == null) {
         return false;
      } else {
         Calendar var5 = Calendar.getInstance(var1.getTimeZone());
         var5.setTime(var4);
         int var6 = var5.get(1);
         int var7 = var5.get(2);
         int var8 = var5.get(5);
         if (var2) {
            return var6 == var1.get(1) && var7 == var1.get(2) && var8 == var1.get(5);
         } else {
            while (var4.before(var3)) {
               var4 = this.getFireTimeAfter(var4);
            }

            return var4.equals(var3);
         }
      }
   }

   @Override
   public void triggered(org.quartz.Calendar var1) {
      this.previousFireTime = this.nextFireTime;
      this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
      }
   }

   @Override
   public void updateWithNewCalendar(org.quartz.Calendar var1, long var2) {
      this.nextFireTime = this.getFireTimeAfter(this.previousFireTime);
      if (this.nextFireTime != null && var1 != null) {
         Date var4 = new Date();

         while (this.nextFireTime != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
            this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
            if (this.nextFireTime == null) {
               break;
            }

            GregorianCalendar var5 = new GregorianCalendar();
            var5.setTime(this.nextFireTime);
            if (var5.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
               this.nextFireTime = null;
            }

            if (this.nextFireTime != null && this.nextFireTime.before(var4)) {
               long var6 = var4.getTime() - this.nextFireTime.getTime();
               if (var6 >= var2) {
                  this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
               }
            }
         }
      }
   }

   @Override
   public Date computeFirstFireTime(org.quartz.Calendar var1) {
      this.nextFireTime = this.getFireTimeAfter(new Date(this.getStartTime().getTime() - 1000L));

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
      }

      return this.nextFireTime;
   }

   @Override
   public String getExpressionSummary() {
      return this.cronEx == null ? null : this.cronEx.getExpressionSummary();
   }

   @Override
   public boolean hasAdditionalProperties() {
      return false;
   }

   @Override
   public ScheduleBuilder<CronTrigger> getScheduleBuilder() {
      CronScheduleBuilder var1 = CronScheduleBuilder.cronSchedule(this.getCronExpression()).inTimeZone(this.getTimeZone());
      switch (this.getMisfireInstruction()) {
         case 1:
            var1.withMisfireHandlingInstructionFireAndProceed();
            break;
         case 2:
            var1.withMisfireHandlingInstructionDoNothing();
      }

      return var1;
   }

   protected Date getTimeAfter(Date var1) {
      return this.cronEx == null ? null : this.cronEx.getTimeAfter(var1);
   }

   protected Date getTimeBefore(Date var1) {
      return this.cronEx == null ? null : this.cronEx.getTimeBefore(var1);
   }
}
