package org.quartz.plugins.history;

import java.text.MessageFormat;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingJobHistoryPlugin implements SchedulerPlugin, JobListener {
   private String name;
   private String jobToBeFiredMessage = "Job {1}.{0} fired (by trigger {4}.{3}) at: {2, date, HH:mm:ss MM/dd/yyyy}";
   private String jobSuccessMessage = "Job {1}.{0} execution complete at {2, date, HH:mm:ss MM/dd/yyyy} and reports: {8}";
   private String jobFailedMessage = "Job {1}.{0} execution failed at {2, date, HH:mm:ss MM/dd/yyyy} and reports: {8}";
   private String jobWasVetoedMessage = "Job {1}.{0} was vetoed.  It was to be fired (by trigger {4}.{3}) at: {2, date, HH:mm:ss MM/dd/yyyy}";
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public LoggingJobHistoryPlugin() {
   }

   protected Logger getLog() {
      return this.log;
   }

   public String getJobSuccessMessage() {
      return this.jobSuccessMessage;
   }

   public String getJobFailedMessage() {
      return this.jobFailedMessage;
   }

   public String getJobToBeFiredMessage() {
      return this.jobToBeFiredMessage;
   }

   public void setJobSuccessMessage(String var1) {
      this.jobSuccessMessage = var1;
   }

   public void setJobFailedMessage(String var1) {
      this.jobFailedMessage = var1;
   }

   public void setJobToBeFiredMessage(String var1) {
      this.jobToBeFiredMessage = var1;
   }

   public String getJobWasVetoedMessage() {
      return this.jobWasVetoedMessage;
   }

   public void setJobWasVetoedMessage(String var1) {
      this.jobWasVetoedMessage = var1;
   }

   @Override
   public void initialize(String var1, Scheduler var2, ClassLoadHelper var3) {
      this.name = var1;
      var2.getListenerManager().addJobListener(this, EverythingMatcher.allJobs());
   }

   @Override
   public void start() {
   }

   @Override
   public void shutdown() {
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
      if (this.getLog().isInfoEnabled()) {
         Trigger var2 = var1.getTrigger();
         Object[] var3 = new Object[]{
            var1.getJobDetail().getKey().getName(),
            var1.getJobDetail().getKey().getGroup(),
            new Date(),
            var2.getKey().getName(),
            var2.getKey().getGroup(),
            var2.getPreviousFireTime(),
            var2.getNextFireTime(),
            var1.getRefireCount()
         };
         this.getLog().info(MessageFormat.format(this.getJobToBeFiredMessage(), var3));
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
      Trigger var3 = var1.getTrigger();
      Object var4 = null;
      if (var2 != null) {
         if (!this.getLog().isWarnEnabled()) {
            return;
         }

         String var5 = var2.getMessage();
         var4 = new Object[]{
            var1.getJobDetail().getKey().getName(),
            var1.getJobDetail().getKey().getGroup(),
            new Date(),
            var3.getKey().getName(),
            var3.getKey().getGroup(),
            var3.getPreviousFireTime(),
            var3.getNextFireTime(),
            var1.getRefireCount(),
            var5
         };
         this.getLog().warn(MessageFormat.format(this.getJobFailedMessage(), (Object[])var4), var2);
      } else {
         if (!this.getLog().isInfoEnabled()) {
            return;
         }

         String var8 = String.valueOf(var1.getResult());
         var4 = new Object[]{
            var1.getJobDetail().getKey().getName(),
            var1.getJobDetail().getKey().getGroup(),
            new Date(),
            var3.getKey().getName(),
            var3.getKey().getGroup(),
            var3.getPreviousFireTime(),
            var3.getNextFireTime(),
            var1.getRefireCount(),
            var8
         };
         this.getLog().info(MessageFormat.format(this.getJobSuccessMessage(), (Object[])var4));
      }
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
      if (this.getLog().isInfoEnabled()) {
         Trigger var2 = var1.getTrigger();
         Object[] var3 = new Object[]{
            var1.getJobDetail().getKey().getName(),
            var1.getJobDetail().getKey().getGroup(),
            new Date(),
            var2.getKey().getName(),
            var2.getKey().getGroup(),
            var2.getPreviousFireTime(),
            var2.getNextFireTime(),
            var1.getRefireCount()
         };
         this.getLog().info(MessageFormat.format(this.getJobWasVetoedMessage(), var3));
      }
   }
}
