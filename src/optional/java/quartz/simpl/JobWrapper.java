package org.quartz.simpl;

import org.quartz.JobDetail;
import org.quartz.JobKey;

class JobWrapper {
   public JobKey key;
   public JobDetail jobDetail;

   JobWrapper(JobDetail var1) {
      this.jobDetail = var1;
      this.key = var1.getKey();
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 instanceof JobWrapper) {
         JobWrapper var2 = (JobWrapper)var1;
         if (var2.key.equals(this.key)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.key.hashCode();
   }
}
