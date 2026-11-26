package org.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;

public class CalendarIntervalTriggerImpl extends AbstractTrigger<CalendarIntervalTrigger> implements CalendarIntervalTrigger, CoreTrigger {
   private static final long serialVersionUID = -2635982274232850343L;
   private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = Calendar.getInstance().get(1) + 100;
   private Date startTime = null;
   private Date endTime = null;
   private Date nextFireTime = null;
   private Date previousFireTime = null;
   private int repeatInterval = 0;
   private DateBuilder.IntervalUnit repeatIntervalUnit = DateBuilder.IntervalUnit.DAY;
   private TimeZone timeZone;
   private boolean preserveHourOfDayAcrossDaylightSavings = false;
   private boolean skipDayIfHourDoesNotExist = false;
   private int timesTriggered = 0;
   private boolean complete = false;

   public CalendarIntervalTriggerImpl() {
   }

   public CalendarIntervalTriggerImpl(String var1, DateBuilder.IntervalUnit var2, int var3) {
      this(var1, null, var2, var3);
   }

   public CalendarIntervalTriggerImpl(String var1, String var2, DateBuilder.IntervalUnit var3, int var4) {
      this(var1, var2, new Date(), null, var3, var4);
   }

   public CalendarIntervalTriggerImpl(String var1, Date var2, Date var3, DateBuilder.IntervalUnit var4, int var5) {
      this(var1, null, var2, var3, var4, var5);
   }

   public CalendarIntervalTriggerImpl(String var1, String var2, Date var3, Date var4, DateBuilder.IntervalUnit var5, int var6) {
      super(var1, var2);
      this.setStartTime(var3);
      this.setEndTime(var4);
      this.setRepeatIntervalUnit(var5);
      this.setRepeatInterval(var6);
   }

   public CalendarIntervalTriggerImpl(String var1, String var2, String var3, String var4, Date var5, Date var6, DateBuilder.IntervalUnit var7, int var8) {
      super(var1, var2, var3, var4);
      this.setStartTime(var5);
      this.setEndTime(var6);
      this.setRepeatIntervalUnit(var7);
      this.setRepeatInterval(var8);
   }

   @Override
   public Date getStartTime() {
      if (this.startTime == null) {
         this.startTime = new Date();
      }

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
            this.startTime = var1;
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
   public DateBuilder.IntervalUnit getRepeatIntervalUnit() {
      return this.repeatIntervalUnit;
   }

   public void setRepeatIntervalUnit(DateBuilder.IntervalUnit var1) {
      this.repeatIntervalUnit = var1;
   }

   @Override
   public int getRepeatInterval() {
      return this.repeatInterval;
   }

   public void setRepeatInterval(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("Repeat interval must be >= 1");
      } else {
         this.repeatInterval = var1;
      }
   }

   @Override
   public TimeZone getTimeZone() {
      if (this.timeZone == null) {
         this.timeZone = TimeZone.getDefault();
      }

      return this.timeZone;
   }

   public void setTimeZone(TimeZone var1) {
      this.timeZone = var1;
   }

   @Override
   public boolean isPreserveHourOfDayAcrossDaylightSavings() {
      return this.preserveHourOfDayAcrossDaylightSavings;
   }

   public void setPreserveHourOfDayAcrossDaylightSavings(boolean var1) {
      this.preserveHourOfDayAcrossDaylightSavings = var1;
   }

   @Override
   public boolean isSkipDayIfHourDoesNotExist() {
      return this.skipDayIfHourDoesNotExist;
   }

   public void setSkipDayIfHourDoesNotExist(boolean var1) {
      this.skipDayIfHourDoesNotExist = var1;
   }

   @Override
   public int getTimesTriggered() {
      return this.timesTriggered;
   }

   public void setTimesTriggered(int var1) {
      this.timesTriggered = var1;
   }

