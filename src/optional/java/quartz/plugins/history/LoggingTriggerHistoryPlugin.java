package org.quartz.plugins.history;

import java.text.MessageFormat;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTriggerHistoryPlugin implements SchedulerPlugin, TriggerListener {
   private String name;
   private String triggerFiredMessage = "Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss MM/dd/yyyy}";
   private String triggerMisfiredMessage = "Trigger {1}.{0} misfired job {6}.{5}  at: {4, date, HH:mm:ss MM/dd/yyyy}.  Should have fired at: {3, date, HH:mm:ss MM/dd/yyyy}";
   private String triggerCompleteMessage = "Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss MM/dd/yyyy} with resulting trigger instruction code: {9}";
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public LoggingTriggerHistoryPlugin() {
   }

   protected Logger getLog() {
      return this.log;
   }

   public String getTriggerCompleteMessage() {
      return this.triggerCompleteMessage;
   }

   public String getTriggerFiredMessage() {
      return this.triggerFiredMessage;
   }

   public String getTriggerMisfiredMessage() {
      return this.triggerMisfiredMessage;
   }

   public void setTriggerCompleteMessage(String var1) {
      this.triggerCompleteMessage = var1;
   }

   public void setTriggerFiredMessage(String var1) {
      this.triggerFiredMessage = var1;
   }

   public void setTriggerMisfiredMessage(String var1) {
      this.triggerMisfiredMessage = var1;
   }

   @Override
   public void initialize(String var1, Scheduler var2, ClassLoadHelper var3) {
      this.name = var1;
      var2.getListenerManager().addTriggerListener(this, EverythingMatcher.allTriggers());
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
   public void triggerFired(Trigger var1, JobExecutionContext var2) {
      if (this.getLog().isInfoEnabled()) {
         Object[] var3 = new Object[]{
            var1.getKey().getName(),
            var1.getKey().getGroup(),
            var1.getPreviousFireTime(),
            var1.getNextFireTime(),
            new Date(),
            var2.getJobDetail().getKey().getName(),
            var2.getJobDetail().getKey().getGroup(),
            var2.getRefireCount()
         };
         this.getLog().info(MessageFormat.format(this.getTriggerFiredMessage(), var3));
      }
   }

   @Override
   public void triggerMisfired(Trigger var1) {
      if (this.getLog().isInfoEnabled()) {
         Object[] var2 = new Object[]{
            var1.getKey().getName(),
            var1.getKey().getGroup(),
            var1.getPreviousFireTime(),
            var1.getNextFireTime(),
            new Date(),
            var1.getJobKey().getName(),
            var1.getJobKey().getGroup()
         };
         this.getLog().info(MessageFormat.format(this.getTriggerMisfiredMessage(), var2));
      }
   }

   @Override
   public void triggerComplete(Trigger var1, JobExecutionContext var2, Trigger.CompletedExecutionInstruction var3) {
      if (this.getLog().isInfoEnabled()) {
         String var4 = "UNKNOWN";
         if (var3 == Trigger.CompletedExecutionInstruction.DELETE_TRIGGER) {
            var4 = "DELETE TRIGGER";
         } else if (var3 == Trigger.CompletedExecutionInstruction.NOOP) {
            var4 = "DO NOTHING";
         } else if (var3 == Trigger.CompletedExecutionInstruction.RE_EXECUTE_JOB) {
            var4 = "RE-EXECUTE JOB";
         } else if (var3 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
            var4 = "SET ALL OF JOB'S TRIGGERS COMPLETE";
         } else if (var3 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
            var4 = "SET THIS TRIGGER COMPLETE";
         }

         Object[] var5 = new Object[]{
            var1.getKey().getName(),
            var1.getKey().getGroup(),
            var1.getPreviousFireTime(),
            var1.getNextFireTime(),
            new Date(),
            var2.getJobDetail().getKey().getName(),
            var2.getJobDetail().getKey().getGroup(),
            var2.getRefireCount(),
            var3.toString(),
            var4
         };
         this.getLog().info(MessageFormat.format(this.getTriggerCompleteMessage(), var5));
      }
   }

   @Override
   public boolean vetoJobExecution(Trigger var1, JobExecutionContext var2) {
      return false;
   }
}
