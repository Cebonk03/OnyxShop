package org.quartz.simpl;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleJobFactory implements JobFactory {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public SimpleJobFactory() {
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public Job newJob(TriggerFiredBundle var1, Scheduler var2) {
      JobDetail var3 = var1.getJobDetail();
      Class var4 = var3.getJobClass();

      try {
         if (this.log.isDebugEnabled()) {
            this.log.debug("Producing instance of Job '" + var3.getKey() + "', class=" + var4.getName());
         }

         return (Job)var4.newInstance();
      } catch (Exception var7) {
         SchedulerException var6 = new SchedulerException("Problem instantiating class '" + var3.getJobClass().getName() + "'", var7);
         throw var6;
      }
   }
}