   @Override
   protected boolean validateMisfireInstruction(int var1) {
      return var1 < -1 ? false : var1 <= 2;
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

   @Override
   public void triggered(org.quartz.Calendar var1) {
      this.timesTriggered++;
      this.previousFireTime = this.nextFireTime;
      this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
         if (this.nextFireTime == null) {
            break;
         }

         Calendar var2 = Calendar.getInstance();
         var2.setTime(this.nextFireTime);
         if (var2.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
            this.nextFireTime = null;
         }
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

            Calendar var5 = Calendar.getInstance();
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
      this.nextFireTime = this.getStartTime();

      while (this.nextFireTime != null && var1 != null && !var1.isTimeIncluded(this.nextFireTime.getTime())) {
         this.nextFireTime = this.getFireTimeAfter(this.nextFireTime);
         if (this.nextFireTime == null) {
            break;
         }

         Calendar var2 = Calendar.getInstance();
         var2.setTime(this.nextFireTime);
         if (var2.get(1) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
            return null;
         }
      }

      return this.nextFireTime;
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
   public Date getFireTimeAfter(Date var1) {
      return this.getFireTimeAfter(var1, false);
   }

   protected Date getFireTimeAfter(Date var1, boolean var2) {
      if (this.complete) {
         return null;
      } else {
         if (var1 == null) {
            var1 = new Date();
         }

         long var3 = this.getStartTime().getTime();
         long var5 = var1.getTime();
         long var7 = this.getEndTime() == null ? Long.MAX_VALUE : this.getEndTime().getTime();
         if (!var2 && var7 <= var5) {
            return null;
         } else if (var5 < var3) {
            return new Date(var3);
         } else {
            long var9 = 1L + (var5 - var3) / 1000L;
            Date var11 = null;
            long var12 = this.getRepeatInterval();
            Calendar var14 = Calendar.getInstance();
            var14.setTime(var1);
            Calendar var15 = Calendar.getInstance();
            if (this.timeZone != null) {
               var15.setTimeZone(this.timeZone);
            }

            var15.setTime(this.getStartTime());
            var15.setLenient(true);
            if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.SECOND)) {
               long var16 = var9 / var12;
               if (var9 % var12 != 0L) {
                  var16++;
               }

               var15.add(13, this.getRepeatInterval() * (int)var16);
               var11 = var15.getTime();
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.MINUTE)) {
               long var19 = var9 / (var12 * 60L);
               if (var9 % (var12 * 60L) != 0L) {
                  var19++;
               }

               var15.add(12, this.getRepeatInterval() * (int)var19);
               var11 = var15.getTime();
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.HOUR)) {
               long var20 = var9 / (var12 * 60L * 60L);
               if (var9 % (var12 * 60L * 60L) != 0L) {
                  var20++;
               }

               var15.add(11, this.getRepeatInterval() * (int)var20);
               var11 = var15.getTime();
            } else {
               int var21 = var15.get(11);
               if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.DAY)) {
                  var15.setLenient(true);
                  long var17 = var9 / (var12 * 24L * 60L * 60L);
                  if (var17 > 20L) {
                     if (var17 < 50L) {
                        var17 = (long)(var17 * 0.8);
                     } else if (var17 < 500L) {
                        var17 = (long)(var17 * 0.9);
                     } else {
                        var17 = (long)(var17 * 0.95);
                     }

                     var15.add(6, (int)(this.getRepeatInterval() * var17));
                  }

                  while (!var15.getTime().after(var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(6, this.getRepeatInterval());
                  }

                  while (this.daylightSavingHourShiftOccurredAndAdvanceNeeded(var15, var21, var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(6, this.getRepeatInterval());
                  }

                  var11 = var15.getTime();
               } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.WEEK)) {
                  var15.setLenient(true);
                  long var23 = var9 / (var12 * 7L * 24L * 60L * 60L);
                  if (var23 > 20L) {
                     if (var23 < 50L) {
                        var23 = (long)(var23 * 0.8);
                     } else if (var23 < 500L) {
                        var23 = (long)(var23 * 0.9);
                     } else {
                        var23 = (long)(var23 * 0.95);
                     }

                     var15.add(3, (int)(this.getRepeatInterval() * var23));
                  }

                  while (!var15.getTime().after(var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(3, this.getRepeatInterval());
                  }

                  while (this.daylightSavingHourShiftOccurredAndAdvanceNeeded(var15, var21, var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(3, this.getRepeatInterval());
                  }

                  var11 = var15.getTime();
               } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.MONTH)) {
                  var15.setLenient(true);

                  while (!var15.getTime().after(var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(2, this.getRepeatInterval());
                  }

                  while (this.daylightSavingHourShiftOccurredAndAdvanceNeeded(var15, var21, var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(2, this.getRepeatInterval());
                  }

                  var11 = var15.getTime();
               } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.YEAR)) {
                  while (!var15.getTime().after(var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(1, this.getRepeatInterval());
                  }

                  while (this.daylightSavingHourShiftOccurredAndAdvanceNeeded(var15, var21, var1) && var15.get(1) < YEAR_TO_GIVEUP_SCHEDULING_AT) {
                     var15.add(1, this.getRepeatInterval());
                  }

                  var11 = var15.getTime();
               }
            }

            return !var2 && var7 <= var11.getTime() ? null : var11;
         }
      }
   }

   private boolean daylightSavingHourShiftOccurredAndAdvanceNeeded(Calendar var1, int var2, Date var3) {
      if (this.isPreserveHourOfDayAcrossDaylightSavings() && var1.get(11) != var2) {
         var1.set(11, var2);
         return var1.get(11) != var2 ? this.isSkipDayIfHourDoesNotExist() : !var1.getTime().after(var3);
      } else {
         return false;
      }
   }

   @Override
   public Date getFinalFireTime() {
      if (!this.complete && this.getEndTime() != null) {
         Date var1 = new Date(this.getEndTime().getTime() - 1000L);
         var1 = this.getFireTimeAfter(var1, true);
         if (var1.equals(this.getEndTime())) {
            return var1;
         } else {
            Calendar var2 = Calendar.getInstance();
            if (this.timeZone != null) {
               var2.setTimeZone(this.timeZone);
            }

            var2.setTime(var1);
            var2.setLenient(true);
            if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.SECOND)) {
               var2.add(13, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.MINUTE)) {
               var2.add(12, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.HOUR)) {
               var2.add(11, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.DAY)) {
               var2.add(6, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.WEEK)) {
               var2.add(3, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.MONTH)) {
               var2.add(2, -1 * this.getRepeatInterval());
            } else if (this.getRepeatIntervalUnit().equals(DateBuilder.IntervalUnit.YEAR)) {
               var2.add(1, -1 * this.getRepeatInterval());
            }

            return var2.getTime();
         }
      } else {
         return null;
      }
   }

   @Override
   public boolean mayFireAgain() {
      return this.getNextFireTime() != null;
   }

   @Override
   public void validate() throws SchedulerException {
      super.validate();
      if (this.repeatInterval < 1) {
         throw new SchedulerException("Repeat Interval cannot be zero.");
      }
   }

   @Override
   public ScheduleBuilder<CalendarIntervalTrigger> getScheduleBuilder() {
      CalendarIntervalScheduleBuilder var1 = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
         .withInterval(this.getRepeatInterval(), this.getRepeatIntervalUnit());
      switch (this.getMisfireInstruction()) {
         case 1:
            var1.withMisfireHandlingInstructionFireAndProceed();
            break;
         case 2:
            var1.withMisfireHandlingInstructionDoNothing();
      }

      return var1;
   }

   @Override
   public boolean hasAdditionalProperties() {
      return false;
   }
}
