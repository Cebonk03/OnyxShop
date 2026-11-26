package org.quartz.simpl;

import org.quartz.SchedulerException;
import org.quartz.spi.InstanceIdGenerator;

public class SystemPropertyInstanceIdGenerator implements InstanceIdGenerator {
   public static final String SYSTEM_PROPERTY = "org.quartz.scheduler.instanceId";
   private String prepend = null;
   private String postpend = null;
   private String systemPropertyName = "org.quartz.scheduler.instanceId";

   public SystemPropertyInstanceIdGenerator() {
   }

   @Override
   public String generateInstanceId() throws SchedulerException {
      String var1 = System.getProperty(this.getSystemPropertyName());
      if (var1 == null) {
         throw new SchedulerException("No value for 'org.quartz.scheduler.instanceId' system property found, please configure your environment accordingly!");
      } else {
         if (this.getPrepend() != null) {
            var1 = this.getPrepend() + var1;
         }

         if (this.getPostpend() != null) {
            var1 = var1 + this.getPostpend();
         }

         return var1;
      }
   }

   public String getPrepend() {
      return this.prepend;
   }

   public void setPrepend(String var1) {
      this.prepend = var1 == null ? null : var1.trim();
   }

   public String getPostpend() {
      return this.postpend;
   }

   public void setPostpend(String var1) {
      this.postpend = var1 == null ? null : var1.trim();
   }

   public String getSystemPropertyName() {
      return this.systemPropertyName;
   }

   public void setSystemPropertyName(String var1) {
      this.systemPropertyName = var1 == null ? "org.quartz.scheduler.instanceId" : var1.trim();
   }
}
