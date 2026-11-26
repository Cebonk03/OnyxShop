package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StdRowLockSemaphore extends DBSemaphore {
   public static final String SELECT_FOR_LOCK = "SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE";
   public static final String INSERT_LOCK = "INSERT INTO {0}LOCKS(SCHED_NAME, LOCK_NAME) VALUES ({1}, ?)";

   public StdRowLockSemaphore() {
      super(
         "QRTZ_",
         null,
         "SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE",
         "INSERT INTO {0}LOCKS(SCHED_NAME, LOCK_NAME) VALUES ({1}, ?)"
      );
   }

   public StdRowLockSemaphore(String var1, String var2, String var3) {
      super(
         var1,
         var2,
         var3 != null ? var3 : "SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE",
         "INSERT INTO {0}LOCKS(SCHED_NAME, LOCK_NAME) VALUES ({1}, ?)"
      );
   }

   @Override
   protected void executeSQL(Connection var1, String var2, String var3, String var4) {
      PreparedStatement var5 = null;
      ResultSet var6 = null;
      SQLException var7 = null;
      int var8 = 0;

      while (true) {
         var8++;

         try {
            var5 = var1.prepareStatement(var3);
            var5.setString(1, var2);
            if (this.getLog().isDebugEnabled()) {
               this.getLog().debug("Lock '" + var2 + "' is being obtained: " + Thread.currentThread().getName());
            }

            var6 = var5.executeQuery();
            if (var6.next()) {
               break;
            }

            this.getLog().debug("Inserting new lock row for lock: '" + var2 + "' being obtained by thread: " + Thread.currentThread().getName());
            var6.close();
            var6 = null;
            var5.close();
            var5 = null;
            var5 = var1.prepareStatement(var4);
            var5.setString(1, var2);
            int var9 = var5.executeUpdate();
            if (var9 == 1) {
               break;
            }

            if (var8 >= 3) {
               throw new SQLException(
                  Util.rtp(
                     "No row exists, and one could not be inserted in table {0}LOCKS for lock named: " + var2,
                     this.getTablePrefix(),
                     this.getSchedulerNameLiteral()
                  )
               );
            }

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var28) {
               Thread.currentThread().interrupt();
            }
         } catch (SQLException var29) {
            if (var7 == null) {
               var7 = var29;
            }

            if (this.getLog().isDebugEnabled()) {
               this.getLog().debug("Lock '" + var2 + "' was not obtained by: " + Thread.currentThread().getName() + (var8 < 3 ? " - will try again." : ""));
            }

            if (var8 >= 3) {
               throw new LockException("Failure obtaining db row lock: " + var29.getMessage(), var29);
            }

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var27) {
               Thread.currentThread().interrupt();
            }
         } finally {
            if (var6 != null) {
               try {
                  var6.close();
               } catch (Exception var26) {
               }
            }

            if (var5 != null) {
               try {
                  var5.close();
               } catch (Exception var25) {
               }
            }
         }

         if (var8 >= 4) {
            throw new LockException(
               "Failure obtaining db row lock, reached maximum number of attempts. Initial exception (if any) attached as root cause.", var7
            );
         }
      }
   }

   protected String getSelectWithLockSQL() {
      return this.getSQL();
   }

   public void setSelectWithLockSQL(String var1) {
      this.setSQL(var1);
   }
}
