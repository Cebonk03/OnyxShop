package org.quartz.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.jmx.JobDetailSupport;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.spi.JobFactory;

public abstract class RemoteMBeanScheduler implements Scheduler {
   private ObjectName schedulerObjectName;

   public RemoteMBeanScheduler() {
   }

   protected ObjectName getSchedulerObjectName() {
      return this.schedulerObjectName;
   }

   public void setSchedulerObjectName(String var1) {
      try {
         this.schedulerObjectName = new ObjectName(var1);
      } catch (MalformedObjectNameException var3) {
         throw new SchedulerException("Failed to parse Scheduler MBean name: " + var1, var3);
      }
   }

   public void setSchedulerObjectName(ObjectName var1) {
      this.schedulerObjectName = var1;
   }

   public abstract void initialize();

   protected abstract Object getAttribute(String var1);

   protected abstract AttributeList getAttributes(String[] var1);

   protected abstract Object invoke(String var1, Object[] var2, String[] var3);

   @Override
   public String getSchedulerName() {
      return (String)this.getAttribute("SchedulerName");
   }

   @Override
   public String getSchedulerInstanceId() {
      return (String)this.getAttribute("SchedulerInstanceId");
   }

   @Override
   public SchedulerMetaData getMetaData() throws SchedulerException {
      AttributeList var1 = this.getAttributes(
         new String[]{
            "SchedulerName",
            "SchedulerInstanceId",
            "StandbyMode",
            "Shutdown",
            "JobStoreClassName",
            "ThreadPoolClassName",
            "ThreadPoolSize",
            "Version",
            "PerformanceMetrics"
         }
      );

      try {
         return new SchedulerMetaData(
            (String)this.getAttribute(var1, 0).getValue(),
            (String)this.getAttribute(var1, 1).getValue(),
            this.getClass(),
            true,
            false,
            (Boolean)this.getAttribute(var1, 2).getValue(),
            (Boolean)this.getAttribute(var1, 3).getValue(),
            null,
            Integer.parseInt(((Map)this.getAttribute(var1, 8).getValue()).get("JobsExecuted").toString()),
            Class.forName((String)this.getAttribute(var1, 4).getValue()),
            false,
            false,
            Class.forName((String)this.getAttribute(var1, 5).getValue()),
            (Integer)this.getAttribute(var1, 6).getValue(),
            (String)this.getAttribute(var1, 7).getValue()
         );
      } catch (ClassNotFoundException var3) {
         throw new SchedulerException(var3);
      }
   }

   private Attribute getAttribute(AttributeList var1, int var2) {
      return (Attribute)var1.get(var2);
   }

