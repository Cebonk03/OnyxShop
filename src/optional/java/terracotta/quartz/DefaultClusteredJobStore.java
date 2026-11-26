package org.terracotta.quartz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.quartz.collections.TimeTriggerSet;
import org.terracotta.quartz.collections.ToolkitDSHolder;
import org.terracotta.quartz.wrappers.DefaultWrapperFactory;
import org.terracotta.quartz.wrappers.FiredTrigger;
import org.terracotta.quartz.wrappers.JobFacade;
import org.terracotta.quartz.wrappers.JobWrapper;
import org.terracotta.quartz.wrappers.TriggerFacade;
import org.terracotta.quartz.wrappers.TriggerWrapper;
import org.terracotta.quartz.wrappers.WrapperFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.atomic.ToolkitTransactionType;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.rejoin.RejoinException;
import org.terracotta.toolkit.store.ToolkitStore;

class DefaultClusteredJobStore implements ClusteredJobStore {
   private final ToolkitDSHolder toolkitDSHolder;
   private final Toolkit toolkit;
   private final JobFacade jobFacade;
   private final TriggerFacade triggerFacade;
   private final TimeTriggerSet timeTriggers;
   private final ToolkitStore<String, Calendar> calendarsByName;
   private long misfireThreshold = 60000L;
   private final ToolkitLockTypeInternal lockType;
   private final transient ToolkitLock lock;
   private final ClusterInfo clusterInfo;
   private final WrapperFactory wrapperFactory;
   private long ftrCtr;
   private volatile SchedulerSignaler signaler;
   private final Logger logger;
   private volatile String terracottaClientId;
   private long estimatedTimeToReleaseAndAcquireTrigger = 15L;
   private volatile DefaultClusteredJobStore.LocalLockState localStateLock;
   private volatile DefaultClusteredJobStore.TriggerRemovedFromCandidateFiringListHandler triggerRemovedFromCandidateFiringListHandler;
   private volatile boolean toolkitShutdown;
   private long retryInterval;

   public DefaultClusteredJobStore(boolean var1, Toolkit var2, String var3) {
      this(var1, var2, var3, new ToolkitDSHolder(var3, var2), new DefaultWrapperFactory());
   }

   public DefaultClusteredJobStore(boolean var1, Toolkit var2, String var3, ToolkitDSHolder var4, WrapperFactory var5) {
      this.toolkit = var2;
      this.wrapperFactory = var5;
      this.clusterInfo = var2.getClusterInfo();
      this.toolkitDSHolder = var4;
      this.jobFacade = new JobFacade(var4);
      this.triggerFacade = new TriggerFacade(var4);
      this.timeTriggers = var4.getOrCreateTimeTriggerSet();
      this.calendarsByName = var4.getOrCreateCalendarWrapperMap();
      this.lockType = var1 ? ToolkitLockTypeInternal.SYNCHRONOUS_WRITE : ToolkitLockTypeInternal.WRITE;
      ToolkitTransactionType var6 = var1 ? ToolkitTransactionType.SYNC : ToolkitTransactionType.NORMAL;
      this.lock = new TransactionControllingLock((ToolkitInternal)var2, var4.getLock(this.lockType), var6);
      this.logger = LoggerFactory.getLogger(this.getClass());
      this.getLog().info("Synchronous write locking is [" + var1 + "]");
   }

   private Logger getLog() {
      return this.logger;
   }

   private void disable() {
      this.toolkitShutdown = true;

      try {
         this.getLocalLockState().disableLocking();
      } catch (InterruptedException var2) {
         this.getLog().error("failed to disable the job store", var2);
      }
   }

   private DefaultClusteredJobStore.LocalLockState getLocalLockState() {
      DefaultClusteredJobStore.LocalLockState var1 = this.localStateLock;
      if (var1 != null) {
         return var1;
      } else {
         synchronized (DefaultClusteredJobStore.class) {
            if (this.localStateLock == null) {
               this.localStateLock = new DefaultClusteredJobStore.LocalLockState();
            }

            return this.localStateLock;
         }
      }
   }

   void lock() {
      this.getLocalLockState().attemptAcquireBegin();

      try {
         this.lock.lock();
      } catch (RejoinException var2) {
         this.getLocalLockState().release();
         throw var2;
      }
   }

   void unlock() {
      try {
         this.lock.unlock();
      } finally {
         this.getLocalLockState().release();
      }
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      this.terracottaClientId = this.clusterInfo.getCurrentNode().getId();
      this.ftrCtr = System.currentTimeMillis();
      this.signaler = var2;
      this.getLog().info(this.getClass().getSimpleName() + " initialized.");
      ((ToolkitInternal)this.toolkit).registerBeforeShutdownHook(new DefaultClusteredJobStore.ShutdownHook(this));
   }

