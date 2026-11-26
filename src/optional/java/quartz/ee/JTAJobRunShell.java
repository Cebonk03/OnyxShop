package org.quartz.ee.jta;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShell;
import org.quartz.spi.TriggerFiredBundle;

public class JTAJobRunShell extends JobRunShell {
   private final Integer transactionTimeout;
   private UserTransaction ut;

   public JTAJobRunShell(Scheduler var1, TriggerFiredBundle var2) {
      super(var1, var2);
      this.transactionTimeout = null;
   }

   public JTAJobRunShell(Scheduler var1, TriggerFiredBundle var2, int var3) {
      super(var1, var2);
      this.transactionTimeout = var3;
   }

   @Override
   protected void begin() throws SchedulerException {
      this.cleanupUserTransaction();
      boolean var1 = false;

      try {
         this.getLog().debug("Looking up UserTransaction.");
         this.ut = UserTransactionHelper.lookupUserTransaction();
         if (this.transactionTimeout != null) {
            this.ut.setTransactionTimeout(this.transactionTimeout);
         }

         this.getLog().debug("Beginning UserTransaction.");
         this.ut.begin();
         var1 = true;
      } catch (SchedulerException var7) {
         throw var7;
      } catch (Exception var8) {
         throw new SchedulerException("JTAJobRunShell could not start UserTransaction.", var8);
      } finally {
         if (!var1) {
            this.cleanupUserTransaction();
         }
      }
   }

   @Override
   protected void complete(boolean var1) {
      if (this.ut != null) {
         try {
            try {
               if (this.ut.getStatus() == 1) {
                  this.getLog().debug("UserTransaction marked for rollback only.");
                  var1 = false;
               }
            } catch (SystemException var10) {
               throw new SchedulerException("JTAJobRunShell could not read UserTransaction status.", var10);
            }

            if (var1) {
               try {
                  this.getLog().debug("Committing UserTransaction.");
                  this.ut.commit();
               } catch (Exception var9) {
                  throw new SchedulerException("JTAJobRunShell could not commit UserTransaction.", var9);
               }
            } else {
               try {
                  this.getLog().debug("Rolling-back UserTransaction.");
                  this.ut.rollback();
               } catch (Exception var8) {
                  throw new SchedulerException("JTAJobRunShell could not rollback UserTransaction.", var8);
               }
            }
         } finally {
            this.cleanupUserTransaction();
         }
      }
   }

   @Override
   public void passivate() {
      this.cleanupUserTransaction();
      super.passivate();
   }

   private void cleanupUserTransaction() {
      if (this.ut != null) {
         UserTransactionHelper.returnUserTransaction(this.ut);
         this.ut = null;
      }
   }
}
