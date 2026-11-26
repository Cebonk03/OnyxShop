package org.quartz.core;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.quartz.Calendar;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.jmx.QuartzSchedulerMBean;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.ThreadExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzScheduler implements RemotableQuartzScheduler {
   private static String VERSION_MAJOR = "UNKNOWN";
   private static String VERSION_MINOR = "UNKNOWN";
   private static String VERSION_ITERATION = "UNKNOWN";
   private QuartzSchedulerResources resources;
   private QuartzSchedulerThread schedThread;
   private ThreadGroup threadGroup;
   private SchedulerContext context = new SchedulerContext();
   private ListenerManager listenerManager = new ListenerManagerImpl();
   private HashMap<String, JobListener> internalJobListeners = new HashMap<>(10);
   private HashMap<String, TriggerListener> internalTriggerListeners = new HashMap<>(10);
   private ArrayList<SchedulerListener> internalSchedulerListeners = new ArrayList<>(10);
   private JobFactory jobFactory = new PropertySettingJobFactory();
   ExecutingJobsManager jobMgr = null;
   ErrorLogger errLogger = null;
   private SchedulerSignaler signaler;
   private Random random = new Random();
   private ArrayList<Object> holdToPreventGC = new ArrayList<>(5);
   private boolean signalOnSchedulingChange = true;
   private volatile boolean closed = false;
   private volatile boolean shuttingDown = false;
   private boolean boundRemotely = false;
   private QuartzSchedulerMBean jmxBean = null;
   private Date initialStart = null;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public QuartzScheduler(QuartzSchedulerResources var1, long var2, @Deprecated long var4) {
      this.resources = var1;
      if (var1.getJobStore() instanceof JobListener) {
         this.addInternalJobListener((JobListener)var1.getJobStore());
      }

      this.schedThread = new QuartzSchedulerThread(this, var1);
      ThreadExecutor var6 = var1.getThreadExecutor();
      var6.execute(this.schedThread);
      if (var2 > 0L) {
         this.schedThread.setIdleWaitTime(var2);
      }

      this.jobMgr = new ExecutingJobsManager();
      this.addInternalJobListener(this.jobMgr);
      this.errLogger = new ErrorLogger();
      this.addInternalSchedulerListener(this.errLogger);
      this.signaler = new SchedulerSignalerImpl(this, this.schedThread);
      this.getLog().info("Quartz Scheduler v." + this.getVersion() + " created.");
   }

   public void initialize() throws SchedulerException {
      try {
         this.bind();
      } catch (Exception var3) {
         throw new SchedulerException("Unable to bind scheduler to RMI Registry.", var3);
      }

      if (this.resources.getJMXExport()) {
         try {
            this.registerJMX();
         } catch (Exception var2) {
            throw new SchedulerException("Unable to register scheduler with MBeanServer.", var2);
         }
      }

      this.getLog()
         .info(
            "Scheduler meta-data: "
               + new SchedulerMetaData(
                     this.getSchedulerName(),
                     this.getSchedulerInstanceId(),
                     this.getClass(),
                     this.boundRemotely,
                     this.runningSince() != null,
                     this.isInStandbyMode(),
                     this.isShutdown(),
                     this.runningSince(),
                     this.numJobsExecuted(),
                     this.getJobStoreClass(),
                     this.supportsPersistence(),
                     this.isClustered(),
                     this.getThreadPoolClass(),
                     this.getThreadPoolSize(),
                     this.getVersion()
                  )
                  .toString()
         );
   }

   @Override
   public String getVersion() {
      return getVersionMajor() + "." + getVersionMinor() + "." + getVersionIteration();
   }

   public static String getVersionMajor() {
      return VERSION_MAJOR;
   }

   public static String getVersionMinor() {
      return VERSION_MINOR;
   }

   public static String getVersionIteration() {
      return VERSION_ITERATION;
   }

   public SchedulerSignaler getSchedulerSignaler() {
      return this.signaler;
   }

   public Logger getLog() {
      return this.log;
   }

   private void registerJMX() throws Exception {
      String var1 = this.resources.getJMXObjectName();
      MBeanServer var2 = ManagementFactory.getPlatformMBeanServer();
      this.jmxBean = new QuartzSchedulerMBeanImpl(this);
      var2.registerMBean(this.jmxBean, new ObjectName(var1));
   }

   private void unregisterJMX() throws Exception {
      String var1 = this.resources.getJMXObjectName();
      MBeanServer var2 = ManagementFactory.getPlatformMBeanServer();
      var2.unregisterMBean(new ObjectName(var1));
      this.jmxBean.setSampledStatisticsEnabled(false);
      this.getLog().info("Scheduler unregistered from name '" + var1 + "' in the local MBeanServer.");
   }

   private void bind() throws Exception {
      String var1 = this.resources.getRMIRegistryHost();
      if (var1 != null && var1.length() != 0) {
         Object var2 = null;
         if (this.resources.getRMIServerPort() > 0) {
            var2 = (RemotableQuartzScheduler)UnicastRemoteObject.exportObject(this, this.resources.getRMIServerPort());
         } else {
            var2 = (RemotableQuartzScheduler)UnicastRemoteObject.exportObject(this);
         }

         Registry var3 = null;
         if (this.resources.getRMICreateRegistryStrategy().equals("as_needed")) {
            try {
               var3 = LocateRegistry.getRegistry(this.resources.getRMIRegistryPort());
               var3.list();
            } catch (Exception var6) {
               var3 = LocateRegistry.createRegistry(this.resources.getRMIRegistryPort());
            }
         } else if (this.resources.getRMICreateRegistryStrategy().equals("always")) {
            try {
               var3 = LocateRegistry.createRegistry(this.resources.getRMIRegistryPort());
            } catch (Exception var5) {
               var3 = LocateRegistry.getRegistry(this.resources.getRMIRegistryPort());
            }
         } else {
            var3 = LocateRegistry.getRegistry(this.resources.getRMIRegistryHost(), this.resources.getRMIRegistryPort());
         }

         String var4 = this.resources.getRMIBindName();
         var3.rebind(var4, (Remote)var2);
         this.boundRemotely = true;
         this.getLog().info("Scheduler bound to RMI registry under name '" + var4 + "'");
      }
   }

   private void unBind() throws Exception {
      String var1 = this.resources.getRMIRegistryHost();
      if (var1 != null && var1.length() != 0) {
         Registry var2 = LocateRegistry.getRegistry(this.resources.getRMIRegistryHost(), this.resources.getRMIRegistryPort());
         String var3 = this.resources.getRMIBindName();

         try {
            var2.unbind(var3);
            UnicastRemoteObject.unexportObject(this, true);
         } catch (NotBoundException var5) {
         }

         this.getLog().info("Scheduler un-bound from name '" + var3 + "' in RMI registry");
      }
   }

   @Override
   public String getSchedulerName() {
      return this.resources.getName();
   }

   @Override
   public String getSchedulerInstanceId() {
      return this.resources.getInstanceId();
   }

   public ThreadGroup getSchedulerThreadGroup() {
      if (this.threadGroup == null) {
         this.threadGroup = new ThreadGroup("QuartzScheduler:" + this.getSchedulerName());
         if (this.resources.getMakeSchedulerThreadDaemon()) {
            this.threadGroup.setDaemon(true);
         }
      }

      return this.threadGroup;
   }

   public void addNoGCObject(Object var1) {
      this.holdToPreventGC.add(var1);
   }

   public boolean removeNoGCObject(Object var1) {
      return this.holdToPreventGC.remove(var1);
   }

   @Override
   public SchedulerContext getSchedulerContext() {
      return this.context;
   }

   public boolean isSignalOnSchedulingChange() {
      return this.signalOnSchedulingChange;
   }

   public void setSignalOnSchedulingChange(boolean var1) {
      this.signalOnSchedulingChange = var1;
   }

   @Override
   public void start() throws SchedulerException {
      if (!this.shuttingDown && !this.closed) {
         this.notifySchedulerListenersStarting();
         if (this.initialStart == null) {
            this.initialStart = new Date();
            this.resources.getJobStore().schedulerStarted();
            this.startPlugins();
         } else {
            this.resources.getJobStore().schedulerResumed();
         }

         this.schedThread.togglePause(false);
         this.getLog().info("Scheduler " + this.resources.getUniqueIdentifier() + " started.");
         this.notifySchedulerListenersStarted();
      } else {
         throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
      }
   }

   @Override
   public void startDelayed(final int var1) throws SchedulerException {
      if (!this.shuttingDown && !this.closed) {
         Thread var2 = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  Thread.sleep(var1 * 1000L);
               } catch (InterruptedException var3) {
               }

               try {
                  QuartzScheduler.this.start();
               } catch (SchedulerException var2x) {
                  QuartzScheduler.this.getLog().error("Unable to start scheduler after startup delay.", var2x);
               }
            }
         });
         var2.start();
      } else {
         throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
      }
   }

   @Override
   public void standby() {
      this.resources.getJobStore().schedulerPaused();
      this.schedThread.togglePause(true);
      this.getLog().info("Scheduler " + this.resources.getUniqueIdentifier() + " paused.");
      this.notifySchedulerListenersInStandbyMode();
   }

   @Override
   public boolean isInStandbyMode() {
      return this.schedThread.isPaused();
   }

   @Override
   public Date runningSince() {
      return this.initialStart == null ? null : new Date(this.initialStart.getTime());
   }

   @Override
   public int numJobsExecuted() {
      return this.jobMgr.getNumJobsFired();
   }

   @Override
   public Class<?> getJobStoreClass() {
      return this.resources.getJobStore().getClass();
   }

   @Override
   public boolean supportsPersistence() {
      return this.resources.getJobStore().supportsPersistence();
   }

   @Override
   public boolean isClustered() {
      return this.resources.getJobStore().isClustered();
   }

   @Override
   public Class<?> getThreadPoolClass() {
      return this.resources.getThreadPool().getClass();
   }

   @Override
   public int getThreadPoolSize() {
      return this.resources.getThreadPool().getPoolSize();
   }

   @Override
   public void shutdown() {
      this.shutdown(false);
   }

   @Override
   public void shutdown(boolean var1) {
      if (!this.shuttingDown && !this.closed) {
         this.shuttingDown = true;
         this.getLog().info("Scheduler " + this.resources.getUniqueIdentifier() + " shutting down.");
         this.standby();
         this.schedThread.halt(var1);
         this.notifySchedulerListenersShuttingdown();
         if (this.resources.isInterruptJobsOnShutdown() && !var1 || this.resources.isInterruptJobsOnShutdownWithWait() && var1) {
            for (JobExecutionContext var4 : this.getCurrentlyExecutingJobs()) {
               if (var4.getJobInstance() instanceof InterruptableJob) {
                  try {
                     ((InterruptableJob)var4.getJobInstance()).interrupt();
                  } catch (Throwable var8) {
                     this.getLog().warn("Encountered error when interrupting job {} during shutdown: {}", var4.getJobDetail().getKey(), var8);
                  }
               }
            }
         }

         this.resources.getThreadPool().shutdown(var1);
         this.closed = true;
         if (this.resources.getJMXExport()) {
            try {
               this.unregisterJMX();
            } catch (Exception var7) {
            }
         }

         if (this.boundRemotely) {
            try {
               this.unBind();
            } catch (Exception var6) {
            }
         }

         this.shutdownPlugins();
         this.resources.getJobStore().shutdown();
         this.notifySchedulerListenersShutdown();
         SchedulerRepository.getInstance().remove(this.resources.getName());
         this.holdToPreventGC.clear();
         this.getLog().info("Scheduler " + this.resources.getUniqueIdentifier() + " shutdown complete.");
      }
   }

   @Override
   public boolean isShutdown() {
      return this.closed;
   }

   public boolean isShuttingDown() {
      return this.shuttingDown;
   }

   public boolean isStarted() {
      return !this.shuttingDown && !this.closed && !this.isInStandbyMode() && this.initialStart != null;
   }

   public void validateState() throws SchedulerException {
      if (this.isShutdown()) {
         throw new SchedulerException("The Scheduler has been shutdown.");
      }
   }

   @Override
   public List<JobExecutionContext> getCurrentlyExecutingJobs() {
      return this.jobMgr.getExecutingJobs();
   }

   @Override
   public Date scheduleJob(JobDetail var1, Trigger var2) throws SchedulerException {
      this.validateState();
      if (var1 == null) {
         throw new SchedulerException("JobDetail cannot be null");
      } else if (var2 == null) {
         throw new SchedulerException("Trigger cannot be null");
      } else if (var1.getKey() == null) {
         throw new SchedulerException("Job's key cannot be null");
      } else if (var1.getJobClass() == null) {
         throw new SchedulerException("Job's class cannot be null");
      } else {
         OperableTrigger var3 = (OperableTrigger)var2;
         if (var2.getJobKey() == null) {
            var3.setJobKey(var1.getKey());
         } else if (!var2.getJobKey().equals(var1.getKey())) {
            throw new SchedulerException("Trigger does not reference given job!");
         }

         var3.validate();
         Calendar var4 = null;
         if (var2.getCalendarName() != null) {
            var4 = this.resources.getJobStore().retrieveCalendar(var2.getCalendarName());
         }

         Date var5 = var3.computeFirstFireTime(var4);
         if (var5 == null) {
            throw new SchedulerException("Based on configured schedule, the given trigger '" + var2.getKey() + "' will never fire.");
         } else {
            this.resources.getJobStore().storeJobAndTrigger(var1, var3);
            this.notifySchedulerListenersJobAdded(var1);
            this.notifySchedulerThread(var2.getNextFireTime().getTime());
            this.notifySchedulerListenersSchduled(var2);
            return var5;
         }
      }
   }

   @Override
   public Date scheduleJob(Trigger var1) throws SchedulerException {
      this.validateState();
      if (var1 == null) {
         throw new SchedulerException("Trigger cannot be null");
      } else {
         OperableTrigger var2 = (OperableTrigger)var1;
         var2.validate();
         Calendar var3 = null;
         if (var1.getCalendarName() != null) {
            var3 = this.resources.getJobStore().retrieveCalendar(var1.getCalendarName());
            if (var3 == null) {
               throw new SchedulerException("Calendar not found: " + var1.getCalendarName());
            }
         }

         Date var4 = var2.computeFirstFireTime(var3);
         if (var4 == null) {
            throw new SchedulerException("Based on configured schedule, the given trigger '" + var1.getKey() + "' will never fire.");
         } else {
            this.resources.getJobStore().storeTrigger(var2, false);
            this.notifySchedulerThread(var1.getNextFireTime().getTime());
            this.notifySchedulerListenersSchduled(var1);
            return var4;
         }
      }
   }

   @Override
   public void addJob(JobDetail var1, boolean var2) throws SchedulerException {
      this.addJob(var1, var2, false);
   }

   @Override
   public void addJob(JobDetail var1, boolean var2, boolean var3) throws SchedulerException {
      this.validateState();
      if (!var3 && !var1.isDurable()) {
         throw new SchedulerException("Jobs added with no trigger must be durable.");
      } else {
         this.resources.getJobStore().storeJob(var1, var2);
         this.notifySchedulerThread(0L);
         this.notifySchedulerListenersJobAdded(var1);
      }
   }

   @Override
   public boolean deleteJob(JobKey var1) throws SchedulerException {
      this.validateState();
      boolean var2 = false;

      for (Trigger var5 : this.getTriggersOfJob(var1)) {
         if (!this.unscheduleJob(var5.getKey())) {
            StringBuilder var6 = new StringBuilder()
               .append("Unable to unschedule trigger [")
               .append(var5.getKey())
               .append("] while deleting job [")
               .append(var1)
               .append("]");
            throw new SchedulerException(var6.toString());
         }

         var2 = true;
      }

      var2 = this.resources.getJobStore().removeJob(var1) || var2;
      if (var2) {
         this.notifySchedulerThread(0L);
         this.notifySchedulerListenersJobDeleted(var1);
      }

      return var2;
   }

   @Override
   public boolean deleteJobs(List<JobKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      boolean var2 = false;
      var2 = this.resources.getJobStore().removeJobs(var1);
      this.notifySchedulerThread(0L);

      for (JobKey var4 : var1) {
         this.notifySchedulerListenersJobDeleted(var4);
      }

      return var2;
   }

   @Override
   public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) throws SchedulerException {
      this.validateState();

      for (Entry var4 : var1.entrySet()) {
         JobDetail var5 = (JobDetail)var4.getKey();
         if (var5 != null) {
            Set<Trigger> var6 = (Set<Trigger>)var4.getValue();
            if (var6 != null) {
               for (Trigger var8 : var6) {
                  OperableTrigger var9 = (OperableTrigger)var8;
                  var9.setJobKey(var5.getKey());
                  var9.validate();
                  Calendar var10 = null;
                  if (var8.getCalendarName() != null) {
                     var10 = this.resources.getJobStore().retrieveCalendar(var8.getCalendarName());
                     if (var10 == null) {
                        throw new SchedulerException("Calendar '" + var8.getCalendarName() + "' not found for trigger: " + var8.getKey());
                     }
                  }

                  Date var11 = var9.computeFirstFireTime(var10);
                  if (var11 == null) {
                     throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
                  }
               }
            }
         }
      }

      this.resources.getJobStore().storeJobsAndTriggers(var1, var2);
      this.notifySchedulerThread(0L);

      for (JobDetail var13 : var1.keySet()) {
         this.notifySchedulerListenersJobAdded(var13);
      }
   }

   @Override
   public void scheduleJob(JobDetail var1, Set<? extends Trigger> var2, boolean var3) throws SchedulerException {
      HashMap var4 = new HashMap();
      var4.put(var1, var2);
      this.scheduleJobs(var4, var3);
   }

   @Override
   public boolean unscheduleJobs(List<TriggerKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      boolean var2 = false;
      var2 = this.resources.getJobStore().removeTriggers(var1);
      this.notifySchedulerThread(0L);

      for (TriggerKey var4 : var1) {
         this.notifySchedulerListenersUnscheduled(var4);
      }

      return var2;
   }

   @Override
   public boolean unscheduleJob(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (this.resources.getJobStore().removeTrigger(var1)) {
         this.notifySchedulerThread(0L);
         this.notifySchedulerListenersUnscheduled(var1);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public Date rescheduleJob(TriggerKey var1, Trigger var2) throws SchedulerException {
      this.validateState();
      if (var1 == null) {
         throw new IllegalArgumentException("triggerKey cannot be null");
      } else if (var2 == null) {
         throw new IllegalArgumentException("newTrigger cannot be null");
      } else {
         OperableTrigger var3 = (OperableTrigger)var2;
         Trigger var4 = this.getTrigger(var1);
         if (var4 == null) {
            return null;
         } else {
            var3.setJobKey(var4.getJobKey());
            var3.validate();
            Calendar var5 = null;
            if (var2.getCalendarName() != null) {
               var5 = this.resources.getJobStore().retrieveCalendar(var2.getCalendarName());
            }

            Date var6 = var3.computeFirstFireTime(var5);
            if (var6 == null) {
               throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
            } else if (this.resources.getJobStore().replaceTrigger(var1, var3)) {
               this.notifySchedulerThread(var2.getNextFireTime().getTime());
               this.notifySchedulerListenersUnscheduled(var1);
               this.notifySchedulerListenersSchduled(var2);
               return var6;
            } else {
               return null;
            }
         }
      }
   }

   private String newTriggerId() {
      long var1 = this.random.nextLong();
      if (var1 < 0L) {
         var1 = -var1;
      }

      return "MT_" + Long.toString(var1, 30 + (int)(System.currentTimeMillis() % 7L));
   }

   @Override
   public void triggerJob(JobKey var1, JobDataMap var2) throws SchedulerException , JobPersistenceException{
      this.validateState();
      OperableTrigger var3 = (OperableTrigger)TriggerBuilder.newTrigger().withIdentity(this.newTriggerId(), "DEFAULT").forJob(var1).build();
      var3.computeFirstFireTime(null);
      if (var2 != null) {
         var3.setJobDataMap(var2);
      }

      boolean var4 = true;

      while (var4) {
         try {
            this.resources.getJobStore().storeTrigger(var3, false);
            var4 = false;
         } catch (ObjectAlreadyExistsException var6) {
            var3.setKey(new TriggerKey(this.newTriggerId(), "DEFAULT"));
         }
      }

      this.notifySchedulerThread(var3.getNextFireTime().getTime());
      this.notifySchedulerListenersSchduled(var3);
   }

   @Override
   public void triggerJob(OperableTrigger var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      var1.computeFirstFireTime(null);
      boolean var2 = true;

      while (var2) {
         try {
            this.resources.getJobStore().storeTrigger(var1, false);
            var2 = false;
         } catch (ObjectAlreadyExistsException var4) {
            var1.setKey(new TriggerKey(this.newTriggerId(), "DEFAULT"));
         }
      }

      this.notifySchedulerThread(var1.getNextFireTime().getTime());
      this.notifySchedulerListenersSchduled(var1);
   }

   @Override
   public void pauseTrigger(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().pauseTrigger(var1);
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersPausedTrigger(var1);
   }

   @Override
   public void pauseTriggers(GroupMatcher<TriggerKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      Collection<String> var2 = this.resources.getJobStore().pauseTriggers(var1);
      this.notifySchedulerThread(0L);

      for (String var4 : var2) {
         this.notifySchedulerListenersPausedTriggers(var4);
      }
   }

   @Override
   public void pauseJob(JobKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().pauseJob(var1);
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersPausedJob(var1);
   }

   @Override
   public void pauseJobs(GroupMatcher<JobKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      Collection<String> var2 = this.resources.getJobStore().pauseJobs(var1);
      this.notifySchedulerThread(0L);

      for (String var4 : var2) {
         this.notifySchedulerListenersPausedJobs(var4);
      }
   }

   @Override
   public void resumeTrigger(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().resumeTrigger(var1);
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersResumedTrigger(var1);
   }

   @Override
   public void resumeTriggers(GroupMatcher<TriggerKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      Collection<String> var2 = this.resources.getJobStore().resumeTriggers(var1);
      this.notifySchedulerThread(0L);

      for (String var4 : var2) {
         this.notifySchedulerListenersResumedTriggers(var4);
      }
   }

   @Override
   public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
      return this.resources.getJobStore().getPausedTriggerGroups();
   }

   @Override
   public void resumeJob(JobKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().resumeJob(var1);
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersResumedJob(var1);
   }

   @Override
   public void resumeJobs(GroupMatcher<JobKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      Collection<String> var2 = this.resources.getJobStore().resumeJobs(var1);
      this.notifySchedulerThread(0L);

      for (String var4 : var2) {
         this.notifySchedulerListenersResumedJobs(var4);
      }
   }

   @Override
   public void pauseAll() throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().pauseAll();
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersPausedTriggers(null);
   }

   @Override
   public void resumeAll() throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().resumeAll();
      this.notifySchedulerThread(0L);
      this.notifySchedulerListenersResumedTrigger(null);
   }

   @Override
   public List<String> getJobGroupNames() throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().getJobGroupNames();
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      return this.resources.getJobStore().getJobKeys(var1);
   }

   @Override
   public List<? extends Trigger> getTriggersOfJob(JobKey var1) throws SchedulerException, JobPersistenceException {
      this.validateState();
      return this.resources.getJobStore().getTriggersForJob(var1);
   }

   @Override
   public List<String> getTriggerGroupNames() throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().getTriggerGroupNames();
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      if (var1 == null) {
         var1 = GroupMatcher.groupEquals("DEFAULT");
      }

      return this.resources.getJobStore().getTriggerKeys(var1);
   }

   @Override
   public JobDetail getJobDetail(JobKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().retrieveJob(var1);
   }

   @Override
   public Trigger getTrigger(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().retrieveTrigger(var1);
   }

   @Override
   public boolean checkExists(JobKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().checkExists(var1);
   }

   @Override
   public boolean checkExists(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().checkExists(var1);
   }

   @Override
   public void clear() throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().clearAllSchedulingData();
      this.notifySchedulerListenersUnscheduled(null);
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().getTriggerState(var1);
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      this.resources.getJobStore().resetTriggerFromErrorState(var1);
   }

   @Override
   public void addCalendar(String var1, Calendar var2, boolean var3, boolean var4) throws SchedulerException , ObjectAlreadyExistsException{
      this.validateState();
      this.resources.getJobStore().storeCalendar(var1, var2, var3, var4);
   }

   @Override
   public boolean deleteCalendar(String var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().removeCalendar(var1);
   }

   @Override
   public Calendar getCalendar(String var1) throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().retrieveCalendar(var1);
   }

   @Override
   public List<String> getCalendarNames() throws SchedulerException , JobPersistenceException{
      this.validateState();
      return this.resources.getJobStore().getCalendarNames();
   }

   public ListenerManager getListenerManager() {
      return this.listenerManager;
   }

   public void addInternalJobListener(JobListener var1) {
      if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.internalJobListeners) {
            this.internalJobListeners.put(var1.getName(), var1);
         }
      } else {
         throw new IllegalArgumentException("JobListener name cannot be empty.");
      }
   }

   public boolean removeInternalJobListener(String var1) {
      synchronized (this.internalJobListeners) {
         return this.internalJobListeners.remove(var1) != null;
      }
   }

   public List<JobListener> getInternalJobListeners() {
      synchronized (this.internalJobListeners) {
         return Collections.unmodifiableList(new LinkedList<>(this.internalJobListeners.values()));
      }
   }

   public JobListener getInternalJobListener(String var1) {
      synchronized (this.internalJobListeners) {
         return this.internalJobListeners.get(var1);
      }
   }

   public void addInternalTriggerListener(TriggerListener var1) {
      if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.internalTriggerListeners) {
            this.internalTriggerListeners.put(var1.getName(), var1);
         }
      } else {
         throw new IllegalArgumentException("TriggerListener name cannot be empty.");
      }
   }

   public boolean removeinternalTriggerListener(String var1) {
      synchronized (this.internalTriggerListeners) {
         return this.internalTriggerListeners.remove(var1) != null;
      }
   }

   public List<TriggerListener> getInternalTriggerListeners() {
      synchronized (this.internalTriggerListeners) {
         return Collections.unmodifiableList(new LinkedList<>(this.internalTriggerListeners.values()));
      }
   }

   public TriggerListener getInternalTriggerListener(String var1) {
      synchronized (this.internalTriggerListeners) {
         return this.internalTriggerListeners.get(var1);
      }
   }

   public void addInternalSchedulerListener(SchedulerListener var1) {
      synchronized (this.internalSchedulerListeners) {
         this.internalSchedulerListeners.add(var1);
      }
   }

   public boolean removeInternalSchedulerListener(SchedulerListener var1) {
      synchronized (this.internalSchedulerListeners) {
         return this.internalSchedulerListeners.remove(var1);
      }
   }

   public List<SchedulerListener> getInternalSchedulerListeners() {
      synchronized (this.internalSchedulerListeners) {
         return Collections.unmodifiableList(new ArrayList<>(this.internalSchedulerListeners));
      }
   }

   protected void notifyJobStoreJobComplete(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      this.resources.getJobStore().triggeredJobComplete(var1, var2, var3);
   }

   protected void notifyJobStoreJobVetoed(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      this.resources.getJobStore().triggeredJobComplete(var1, var2, var3);
   }

   protected void notifySchedulerThread(long var1) {
      if (this.isSignalOnSchedulingChange()) {
         this.signaler.signalSchedulingChange(var1);
      }
   }

   private List<TriggerListener> buildTriggerListenerList() {
      LinkedList var1 = new LinkedList();
      var1.addAll(this.getListenerManager().getTriggerListeners());
      var1.addAll(this.getInternalTriggerListeners());
      return var1;
   }

   private List<JobListener> buildJobListenerList() {
      LinkedList var1 = new LinkedList();
      var1.addAll(this.getListenerManager().getJobListeners());
      var1.addAll(this.getInternalJobListeners());
      return var1;
   }

   private List<SchedulerListener> buildSchedulerListenerList() {
      LinkedList var1 = new LinkedList();
      var1.addAll(this.getListenerManager().getSchedulerListeners());
      var1.addAll(this.getInternalSchedulerListeners());
      return var1;
   }

   private boolean matchJobListener(JobListener var1, JobKey var2) {
      List<Matcher<JobKey>> var3 = this.getListenerManager().getJobListenerMatchers(var1.getName());
      if (var3 == null) {
         return true;
      } else {
         for (Matcher var5 : var3) {
            if (var5.isMatch(var2)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean matchTriggerListener(TriggerListener var1, TriggerKey var2) {
      List<Matcher<TriggerKey>> var3 = this.getListenerManager().getTriggerListenerMatchers(var1.getName());
      if (var3 == null) {
         return true;
      } else {
         for (Matcher var5 : var3) {
            if (var5.isMatch(var2)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean notifyTriggerListenersFired(JobExecutionContext var1) throws SchedulerException {
      boolean var2 = false;

      for (TriggerListener var5 : this.buildTriggerListenerList()) {
         try {
            if (this.matchTriggerListener(var5, var1.getTrigger().getKey())) {
               var5.triggerFired(var1.getTrigger(), var1);
               if (var5.vetoJobExecution(var1.getTrigger(), var1)) {
                  var2 = true;
               }
            }
         } catch (Exception var8) {
            SchedulerException var7 = new SchedulerException("TriggerListener '" + var5.getName() + "' threw exception: " + var8.getMessage(), var8);
            throw var7;
         }
      }

      return var2;
   }

   public void notifyTriggerListenersMisfired(Trigger var1) throws SchedulerException {
      for (TriggerListener var4 : this.buildTriggerListenerList()) {
         try {
            if (this.matchTriggerListener(var4, var1.getKey())) {
               var4.triggerMisfired(var1);
            }
         } catch (Exception var7) {
            SchedulerException var6 = new SchedulerException("TriggerListener '" + var4.getName() + "' threw exception: " + var7.getMessage(), var7);
            throw var6;
         }
      }
   }

   public void notifyTriggerListenersComplete(JobExecutionContext var1, Trigger.CompletedExecutionInstruction var2) throws SchedulerException {
      for (TriggerListener var5 : this.buildTriggerListenerList()) {
         try {
            if (this.matchTriggerListener(var5, var1.getTrigger().getKey())) {
               var5.triggerComplete(var1.getTrigger(), var1, var2);
            }
         } catch (Exception var8) {
            SchedulerException var7 = new SchedulerException("TriggerListener '" + var5.getName() + "' threw exception: " + var8.getMessage(), var8);
            throw var7;
         }
      }
   }

   public void notifyJobListenersToBeExecuted(JobExecutionContext var1) throws SchedulerException {
      for (JobListener var4 : this.buildJobListenerList()) {
         try {
            if (this.matchJobListener(var4, var1.getJobDetail().getKey())) {
               var4.jobToBeExecuted(var1);
            }
         } catch (Exception var7) {
            SchedulerException var6 = new SchedulerException("JobListener '" + var4.getName() + "' threw exception: " + var7.getMessage(), var7);
            throw var6;
         }
      }
   }

   public void notifyJobListenersWasVetoed(JobExecutionContext var1) throws SchedulerException {
      for (JobListener var4 : this.buildJobListenerList()) {
         try {
            if (this.matchJobListener(var4, var1.getJobDetail().getKey())) {
               var4.jobExecutionVetoed(var1);
            }
         } catch (Exception var7) {
            SchedulerException var6 = new SchedulerException("JobListener '" + var4.getName() + "' threw exception: " + var7.getMessage(), var7);
            throw var6;
         }
      }
   }

   public void notifyJobListenersWasExecuted(JobExecutionContext var1, JobExecutionException var2) throws SchedulerException {
      for (JobListener var5 : this.buildJobListenerList()) {
         try {
            if (this.matchJobListener(var5, var1.getJobDetail().getKey())) {
               var5.jobWasExecuted(var1, var2);
            }
         } catch (Exception var8) {
            SchedulerException var7 = new SchedulerException("JobListener '" + var5.getName() + "' threw exception: " + var8.getMessage(), var8);
            throw var7;
         }
      }
   }

   public void notifySchedulerListenersError(String var1, SchedulerException var2) {
      for (SchedulerListener var5 : this.buildSchedulerListenerList()) {
         try {
            var5.schedulerError(var1, var2);
         } catch (Exception var7) {
            this.getLog().error("Error while notifying SchedulerListener of error: ", var7);
            this.getLog().error("  Original error (for notification) was: " + var1, var2);
         }
      }
   }

   public void notifySchedulerListenersSchduled(Trigger var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobScheduled(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of scheduled job.  Triger=" + var1.getKey(), var6);
         }
      }
   }

   public void notifySchedulerListenersUnscheduled(TriggerKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            if (var1 == null) {
               var4.schedulingDataCleared();
            } else {
               var4.jobUnscheduled((TriggerKey)var1);
            }
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of unscheduled job.  Triger=" + (var1 == null ? "ALL DATA" : var1), var6);
         }
      }
   }

   public void notifySchedulerListenersFinalized(Trigger var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.triggerFinalized(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of finalized trigger.  Triger=" + var1.getKey(), var6);
         }
      }
   }

   public void notifySchedulerListenersPausedTrigger(TriggerKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.triggerPaused(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of paused trigger: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersPausedTriggers(String var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.triggersPaused(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of paused trigger group." + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersResumedTrigger(TriggerKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.triggerResumed(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of resumed trigger: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersResumedTriggers(String var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.triggersResumed(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of resumed group: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersPausedJob(JobKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobPaused(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of paused job: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersPausedJobs(String var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobsPaused(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of paused job group: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersResumedJob(JobKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobResumed(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of resumed job: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersResumedJobs(String var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobsResumed(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of resumed job group: " + var1, var6);
         }
      }
   }

   public void notifySchedulerListenersInStandbyMode() {
      for (SchedulerListener var3 : this.buildSchedulerListenerList()) {
         try {
            var3.schedulerInStandbyMode();
         } catch (Exception var5) {
            this.getLog().error("Error while notifying SchedulerListener of inStandByMode.", var5);
         }
      }
   }

   public void notifySchedulerListenersStarted() {
      for (SchedulerListener var3 : this.buildSchedulerListenerList()) {
         try {
            var3.schedulerStarted();
         } catch (Exception var5) {
            this.getLog().error("Error while notifying SchedulerListener of startup.", var5);
         }
      }
   }

   public void notifySchedulerListenersStarting() {
      for (SchedulerListener var3 : this.buildSchedulerListenerList()) {
         try {
            var3.schedulerStarting();
         } catch (Exception var5) {
            this.getLog().error("Error while notifying SchedulerListener of startup.", var5);
         }
      }
   }

   public void notifySchedulerListenersShutdown() {
      for (SchedulerListener var3 : this.buildSchedulerListenerList()) {
         try {
            var3.schedulerShutdown();
         } catch (Exception var5) {
            this.getLog().error("Error while notifying SchedulerListener of shutdown.", var5);
         }
      }
   }

   public void notifySchedulerListenersShuttingdown() {
      for (SchedulerListener var3 : this.buildSchedulerListenerList()) {
         try {
            var3.schedulerShuttingdown();
         } catch (Exception var5) {
            this.getLog().error("Error while notifying SchedulerListener of shutdown.", var5);
         }
      }
   }

   public void notifySchedulerListenersJobAdded(JobDetail var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobAdded(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of JobAdded.", var6);
         }
      }
   }

   public void notifySchedulerListenersJobDeleted(JobKey var1) {
      for (SchedulerListener var4 : this.buildSchedulerListenerList()) {
         try {
            var4.jobDeleted(var1);
         } catch (Exception var6) {
            this.getLog().error("Error while notifying SchedulerListener of JobAdded.", var6);
         }
      }
   }

   public void setJobFactory(JobFactory var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("JobFactory cannot be set to null!");
      } else {
         this.getLog().info("JobFactory set to: " + var1);
         this.jobFactory = var1;
      }
   }

   public JobFactory getJobFactory() {
      return this.jobFactory;
   }

   @Override
   public boolean interrupt(JobKey var1) throws UnableToInterruptJobException {
      List<JobExecutionContext> var2 = this.getCurrentlyExecutingJobs();
      Object var3 = null;
      Object var4 = null;
      boolean var5 = false;

      for (JobExecutionContext var7 : var2) {
         var3 = var7.getJobDetail();
         if (var1.equals(((JobDetail)var3).getKey())) {
            var4 = var7.getJobInstance();
            if (!(var4 instanceof InterruptableJob)) {
               throw new UnableToInterruptJobException(
                  "Job " + ((JobDetail)var3).getKey() + " can not be interrupted, since it does not implement " + InterruptableJob.class.getName()
               );
            }

            ((InterruptableJob)var4).interrupt();
            var5 = true;
         }
      }

      return var5;
   }

   @Override
   public boolean interrupt(String var1) throws UnableToInterruptJobException {
      List<JobExecutionContext> var2 = this.getCurrentlyExecutingJobs();
      Object var3 = null;

      for (JobExecutionContext var5 : var2) {
         if (var5.getFireInstanceId().equals(var1)) {
            var3 = var5.getJobInstance();
            if (var3 instanceof InterruptableJob) {
               ((InterruptableJob)var3).interrupt();
               return true;
            }

            throw new UnableToInterruptJobException(
               "Job " + var5.getJobDetail().getKey() + " can not be interrupted, since it does not implement " + InterruptableJob.class.getName()
            );
         }
      }

      return false;
   }

   private void shutdownPlugins() {
      for (SchedulerPlugin var2 : this.resources.getSchedulerPlugins()) {
         var2.shutdown();
      }
   }

   private void startPlugins() {
      for (SchedulerPlugin var2 : this.resources.getSchedulerPlugins()) {
         var2.start();
      }
   }

   static {
      Properties var0 = new Properties();
      InputStream var1 = null;

      try {
         var1 = QuartzScheduler.class.getResourceAsStream("quartz-build.properties");
         if (var1 != null) {
            var0.load(var1);
            String var2 = var0.getProperty("version");
            if (var2 != null) {
               String[] var3 = var2.split("\\.");
               VERSION_MAJOR = var3[0];
               VERSION_MINOR = var3[1];
               if (var3.length > 2) {
                  VERSION_ITERATION = var3[2];
               } else {
                  VERSION_ITERATION = "0";
               }
            } else {
               LoggerFactory.getLogger(QuartzScheduler.class).error("Can't parse Quartz version from quartz-build.properties");
            }
         }
      } catch (Exception var12) {
         LoggerFactory.getLogger(QuartzScheduler.class).error("Error loading version info from quartz-build.properties.", var12);
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Exception var11) {
            }
         }
      }
   }
}
