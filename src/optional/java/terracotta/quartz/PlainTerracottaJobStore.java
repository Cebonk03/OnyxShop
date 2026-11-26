package org.terracotta.quartz;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;
import org.terracotta.toolkit.internal.ToolkitInternal;

public class PlainTerracottaJobStore<T extends ClusteredJobStore> implements TerracottaJobStoreExtensions {
   private static final long WEEKLY = 604800000L;
   private Timer updateCheckTimer;
   private volatile T clusteredJobStore = (T)null;
   private Long misfireThreshold = null;
   private String schedName;
   private String synchWrite = "false";
   private Long estimatedTimeToReleaseAndAcquireTrigger = null;
   private String schedInstanceId;
   private long tcRetryInterval;
   private int threadPoolSize;
   protected final ToolkitInternal toolkit;

   public PlainTerracottaJobStore(ToolkitInternal var1) {
      this.toolkit = var1;
   }

   @Override
   public void setSynchronousWrite(String var1) {
      this.synchWrite = var1;
   }

   @Override
   public void setThreadPoolSize(int var1) {
      this.threadPoolSize = var1;
   }

   @Override
   public List<OperableTrigger> acquireNextTriggers(long var1, int var3, long var4) {
      return this.clusteredJobStore.acquireNextTriggers(var1, var3, var4);
   }

   @Override
   public List<String> getCalendarNames() {
      return this.clusteredJobStore.getCalendarNames();
   }

   @Override
   public List<String> getJobGroupNames() {
      return this.clusteredJobStore.getJobGroupNames();
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      return this.clusteredJobStore.getJobKeys(var1);
   }

   @Override
   public int getNumberOfCalendars() {
      return this.clusteredJobStore.getNumberOfCalendars();
   }

   @Override
   public int getNumberOfJobs() {
      return this.clusteredJobStore.getNumberOfJobs();
   }

   @Override
   public int getNumberOfTriggers() {
      return this.clusteredJobStore.getNumberOfTriggers();
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      return this.clusteredJobStore.getPausedTriggerGroups();
   }

   @Override
   public List<String> getTriggerGroupNames() {
      return this.clusteredJobStore.getTriggerGroupNames();
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      return this.clusteredJobStore.getTriggerKeys(var1);
   }

   @Override
   public List<OperableTrigger> getTriggersForJob(JobKey var1) {
      return this.clusteredJobStore.getTriggersForJob(var1);
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      return this.clusteredJobStore.getTriggerState(var1);
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      this.clusteredJobStore.resetTriggerFromErrorState(var1);
   }

