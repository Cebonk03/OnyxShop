package org.quartz.core;

import java.util.ArrayList;
import java.util.List;
import org.quartz.management.ManagementRESTServiceConfiguration;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.ThreadPool;

public class QuartzSchedulerResources {
   public static final String CREATE_REGISTRY_NEVER = "never";
   public static final String CREATE_REGISTRY_ALWAYS = "always";
   public static final String CREATE_REGISTRY_AS_NEEDED = "as_needed";
   private String name;
   private String instanceId;
   private String threadName;
   private String rmiRegistryHost = null;
   private int rmiRegistryPort = 1099;
   private int rmiServerPort = -1;
   private String rmiCreateRegistryStrategy = "never";
   private ThreadPool threadPool;
   private JobStore jobStore;
   private JobRunShellFactory jobRunShellFactory;
   private List<SchedulerPlugin> schedulerPlugins = new ArrayList<>(10);
   private boolean makeSchedulerThreadDaemon = false;
   private boolean threadsInheritInitializersClassLoadContext = false;
   private String rmiBindName;
   private boolean jmxExport;
   private String jmxObjectName;
   private ManagementRESTServiceConfiguration managementRESTServiceConfiguration;
   private ThreadExecutor threadExecutor;
   private long batchTimeWindow = 0L;
   private int maxBatchSize = 1;
   private boolean interruptJobsOnShutdown = false;
   private boolean interruptJobsOnShutdownWithWait = false;

   public QuartzSchedulerResources() {
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.name = var1;
         if (this.threadName == null) {
            this.setThreadName(var1 + "_QuartzSchedulerThread");
         }
      } else {
         throw new IllegalArgumentException("Scheduler name cannot be empty.");
      }
   }

   public String getInstanceId() {
      return this.instanceId;
   }

   public void setInstanceId(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.instanceId = var1;
      } else {
         throw new IllegalArgumentException("Scheduler instanceId cannot be empty.");
      }
   }

   public static String getUniqueIdentifier(String var0, String var1) {
      return var0 + "_$_" + var1;
   }

   public String getUniqueIdentifier() {
      return getUniqueIdentifier(this.name, this.instanceId);
   }

   public String getRMIRegistryHost() {
      return this.rmiRegistryHost;
   }

   public void setRMIRegistryHost(String var1) {
      this.rmiRegistryHost = var1;
   }

   public int getRMIRegistryPort() {
      return this.rmiRegistryPort;
   }

   public void setRMIRegistryPort(int var1) {
      this.rmiRegistryPort = var1;
   }

   public int getRMIServerPort() {
      return this.rmiServerPort;
   }

   public void setRMIServerPort(int var1) {
      this.rmiServerPort = var1;
   }

   public String getRMICreateRegistryStrategy() {
      return this.rmiCreateRegistryStrategy;
   }

   public String getThreadName() {
      return this.threadName;
   }

   public void setThreadName(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.threadName = var1;
      } else {
         throw new IllegalArgumentException("Scheduler thread name cannot be empty.");
      }
   }

   public void setRMICreateRegistryStrategy(String var1) {
      if (var1 == null || var1.trim().length() == 0) {
         var1 = "never";
      } else if (var1.equalsIgnoreCase("true")) {
         var1 = "as_needed";
      } else if (var1.equalsIgnoreCase("false")) {
         var1 = "never";
      } else if (var1.equalsIgnoreCase("always")) {
         var1 = "always";
      } else if (var1.equalsIgnoreCase("as_needed")) {
         var1 = "as_needed";
      } else {
         if (!var1.equalsIgnoreCase("never")) {
            throw new IllegalArgumentException("Faild to set RMICreateRegistryStrategy - strategy unknown: '" + var1 + "'");
         }

         var1 = "never";
      }

      this.rmiCreateRegistryStrategy = var1;
   }

   public ThreadPool getThreadPool() {
      return this.threadPool;
   }

   public void setThreadPool(ThreadPool var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("ThreadPool cannot be null.");
      } else {
         this.threadPool = var1;
      }
   }

   public JobStore getJobStore() {
      return this.jobStore;
   }

   public void setJobStore(JobStore var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("JobStore cannot be null.");
      } else {
         this.jobStore = var1;
      }
   }

   public JobRunShellFactory getJobRunShellFactory() {
      return this.jobRunShellFactory;
   }

   public void setJobRunShellFactory(JobRunShellFactory var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("JobRunShellFactory cannot be null.");
      } else {
         this.jobRunShellFactory = var1;
      }
   }

   public void addSchedulerPlugin(SchedulerPlugin var1) {
      this.schedulerPlugins.add(var1);
   }

   public List<SchedulerPlugin> getSchedulerPlugins() {
      return this.schedulerPlugins;
   }

   public boolean getMakeSchedulerThreadDaemon() {
      return this.makeSchedulerThreadDaemon;
   }

   public void setMakeSchedulerThreadDaemon(boolean var1) {
      this.makeSchedulerThreadDaemon = var1;
   }

   public boolean isThreadsInheritInitializersClassLoadContext() {
      return this.threadsInheritInitializersClassLoadContext;
   }

   public void setThreadsInheritInitializersClassLoadContext(boolean var1) {
      this.threadsInheritInitializersClassLoadContext = var1;
   }

   public String getRMIBindName() {
      return this.rmiBindName == null ? this.getUniqueIdentifier() : this.rmiBindName;
   }

   public void setRMIBindName(String var1) {
      this.rmiBindName = var1;
   }

   public boolean getJMXExport() {
      return this.jmxExport;
   }

   public void setJMXExport(boolean var1) {
      this.jmxExport = var1;
   }

   public String getJMXObjectName() {
      return this.jmxObjectName == null ? generateJMXObjectName(this.name, this.instanceId) : this.jmxObjectName;
   }

   public void setJMXObjectName(String var1) {
      this.jmxObjectName = var1;
   }

   public ThreadExecutor getThreadExecutor() {
      return this.threadExecutor;
   }

   public void setThreadExecutor(ThreadExecutor var1) {
      this.threadExecutor = var1;
   }

   public static String generateJMXObjectName(String var0, String var1) {
      return "quartz:type=QuartzScheduler,name=" + var0.replaceAll(":|=|\n", ".") + ",instance=" + var1;
   }

   public long getBatchTimeWindow() {
      return this.batchTimeWindow;
   }

   public void setBatchTimeWindow(long var1) {
      this.batchTimeWindow = var1;
   }

   public int getMaxBatchSize() {
      return this.maxBatchSize;
   }

   public void setMaxBatchSize(int var1) {
      this.maxBatchSize = var1;
   }

   public boolean isInterruptJobsOnShutdown() {
      return this.interruptJobsOnShutdown;
   }

   public void setInterruptJobsOnShutdown(boolean var1) {
      this.interruptJobsOnShutdown = var1;
   }

   public boolean isInterruptJobsOnShutdownWithWait() {
      return this.interruptJobsOnShutdownWithWait;
   }

   public void setInterruptJobsOnShutdownWithWait(boolean var1) {
      this.interruptJobsOnShutdownWithWait = var1;
   }

   public ManagementRESTServiceConfiguration getManagementRESTServiceConfiguration() {
      return this.managementRESTServiceConfiguration;
   }

   public void setManagementRESTServiceConfiguration(ManagementRESTServiceConfiguration var1) {
      this.managementRESTServiceConfiguration = var1;
   }
}
