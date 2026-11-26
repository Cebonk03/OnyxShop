package org.quartz.impl.jdbcjobstore.oracle;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import oracle.sql.BLOB;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate;
import org.quartz.spi.OperableTrigger;

public class OracleDelegate extends StdJDBCDelegate {
   public static final String INSERT_ORACLE_JOB_DETAIL = "INSERT INTO {0}JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, EMPTY_BLOB())";
   public static final String UPDATE_ORACLE_JOB_DETAIL = "UPDATE {0}JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = EMPTY_BLOB()  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?";
   public static final String UPDATE_ORACLE_JOB_DETAIL_BLOB = "UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?";
   public static final String SELECT_ORACLE_JOB_DETAIL_BLOB = "SELECT JOB_DATA FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ? FOR UPDATE";
   public static final String UPDATE_ORACLE_TRIGGER = "UPDATE {0}TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   public static final String SELECT_ORACLE_TRIGGER_JOB_DETAIL_BLOB = "SELECT JOB_DATA FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? FOR UPDATE";
   public static final String UPDATE_ORACLE_TRIGGER_JOB_DETAIL_BLOB = "UPDATE {0}TRIGGERS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   public static final String UPDATE_ORACLE_TRIGGER_JOB_DETAIL_EMPTY_BLOB = "UPDATE {0}TRIGGERS SET JOB_DATA = EMPTY_BLOB()  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   public static final String INSERT_ORACLE_CALENDAR = "INSERT INTO {0}CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR)  VALUES({1}, ?, EMPTY_BLOB())";
   public static final String SELECT_ORACLE_CALENDAR_BLOB = "SELECT CALENDAR FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ? FOR UPDATE";
   public static final String UPDATE_ORACLE_CALENDAR_BLOB = "UPDATE {0}CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?";

   public OracleDelegate() {
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      InputStream var4 = var1.getBinaryStream(var2);
      if (var4 != null) {
         ObjectInputStream var5 = new ObjectInputStream(var4);

         try {
            var3 = var5.readObject();
         } finally {
            var5.close();
         }
      }

      return var3;
   }

   @Override
   public int insertJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      byte[] var4 = var3.toByteArray();
      PreparedStatement var5 = null;
      ResultSet var6 = null;

