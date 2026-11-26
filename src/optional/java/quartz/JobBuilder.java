package org.quartz;

import org.quartz.impl.JobDetailImpl;
import org.quartz.utils.Key;

public class JobBuilder {
   private JobKey key;
   private String description;
   private Class<? extends Job> jobClass;
   private boolean durability;
   private boolean shouldRecover;
   private JobDataMap jobDataMap = new JobDataMap();

   protected JobBuilder() {
   }

   public static JobBuilder newJob() {
      return new JobBuilder();
   }

   public static JobBuilder newJob(Class<? extends Job> var0) {
      JobBuilder var1 = new JobBuilder();
      var1.ofType(var0);
      return var1;
   }

   public JobDetail build() {
      JobDetailImpl var1 = new JobDetailImpl();
      var1.setJobClass(this.jobClass);
      var1.setDescription(this.description);
      if (this.key == null) {
         this.key = new JobKey(Key.createUniqueName(null), null);
      }

      var1.setKey(this.key);
      var1.setDurability(this.durability);
      var1.setRequestsRecovery(this.shouldRecover);
      if (!this.jobDataMap.isEmpty()) {
         var1.setJobDataMap(this.jobDataMap);
      }

      return var1;
   }

   public JobBuilder withIdentity(String var1) {
      this.key = new JobKey(var1, null);
      return this;
   }

   public JobBuilder withIdentity(String var1, String var2) {
      this.key = new JobKey(var1, var2);
      return this;
   }

   public JobBuilder withIdentity(JobKey var1) {
      this.key = var1;
      return this;
   }

   public JobBuilder withDescription(String var1) {
      this.description = var1;
      return this;
   }

   public JobBuilder ofType(Class<? extends Job> var1) {
      this.jobClass = var1;
      return this;
   }

   public JobBuilder requestRecovery() {
      this.shouldRecover = true;
      return this;
   }

   public JobBuilder requestRecovery(boolean var1) {
      this.shouldRecover = var1;
      return this;
   }

   public JobBuilder storeDurably() {
      this.durability = true;
      return this;
   }

   public JobBuilder storeDurably(boolean var1) {
      this.durability = var1;
      return this;
   }

   public JobBuilder usingJobData(String var1, String var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(String var1, Integer var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(String var1, Long var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(String var1, Float var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(String var1, Double var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(String var1, Boolean var2) {
      this.jobDataMap.put(var1, var2);
      return this;
   }

   public JobBuilder usingJobData(JobDataMap var1) {
      this.jobDataMap.putAll(var1);
      return this;
   }

   public JobBuilder setJobData(JobDataMap var1) {
      this.jobDataMap = var1;
      return this;
   }
}