   @Override
   public synchronized void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      if (this.clusteredJobStore != null) {
         throw new IllegalStateException("already initialized");
      } else {
         this.clusteredJobStore = this.createNewJobStoreInstance(this.schedName, Boolean.valueOf(this.synchWrite));
         this.clusteredJobStore.setThreadPoolSize(this.threadPoolSize);
         if (this.misfireThreshold != null) {
            this.clusteredJobStore.setMisfireThreshold(this.misfireThreshold);
            this.misfireThreshold = null;
         }

         if (this.estimatedTimeToReleaseAndAcquireTrigger != null) {
            this.clusteredJobStore.setEstimatedTimeToReleaseAndAcquireTrigger(this.estimatedTimeToReleaseAndAcquireTrigger);
            this.estimatedTimeToReleaseAndAcquireTrigger = null;
         }

         this.clusteredJobStore.setInstanceId(this.schedInstanceId);
         this.clusteredJobStore.setTcRetryInterval(this.tcRetryInterval);
         this.clusteredJobStore.initialize(var1, var2);
      }
   }

   @Override
   public void pauseAll() {
      this.clusteredJobStore.pauseAll();
   }

   @Override
   public void pauseJob(JobKey var1) {
      this.clusteredJobStore.pauseJob(var1);
   }

   @Override
   public Collection<String> pauseJobs(GroupMatcher<JobKey> var1) {
      return this.clusteredJobStore.pauseJobs(var1);
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      this.clusteredJobStore.pauseTrigger(var1);
   }

   @Override
   public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> var1) {
      return this.clusteredJobStore.pauseTriggers(var1);
   }

   @Override
   public void releaseAcquiredTrigger(OperableTrigger var1) {
      this.clusteredJobStore.releaseAcquiredTrigger(var1);
   }

   @Override
   public List<TriggerFiredResult> triggersFired(List<OperableTrigger> var1) {
      return this.clusteredJobStore.triggersFired(var1);
   }

   @Override
   public boolean removeCalendar(String var1) {
      return this.clusteredJobStore.removeCalendar(var1);
   }

   @Override
   public boolean removeJob(JobKey var1) {
      return this.clusteredJobStore.removeJob(var1);
   }

   @Override
   public boolean removeTrigger(TriggerKey var1) {
      return this.clusteredJobStore.removeTrigger(var1);
   }

   @Override
   public boolean removeJobs(List<JobKey> var1) {
      return this.clusteredJobStore.removeJobs(var1);
   }

   @Override
   public boolean removeTriggers(List<TriggerKey> var1) {
      return this.clusteredJobStore.removeTriggers(var1);
   }

   @Override
   public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      this.clusteredJobStore.storeJobsAndTriggers(var1, var2);
   }

   @Override
   public boolean replaceTrigger(TriggerKey var1, OperableTrigger var2) {
      return this.clusteredJobStore.replaceTrigger(var1, var2);
   }

   @Override
   public void resumeAll() {
      this.clusteredJobStore.resumeAll();
   }

   @Override
   public void resumeJob(JobKey var1) {
      this.clusteredJobStore.resumeJob(var1);
   }

   @Override
   public Collection<String> resumeJobs(GroupMatcher<JobKey> var1) {
      return this.clusteredJobStore.resumeJobs(var1);
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      this.clusteredJobStore.resumeTrigger(var1);
   }

   @Override
   public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> var1) {
      return this.clusteredJobStore.resumeTriggers(var1);
   }

   @Override
   public Calendar retrieveCalendar(String var1) {
      return this.clusteredJobStore.retrieveCalendar(var1);
   }

   @Override
   public JobDetail retrieveJob(JobKey var1) {
      return this.clusteredJobStore.retrieveJob(var1);
   }

   @Override
   public OperableTrigger retrieveTrigger(TriggerKey var1) {
      return this.clusteredJobStore.retrieveTrigger(var1);
   }

   @Override
   public boolean checkExists(JobKey var1) {
      return this.clusteredJobStore.checkExists(var1);
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      return this.clusteredJobStore.checkExists(var1);
   }

   @Override
   public void clearAllSchedulingData() {
      this.clusteredJobStore.clearAllSchedulingData();
   }

   @Override
   public void schedulerStarted() {
      this.clusteredJobStore.schedulerStarted();
   }

   @Override
   public void schedulerPaused() {
      if (this.clusteredJobStore != null) {
         this.clusteredJobStore.schedulerPaused();
      }
   }

   @Override
   public void schedulerResumed() {
      this.clusteredJobStore.schedulerResumed();
   }

   @Override
   public void shutdown() {
      if (this.clusteredJobStore != null) {
         this.clusteredJobStore.shutdown();
      }

      if (this.updateCheckTimer != null) {
         this.updateCheckTimer.cancel();
      }
   }

   @Override
   public void storeCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      this.clusteredJobStore.storeCalendar(var1, var2, var3, var4);
   }

   @Override
   public void storeJob(JobDetail var1, boolean var2) {
      this.clusteredJobStore.storeJob(var1, var2);
   }

   @Override
   public void storeJobAndTrigger(JobDetail var1, OperableTrigger var2) {
      this.clusteredJobStore.storeJobAndTrigger(var1, var2);
   }

   @Override
   public void storeTrigger(OperableTrigger var1, boolean var2) {
      this.clusteredJobStore.storeTrigger(var1, var2);
   }

   @Override
   public boolean supportsPersistence() {
      return true;
   }

   @Override
   public String toString() {
      return this.clusteredJobStore.toString();
   }

   @Override
   public void triggeredJobComplete(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      this.clusteredJobStore.triggeredJobComplete(var1, var2, var3);
   }

   @Override
   public synchronized void setMisfireThreshold(long var1) {
      ClusteredJobStore var3 = this.clusteredJobStore;
      if (var3 != null) {
         var3.setMisfireThreshold(var1);
      } else {
         this.misfireThreshold = var1;
      }
   }

   @Override
   public synchronized void setEstimatedTimeToReleaseAndAcquireTrigger(long var1) {
      ClusteredJobStore var3 = this.clusteredJobStore;
      if (var3 != null) {
         var3.setEstimatedTimeToReleaseAndAcquireTrigger(var1);
      } else {
         this.estimatedTimeToReleaseAndAcquireTrigger = var1;
      }
   }

   @Override
   public void setInstanceId(String var1) {
      this.schedInstanceId = var1;
   }

   @Override
   public void setInstanceName(String var1) {
      this.schedName = var1;
   }

   @Override
   public void setTcRetryInterval(long var1) {
      this.tcRetryInterval = var1;
   }

   @Override
   public long getAcquireRetryDelay(int var1) {
      return this.tcRetryInterval;
   }

   @Override
   public String getUUID() {
      return this.toolkit.getClientUUID();
   }

   protected T createNewJobStoreInstance(String var1, boolean var2) {
      return (T)(new DefaultClusteredJobStore(var2, this.toolkit, var1));
   }

   @Override
   public long getEstimatedTimeToReleaseAndAcquireTrigger() {
      return this.clusteredJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
   }

   @Override
   public boolean isClustered() {
      return true;
   }

   protected T getClusteredJobStore() {
      return this.clusteredJobStore;
   }

   @Override
   public String getName() {
      return this.getClass().getName();
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
   }
}
