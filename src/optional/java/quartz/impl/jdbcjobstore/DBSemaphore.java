package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBSemaphore implements Semaphore, Constants, StdJDBCConstants, TablePrefixAware {
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<>();
   private String sql;
   private String insertSql;
   private String tablePrefix;
   private String schedName;
   private String expandedSQL;
   private String expandedInsertSQL;
   private String schedNameLiteral = null;

   public DBSemaphore(String var1, String var2, String var3, String var4) {
      this.tablePrefix = var1;
      this.schedName = var2;
      this.setSQL(var3);
      this.setInsertSQL(var4);
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

   protected abstract void executeSQL(Connection var1, String var2, String var3, String var4);

   @Override
   public boolean obtainLock(Connection var1, String var2) {
      if (this.log.isDebugEnabled()) {
         this.log.debug("Lock '" + var2 + "' is desired by: " + Thread.currentThread().getName());
      }

      if (!this.isLockOwner(var2)) {
         this.executeSQL(var1, var2, this.expandedSQL, this.expandedInsertSQL);
         if (this.log.isDebugEnabled()) {
            this.log.debug("Lock '" + var2 + "' given to: " + Thread.currentThread().getName());
         }

         this.getThreadLocks().add(var2);
      } else if (this.log.isDebugEnabled()) {
         this.log.debug("Lock '" + var2 + "' Is already owned by: " + Thread.currentThread().getName());
      }

      return true;
   }

   @Override
   public void releaseLock(String var1) {
      if (this.isLockOwner(var1)) {
         if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("Lock '" + var1 + "' returned by: " + Thread.currentThread().getName());
         }

         this.getThreadLocks().remove(var1);
      } else if (this.getLog().isDebugEnabled()) {
         this.getLog()
            .warn(
               "Lock '" + var1 + "' attempt to return by: " + Thread.currentThread().getName() + " -- but not owner!",
               new Exception("stack-trace of wrongful returner")
            );
      }
   }

   public boolean isLockOwner(String var1) {
      return this.getThreadLocks().contains(var1);
   }

   @Override
   public boolean requiresConnection() {
      return true;
   }

   protected String getSQL() {
      return this.sql;
   }

   protected void setSQL(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.sql = var1.trim();
      }

      this.setExpandedSQL();
   }

   protected void setInsertSQL(String var1) {
      if (var1 != null && var1.trim().length() != 0) {
         this.insertSql = var1.trim();
      }

      this.setExpandedSQL();
   }

   private void setExpandedSQL() {
      if (this.getTablePrefix() != null && this.getSchedName() != null && this.sql != null && this.insertSql != null) {
         this.expandedSQL = Util.rtp(this.sql, this.getTablePrefix(), this.getSchedulerNameLiteral());
         this.expandedInsertSQL = Util.rtp(this.insertSql, this.getTablePrefix(), this.getSchedulerNameLiteral());
      }
   }

   protected String getSchedulerNameLiteral() {
      if (this.schedNameLiteral == null) {
         this.schedNameLiteral = "'" + this.schedName + "'";
      }

      return this.schedNameLiteral;
   }

   public String getSchedName() {
      return this.schedName;
   }

   @Override
   public void setSchedName(String var1) {
      this.schedName = var1;
      this.setExpandedSQL();
   }

   protected String getTablePrefix() {
      return this.tablePrefix;
   }

   @Override
   public void setTablePrefix(String var1) {
      this.tablePrefix = var1;
      this.setExpandedSQL();
   }
}
