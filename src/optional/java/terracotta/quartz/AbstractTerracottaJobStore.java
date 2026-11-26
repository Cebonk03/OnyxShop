package org.terracotta.quartz;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

public abstract class AbstractTerracottaJobStore implements JobStore {
   public static final String TC_CONFIG_PROP = "org.quartz.jobStore.tcConfig";
   public static final String TC_CONFIGURL_PROP = "org.quartz.jobStore.tcConfigUrl";
   public static final String TC_REJOIN_PROP = "org.quartz.jobStore.rejoin";
   private volatile ToolkitInternal toolkit;
   private volatile TerracottaJobStoreExtensions realJobStore;
   private String tcConfig = null;
   private String tcConfigUrl = null;
   private String schedInstId = null;
   private String schedName = null;
   private Long misFireThreshold = null;
   private String synchWrite = null;
   private String rejoin = null;
   private Long estimatedTimeToReleaseAndAcquireTrigger = null;
   private long tcRetryInterval = TimeUnit.SECONDS.toMillis(15L);

   public AbstractTerracottaJobStore() {
   }

   private void init() {
      if (this.realJobStore == null) {
         if (this.tcConfig != null && this.tcConfigUrl != null) {
            throw new SchedulerConfigException(
               "Both org.quartz.jobStore.tcConfig and org.quartz.jobStore.tcConfigUrl are set in your properties. Please define only one of them"
            );
         } else if (this.tcConfig == null && this.tcConfigUrl == null) {
            throw new SchedulerConfigException(
               "Neither org.quartz.jobStore.tcConfig or org.quartz.jobStore.tcConfigUrl are set in your properties. Please define one of them"
            );
         } else {
            boolean var1 = this.tcConfig == null;
            TerracottaToolkitBuilder var2 = new TerracottaToolkitBuilder();
            if (var1) {
               var2.setTCConfigUrl(this.tcConfigUrl);
            } else {
               var2.setTCConfigSnippet(this.tcConfig);
            }

            if (this.rejoin != null) {
               var2.setRejoin(this.rejoin);
            }

            var2.addTunnelledMBeanDomain("quartz");
            this.toolkit = (ToolkitInternal)var2.buildToolkit();

            try {
               this.realJobStore = this.getRealStore(this.toolkit);
            } catch (Exception var4) {
               throw new SchedulerConfigException("Unable to create Terracotta client", var4);
            }
         }
      }
   }

   abstract TerracottaJobStoreExtensions getRealStore(ToolkitInternal var1);

   public String getUUID() {
      if (this.realJobStore == null) {
         try {
            this.init();
         } catch (Exception var2) {
            throw new RuntimeException(var2);
         }
      }

      return this.realJobStore.getUUID();
   }

   public void setMisfireThreshold(long var1) {
      this.misFireThreshold = var1;
   }

   public void setTcRetryInterval(long var1) {
      this.tcRetryInterval = var1;
   }

   @Override
   public List<OperableTrigger> acquireNextTriggers(long var1, int var3, long var4) {
      try {
         return this.realJobStore.acquireNextTriggers(var1, var3, var4);
      } catch (RejoinException var7) {
         throw new JobPersistenceException("Trigger acquisition failed due to client rejoin", var7);
      }
   }

