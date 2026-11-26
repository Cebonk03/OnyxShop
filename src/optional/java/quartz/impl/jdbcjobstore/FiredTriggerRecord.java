package org.quartz.impl.jdbcjobstore;

import java.io.Serializable;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class FiredTriggerRecord implements Serializable {
   private static final long serialVersionUID = -7183096398865657533L;
   private String fireInstanceId;
   private long fireTimestamp;
   private long scheduleTimestamp;
   private String schedulerInstanceId;
   private TriggerKey triggerKey;
   private String fireInstanceState;
   private JobKey jobKey;
   private boolean jobDisallowsConcurrentExecution;
   private boolean jobRequestsRecovery;
   private int priority;

   public FiredTriggerRecord() {
   }

   public String getFireInstanceId() {
      return this.fireInstanceId;
   }

   public long getFireTimestamp() {
      return this.fireTimestamp;
   }

   public long getScheduleTimestamp() {
      return this.scheduleTimestamp;
   }

   public boolean isJobDisallowsConcurrentExecution() {
      return this.jobDisallowsConcurrentExecution;
   }

   public JobKey getJobKey() {
      return this.jobKey;
   }

   public String getSchedulerInstanceId() {
      return this.schedulerInstanceId;
   }

   public TriggerKey getTriggerKey() {
      return this.triggerKey;
   }

   public String getFireInstanceState() {
      return this.fireInstanceState;
   }

   public void setFireInstanceId(String var1) {
      this.fireInstanceId = var1;
   }

   public void setFireTimestamp(long var1) {
      this.fireTimestamp = var1;
   }

   public void setScheduleTimestamp(long var1) {
      this.scheduleTimestamp = var1;
   }

   public void setJobDisallowsConcurrentExecution(boolean var1) {
      this.jobDisallowsConcurrentExecution = var1;
   }

   public void setJobKey(JobKey var1) {
      this.jobKey = var1;
   }

   public void setSchedulerInstanceId(String var1) {
      this.schedulerInstanceId = var1;
   }

   public void setTriggerKey(TriggerKey var1) {
      this.triggerKey = var1;
   }

   public void setFireInstanceState(String var1) {
      this.fireInstanceState = var1;
   }

   public boolean isJobRequestsRecovery() {
      return this.jobRequestsRecovery;
   }

   public void setJobRequestsRecovery(boolean var1) {
      this.jobRequestsRecovery = var1;
   }

   public int getPriority() {
      return this.priority;
   }

   public void setPriority(int var1) {
      this.priority = var1;
   }
}
