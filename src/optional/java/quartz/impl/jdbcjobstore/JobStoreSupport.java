package org.quartz.impl.jdbcjobstore;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.DefaultThreadExecutor;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JobStoreSupport implements JobStore, Constants {
   protected static final String LOCK_TRIGGER_ACCESS = "TRIGGER_ACCESS";
   protected static final String LOCK_STATE_ACCESS = "STATE_ACCESS";
   protected String dsName;
   protected String tablePrefix = "QRTZ_";
   protected boolean useProperties = false;
   protected String instanceId;
   protected String instanceName;
   protected String delegateClassName;
   protected String delegateInitString;
   protected Class<? extends DriverDelegate> delegateClass = StdJDBCDelegate.class;
   protected HashMap<String, Calendar> calendarCache = new HashMap<>();
   private DriverDelegate delegate;
   private long misfireThreshold = 60000L;
   private boolean dontSetAutoCommitFalse = false;
   private boolean isClustered = false;
   private boolean useDBLocks = false;
   private boolean lockOnInsert = true;
   private Semaphore lockHandler = null;
   private String selectWithLockSQL = null;
   private long clusterCheckinInterval = 7500L;
   private JobStoreSupport.ClusterManager clusterManagementThread = null;
   private JobStoreSupport.MisfireHandler misfireHandler = null;
   private ClassLoadHelper classLoadHelper;
   private SchedulerSignaler schedSignaler;
   protected int maxToRecoverAtATime = 20;
   private boolean setTxIsolationLevelSequential = false;
   private boolean acquireTriggersWithinLock = false;
   private long dbRetryInterval = 15000L;
   private boolean makeThreadsDaemons = false;
   private boolean threadsInheritInitializersClassLoadContext = false;
   private ClassLoader initializersLoader = null;
   private boolean doubleCheckLockMisfireHandler = true;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private ThreadExecutor threadExecutor = new DefaultThreadExecutor();
   private volatile boolean schedulerRunning = false;
   private volatile boolean shutdown = false;
   private static long ftrCtr = System.currentTimeMillis();
   protected ThreadLocal<Long> sigChangeForTxCompletion = new ThreadLocal<>();
   protected boolean firstCheckIn = true;
   protected long lastCheckin = System.currentTimeMillis();

   public JobStoreSupport() {
   }

   public void setDataSource(String var1) {
      this.dsName = var1;
   }

   public String getDataSource() {
      return this.dsName;
   }

   public void setTablePrefix(String var1) {
      if (var1 == null) {
         var1 = "";
      }

      this.tablePrefix = var1;
   }

   public String getTablePrefix() {
      return this.tablePrefix;
   }

   public void setUseProperties(String var1) {
      if (var1 == null) {
         var1 = "false";
      }

      this.useProperties = Boolean.valueOf(var1);
   }

   public boolean canUseProperties() {
      return this.useProperties;
   }

   @Override
   public void setInstanceId(String var1) {
      this.instanceId = var1;
   }

   public String getInstanceId() {
      return this.instanceId;
   }

   @Override
   public void setInstanceName(String var1) {
      this.instanceName = var1;
   }

   @Override
   public void setThreadPoolSize(int var1) {
   }

   public void setThreadExecutor(ThreadExecutor var1) {
      this.threadExecutor = var1;
   }

   public ThreadExecutor getThreadExecutor() {
      return this.threadExecutor;
   }

   public String getInstanceName() {
      return this.instanceName;
   }

   @Override
   public long getEstimatedTimeToReleaseAndAcquireTrigger() {
      return 70L;
   }

   public void setIsClustered(boolean var1) {
      this.isClustered = var1;
   }

   @Override
   public boolean isClustered() {
      return this.isClustered;
   }

   public long getClusterCheckinInterval() {
      return this.clusterCheckinInterval;
   }

   public void setClusterCheckinInterval(long var1) {
      this.clusterCheckinInterval = var1;
   }

   public int getMaxMisfiresToHandleAtATime() {
      return this.maxToRecoverAtATime;
   }

   public void setMaxMisfiresToHandleAtATime(int var1) {
      this.maxToRecoverAtATime = var1;
   }

   public long getDbRetryInterval() {
      return this.dbRetryInterval;
   }

   public void setDbRetryInterval(long var1) {
      this.dbRetryInterval = var1;
   }

   public void setUseDBLocks(boolean var1) {
      this.useDBLocks = var1;
   }

   public boolean getUseDBLocks() {
      return this.useDBLocks;
   }

   public boolean isLockOnInsert() {
      return this.lockOnInsert;
   }

   public void setLockOnInsert(boolean var1) {
      this.lockOnInsert = var1;
   }

   public long getMisfireThreshold() {
      return this.misfireThreshold;
   }

   public void setMisfireThreshold(long var1) {
      if (var1 < 1L) {
         throw new IllegalArgumentException("Misfirethreshold must be larger than 0");
      } else {
         this.misfireThreshold = var1;
      }
   }

   public boolean isDontSetAutoCommitFalse() {
      return this.dontSetAutoCommitFalse;
   }

   public void setDontSetAutoCommitFalse(boolean var1) {
      this.dontSetAutoCommitFalse = var1;
   }

   public boolean isTxIsolationLevelSerializable() {
      return this.setTxIsolationLevelSequential;
   }

   public void setTxIsolationLevelSerializable(boolean var1) {
      this.setTxIsolationLevelSequential = var1;
   }

   public boolean isAcquireTriggersWithinLock() {
      return this.acquireTriggersWithinLock;
   }

   public void setAcquireTriggersWithinLock(boolean var1) {
      this.acquireTriggersWithinLock = var1;
   }

   public void setDriverDelegateClass(String var1) {
      synchronized (this) {
         this.delegateClassName = var1;
      }
   }

   public String getDriverDelegateClass() {
      return this.delegateClassName;
   }

   public void setDriverDelegateInitString(String var1) {
      this.delegateInitString = var1;
   }

   public String getDriverDelegateInitString() {
      return this.delegateInitString;
   }

   public String getSelectWithLockSQL() {
      return this.selectWithLockSQL;
   }

   public void setSelectWithLockSQL(String var1) {
      this.selectWithLockSQL = var1;
   }

   protected ClassLoadHelper getClassLoadHelper() {
      return this.classLoadHelper;
   }

   public boolean getMakeThreadsDaemons() {
      return this.makeThreadsDaemons;
   }

   public void setMakeThreadsDaemons(boolean var1) {
      this.makeThreadsDaemons = var1;
   }

   public boolean isThreadsInheritInitializersClassLoadContext() {
      return this.threadsInheritInitializersClassLoadContext;
   }

   public void setThreadsInheritInitializersClassLoadContext(boolean var1) {
      this.threadsInheritInitializersClassLoadContext = var1;
   }

   public boolean getDoubleCheckLockMisfireHandler() {
      return this.doubleCheckLockMisfireHandler;
   }

   public void setDoubleCheckLockMisfireHandler(boolean var1) {
      this.doubleCheckLockMisfireHandler = var1;
   }

   @Override
   public long getAcquireRetryDelay(int var1) {
      return this.dbRetryInterval;
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) throws SchedulerConfigException {
      if (this.dsName == null) {
         throw new SchedulerConfigException("DataSource name not set.");
      } else {
         this.classLoadHelper = var1;
         if (this.isThreadsInheritInitializersClassLoadContext()) {
            this.log.info("JDBCJobStore threads will inherit ContextClassLoader of thread: " + Thread.currentThread().getName());
            this.initializersLoader = Thread.currentThread().getContextClassLoader();
         }

         this.schedSignaler = var2;
         if (this.getLockHandler() == null) {
            if (this.isClustered()) {
               this.setUseDBLocks(true);
            }

            if (this.getUseDBLocks()) {
               if (this.getDriverDelegateClass() != null
                  && this.getDriverDelegateClass().equals(MSSQLDelegate.class.getName())
                  && this.getSelectWithLockSQL() == null) {
                  String var3 = "SELECT * FROM {0}LOCKS WITH (UPDLOCK,ROWLOCK) WHERE SCHED_NAME = {1} AND LOCK_NAME = ?";
                  this.getLog().info("Detected usage of MSSQLDelegate class - defaulting 'selectWithLockSQL' to '" + var3 + "'.");
                  this.setSelectWithLockSQL(var3);
               }

               this.getLog().info("Using db table-based data access locking (synchronization).");
               this.setLockHandler(new StdRowLockSemaphore(this.getTablePrefix(), this.getInstanceName(), this.getSelectWithLockSQL()));
            } else {
               this.getLog().info("Using thread monitor-based data access locking (synchronization).");
               this.setLockHandler(new SimpleSemaphore());
            }
         }
      }
   }

   @Override
   public void schedulerStarted() throws SchedulerConfigException {
      if (this.isClustered()) {
         this.clusterManagementThread = new JobStoreSupport.ClusterManager();
         if (this.initializersLoader != null) {
            this.clusterManagementThread.setContextClassLoader(this.initializersLoader);
         }

         this.clusterManagementThread.initialize();
      } else {
         try {
            this.recoverJobs();
         } catch (SchedulerException var2) {
            throw new SchedulerConfigException("Failure occured during job recovery.", var2);
         }
      }

      this.misfireHandler = new JobStoreSupport.MisfireHandler();
      if (this.initializersLoader != null) {
         this.misfireHandler.setContextClassLoader(this.initializersLoader);
      }

      this.misfireHandler.initialize();
      this.schedulerRunning = true;
      this.getLog().debug("JobStore background threads started (as scheduler was started).");
   }

   @Override
   public void schedulerPaused() {
      this.schedulerRunning = false;
   }

   @Override
   public void schedulerResumed() {
      this.schedulerRunning = true;
   }

   @Override
   public void shutdown() {
      this.shutdown = true;
      if (this.misfireHandler != null) {
         this.misfireHandler.shutdown();

         try {
            this.misfireHandler.join();
         } catch (InterruptedException var4) {
         }
      }

      if (this.clusterManagementThread != null) {
         this.clusterManagementThread.shutdown();

         try {
            this.clusterManagementThread.join();
         } catch (InterruptedException var3) {
         }
      }

      try {
         DBConnectionManager.getInstance().shutdown(this.getDataSource());
      } catch (SQLException var2) {
         this.getLog().warn("Database connection shutdown unsuccessful.", var2);
      }

      this.getLog().debug("JobStore background threads shutdown.");
   }

   @Override
   public boolean supportsPersistence() {
      return true;
   }

   protected abstract Connection getNonManagedTXConnection() throws JobPersistenceException;

   protected Connection getAttributeRestoringConnection(Connection var1) {
      return (Connection)Proxy.newProxyInstance(
         Thread.currentThread().getContextClassLoader(), new Class[]{Connection.class}, new AttributeRestoringConnectionInvocationHandler(var1)
      );
   }

   protected Connection getConnection() throws JobPersistenceException {
      Connection var1;
      try {
         var1 = DBConnectionManager.getInstance().getConnection(this.getDataSource());
      } catch (SQLException var7) {
         throw new JobPersistenceException("Failed to obtain DB connection from data source '" + this.getDataSource() + "': " + var7.toString(), var7);
      } catch (Throwable var8) {
         throw new JobPersistenceException("Failed to obtain DB connection from data source '" + this.getDataSource() + "': " + var8.toString(), var8);
      }

      if (var1 == null) {
         throw new JobPersistenceException("Could not get connection from DataSource '" + this.getDataSource() + "'");
      } else {
         var1 = this.getAttributeRestoringConnection(var1);

         try {
            if (!this.isDontSetAutoCommitFalse()) {
               var1.setAutoCommit(false);
            }

            if (this.isTxIsolationLevelSerializable()) {
               var1.setTransactionIsolation(8);
            }
         } catch (SQLException var5) {
            this.getLog().warn("Failed to override connection auto commit/transaction isolation.", var5);
         } catch (Throwable var6) {
            try {
               var1.close();
            } catch (Throwable var4) {
            }

            throw new JobPersistenceException("Failure setting up connection.", var6);
         }

         return var1;
      }
   }

   protected void releaseLock(String var1, boolean var2) {
      if (var2) {
         try {
            this.getLockHandler().releaseLock(var1);
         } catch (LockException var4) {
            this.getLog().error("Error returning lock: " + var4.getMessage(), var4);
         }
      }
   }

   protected void recoverJobs() {
      this.executeInNonManagedTXLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1) throws NoSuchDelegateException {
            JobStoreSupport.this.recoverJobs(var1);
         }
      }, null);
   }

   protected void recoverJobs(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         int var2 = this.getDelegate().updateTriggerStatesFromOtherStates(var1, "WAITING", "ACQUIRED", "BLOCKED");
         var2 += this.getDelegate().updateTriggerStatesFromOtherStates(var1, "PAUSED", "PAUSED_BLOCKED", "PAUSED_BLOCKED");
         this.getLog().info("Freed " + var2 + " triggers from 'acquired' / 'blocked' state.");
         this.recoverMisfiredJobs(var1, true);
         List<OperableTrigger> var3 = this.getDelegate().selectTriggersForRecoveringJobs(var1);
         this.getLog().info("Recovering " + var3.size() + " jobs that were in-progress at the time of the last shut-down.");

         for (OperableTrigger var5 : var3) {
            if (this.jobExists(var1, var5.getJobKey())) {
               var5.computeFirstFireTime(null);
               this.storeTrigger(var1, var5, null, false, "WAITING", false, true);
            }
         }

         this.getLog().info("Recovery complete.");
         List<TriggerKey> var10 = this.getDelegate().selectTriggersInState(var1, "COMPLETE");

         for (TriggerKey var6 : var10) {
            this.removeTrigger(var1, var6);
         }

         this.getLog().info("Removed " + var10.size() + " 'complete' triggers.");
         int var12 = this.getDelegate().deleteFiredTriggers(var1);
         this.getLog().info("Removed " + var12 + " stale fired job entries.");
      } catch (JobPersistenceException var7) {
         throw var7;
      } catch (Exception var8) {
         throw new JobPersistenceException("Couldn't recover jobs: " + var8.getMessage(), var8);
      }
   }

   protected long getMisfireTime() {
      long var1 = System.currentTimeMillis();
      if (this.getMisfireThreshold() > 0L) {
         var1 -= this.getMisfireThreshold();
      }

      return var1 > 0L ? var1 : 0L;
   }

   protected JobStoreSupport.RecoverMisfiredJobsResult recoverMisfiredJobs(Connection var1, boolean var2) throws NoSuchDelegateException , SQLException, JobPersistenceException{
      int var3 = var2 ? -1 : this.getMaxMisfiresToHandleAtATime();
      LinkedList<TriggerKey> var4 = new LinkedList();
      long var5 = Long.MAX_VALUE;
      boolean var7 = this.getDelegate().hasMisfiredTriggersInState(var1, "WAITING", this.getMisfireTime(), var3, var4);
      if (var7) {
         this.getLog()
            .info("Handling the first " + var4.size() + " triggers that missed their scheduled fire-time.  " + "More misfired triggers remain to be processed.");
      } else {
         if (var4.size() <= 0) {
            this.getLog().debug("Found 0 triggers that missed their scheduled fire-time.");
            return JobStoreSupport.RecoverMisfiredJobsResult.NO_OP;
         }

         this.getLog().info("Handling " + var4.size() + " trigger(s) that missed their scheduled fire-time.");
      }

      for (TriggerKey var9 : var4) {
         OperableTrigger var10 = this.retrieveTrigger(var1, var9);
         if (var10 != null) {
            this.doUpdateOfMisfiredTrigger(var1, var10, false, "WAITING", var2);
            if (var10.getNextFireTime() != null && var10.getNextFireTime().getTime() < var5) {
               var5 = var10.getNextFireTime().getTime();
            }
         }
      }

      return new JobStoreSupport.RecoverMisfiredJobsResult(var7, var4.size(), var5);
   }

   protected boolean updateMisfiredTrigger(Connection var1, TriggerKey var2, String var3, boolean var4) throws JobPersistenceException {
      try {
         OperableTrigger var5 = this.retrieveTrigger(var1, var2);
         long var6 = System.currentTimeMillis();
         if (this.getMisfireThreshold() > 0L) {
            var6 -= this.getMisfireThreshold();
         }

         if (var5.getNextFireTime().getTime() > var6) {
            return false;
         } else {
            this.doUpdateOfMisfiredTrigger(var1, var5, var4, var3, false);
            return true;
         }
      } catch (Exception var8) {
         throw new JobPersistenceException("Couldn't update misfired trigger '" + var2 + "': " + var8.getMessage(), var8);
      }
   }

   private void doUpdateOfMisfiredTrigger(Connection var1, OperableTrigger var2, boolean var3, String var4, boolean var5) throws NoSuchDelegateException , ObjectAlreadyExistsException{
      Calendar var6 = null;
      if (var2.getCalendarName() != null) {
         var6 = this.retrieveCalendar(var1, var2.getCalendarName());
      }

      this.schedSignaler.notifyTriggerListenersMisfired(var2);
      var2.updateAfterMisfire(var6);
      if (var2.getNextFireTime() == null) {
         this.storeTrigger(var1, var2, null, true, "COMPLETE", var3, var5);
         this.schedSignaler.notifySchedulerListenersFinalized(var2);
      } else {
         this.storeTrigger(var1, var2, null, true, var4, var3, var5);
      }
   }

   @Override
   public void storeJobAndTrigger(final JobDetail var1, final OperableTrigger var2) {
      this.executeInLock(this.isLockOnInsert() ? "TRIGGER_ACCESS" : null, new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws ObjectAlreadyExistsException {
            JobStoreSupport.this.storeJob(var1x, var1, false);
            JobStoreSupport.this.storeTrigger(var1x, var2, var1, false, "WAITING", false, false);
         }
      });
   }

   @Override
   public void storeJob(final JobDetail var1, final boolean var2) {
      this.executeInLock(!this.isLockOnInsert() && !var2 ? null : "TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws ObjectAlreadyExistsException {
            JobStoreSupport.this.storeJob(var1x, var1, var2);
         }
      });
   }

   protected void storeJob(Connection var1, JobDetail var2, boolean var3) throws ObjectAlreadyExistsException , NoSuchDelegateException, JobPersistenceException{
      boolean var4 = this.jobExists(var1, var2.getKey());

      try {
         if (var4) {
            if (!var3) {
               throw new ObjectAlreadyExistsException(var2);
            }

            this.getDelegate().updateJobDetail(var1, var2);
         } else {
            this.getDelegate().insertJobDetail(var1, var2);
         }
      } catch (IOException var6) {
         throw new JobPersistenceException("Couldn't store job: " + var6.getMessage(), var6);
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't store job: " + var7.getMessage(), var7);
      }
   }

   protected boolean jobExists(Connection var1, JobKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().jobExists(var1, var2);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't determine job existence (" + var2 + "): " + var4.getMessage(), var4);
      }
   }

   @Override
   public void storeTrigger(final OperableTrigger var1, final boolean var2) {
      this.executeInLock(!this.isLockOnInsert() && !var2 ? null : "TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws ObjectAlreadyExistsException {
            JobStoreSupport.this.storeTrigger(var1x, var1, null, var2, "WAITING", false, false);
         }
      });
   }

   protected void storeTrigger(Connection var1, OperableTrigger var2, JobDetail var3, boolean var4, String var5, boolean var6, boolean var7) throws ObjectAlreadyExistsException , JobPersistenceException{
      boolean var8 = this.triggerExists(var1, var2.getKey());
      if (var8 && !var4) {
         throw new ObjectAlreadyExistsException(var2);
      } else {
         try {
            if (!var6) {
               boolean var9 = this.getDelegate().isTriggerGroupPaused(var1, var2.getKey().getGroup());
               if (!var9) {
                  var9 = this.getDelegate().isTriggerGroupPaused(var1, "_$_ALL_GROUPS_PAUSED_$_");
                  if (var9) {
                     this.getDelegate().insertPausedTriggerGroup(var1, var2.getKey().getGroup());
                  }
               }

               if (var9 && (var5.equals("WAITING") || var5.equals("ACQUIRED"))) {
                  var5 = "PAUSED";
               }
            }

            if (var3 == null) {
               var3 = this.retrieveJob(var1, var2.getJobKey());
            }

            if (var3 == null) {
               throw new JobPersistenceException("The job (" + var2.getJobKey() + ") referenced by the trigger does not exist.");
            } else {
               if (var3.isConcurrentExectionDisallowed() && !var7) {
                  var5 = this.checkBlockedState(var1, var3.getKey(), var5);
               }

               if (var8) {
                  this.getDelegate().updateTrigger(var1, var2, var5, var3);
               } else {
                  this.getDelegate().insertTrigger(var1, var2, var5, var3);
               }
            }
         } catch (Exception var10) {
            throw new JobPersistenceException("Couldn't store trigger '" + var2.getKey() + "' for '" + var2.getJobKey() + "' job:" + var10.getMessage(), var10);
         }
      }
   }

   protected boolean triggerExists(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().triggerExists(var1, var2);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't determine trigger existence (" + var2 + "): " + var4.getMessage(), var4);
      }
   }

   @Override
   public boolean removeJob(final JobKey var1) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.removeJob(var1x, var1) ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   protected boolean removeJob(Connection var1, JobKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         for (TriggerKey var5 : this.getDelegate().selectTriggerKeysForJob(var1, var2)) {
            this.deleteTriggerAndChildren(var1, var5);
         }

         return this.deleteJobAndChildren(var1, var2);
      } catch (SQLException var6) {
         throw new JobPersistenceException("Couldn't remove job: " + var6.getMessage(), var6);
      }
   }

   @Override
   public boolean removeJobs(final List<JobKey> var1) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            boolean var2 = true;

            for (JobKey var4 : var1) {
               var2 = JobStoreSupport.this.removeJob(var1x, var4) && var2;
            }

            return var2 ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   @Override
   public boolean removeTriggers(final List<TriggerKey> var1) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            boolean var2 = true;

            for (TriggerKey var4 : var1) {
               var2 = JobStoreSupport.this.removeTrigger(var1x, var4) && var2;
            }

            return var2 ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   @Override
   public void storeJobsAndTriggers(final Map<JobDetail, Set<? extends Trigger>> var1, final boolean var2) {
      this.executeInLock(!this.isLockOnInsert() && !var2 ? null : "TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws ObjectAlreadyExistsException {
            for (JobDetail var3 : var1.keySet()) {
               JobStoreSupport.this.storeJob(var1x, var3, var2);

               for (Trigger var5 : var1.get(var3)) {
                  JobStoreSupport.this.storeTrigger(var1x, (OperableTrigger)var5, var3, var2, "WAITING", false, false);
               }
            }
         }
      });
   }

   private boolean deleteJobAndChildren(Connection var1, JobKey var2) throws NoSuchDelegateException , SQLException{
      return this.getDelegate().deleteJobDetail(var1, var2) > 0;
   }

   private boolean deleteTriggerAndChildren(Connection var1, TriggerKey var2) throws NoSuchDelegateException , SQLException{
      return this.getDelegate().deleteTrigger(var1, var2) > 0;
   }

   @Override
   public JobDetail retrieveJob(final JobKey var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<JobDetail>() {
         @Override
         public JobDetail execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.retrieveJob(var1x, var1);
         }
      });
   }

   protected JobDetail retrieveJob(Connection var1, JobKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectJobDetail(var1, var2, this.getClassLoadHelper());
      } catch (ClassNotFoundException var4) {
         throw new JobPersistenceException("Couldn't retrieve job because a required class was not found: " + var4.getMessage(), var4);
      } catch (IOException var5) {
         throw new JobPersistenceException("Couldn't retrieve job because the BLOB couldn't be deserialized: " + var5.getMessage(), var5);
      } catch (SQLException var6) {
         throw new JobPersistenceException("Couldn't retrieve job: " + var6.getMessage(), var6);
      }
   }

   @Override
   public boolean removeTrigger(final TriggerKey var1) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.removeTrigger(var1x, var1) ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   protected boolean removeTrigger(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         JobDetail var4 = this.getDelegate().selectJobForTrigger(var1, this.getClassLoadHelper(), var2, false);
         boolean var3 = this.deleteTriggerAndChildren(var1, var2);
         if (null != var4 && !var4.isDurable()) {
            int var5 = this.getDelegate().selectNumTriggersForJob(var1, var4.getKey());
            if (var5 == 0) {
               this.deleteJobAndChildren(var1, var4.getKey());
            }
         }

         return var3;
      } catch (ClassNotFoundException var6) {
         throw new JobPersistenceException("Couldn't remove trigger: " + var6.getMessage(), var6);
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't remove trigger: " + var7.getMessage(), var7);
      }
   }

   @Override
   public boolean replaceTrigger(final TriggerKey var1, final OperableTrigger var2) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.replaceTrigger(var1x, var1, var2) ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   protected boolean replaceTrigger(Connection var1, TriggerKey var2, OperableTrigger var3) throws NoSuchDelegateException , JobPersistenceException{
      try {
         JobDetail var4 = this.getDelegate().selectJobForTrigger(var1, this.getClassLoadHelper(), var2);
         if (var4 == null) {
            return false;
         } else if (!var3.getJobKey().equals(var4.getKey())) {
            throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
         } else {
            boolean var5 = this.deleteTriggerAndChildren(var1, var2);
            this.storeTrigger(var1, var3, var4, false, "WAITING", false, false);
            return var5;
         }
      } catch (ClassNotFoundException var6) {
         throw new JobPersistenceException("Couldn't remove trigger: " + var6.getMessage(), var6);
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't remove trigger: " + var7.getMessage(), var7);
      }
   }

   @Override
   public OperableTrigger retrieveTrigger(final TriggerKey var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<OperableTrigger>() {
         @Override
         public OperableTrigger execute(Connection var1x) throws JobPersistenceException {
            return JobStoreSupport.this.retrieveTrigger(var1x, var1);
         }
      });
   }

   protected OperableTrigger retrieveTrigger(Connection var1, TriggerKey var2) throws JobPersistenceException {
      try {
         return this.getDelegate().selectTrigger(var1, var2);
      } catch (Exception var4) {
         throw new JobPersistenceException("Couldn't retrieve trigger: " + var4.getMessage(), var4);
      }
   }

   @Override
   public Trigger.TriggerState getTriggerState(final TriggerKey var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<Trigger.TriggerState>() {
         @Override
         public Trigger.TriggerState execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.getTriggerState(var1x, var1);
         }
      });
   }

   public Trigger.TriggerState getTriggerState(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         String var3 = this.getDelegate().selectTriggerState(var1, var2);
         if (var3 == null) {
            return Trigger.TriggerState.NONE;
         } else if (var3.equals("DELETED")) {
            return Trigger.TriggerState.NONE;
         } else if (var3.equals("COMPLETE")) {
            return Trigger.TriggerState.COMPLETE;
         } else if (var3.equals("PAUSED")) {
            return Trigger.TriggerState.PAUSED;
         } else if (var3.equals("PAUSED_BLOCKED")) {
            return Trigger.TriggerState.PAUSED;
         } else if (var3.equals("ERROR")) {
            return Trigger.TriggerState.ERROR;
         } else {
            return var3.equals("BLOCKED") ? Trigger.TriggerState.BLOCKED : Trigger.TriggerState.NORMAL;
         }
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't determine state of trigger (" + var2 + "): " + var4.getMessage(), var4);
      }
   }

   @Override
   public void resetTriggerFromErrorState(final TriggerKey var1) {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws NoSuchDelegateException {
            JobStoreSupport.this.resetTriggerFromErrorState(var1x, var1);
         }
      });
   }

   void resetTriggerFromErrorState(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         String var3 = "WAITING";
         if (this.getDelegate().isTriggerGroupPaused(var1, var2.getGroup())) {
            var3 = "PAUSED";
         }

         this.getDelegate().updateTriggerStateFromOtherState(var1, var2, var3, "ERROR");
         this.getLog().info("Trigger " + var2 + " reset from ERROR state to: " + var3);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't reset from error state of trigger (" + var2 + "): " + var4.getMessage(), var4);
      }
   }

   @Override
   public void storeCalendar(final String var1, final Calendar var2, final boolean var3, final boolean var4) {
      this.executeInLock(!this.isLockOnInsert() && !var4 ? null : "TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws ObjectAlreadyExistsException {
            JobStoreSupport.this.storeCalendar(var1x, var1, var2, var3, var4);
         }
      });
   }

   protected void storeCalendar(Connection var1, String var2, Calendar var3, boolean var4, boolean var5) throws ObjectAlreadyExistsException , NoSuchDelegateException, JobPersistenceException{
      try {
         boolean var6 = this.calendarExists(var1, var2);
         if (var6 && !var4) {
            throw new ObjectAlreadyExistsException("Calendar with name '" + var2 + "' already exists.");
         } else {
            if (var6) {
               if (this.getDelegate().updateCalendar(var1, var2, var3) < 1) {
                  throw new JobPersistenceException("Couldn't store calendar.  Update failed.");
               }

               if (var5) {
                  for (OperableTrigger var9 : this.getDelegate().selectTriggersForCalendar(var1, var2)) {
                     var9.updateWithNewCalendar(var3, this.getMisfireThreshold());
                     this.storeTrigger(var1, var9, null, true, "WAITING", false, false);
                  }
               }
            } else if (this.getDelegate().insertCalendar(var1, var2, var3) < 1) {
               throw new JobPersistenceException("Couldn't store calendar.  Insert failed.");
            }

            if (!this.isClustered) {
               this.calendarCache.put(var2, var3);
            }
         }
      } catch (IOException var10) {
         throw new JobPersistenceException("Couldn't store calendar because the BLOB couldn't be serialized: " + var10.getMessage(), var10);
      } catch (ClassNotFoundException var11) {
         throw new JobPersistenceException("Couldn't store calendar: " + var11.getMessage(), var11);
      } catch (SQLException var12) {
         throw new JobPersistenceException("Couldn't store calendar: " + var12.getMessage(), var12);
      }
   }

   protected boolean calendarExists(Connection var1, String var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().calendarExists(var1, var2);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't determine calendar existence (" + var2 + "): " + var4.getMessage(), var4);
      }
   }

   @Override
   public boolean removeCalendar(final String var1) {
      return this.<Boolean>executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.removeCalendar(var1x, var1) ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   protected boolean removeCalendar(Connection var1, String var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         if (this.getDelegate().calendarIsReferenced(var1, var2)) {
            throw new JobPersistenceException("Calender cannot be removed if it referenced by a trigger!");
         } else {
            if (!this.isClustered) {
               this.calendarCache.remove(var2);
            }

            return this.getDelegate().deleteCalendar(var1, var2) > 0;
         }
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't remove calendar: " + var4.getMessage(), var4);
      }
   }

   @Override
   public Calendar retrieveCalendar(final String var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<Calendar>() {
         @Override
         public Calendar execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.retrieveCalendar(var1x, var1);
         }
      });
   }

   protected Calendar retrieveCalendar(Connection var1, String var2) throws NoSuchDelegateException , JobPersistenceException{
      Calendar var3 = this.isClustered ? null : this.calendarCache.get(var2);
      if (var3 != null) {
         return var3;
      } else {
         try {
            var3 = this.getDelegate().selectCalendar(var1, var2);
            if (!this.isClustered) {
               this.calendarCache.put(var2, var3);
            }

            return var3;
         } catch (ClassNotFoundException var5) {
            throw new JobPersistenceException("Couldn't retrieve calendar because a required class was not found: " + var5.getMessage(), var5);
         } catch (IOException var6) {
            throw new JobPersistenceException("Couldn't retrieve calendar because the BLOB couldn't be deserialized: " + var6.getMessage(), var6);
         } catch (SQLException var7) {
            throw new JobPersistenceException("Couldn't retrieve calendar: " + var7.getMessage(), var7);
         }
      }
   }

   @Override
   public int getNumberOfJobs() {
      return this.<Integer>executeWithoutLock(new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getNumberOfJobs(var1);
         }
      });
   }

   protected int getNumberOfJobs(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectNumJobs(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Couldn't obtain number of jobs: " + var3.getMessage(), var3);
      }
   }

   @Override
   public int getNumberOfTriggers() {
      return this.<Integer>executeWithoutLock(new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getNumberOfTriggers(var1);
         }
      });
   }

   protected int getNumberOfTriggers(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectNumTriggers(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Couldn't obtain number of triggers: " + var3.getMessage(), var3);
      }
   }

   @Override
   public int getNumberOfCalendars() {
      return this.<Integer>executeWithoutLock(new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getNumberOfCalendars(var1);
         }
      });
   }

   protected int getNumberOfCalendars(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectNumCalendars(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Couldn't obtain number of calendars: " + var3.getMessage(), var3);
      }
   }

   @Override
   public Set<JobKey> getJobKeys(final GroupMatcher<JobKey> var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<Set<JobKey>>() {
         @Override
         public Set<JobKey> execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.getJobNames(var1x, var1);
         }
      });
   }

   protected Set<JobKey> getJobNames(Connection var1, GroupMatcher<JobKey> var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectJobsInGroup(var1, var2);
      } catch (SQLException var5) {
         throw new JobPersistenceException("Couldn't obtain job names: " + var5.getMessage(), var5);
      }
   }

   @Override
   public boolean checkExists(final JobKey var1) {
      return this.<Boolean>executeWithoutLock(new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.checkExists(var1x, var1);
         }
      });
   }

   protected boolean checkExists(Connection var1, JobKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().jobExists(var1, var2);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't check for existence of job: " + var4.getMessage(), var4);
      }
   }

   @Override
   public boolean checkExists(final TriggerKey var1) {
      return this.<Boolean>executeWithoutLock(new JobStoreSupport.TransactionCallback() {
         @Override
         public Object execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.checkExists(var1x, var1);
         }
      });
   }

   protected boolean checkExists(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().triggerExists(var1, var2);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't check for existence of job: " + var4.getMessage(), var4);
      }
   }

   @Override
   public void clearAllSchedulingData() {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1) throws NoSuchDelegateException {
            JobStoreSupport.this.clearAllSchedulingData(var1);
         }
      });
   }

   protected void clearAllSchedulingData(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         this.getDelegate().clearData(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Error clearing scheduling data: " + var3.getMessage(), var3);
      }
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<Set<TriggerKey>>() {
         @Override
         public Set<TriggerKey> execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.getTriggerNames(var1x, var1);
         }
      });
   }

   protected Set<TriggerKey> getTriggerNames(Connection var1, GroupMatcher<TriggerKey> var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectTriggersInGroup(var1, var2);
      } catch (SQLException var5) {
         throw new JobPersistenceException("Couldn't obtain trigger names: " + var5.getMessage(), var5);
      }
   }

   @Override
   public List<String> getJobGroupNames() {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<List<String>>() {
         @Override
         public List<String> execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getJobGroupNames(var1);
         }
      });
   }

   protected List<String> getJobGroupNames(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectJobGroups(var1);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't obtain job groups: " + var4.getMessage(), var4);
      }
   }

   @Override
   public List<String> getTriggerGroupNames() {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<List<String>>() {
         @Override
         public List<String> execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getTriggerGroupNames(var1);
         }
      });
   }

   protected List<String> getTriggerGroupNames(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectTriggerGroups(var1);
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't obtain trigger groups: " + var4.getMessage(), var4);
      }
   }

   @Override
   public List<String> getCalendarNames() {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<List<String>>() {
         @Override
         public List<String> execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getCalendarNames(var1);
         }
      });
   }

   protected List<String> getCalendarNames(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectCalendars(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Couldn't obtain trigger groups: " + var3.getMessage(), var3);
      }
   }

   @Override
   public List<OperableTrigger> getTriggersForJob(final JobKey var1) {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<List<OperableTrigger>>() {
         @Override
         public List<OperableTrigger> execute(Connection var1x) throws JobPersistenceException {
            return JobStoreSupport.this.getTriggersForJob(var1x, var1);
         }
      });
   }

   protected List<OperableTrigger> getTriggersForJob(Connection var1, JobKey var2) throws JobPersistenceException {
      try {
         return this.getDelegate().selectTriggersForJob(var1, var2);
      } catch (Exception var5) {
         throw new JobPersistenceException("Couldn't obtain triggers for job: " + var5.getMessage(), var5);
      }
   }

   @Override
   public void pauseTrigger(final TriggerKey var1) {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws NoSuchDelegateException {
            JobStoreSupport.this.pauseTrigger(var1x, var1);
         }
      });
   }

   public void pauseTrigger(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         String var3 = this.getDelegate().selectTriggerState(var1, var2);
         if (var3.equals("WAITING") || var3.equals("ACQUIRED")) {
            this.getDelegate().updateTriggerState(var1, var2, "PAUSED");
         } else if (var3.equals("BLOCKED")) {
            this.getDelegate().updateTriggerState(var1, var2, "PAUSED_BLOCKED");
         }
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't pause trigger '" + var2 + "': " + var4.getMessage(), var4);
      }
   }

   @Override
   public void pauseJob(final JobKey var1) {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws JobPersistenceException , NoSuchDelegateException{
            for (OperableTrigger var4 : JobStoreSupport.this.getTriggersForJob(var1x, var1)) {
               JobStoreSupport.this.pauseTrigger(var1x, var4.getKey());
            }
         }
      });
   }

   public Set<String> pauseJobs(final GroupMatcher<JobKey> var1) {
      return this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback<Set<String>>() {
         public Set<String> execute(Connection var1x) throws NoSuchDelegateException , JobPersistenceException{
            HashSet var2 = new HashSet();

            for (JobKey var5 : JobStoreSupport.this.getJobNames(var1x, var1)) {
               for (OperableTrigger var8 : JobStoreSupport.this.getTriggersForJob(var1x, var5)) {
                  JobStoreSupport.this.pauseTrigger(var1x, var8.getKey());
               }

               var2.add(var5.getGroup());
            }

            return var2;
         }
      });
   }

   protected String checkBlockedState(Connection var1, JobKey var2, String var3) throws NoSuchDelegateException , JobPersistenceException{
      if (!var3.equals("WAITING") && !var3.equals("PAUSED")) {
         return var3;
      } else {
         try {
            List var4 = this.getDelegate().selectFiredTriggerRecordsByJob(var1, var2.getName(), var2.getGroup());
            if (var4.size() > 0) {
               FiredTriggerRecord var5 = (FiredTriggerRecord)var4.get(0);
               if (var5.isJobDisallowsConcurrentExecution()) {
                  return "PAUSED".equals(var3) ? "PAUSED_BLOCKED" : "BLOCKED";
               }
            }

            return var3;
         } catch (SQLException var6) {
            throw new JobPersistenceException("Couldn't determine if trigger should be in a blocked state '" + var2 + "': " + var6.getMessage(), var6);
         }
      }
   }

   @Override
   public void resumeTrigger(final TriggerKey var1) {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws NoSuchDelegateException {
            JobStoreSupport.this.resumeTrigger(var1x, var1);
         }
      });
   }

   public void resumeTrigger(Connection var1, TriggerKey var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         TriggerStatus var3 = this.getDelegate().selectTriggerStatus(var1, var2);
         if (var3 != null && var3.getNextFireTime() != null) {
            boolean var4 = false;
            if ("PAUSED_BLOCKED".equals(var3.getStatus())) {
               var4 = true;
            }

            String var5 = this.checkBlockedState(var1, var3.getJobKey(), "WAITING");
            boolean var6 = false;
            if (this.schedulerRunning && var3.getNextFireTime().before(new Date())) {
               var6 = this.updateMisfiredTrigger(var1, var2, var5, true);
            }

            if (!var6) {
               if (var4) {
                  this.getDelegate().updateTriggerStateFromOtherState(var1, var2, var5, "PAUSED_BLOCKED");
               } else {
                  this.getDelegate().updateTriggerStateFromOtherState(var1, var2, var5, "PAUSED");
               }
            }
         }
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't resume trigger '" + var2 + "': " + var7.getMessage(), var7);
      }
   }

   @Override
   public void resumeJob(final JobKey var1) {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws JobPersistenceException , NoSuchDelegateException{
            for (OperableTrigger var4 : JobStoreSupport.this.getTriggersForJob(var1x, var1)) {
               JobStoreSupport.this.resumeTrigger(var1x, var4.getKey());
            }
         }
      });
   }

   public Set<String> resumeJobs(final GroupMatcher<JobKey> var1) {
      return this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback<Set<String>>() {
         public Set<String> execute(Connection var1x) throws NoSuchDelegateException , JobPersistenceException{
            Set<JobKey> var2 = JobStoreSupport.this.getJobNames(var1x, var1);
            HashSet var3 = new HashSet();

            for (JobKey var5 : var2) {
               for (OperableTrigger var8 : JobStoreSupport.this.getTriggersForJob(var1x, var5)) {
                  JobStoreSupport.this.resumeTrigger(var1x, var8.getKey());
               }

               var3.add(var5.getGroup());
            }

            return var3;
         }
      });
   }

   public Set<String> pauseTriggers(final GroupMatcher<TriggerKey> var1) {
      return this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback<Set<String>>() {
         public Set<String> execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.pauseTriggerGroup(var1x, var1);
         }
      });
   }

   public Set<String> pauseTriggerGroup(Connection var1, GroupMatcher<TriggerKey> var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         this.getDelegate().updateTriggerGroupStateFromOtherStates(var1, var2, "PAUSED", "ACQUIRED", "WAITING", "WAITING");
         this.getDelegate().updateTriggerGroupStateFromOtherState(var1, var2, "PAUSED_BLOCKED", "BLOCKED");
         List<String> var3 = this.getDelegate().selectTriggerGroups(var1, var2);
         StringMatcher.StringOperatorName var4 = var2.getCompareWithOperator();
         if (var4.equals(StringMatcher.StringOperatorName.EQUALS) && !var3.contains(var2.getCompareToValue())) {
            var3.add(var2.getCompareToValue());
         }

         for (String var6 : var3) {
            if (!this.getDelegate().isTriggerGroupPaused(var1, var6)) {
               this.getDelegate().insertPausedTriggerGroup(var1, var6);
            }
         }

         return new HashSet<>(var3);
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't pause trigger group '" + var2 + "': " + var7.getMessage(), var7);
      }
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      return this.executeWithoutLock(new JobStoreSupport.TransactionCallback<Set<String>>() {
         @Override
         public Set<String> execute(Connection var1) throws NoSuchDelegateException {
            return JobStoreSupport.this.getPausedTriggerGroups(var1);
         }
      });
   }

   public Set<String> getPausedTriggerGroups(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      try {
         return this.getDelegate().selectPausedTriggerGroups(var1);
      } catch (SQLException var3) {
         throw new JobPersistenceException("Couldn't determine paused trigger groups: " + var3.getMessage(), var3);
      }
   }

   public Set<String> resumeTriggers(final GroupMatcher<TriggerKey> var1) {
      return this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback<Set<String>>() {
         public Set<String> execute(Connection var1x) throws NoSuchDelegateException {
            return JobStoreSupport.this.resumeTriggerGroup(var1x, var1);
         }
      });
   }

   public Set<String> resumeTriggerGroup(Connection var1, GroupMatcher<TriggerKey> var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         this.getDelegate().deletePausedTriggerGroup(var1, var2);
         HashSet var3 = new HashSet();

         for (TriggerKey var6 : this.getDelegate().selectTriggersInGroup(var1, var2)) {
            this.resumeTrigger(var1, var6);
            var3.add(var6.getGroup());
         }

         return var3;
      } catch (SQLException var7) {
         throw new JobPersistenceException("Couldn't pause trigger group '" + var2 + "': " + var7.getMessage(), var7);
      }
   }

   @Override
   public void pauseAll() {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1) throws NoSuchDelegateException {
            JobStoreSupport.this.pauseAll(var1);
         }
      });
   }

   public void pauseAll(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      for (String var4 : this.getTriggerGroupNames(var1)) {
         this.pauseTriggerGroup(var1, GroupMatcher.triggerGroupEquals(var4));
      }

      try {
         if (!this.getDelegate().isTriggerGroupPaused(var1, "_$_ALL_GROUPS_PAUSED_$_")) {
            this.getDelegate().insertPausedTriggerGroup(var1, "_$_ALL_GROUPS_PAUSED_$_");
         }
      } catch (SQLException var5) {
         throw new JobPersistenceException("Couldn't pause all trigger groups: " + var5.getMessage(), var5);
      }
   }

   @Override
   public void resumeAll() {
      this.executeInLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1) throws NoSuchDelegateException {
            JobStoreSupport.this.resumeAll(var1);
         }
      });
   }

   public void resumeAll(Connection var1) throws NoSuchDelegateException , JobPersistenceException{
      for (String var4 : this.getTriggerGroupNames(var1)) {
         this.resumeTriggerGroup(var1, GroupMatcher.triggerGroupEquals(var4));
      }

      try {
         this.getDelegate().deletePausedTriggerGroup(var1, "_$_ALL_GROUPS_PAUSED_$_");
      } catch (SQLException var5) {
         throw new JobPersistenceException("Couldn't resume all trigger groups: " + var5.getMessage(), var5);
      }
   }

   protected synchronized String getFiredTriggerRecordId() {
      return this.getInstanceId() + ftrCtr++;
   }

   @Override
   public List<OperableTrigger> acquireNextTriggers(final long var1, final int var3, final long var4) {
      String var6;
      if (!this.isAcquireTriggersWithinLock() && var3 <= 1) {
         var6 = null;
      } else {
         var6 = "TRIGGER_ACCESS";
      }

      return this.executeInNonManagedTXLock(var6, new JobStoreSupport.TransactionCallback<List<OperableTrigger>>() {
         public List<OperableTrigger> execute(Connection var1x) throws JobPersistenceException {
            return JobStoreSupport.this.acquireNextTrigger(var1x, var1, var3, var4);
         }
      }, new JobStoreSupport.TransactionValidator<List<OperableTrigger>>() {
         public Boolean validate(Connection var1, List<OperableTrigger> var2) throws NoSuchDelegateException , JobPersistenceException{
            try {
               List<FiredTriggerRecord> var3x = JobStoreSupport.this.getDelegate().selectInstancesFiredTriggerRecords(var1, JobStoreSupport.this.getInstanceId());
               HashSet var4x = new HashSet();

               for (FiredTriggerRecord var6x : var3x) {
                  var4x.add(var6x.getFireInstanceId());
               }

               for (OperableTrigger var9 : var2) {
                  if (var4x.contains(var9.getFireInstanceId())) {
                     return true;
                  }
               }

               return false;
            } catch (SQLException var7) {
               throw new JobPersistenceException("error validating trigger acquisition", var7);
            }
         }
      });
   }

   protected List<OperableTrigger> acquireNextTrigger(Connection var1, long var2, int var4, long var5) throws JobPersistenceException {
      if (var5 < 0L) {
         throw new IllegalArgumentException();
      } else {
         ArrayList var7 = new ArrayList();
         HashSet var8 = new HashSet();
         byte var9 = 3;
         int var10 = 0;

         while (true) {
            var10++;

            try {
               List<TriggerKey> var11 = this.getDelegate().selectTriggerToAcquire(var1, var2 + var5, this.getMisfireTime(), var4);
               if (var11 != null && var11.size() != 0) {
                  long var12 = var2;

                  for (TriggerKey var15 : var11) {
                     OperableTrigger var16 = this.retrieveTrigger(var1, var15);
                     if (var16 != null) {
                        JobKey var17 = var16.getJobKey();

                        JobDetail var18;
                        try {
                           var18 = this.retrieveJob(var1, var17);
                        } catch (JobPersistenceException var22) {
                           JobPersistenceException var19 = var22;

                           try {
                              this.getLog().error("Error retrieving job, setting trigger state to ERROR.", var19);
                              this.getDelegate().updateTriggerState(var1, var15, "ERROR");
                           } catch (SQLException var21) {
                              this.getLog().error("Unable to set trigger state to ERROR.", var21);
                           }
                           continue;
                        }

                        if (var18.isConcurrentExectionDisallowed()) {
                           if (var8.contains(var17)) {
                              continue;
                           }

                           var8.add(var17);
                        }

                        if (var16.getNextFireTime().getTime() > var12) {
                           break;
                        }

                        int var24 = this.getDelegate().updateTriggerStateFromOtherState(var1, var15, "ACQUIRED", "WAITING");
                        if (var24 > 0) {
                           var16.setFireInstanceId(this.getFiredTriggerRecordId());
                           this.getDelegate().insertFiredTrigger(var1, var16, "ACQUIRED", null);
                           if (var7.isEmpty()) {
                              var12 = Math.max(var16.getNextFireTime().getTime(), System.currentTimeMillis()) + var5;
                           }

                           var7.add(var16);
                        }
                     }
                  }

                  if (var7.size() == 0 && var10 < 3) {
                     continue;
                  }

                  return var7;
               }

               return var7;
            } catch (Exception var23) {
               throw new JobPersistenceException("Couldn't acquire next trigger: " + var23.getMessage(), var23);
            }
         }
      }
   }

   @Override
   public void releaseAcquiredTrigger(final OperableTrigger var1) {
      this.retryExecuteInNonManagedTXLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws NoSuchDelegateException {
            JobStoreSupport.this.releaseAcquiredTrigger(var1x, var1);
         }
      });
   }

   protected void releaseAcquiredTrigger(Connection var1, OperableTrigger var2) throws NoSuchDelegateException , JobPersistenceException{
      try {
         this.getDelegate().updateTriggerStateFromOtherState(var1, var2.getKey(), "WAITING", "ACQUIRED");
         this.getDelegate().deleteFiredTrigger(var1, var2.getFireInstanceId());
      } catch (SQLException var4) {
         throw new JobPersistenceException("Couldn't release acquired trigger: " + var4.getMessage(), var4);
      }
   }

   @Override
   public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> var1) {
      return this.executeInNonManagedTXLock("TRIGGER_ACCESS", new JobStoreSupport.TransactionCallback<List<TriggerFiredResult>>() {
         public List<TriggerFiredResult> execute(Connection var1x) {
            ArrayList var2 = new ArrayList();

            for (OperableTrigger var5 : var1) {
               TriggerFiredResult var3;
               try {
                  TriggerFiredBundle var6 = JobStoreSupport.this.triggerFired(var1x, var5);
                  var3 = new TriggerFiredResult(var6);
               } catch (JobPersistenceException var7) {
                  var3 = new TriggerFiredResult(var7);
               } catch (RuntimeException var8) {
                  var3 = new TriggerFiredResult(var8);
               }

               var2.add(var3);
            }

            return var2;
         }
      }, new JobStoreSupport.TransactionValidator<List<TriggerFiredResult>>() {
         public Boolean validate(Connection var1, List<TriggerFiredResult> var2) throws NoSuchDelegateException , JobPersistenceException{
            try {
               List<FiredTriggerRecord> var3 = JobStoreSupport.this.getDelegate().selectInstancesFiredTriggerRecords(var1, JobStoreSupport.this.getInstanceId());
               HashSet var4 = new HashSet();

               for (FiredTriggerRecord var6 : var3) {
                  if ("EXECUTING".equals(var6.getFireInstanceState())) {
                     var4.add(var6.getFireInstanceId());
                  }
               }

               for (TriggerFiredResult var9 : var2) {
                  if (var9.getTriggerFiredBundle() != null && var4.contains(var9.getTriggerFiredBundle().getTrigger().getFireInstanceId())) {
                     return true;
                  }
               }

               return false;
            } catch (SQLException var7) {
               throw new JobPersistenceException("error validating trigger acquisition", var7);
            }
         }
      });
   }

   protected TriggerFiredBundle triggerFired(Connection var1, OperableTrigger var2) throws NoSuchDelegateException , JobPersistenceException{
      Calendar var4 = null;

      try {
         String var5 = this.getDelegate().selectTriggerState(var1, var2.getKey());
         if (!var5.equals("ACQUIRED")) {
            return null;
         }
      } catch (SQLException var12) {
         throw new JobPersistenceException("Couldn't select trigger state: " + var12.getMessage(), var12);
      }

      JobDetail var3;
      try {
         var3 = this.retrieveJob(var1, var2.getJobKey());
         if (var3 == null) {
            return null;
         }
      } catch (JobPersistenceException var13) {
         JobPersistenceException var14 = var13;

         try {
            this.getLog().error("Error retrieving job, setting trigger state to ERROR.", var14);
            this.getDelegate().updateTriggerState(var1, var2.getKey(), "ERROR");
         } catch (SQLException var9) {
            this.getLog().error("Unable to set trigger state to ERROR.", var9);
         }

         throw var13;
      }

      if (var2.getCalendarName() != null) {
         var4 = this.retrieveCalendar(var1, var2.getCalendarName());
         if (var4 == null) {
            return null;
         }
      }

      try {
         this.getDelegate().updateFiredTrigger(var1, var2, "EXECUTING", var3);
      } catch (SQLException var11) {
         throw new JobPersistenceException("Couldn't insert fired trigger: " + var11.getMessage(), var11);
      }

      Date var15 = var2.getPreviousFireTime();
      var2.triggered(var4);
      String var6 = "WAITING";
      boolean var7 = true;
      if (var3.isConcurrentExectionDisallowed()) {
         var6 = "BLOCKED";
         var7 = false;

         try {
            this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var3.getKey(), "BLOCKED", "WAITING");
            this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var3.getKey(), "BLOCKED", "ACQUIRED");
            this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var3.getKey(), "PAUSED_BLOCKED", "PAUSED");
         } catch (SQLException var10) {
            throw new JobPersistenceException("Couldn't update states of blocked triggers: " + var10.getMessage(), var10);
         }
      }

      if (var2.getNextFireTime() == null) {
         var6 = "COMPLETE";
         var7 = true;
      }

      this.storeTrigger(var1, var2, var3, true, var6, var7, false);
      var3.getJobDataMap().clearDirtyFlag();
      return new TriggerFiredBundle(
         var3, var2, var4, var2.getKey().getGroup().equals("RECOVERING_JOBS"), new Date(), var2.getPreviousFireTime(), var15, var2.getNextFireTime()
      );
   }

   @Override
   public void triggeredJobComplete(final OperableTrigger var1, final JobDetail var2, final Trigger.CompletedExecutionInstruction var3) {
      this.retryExecuteInNonManagedTXLock("TRIGGER_ACCESS", new JobStoreSupport.VoidTransactionCallback() {
         @Override
         public void executeVoid(Connection var1x) throws NoSuchDelegateException {
            JobStoreSupport.this.triggeredJobComplete(var1x, var1, var2, var3);
         }
      });
   }

   protected void triggeredJobComplete(Connection var1, OperableTrigger var2, JobDetail var3, Trigger.CompletedExecutionInstruction var4) throws NoSuchDelegateException , JobPersistenceException{
      try {
         if (var4 == Trigger.CompletedExecutionInstruction.DELETE_TRIGGER) {
            if (var2.getNextFireTime() == null) {
               TriggerStatus var5 = this.getDelegate().selectTriggerStatus(var1, var2.getKey());
               if (var5 != null && var5.getNextFireTime() == null) {
                  this.removeTrigger(var1, var2.getKey());
               }
            } else {
               this.removeTrigger(var1, var2.getKey());
               this.signalSchedulingChangeOnTxCompletion(0L);
            }
         } else if (var4 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
            this.getDelegate().updateTriggerState(var1, var2.getKey(), "COMPLETE");
            this.signalSchedulingChangeOnTxCompletion(0L);
         } else if (var4 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
            this.getLog().info("Trigger " + var2.getKey() + " set to ERROR state.");
            this.getDelegate().updateTriggerState(var1, var2.getKey(), "ERROR");
            this.signalSchedulingChangeOnTxCompletion(0L);
         } else if (var4 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
            this.getDelegate().updateTriggerStatesForJob(var1, var2.getJobKey(), "COMPLETE");
            this.signalSchedulingChangeOnTxCompletion(0L);
         } else if (var4 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
            this.getLog().info("All triggers of Job " + var2.getKey() + " set to ERROR state.");
            this.getDelegate().updateTriggerStatesForJob(var1, var2.getJobKey(), "ERROR");
            this.signalSchedulingChangeOnTxCompletion(0L);
         }

         if (var3.isConcurrentExectionDisallowed()) {
            this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var3.getKey(), "WAITING", "BLOCKED");
            this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var3.getKey(), "PAUSED", "PAUSED_BLOCKED");
            this.signalSchedulingChangeOnTxCompletion(0L);
         }

         if (var3.isPersistJobDataAfterExecution()) {
            try {
               if (var3.getJobDataMap().isDirty()) {
                  this.getDelegate().updateJobData(var1, var3);
               }
            } catch (IOException var7) {
               throw new JobPersistenceException("Couldn't serialize job data: " + var7.getMessage(), var7);
            } catch (SQLException var8) {
               throw new JobPersistenceException("Couldn't update job data: " + var8.getMessage(), var8);
            }
         }
      } catch (SQLException var9) {
         throw new JobPersistenceException("Couldn't update trigger state(s): " + var9.getMessage(), var9);
      }

      try {
         this.getDelegate().deleteFiredTrigger(var1, var2.getFireInstanceId());
      } catch (SQLException var6) {
         throw new JobPersistenceException("Couldn't delete fired trigger: " + var6.getMessage(), var6);
      }
   }

   protected DriverDelegate getDelegate() throws NoSuchDelegateException {
      synchronized (this) {
         if (null == this.delegate) {
            try {
               if (this.delegateClassName != null) {
                  this.delegateClass = this.getClassLoadHelper().loadClass(this.delegateClassName, DriverDelegate.class);
               }

               this.delegate = this.delegateClass.newInstance();
               this.delegate
                  .initialize(
                     this.getLog(),
                     this.tablePrefix,
                     this.instanceName,
                     this.instanceId,
                     this.getClassLoadHelper(),
                     this.canUseProperties(),
                     this.getDriverDelegateInitString()
                  );
            } catch (InstantiationException var4) {
               throw new NoSuchDelegateException("Couldn't create delegate: " + var4.getMessage(), var4);
            } catch (IllegalAccessException var5) {
               throw new NoSuchDelegateException("Couldn't create delegate: " + var5.getMessage(), var5);
            } catch (ClassNotFoundException var6) {
               throw new NoSuchDelegateException("Couldn't load delegate class: " + var6.getMessage(), var6);
            }
         }

         return this.delegate;
      }
   }

   protected Semaphore getLockHandler() {
      return this.lockHandler;
   }

   public void setLockHandler(Semaphore var1) {
      this.lockHandler = var1;
   }

   protected JobStoreSupport.RecoverMisfiredJobsResult doRecoverMisfires() throws JobPersistenceException {
      boolean var1 = false;
      Connection var2 = this.getNonManagedTXConnection();

      JobStoreSupport.RecoverMisfiredJobsResult var5;
      try {
         JobStoreSupport.RecoverMisfiredJobsResult var3 = JobStoreSupport.RecoverMisfiredJobsResult.NO_OP;
         int var4 = this.getDoubleCheckLockMisfireHandler()
            ? this.getDelegate().countMisfiredTriggersInState(var2, "WAITING", this.getMisfireTime())
            : Integer.MAX_VALUE;
         if (var4 == 0) {
            this.getLog().debug("Found 0 triggers that missed their scheduled fire-time.");
         } else {
            var1 = this.getLockHandler().obtainLock(var2, "TRIGGER_ACCESS");
            var3 = this.recoverMisfiredJobs(var2, false);
         }

         this.commitConnection(var2);
         var5 = var3;
      } catch (JobPersistenceException var28) {
         this.rollbackConnection(var2);
         throw var28;
      } catch (SQLException var29) {
         this.rollbackConnection(var2);
         throw new JobPersistenceException("Database error recovering from misfires.", var29);
      } catch (RuntimeException var30) {
         this.rollbackConnection(var2);
         throw new JobPersistenceException("Unexpected runtime exception: " + var30.getMessage(), var30);
      } finally {
         try {
            this.releaseLock("TRIGGER_ACCESS", var1);
         } finally {
            this.cleanupConnection(var2);
         }
      }

      return var5;
   }

   protected void signalSchedulingChangeOnTxCompletion(long var1) {
      Long var3 = this.sigChangeForTxCompletion.get();
      if (var3 == null && var1 >= 0L) {
         this.sigChangeForTxCompletion.set(var1);
      } else if (var3 == null || var1 < var3) {
         this.sigChangeForTxCompletion.set(var1);
      }
   }

   protected Long clearAndGetSignalSchedulingChangeOnTxCompletion() {
      Long var1 = this.sigChangeForTxCompletion.get();
      this.sigChangeForTxCompletion.set(null);
      return var1;
   }

   protected void signalSchedulingChangeImmediately(long var1) {
      this.schedSignaler.signalSchedulingChange(var1);
   }

   protected boolean doCheckin() throws JobPersistenceException , LockException{
      boolean var1 = false;
      boolean var2 = false;
      boolean var3 = false;
      Connection var4 = this.getNonManagedTXConnection();

      try {
         List var5 = null;
         if (!this.firstCheckIn) {
            var5 = this.clusterCheckIn(var4);
            this.commitConnection(var4);
         }

         if (this.firstCheckIn || var5.size() > 0) {
            this.getLockHandler().obtainLock(var4, "STATE_ACCESS");
            var2 = true;
            var5 = this.firstCheckIn ? this.clusterCheckIn(var4) : this.findFailedInstances(var4);
            if (var5.size() > 0) {
               this.getLockHandler().obtainLock(var4, "TRIGGER_ACCESS");
               var1 = true;
               this.clusterRecover(var4, var5);
               var3 = true;
            }
         }

         this.commitConnection(var4);
      } catch (JobPersistenceException var66) {
         this.rollbackConnection(var4);
         throw var66;
      } finally {
         try {
            this.releaseLock("TRIGGER_ACCESS", var1);
         } finally {
            try {
               this.releaseLock("STATE_ACCESS", var2);
            } finally {
               this.cleanupConnection(var4);
            }
         }
      }

      this.firstCheckIn = false;
      return var3;
   }

   protected List<SchedulerStateRecord> findFailedInstances(Connection var1) throws JobPersistenceException {
      try {
         LinkedList var2 = new LinkedList();
         boolean var3 = false;
         long var4 = System.currentTimeMillis();
         List<SchedulerStateRecord> var6 = this.getDelegate().selectSchedulerStateRecords(var1, null);

         for (SchedulerStateRecord var8 : var6) {
            if (var8.getSchedulerInstanceId().equals(this.getInstanceId())) {
               var3 = true;
               if (this.firstCheckIn) {
                  var2.add(var8);
               }
            } else if (this.calcFailedIfAfter(var8) < var4) {
               var2.add(var8);
            }
         }

         if (this.firstCheckIn) {
            var2.addAll(this.findOrphanedFailedInstances(var1, var6));
         }

         if (!var3 && !this.firstCheckIn) {
            this.getLog()
               .warn(
                  "This scheduler instance ("
                     + this.getInstanceId()
                     + ") is still "
                     + "active but was recovered by another instance in the cluster.  "
                     + "This may cause inconsistent behavior."
               );
         }

         return var2;
      } catch (Exception var9) {
         this.lastCheckin = System.currentTimeMillis();
         throw new JobPersistenceException("Failure identifying failed instances when checking-in: " + var9.getMessage(), var9);
      }
   }

   private List<SchedulerStateRecord> findOrphanedFailedInstances(Connection var1, List<SchedulerStateRecord> var2) throws NoSuchDelegateException , SQLException{
      ArrayList var3 = new ArrayList();
      Set<String> var4 = this.getDelegate().selectFiredTriggerInstanceNames(var1);
      if (!var4.isEmpty()) {
         for (SchedulerStateRecord var6 : var2) {
            var4.remove(var6.getSchedulerInstanceId());
         }

         for (String var9 : var4) {
            SchedulerStateRecord var7 = new SchedulerStateRecord();
            var7.setSchedulerInstanceId(var9);
            var3.add(var7);
            this.getLog().warn("Found orphaned fired triggers for instance: " + var7.getSchedulerInstanceId());
         }
      }

      return var3;
   }

   protected long calcFailedIfAfter(SchedulerStateRecord var1) {
      return var1.getCheckinTimestamp() + Math.max(var1.getCheckinInterval(), System.currentTimeMillis() - this.lastCheckin) + 7500L;
   }

   protected List<SchedulerStateRecord> clusterCheckIn(Connection var1) throws JobPersistenceException {
      List var2 = this.findFailedInstances(var1);

      try {
         this.lastCheckin = System.currentTimeMillis();
         if (this.getDelegate().updateSchedulerState(var1, this.getInstanceId(), this.lastCheckin) == 0) {
            this.getDelegate().insertSchedulerState(var1, this.getInstanceId(), this.lastCheckin, this.getClusterCheckinInterval());
         }

         return var2;
      } catch (Exception var4) {
         throw new JobPersistenceException("Failure updating scheduler state when checking-in: " + var4.getMessage(), var4);
      }
   }

   protected void clusterRecover(Connection var1, List<SchedulerStateRecord> var2) throws JobPersistenceException {
      if (var2.size() > 0) {
         long var3 = System.currentTimeMillis();
         this.logWarnIfNonZero(var2.size(), "ClusterManager: detected " + var2.size() + " failed or restarted instances.");

         try {
            for (SchedulerStateRecord var6 : var2) {
               this.getLog().info("ClusterManager: Scanning for instance \"" + var6.getSchedulerInstanceId() + "\"'s failed in-progress jobs.");
               List<FiredTriggerRecord> var7 = this.getDelegate().selectInstancesFiredTriggerRecords(var1, var6.getSchedulerInstanceId());
               int var8 = 0;
               int var9 = 0;
               int var10 = 0;
               HashSet<TriggerKey> var11 = new HashSet<TriggerKey>();

               for (FiredTriggerRecord var13 : var7) {
                  TriggerKey var14 = var13.getTriggerKey();
                  JobKey var15 = var13.getJobKey();
                  var11.add(var14);
                  if (var13.getFireInstanceState().equals("BLOCKED")) {
                     this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var15, "WAITING", "BLOCKED");
                  } else if (var13.getFireInstanceState().equals("PAUSED_BLOCKED")) {
                     this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var15, "PAUSED", "PAUSED_BLOCKED");
                  }

                  if (var13.getFireInstanceState().equals("ACQUIRED")) {
                     this.getDelegate().updateTriggerStateFromOtherState(var1, var14, "WAITING", "ACQUIRED");
                     var8++;
                  } else if (var13.isJobRequestsRecovery()) {
                     if (this.jobExists(var1, var15)) {
                        SimpleTriggerImpl var16 = new SimpleTriggerImpl(
                           "recover_" + var6.getSchedulerInstanceId() + "_" + var3++, "RECOVERING_JOBS", new Date(var13.getScheduleTimestamp())
                        );
                        var16.setJobName(var15.getName());
                        var16.setJobGroup(var15.getGroup());
                        var16.setMisfireInstruction(-1);
                        var16.setPriority(var13.getPriority());
                        JobDataMap var17 = this.getDelegate().selectTriggerJobDataMap(var1, var14.getName(), var14.getGroup());
                        var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_NAME", var14.getName());
                        var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_GROUP", var14.getGroup());
                        var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var13.getFireTimestamp()));
                        var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var13.getScheduleTimestamp()));
                        var16.setJobDataMap(var17);
                        var16.computeFirstFireTime(null);
                        this.storeTrigger(var1, var16, null, false, "WAITING", false, true);
                        var9++;
                     } else {
                        this.getLog().warn("ClusterManager: failed job '" + var15 + "' no longer exists, cannot schedule recovery.");
                        var10++;
                     }
                  } else {
                     var10++;
                  }

                  if (var13.isJobDisallowsConcurrentExecution()) {
                     this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var15, "WAITING", "BLOCKED");
                     this.getDelegate().updateTriggerStatesForJobFromOtherState(var1, var15, "PAUSED", "PAUSED_BLOCKED");
                  }
               }

               this.getDelegate().deleteFiredTriggers(var1, var6.getSchedulerInstanceId());
               int var19 = 0;

               for (TriggerKey var21 : var11) {
                  if (this.getDelegate().selectTriggerState(var1, var21).equals("COMPLETE")) {
                     List var22 = this.getDelegate().selectFiredTriggerRecords(var1, var21.getName(), var21.getGroup());
                     if (var22.isEmpty() && this.removeTrigger(var1, var21)) {
                        var19++;
                     }
                  }
               }

               this.logWarnIfNonZero(var8, "ClusterManager: ......Freed " + var8 + " acquired trigger(s).");
               this.logWarnIfNonZero(var19, "ClusterManager: ......Deleted " + var19 + " complete triggers(s).");
               this.logWarnIfNonZero(var9, "ClusterManager: ......Scheduled " + var9 + " recoverable job(s) for recovery.");
               this.logWarnIfNonZero(var10, "ClusterManager: ......Cleaned-up " + var10 + " other failed job(s).");
               if (!var6.getSchedulerInstanceId().equals(this.getInstanceId())) {
                  this.getDelegate().deleteSchedulerState(var1, var6.getSchedulerInstanceId());
               }
            }
         } catch (Throwable var18) {
            throw new JobPersistenceException("Failure recovering jobs: " + var18.getMessage(), var18);
         }
      }
   }

   protected void logWarnIfNonZero(int var1, String var2) {
      if (var1 > 0) {
         this.getLog().info(var2);
      } else {
         this.getLog().debug(var2);
      }
   }

   protected void cleanupConnection(Connection var1) {
      if (var1 != null) {
         if (var1 instanceof Proxy) {
            Proxy var2 = (Proxy)var1;
            InvocationHandler var3 = Proxy.getInvocationHandler(var2);
            if (var3 instanceof AttributeRestoringConnectionInvocationHandler) {
               AttributeRestoringConnectionInvocationHandler var4 = (AttributeRestoringConnectionInvocationHandler)var3;
               var4.restoreOriginalAtributes();
               this.closeConnection(var4.getWrappedConnection());
               return;
            }
         }

         this.closeConnection(var1);
      }
   }

   protected void closeConnection(Connection var1) {
      if (var1 != null) {
         try {
            var1.close();
         } catch (SQLException var3) {
            this.getLog().error("Failed to close Connection", var3);
         } catch (Throwable var4) {
            this.getLog().error("Unexpected exception closing Connection.  This is often due to a Connection being returned after or during shutdown.", var4);
         }
      }
   }

   protected void rollbackConnection(Connection var1) {
      if (var1 != null) {
         try {
            var1.rollback();
         } catch (SQLException var3) {
            this.getLog().error("Couldn't rollback jdbc connection. " + var3.getMessage(), var3);
         }
      }
   }

   protected void commitConnection(Connection var1) throws JobPersistenceException {
      if (var1 != null) {
         try {
            var1.commit();
         } catch (SQLException var3) {
            throw new JobPersistenceException("Couldn't commit jdbc connection. " + var3.getMessage(), var3);
         }
      }
   }

   public <T> T executeWithoutLock(JobStoreSupport.TransactionCallback<T> var1) {
      return this.executeInLock(null, var1);
   }

   protected abstract <T> T executeInLock(String var1, JobStoreSupport.TransactionCallback<T> var2);

   protected <T> T retryExecuteInNonManagedTXLock(String var1, JobStoreSupport.TransactionCallback<T> var2) {
      for (int var3 = 1; !this.shutdown; var3++) {
         try {
            return this.executeInNonManagedTXLock(var1, var2, null);
         } catch (JobPersistenceException var6) {
            if (var3 % 4 == 0) {
               this.schedSignaler.notifySchedulerListenersError("An error occurred while " + var2, var6);
            }
         } catch (RuntimeException var7) {
            this.getLog().error("retryExecuteInNonManagedTXLock: RuntimeException " + var7.getMessage(), var7);
         }

         try {
            Thread.sleep(this.getDbRetryInterval());
         } catch (InterruptedException var5) {
            throw new IllegalStateException("Received interrupted exception", var5);
         }
      }

      throw new IllegalStateException("JobStore is shutdown - aborting retry");
   }

   protected <T> T executeInNonManagedTXLock(String var1, JobStoreSupport.TransactionCallback<T> var2, final JobStoreSupport.TransactionValidator<T> var3) {
      boolean var4 = false;
      Connection var5 = null;

      Object var8;
      try {
         if (var1 != null) {
            if (this.getLockHandler().requiresConnection()) {
               var5 = this.getNonManagedTXConnection();
            }

            var4 = this.getLockHandler().obtainLock(var5, var1);
         }

         if (var5 == null) {
            var5 = this.getNonManagedTXConnection();
         }

         final T var6 = var2.execute(var5);

         try {
            this.commitConnection(var5);
         } catch (JobPersistenceException var31) {
            this.rollbackConnection(var5);
            if (var3 == null || !this.retryExecuteInNonManagedTXLock(var1, new JobStoreSupport.TransactionCallback<Boolean>() {
               public Boolean execute(Connection var1) throws JobPersistenceException {
                  return var3.validate(var1, var6);
               }
            })) {
               throw var31;
            }
         }

         Long var7 = this.clearAndGetSignalSchedulingChangeOnTxCompletion();
         if (var7 != null && var7 >= 0L) {
            this.signalSchedulingChangeImmediately(var7);
         }

         var8 = var6;
      } catch (JobPersistenceException var32) {
         this.rollbackConnection(var5);
         throw var32;
      } catch (RuntimeException var33) {
         this.rollbackConnection(var5);
         throw new JobPersistenceException("Unexpected runtime exception: " + var33.getMessage(), var33);
      } finally {
         try {
            this.releaseLock(var1, var4);
         } finally {
            this.cleanupConnection(var5);
         }
      }

      return (T)var8;
   }

   class ClusterManager extends Thread {
      private volatile boolean shutdown = false;
      private int numFails = 0;

      ClusterManager() {
         this.setPriority(7);
         this.setName("QuartzScheduler_" + JobStoreSupport.this.instanceName + "-" + JobStoreSupport.this.instanceId + "_ClusterManager");
         this.setDaemon(JobStoreSupport.this.getMakeThreadsDaemons());
      }

      public void initialize() {
         this.manage();
         ThreadExecutor var1 = JobStoreSupport.this.getThreadExecutor();
         var1.execute(this);
      }

      public void shutdown() {
         this.shutdown = true;
         this.interrupt();
      }

      private boolean manage() {
         boolean var1 = false;

         try {
            var1 = JobStoreSupport.this.doCheckin();
            this.numFails = 0;
            JobStoreSupport.this.getLog().debug("ClusterManager: Check-in complete.");
         } catch (Exception var3) {
            if (this.numFails % 4 == 0) {
               JobStoreSupport.this.getLog().error("ClusterManager: Error managing cluster: " + var3.getMessage(), var3);
            }

            this.numFails++;
         }

         return var1;
      }

      @Override
      public void run() {
         while (!this.shutdown) {
            if (!this.shutdown) {
               long var1 = JobStoreSupport.this.getClusterCheckinInterval();
               long var3 = System.currentTimeMillis() - JobStoreSupport.this.lastCheckin;
               var1 -= var3;
               if (var1 <= 0L) {
                  var1 = 100L;
               }

               if (this.numFails > 0) {
                  var1 = Math.max(JobStoreSupport.this.getDbRetryInterval(), var1);
               }

               try {
                  Thread.sleep(var1);
               } catch (Exception var6) {
               }
            }

            if (!this.shutdown && this.manage()) {
               JobStoreSupport.this.signalSchedulingChangeImmediately(0L);
            }
         }
      }
   }

   class MisfireHandler extends Thread {
      private volatile boolean shutdown = false;
      private int numFails = 0;

      MisfireHandler() {
         this.setName("QuartzScheduler_" + JobStoreSupport.this.instanceName + "-" + JobStoreSupport.this.instanceId + "_MisfireHandler");
         this.setDaemon(JobStoreSupport.this.getMakeThreadsDaemons());
      }

      public void initialize() {
         ThreadExecutor var1 = JobStoreSupport.this.getThreadExecutor();
         var1.execute(this);
      }

      public void shutdown() {
         this.shutdown = true;
         this.interrupt();
      }

      private JobStoreSupport.RecoverMisfiredJobsResult manage() {
         try {
            JobStoreSupport.this.getLog().debug("MisfireHandler: scanning for misfires...");
            JobStoreSupport.RecoverMisfiredJobsResult var1 = JobStoreSupport.this.doRecoverMisfires();
            this.numFails = 0;
            return var1;
         } catch (Exception var2) {
            if (this.numFails % 4 == 0) {
               JobStoreSupport.this.getLog().error("MisfireHandler: Error handling misfires: " + var2.getMessage(), var2);
            }

            this.numFails++;
            return JobStoreSupport.RecoverMisfiredJobsResult.NO_OP;
         }
      }

      @Override
      public void run() {
         while (!this.shutdown) {
            long var1 = System.currentTimeMillis();
            JobStoreSupport.RecoverMisfiredJobsResult var3 = this.manage();
            if (var3.getProcessedMisfiredTriggerCount() > 0) {
               JobStoreSupport.this.signalSchedulingChangeImmediately(var3.getEarliestNewTime());
            }

            if (!this.shutdown) {
               long var4 = 50L;
               if (!var3.hasMoreMisfiredTriggers()) {
                  var4 = JobStoreSupport.this.getMisfireThreshold() - (System.currentTimeMillis() - var1);
                  if (var4 <= 0L) {
                     var4 = 50L;
                  }

                  if (this.numFails > 0) {
                     var4 = Math.max(JobStoreSupport.this.getDbRetryInterval(), var4);
                  }
               }

               try {
                  Thread.sleep(var4);
               } catch (Exception var7) {
               }
            }
         }
      }
   }

   protected static class RecoverMisfiredJobsResult {
      public static final JobStoreSupport.RecoverMisfiredJobsResult NO_OP = new JobStoreSupport.RecoverMisfiredJobsResult(false, 0, Long.MAX_VALUE);
      private boolean _hasMoreMisfiredTriggers;
      private int _processedMisfiredTriggerCount;
      private long _earliestNewTime;

      public RecoverMisfiredJobsResult(boolean var1, int var2, long var3) {
         this._hasMoreMisfiredTriggers = var1;
         this._processedMisfiredTriggerCount = var2;
         this._earliestNewTime = var3;
      }

      public boolean hasMoreMisfiredTriggers() {
         return this._hasMoreMisfiredTriggers;
      }

      public int getProcessedMisfiredTriggerCount() {
         return this._processedMisfiredTriggerCount;
      }

      public long getEarliestNewTime() {
         return this._earliestNewTime;
      }
   }

   protected interface TransactionCallback<T> {
      T execute(Connection var1) throws JobPersistenceException;
   }

   protected interface TransactionValidator<T> {
      Boolean validate(Connection var1, T var2) throws JobPersistenceException;
   }

   protected abstract class VoidTransactionCallback implements JobStoreSupport.TransactionCallback<Void> {
      protected VoidTransactionCallback() {
      }

      public final Void execute(Connection var1) throws JobPersistenceException {
         try {
            this.executeVoid(var1);
            return null;
         } catch (JobPersistenceException var3) {
            throw var3;
         } catch (Exception var3) {
            throw new JobPersistenceException(var3.getMessage(), var3);
         }
      }

      abstract void executeVoid(Connection var1) throws Exception;
   }
}
