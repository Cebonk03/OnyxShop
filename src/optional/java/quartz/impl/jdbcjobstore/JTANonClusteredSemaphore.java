package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.util.HashSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTANonClusteredSemaphore implements Semaphore {
   public static final String DEFAULT_TRANSACTION_MANANGER_LOCATION = "java:TransactionManager";
   ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<>();
   HashSet<String> locks = new HashSet<>();
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private String transactionManagerJNDIName = "java:TransactionManager";

   public JTANonClusteredSemaphore() {
   }

   protected Logger getLog() {
      return this.log;
   }

   public void setTransactionManagerJNDIName(String var1) {
      this.transactionManagerJNDIName = var1;
   }

   private HashSet<String> getThreadLocks() {
      HashSet var1 = this.lockOwners.get();
      if (var1 == null) {
         var1 = new HashSet();
         this.lockOwners.set(var1);
      }

      return var1;
   }

   @Override
   public synchronized boolean obtainLock(Connection var1, String var2) {
      var2 = var2.intern();
      if (this.log.isDebugEnabled()) {
         this.log.debug("Lock '" + var2 + "' is desired by: " + Thread.currentThread().getName());
      }

      if (!this.isLockOwner(var1, var2)) {
         if (this.log.isDebugEnabled()) {
            this.log.debug("Lock '" + var2 + "' is being obtained: " + Thread.currentThread().getName());
         }

         while (this.locks.contains(var2)) {
            try {
               this.wait();
            } catch (InterruptedException var6) {
               if (this.log.isDebugEnabled()) {
                  this.log.debug("Lock '" + var2 + "' was not obtained by: " + Thread.currentThread().getName());
               }
            }
         }

         Transaction var3 = this.getTransaction();
         if (var3 != null) {
            try {
               var3.registerSynchronization(new JTANonClusteredSemaphore.SemaphoreSynchronization(var2));
            } catch (Exception var5) {
               throw new LockException("Failed to register semaphore with Transaction.", var5);
            }
         }

         if (this.log.isDebugEnabled()) {
            this.log.debug("Lock '" + var2 + "' given to: " + Thread.currentThread().getName());
         }

         this.getThreadLocks().add(var2);
         this.locks.add(var2);
      } else if (this.log.isDebugEnabled()) {
         this.log
            .debug(
               "Lock '" + var2 + "' already owned by: " + Thread.currentThread().getName() + " -- but not owner!",
               new Exception("stack-trace of wrongful returner")
            );
      }

      return true;
   }

   protected Transaction getTransaction() throws LockException {
      InitialContext var1 = null;

      Transaction var3;
      try {
         var1 = new InitialContext();
         TransactionManager var2 = (TransactionManager)var1.lookup(this.transactionManagerJNDIName);
         var3 = var2.getTransaction();
      } catch (SystemException var13) {
         throw new LockException("Failed to get Transaction from TransactionManager", var13);
      } catch (NamingException var14) {
         throw new LockException("Failed to find TransactionManager in JNDI under name: " + this.transactionManagerJNDIName, var14);
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (NamingException var12) {
            }
         }
      }

      return var3;
   }

   @Override
   public synchronized void releaseLock(String var1) {
      this.releaseLock(var1, false);
   }

   protected synchronized void releaseLock(String var1, boolean var2) {
      var1 = var1.intern();
      if (this.isLockOwner(null, var1)) {
         if (!var2) {
            Transaction var3 = this.getTransaction();
            if (var3 != null) {
               if (this.getLog().isDebugEnabled()) {
                  this.getLog().debug("Lock '" + var1 + "' is in a JTA transaction.  " + "Return deferred by: " + Thread.currentThread().getName());
               }

               return;
            }
         }

         if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("Lock '" + var1 + "' returned by: " + Thread.currentThread().getName());
         }

         this.getThreadLocks().remove(var1);
         this.locks.remove(var1);
         this.notify();
      } else if (this.getLog().isDebugEnabled()) {
         this.getLog()
            .debug(
               "Lock '" + var1 + "' attempt to return by: " + Thread.currentThread().getName() + " -- but not owner!",
               new Exception("stack-trace of wrongful returner")
            );
      }
   }

   public synchronized boolean isLockOwner(Connection var1, String var2) {
      var2 = var2.intern();
      return this.getThreadLocks().contains(var2);
   }

   @Override
   public boolean requiresConnection() {
      return false;
   }

   private class SemaphoreSynchronization implements Synchronization {
      private String lockName;

      public SemaphoreSynchronization(String var2) {
         this.lockName = var2;
      }

      public void beforeCompletion() {
      }

      public void afterCompletion(int var1) {
         try {
            JTANonClusteredSemaphore.this.releaseLock(this.lockName, true);
         } catch (LockException var3) {
         }
      }
   }
}
