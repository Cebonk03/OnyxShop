package org.quartz.plugins.interrupt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobInterruptMonitorPlugin extends TriggerListenerSupport implements SchedulerPlugin {
   private static final String JOB_INTERRUPT_MONITOR_KEY = "JOB_INTERRUPT_MONITOR_KEY";
   private long DEFAULT_MAX_RUNTIME = 300000L;
   private String name;
   private ScheduledExecutorService executor;
   private ScheduledFuture future;
   private Scheduler scheduler;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   public static final String AUTO_INTERRUPTIBLE = "AutoInterruptable";
   public static final String MAX_RUN_TIME = "MaxRunTime";

   public JobInterruptMonitorPlugin() {
   }

   @Override
   public void start() {
   }

   @Override
   public void shutdown() {
      this.executor.shutdown();
   }

   @Override
   protected Logger getLog() {
      return this.log;
   }

   public ScheduledFuture scheduleJobInterruptMonitor(JobKey var1, long var2) {
      return this.executor.schedule(new JobInterruptMonitorPlugin.InterruptMonitor(var1, this.scheduler), var2, TimeUnit.MILLISECONDS);
   }

   public long getDefaultMaxRunTime() {
      return this.DEFAULT_MAX_RUNTIME;
   }

   public void setDefaultMaxRunTime(long var1) {
      this.DEFAULT_MAX_RUNTIME = var1;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public void triggerFired(Trigger var1, JobExecutionContext var2) {
      try {
         if (var2.getJobDetail().getJobDataMap().getBoolean("AutoInterruptable")) {
            JobInterruptMonitorPlugin var3 = (JobInterruptMonitorPlugin)var2.getScheduler().getContext().get("JOB_INTERRUPT_MONITOR_KEY");
            long var4 = this.DEFAULT_MAX_RUNTIME;
            if (var2.getJobDetail().getJobDataMap().get("MaxRunTime") != null) {
               var4 = var2.getJobDetail().getJobDataMap().getLong("MaxRunTime");
            }

            this.future = var3.scheduleJobInterruptMonitor(var2.getJobDetail().getKey(), var4);
            this.getLog().debug("Job's Interrupt Monitor has been scheduled to interrupt with the delay :" + this.DEFAULT_MAX_RUNTIME);
         }
      } catch (SchedulerException var6) {
         this.getLog().info("Error scheduling interrupt monitor " + var6.getMessage(), var6);
      }
   }

   @Override
   public void triggerComplete(Trigger var1, JobExecutionContext var2, Trigger.CompletedExecutionInstruction var3) {
      if (this.future != null) {
         this.future.cancel(true);
      }
   }

   @Override
   public void initialize(String var1, Scheduler var2, ClassLoadHelper var3) {
      this.getLog().info("Registering Job Interrupt Monitor Plugin");
      this.name = var1;
      this.executor = Executors.newScheduledThreadPool(1);
      var2.getContext().put("JOB_INTERRUPT_MONITOR_KEY", this);
      this.scheduler = var2;
      this.scheduler.getListenerManager().addTriggerListener(this);
   }

   static class InterruptMonitor implements Runnable {
      private final JobKey jobKey;
      private final Scheduler scheduler;
      private final Logger log = LoggerFactory.getLogger(this.getClass());

      InterruptMonitor(JobKey var1, Scheduler var2) {
         this.jobKey = var1;
         this.scheduler = var2;
      }

      protected Logger getLog() {
         return this.log;
      }

      @Override
      public void run() {
         try {
            this.getLog()
               .info("Interrupting Job as it ran more than the configured max time. Job Details [" + this.jobKey.getName() + ":" + this.jobKey.getGroup() + "]");
            this.scheduler.interrupt(this.jobKey);
         } catch (SchedulerException var2) {
            this.getLog().info("Error interrupting Job: " + var2.getMessage(), var2);
         }
      }
   }
}
