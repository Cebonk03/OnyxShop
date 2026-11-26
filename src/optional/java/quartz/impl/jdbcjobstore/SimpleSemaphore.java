package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSemaphore implements Semaphore {
   ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<>();
   HashSet<String> locks = new HashSet<>();
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public SimpleSemaphore() {
   }

   protected Logger getLog() {
      return this.log;
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

      if (!this.isLockOwner(var2)) {
         if (this.log.isDebugEnabled()) {
            this.log.debug("Lock '" + var2 + "' is being obtained: " + Thread.currentThread().getName());
         }

         while (this.locks.contains(var2)) {
            try {
               this.wait();
            } catch (InterruptedException var4) {
               if (this.log.isDebugEnabled()) {
                  this.log.debug("Lock '" + var2 + "' was not obtained by: " + Thread.currentThread().getName());
               }
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

   @Override
   public synchronized void releaseLock(String var1) {
      var1 = var1.intern();
      if (this.isLockOwner(var1)) {
         if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("Lock '" + var1 + "' retuned by: " + Thread.currentThread().getName());
         }

         this.getThreadLocks().remove(var1);
         this.locks.remove(var1);
         this.notifyAll();
      } else if (this.getLog().isDebugEnabled()) {
         this.getLog()
            .debug(
               "Lock '" + var1 + "' attempt to retun by: " + Thread.currentThread().getName() + " -- but not owner!",
               new Exception("stack-trace of wrongful returner")
            );
      }
   }

   public synchronized boolean isLockOwner(String var1) {
      var1 = var1.intern();
      return this.getThreadLocks().contains(var1);
   }

   @Override
   public boolean requiresConnection() {
      return false;
   }
}
