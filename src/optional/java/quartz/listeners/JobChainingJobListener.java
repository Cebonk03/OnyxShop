package org.quartz.listeners;

import java.util.HashMap;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public class JobChainingJobListener extends JobListenerSupport {
   private String name;
   private Map<JobKey, JobKey> chainLinks;

   public JobChainingJobListener(String var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Listener name cannot be null!");
      } else {
         this.name = var1;
         this.chainLinks = new HashMap<>();
      }
   }

   @Override
   public String getName() {
      return this.name;
   }

   public void addJobChainLink(JobKey var1, JobKey var2) {
      if (var1 == null || var2 == null) {
         throw new IllegalArgumentException("Key cannot be null!");
      } else if (var1.getName() != null && var2.getName() != null) {
         this.chainLinks.put(var1, var2);
      } else {
         throw new IllegalArgumentException("Key cannot have a null name!");
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
      JobKey var3 = this.chainLinks.get(var1.getJobDetail().getKey());
      if (var3 != null) {
         this.getLog().info("Job '" + var1.getJobDetail().getKey() + "' will now chain to Job '" + var3 + "'");

         try {
            var1.getScheduler().triggerJob(var3);
         } catch (SchedulerException var5) {
            this.getLog().error("Error encountered during chaining to Job '" + var3 + "'", var5);
         }
      }
   }
}
