package org.quartz.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectSchedulerFactory implements SchedulerFactory {
   public static final String DEFAULT_INSTANCE_ID = "SIMPLE_NON_CLUSTERED";
   public static final String DEFAULT_SCHEDULER_NAME = "SimpleQuartzScheduler";
   private static final boolean DEFAULT_JMX_EXPORT = false;
   private static final String DEFAULT_JMX_OBJECTNAME = null;
   private static final DefaultThreadExecutor DEFAULT_THREAD_EXECUTOR = new DefaultThreadExecutor();
   private static final int DEFAULT_BATCH_MAX_SIZE = 1;
   private static final long DEFAULT_BATCH_TIME_WINDOW = 0L;
   private boolean initialized = false;
   private static DirectSchedulerFactory instance = new DirectSchedulerFactory();
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   protected Logger getLog() {
      return this.log;
   }

   protected DirectSchedulerFactory() {
   }

   public static DirectSchedulerFactory getInstance() {
      return instance;
   }

   public void createVolatileScheduler(int var1) {
      SimpleThreadPool var2 = new SimpleThreadPool(var1, 5);
      var2.initialize();
      RAMJobStore var3 = new RAMJobStore();
      this.createScheduler(var2, var3);
   }

   public void createRemoteScheduler(String var1, int var2) {
      this.createRemoteScheduler("SimpleQuartzScheduler", "SIMPLE_NON_CLUSTERED", var1, var2);
   }

   public void createRemoteScheduler(String var1, String var2, String var3, int var4) {
      this.createRemoteScheduler(var1, var2, null, var3, var4);
   }

   public void createRemoteScheduler(String var1, String var2, String var3, String var4, int var5) {
      String var6 = var3 != null ? var3 : QuartzSchedulerResources.getUniqueIdentifier(var1, var2);
      RemoteScheduler var7 = new RemoteScheduler(var6, var4, var5);
      SchedulerRepository var8 = SchedulerRepository.getInstance();
      var8.bind(var7);
      this.initialized = true;
   }

   public void createScheduler(ThreadPool var1, JobStore var2) {
      this.createScheduler("SimpleQuartzScheduler", "SIMPLE_NON_CLUSTERED", var1, var2);
   }

   public void createScheduler(String var1, String var2, ThreadPool var3, JobStore var4) {
      this.createScheduler(var1, var2, var3, var4, null, 0, -1L, -1L);
   }

   public void createScheduler(String var1, String var2, ThreadPool var3, JobStore var4, String var5, int var6, long var7, long var9) {
      this.createScheduler(var1, var2, var3, var4, null, var5, var6, var7, var9, false, DEFAULT_JMX_OBJECTNAME);
   }

   public void createScheduler(
      String var1,
      String var2,
      ThreadPool var3,
      JobStore var4,
      Map<String, SchedulerPlugin> var5,
      String var6,
      int var7,
      long var8,
      long var10,
      boolean var12,
      String var13
   ) {
      this.createScheduler(var1, var2, var3, DEFAULT_THREAD_EXECUTOR, var4, var5, var6, var7, var8, var10, var12, var13);
   }

   public void createScheduler(
      String var1,
      String var2,
      ThreadPool var3,
      ThreadExecutor var4,
      JobStore var5,
      Map<String, SchedulerPlugin> var6,
      String var7,
      int var8,
      long var9,
      long var11,
      boolean var13,
      String var14
   ) {
      this.createScheduler(var1, var2, var3, DEFAULT_THREAD_EXECUTOR, var5, var6, var7, var8, var9, var11, var13, var14, 1, 0L);
   }

   public void createScheduler(
      String var1,
      String var2,
      ThreadPool var3,
      ThreadExecutor var4,
      JobStore var5,
      Map<String, SchedulerPlugin> var6,
      String var7,
      int var8,
      long var9,
      long var11,
      boolean var13,
      String var14,
      int var15,
      long var16
   ) {
      StdJobRunShellFactory var18 = new StdJobRunShellFactory();
      var3.initialize();
      QuartzSchedulerResources var19 = new QuartzSchedulerResources();
      var19.setName(var1);
      var19.setInstanceId(var2);
      SchedulerDetailsSetter.setDetails(var3, var1, var2);
      var19.setJobRunShellFactory(var18);
      var19.setThreadPool(var3);
      var19.setThreadExecutor(var4);
      var19.setJobStore(var5);
      var19.setMaxBatchSize(var15);
      var19.setBatchTimeWindow(var16);
      var19.setRMIRegistryHost(var7);
      var19.setRMIRegistryPort(var8);
      var19.setJMXExport(var13);
      if (var14 != null) {
         var19.setJMXObjectName(var14);
      }

      if (var6 != null) {
         Iterator var20 = var6.values().iterator();

         while (var20.hasNext()) {
            var19.addSchedulerPlugin((SchedulerPlugin)var20.next());
         }
      }

      QuartzScheduler var25 = new QuartzScheduler(var19, var9, var11);
      CascadingClassLoadHelper var21 = new CascadingClassLoadHelper();
      var21.initialize();
      SchedulerDetailsSetter.setDetails(var5, var1, var2);
      var5.initialize(var21, var25.getSchedulerSignaler());
      StdScheduler var22 = new StdScheduler(var25);
      var18.initialize(var22);
      var25.initialize();
      if (var6 != null) {
         for (Entry var24 : var6.entrySet()) {
            ((SchedulerPlugin)var24.getValue()).initialize((String)var24.getKey(), var22, var21);
         }
      }

      this.getLog().info("Quartz scheduler '" + var22.getSchedulerName());
      this.getLog().info("Quartz scheduler version: " + var25.getVersion());
      SchedulerRepository var26 = SchedulerRepository.getInstance();
      var25.addNoGCObject(var26);
      var26.bind(var22);
      this.initialized = true;
   }

   @Override
   public Scheduler getScheduler() throws SchedulerException {
      if (!this.initialized) {
         throw new SchedulerException("you must call createRemoteScheduler or createScheduler methods before calling getScheduler()");
      } else {
         return this.getScheduler("SimpleQuartzScheduler");
      }
   }

   @Override
   public Scheduler getScheduler(String var1) {
      SchedulerRepository var2 = SchedulerRepository.getInstance();
      return var2.lookup(var1);
   }

   @Override
   public Collection<Scheduler> getAllSchedulers() {
      return SchedulerRepository.getInstance().lookupAll();
   }
}