   @Override
   public SchedulerContext getContext() throws SchedulerException {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void start() {
      this.invoke("start", new Object[0], new String[0]);
   }

   @Override
   public void startDelayed(int var1) {
      this.invoke("startDelayed", new Object[]{var1}, new String[]{int.class.getName()});
   }

   @Override
   public void standby() {
      this.invoke("standby", new Object[0], new String[0]);
   }

   @Override
   public boolean isStarted() {
      return (Boolean)this.getAttribute("Started");
   }

   @Override
   public boolean isInStandbyMode() {
      return (Boolean)this.getAttribute("StandbyMode");
   }

   @Override
   public void shutdown() {
      String var1 = this.getSchedulerName();
      this.invoke("shutdown", new Object[0], new String[0]);
      SchedulerRepository.getInstance().remove(var1);
   }

   @Override
   public void shutdown(boolean var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean isShutdown() throws SchedulerException {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public Date scheduleJob(JobDetail var1, Trigger var2) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public Date scheduleJob(Trigger var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void addJob(JobDetail var1, boolean var2) {
      this.invoke("addJob", new Object[]{JobDetailSupport.toCompositeData(var1), var2}, new String[]{CompositeData.class.getName(), boolean.class.getName()});
   }

   @Override
   public void addJob(JobDetail var1, boolean var2, boolean var3) {
      this.invoke(
         "addJob",
         new Object[]{JobDetailSupport.toCompositeData(var1), var2, var3},
         new String[]{CompositeData.class.getName(), boolean.class.getName(), boolean.class.getName()}
      );
   }

   @Override
   public boolean deleteJob(JobKey var1) {
      return (Boolean)this.invoke("deleteJob", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public boolean unscheduleJob(TriggerKey var1) {
      return (Boolean)this.invoke("unscheduleJob", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public boolean deleteJobs(List<JobKey> var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void scheduleJob(JobDetail var1, Set<? extends Trigger> var2, boolean var3) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean unscheduleJobs(List<TriggerKey> var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public Date rescheduleJob(TriggerKey var1, Trigger var2) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void triggerJob(JobKey var1) {
      this.triggerJob(var1, null);
   }

   @Override
   public void triggerJob(JobKey var1, JobDataMap var2) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      this.invoke("pauseTrigger", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public void pauseTriggers(GroupMatcher<TriggerKey> var1) {
      String var2 = null;
      switch (var1.getCompareWithOperator()) {
         case EQUALS:
            var2 = "pauseTriggerGroup";
            break;
         case CONTAINS:
            var2 = "pauseTriggersContaining";
            break;
         case STARTS_WITH:
            var2 = "pauseTriggersStartingWith";
            break;
         case ENDS_WITH:
            var2 = "pauseTriggersEndingWith";
         case ANYTHING:
            var2 = "pauseTriggersAll";
      }

      if (var2 != null) {
         this.invoke(var2, new Object[]{var1.getCompareToValue()}, new String[]{String.class.getName()});
      } else {
         throw new SchedulerException("Unsupported GroupMatcher kind for pausing triggers: " + var1.getCompareWithOperator());
      }
   }

   @Override
   public void pauseJob(JobKey var1) {
      this.invoke("pauseJob", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public void pauseJobs(GroupMatcher<JobKey> var1) {
      String var2 = null;
      switch (var1.getCompareWithOperator()) {
         case EQUALS:
            var2 = "pauseJobGroup";
            break;
         case CONTAINS:
            var2 = "pauseJobsContaining";
         case ANYTHING:
            var2 = "pauseJobsAll";
            break;
         case STARTS_WITH:
            var2 = "pauseJobsStartingWith";
            break;
         case ENDS_WITH:
            var2 = "pauseJobsEndingWith";
      }

      this.invoke(var2, new Object[]{var1.getCompareToValue()}, new String[]{String.class.getName()});
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      this.invoke("resumeTrigger", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public void resumeTriggers(GroupMatcher<TriggerKey> var1) {
      String var2 = null;
      switch (var1.getCompareWithOperator()) {
         case EQUALS:
            var2 = "resumeTriggerGroup";
            break;
         case CONTAINS:
            var2 = "resumeTriggersContaining";
            break;
         case STARTS_WITH:
            var2 = "resumeTriggersStartingWith";
            break;
         case ENDS_WITH:
            var2 = "resumeTriggersEndingWith";
         case ANYTHING:
            var2 = "resumeTriggersAll";
      }

      if (var2 != null) {
         this.invoke(var2, new Object[]{var1.getCompareToValue()}, new String[]{String.class.getName()});
      } else {
         throw new SchedulerException("Unsupported GroupMatcher kind for resuming triggers: " + var1.getCompareWithOperator());
      }
   }

   @Override
   public void resumeJob(JobKey var1) {
      this.invoke("resumeJob", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public void resumeJobs(GroupMatcher<JobKey> var1) {
      String var2 = null;
      switch (var1.getCompareWithOperator()) {
         case EQUALS:
            var2 = "resumeJobGroup";
            break;
         case CONTAINS:
            var2 = "resumeJobsContaining";
         case ANYTHING:
            var2 = "resumeJobsAll";
            break;
         case STARTS_WITH:
            var2 = "resumeJobsStartingWith";
            break;
         case ENDS_WITH:
            var2 = "resumeJobsEndingWith";
      }

      this.invoke(var2, new Object[]{var1.getCompareToValue()}, new String[]{String.class.getName()});
   }

   @Override
   public void pauseAll() {
      this.invoke("pauseAllTriggers", new Object[0], new String[0]);
   }

   @Override
   public void resumeAll() {
      this.invoke("resumeAllTriggers", new Object[0], new String[0]);
   }

   @Override
   public List<String> getJobGroupNames() {
      return (List<String>)this.getAttribute("JobGroupNames");
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      if (var1.getCompareWithOperator().equals(StringMatcher.StringOperatorName.EQUALS)) {
         List var2 = (List)this.invoke("getJobNames", new Object[]{var1.getCompareToValue()}, new String[]{String.class.getName()});
         return new HashSet<>(var2);
      } else {
         throw new SchedulerException("Only equals matcher are supported for looking up JobKeys");
      }
   }

   @Override
   public List<Trigger> getTriggersOfJob(JobKey var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public List<String> getTriggerGroupNames() {
      return (List<String>)this.getAttribute("TriggerGroupNames");
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public JobDetail getJobDetail(JobKey var1) {
      try {
         return JobDetailSupport.newJobDetail(
            (CompositeData)this.invoke(
               "getJobDetail", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()}
            )
         );
      } catch (ClassNotFoundException var3) {
         throw new SchedulerException("Unable to resolve job class", var3);
      }
   }

   @Override
   public Trigger getTrigger(TriggerKey var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean checkExists(JobKey var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      return (Boolean)this.invoke("checkExists", new Object[]{var1}, new String[]{TriggerKey.class.getName()});
   }

   @Override
   public void clear() {
      this.invoke("clear", new Object[0], new String[0]);
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      return Trigger.TriggerState.valueOf(
         (String)this.invoke("getTriggerState", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()})
      );
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      this.invoke("resetTriggerFromErrorState", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()});
   }

   @Override
   public void addCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      this.invoke(
         "addCalendar",
         new Object[]{var1, var2, var3, var4},
         new String[]{String.class.getName(), Calendar.class.getName(), boolean.class.getName(), boolean.class.getName()}
      );
   }

   @Override
   public boolean deleteCalendar(String var1) {
      this.invoke("deleteCalendar", new Object[]{var1}, new String[]{String.class.getName()});
      return true;
   }

   @Override
   public Calendar getCalendar(String var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public List<String> getCalendarNames() {
      return (List<String>)this.getAttribute("CalendarNames");
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      return (Set<String>)this.getAttribute("PausedTriggerGroups");
   }

   @Override
   public ListenerManager getListenerManager() throws SchedulerException {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean interrupt(JobKey var1) {
      try {
         return (Boolean)this.invoke(
            "interruptJob", new Object[]{var1.getName(), var1.getGroup()}, new String[]{String.class.getName(), String.class.getName()}
         );
      } catch (SchedulerException var3) {
         throw new UnableToInterruptJobException(var3);
      }
   }

   @Override
   public boolean interrupt(String var1) {
      try {
         return (Boolean)this.invoke("interruptJob", new Object[]{var1}, new String[]{String.class.getName()});
      } catch (SchedulerException var3) {
         throw new UnableToInterruptJobException(var3);
      }
   }

   @Override
   public void setJobFactory(JobFactory var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }
}
