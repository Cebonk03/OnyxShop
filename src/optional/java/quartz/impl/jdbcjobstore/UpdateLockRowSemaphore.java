package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateLockRowSemaphore extends DBSemaphore {
   public static final String UPDATE_FOR_LOCK = "UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE SCHED_NAME = {1} AND LOCK_NAME = ? ";
   public static final String INSERT_LOCK = "INSERT INTO {0}LOCKS(SCHED_NAME, LOCK_NAME) VALUES ({1}, ?)";
   private static final int RETRY_COUNT = 2;

   public UpdateLockRowSemaphore() {
      super(
         "QRTZ_",
         null,
         "UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE SCHED_NAME = {1} AND LOCK_NAME = ? ",
         "INSERT INTO {0}LOCKS(SCHED_NAME, LOCK_NAME) VALUES ({1}, ?)"
      );
   }

   @Override
   protected void executeSQL(Connection var1, String var2, String var3, String var4) {
      SQLException var5 = null;

      for (int var6 = 0; var6 < 2; var6++) {
         try {
            if (!this.lockViaUpdate(var1, var2, var3)) {
               this.lockViaInsert(var1, var2, var4);
            }

            return;
         } catch (SQLException var10) {
            var5 = var10;
            if (var6 + 1 == 2) {
               this.getLog().debug("Lock '{}' was not obtained by: {}", var2, Thread.currentThread().getName());
            } else {
               this.getLog().debug("Lock '{}' was not obtained by: {} - will try again.", var2, Thread.currentThread().getName());
            }

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var9) {
               Thread.currentThread().interrupt();
            }
         }
      }

      throw new LockException("Failure obtaining db row lock: " + var5.getMessage(), var5);
   }

   protected String getUpdateLockRowSQL() {
      return this.getSQL();
   }

   public void setUpdateLockRowSQL(String var1) {
      this.setSQL(var1);
   }

   private boolean lockViaUpdate(Connection var1, String var2, String var3) {
      PreparedStatement var4 = var1.prepareStatement(var3);

      boolean var5;
      try {
         var4.setString(1, var2);
         this.getLog().debug("Lock '" + var2 + "' is being obtained: " + Thread.currentThread().getName());
         var5 = var4.executeUpdate() >= 1;
      } finally {
         var4.close();
      }

      return var5;
   }

   private void lockViaInsert(Connection var1, String var2, String var3) {
      this.getLog().debug("Inserting new lock row for lock: '" + var2 + "' being obtained by thread: " + Thread.currentThread().getName());
      PreparedStatement var4 = var1.prepareStatement(var3);

      try {
         var4.setString(1, var2);
         if (var4.executeUpdate() != 1) {
            throw new SQLException(
               Util.rtp(
                  "No row exists, and one could not be inserted in table {0}LOCKS for lock named: " + var2,
                  this.getTablePrefix(),
                  this.getSchedulerNameLiteral()
               )
            );
         }
      } finally {
         var4.close();
      }
   }
}
