package org.quartz;

import java.io.Serializable;
import java.util.Date;

public class SchedulerMetaData implements Serializable {
   private static final long serialVersionUID = 4203690002633917647L;
   private String schedName;
   private String schedInst;
   private Class<?> schedClass;
   private boolean isRemote;
   private boolean started;
   private boolean isInStandbyMode;
   private boolean shutdown;
   private Date startTime;
   private int numJobsExec;
   private Class<?> jsClass;
   private boolean jsPersistent;
   private boolean jsClustered;
   private Class<?> tpClass;
   private int tpSize;
   private String version;

   public SchedulerMetaData(
      String var1,
      String var2,
      Class<?> var3,
      boolean var4,
      boolean var5,
      boolean var6,
      boolean var7,
      Date var8,
      int var9,
      Class<?> var10,
      boolean var11,
      boolean var12,
      Class<?> var13,
      int var14,
      String var15
   ) {
      this.schedName = var1;
      this.schedInst = var2;
      this.schedClass = var3;
      this.isRemote = var4;
      this.started = var5;
      this.isInStandbyMode = var6;
      this.shutdown = var7;
      this.startTime = var8;
      this.numJobsExec = var9;
      this.jsClass = var10;
      this.jsPersistent = var11;
      this.jsClustered = var12;
      this.tpClass = var13;
      this.tpSize = var14;
      this.version = var15;
   }

   public String getSchedulerName() {
      return this.schedName;
   }

   public String getSchedulerInstanceId() {
      return this.schedInst;
   }

   public Class<?> getSchedulerClass() {
      return this.schedClass;
   }

   public Date getRunningSince() {
      return this.startTime;
   }

   public int getNumberOfJobsExecuted() {
      return this.numJobsExec;
   }

   public boolean isSchedulerRemote() {
      return this.isRemote;
   }

   public boolean isStarted() {
      return this.started;
   }

   public boolean isInStandbyMode() {
      return this.isInStandbyMode;
   }

   public boolean isShutdown() {
      return this.shutdown;
   }

   public Class<?> getJobStoreClass() {
      return this.jsClass;
   }

   public boolean isJobStoreSupportsPersistence() {
      return this.jsPersistent;
   }

   public boolean isJobStoreClustered() {
      return this.jsClustered;
   }

   public Class<?> getThreadPoolClass() {
      return this.tpClass;
   }

   public int getThreadPoolSize() {
      return this.tpSize;
   }

   public String getVersion() {
      return this.version;
   }

   @Override
   public String toString() {
      try {
         return this.getSummary();
      } catch (SchedulerException var2) {
         return "SchedulerMetaData: undeterminable.";
      }
   }

   public String getSummary() throws SchedulerException {
      StringBuilder var1 = new StringBuilder("Quartz Scheduler (v");
      var1.append(this.getVersion());
      var1.append(") '");
      var1.append(this.getSchedulerName());
      var1.append("' with instanceId '");
      var1.append(this.getSchedulerInstanceId());
      var1.append("'\n");
      var1.append("  Scheduler class: '");
      var1.append(this.getSchedulerClass().getName());
      var1.append("'");
      if (this.isSchedulerRemote()) {
         var1.append(" - access via RMI.");
      } else {
         var1.append(" - running locally.");
      }

      var1.append("\n");
      if (!this.isShutdown()) {
         if (this.getRunningSince() != null) {
            var1.append("  Running since: ");
            var1.append(this.getRunningSince());
         } else {
            var1.append("  NOT STARTED.");
         }

         var1.append("\n");
         if (this.isInStandbyMode()) {
            var1.append("  Currently in standby mode.");
         } else {
            var1.append("  Not currently in standby mode.");
         }
      } else {
         var1.append("  Scheduler has been SHUTDOWN.");
      }

      var1.append("\n");
      var1.append("  Number of jobs executed: ");
      var1.append(this.getNumberOfJobsExecuted());
      var1.append("\n");
      var1.append("  Using thread pool '");
      var1.append(this.getThreadPoolClass().getName());
      var1.append("' - with ");
      var1.append(this.getThreadPoolSize());
      var1.append(" threads.");
      var1.append("\n");
      var1.append("  Using job-store '");
      var1.append(this.getJobStoreClass().getName());
      var1.append("' - which ");
      if (this.isJobStoreSupportsPersistence()) {
         var1.append("supports persistence.");
      } else {
         var1.append("does not support persistence.");
      }

      if (this.isJobStoreClustered()) {
         var1.append(" and is clustered.");
      } else {
         var1.append(" and is not clustered.");
      }

      var1.append("\n");
      return var1.toString();
   }
}
