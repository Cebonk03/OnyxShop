package org.quartz.impl;

import java.io.Serializable;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.utils.ClassUtils;

public class JobDetailImpl implements Cloneable, Serializable, JobDetail {
   private static final long serialVersionUID = -6069784757781506897L;
   private String name;
   private String group = "DEFAULT";
   private String description;
   private Class<? extends Job> jobClass;
   private JobDataMap jobDataMap;
   private boolean durability = false;
   private boolean shouldRecover = false;
   private transient JobKey key = null;

   public JobDetailImpl() {
   }

   /** @deprecated */
   public JobDetailImpl(String var1, Class<? extends Job> var2) {
      this(var1, null, var2);
   }

   /** @deprecated */
   public JobDetailImpl(String var1, String var2, Class<? extends Job> var3) {
      this.setName(var1);
      this.setGroup(var2);
      this.setJobClass(var3);
   }

   /** @deprecated */
   public JobDetailImpl(String var1, String var2, Class<? extends Job> var3, boolean var4, boolean var5) {
      this.setName(var1);
      this.setGroup(var2);
      this.setJobClass(var3);
      this.setDurability(var4);
      this.setRequestsRecovery(var5);
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.name = var1;
         this.key = null;
      } else {
         throw new IllegalArgumentException("Job name cannot be empty.");
      }
   }

   public String getGroup() {
      return this.group;
   }

   public void setGroup(String var1) {
      if (var1 != null && var1.trim().length() == 0) {
         throw new IllegalArgumentException("Group name cannot be empty.");
      } else {
         if (var1 == null) {
            var1 = "DEFAULT";
         }

         this.group = var1;
         this.key = null;
      }
   }

   public String getFullName() {
      return this.group + "." + this.name;
   }

   @Override
   public JobKey getKey() {
      if (this.key == null) {
         if (this.getName() == null) {
            return null;
         }

         this.key = new JobKey(this.getName(), this.getGroup());
      }

      return this.key;
   }

   public void setKey(JobKey var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Key cannot be null!");
      } else {
         this.setName(var1.getName());
         this.setGroup(var1.getGroup());
         this.key = var1;
      }
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   public void setDescription(String var1) {
      this.description = var1;
   }

   @Override
   public Class<? extends Job> getJobClass() {
      return this.jobClass;
   }

   public void setJobClass(Class<? extends Job> var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Job class cannot be null.");
      } else if (!Job.class.isAssignableFrom(var1)) {
         throw new IllegalArgumentException("Job class must implement the Job interface.");
      } else {
         this.jobClass = var1;
      }
   }

   @Override
   public JobDataMap getJobDataMap() {
      if (this.jobDataMap == null) {
         this.jobDataMap = new JobDataMap();
      }

      return this.jobDataMap;
   }

   public void setJobDataMap(JobDataMap var1) {
      this.jobDataMap = var1;
   }

   public void setDurability(boolean var1) {
      this.durability = var1;
   }

   public void setRequestsRecovery(boolean var1) {
      this.shouldRecover = var1;
   }

   @Override
   public boolean isDurable() {
      return this.durability;
   }

   @Override
   public boolean isPersistJobDataAfterExecution() {
      return ClassUtils.isAnnotationPresent(this.jobClass, PersistJobDataAfterExecution.class);
   }

   @Override
   public boolean isConcurrentExectionDisallowed() {
      return ClassUtils.isAnnotationPresent(this.jobClass, DisallowConcurrentExecution.class);
   }

   @Override
   public boolean requestsRecovery() {
      return this.shouldRecover;
   }

   @Override
   public String toString() {
      return "JobDetail '"
         + this.getFullName()
         + "':  jobClass: '"
         + (this.getJobClass() == null ? null : this.getJobClass().getName())
         + " concurrentExectionDisallowed: "
         + this.isConcurrentExectionDisallowed()
         + " persistJobDataAfterExecution: "
         + this.isPersistJobDataAfterExecution()
         + " isDurable: "
         + this.isDurable()
         + " requestsRecovers: "
         + this.requestsRecovery();
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof JobDetail)) {
         return false;
      } else {
         JobDetail var2 = (JobDetail)var1;
         return var2.getKey() != null && this.getKey() != null ? var2.getKey().equals(this.getKey()) : false;
      }
   }

   @Override
   public int hashCode() {
      JobKey var1 = this.getKey();
      return var1 == null ? 0 : this.getKey().hashCode();
   }

   @Override
   public Object clone() {
      try {
         JobDetailImpl var1 = (JobDetailImpl)super.clone();
         if (this.jobDataMap != null) {
            var1.jobDataMap = (JobDataMap)this.jobDataMap.clone();
         }

         return var1;
      } catch (CloneNotSupportedException var3) {
         throw new IncompatibleClassChangeError("Not Cloneable.");
      }
   }

   @Override
   public JobBuilder getJobBuilder() {
      return JobBuilder.newJob()
         .ofType(this.getJobClass())
         .requestRecovery(this.requestsRecovery())
         .storeDurably(this.isDurable())
         .usingJobData(this.getJobDataMap())
         .withDescription(this.getDescription())
         .withIdentity(this.getKey());
   }
}
