package org.quartz.impl.triggers;

import java.util.Date;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

public abstract class AbstractTrigger<T extends Trigger> implements OperableTrigger {
   private static final long serialVersionUID = -3904243490805975570L;
   private String name;
   private String group = "DEFAULT";
   private String jobName;
   private String jobGroup = "DEFAULT";
   private String description;
   private JobDataMap jobDataMap;
   private boolean volatility = false;
   private String calendarName = null;
   private String fireInstanceId = null;
   private int misfireInstruction = 0;
   private int priority = 5;
   private transient TriggerKey key = null;

   public AbstractTrigger() {
   }

   public AbstractTrigger(String var1) {
      this.setName(var1);
      this.setGroup(null);
   }

   public AbstractTrigger(String var1, String var2) {
      this.setName(var1);
      this.setGroup(var2);
   }

   public AbstractTrigger(String var1, String var2, String var3, String var4) {
      this.setName(var1);
      this.setGroup(var2);
      this.setJobName(var3);
      this.setJobGroup(var4);
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.name = var1;
         this.key = null;
      } else {
         throw new IllegalArgumentException("Trigger name cannot be null or empty.");
      }
   }

   public String getGroup() {
      return this.group;
   }

   public void setGroup(String var1) {
      if (var1 != null && var1.trim().length() == 0) {
         throw new IllegalArgumentException("Group name cannot be an empty string.");
      } else {
         if (var1 == null) {
            var1 = "DEFAULT";
         }

         this.group = var1;
         this.key = null;
      }
   }

   @Override
   public void setKey(TriggerKey var1) {
      this.setName(var1.getName());
      this.setGroup(var1.getGroup());
      this.key = var1;
   }

   public String getJobName() {
      return this.jobName;
   }

   public void setJobName(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.jobName = var1;
      } else {
         throw new IllegalArgumentException("Job name cannot be null or empty.");
      }
   }

   public String getJobGroup() {
      return this.jobGroup;
   }

   public void setJobGroup(String var1) {
      if (var1 != null && var1.trim().length() == 0) {
         throw new IllegalArgumentException("Group name cannot be null or empty.");
      } else {
         if (var1 == null) {
            var1 = "DEFAULT";
         }

         this.jobGroup = var1;
      }
   }

   @Override
   public void setJobKey(JobKey var1) {
      this.setJobName(var1.getName());
      this.setJobGroup(var1.getGroup());
   }

   public String getFullName() {
      return this.group + "." + this.name;
   }

   @Override
   public TriggerKey getKey() {
      if (this.key == null) {
         if (this.getName() == null) {
            return null;
         }

         this.key = new TriggerKey(this.getName(), this.getGroup());
      }

      return this.key;
   }

   @Override
   public JobKey getJobKey() {
      return this.getJobName() == null ? null : new JobKey(this.getJobName(), this.getJobGroup());
   }

   public String getFullJobName() {
      return this.jobGroup + "." + this.jobName;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   @Override
   public void setDescription(String var1) {
      this.description = var1;
   }

   @Override
   public void setCalendarName(String var1) {
      this.calendarName = var1;
   }

   @Override
   public String getCalendarName() {
      return this.calendarName;
   }

   @Override
   public JobDataMap getJobDataMap() {
      if (this.jobDataMap == null) {
         this.jobDataMap = new JobDataMap();
      }

      return this.jobDataMap;
   }

   @Override
   public void setJobDataMap(JobDataMap var1) {
      this.jobDataMap = var1;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public void setPriority(int var1) {
      this.priority = var1;
   }

   @Override
   public abstract void triggered(Calendar var1);

   @Override
   public abstract Date computeFirstFireTime(Calendar var1);

   @Override
   public Trigger.CompletedExecutionInstruction executionComplete(JobExecutionContext var1, JobExecutionException var2) {
      if (var2 != null && var2.refireImmediately()) {
         return Trigger.CompletedExecutionInstruction.RE_EXECUTE_JOB;
      } else if (var2 != null && var2.unscheduleFiringTrigger()) {
         return Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE;
      } else if (var2 != null && var2.unscheduleAllTriggers()) {
         return Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE;
      } else {
         return !this.mayFireAgain() ? Trigger.CompletedExecutionInstruction.DELETE_TRIGGER : Trigger.CompletedExecutionInstruction.NOOP;
      }
   }

   @Override
   public abstract boolean mayFireAgain();

   @Override
   public abstract Date getStartTime();

   @Override
   public abstract void setStartTime(Date var1);

   @Override
   public abstract void setEndTime(Date var1);

   @Override
   public abstract Date getEndTime();

   @Override
   public abstract Date getNextFireTime();

   @Override
   public abstract Date getPreviousFireTime();

   @Override
   public abstract Date getFireTimeAfter(Date var1);

   @Override
   public abstract Date getFinalFireTime();

   @Override
   public void setMisfireInstruction(int var1) {
      if (!this.validateMisfireInstruction(var1)) {
         throw new IllegalArgumentException("The misfire instruction code is invalid for this type of trigger.");
      } else {
         this.misfireInstruction = var1;
      }
   }

   protected abstract boolean validateMisfireInstruction(int var1);

   @Override
   public int getMisfireInstruction() {
      return this.misfireInstruction;
   }

   @Override
   public abstract void updateAfterMisfire(Calendar var1);

   @Override
   public abstract void updateWithNewCalendar(Calendar var1, long var2);

   @Override
   public void validate() throws SchedulerException {
      if (this.name == null) {
         throw new SchedulerException("Trigger's name cannot be null");
      } else if (this.group == null) {
         throw new SchedulerException("Trigger's group cannot be null");
      } else if (this.jobName == null) {
         throw new SchedulerException("Trigger's related Job's name cannot be null");
      } else if (this.jobGroup == null) {
         throw new SchedulerException("Trigger's related Job's group cannot be null");
      }
   }

   @Override
   public void setFireInstanceId(String var1) {
      this.fireInstanceId = var1;
   }

   @Override
   public String getFireInstanceId() {
      return this.fireInstanceId;
   }

   @Override
   public String toString() {
      return "Trigger '"
         + this.getFullName()
         + "':  triggerClass: '"
         + this.getClass().getName()
         + " calendar: '"
         + this.getCalendarName()
         + "' misfireInstruction: "
         + this.getMisfireInstruction()
         + " nextFireTime: "
         + this.getNextFireTime();
   }

   @Override
   public int compareTo(Trigger var1) {
      if (var1.getKey() == null && this.getKey() == null) {
         return 0;
      } else if (var1.getKey() == null) {
         return -1;
      } else {
         return this.getKey() == null ? 1 : this.getKey().compareTo(var1.getKey());
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof Trigger)) {
         return false;
      } else {
         Trigger var2 = (Trigger)var1;
         return var2.getKey() != null && this.getKey() != null && this.getKey().equals(var2.getKey());
      }
   }

   @Override
   public int hashCode() {
      return this.getKey() == null ? super.hashCode() : this.getKey().hashCode();
   }

   @Override
   public Object clone() {
      try {
         AbstractTrigger var1 = (AbstractTrigger)super.clone();
         if (this.jobDataMap != null) {
            var1.jobDataMap = (JobDataMap)this.jobDataMap.clone();
         }

         return var1;
      } catch (CloneNotSupportedException var3) {
         throw new IncompatibleClassChangeError("Not Cloneable.");
      }
   }

   @Override
   public TriggerBuilder<T> getTriggerBuilder() {
      return TriggerBuilder.newTrigger()
         .forJob(this.getJobKey())
         .modifiedByCalendar(this.getCalendarName())
         .usingJobData(this.getJobDataMap())
         .withDescription(this.getDescription())
         .endAt(this.getEndTime())
         .withIdentity(this.getKey())
         .withPriority(this.getPriority())
         .startAt(this.getStartTime())
         .withSchedule(this.getScheduleBuilder());
   }

   @Override
   public abstract ScheduleBuilder<T> getScheduleBuilder();
}
