package org.quartz.listeners;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JobListenerSupport implements JobListener {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public JobListenerSupport() {
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
   }
}
