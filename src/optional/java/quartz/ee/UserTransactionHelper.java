package org.quartz.ee.jta;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTransactionHelper {
   public static final String DEFAULT_USER_TX_LOCATION = "java:comp/UserTransaction";
   private static String userTxURL = "java:comp/UserTransaction";

   private UserTransactionHelper() {
   }

   public static String getUserTxLocation() {
      return userTxURL;
   }

   public static void setUserTxLocation(String var0) {
      if (var0 != null) {
         userTxURL = var0;
      }
   }

   public static UserTransaction lookupUserTransaction() {
      return new UserTransactionHelper.UserTransactionWithContext();
   }

   public static void returnUserTransaction(UserTransaction var0) {
      if (var0 != null && var0 instanceof UserTransactionHelper.UserTransactionWithContext) {
         UserTransactionHelper.UserTransactionWithContext var1 = (UserTransactionHelper.UserTransactionWithContext)var0;
         var1.closeContext();
      }
   }

   private static class UserTransactionWithContext implements UserTransaction {
      InitialContext context;
      UserTransaction userTransaction;

      public UserTransactionWithContext() throws SchedulerException {
         try {
            this.context = new InitialContext();
         } catch (Throwable var3) {
            throw new SchedulerException("UserTransactionHelper failed to create InitialContext to lookup/create UserTransaction.", var3);
         }

         try {
            this.userTransaction = (UserTransaction)this.context.lookup(UserTransactionHelper.userTxURL);
         } catch (Throwable var2) {
            this.closeContext();
            throw new SchedulerException("UserTransactionHelper could not lookup/create UserTransaction.", var2);
         }

         if (this.userTransaction == null) {
            this.closeContext();
            throw new SchedulerException("UserTransactionHelper could not lookup/create UserTransaction from the InitialContext.");
         }
      }

      public void closeContext() {
         try {
            if (this.context != null) {
               this.context.close();
            }
         } catch (Throwable var2) {
            getLog().warn("Failed to close InitialContext used to get a UserTransaction.", var2);
         }

         this.context = null;
      }

      @Override
      protected void finalize() {
         try {
            if (this.context != null) {
               getLog().warn("UserTransaction was never returned to the UserTransactionHelper.");
               this.closeContext();
            }
         } finally {
            super.finalize();
         }
      }

      private static Logger getLog() {
         return LoggerFactory.getLogger(UserTransactionHelper.UserTransactionWithContext.class);
      }

      public void begin() {
         this.userTransaction.begin();
      }

      public void commit() {
         this.userTransaction.commit();
      }

      public void rollback() {
         this.userTransaction.rollback();
      }

      public void setRollbackOnly() {
         this.userTransaction.setRollbackOnly();
      }

      public int getStatus() {
         return this.userTransaction.getStatus();
      }

      public void setTransactionTimeout(int var1) {
         this.userTransaction.setTransactionTimeout(var1);
      }
   }
}