      int var9;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, EMPTY_BLOB())"
            )
         );
         var5.setString(1, var2.getKey().getName());
         var5.setString(2, var2.getKey().getGroup());
         var5.setString(3, var2.getDescription());
         var5.setString(4, var2.getJobClass().getName());
         this.setBoolean(var5, 5, var2.isDurable());
         this.setBoolean(var5, 6, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var5, 7, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var5, 8, var2.requestsRecovery());
         var5.executeUpdate();
         var5.close();
         var5 = var1.prepareStatement(this.rtp("SELECT JOB_DATA FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ? FOR UPDATE"));
         var5.setString(1, var2.getKey().getName());
         var5.setString(2, var2.getKey().getGroup());
         var6 = var5.executeQuery();
         int var7 = 0;
         Object var8 = null;
         if (!var6.next()) {
            return var7;
         }

         var8 = this.writeDataToBlob(var6, 1, var4);
         var6.close();
         var5.close();
         var5 = var1.prepareStatement(this.rtp("UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var5.setBlob(1, (Blob)var8);
         var5.setString(2, var2.getKey().getName());
         var5.setString(3, var2.getKey().getGroup());
         var7 = var5.executeUpdate();
         var9 = var7;
      } finally {
         closeResultSet(var6);
         closeStatement(var5);
      }

      return var9;
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      return this.canUseProperties() ? var1.getBinaryStream(var2) : this.getObjectFromBlob(var1, var2);
   }

   @Override
   public int updateJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      byte[] var4 = var3.toByteArray();
      PreparedStatement var5 = null;
      PreparedStatement var6 = null;
      ResultSet var7 = null;

      int var13;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = EMPTY_BLOB()  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"
            )
         );
         var5.setString(1, var2.getDescription());
         var5.setString(2, var2.getJobClass().getName());
         this.setBoolean(var5, 3, var2.isDurable());
         this.setBoolean(var5, 4, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var5, 5, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var5, 6, var2.requestsRecovery());
         var5.setString(7, var2.getKey().getName());
         var5.setString(8, var2.getKey().getGroup());
         var5.executeUpdate();
         var5.close();
         var5 = var1.prepareStatement(this.rtp("SELECT JOB_DATA FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ? FOR UPDATE"));
         var5.setString(1, var2.getKey().getName());
         var5.setString(2, var2.getKey().getGroup());
         var7 = var5.executeQuery();
         int var8 = 0;
         if (var7.next()) {
            Blob var9 = this.writeDataToBlob(var7, 1, var4);
            var6 = var1.prepareStatement(this.rtp("UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
            var6.setBlob(1, var9);
            var6.setString(2, var2.getKey().getName());
            var6.setString(3, var2.getKey().getGroup());
            var8 = var6.executeUpdate();
         }

         var13 = var8;
      } finally {
         closeResultSet(var7);
         closeStatement(var5);
         closeStatement(var6);
      }

      return var13;
   }

   @Override
   public int insertTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      byte[] var5 = null;
      if (var2.getJobDataMap().size() > 0) {
         var5 = this.serializeJobData(var2.getJobDataMap()).toByteArray();
      }

      PreparedStatement var6 = null;
      ResultSet var7 = null;
      int var8 = 0;

      try {
         var6 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA, PRIORITY)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
         );
         var6.setString(1, var2.getKey().getName());
         var6.setString(2, var2.getKey().getGroup());
         var6.setString(3, var2.getJobKey().getName());
         var6.setString(4, var2.getJobKey().getGroup());
         var6.setString(5, var2.getDescription());
         var6.setBigDecimal(6, new BigDecimal(String.valueOf(var2.getNextFireTime().getTime())));
         long var9 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var9 = var2.getPreviousFireTime().getTime();
         }

         var6.setBigDecimal(7, new BigDecimal(String.valueOf(var9)));
         var6.setString(8, var3);
         TriggerPersistenceDelegate var11 = this.findTriggerPersistenceDelegate(var2);
         String var12 = "BLOB";
         if (var11 != null) {
            var12 = var11.getHandledTriggerTypeDiscriminator();
         }

         var6.setString(9, var12);
         var6.setBigDecimal(10, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var13 = 0L;
         if (var2.getEndTime() != null) {
            var13 = var2.getEndTime().getTime();
         }

         var6.setBigDecimal(11, new BigDecimal(String.valueOf(var13)));
         var6.setString(12, var2.getCalendarName());
         var6.setInt(13, var2.getMisfireInstruction());
         var6.setBinaryStream(14, null, 0);
         var6.setInt(15, var2.getPriority());
         var8 = var6.executeUpdate();
         if (var5 != null) {
            var6.close();
            var6 = var1.prepareStatement(
               this.rtp("UPDATE {0}TRIGGERS SET JOB_DATA = EMPTY_BLOB()  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?")
            );
            var6.setString(1, var2.getKey().getName());
            var6.setString(2, var2.getKey().getGroup());
            var6.executeUpdate();
            var6.close();
            var6 = var1.prepareStatement(
               this.rtp("SELECT JOB_DATA FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? FOR UPDATE")
            );
            var6.setString(1, var2.getKey().getName());
            var6.setString(2, var2.getKey().getGroup());
            var7 = var6.executeQuery();
            Object var15 = null;
            if (!var7.next()) {
               return 0;
            }

            var15 = this.writeDataToBlob(var7, 1, var5);
            var7.close();
            var6.close();
            var6 = var1.prepareStatement(this.rtp("UPDATE {0}TRIGGERS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
            var6.setBlob(1, (Blob)var15);
            var6.setString(2, var2.getKey().getName());
            var6.setString(3, var2.getKey().getGroup());
            var6.executeUpdate();
         }

         if (var11 == null) {
            this.insertBlobTrigger(var1, var2);
         } else {
            var11.insertExtendedTriggerProperties(var1, var2, var3, var4);
         }

         return var8;
      } finally {
         closeResultSet(var7);
         closeStatement(var6);
      }
   }

   @Override
   public int updateTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      boolean var5 = var2.getJobDataMap().isDirty();
      byte[] var6 = null;
      if (var5 && var2.getJobDataMap().size() > 0) {
         var6 = this.serializeJobData(var2.getJobDataMap()).toByteArray();
      }

      PreparedStatement var7 = null;
      PreparedStatement var8 = null;
      ResultSet var9 = null;
      int var10 = 0;

      try {
         var7 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"
            )
         );
         var7.setString(1, var2.getJobKey().getName());
         var7.setString(2, var2.getJobKey().getGroup());
         var7.setString(3, var2.getDescription());
         long var11 = -1L;
         if (var2.getNextFireTime() != null) {
            var11 = var2.getNextFireTime().getTime();
         }

         var7.setBigDecimal(4, new BigDecimal(String.valueOf(var11)));
         long var13 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var13 = var2.getPreviousFireTime().getTime();
         }

         var7.setBigDecimal(5, new BigDecimal(String.valueOf(var13)));
         var7.setString(6, var3);
         TriggerPersistenceDelegate var15 = this.findTriggerPersistenceDelegate(var2);
         String var16 = "BLOB";
         if (var15 != null) {
            var16 = var15.getHandledTriggerTypeDiscriminator();
         }

         var7.setString(7, var16);
         var7.setBigDecimal(8, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var17 = 0L;
         if (var2.getEndTime() != null) {
            var17 = var2.getEndTime().getTime();
         }

         var7.setBigDecimal(9, new BigDecimal(String.valueOf(var17)));
         var7.setString(10, var2.getCalendarName());
         var7.setInt(11, var2.getMisfireInstruction());
         var7.setInt(12, var2.getPriority());
         var7.setString(13, var2.getKey().getName());
         var7.setString(14, var2.getKey().getGroup());
         var10 = var7.executeUpdate();
         if (var5) {
            var7.close();
            var7 = var1.prepareStatement(
               this.rtp("UPDATE {0}TRIGGERS SET JOB_DATA = EMPTY_BLOB()  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?")
            );
            var7.setString(1, var2.getKey().getName());
            var7.setString(2, var2.getKey().getGroup());
            var7.executeUpdate();
            var7.close();
            var7 = var1.prepareStatement(
               this.rtp("SELECT JOB_DATA FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? FOR UPDATE")
            );
            var7.setString(1, var2.getKey().getName());
            var7.setString(2, var2.getKey().getGroup());
            var9 = var7.executeQuery();
            if (var9.next()) {
               Blob var19 = this.writeDataToBlob(var9, 1, var6);
               var8 = var1.prepareStatement(this.rtp("UPDATE {0}TRIGGERS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
               var8.setBlob(1, var19);
               var8.setString(2, var2.getKey().getName());
               var8.setString(3, var2.getKey().getGroup());
               var8.executeUpdate();
            }
         }

         if (var15 == null) {
            this.updateBlobTrigger(var1, var2);
         } else {
            var15.updateExtendedTriggerProperties(var1, var2, var3, var4);
         }
      } finally {
         closeResultSet(var9);
         closeStatement(var7);
         closeStatement(var8);
      }

      return var10;
   }

   @Override
   public int insertCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      PreparedStatement var5 = null;
      PreparedStatement var6 = null;
      ResultSet var7 = null;

      int var9;
      try {
         var5 = var1.prepareStatement(this.rtp("INSERT INTO {0}CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR)  VALUES({1}, ?, EMPTY_BLOB())"));
         var5.setString(1, var2);
         var5.executeUpdate();
         var5.close();
         var5 = var1.prepareStatement(this.rtp("SELECT CALENDAR FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ? FOR UPDATE"));
         var5.setString(1, var2);
         var7 = var5.executeQuery();
         if (!var7.next()) {
            return 0;
         }

         Blob var8 = this.writeDataToBlob(var7, 1, var4.toByteArray());
         var6 = var1.prepareStatement(this.rtp("UPDATE {0}CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var6.setBlob(1, var8);
         var6.setString(2, var2);
         var9 = var6.executeUpdate();
      } finally {
         closeResultSet(var7);
         closeStatement(var5);
         closeStatement(var6);
      }

      return var9;
   }

   @Override
   public int updateCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      PreparedStatement var5 = null;
      PreparedStatement var6 = null;
      ResultSet var7 = null;

      int var9;
      try {
         var5 = var1.prepareStatement(this.rtp("SELECT CALENDAR FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ? FOR UPDATE"));
         var5.setString(1, var2);
         var7 = var5.executeQuery();
         if (!var7.next()) {
            return 0;
         }

         Blob var8 = this.writeDataToBlob(var7, 1, var4.toByteArray());
         var6 = var1.prepareStatement(this.rtp("UPDATE {0}CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var6.setBlob(1, var8);
         var6.setString(2, var2);
         var9 = var6.executeUpdate();
      } finally {
         closeResultSet(var7);
         closeStatement(var5);
         closeStatement(var6);
      }

      return var9;
   }

   @Override
   public int updateJobData(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      byte[] var4 = var3.toByteArray();
      PreparedStatement var5 = null;
      PreparedStatement var6 = null;
      ResultSet var7 = null;

      int var13;
      try {
         var5 = var1.prepareStatement(this.rtp("SELECT JOB_DATA FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ? FOR UPDATE"));
         var5.setString(1, var2.getKey().getName());
         var5.setString(2, var2.getKey().getGroup());
         var7 = var5.executeQuery();
         int var8 = 0;
         if (var7.next()) {
            Blob var9 = this.writeDataToBlob(var7, 1, var4);
            var6 = var1.prepareStatement(this.rtp("UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
            var6.setBlob(1, var9);
            var6.setString(2, var2.getKey().getName());
            var6.setString(3, var2.getKey().getGroup());
            var8 = var6.executeUpdate();
         }

         var13 = var8;
      } finally {
         closeResultSet(var7);
         closeStatement(var5);
         closeStatement(var6);
      }

      return var13;
   }

   protected Blob writeDataToBlob(ResultSet var1, int var2, byte[] var3) {
      Blob var4 = var1.getBlob(var2);
      if (var4 == null) {
         throw new SQLException("Driver's Blob representation is null!");
      } else if (var4 instanceof BLOB) {
         ((BLOB)var4).putBytes(1L, var3);
         ((BLOB)var4).trim(var3.length);
         return var4;
      } else {
         throw new SQLException("Driver's Blob representation is of an unsupported type: " + var4.getClass().getName());
      }
   }
}
