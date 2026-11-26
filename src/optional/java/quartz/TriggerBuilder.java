package org.quartz;

import java.util.Date;
import org.quartz.spi.MutableTrigger;
import org.quartz.utils.Key;

public class TriggerBuilder<T extends Trigger> {
   private TriggerKey key;
   private String description;
   private Date startTime = new Date();
   private Date endTime;
   private int priority = 5;
   private String calendarName;
   private JobKey jobKey;
   private JobDataMap jobDataMap = new JobDataMap();
   private ScheduleBuilder<?> scheduleBuilder = null;

   private TriggerBuilder() {
   }

   public static TriggerBuilder<Trigger> newTrigger() {
      return new TriggerBuilder<>();
   }

   public T build() {
      if (this.scheduleBuilder == null) {
         this.scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
      }

      MutableTrigger var1 = this.scheduleBuilder.build();
      var1.setCalendarName(this.calendarName);
      var1.setDescription(this.description);
      var1.setStartTime(this.startTime);
      var1.setEndTime(this.endTime);
      if (this.key == null) {
         this.key = new TriggerKey(Key.createUniqueName(null), null);
      }

      var1.setKey(this.key);
      if (this.jobKey != null) {
         var1.setJobKey(this.jobKey);
      }

      var1.setPriority(this.priority);
      if (!this.jobDataMap.isEmpty()) {
         var1.setJobDataMap(this.jobDataMap);
      }

      return (T)var1;
   }

   public TriggerBuilder<T> withIdentity(String var1) {
      this.key = new TriggerKey(var1, null);
      return this;
   }

   public TriggerBuilder<T> withIdentity(String var1, String var2) {
      this.key = new TriggerKey(var1, var2);
      return this;
   }

   public TriggerBuilder<T> withIdentity(TriggerKey var1) {
      this.key = var1;
      return this;
   }

   public TriggerBuilder<T> withDescription(String var1) {
      this.description = var1;
      return this;
   }

   public TriggerBuilder<T> withPriority(int var1) {
      this.priority = var1;
      return this;
   }

   public TriggerBuilder<T> modifiedByCalendar(String var1) {
      this.calendarName = var1;
      return this;
   }

   public TriggerBuilder<T> startAt(Date var1) {
      this.startTime = var1;
      return this;
   }

   public TriggerBuilder<T> startNow() {
      this.startTime = new Date();
      return this;
   }

   public TriggerBuilder<T> endAt(Date var1) {
      this.endTime = var1;
      return this;
   }

   public <SBT extends T> TriggerBuilder<SBT> withSchedule(ScheduleBuilder<SBT> var1) {
      this.scheduleBuilder = var1;
      return (TriggerBuilder<SBT>)this;
   }

   public TriggerBuilder<T> forJob(JobKey var1) {
      this.jobKey = var1;
      return this;
   }

   public TriggerBuilder<T> forJob(String var1) {
      this.jobKey = new JobKey(var1, null);
      return this;
   }

   public TriggerBuilder<T> forJob(String var1, String var2) {
      this.jobKey = new JobKey(var1, var2);
      return this;
   }

   public TriggerBuilder<T> forJob(JobDetail var1) {
      JobKey var2 = var1.getKey();
      if (var2.getName() == null) {
         throw new IllegalArgumentException("The given job has not yet had a name assigned to it.");
      } else {
         this.jobKey = var2;
         return this;
      }
   }

   public TriggerBuilder<T> usingJobData(String var1, String var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(String var1, Integer var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(String var1, Long var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(String var1, Float var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(String var1, Double var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(String var1, Boolean var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public TriggerBuilder<T> usingJobData(JobDataMap var1) {
      for (String var3 : this.jobDataMap.keySet()) {
         var1.put(var3, this.jobDataMap.get(var3));
      }

      this.jobDataMap = var1;
      return this;
   }
}
