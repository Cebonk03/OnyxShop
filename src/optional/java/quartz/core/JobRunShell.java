package org.quartz.core;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRunShell extends SchedulerListenerSupport implements Runnable {
   protected JobExecutionContextImpl jec = null;
   protected QuartzScheduler qs = null;
   protected TriggerFiredBundle firedTriggerBundle = null;
   protected Scheduler scheduler = null;
   protected volatile boolean shutdownRequested = false;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public JobRunShell(Scheduler var1, TriggerFiredBundle var2) {
      this.scheduler = var1;
      this.firedTriggerBundle = var2;
   }

   @Override
   public void schedulerShuttingdown() {
      this.requestShutdown();
   }

   @Override
   protected Logger getLog() {
      return this.log;
   }

   public void initialize(QuartzScheduler var1) throws SchedulerException {
      this.qs = var1;
      Object var2 = null;
      JobDetail var3 = this.firedTriggerBundle.getJobDetail();

      try {
         var2 = var1.getJobFactory().newJob(this.firedTriggerBundle, this.scheduler);
      } catch (SchedulerException var6) {
         var1.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + var3.getKey() + "'", var6);
         throw var6;
      } catch (Throwable var7) {
         SchedulerException var5 = new SchedulerException("Problem instantiating class '" + var3.getJobClass().getName() + "' - ", var7);
         var1.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + var3.getKey() + "'", var5);
         throw var5;
      }

      this.jec = new JobExecutionContextImpl(this.scheduler, this.firedTriggerBundle, (Job)var2);
   }

   public void requestShutdown() {
      this.shutdownRequested = true;
   }

   @Override
   public void run() {
      this.qs.addInternalSchedulerListener(this);

      try {
         OperableTrigger var1 = (OperableTrigger)this.jec.getTrigger();
         JobDetail var2 = this.jec.getJobDetail();

         while (true) {
            JobExecutionException var3 = null;
            Job var4 = this.jec.getJobInstance();

            try {
               this.begin();
            } catch (SchedulerException var29) {
               this.qs.notifySchedulerListenersError("Error executing Job (" + this.jec.getJobDetail().getKey() + ": couldn't begin execution.", var29);
               break;
            }

            try {
               if (!this.notifyListenersBeginning(this.jec)) {
                  break;
               }
            } catch (JobRunShell.VetoedException var28) {
               try {
                  Trigger.CompletedExecutionInstruction var6 = var1.executionComplete(this.jec, null);
                  this.qs.notifyJobStoreJobVetoed(var1, var2, var6);
                  if (this.jec.getTrigger().getNextFireTime() == null) {
                     this.qs.notifySchedulerListenersFinalized(this.jec.getTrigger());
                  }

                  this.complete(true);
               } catch (SchedulerException var22) {
                  this.qs
                     .notifySchedulerListenersError("Error during veto of Job (" + this.jec.getJobDetail().getKey() + ": couldn't finalize execution.", var22);
               }
               break;
            }

            long var5 = System.currentTimeMillis();

            long var7;
            try {
               this.log.debug("Calling execute on job " + var2.getKey());
               var4.execute(this.jec);
               var7 = System.currentTimeMillis();
            } catch (JobExecutionException var25) {
               var7 = System.currentTimeMillis();
               var3 = var25;
               this.getLog().info("Job " + var2.getKey() + " threw a JobExecutionException: ", var25);
            } catch (Throwable var26) {
               var7 = System.currentTimeMillis();
               this.getLog().error("Job " + var2.getKey() + " threw an unhandled Exception: ", var26);
               SchedulerException var10 = new SchedulerException("Job threw an unhandled exception.", var26);
               this.qs.notifySchedulerListenersError("Job (" + this.jec.getJobDetail().getKey() + " threw an exception.", var10);
               var3 = new JobExecutionException(var10, false);
            }

            this.jec.setJobRunTime(var7 - var5);
            if (!this.notifyJobListenersComplete(this.jec, var3)) {
               break;
            }

            Trigger.CompletedExecutionInstruction var9 = Trigger.CompletedExecutionInstruction.NOOP;

            try {
               var9 = var1.executionComplete(this.jec, var3);
            } catch (Exception var24) {
               SchedulerException var11 = new SchedulerException("Trigger threw an unhandled exception.", var24);
               this.qs.notifySchedulerListenersError("Please report this error to the Quartz developers.", var11);
            }

            if (!this.notifyTriggerListenersComplete(this.jec, var9)) {
               break;
            }

            if (var9 == Trigger.CompletedExecutionInstruction.RE_EXECUTE_JOB) {
               this.jec.incrementRefireCount();

               try {
                  this.complete(false);
               } catch (SchedulerException var23) {
                  this.qs.notifySchedulerListenersError("Error executing Job (" + this.jec.getJobDetail().getKey() + ": couldn't finalize execution.", var23);
               }
            } else {
               try {
                  this.complete(true);
                  this.qs.notifyJobStoreJobComplete(var1, var2, var9);
               } catch (SchedulerException var27) {
                  this.qs.notifySchedulerListenersError("Error executing Job (" + this.jec.getJobDetail().getKey() + ": couldn't finalize execution.", var27);
                  continue;
               }
               break;
            }
         }
      } finally {
         this.qs.removeInternalSchedulerListener(this);
      }
   }

   protected void begin() throws SchedulerException {
   }

   protected void complete(boolean var1) throws SchedulerException {
   }

   public void passivate() {
      this.jec = null;
      this.qs = null;
   }

   private boolean notifyListenersBeginning(JobExecutionContext var1) throws JobRunShell.VetoedException {
      boolean var2 = false;

      try {
         var2 = this.qs.notifyTriggerListenersFired(var1);
      } catch (SchedulerException var6) {
         this.qs
            .notifySchedulerListenersError(
               "Unable to notify TriggerListener(s) while firing trigger (Trigger and Job will NOT be fired!). trigger= "
                  + var1.getTrigger().getKey()
                  + " job= "
                  + var1.getJobDetail().getKey(),
               var6
            );
         return false;
      }

      if (var2) {
         try {
            this.qs.notifyJobListenersWasVetoed(var1);
         } catch (SchedulerException var4) {
            this.qs
               .notifySchedulerListenersError(
                  "Unable to notify JobListener(s) of vetoed execution while firing trigger (Trigger and Job will NOT be fired!). trigger= "
                     + var1.getTrigger().getKey()
                     + " job= "
                     + var1.getJobDetail().getKey(),
                  var4
               );
         }

         throw new JobRunShell.VetoedException();
      } else {
         try {
            this.qs.notifyJobListenersToBeExecuted(var1);
            return true;
         } catch (SchedulerException var5) {
            this.qs
               .notifySchedulerListenersError(
                  "Unable to notify JobListener(s) of Job to be executed: (Job will NOT be executed!). trigger= "
                     + var1.getTrigger().getKey()
                     + " job= "
                     + var1.getJobDetail().getKey(),
                  var5
               );
            return false;
         }
      }
   }

   private boolean notifyJobListenersComplete(JobExecutionContext var1, JobExecutionException var2) {
      try {
         this.qs.notifyJobListenersWasExecuted(var1, var2);
         return true;
      } catch (SchedulerException var4) {
         this.qs
            .notifySchedulerListenersError(
               "Unable to notify JobListener(s) of Job that was executed: (error will be ignored). trigger= "
                  + var1.getTrigger().getKey()
                  + " job= "
                  + var1.getJobDetail().getKey(),
               var4
            );
         return false;
      }
   }

   private boolean notifyTriggerListenersComplete(JobExecutionContext var1, Trigger.CompletedExecutionInstruction var2) {
      try {
         this.qs.notifyTriggerListenersComplete(var1, var2);
         if (var1.getTrigger().getNextFireTime() == null) {
            this.qs.notifySchedulerListenersFinalized(var1.getTrigger());
         }
      } catch (SchedulerException var4) {
         this.qs
            .notifySchedulerListenersError(
               "Unable to notify TriggerListener(s) of Job that was executed: (error will be ignored). trigger= "
                  + var1.getTrigger().getKey()
                  + " job= "
                  + var1.getJobDetail().getKey(),
               var4
            );
         return false;
      }

      return true;
   }

   static class VetoedException extends Exception {
      private static final long serialVersionUID = 1539955697495918463L;

      public VetoedException() {
      }
   }
}
