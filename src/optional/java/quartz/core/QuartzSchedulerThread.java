package org.quartz.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzSchedulerThread extends Thread {
   private QuartzScheduler qs;
   private QuartzSchedulerResources qsRsrcs;
   private final Object sigLock = new Object();
   private boolean signaled;
   private long signaledNextFireTime;
   private boolean paused;
   private AtomicBoolean halted;
   private Random random = new Random(System.currentTimeMillis());
   private static long DEFAULT_IDLE_WAIT_TIME = 30000L;
   private long idleWaitTime = DEFAULT_IDLE_WAIT_TIME;
   private int idleWaitVariablness = 7000;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private static final long MIN_DELAY = 20L;
   private static final long MAX_DELAY = 600000L;

   QuartzSchedulerThread(QuartzScheduler var1, QuartzSchedulerResources var2) {
      this(var1, var2, var2.getMakeSchedulerThreadDaemon(), 5);
   }

   QuartzSchedulerThread(QuartzScheduler var1, QuartzSchedulerResources var2, boolean var3, int var4) {
      super(var1.getSchedulerThreadGroup(), var2.getThreadName());
      this.qs = var1;
      this.qsRsrcs = var2;
      this.setDaemon(var3);
      if (var2.isThreadsInheritInitializersClassLoadContext()) {
         this.log.info("QuartzSchedulerThread Inheriting ContextClassLoader of thread: " + Thread.currentThread().getName());
         this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
      }

      this.setPriority(var4);
      this.paused = true;
      this.halted = new AtomicBoolean(false);
   }

   void setIdleWaitTime(long var1) {
      this.idleWaitTime = var1;
      this.idleWaitVariablness = (int)(var1 * 0.2);
   }

   private long getRandomizedIdleWaitTime() {
      return this.idleWaitTime - this.random.nextInt(this.idleWaitVariablness);
   }

   void togglePause(boolean var1) {
      synchronized (this.sigLock) {
         this.paused = var1;
         if (this.paused) {
            this.signalSchedulingChange(0L);
         } else {
            this.sigLock.notifyAll();
         }
      }
   }

   void halt(boolean var1) {
      synchronized (this.sigLock) {
         this.halted.set(true);
         if (this.paused) {
            this.sigLock.notifyAll();
         } else {
            this.signalSchedulingChange(0L);
         }
      }

      if (var1) {
         boolean var11 = false;

         try {
            while (true) {
               try {
                  this.join();
                  break;
               } catch (InterruptedException var9) {
                  var11 = true;
               }
            }
         } finally {
            if (var11) {
               Thread.currentThread().interrupt();
            }
         }
      }
   }

   boolean isPaused() {
      return this.paused;
   }

   public void signalSchedulingChange(long var1) {
      synchronized (this.sigLock) {
         this.signaled = true;
         this.signaledNextFireTime = var1;
         this.sigLock.notifyAll();
      }
   }

   public void clearSignaledSchedulingChange() {
      synchronized (this.sigLock) {
         this.signaled = false;
         this.signaledNextFireTime = 0L;
      }
   }

   public boolean isScheduleChanged() {
      synchronized (this.sigLock) {
         return this.signaled;
      }
   }

   public long getSignaledNextFireTime() {
      synchronized (this.sigLock) {
         return this.signaledNextFireTime;
      }
   }

   @Override
   public void run() {
      int var1 = 0;

      while (!this.halted.get()) {
         try {
            synchronized (this.sigLock) {
               for (; this.paused && !this.halted.get(); var1 = 0) {
                  try {
                     this.sigLock.wait(1000L);
                  } catch (InterruptedException var24) {
                  }
               }

               if (this.halted.get()) {
                  break;
               }
            }

            if (var1 > 1) {
               try {
                  long var32 = computeDelayForRepeatedErrors(this.qsRsrcs.getJobStore(), var1);
                  Thread.sleep(var32);
               } catch (Exception var23) {
               }
            }

            int var33 = this.qsRsrcs.getThreadPool().blockForAvailableThreads();
            if (var33 > 0) {
               long var4 = System.currentTimeMillis();
               this.clearSignaledSchedulingChange();

               List var3;
               try {
                  var3 = this.qsRsrcs
                     .getJobStore()
                     .acquireNextTriggers(var4 + this.idleWaitTime, Math.min(var33, this.qsRsrcs.getMaxBatchSize()), this.qsRsrcs.getBatchTimeWindow());
                  var1 = 0;
                  if (this.log.isDebugEnabled()) {
                     this.log.debug("batch acquisition of " + (var3 == null ? 0 : var3.size()) + " triggers");
                  }
               } catch (JobPersistenceException var26) {
                  if (var1 == 0) {
                     this.qs.notifySchedulerListenersError("An error occurred while scanning for the next triggers to fire.", var26);
                  }

                  if (var1 < Integer.MAX_VALUE) {
                     var1++;
                  }
                  continue;
               } catch (RuntimeException var27) {
                  if (var1 == 0) {
                     this.getLog().error("quartzSchedulerThreadLoop: RuntimeException " + var27.getMessage(), var27);
                  }

                  if (var1 < Integer.MAX_VALUE) {
                     var1++;
                  }
                  continue;
               }

               if (var3 != null && !var3.isEmpty()) {
                  var4 = System.currentTimeMillis();
                  long var6 = ((OperableTrigger)var3.get(0)).getNextFireTime().getTime();
                  long var8 = var6 - var4;

                  while (var8 > 2L) {
                     synchronized (this.sigLock) {
                        if (this.halted.get()) {
                           break;
                        }

                        if (!this.isCandidateNewTimeEarlierWithinReason(var6, false)) {
                           try {
                              var4 = System.currentTimeMillis();
                              var8 = var6 - var4;
                              if (var8 >= 1L) {
                                 this.sigLock.wait(var8);
                              }
                           } catch (InterruptedException var22) {
                           }
                        }
                     }

                     if (this.releaseIfScheduleChangedSignificantly(var3, var6)) {
                        break;
                     }

                     var4 = System.currentTimeMillis();
                     var8 = var6 - var4;
                  }

                  if (!var3.isEmpty()) {
                     List<TriggerFiredResult> var39 = new ArrayList<TriggerFiredResult>();
                     boolean var11 = true;
                     synchronized (this.sigLock) {
                        var11 = !this.halted.get();
                     }

                     if (var11) {
                        try {
                           List<TriggerFiredResult> var41 = this.qsRsrcs.getJobStore().triggersFired(var3);
                           if (var41 != null) {
                              var39 = var41;
                           }
                        } catch (SchedulerException var25) {
                           this.qs.notifySchedulerListenersError("An error occurred while firing triggers '" + var3 + "'", var25);

                           for (int var13 = 0; var13 < var3.size(); var13++) {
                              this.qsRsrcs.getJobStore().releaseAcquiredTrigger((OperableTrigger)var3.get(var13));
                           }
                           continue;
                        }
                     }

                     for (int var42 = 0; var42 < var39.size(); var42++) {
                        TriggerFiredResult var43 = (TriggerFiredResult)var39.get(var42);
                        TriggerFiredBundle var14 = var43.getTriggerFiredBundle();
                        Exception var15 = var43.getException();
                        if (var15 instanceof RuntimeException) {
                           this.getLog().error("RuntimeException while firing trigger " + var3.get(var42), var15);
                           this.qsRsrcs.getJobStore().releaseAcquiredTrigger((OperableTrigger)var3.get(var42));
                        } else if (var14 == null) {
                           this.qsRsrcs.getJobStore().releaseAcquiredTrigger((OperableTrigger)var3.get(var42));
                        } else {
                           JobRunShell var16 = null;

                           try {
                              var16 = this.qsRsrcs.getJobRunShellFactory().createJobRunShell(var14);
                              var16.initialize(this.qs);
                           } catch (SchedulerException var28) {
                              this.qsRsrcs
                                 .getJobStore()
                                 .triggeredJobComplete(
                                    (OperableTrigger)var3.get(var42), var14.getJobDetail(), Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR
                                 );
                              continue;
                           }

                           if (!this.qsRsrcs.getThreadPool().runInThread(var16)) {
                              this.getLog().error("ThreadPool.runInThread() return false!");
                              this.qsRsrcs
                                 .getJobStore()
                                 .triggeredJobComplete(
                                    (OperableTrigger)var3.get(var42), var14.getJobDetail(), Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR
                                 );
                           }
                        }
                     }
                  }
               } else {
                  long var34 = System.currentTimeMillis();
                  long var5 = var34 + this.getRandomizedIdleWaitTime();
                  long var7 = var5 - var34;
                  synchronized (this.sigLock) {
                     try {
                        if (!this.halted.get() && !this.isScheduleChanged()) {
                           this.sigLock.wait(var7);
                        }
                     } catch (InterruptedException var19) {
                     }
                  }
               }
            }
         } catch (RuntimeException var31) {
            this.getLog().error("Runtime error occurred in main trigger firing loop.", var31);
         }
      }

      this.qs = null;
      this.qsRsrcs = null;
   }

   private static long computeDelayForRepeatedErrors(JobStore var0, int var1) {
      long var2;
      try {
         var2 = var0.getAcquireRetryDelay(var1);
      } catch (Exception var5) {
         var2 = 100L;
      }

      if (var2 < 20L) {
         var2 = 20L;
      }

      if (var2 > 600000L) {
         var2 = 600000L;
      }

      return var2;
   }

   private boolean releaseIfScheduleChangedSignificantly(List<OperableTrigger> var1, long var2) {
      if (!this.isCandidateNewTimeEarlierWithinReason(var2, true)) {
         return false;
      } else {
         for (OperableTrigger var5 : var1) {
            this.qsRsrcs.getJobStore().releaseAcquiredTrigger(var5);
         }

         var1.clear();
         return true;
      }
   }

   private boolean isCandidateNewTimeEarlierWithinReason(long var1, boolean var3) {
      synchronized (this.sigLock) {
         if (!this.isScheduleChanged()) {
            return false;
         } else {
            boolean var5 = false;
            if (this.getSignaledNextFireTime() == 0L) {
               var5 = true;
            } else if (this.getSignaledNextFireTime() < var1) {
               var5 = true;
            }

            if (var5) {
               long var6 = var1 - System.currentTimeMillis();
               if (var6 < (this.qsRsrcs.getJobStore().supportsPersistence() ? 70L : 7L)) {
                  var5 = false;
               }
            }

            if (var3) {
               this.clearSignaledSchedulingChange();
            }

            return var5;
         }
      }
   }

   public Logger getLog() {
      return this.log;
   }
}