   @Override
   public List<String> getCalendarNames() {
      try {
         return this.realJobStore.getCalendarNames();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Calendar name retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public List<String> getJobGroupNames() {
      try {
         return this.realJobStore.getJobGroupNames();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Job name retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      try {
         return this.realJobStore.getJobKeys(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Job key retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public int getNumberOfCalendars() {
      try {
         return this.realJobStore.getNumberOfCalendars();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Calendar count retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public int getNumberOfJobs() {
      try {
         return this.realJobStore.getNumberOfJobs();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Job count retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public int getNumberOfTriggers() {
      try {
         return this.realJobStore.getNumberOfTriggers();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Trigger count retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      try {
         return this.realJobStore.getPausedTriggerGroups();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Paused trigger group retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public List<String> getTriggerGroupNames() {
      try {
         return this.realJobStore.getTriggerGroupNames();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Trigger group retrieval failed due to client rejoin", var2);
      }
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      try {
         return this.realJobStore.getTriggerKeys(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger key retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public List<OperableTrigger> getTriggersForJob(JobKey var1) {
      try {
         return this.realJobStore.getTriggersForJob(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      try {
         return this.realJobStore.getTriggerState(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger state retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      try {
         this.realJobStore.resetTriggerFromErrorState(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger state reset failed due to client rejoin", var3);
      }
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      this.init();
      this.realJobStore.setInstanceId(this.schedInstId);
      this.realJobStore.setInstanceName(this.schedName);
      this.realJobStore.setTcRetryInterval(this.tcRetryInterval);
      if (this.misFireThreshold != null) {
         this.realJobStore.setMisfireThreshold(this.misFireThreshold);
      }

      if (this.synchWrite != null) {
         this.realJobStore.setSynchronousWrite(this.synchWrite);
      }

      if (this.estimatedTimeToReleaseAndAcquireTrigger != null) {
         this.realJobStore.setEstimatedTimeToReleaseAndAcquireTrigger(this.estimatedTimeToReleaseAndAcquireTrigger);
      }

      this.realJobStore.initialize(var1, var2);
   }

   @Override
   public void pauseAll() {
      try {
         this.realJobStore.pauseAll();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Pausing failed due to client rejoin", var2);
      }
   }

   @Override
   public void pauseJob(JobKey var1) {
      try {
         this.realJobStore.pauseJob(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Pausing job failed due to client rejoin", var3);
      }
   }

   @Override
   public Collection<String> pauseJobs(GroupMatcher<JobKey> var1) {
      try {
         return this.realJobStore.pauseJobs(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Pausing jobs failed due to client rejoin", var3);
      }
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      try {
         this.realJobStore.pauseTrigger(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Pausing trigger failed due to client rejoin", var3);
      }
   }

   @Override
   public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> var1) {
      try {
         return this.realJobStore.pauseTriggers(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Pausing triggers failed due to client rejoin", var3);
      }
   }

   @Override
   public void releaseAcquiredTrigger(OperableTrigger var1) {
      this.realJobStore.releaseAcquiredTrigger(var1);
   }

   @Override
   public boolean removeCalendar(String var1) {
      try {
         return this.realJobStore.removeCalendar(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Removing calendar failed due to client rejoin", var3);
      }
   }

   @Override
   public boolean removeJob(JobKey var1) {
      try {
         return this.realJobStore.removeJob(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Removing job failed due to client rejoin", var3);
      }
   }

   @Override
   public boolean removeTrigger(TriggerKey var1) {
      try {
         return this.realJobStore.removeTrigger(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Removing trigger failed due to client rejoin", var3);
      }
   }

   @Override
   public boolean replaceTrigger(TriggerKey var1, OperableTrigger var2) {
      try {
         return this.realJobStore.replaceTrigger(var1, var2);
      } catch (RejoinException var4) {
         throw new JobPersistenceException("Replacing trigger failed due to client rejoin", var4);
      }
   }

   @Override
   public void resumeAll() {
      try {
         this.realJobStore.resumeAll();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Resuming failed due to client rejoin", var2);
      }
   }

   @Override
   public void resumeJob(JobKey var1) {
      try {
         this.realJobStore.resumeJob(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Reusming job failed due to client rejoin", var3);
      }
   }

   @Override
   public Collection<String> resumeJobs(GroupMatcher<JobKey> var1) {
      try {
         return this.realJobStore.resumeJobs(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Resuming jobs failed due to client rejoin", var3);
      }
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      try {
         this.realJobStore.resumeTrigger(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Resuming trigger failed due to client rejoin", var3);
      }
   }

   @Override
   public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> var1) {
      try {
         return this.realJobStore.resumeTriggers(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Resuming triggers failed due to client rejoin", var3);
      }
   }

   @Override
   public Calendar retrieveCalendar(String var1) {
      try {
         return this.realJobStore.retrieveCalendar(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Calendar retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public JobDetail retrieveJob(JobKey var1) {
      try {
         return this.realJobStore.retrieveJob(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Job retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public OperableTrigger retrieveTrigger(TriggerKey var1) {
      try {
         return this.realJobStore.retrieveTrigger(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger retrieval failed due to client rejoin", var3);
      }
   }

   @Override
   public void schedulerStarted() {
      try {
         this.realJobStore.schedulerStarted();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Scheduler start failed due to client rejoin", var2);
      }
   }

   @Override
   public void schedulerPaused() {
      this.realJobStore.schedulerPaused();
   }

   @Override
   public void schedulerResumed() {
      this.realJobStore.schedulerResumed();
   }

   @Override
   public void setInstanceId(String var1) {
      this.schedInstId = var1;
   }

   @Override
   public void setInstanceName(String var1) {
      this.schedName = var1;
   }

   @Override
   public void setThreadPoolSize(int var1) {
      this.realJobStore.setThreadPoolSize(var1);
   }

   @Override
   public void shutdown() {
      if (this.realJobStore != null) {
         this.realJobStore.shutdown();
      }

      if (this.toolkit != null) {
         this.toolkit.shutdown();
      }
   }

   @Override
   public void storeCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      try {
         this.realJobStore.storeCalendar(var1, var2, var3, var4);
      } catch (RejoinException var6) {
         throw new JobPersistenceException("Storing calendar failed due to client rejoin", var6);
      }
   }

   @Override
   public void storeJob(JobDetail var1, boolean var2) {
      try {
         this.realJobStore.storeJob(var1, var2);
      } catch (RejoinException var4) {
         throw new JobPersistenceException("Storing job failed due to client rejoin", var4);
      }
   }

   @Override
   public void storeJobAndTrigger(JobDetail var1, OperableTrigger var2) {
      try {
         this.realJobStore.storeJobAndTrigger(var1, var2);
      } catch (RejoinException var4) {
         throw new JobPersistenceException("Storing job and trigger failed due to client rejoin", var4);
      }
   }

   @Override
   public void storeTrigger(OperableTrigger var1, boolean var2) {
      try {
         this.realJobStore.storeTrigger(var1, var2);
      } catch (RejoinException var4) {
         throw new JobPersistenceException("Storing trigger failed due to client rejoin", var4);
      }
   }

   @Override
   public boolean supportsPersistence() {
      return true;
   }

   @Override
   public void triggeredJobComplete(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      this.realJobStore.triggeredJobComplete(var1, var2, var3);
   }

   @Override
   public List<TriggerFiredResult> triggersFired(List<OperableTrigger> var1) {
      try {
         return this.realJobStore.triggersFired(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger fire marking failed due to client rejoin", var3);
      }
   }

   public void setTcConfig(String var1) {
      this.tcConfig = var1.trim();
   }

   public void setTcConfigUrl(String var1) {
      this.tcConfigUrl = var1.trim();
   }

   public void setSynchronousWrite(String var1) {
      this.synchWrite = var1;
   }

   public void setRejoin(String var1) {
      this.rejoin = var1;
      this.setSynchronousWrite(Boolean.TRUE.toString());
   }

   @Override
   public long getEstimatedTimeToReleaseAndAcquireTrigger() {
      return this.realJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
   }

   public void setEstimatedTimeToReleaseAndAcquireTrigger(long var1) {
      this.estimatedTimeToReleaseAndAcquireTrigger = var1;
   }

   @Override
   public boolean isClustered() {
      return true;
   }

   @Override
   public boolean checkExists(JobKey var1) {
      try {
         return this.realJobStore.checkExists(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Job existence check failed due to client rejoin", var3);
      }
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      try {
         return this.realJobStore.checkExists(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Trigger existence check failed due to client rejoin", var3);
      }
   }

   @Override
   public void clearAllSchedulingData() {
      try {
         this.realJobStore.clearAllSchedulingData();
      } catch (RejoinException var2) {
         throw new JobPersistenceException("Scheduler data clear failed due to client rejoin", var2);
      }
   }

   @Override
   public boolean removeTriggers(List<TriggerKey> var1) {
      try {
         return this.realJobStore.removeTriggers(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Remvoing triggers failed due to client rejoin", var3);
      }
   }

   @Override
   public boolean removeJobs(List<JobKey> var1) {
      try {
         return this.realJobStore.removeJobs(var1);
      } catch (RejoinException var3) {
         throw new JobPersistenceException("Removing jobs failed due to client rejoin", var3);
      }
   }

   @Override
   public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      try {
         this.realJobStore.storeJobsAndTriggers(var1, var2);
      } catch (RejoinException var4) {
         throw new JobPersistenceException("Store jobs and triggers failed due to client rejoin", var4);
      }
   }

   @Override
   public long getAcquireRetryDelay(int var1) {
      try {
         return this.realJobStore.getAcquireRetryDelay(var1);
      } catch (Exception var3) {
         return 20L;
      }
   }

   protected TerracottaJobStoreExtensions getRealJobStore() {
      return this.realJobStore;
   }
}
