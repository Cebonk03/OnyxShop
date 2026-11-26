package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;

public class JobStoreTX extends JobStoreSupport {
   public JobStoreTX() {
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      super.initialize(var1, var2);
      this.getLog().info("JobStoreTX initialized.");
   }

   @Override
   protected Connection getNonManagedTXConnection() {
      return this.getConnection();
   }

   @Override
   protected Object executeInLock(String var1, JobStoreSupport.TransactionCallback var2) {
      return this.executeInNonManagedTXLock(var1, var2, null);
   }
}
