package org.quartz.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

public class StdScheduler implements Scheduler {
   private QuartzScheduler sched;

   public StdScheduler(QuartzScheduler var1) {
      this.sched = var1;
   }

   @Override
   public String getSchedulerName() {
      return this.sched.getSchedulerName();
   }

   @Override
   public String getSchedulerInstanceId() {
      return this.sched.getSchedulerInstanceId();
   }

   @Override
   public SchedulerMetaData getMetaData() {
      return new SchedulerMetaData(
         this.getSchedulerName(),
         this.getSchedulerInstanceId(),
         this.getClass(),
         false,
         this.isStarted(),
         this.isInStandbyMode(),
         this.isShutdown(),
         this.sched.runningSince(),
         this.sched.numJobsExecuted(),
         this.sched.getJobStoreClass(),
         this.sched.supportsPersistence(),
         this.sched.isClustered(),
         this.sched.getThreadPoolClass(),
         this.sched.getThreadPoolSize(),
         this.sched.getVersion()
      );
   }

   @Override
   public SchedulerContext getContext() {
      return this.sched.getSchedulerContext();
   }

   @Override
   public void start() {
      this.sched.start();
   }

   @Override
   public void startDelayed(int var1) {
      this.sched.startDelayed(var1);
   }

   @Override
   public void standby() {
      this.sched.standby();
   }

   @Override
   public boolean isStarted() {
      return this.sched.runningSince() != null;
   }

   @Override
   public boolean isInStandbyMode() {
      return this.sched.isInStandbyMode();
   }

   @Override
   public void shutdown() {
      this.sched.shutdown();
   }

   @Override
   public void shutdown(boolean var1) {
      this.sched.shutdown(var1);
   }

   @Override
   public boolean isShutdown() {
      return this.sched.isShutdown();
   }

   @Override
   public List<JobExecutionContext> getCurrentlyExecutingJobs() {
      return this.sched.getCurrentlyExecutingJobs();
   }

   @Override
   public void clear() {
      this.sched.clear();
   }

   @Override
   public Date scheduleJob(JobDetail var1, Trigger var2) {
      return this.sched.scheduleJob(var1, var2);
   }

   @Override
   public Date scheduleJob(Trigger var1) {
      return this.sched.scheduleJob(var1);
   }

   @Override
   public void addJob(JobDetail var1, boolean var2) {
      this.sched.addJob(var1, var2);
   }

   @Override
   public void addJob(JobDetail var1, boolean var2, boolean var3) {
      this.sched.addJob(var1, var2, var3);
   }

   @Override
   public boolean deleteJobs(List<JobKey> var1) {
      return this.sched.deleteJobs(var1);
   }

   @Override
   public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      this.sched.scheduleJobs(var1, var2);
   }

   @Override
   public void scheduleJob(JobDetail var1, Set<? extends Trigger> var2, boolean var3) {
      this.sched.scheduleJob(var1, var2, var3);
   }

   @Override
   public boolean unscheduleJobs(List<TriggerKey> var1) {
      return this.sched.unscheduleJobs(var1);
   }

   @Override
   public boolean deleteJob(JobKey var1) {
      return this.sched.deleteJob(var1);
   }

   @Override
   public boolean unscheduleJob(TriggerKey var1) {
      return this.sched.unscheduleJob(var1);
   }

   @Override
   public Date rescheduleJob(TriggerKey var1, Trigger var2) {
      return this.sched.rescheduleJob(var1, var2);
   }

   @Override
   public void triggerJob(JobKey var1) {
      this.triggerJob(var1, null);
   }

   @Override
   public void triggerJob(JobKey var1, JobDataMap var2) {
      this.sched.triggerJob(var1, var2);
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      this.sched.pauseTrigger(var1);
   }

   @Override
   public void pauseTriggers(GroupMatcher<TriggerKey> var1) {
      this.sched.pauseTriggers(var1);
   }

   @Override
   public void pauseJob(JobKey var1) {
      this.sched.pauseJob(var1);
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      return this.sched.getPausedTriggerGroups();
   }

   @Override
   public void pauseJobs(GroupMatcher<JobKey> var1) {
      this.sched.pauseJobs(var1);
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      this.sched.resumeTrigger(var1);
   }

   @Override
   public void resumeTriggers(GroupMatcher<TriggerKey> var1) {
      this.sched.resumeTriggers(var1);
   }

   @Override
   public void resumeJob(JobKey var1) {
      this.sched.resumeJob(var1);
   }

   @Override
   public void resumeJobs(GroupMatcher<JobKey> var1) {
      this.sched.resumeJobs(var1);
   }

   @Override
   public void pauseAll() {
      this.sched.pauseAll();
   }

   @Override
   public void resumeAll() {
      this.sched.resumeAll();
   }

   @Override
   public List<String> getJobGroupNames() {
      return this.sched.getJobGroupNames();
   }

   @Override
   public List<? extends Trigger> getTriggersOfJob(JobKey var1) {
      return this.sched.getTriggersOfJob(var1);
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      return this.sched.getJobKeys(var1);
   }

   @Override
   public List<String> getTriggerGroupNames() {
      return this.sched.getTriggerGroupNames();
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      return this.sched.getTriggerKeys(var1);
   }

   @Override
   public JobDetail getJobDetail(JobKey var1) {
      return this.sched.getJobDetail(var1);
   }

   @Override
   public Trigger getTrigger(TriggerKey var1) {
      return this.sched.getTrigger(var1);
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      return this.sched.getTriggerState(var1);
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      this.sched.resetTriggerFromErrorState(var1);
   }

   @Override
   public void addCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      this.sched.addCalendar(var1, var2, var3, var4);
   }

   @Override
   public boolean deleteCalendar(String var1) {
      return this.sched.deleteCalendar(var1);
   }

   @Override
   public Calendar getCalendar(String var1) {
      return this.sched.getCalendar(var1);
   }

   @Override
   public List<String> getCalendarNames() {
      return this.sched.getCalendarNames();
   }

   @Override
   public boolean checkExists(JobKey var1) {
      return this.sched.checkExists(var1);
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      return this.sched.checkExists(var1);
   }

   @Override
   public void setJobFactory(JobFactory var1) {
      this.sched.setJobFactory(var1);
   }

   @Override
   public ListenerManager getListenerManager() {
      return this.sched.getListenerManager();
   }

   @Override
   public boolean interrupt(JobKey var1) {
      return this.sched.interrupt(var1);
   }

   @Override
   public boolean interrupt(String var1) {
      return this.sched.interrupt(var1);
   }
}
