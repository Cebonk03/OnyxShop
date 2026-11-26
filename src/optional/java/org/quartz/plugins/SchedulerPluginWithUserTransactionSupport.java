package org.quartz.plugins;

import javax.transaction.UserTransaction;
import org.quartz.Scheduler;
import org.quartz.ee.jta.UserTransactionHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SchedulerPluginWithUserTransactionSupport implements SchedulerPlugin {
   private String name;
   private Scheduler scheduler;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private boolean wrapInUserTransaction = false;

   public SchedulerPluginWithUserTransactionSupport() {
   }

   protected void start(UserTransaction var1) {
   }

   protected void shutdown(UserTransaction var1) {
   }

   protected Logger getLog() {
      return this.log;
   }

   protected String getName() {
      return this.name;
   }

   protected Scheduler getScheduler() {
      return this.scheduler;
   }

   public void initialize(String var1, Scheduler var2) {
      this.name = var1;
      this.scheduler = var2;
   }

   public boolean getWrapInUserTransaction() {
      return this.wrapInUserTransaction;
   }

   public void setWrapInUserTransaction(boolean var1) {
      this.wrapInUserTransaction = var1;
   }

   @Override
   public void start() {
      UserTransaction var1 = this.startUserTransaction();

      try {
         this.start(var1);
      } finally {
         this.resolveUserTransaction(var1);
      }
   }

   @Override
   public void shutdown() {
      UserTransaction var1 = this.startUserTransaction();

      try {
         this.shutdown(var1);
      } finally {
         this.resolveUserTransaction(var1);
      }
   }

   private UserTransaction startUserTransaction() {
      if (!this.wrapInUserTransaction) {
         return null;
      } else {
         UserTransaction var1 = null;

         try {
            var1 = UserTransactionHelper.lookupUserTransaction();
            var1.begin();
         } catch (Throwable var3) {
            UserTransactionHelper.returnUserTransaction(var1);
            var1 = null;
            this.getLog().error("Failed to start UserTransaction for plugin: " + this.getName(), var3);
         }

         return var1;
      }
   }

   private void resolveUserTransaction(UserTransaction var1) {
      if (var1 != null) {
         try {
            if (var1.getStatus() == 1) {
               var1.rollback();
            } else {
               var1.commit();
            }
         } catch (Throwable var6) {
            this.getLog().error("Failed to resolve UserTransaction for plugin: " + this.getName(), var6);
         } finally {
            UserTransactionHelper.returnUserTransaction(var1);
         }
      }
   }
}
