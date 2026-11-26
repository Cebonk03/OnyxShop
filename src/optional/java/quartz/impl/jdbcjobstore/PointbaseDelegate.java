package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.spi.OperableTrigger;

public class PointbaseDelegate extends StdJDBCDelegate {
   public PointbaseDelegate() {
   }

   @Override
   public int insertJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      int var4 = var3.toByteArray().length;
      ByteArrayInputStream var5 = new ByteArrayInputStream(var3.toByteArray());
      PreparedStatement var6 = null;
      int var7 = 0;

      try {
         var6 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
         );
         var6.setString(1, var2.getKey().getName());
         var6.setString(2, var2.getKey().getGroup());
         var6.setString(3, var2.getDescription());
         var6.setString(4, var2.getJobClass().getName());
         this.setBoolean(var6, 5, var2.isDurable());
         this.setBoolean(var6, 6, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var6, 7, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var6, 8, var2.requestsRecovery());
         var6.setBinaryStream(9, var5, var4);
         var7 = var6.executeUpdate();
      } finally {
         closeStatement(var6);
      }

      return var7;
   }

   @Override
   public int updateJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      int var4 = var3.toByteArray().length;
      ByteArrayInputStream var5 = new ByteArrayInputStream(var3.toByteArray());
      PreparedStatement var6 = null;
      int var7 = 0;

      try {
         var6 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"
            )
         );
         var6.setString(1, var2.getDescription());
         var6.setString(2, var2.getJobClass().getName());
         this.setBoolean(var6, 3, var2.isDurable());
         this.setBoolean(var6, 4, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var6, 5, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var6, 6, var2.requestsRecovery());
         var6.setBinaryStream(7, var5, var4);
         var6.setString(8, var2.getKey().getName());
         var6.setString(9, var2.getKey().getGroup());
         var7 = var6.executeUpdate();
      } finally {
         closeStatement(var6);
      }

      return var7;
   }

   @Override
   public int insertTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      ByteArrayOutputStream var5 = this.serializeJobData(var2.getJobDataMap());
      int var6 = var5.toByteArray().length;
      ByteArrayInputStream var7 = new ByteArrayInputStream(var5.toByteArray());
      PreparedStatement var8 = null;
      int var9 = 0;

      try {
         var8 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA, PRIORITY)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
         );
         var8.setString(1, var2.getKey().getName());
         var8.setString(2, var2.getKey().getGroup());
         var8.setString(3, var2.getJobKey().getName());
         var8.setString(4, var2.getJobKey().getGroup());
         var8.setString(5, var2.getDescription());
         var8.setBigDecimal(6, new BigDecimal(String.valueOf(var2.getNextFireTime().getTime())));
         long var10 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var10 = var2.getPreviousFireTime().getTime();
         }

         var8.setBigDecimal(7, new BigDecimal(String.valueOf(var10)));
         var8.setString(8, var3);
         TriggerPersistenceDelegate var12 = this.findTriggerPersistenceDelegate(var2);
         String var13 = "BLOB";
         if (var12 != null) {
            var13 = var12.getHandledTriggerTypeDiscriminator();
         }

         var8.setString(9, var13);
         var8.setBigDecimal(10, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var14 = 0L;
         if (var2.getEndTime() != null) {
            var14 = var2.getEndTime().getTime();
         }

         var8.setBigDecimal(11, new BigDecimal(String.valueOf(var14)));
         var8.setString(12, var2.getCalendarName());
         var8.setInt(13, var2.getMisfireInstruction());
         var8.setBinaryStream(14, var7, var6);
         var8.setInt(15, var2.getPriority());
         var9 = var8.executeUpdate();
         if (var12 == null) {
            this.insertBlobTrigger(var1, var2);
         } else {
            var12.insertExtendedTriggerProperties(var1, var2, var3, var4);
         }
      } finally {
         closeStatement(var8);
      }

      return var9;
   }

   @Override
   public int updateTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      ByteArrayOutputStream var5 = this.serializeJobData(var2.getJobDataMap());
      int var6 = var5.toByteArray().length;
      ByteArrayInputStream var7 = new ByteArrayInputStream(var5.toByteArray());
      PreparedStatement var8 = null;
      int var9 = 0;

      try {
         var8 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ?, JOB_DATA = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"
            )
         );
         var8.setString(1, var2.getJobKey().getName());
         var8.setString(2, var2.getJobKey().getGroup());
         var8.setString(3, var2.getDescription());
         long var10 = -1L;
         if (var2.getNextFireTime() != null) {
            var10 = var2.getNextFireTime().getTime();
         }

         var8.setBigDecimal(4, new BigDecimal(String.valueOf(var10)));
         long var12 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var12 = var2.getPreviousFireTime().getTime();
         }

         var8.setBigDecimal(5, new BigDecimal(String.valueOf(var12)));
         var8.setString(6, var3);
         TriggerPersistenceDelegate var14 = this.findTriggerPersistenceDelegate(var2);
         String var15 = "BLOB";
         if (var14 != null) {
            var15 = var14.getHandledTriggerTypeDiscriminator();
         }

         var8.setString(7, var15);
         var8.setBigDecimal(8, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var16 = 0L;
         if (var2.getEndTime() != null) {
            var16 = var2.getEndTime().getTime();
         }

         var8.setBigDecimal(9, new BigDecimal(String.valueOf(var16)));
         var8.setString(10, var2.getCalendarName());
         var8.setInt(11, var2.getMisfireInstruction());
         var8.setInt(12, var2.getPriority());
         var8.setBinaryStream(13, var7, var6);
         var8.setString(14, var2.getKey().getName());
         var8.setString(15, var2.getKey().getGroup());
         var9 = var8.executeUpdate();
         if (var14 == null) {
            this.updateBlobTrigger(var1, var2);
         } else {
            var14.updateExtendedTriggerProperties(var1, var2, var3, var4);
         }
      } finally {
         closeStatement(var8);
      }

      return var9;
   }

   @Override
   public int updateJobData(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      int var4 = var3.toByteArray().length;
      ByteArrayInputStream var5 = new ByteArrayInputStream(var3.toByteArray());
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(this.rtp("UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var6.setBinaryStream(1, var5, var4);
         var6.setString(2, var2.getKey().getName());
         var6.setString(3, var2.getKey().getGroup());
         var7 = var6.executeUpdate();
      } finally {
         closeStatement(var6);
      }

      return var7;
   }

   @Override
   public int insertCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      byte[] var5 = var4.toByteArray();
      ByteArrayInputStream var6 = new ByteArrayInputStream(var5);
      PreparedStatement var7 = null;

      int var8;
      try {
         var7 = var1.prepareStatement(this.rtp("INSERT INTO {0}CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR)  VALUES({1}, ?, ?)"));
         var7.setString(1, var2);
         var7.setBinaryStream(2, var6, var5.length);
         var8 = var7.executeUpdate();
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   @Override
   public int updateCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      byte[] var5 = var4.toByteArray();
      ByteArrayInputStream var6 = new ByteArrayInputStream(var5);
      PreparedStatement var7 = null;

      int var8;
      try {
         var7 = var1.prepareStatement(this.rtp("UPDATE {0}CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var7.setBinaryStream(1, var6, var5.length);
         var7.setString(2, var2);
         var8 = var7.executeUpdate();
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      byte[] var4 = var1.getBytes(var2);
      ByteArrayInputStream var5 = new ByteArrayInputStream(var4);
      if (null != var5 && var5.available() != 0) {
         ObjectInputStream var6 = new ObjectInputStream(var5);

         try {
            var3 = var6.readObject();
         } finally {
            var6.close();
         }
      }

      return var3;
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         byte[] var3 = var1.getBytes(var2);
         return var3 == null ? null : new ByteArrayInputStream(var3);
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }
}
