package org.quartz.impl.jdbcjobstore;

import java.util.Date;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class TriggerStatus {
   private TriggerKey key;
   private JobKey jobKey;
   private String status;
   private Date nextFireTime;

   public TriggerStatus(String var1, Date var2) {
      this.status = var1;
      this.nextFireTime = var2;
   }

   public JobKey getJobKey() {
      return this.jobKey;
   }

   public void setJobKey(JobKey var1) {
      this.jobKey = var1;
   }

   public TriggerKey getKey() {
      return this.key;
   }

   public void setKey(TriggerKey var1) {
      this.key = var1;
   }

   public String getStatus() {
      return this.status;
   }

   public Date getNextFireTime() {
      return this.nextFireTime;
   }

   @Override
   public String toString() {
      return "status: " + this.getStatus() + ", next Fire = " + this.getNextFireTime();
   }
}
