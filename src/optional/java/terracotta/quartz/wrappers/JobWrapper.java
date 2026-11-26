package org.terracotta.quartz.wrappers;

import java.io.Serializable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

public class JobWrapper implements Serializable {
   protected JobDetail jobDetail;

   protected JobWrapper(JobDetail var1) {
      this.jobDetail = var1;
   }

   public JobKey getKey() {
      return this.jobDetail.getKey();
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 instanceof JobWrapper) {
         JobWrapper var2 = (JobWrapper)var1;
         if (var2.jobDetail.getKey().equals(this.jobDetail.getKey())) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.jobDetail.getKey().hashCode();
   }

   @Override
   public String toString() {
      return "[" + this.jobDetail.toString() + "]";
   }

   public boolean requestsRecovery() {
      return this.jobDetail.requestsRecovery();
   }

   public boolean isPersistJobDataAfterExecution() {
      return this.jobDetail.isPersistJobDataAfterExecution();
   }

   public boolean isConcurrentExectionDisallowed() {
      return this.jobDetail.isConcurrentExectionDisallowed();
   }

   public boolean isDurable() {
      return this.jobDetail.isDurable();
   }

   public JobDetail getJobDetailClone() {
      return (JobDetail)this.jobDetail.clone();
   }

   public void setJobDataMap(JobDataMap var1, JobFacade var2) {
      this.jobDetail = this.jobDetail.getJobBuilder().setJobData(var1).build();
      var2.put(this.jobDetail.getKey(), this);
   }

   public JobDataMap getJobDataMapClone() {
      return (JobDataMap)this.jobDetail.getJobDataMap().clone();
   }

   public boolean isInstanceof(Class var1) {
      return var1.isInstance(this.jobDetail);
   }
}