   @Override
   public void schedulerStarted() {
      this.clusterInfo.addClusterListener(this);
      Set var1 = this.clusterInfo.getNodes();
      HashSet var2 = new HashSet();

      for (ClusterNode var4 : var1) {
         boolean var5 = var2.add(var4.getId());
         if (!var5) {
            this.getLog().error("DUPLICATE node ID detected: " + var4);
         }
      }

      this.lock();

      try {
         ArrayList var11 = new ArrayList();

         for (TriggerKey var15 : this.triggerFacade.allTriggerKeys()) {
            TriggerWrapper var6 = this.triggerFacade.get(var15);
            String var7 = var6.getLastTerracotaClientId();
            if (var7 != null && (!var2.contains(var7) || var6.getState() == TriggerWrapper.TriggerState.ERROR)) {
               var11.add(var6);
            }
         }

         for (TriggerWrapper var16 : var11) {
            this.evalOrphanedTrigger(var16, true);
         }

         Iterator var14 = this.triggerFacade.allFiredTriggers().iterator();

         while (var14.hasNext()) {
            FiredTrigger var17 = (FiredTrigger)var14.next();
            if (!var2.contains(var17.getClientId())) {
               this.getLog().info("Found non-complete fired trigger: " + var17);
               var14.remove();
               TriggerWrapper var18 = this.triggerFacade.get(var17.getTriggerKey());
               if (var18 == null) {
                  this.getLog().error("no trigger found for executing trigger: " + var17.getTriggerKey());
               } else {
                  this.scheduleRecoveryIfNeeded(var18, var17);
               }
            }
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public void schedulerPaused() {
   }

   @Override
   public void schedulerResumed() {
   }

   private void evalOrphanedTrigger(TriggerWrapper var1, boolean var2) {
      this.getLog().info("Evaluating orphaned trigger " + var1);
      JobWrapper var3 = this.jobFacade.get(var1.getJobKey());
      if (var3 == null) {
         this.getLog().error("No job found for orphaned trigger: " + var1);
         this.jobFacade.removeBlockedJob(var1.getJobKey());
      } else {
         if (var2 && var1.getState() == TriggerWrapper.TriggerState.ERROR) {
            var1.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
            this.timeTriggers.add(var1);
         }

         if (var1.getState() == TriggerWrapper.TriggerState.BLOCKED) {
            var1.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
            this.timeTriggers.add(var1);
         } else if (var1.getState() == TriggerWrapper.TriggerState.PAUSED_BLOCKED) {
            var1.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
         }

         if (var1.getState() == TriggerWrapper.TriggerState.ACQUIRED) {
            var1.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
            this.timeTriggers.add(var1);
         }

         if (!var1.mayFireAgain() && !var3.requestsRecovery()) {
            try {
               this.removeTrigger(var1.getKey());
            } catch (JobPersistenceException var7) {
               this.getLog().error("Can't remove completed trigger (and related job) " + var1, var7);
            }
         }

         if (var3.isConcurrentExectionDisallowed()) {
            this.jobFacade.removeBlockedJob(var3.getKey());

            for (TriggerWrapper var6 : this.triggerFacade.getTriggerWrappersForJob(var3.getKey())) {
               if (var6.getState() == TriggerWrapper.TriggerState.BLOCKED) {
                  var6.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
                  this.timeTriggers.add(var6);
               } else if (var6.getState() == TriggerWrapper.TriggerState.PAUSED_BLOCKED) {
                  var6.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
               }
            }
         }
      }
   }

   private void scheduleRecoveryIfNeeded(TriggerWrapper var1, FiredTrigger var2) {
      JobWrapper var3 = this.jobFacade.get(var1.getJobKey());
      if (var3 == null) {
         this.getLog().error("No job found for orphaned trigger: " + var1);
      } else {
         if (var3.requestsRecovery()) {
            OperableTrigger var4 = this.createRecoveryTrigger(var1, var3, "recover_" + this.terracottaClientId + "_" + this.ftrCtr++, var2);
            JobDataMap var5 = var1.getTriggerClone().getJobDataMap();
            var5.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_NAME", var1.getKey().getName());
            var5.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_GROUP", var1.getKey().getGroup());
            var5.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var2.getFireTime()));
            var5.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var2.getScheduledFireTime()));
            var4.setJobDataMap(var5);
            var4.computeFirstFireTime(null);

            try {
               this.storeTrigger(var4, false);
               if (!var1.mayFireAgain()) {
                  this.removeTrigger(var1.getKey());
               }

               this.getLog().info("Recovered job " + var3 + " for trigger " + var1);
            } catch (JobPersistenceException var7) {
               this.getLog().error("Can't recover job " + var3 + " for trigger " + var1, var7);
            }
         }
      }
   }

   protected OperableTrigger createRecoveryTrigger(TriggerWrapper var1, JobWrapper var2, String var3, FiredTrigger var4) {
      SimpleTriggerImpl var5 = new SimpleTriggerImpl(var3, "RECOVERING_JOBS", new Date(var4.getScheduledFireTime()));
      var5.setJobName(var2.getKey().getName());
      var5.setJobGroup(var2.getKey().getGroup());
      var5.setMisfireInstruction(-1);
      var5.setPriority(var1.getPriority());
      return var5;
   }

   private long getMisfireThreshold() {
      return this.misfireThreshold;
   }

   @Override
   public void setMisfireThreshold(long var1) {
      if (var1 < 1L) {
         throw new IllegalArgumentException("Misfirethreashold must be larger than 0");
      } else {
         this.misfireThreshold = var1;
      }
   }

   @Override
   public void shutdown() {
   }

   @Override
   public boolean supportsPersistence() {
      throw new AssertionError();
   }

   @Override
   public void storeJobAndTrigger(JobDetail var1, OperableTrigger var2) {
      this.lock();

      try {
         this.storeJob(var1, false);
         this.storeTrigger(var2, false);
      } finally {
         this.unlock();
      }
   }

   @Override
   public void storeJob(JobDetail var1, boolean var2) {
      JobDetail var3 = (JobDetail)var1.clone();
      this.lock();

      try {
         JobWrapper var4 = this.wrapperFactory.createJobWrapper(var3);
         if (this.jobFacade.containsKey(var4.getKey())) {
            if (!var2) {
               throw new ObjectAlreadyExistsException(var1);
            }
         } else {
            Set var5 = this.toolkitDSHolder.getOrCreateJobsGroupMap(var1.getKey().getGroup());
            var5.add(var4.getKey().getName());
            if (!this.jobFacade.hasGroup(var4.getKey().getGroup())) {
               this.jobFacade.addGroup(var4.getKey().getGroup());
            }
         }

         this.jobFacade.put(var4.getKey(), var4);
      } finally {
         this.unlock();
      }
   }

   @Override
   public boolean removeJob(JobKey var1) {
      boolean var2 = false;
      this.lock();

      try {
         for (OperableTrigger var5 : this.getTriggersForJob(var1)) {
            this.removeTrigger(var5.getKey());
            var2 = true;
         }

         var2 = this.jobFacade.remove(var1) != null | var2;
         if (var2) {
            Set var10 = this.toolkitDSHolder.getOrCreateJobsGroupMap(var1.getGroup());
            var10.remove(var1.getName());
            if (var10.isEmpty()) {
               this.toolkitDSHolder.removeJobsGroupMap(var1.getGroup());
               this.jobFacade.removeGroup(var1.getGroup());
            }
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public boolean removeJobs(List<JobKey> var1) {
      boolean var2 = true;
      this.lock();

      try {
         for (JobKey var4 : var1) {
            var2 = this.removeJob(var4) && var2;
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public boolean removeTriggers(List<TriggerKey> var1) {
      boolean var2 = true;
      this.lock();

      try {
         for (TriggerKey var4 : var1) {
            var2 = this.removeTrigger(var4) && var2;
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      this.lock();

      try {
         if (!var2) {
            for (JobDetail var4 : var1.keySet()) {
               if (this.checkExists(var4.getKey())) {
                  throw new ObjectAlreadyExistsException(var4);
               }

               for (Trigger var6 : (Set)var1.get(var4)) {
                  if (this.checkExists(var6.getKey())) {
                     throw new ObjectAlreadyExistsException(var6);
                  }
               }
            }
         }

         for (JobDetail var11 : var1.keySet()) {
            this.storeJob(var11, true);

            for (Trigger var13 : (Set)var1.get(var11)) {
               this.storeTrigger((OperableTrigger)var13, true);
            }
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public void storeTrigger(OperableTrigger var1, boolean var2) {
      OperableTrigger var3 = (OperableTrigger)var1.clone();
      this.lock();

      try {
         JobDetail var4 = this.retrieveJob(var1.getJobKey());
         if (var4 == null) {
            throw new JobPersistenceException("The job (" + var1.getJobKey() + ") referenced by the trigger does not exist.");
         }

         TriggerWrapper var5 = this.wrapperFactory.createTriggerWrapper(var3, var4.isConcurrentExectionDisallowed());
         if (this.triggerFacade.containsKey(var5.getKey())) {
            if (!var2) {
               throw new ObjectAlreadyExistsException(var1);
            }

            this.removeTrigger(var1.getKey(), false);
         }

         Set var6 = this.toolkitDSHolder.getOrCreateTriggersGroupMap(var1.getKey().getGroup());
         var6.add(var1.getKey().getName());
         if (!this.triggerFacade.hasGroup(var1.getKey().getGroup())) {
            this.triggerFacade.addGroup(var1.getKey().getGroup());
         }

         if (this.triggerFacade.pausedGroupsContain(var1.getKey().getGroup()) || this.jobFacade.pausedGroupsContain(var1.getJobKey().getGroup())) {
            var5.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
            if (this.jobFacade.blockedJobsContain(var5.getJobKey())) {
               var5.setState(TriggerWrapper.TriggerState.PAUSED_BLOCKED, this.terracottaClientId, this.triggerFacade);
            }
         } else if (this.jobFacade.blockedJobsContain(var5.getJobKey())) {
            var5.setState(TriggerWrapper.TriggerState.BLOCKED, this.terracottaClientId, this.triggerFacade);
         } else {
            this.timeTriggers.add(var5);
         }

         this.triggerFacade.put(var5.getKey(), var5);
      } finally {
         this.unlock();
      }
   }

   @Override
   public boolean removeTrigger(TriggerKey var1) {
      return this.removeTrigger(var1, true);
   }

   private boolean removeTrigger(TriggerKey var1, boolean var2) {
      this.lock();
      TriggerWrapper var3 = null;

      try {
         var3 = this.triggerFacade.remove(var1);
         if (var3 != null) {
            Set var4 = this.toolkitDSHolder.getOrCreateTriggersGroupMap(var1.getGroup());
            var4.remove(var1.getName());
            if (var4.size() == 0) {
               this.toolkitDSHolder.removeTriggersGroupMap(var1.getGroup());
               this.triggerFacade.removeGroup(var1.getGroup());
            }

            this.timeTriggers.remove(var3);
            if (var2) {
               JobWrapper var5 = this.jobFacade.get(var3.getJobKey());
               List var6 = this.getTriggersForJob(var3.getJobKey());
               if ((var6 == null || var6.size() == 0) && !var5.isDurable()) {
                  JobKey var7 = var3.getJobKey();
                  if (this.removeJob(var7)) {
                     this.signaler.notifySchedulerListenersJobDeleted(var7);
                  }
               }
            }
         }
      } finally {
         this.unlock();
      }

      return var3 != null;
   }

   @Override
   public boolean replaceTrigger(TriggerKey var1, OperableTrigger var2) {
      boolean var3 = false;
      this.lock();

      try {
         TriggerWrapper var4 = this.triggerFacade.remove(var1);
         var3 = var4 != null;
         if (var4 != null) {
            if (!var4.getJobKey().equals(var2.getJobKey())) {
               throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
            }

            Set var5 = this.toolkitDSHolder.getOrCreateTriggersGroupMap(var1.getGroup());
            var5.remove(var1.getName());
            if (var5.size() == 0) {
               this.toolkitDSHolder.removeTriggersGroupMap(var1.getGroup());
               this.triggerFacade.removeGroup(var1.getGroup());
            }

            this.timeTriggers.remove(var4);

            try {
               this.storeTrigger(var2, false);
            } catch (JobPersistenceException var10) {
               this.storeTrigger(var4.getTriggerClone(), false);
               throw var10;
            }
         }
      } finally {
         this.unlock();
      }

      return var3;
   }

   @Override
   public JobDetail retrieveJob(JobKey var1) {
      JobWrapper var2 = this.getJob(var1);
      return var2 == null ? null : var2.getJobDetailClone();
   }

   JobWrapper getJob(JobKey var1) {
      this.lock();

      JobWrapper var2;
      try {
         var2 = this.jobFacade.get(var1);
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public OperableTrigger retrieveTrigger(TriggerKey var1) {
      this.lock();

      OperableTrigger var3;
      try {
         TriggerWrapper var2 = this.triggerFacade.get(var1);
         var3 = var2 != null ? var2.getTriggerClone() : null;
      } finally {
         this.unlock();
      }

      return var3;
   }

   @Override
   public boolean checkExists(JobKey var1) {
      return this.jobFacade.containsKey(var1);
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      return this.triggerFacade.containsKey(var1);
   }

   @Override
   public void clearAllSchedulingData() {
      this.lock();

      try {
         for (String var3 : this.getTriggerGroupNames()) {
            for (TriggerKey var6 : this.getTriggerKeys(GroupMatcher.triggerGroupEquals(var3))) {
               this.removeTrigger(var6);
            }
         }

         for (String var14 : this.getJobGroupNames()) {
            for (JobKey var18 : this.getJobKeys(GroupMatcher.jobGroupEquals(var14))) {
               this.removeJob(var18);
            }
         }

         for (String var15 : this.getCalendarNames()) {
            this.removeCalendar(var15);
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      this.lock();

      TriggerWrapper var2;
      try {
         var2 = this.triggerFacade.get(var1);
      } finally {
         this.unlock();
      }

      if (var2 == null) {
         return Trigger.TriggerState.NONE;
      } else if (var2.getState() == TriggerWrapper.TriggerState.COMPLETE) {
         return Trigger.TriggerState.COMPLETE;
      } else if (var2.getState() == TriggerWrapper.TriggerState.PAUSED) {
         return Trigger.TriggerState.PAUSED;
      } else if (var2.getState() == TriggerWrapper.TriggerState.PAUSED_BLOCKED) {
         return Trigger.TriggerState.PAUSED;
      } else if (var2.getState() == TriggerWrapper.TriggerState.BLOCKED) {
         return Trigger.TriggerState.BLOCKED;
      } else {
         return var2.getState() == TriggerWrapper.TriggerState.ERROR ? Trigger.TriggerState.ERROR : Trigger.TriggerState.NORMAL;
      }
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      TriggerWrapper var2 = this.triggerFacade.get(var1);
      if (var2 != null) {
         if (var2.getState() == TriggerWrapper.TriggerState.ERROR) {
            if (this.triggerFacade.pausedGroupsContain(var1.getGroup())) {
               var2.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
            } else {
               var2.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
               this.timeTriggers.add(var2);
            }
         }
      }
   }

   @Override
   public void storeCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      Calendar var5 = (Calendar)var2.clone();
      this.lock();

      try {
         Calendar var6 = (Calendar)this.calendarsByName.get(var1);
         if (var6 != null && !var3) {
            throw new ObjectAlreadyExistsException("Calendar with name '" + var1 + "' already exists.");
         }

         if (var6 != null) {
            this.calendarsByName.remove(var1);
         }

         this.calendarsByName.putNoReturn(var1, var5);
         if (var6 != null && var4) {
            for (TriggerWrapper var9 : this.triggerFacade.getTriggerWrappersForCalendar(var1)) {
               boolean var10 = this.timeTriggers.remove(var9);
               var9.updateWithNewCalendar(var5, this.getMisfireThreshold(), this.triggerFacade);
               if (var10) {
                  this.timeTriggers.add(var9);
               }
            }
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public boolean removeCalendar(String var1) {
      int var2 = 0;
      this.lock();

      boolean var9;
      try {
         for (TriggerKey var4 : this.triggerFacade.allTriggerKeys()) {
            TriggerWrapper var5 = this.triggerFacade.get(var4);
            if (var5.getCalendarName() != null && var5.getCalendarName().equals(var1)) {
               var2++;
            }
         }

         if (var2 > 0) {
            throw new JobPersistenceException("Calender cannot be removed if it referenced by a Trigger!");
         }

         var9 = this.calendarsByName.remove(var1) != null;
      } finally {
         this.unlock();
      }

      return var9;
   }

   @Override
   public Calendar retrieveCalendar(String var1) {
      this.lock();

      Calendar var3;
      try {
         Calendar var2 = (Calendar)this.calendarsByName.get(var1);
         var3 = (Calendar & Calendar)(var2 == null ? null : var2.clone());
      } finally {
         this.unlock();
      }

      return var3;
   }

   @Override
   public int getNumberOfJobs() {
      this.lock();

      int var1;
      try {
         var1 = this.jobFacade.numberOfJobs();
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public int getNumberOfTriggers() {
      this.lock();

      int var1;
      try {
         var1 = this.triggerFacade.numberOfTriggers();
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public int getNumberOfCalendars() {
      this.lock();

      int var1;
      try {
         var1 = this.calendarsByName.size();
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      this.lock();

      HashSet var15;
      try {
         HashSet var2 = new HashSet();
         switch (var1.getCompareWithOperator()) {
            case EQUALS:
               var2.add(var1.getCompareToValue());
               break;
            default:
               for (String var4 : this.jobFacade.getAllGroupNames()) {
                  if (var1.getCompareWithOperator().evaluate(var4, var1.getCompareToValue())) {
                     var2.add(var4);
                  }
               }
         }

         HashSet var13 = new HashSet();

         for (String var5 : var2) {
            for (String var8 : this.toolkitDSHolder.getOrCreateJobsGroupMap(var5)) {
               JobKey var9 = new JobKey(var8, var5);
               if (this.jobFacade.containsKey(var9)) {
                  var13.add(var9);
               }
            }
         }

         var15 = var13;
      } finally {
         this.unlock();
      }

      return var15;
   }

   @Override
   public List<String> getCalendarNames() {
      this.lock();

      ArrayList var2;
      try {
         Set var1 = this.calendarsByName.keySet();
         var2 = new ArrayList(var1);
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      this.lock();

      HashSet var16;
      try {
         HashSet var2 = new HashSet();
         switch (var1.getCompareWithOperator()) {
            case EQUALS:
               var2.add(var1.getCompareToValue());
               break;
            default:
               for (String var4 : this.triggerFacade.allTriggersGroupNames()) {
                  if (var1.getCompareWithOperator().evaluate(var4, var1.getCompareToValue())) {
                     var2.add(var4);
                  }
               }
         }

         HashSet var14 = new HashSet();

         for (String var5 : var2) {
            for (String var8 : this.toolkitDSHolder.getOrCreateTriggersGroupMap(var5)) {
               TriggerKey var9 = new TriggerKey(var8, var5);
               TriggerWrapper var10 = this.triggerFacade.get(var9);
               if (var10 != null) {
                  var14.add(var9);
               }
            }
         }

         var16 = var14;
      } finally {
         this.unlock();
      }

      return var16;
   }

   @Override
   public List<String> getJobGroupNames() {
      this.lock();

      ArrayList var1;
      try {
         var1 = new ArrayList<>(this.jobFacade.getAllGroupNames());
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public List<String> getTriggerGroupNames() {
      this.lock();

      ArrayList var1;
      try {
         var1 = new ArrayList<>(this.triggerFacade.allTriggersGroupNames());
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public List<OperableTrigger> getTriggersForJob(JobKey var1) {
      ArrayList var2 = new ArrayList();
      this.lock();

      try {
         for (TriggerKey var4 : this.triggerFacade.allTriggerKeys()) {
            TriggerWrapper var5 = this.triggerFacade.get(var4);
            if (var5.getJobKey().equals(var1)) {
               var2.add(var5.getTriggerClone());
            }
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      this.lock();

      try {
         TriggerWrapper var2 = this.triggerFacade.get(var1);
         if (var2 == null) {
            return;
         }

         if (var2.getState() != TriggerWrapper.TriggerState.COMPLETE) {
            if (var2.getState() == TriggerWrapper.TriggerState.BLOCKED) {
               var2.setState(TriggerWrapper.TriggerState.PAUSED_BLOCKED, this.terracottaClientId, this.triggerFacade);
            } else {
               var2.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
            }

            this.timeTriggers.remove(var2);
            if (this.triggerRemovedFromCandidateFiringListHandler != null) {
               this.triggerRemovedFromCandidateFiringListHandler.removeCandidateTrigger(var2);
            }

            return;
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> var1) {
      HashSet var2 = new HashSet();
      this.lock();

      try {
         for (TriggerKey var5 : this.getTriggerKeys(var1)) {
            this.triggerFacade.addPausedGroup(var5.getGroup());
            var2.add(var5.getGroup());
            this.pauseTrigger(var5);
         }

         StringMatcher.StringOperatorName var9 = var1.getCompareWithOperator();
         if (var9.equals(StringMatcher.StringOperatorName.EQUALS)) {
            this.triggerFacade.addPausedGroup(var1.getCompareToValue());
            var2.add(var1.getCompareToValue());
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void pauseJob(JobKey var1) {
      this.lock();

      try {
         for (OperableTrigger var3 : this.getTriggersForJob(var1)) {
            this.pauseTrigger(var3.getKey());
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public Collection<String> pauseJobs(GroupMatcher<JobKey> var1) {
      HashSet var2 = new HashSet();
      this.lock();

      try {
         for (JobKey var5 : this.getJobKeys(var1)) {
            for (OperableTrigger var7 : this.getTriggersForJob(var5)) {
               this.pauseTrigger(var7.getKey());
            }

            var2.add(var5.getGroup());
         }

         StringMatcher.StringOperatorName var11 = var1.getCompareWithOperator();
         if (var11.equals(StringMatcher.StringOperatorName.EQUALS)) {
            this.jobFacade.addPausedGroup(var1.getCompareToValue());
            var2.add(var1.getCompareToValue());
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      this.lock();

      try {
         TriggerWrapper var2 = this.triggerFacade.get(var1);
         if (var2 == null) {
            return;
         }

         if (var2.getState() == TriggerWrapper.TriggerState.PAUSED || var2.getState() == TriggerWrapper.TriggerState.PAUSED_BLOCKED) {
            if (this.jobFacade.blockedJobsContain(var2.getJobKey())) {
               var2.setState(TriggerWrapper.TriggerState.BLOCKED, this.terracottaClientId, this.triggerFacade);
            } else {
               var2.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
            }

            this.applyMisfire(var2);
            if (var2.getState() == TriggerWrapper.TriggerState.WAITING) {
               this.timeTriggers.add(var2);
            }

            return;
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> var1) {
      HashSet var2 = new HashSet();
      this.lock();

      try {
         for (TriggerKey var5 : this.getTriggerKeys(var1)) {
            TriggerWrapper var6 = this.triggerFacade.get(var5);
            if (var6 != null) {
               String var7 = var6.getJobKey().getGroup();
               if (this.jobFacade.pausedGroupsContain(var7)) {
                  continue;
               }

               var2.add(var5.getGroup());
            }

            this.resumeTrigger(var5);
         }

         this.triggerFacade.removeAllPausedGroups(var2);
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void resumeJob(JobKey var1) {
      this.lock();

      try {
         for (OperableTrigger var3 : this.getTriggersForJob(var1)) {
            this.resumeTrigger(var3.getKey());
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public Collection<String> resumeJobs(GroupMatcher<JobKey> var1) {
      HashSet var2 = new HashSet();
      this.lock();

      try {
         for (JobKey var5 : this.getJobKeys(var1)) {
            if (var2.add(var5.getGroup())) {
               this.jobFacade.removePausedJobGroup(var5.getGroup());
            }

            for (OperableTrigger var7 : this.getTriggersForJob(var5)) {
               this.resumeTrigger(var7.getKey());
            }
         }
      } finally {
         this.unlock();
      }

      return var2;
   }

   @Override
   public void pauseAll() {
      this.lock();

      try {
         for (String var3 : this.getTriggerGroupNames()) {
            this.pauseTriggers(GroupMatcher.triggerGroupEquals(var3));
         }
      } finally {
         this.unlock();
      }
   }

   @Override
   public void resumeAll() {
      this.lock();

      try {
         this.jobFacade.clearPausedJobGroups();

         for (String var3 : this.getTriggerGroupNames()) {
            this.resumeTriggers(GroupMatcher.triggerGroupEquals(var3));
         }
      } finally {
         this.unlock();
      }
   }

   boolean applyMisfire(TriggerWrapper var1) {
      long var2 = System.currentTimeMillis();
      if (this.getMisfireThreshold() > 0L) {
         var2 -= this.getMisfireThreshold();
      }

      Date var4 = var1.getNextFireTime();
      if (var4 != null && var4.getTime() <= var2 && var1.getMisfireInstruction() != -1) {
         Calendar var5 = null;
         if (var1.getCalendarName() != null) {
            var5 = this.retrieveCalendar(var1.getCalendarName());
         }

         this.signaler.notifyTriggerListenersMisfired(var1.getTriggerClone());
         var1.updateAfterMisfire(var5, this.triggerFacade);
         if (var1.getNextFireTime() == null) {
            var1.setState(TriggerWrapper.TriggerState.COMPLETE, this.terracottaClientId, this.triggerFacade);
            this.signaler.notifySchedulerListenersFinalized(var1.getTriggerClone());
            this.timeTriggers.remove(var1);
         } else if (var4.equals(var1.getNextFireTime())) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public List<OperableTrigger> acquireNextTriggers(long var1, int var3, long var4) {
      ArrayList var6 = new ArrayList();
      this.lock();

      ArrayList var16;
      try {
         for (TriggerWrapper var8 : this.getNextTriggerWrappers(this.timeTriggers, var1, var3, var4)) {
            var6.add(this.markAndCloneTrigger(var8));
         }

         var16 = var6;
      } finally {
         try {
            this.unlock();
         } catch (RejoinException var14) {
            if (!this.validateAcquired(var6)) {
               throw var14;
            }
         }
      }

      return var16;
   }

   private boolean validateAcquired(List<OperableTrigger> var1) {
      if (var1.isEmpty()) {
         return false;
      } else {
         while (!this.toolkitShutdown) {
            try {
               this.lock();

               boolean var5;
               try {
                  Iterator var2 = var1.iterator();

                  OperableTrigger var3;
                  TriggerWrapper var4;
                  do {
                     if (!var2.hasNext()) {
                        return true;
                     }

                     var3 = (OperableTrigger)var2.next();
                     var4 = this.triggerFacade.get(var3.getKey());
                  } while (
                     var3.getFireInstanceId().equals(var4.getTriggerClone().getFireInstanceId())
                        && TriggerWrapper.TriggerState.ACQUIRED.equals(var4.getState())
                  );

                  var5 = false;
               } finally {
                  this.unlock();
               }

               return var5;
            } catch (JobPersistenceException var15) {
               try {
                  Thread.sleep(this.retryInterval);
               } catch (InterruptedException var13) {
                  throw new IllegalStateException("Received interrupted exception", var13);
               }
            } catch (RejoinException var16) {
               try {
                  Thread.sleep(this.retryInterval);
               } catch (InterruptedException var12) {
                  throw new IllegalStateException("Received interrupted exception", var12);
               }
            }
         }

         throw new IllegalStateException("Scheduler has been shutdown");
      }
   }

   OperableTrigger markAndCloneTrigger(TriggerWrapper var1) {
      var1.setState(TriggerWrapper.TriggerState.ACQUIRED, this.terracottaClientId, this.triggerFacade);
      String var2 = this.terracottaClientId + "-" + this.ftrCtr++;
      var1.setFireInstanceId(var2, this.triggerFacade);
      return var1.getTriggerClone();
   }

   List<TriggerWrapper> getNextTriggerWrappers(long var1, int var3, long var4) {
      return this.getNextTriggerWrappers(this.timeTriggers, var1, var3, var4);
   }

   List<TriggerWrapper> getNextTriggerWrappers(TimeTriggerSet var1, long var2, int var4, long var5) {
      ArrayList var7 = new ArrayList();
      HashSet var8 = new HashSet();
      HashSet var9 = new HashSet();
      JobPersistenceException var10 = null;
      long var11 = var2;

      try {
         while (true) {
            TriggerWrapper var13 = null;

            try {
               TriggerKey var14 = var1.removeFirst();
               if (var14 != null) {
                  var13 = this.triggerFacade.get(var14);
               }

               if (var13 == null) {
                  break;
               }
            } catch (NoSuchElementException var15) {
               break;
            }

            if (var13.getNextFireTime() != null) {
               if (this.applyMisfire(var13)) {
                  if (var13.getNextFireTime() != null) {
                     var1.add(var13);
                  }
               } else {
                  if (var13.getNextFireTime().getTime() > var11) {
                     var1.add(var13);
                     break;
                  }

                  if (var13.jobDisallowsConcurrence()) {
                     if (var8.contains(var13.getJobKey())) {
                        var9.add(var13);
                        continue;
                     }

                     var8.add(var13.getJobKey());
                  }

                  if (var7.isEmpty()) {
                     var11 = Math.max(var13.getNextFireTime().getTime(), System.currentTimeMillis()) + var5;
                  }

                  var7.add(var13);
                  if (var7.size() == var4) {
                     break;
                  }
               }
            }
         }
      } catch (JobPersistenceException var16) {
         var10 = var16;
      }

      if (var9.size() > 0) {
         for (TriggerWrapper var19 : var9) {
            var1.add(var19);
         }
      }

      if (var10 == null) {
         return var7;
      } else {
         for (TriggerWrapper var20 : var7) {
            var1.add(var20);
         }

         throw new JobPersistenceException("Exception encountered while trying to select triggers for firing.", var10);
      }
   }

   public void setTriggerRemovedFromCandidateFiringListHandler(DefaultClusteredJobStore.TriggerRemovedFromCandidateFiringListHandler var1) {
      this.triggerRemovedFromCandidateFiringListHandler = var1;
   }

   @Override
   public void releaseAcquiredTrigger(OperableTrigger var1) {
      while (!this.toolkitShutdown) {
         try {
            this.lock();

            try {
               TriggerWrapper var2 = this.triggerFacade.get(var1.getKey());
               if (var2 != null
                  && var1.getFireInstanceId().equals(var2.getTriggerClone().getFireInstanceId())
                  && var2.getState() == TriggerWrapper.TriggerState.ACQUIRED) {
                  var2.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
                  this.timeTriggers.add(var2);
               }
               break;
            } finally {
               this.unlock();
            }
         } catch (RejoinException var12) {
            try {
               Thread.sleep(this.retryInterval);
            } catch (InterruptedException var10) {
               throw new IllegalStateException("Received interrupted exception", var10);
            }
         } catch (JobPersistenceException var13) {
            try {
               Thread.sleep(this.retryInterval);
            } catch (InterruptedException var9) {
               throw new IllegalStateException("Received interrupted exception", var9);
            }
         }
      }
   }

   @Override
   public List<TriggerFiredResult> triggersFired(List<OperableTrigger> var1) {
      ArrayList var2 = new ArrayList();
      this.lock();

      ArrayList var22;
      try {
         for (OperableTrigger var4 : var1) {
            TriggerWrapper var5 = this.triggerFacade.get(var4.getKey());
            if (var5 == null) {
               var2.add(new TriggerFiredResult((TriggerFiredBundle)null));
            } else if (var5.getState() != TriggerWrapper.TriggerState.ACQUIRED) {
               var2.add(new TriggerFiredResult((TriggerFiredBundle)null));
            } else {
               Calendar var6 = null;
               if (var5.getCalendarName() != null) {
                  var6 = this.retrieveCalendar(var5.getCalendarName());
                  if (var6 == null) {
                     var2.add(new TriggerFiredResult((TriggerFiredBundle)null));
                     continue;
                  }
               }

               Date var7 = var4.getPreviousFireTime();
               this.timeTriggers.remove(var5);
               var5.triggered(var6, this.triggerFacade);
               var4.triggered(var6);
               var5.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
               TriggerFiredBundle var8 = new TriggerFiredBundle(
                  this.retrieveJob(var4.getJobKey()), var4, var6, false, new Date(), var4.getPreviousFireTime(), var7, var4.getNextFireTime()
               );
               String var9 = var4.getFireInstanceId();
               FiredTrigger var10 = this.triggerFacade.getFiredTrigger(var9);
               this.triggerFacade.putFiredTrigger(var9, new FiredTrigger(this.terracottaClientId, var5.getKey(), var4.getPreviousFireTime().getTime()));
               this.getLog().trace("Tracking " + var4 + " has fired on " + var9);
               if (var10 != null) {
                  throw new AssertionError("duplicate fireInstanceId detected (" + var9 + ") for " + var4 + ", previous is " + var10);
               }

               JobDetail var11 = var8.getJobDetail();
               if (var11.isConcurrentExectionDisallowed()) {
                  for (TriggerWrapper var14 : this.triggerFacade.getTriggerWrappersForJob(var11.getKey())) {
                     if (!var14.getKey().equals(var5.getKey())) {
                        if (var14.getState() == TriggerWrapper.TriggerState.WAITING) {
                           var14.setState(TriggerWrapper.TriggerState.BLOCKED, this.terracottaClientId, this.triggerFacade);
                        }

                        if (var14.getState() == TriggerWrapper.TriggerState.PAUSED) {
                           var14.setState(TriggerWrapper.TriggerState.PAUSED_BLOCKED, this.terracottaClientId, this.triggerFacade);
                        }

                        this.timeTriggers.remove(var14);
                        if (this.triggerRemovedFromCandidateFiringListHandler != null) {
                           this.triggerRemovedFromCandidateFiringListHandler.removeCandidateTrigger(var14);
                        }
                     }
                  }

                  this.jobFacade.addBlockedJob(var11.getKey());
               } else if (var5.getNextFireTime() != null) {
                  this.timeTriggers.add(var5);
               }

               var2.add(new TriggerFiredResult(var8));
            }
         }

         var22 = var2;
      } finally {
         try {
            this.unlock();
         } catch (RejoinException var20) {
            if (!this.validateFiring(var2)) {
               throw var20;
            }
         }
      }

      return var22;
   }

   private boolean validateFiring(List<TriggerFiredResult> var1) {
      if (var1.isEmpty()) {
         return false;
      } else {
         while (!this.toolkitShutdown) {
            try {
               this.lock();

               boolean var5;
               try {
                  Iterator var2 = var1.iterator();

                  TriggerFiredBundle var4;
                  do {
                     if (!var2.hasNext()) {
                        return true;
                     }

                     TriggerFiredResult var3 = (TriggerFiredResult)var2.next();
                     var4 = var3.getTriggerFiredBundle();
                  } while (var4 == null || this.triggerFacade.containsFiredTrigger(var4.getTrigger().getFireInstanceId()));

                  var5 = false;
               } finally {
                  this.unlock();
               }

               return var5;
            } catch (JobPersistenceException var15) {
               try {
                  Thread.sleep(this.retryInterval);
               } catch (InterruptedException var13) {
                  throw new IllegalStateException("Received interrupted exception", var13);
               }
            } catch (RejoinException var16) {
               try {
                  Thread.sleep(this.retryInterval);
               } catch (InterruptedException var12) {
                  throw new IllegalStateException("Received interrupted exception", var12);
               }
            }
         }

         throw new IllegalStateException("Scheduler has been shutdown");
      }
   }

   @Override
   public void triggeredJobComplete(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      while (!this.toolkitShutdown) {
         try {
            this.lock();

            try {
               String var4 = var1.getFireInstanceId();
               FiredTrigger var5 = this.triggerFacade.removeFiredTrigger(var4);
               if (var5 == null) {
                  this.getLog().warn("No fired trigger record found for " + var1 + " (" + var4 + ")");
               } else {
                  JobKey var6 = var2.getKey();
                  JobWrapper var7 = this.jobFacade.get(var6);
                  TriggerWrapper var8 = this.triggerFacade.get(var1.getKey());
                  if (var7 == null) {
                     this.jobFacade.removeBlockedJob(var6);
                  } else {
                     if (var7.isPersistJobDataAfterExecution()) {
                        JobDataMap var9 = var2.getJobDataMap();
                        if (var9 != null) {
                           var9 = (JobDataMap)var9.clone();
                           var9.clearDirtyFlag();
                        }

                        var7.setJobDataMap(var9, this.jobFacade);
                     }

                     if (var7.isConcurrentExectionDisallowed()) {
                        this.jobFacade.removeBlockedJob(var7.getKey());
                        var8.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
                        this.timeTriggers.add(var8);

                        for (TriggerWrapper var11 : this.triggerFacade.getTriggerWrappersForJob(var7.getKey())) {
                           if (var11.getState() == TriggerWrapper.TriggerState.BLOCKED) {
                              var11.setState(TriggerWrapper.TriggerState.WAITING, this.terracottaClientId, this.triggerFacade);
                              this.timeTriggers.add(var11);
                           }

                           if (var11.getState() == TriggerWrapper.TriggerState.PAUSED_BLOCKED) {
                              var11.setState(TriggerWrapper.TriggerState.PAUSED, this.terracottaClientId, this.triggerFacade);
                           }
                        }

                        this.signaler.signalSchedulingChange(0L);
                     }
                  }

                  if (var8 != null) {
                     if (var3 == Trigger.CompletedExecutionInstruction.DELETE_TRIGGER) {
                        if (var1.getNextFireTime() == null) {
                           if (var8.getNextFireTime() == null) {
                              this.removeTrigger(var1.getKey());
                           }
                        } else {
                           this.removeTrigger(var1.getKey());
                           this.signaler.signalSchedulingChange(0L);
                        }
                     } else if (var3 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
                        var8.setState(TriggerWrapper.TriggerState.COMPLETE, this.terracottaClientId, this.triggerFacade);
                        this.timeTriggers.remove(var8);
                        this.signaler.signalSchedulingChange(0L);
                     } else if (var3 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
                        this.getLog().info("Trigger " + var1.getKey() + " set to ERROR state.");
                        var8.setState(TriggerWrapper.TriggerState.ERROR, this.terracottaClientId, this.triggerFacade);
                        this.signaler.signalSchedulingChange(0L);
                     } else if (var3 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
                        this.getLog().info("All triggers of Job " + var1.getJobKey() + " set to ERROR state.");
                        this.setAllTriggersOfJobToState(var1.getJobKey(), TriggerWrapper.TriggerState.ERROR);
                        this.signaler.signalSchedulingChange(0L);
                     } else if (var3 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
                        this.setAllTriggersOfJobToState(var1.getJobKey(), TriggerWrapper.TriggerState.COMPLETE);
                        this.signaler.signalSchedulingChange(0L);
                     }
                  }
               }
               break;
            } finally {
               this.unlock();
            }
         } catch (RejoinException var21) {
            try {
               Thread.sleep(this.retryInterval);
            } catch (InterruptedException var19) {
               throw new IllegalStateException("Received interrupted exception", var19);
            }
         } catch (JobPersistenceException var22) {
            try {
               Thread.sleep(this.retryInterval);
            } catch (InterruptedException var18) {
               throw new IllegalStateException("Received interrupted exception", var18);
            }
         }
      }
   }

   private void setAllTriggersOfJobToState(JobKey var1, TriggerWrapper.TriggerState var2) {
      for (TriggerWrapper var5 : this.triggerFacade.getTriggerWrappersForJob(var1)) {
         var5.setState(var2, this.terracottaClientId, this.triggerFacade);
         if (var2 != TriggerWrapper.TriggerState.WAITING) {
            this.timeTriggers.remove(var5);
         }
      }
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      this.lock();

      HashSet var1;
      try {
         var1 = new HashSet<>(this.triggerFacade.allPausedTriggersGroupNames());
      } finally {
         this.unlock();
      }

      return var1;
   }

   @Override
   public void setInstanceId(String var1) {
   }

   @Override
   public void setInstanceName(String var1) {
   }

   @Override
   public void setTcRetryInterval(long var1) {
      this.retryInterval = var1;
   }

   public void nodeLeft(ClusterEvent var1) {
      String var2 = var1.getNode().getId();

      try {
         this.lock();
      } catch (JobPersistenceException var11) {
         this.getLog().info("Job store is already disabled, not processing nodeLeft() for " + var2);
         return;
      }

      try {
         ArrayList var3 = new ArrayList();

         for (TriggerKey var5 : this.triggerFacade.allTriggerKeys()) {
            TriggerWrapper var6 = this.triggerFacade.get(var5);
            String var7 = var6.getLastTerracotaClientId();
            if (var7 != null && var7.equals(var2)) {
               var3.add(var6);
            }
         }

         for (TriggerWrapper var15 : var3) {
            this.evalOrphanedTrigger(var15, false);
         }

         Iterator var14 = this.triggerFacade.allFiredTriggers().iterator();

         while (var14.hasNext()) {
            FiredTrigger var16 = (FiredTrigger)var14.next();
            if (var2.equals(var16.getClientId())) {
               this.getLog().info("Found non-complete fired trigger: " + var16);
               var14.remove();
               TriggerWrapper var17 = this.triggerFacade.get(var16.getTriggerKey());
               if (var17 == null) {
                  this.getLog().error("no trigger found for executing trigger: " + var16.getTriggerKey());
               } else {
                  this.scheduleRecoveryIfNeeded(var17, var16);
               }
            }
         }
      } finally {
         this.unlock();
      }

      this.signaler.signalSchedulingChange(0L);
   }

   @Override
   public long getEstimatedTimeToReleaseAndAcquireTrigger() {
      return this.estimatedTimeToReleaseAndAcquireTrigger;
   }

   @Override
   public void setEstimatedTimeToReleaseAndAcquireTrigger(long var1) {
      this.estimatedTimeToReleaseAndAcquireTrigger = var1;
   }

   @Override
   public void setThreadPoolSize(int var1) {
   }

   @Override
   public boolean isClustered() {
      throw new AssertionError();
   }

   @Override
   public long getAcquireRetryDelay(int var1) {
      return this.retryInterval;
   }

   void injectTriggerWrapper(TriggerWrapper var1) {
      this.timeTriggers.add(var1);
   }

   ClusterInfo getDsoCluster() {
      return this.clusterInfo;
   }

   public void onClusterEvent(ClusterEvent var1) {
      switch (var1.getType()) {
         case NODE_JOINED:
         case OPERATIONS_DISABLED:
         case OPERATIONS_ENABLED:
         default:
            break;
         case NODE_LEFT:
            this.getLog().info("Received node left notification for " + var1.getNode().getId());
            this.nodeLeft(var1);
            break;
         case NODE_REJOINED:
            this.getLog().info("Received rejoin notification " + this.terracottaClientId + " => " + var1.getNode().getId());
            this.terracottaClientId = var1.getNode().getId();
      }
   }

   protected TriggerFacade getTriggersFacade() {
      return this.triggerFacade;
   }

   private static class LocalLockState {
      private int acquires = 0;
      private boolean disabled;

      private LocalLockState() {
      }

      synchronized void attemptAcquireBegin() {
         if (this.disabled) {
            throw new JobPersistenceException("org.terracotta.quartz.TerracottaJobStore is disabled");
         } else {
            this.acquires++;
         }
      }

      synchronized void release() {
         this.acquires--;
         this.notifyAll();
      }

      synchronized void disableLocking() {
         this.disabled = true;

         while (this.acquires > 0) {
            this.wait();
         }
      }
   }

   private static class ShutdownHook implements Runnable {
      private final DefaultClusteredJobStore store;

      ShutdownHook(DefaultClusteredJobStore var1) {
         this.store = var1;
      }

      @Override
      public void run() {
         this.store.disable();
      }
   }

   interface TriggerRemovedFromCandidateFiringListHandler {
      boolean removeCandidateTrigger(TriggerWrapper var1);
   }
}
