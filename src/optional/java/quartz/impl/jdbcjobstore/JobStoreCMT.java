package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.SQLException;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.DBConnectionManager;

public class JobStoreCMT extends JobStoreSupport {
   protected String nonManagedTxDsName;
   protected boolean dontSetNonManagedTXConnectionAutoCommitFalse = false;
   protected boolean setTxIsolationLevelReadCommitted = false;

   public JobStoreCMT() {
   }

   public void setNonManagedTXDataSource(String var1) {
      this.nonManagedTxDsName = var1;
   }

   public String getNonManagedTXDataSource() {
      return this.nonManagedTxDsName;
   }

   public boolean isDontSetNonManagedTXConnectionAutoCommitFalse() {
      return this.dontSetNonManagedTXConnectionAutoCommitFalse;
   }

   public void setDontSetNonManagedTXConnectionAutoCommitFalse(boolean var1) {
      this.dontSetNonManagedTXConnectionAutoCommitFalse = var1;
   }

   public boolean isTxIsolationLevelReadCommitted() {
      return this.setTxIsolationLevelReadCommitted;
   }

   public void setTxIsolationLevelReadCommitted(boolean var1) {
      this.setTxIsolationLevelReadCommitted = var1;
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      if (this.nonManagedTxDsName == null) {
         throw new SchedulerConfigException(
            "Non-ManagedTX DataSource name not set!  If your 'org.quartz.jobStore.dataSource' is XA, then set 'org.quartz.jobStore.nonManagedTXDataSource' to a non-XA datasource (for the same DB).  Otherwise, you can set them to be the same."
         );
      } else {
         if (this.getLockHandler() == null) {
            this.setUseDBLocks(true);
         }

         super.initialize(var1, var2);
         this.getLog().info("JobStoreCMT initialized.");
      }
   }

   @Override
   public void shutdown() {
      super.shutdown();

      try {
         DBConnectionManager.getInstance().shutdown(this.getNonManagedTXDataSource());
      } catch (SQLException var2) {
         this.getLog().warn("Database connection shutdown unsuccessful.", var2);
      }
   }

   @Override
   protected Connection getNonManagedTXConnection() throws JobPersistenceException {
      Connection var1 = null;

      try {
         var1 = DBConnectionManager.getInstance().getConnection(this.getNonManagedTXDataSource());
      } catch (SQLException var7) {
         throw new JobPersistenceException(
            "Failed to obtain DB connection from data source '" + this.getNonManagedTXDataSource() + "': " + var7.toString(), var7
         );
      } catch (Throwable var8) {
         throw new JobPersistenceException(
            "Failed to obtain DB connection from data source '" + this.getNonManagedTXDataSource() + "': " + var8.toString(), var8
         );
      }

      if (var1 == null) {
         throw new JobPersistenceException("Could not get connection from DataSource '" + this.getNonManagedTXDataSource() + "'");
      } else {
         var1 = this.getAttributeRestoringConnection((Connection)var1);

         try {
            if (!this.isDontSetNonManagedTXConnectionAutoCommitFalse()) {
               var1.setAutoCommit(false);
            }

            if (this.isTxIsolationLevelReadCommitted()) {
               var1.setTransactionIsolation(2);
            }
         } catch (SQLException var5) {
            this.getLog().warn("Failed to override connection auto commit/transaction isolation.", var5);
         } catch (Throwable var6) {
            try {
               var1.close();
            } catch (Throwable var4) {
            }

            throw new JobPersistenceException("Failure setting up connection.", var6);
         }

         return var1;
      }
   }

   @Override
   protected Object executeInLock(String var1, JobStoreSupport.TransactionCallback var2) {
      boolean var3 = false;
      Connection var4 = null;

      Object var5;
      try {
         if (var1 != null) {
            if (this.getLockHandler().requiresConnection()) {
               var4 = this.getConnection();
            }

            var3 = this.getLockHandler().obtainLock(var4, var1);
         }

         if (var4 == null) {
            var4 = this.getConnection();
         }

         var5 = var2.execute(var4);
      } finally {
         try {
            this.releaseLock(var1, var3);
         } finally {
            this.cleanupConnection(var4);
         }
      }

      return var5;
   }
}
