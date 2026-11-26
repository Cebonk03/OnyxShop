package org.quartz.core;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.SchedulerSignaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerSignalerImpl implements SchedulerSignaler {
   Logger log = LoggerFactory.getLogger(SchedulerSignalerImpl.class);
   protected QuartzScheduler sched;
   protected QuartzSchedulerThread schedThread;

   public SchedulerSignalerImpl(QuartzScheduler var1, QuartzSchedulerThread var2) {
      this.sched = var1;
      this.schedThread = var2;
      this.log.info("Initialized Scheduler Signaller of type: " + this.getClass());
   }

   @Override
   public void notifyTriggerListenersMisfired(Trigger var1) {
      try {
         this.sched.notifyTriggerListenersMisfired(var1);
      } catch (SchedulerException var3) {
         this.sched.getLog().error("Error notifying listeners of trigger misfire.", var3);
         this.sched.notifySchedulerListenersError("Error notifying listeners of trigger misfire.", var3);
      }
   }

   @Override
   public void notifySchedulerListenersFinalized(Trigger var1) {
      this.sched.notifySchedulerListenersFinalized(var1);
   }

   @Override
   public void signalSchedulingChange(long var1) {
      this.schedThread.signalSchedulingChange(var1);
   }

   @Override
   public void notifySchedulerListenersJobDeleted(JobKey var1) {
      this.sched.notifySchedulerListenersJobDeleted(var1);
   }

   @Override
   public void notifySchedulerListenersError(String var1, SchedulerException var2) {
      this.sched.notifySchedulerListenersError(var1, var2);
   }
}
