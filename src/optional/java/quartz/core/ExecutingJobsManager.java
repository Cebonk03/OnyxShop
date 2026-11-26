package org.quartz.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.spi.OperableTrigger;

class ExecutingJobsManager implements JobListener {
   HashMap<String, JobExecutionContext> executingJobs = new HashMap<>();
   AtomicInteger numJobsFired = new AtomicInteger(0);

   ExecutingJobsManager() {
   }

   @Override
   public String getName() {
      return this.getClass().getName();
   }

   public int getNumJobsCurrentlyExecuting() {
      synchronized (this.executingJobs) {
         return this.executingJobs.size();
      }
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
      this.numJobsFired.incrementAndGet();
      synchronized (this.executingJobs) {
         this.executingJobs.put(((OperableTrigger)var1.getTrigger()).getFireInstanceId(), var1);
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
      synchronized (this.executingJobs) {
         this.executingJobs.remove(((OperableTrigger)var1.getTrigger()).getFireInstanceId());
      }
   }

   public int getNumJobsFired() {
      return this.numJobsFired.get();
   }

   public List<JobExecutionContext> getExecutingJobs() {
      synchronized (this.executingJobs) {
         return Collections.unmodifiableList(new ArrayList<>(this.executingJobs.values()));
      }
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
   }
}
