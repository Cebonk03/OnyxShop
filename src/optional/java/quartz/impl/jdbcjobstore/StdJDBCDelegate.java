package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;

public class StdJDBCDelegate implements DriverDelegate, StdJDBCConstants {
   protected Logger logger = null;
   protected String tablePrefix = "QRTZ_";
   protected String instanceId;
   protected String schedName;
   protected boolean useProperties;
   protected ClassLoadHelper classLoadHelper;
   protected List<TriggerPersistenceDelegate> triggerPersistenceDelegates = new LinkedList<>();
   private String schedNameLiteral = null;

   public StdJDBCDelegate() {
   }

   @Override
   public void initialize(Logger var1, String var2, String var3, String var4, ClassLoadHelper var5, boolean var6, String var7) {
      this.logger = var1;
      this.tablePrefix = var2;
      this.schedName = var3;
      this.instanceId = var4;
      this.useProperties = var6;
      this.classLoadHelper = var5;
      this.addDefaultTriggerPersistenceDelegates();
      if (var7 != null) {
         String[] var8 = var7.split("\\|");

         for (String var12 : var8) {
            String[] var13 = var12.split("=");
            String var14 = var13[0];
            if (var13.length != 1 && var13[1] != null && !var13[1].equals("")) {
               if (!var14.equals("triggerPersistenceDelegateClasses")) {
                  throw new NoSuchDelegateException("Unknown setting: '" + var14 + "'");
               }

               String[] var15 = var13[1].split(",");

               for (String var19 : var15) {
                  try {
                     Class var20 = var5.loadClass(var19);
                     this.addTriggerPersistenceDelegate((TriggerPersistenceDelegate)var20.newInstance());
                  } catch (Exception var21) {
                     throw new NoSuchDelegateException("Error instantiating TriggerPersistenceDelegate of type: " + var19, var21);
                  }
               }
            }
         }
      }
   }

   protected void addDefaultTriggerPersistenceDelegates() {
      this.addTriggerPersistenceDelegate(new SimpleTriggerPersistenceDelegate());
      this.addTriggerPersistenceDelegate(new CronTriggerPersistenceDelegate());
      this.addTriggerPersistenceDelegate(new CalendarIntervalTriggerPersistenceDelegate());
      this.addTriggerPersistenceDelegate(new DailyTimeIntervalTriggerPersistenceDelegate());
   }

   protected boolean canUseProperties() {
      return this.useProperties;
   }

   public void addTriggerPersistenceDelegate(TriggerPersistenceDelegate var1) {
      this.logger.debug("Adding TriggerPersistenceDelegate of type: " + var1.getClass().getCanonicalName());
      var1.initialize(this.tablePrefix, this.schedName);
      this.triggerPersistenceDelegates.add(var1);
   }

   public TriggerPersistenceDelegate findTriggerPersistenceDelegate(OperableTrigger var1) {
      for (TriggerPersistenceDelegate var3 : this.triggerPersistenceDelegates) {
         if (var3.canHandleTriggerType(var1)) {
            return var3;
         }
      }

      return null;
   }

   public TriggerPersistenceDelegate findTriggerPersistenceDelegate(String var1) {
      for (TriggerPersistenceDelegate var3 : this.triggerPersistenceDelegates) {
         if (var3.getHandledTriggerTypeDiscriminator().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   @Override
   public int updateTriggerStatesFromOtherStates(Connection var1, String var2, String var3, String var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ?)"));
         var5.setString(1, var2);
         var5.setString(2, var3);
         var5.setString(3, var4);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public List<TriggerKey> selectMisfiredTriggers(Connection var1, long var2) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      LinkedList var12;
      try {
         var4 = var1.prepareStatement(
            this.rtp(
               "SELECT * FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC"
            )
         );
         var4.setBigDecimal(1, new BigDecimal(String.valueOf(var2)));
         var5 = var4.executeQuery();
         LinkedList var6 = new LinkedList();

         while (var5.next()) {
            String var7 = var5.getString("TRIGGER_NAME");
            String var8 = var5.getString("TRIGGER_GROUP");
            var6.add(TriggerKey.triggerKey(var7, var8));
         }

         var12 = var6;
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var12;
   }

   @Override
   public List<TriggerKey> selectTriggersInState(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      LinkedList var6;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         LinkedList var5 = new LinkedList();

         while (var4.next()) {
            var5.add(TriggerKey.triggerKey(var4.getString(1), var4.getString(2)));
         }

         var6 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var6;
   }

   @Override
   public List<TriggerKey> selectMisfiredTriggersInState(Connection var1, String var2, long var3) {
      PreparedStatement var5 = null;
      ResultSet var6 = null;

      LinkedList var13;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC"
            )
         );
         var5.setBigDecimal(1, new BigDecimal(String.valueOf(var3)));
         var5.setString(2, var2);
         var6 = var5.executeQuery();
         LinkedList var7 = new LinkedList();

         while (var6.next()) {
            String var8 = var6.getString("TRIGGER_NAME");
            String var9 = var6.getString("TRIGGER_GROUP");
            var7.add(TriggerKey.triggerKey(var8, var9));
         }

         var13 = var7;
      } finally {
         closeResultSet(var6);
         closeStatement(var5);
      }

      return var13;
   }

   @Override
   public boolean hasMisfiredTriggersInState(Connection var1, String var2, long var3, int var5, List<TriggerKey> var6) {
      PreparedStatement var7 = null;
      ResultSet var8 = null;

      boolean var15;
      try {
         var7 = var1.prepareStatement(
            this.rtp(
               "SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC"
            )
         );
         var7.setBigDecimal(1, new BigDecimal(String.valueOf(var3)));
         var7.setString(2, var2);
         var8 = var7.executeQuery();
         boolean var9 = false;

         while (var8.next() && !var9) {
            if (var6.size() == var5) {
               var9 = true;
            } else {
               String var10 = var8.getString("TRIGGER_NAME");
               String var11 = var8.getString("TRIGGER_GROUP");
               var6.add(TriggerKey.triggerKey(var10, var11));
            }
         }

         var15 = var9;
      } finally {
         closeResultSet(var8);
         closeStatement(var7);
      }

      return var15;
   }

   @Override
   public int countMisfiredTriggersInState(Connection var1, String var2, long var3) {
      PreparedStatement var5 = null;
      ResultSet var6 = null;

      int var7;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "SELECT COUNT(TRIGGER_NAME) FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ?"
            )
         );
         var5.setBigDecimal(1, new BigDecimal(String.valueOf(var3)));
         var5.setString(2, var2);
         var6 = var5.executeQuery();
         if (!var6.next()) {
            throw new SQLException("No misfired trigger count returned.");
         }

         var7 = var6.getInt(1);
      } finally {
         closeResultSet(var6);
         closeStatement(var5);
      }

      return var7;
   }

   @Override
   public List<TriggerKey> selectMisfiredTriggersInGroupInState(Connection var1, String var2, String var3, long var4) {
      PreparedStatement var6 = null;
      ResultSet var7 = null;

      LinkedList var13;
      try {
         var6 = var1.prepareStatement(
            this.rtp(
               "SELECT TRIGGER_NAME FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_GROUP = ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC"
            )
         );
         var6.setBigDecimal(1, new BigDecimal(String.valueOf(var4)));
         var6.setString(2, var2);
         var6.setString(3, var3);
         var7 = var6.executeQuery();
         LinkedList var8 = new LinkedList();

         while (var7.next()) {
            String var9 = var7.getString("TRIGGER_NAME");
            var8.add(TriggerKey.triggerKey(var9, var2));
         }

         var13 = var8;
      } finally {
         closeResultSet(var7);
         closeStatement(var6);
      }

      return var13;
   }

   @Override
   public List<OperableTrigger> selectTriggersForRecoveringJobs(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      LinkedList var21;
      try {
         var2 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ? AND REQUESTS_RECOVERY = ?"));
         var2.setString(1, this.instanceId);
         this.setBoolean(var2, 2, true);
         var3 = var2.executeQuery();
         long var4 = System.currentTimeMillis();
         LinkedList var6 = new LinkedList();

         while (var3.next()) {
            String var7 = var3.getString("JOB_NAME");
            String var8 = var3.getString("JOB_GROUP");
            String var9 = var3.getString("TRIGGER_NAME");
            String var10 = var3.getString("TRIGGER_GROUP");
            long var11 = var3.getLong("FIRED_TIME");
            long var13 = var3.getLong("SCHED_TIME");
            int var15 = var3.getInt("PRIORITY");
            SimpleTriggerImpl var16 = new SimpleTriggerImpl("recover_" + this.instanceId + "_" + var4++, "RECOVERING_JOBS", new Date(var13));
            var16.setJobName(var7);
            var16.setJobGroup(var8);
            var16.setPriority(var15);
            var16.setMisfireInstruction(-1);
            JobDataMap var17 = this.selectTriggerJobDataMap(var1, var9, var10);
            var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_NAME", var9);
            var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_GROUP", var10);
            var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var11));
            var17.put("QRTZ_FAILED_JOB_ORIG_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS_AS_STRING", String.valueOf(var13));
            var16.setJobDataMap(var17);
            var6.add(var16);
         }

         var21 = var6;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var21;
   }

   @Override
   public int deleteFiredTriggers(Connection var1) {
      PreparedStatement var2 = null;

      int var3;
      try {
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeUpdate();
      } finally {
         closeStatement(var2);
      }

      return var3;
   }

   @Override
   public int deleteFiredTriggers(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public void clearData(Connection var1) {
      PreparedStatement var2 = null;

      try {
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}SIMPLE_TRIGGERS  WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}SIMPROP_TRIGGERS  WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}CRON_TRIGGERS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}BLOB_TRIGGERS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}TRIGGERS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}CALENDARS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
         var2.close();
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1}"));
         var2.executeUpdate();
      } finally {
         closeStatement(var2);
      }
   }

   @Override
   public int insertJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      PreparedStatement var4 = null;
      int var5 = 0;

      try {
         var4 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
         );
         var4.setString(1, var2.getKey().getName());
         var4.setString(2, var2.getKey().getGroup());
         var4.setString(3, var2.getDescription());
         var4.setString(4, var2.getJobClass().getName());
         this.setBoolean(var4, 5, var2.isDurable());
         this.setBoolean(var4, 6, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var4, 7, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var4, 8, var2.requestsRecovery());
         this.setBytes(var4, 9, var3);
         var5 = var4.executeUpdate();
      } finally {
         closeStatement(var4);
      }

      return var5;
   }

   @Override
   public int updateJobDetail(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      PreparedStatement var4 = null;
      int var5 = 0;

      try {
         var4 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"
            )
         );
         var4.setString(1, var2.getDescription());
         var4.setString(2, var2.getJobClass().getName());
         this.setBoolean(var4, 3, var2.isDurable());
         this.setBoolean(var4, 4, var2.isConcurrentExectionDisallowed());
         this.setBoolean(var4, 5, var2.isPersistJobDataAfterExecution());
         this.setBoolean(var4, 6, var2.requestsRecovery());
         this.setBytes(var4, 7, var3);
         var4.setString(8, var2.getKey().getName());
         var4.setString(9, var2.getKey().getGroup());
         var5 = var4.executeUpdate();
      } finally {
         closeStatement(var4);
      }

      return var5;
   }

   @Override
   public List<TriggerKey> selectTriggerKeysForJob(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      LinkedList var11;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         LinkedList var5 = new LinkedList();

         while (var4.next()) {
            String var6 = var4.getString("TRIGGER_NAME");
            String var7 = var4.getString("TRIGGER_GROUP");
            var5.add(TriggerKey.triggerKey(var6, var7));
         }

         var11 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var11;
   }

   @Override
   public int deleteJobDetail(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         if (this.logger.isDebugEnabled()) {
            this.logger.debug("Deleting job: " + var2);
         }

         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public boolean isJobNonConcurrent(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT IS_NONCONCURRENT FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (var4.next()) {
            return this.getBoolean(var4, "IS_NONCONCURRENT");
         }

         var5 = false;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public boolean jobExists(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT JOB_NAME FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (!var4.next()) {
            return false;
         }

         var5 = true;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int updateJobData(Connection var1, JobDetail var2) {
      ByteArrayOutputStream var3 = this.serializeJobData(var2.getJobDataMap());
      PreparedStatement var4 = null;

      int var5;
      try {
         var4 = var1.prepareStatement(this.rtp("UPDATE {0}JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         this.setBytes(var4, 1, var3);
         var4.setString(2, var2.getKey().getName());
         var4.setString(3, var2.getKey().getGroup());
         var5 = var4.executeUpdate();
      } finally {
         closeStatement(var4);
      }

      return var5;
   }

   @Override
   public JobDetail selectJobDetail(Connection var1, JobKey var2, ClassLoadHelper var3) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      JobDetailImpl var12;
      try {
         var4 = var1.prepareStatement(this.rtp("SELECT * FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var4.setString(1, var2.getName());
         var4.setString(2, var2.getGroup());
         var5 = var4.executeQuery();
         JobDetailImpl var6 = null;
         if (var5.next()) {
            var6 = new JobDetailImpl();
            var6.setName(var5.getString("JOB_NAME"));
            var6.setGroup(var5.getString("JOB_GROUP"));
            var6.setDescription(var5.getString("DESCRIPTION"));
            var6.setJobClass(var3.loadClass(var5.getString("JOB_CLASS_NAME"), Job.class));
            var6.setDurability(this.getBoolean(var5, "IS_DURABLE"));
            var6.setRequestsRecovery(this.getBoolean(var5, "REQUESTS_RECOVERY"));
            Object var7 = null;
            if (this.canUseProperties()) {
               var7 = this.getMapFromProperties(var5);
            } else {
               var7 = (Map)this.getObjectFromBlob(var5, "JOB_DATA");
            }

            if (null != var7) {
               var6.setJobDataMap(new JobDataMap((Map<?, ?>)var7));
            }
         }

         var12 = var6;
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var12;
   }

   private Map<?, ?> getMapFromProperties(ResultSet var1) {
      InputStream var3 = (InputStream)this.getJobDataFromBlob(var1, "JOB_DATA");
      if (var3 == null) {
         return null;
      } else {
         Properties var4 = new Properties();
         if (var3 != null) {
            try {
               var4.load(var3);
            } finally {
               var3.close();
            }
         }

         return this.convertFromProperty(var4);
      }
   }

   @Override
   public int selectNumJobs(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(JOB_NAME)  FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         if (var3.next()) {
            var4 = var3.getInt(1);
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public List<String> selectJobGroups(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      LinkedList var5;
      try {
         var2 = var1.prepareStatement(this.rtp("SELECT DISTINCT(JOB_GROUP) FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         LinkedList var4 = new LinkedList();

         while (var3.next()) {
            var4.add(var3.getString(1));
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public Set<JobKey> selectJobsInGroup(Connection var1, GroupMatcher<JobKey> var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      HashSet var6;
      try {
         if (this.isMatcherEquals(var2)) {
            var3 = var1.prepareStatement(this.rtp("SELECT JOB_NAME, JOB_GROUP FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_GROUP = ?"));
            var3.setString(1, this.toSqlEqualsClause(var2));
         } else {
            var3 = var1.prepareStatement(this.rtp("SELECT JOB_NAME, JOB_GROUP FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1} AND JOB_GROUP LIKE ?"));
            var3.setString(1, this.toSqlLikeClause(var2));
         }

         var4 = var3.executeQuery();
         LinkedList var5 = new LinkedList();

         while (var4.next()) {
            var5.add(JobKey.jobKey(var4.getString(1), var4.getString(2)));
         }

         var6 = new HashSet(var5);
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var6;
   }

   protected boolean isMatcherEquals(GroupMatcher<?> var1) {
      return var1.getCompareWithOperator().equals(StringMatcher.StringOperatorName.EQUALS);
   }

   protected String toSqlEqualsClause(GroupMatcher<?> var1) {
      return var1.getCompareToValue();
   }

   protected String toSqlLikeClause(GroupMatcher<?> var1) {
      String var2;
      switch (var1.getCompareWithOperator()) {
         case EQUALS:
            var2 = var1.getCompareToValue();
            break;
         case CONTAINS:
            var2 = "%" + var1.getCompareToValue() + "%";
            break;
         case ENDS_WITH:
            var2 = "%" + var1.getCompareToValue();
            break;
         case STARTS_WITH:
            var2 = var1.getCompareToValue() + "%";
            break;
         case ANYTHING:
            var2 = "%";
            break;
         default:
            throw new UnsupportedOperationException("Don't know how to translate " + var1.getCompareWithOperator() + " into SQL");
      }

      return var2;
   }

   @Override
   public int insertTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      ByteArrayOutputStream var5 = null;
      if (var2.getJobDataMap().size() > 0) {
         var5 = this.serializeJobData(var2.getJobDataMap());
      }

      PreparedStatement var6 = null;
      int var7 = 0;

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
         if (var2.getNextFireTime() != null) {
            var6.setBigDecimal(6, new BigDecimal(String.valueOf(var2.getNextFireTime().getTime())));
         } else {
            var6.setBigDecimal(6, null);
         }

         long var8 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var8 = var2.getPreviousFireTime().getTime();
         }

         var6.setBigDecimal(7, new BigDecimal(String.valueOf(var8)));
         var6.setString(8, var3);
         TriggerPersistenceDelegate var10 = this.findTriggerPersistenceDelegate(var2);
         String var11 = "BLOB";
         if (var10 != null) {
            var11 = var10.getHandledTriggerTypeDiscriminator();
         }

         var6.setString(9, var11);
         var6.setBigDecimal(10, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var12 = 0L;
         if (var2.getEndTime() != null) {
            var12 = var2.getEndTime().getTime();
         }

         var6.setBigDecimal(11, new BigDecimal(String.valueOf(var12)));
         var6.setString(12, var2.getCalendarName());
         var6.setInt(13, var2.getMisfireInstruction());
         this.setBytes(var6, 14, var5);
         var6.setInt(15, var2.getPriority());
         var7 = var6.executeUpdate();
         if (var10 == null) {
            this.insertBlobTrigger(var1, var2);
         } else {
            var10.insertExtendedTriggerProperties(var1, var2, var3, var4);
         }
      } finally {
         closeStatement(var6);
      }

      return var7;
   }

   public int insertBlobTrigger(Connection var1, OperableTrigger var2) {
      PreparedStatement var3 = null;
      ByteArrayOutputStream var4 = null;

      int var8;
      try {
         var4 = new ByteArrayOutputStream();
         ObjectOutputStream var5 = new ObjectOutputStream(var4);
         var5.writeObject(var2);
         var5.close();
         byte[] var6 = var4.toByteArray();
         ByteArrayInputStream var7 = new ByteArrayInputStream(var6);
         var3 = var1.prepareStatement(this.rtp("INSERT INTO {0}BLOB_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, BLOB_DATA)  VALUES({1}, ?, ?, ?)"));
         var3.setString(1, var2.getKey().getName());
         var3.setString(2, var2.getKey().getGroup());
         var3.setBinaryStream(3, var7, var6.length);
         var8 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var8;
   }

   @Override
   public int updateTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      boolean var5 = var2.getJobDataMap().isDirty();
      ByteArrayOutputStream var6 = null;
      if (var5) {
         var6 = this.serializeJobData(var2.getJobDataMap());
      }

      PreparedStatement var7 = null;
      int var8 = 0;

      try {
         if (var5) {
            var7 = var1.prepareStatement(
               this.rtp(
                  "UPDATE {0}TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ?, JOB_DATA = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"
               )
            );
         } else {
            var7 = var1.prepareStatement(
               this.rtp(
                  "UPDATE {0}TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"
               )
            );
         }

         var7.setString(1, var2.getJobKey().getName());
         var7.setString(2, var2.getJobKey().getGroup());
         var7.setString(3, var2.getDescription());
         long var9 = -1L;
         if (var2.getNextFireTime() != null) {
            var9 = var2.getNextFireTime().getTime();
         }

         var7.setBigDecimal(4, new BigDecimal(String.valueOf(var9)));
         long var11 = -1L;
         if (var2.getPreviousFireTime() != null) {
            var11 = var2.getPreviousFireTime().getTime();
         }

         var7.setBigDecimal(5, new BigDecimal(String.valueOf(var11)));
         var7.setString(6, var3);
         TriggerPersistenceDelegate var13 = this.findTriggerPersistenceDelegate(var2);
         String var14 = "BLOB";
         if (var13 != null) {
            var14 = var13.getHandledTriggerTypeDiscriminator();
         }

         var7.setString(7, var14);
         var7.setBigDecimal(8, new BigDecimal(String.valueOf(var2.getStartTime().getTime())));
         long var15 = 0L;
         if (var2.getEndTime() != null) {
            var15 = var2.getEndTime().getTime();
         }

         var7.setBigDecimal(9, new BigDecimal(String.valueOf(var15)));
         var7.setString(10, var2.getCalendarName());
         var7.setInt(11, var2.getMisfireInstruction());
         var7.setInt(12, var2.getPriority());
         if (var5) {
            this.setBytes(var7, 13, var6);
            var7.setString(14, var2.getKey().getName());
            var7.setString(15, var2.getKey().getGroup());
         } else {
            var7.setString(13, var2.getKey().getName());
            var7.setString(14, var2.getKey().getGroup());
         }

         var8 = var7.executeUpdate();
         if (var13 == null) {
            this.updateBlobTrigger(var1, var2);
         } else {
            var13.updateExtendedTriggerProperties(var1, var2, var3, var4);
         }
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   public int updateBlobTrigger(Connection var1, OperableTrigger var2) {
      PreparedStatement var3 = null;
      ByteArrayOutputStream var4 = null;

      int var8;
      try {
         var4 = new ByteArrayOutputStream();
         ObjectOutputStream var5 = new ObjectOutputStream(var4);
         var5.writeObject(var2);
         var5.close();
         byte[] var6 = var4.toByteArray();
         ByteArrayInputStream var7 = new ByteArrayInputStream(var6);
         var3 = var1.prepareStatement(this.rtp("UPDATE {0}BLOB_TRIGGERS SET BLOB_DATA = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setBinaryStream(1, var7, var6.length);
         var3.setString(2, var2.getKey().getName());
         var3.setString(3, var2.getKey().getGroup());
         var8 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
         if (var4 != null) {
            var4.close();
         }
      }

      return var8;
   }

   @Override
   public boolean triggerExists(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (!var4.next()) {
            return false;
         }

         var5 = true;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int updateTriggerState(Connection var1, TriggerKey var2, String var3) {
      PreparedStatement var4 = null;

      int var5;
      try {
         var4 = var1.prepareStatement(this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var4.setString(1, var3);
         var4.setString(2, var2.getName());
         var4.setString(3, var2.getGroup());
         var5 = var4.executeUpdate();
      } finally {
         closeStatement(var4);
      }

      return var5;
   }

   @Override
   public int updateTriggerStateFromOtherStates(Connection var1, TriggerKey var2, String var3, String var4, String var5, String var6) {
      PreparedStatement var7 = null;

      int var8;
      try {
         var7 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ? OR TRIGGER_STATE = ?)"
            )
         );
         var7.setString(1, var3);
         var7.setString(2, var2.getName());
         var7.setString(3, var2.getGroup());
         var7.setString(4, var4);
         var7.setString(5, var5);
         var7.setString(6, var6);
         var8 = var7.executeUpdate();
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   @Override
   public int updateTriggerGroupStateFromOtherStates(Connection var1, GroupMatcher<TriggerKey> var2, String var3, String var4, String var5, String var6) {
      PreparedStatement var7 = null;

      int var8;
      try {
         var7 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ? AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ? OR TRIGGER_STATE = ?)"
            )
         );
         var7.setString(1, var3);
         var7.setString(2, this.toSqlLikeClause(var2));
         var7.setString(3, var4);
         var7.setString(4, var5);
         var7.setString(5, var6);
         var8 = var7.executeUpdate();
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   @Override
   public int updateTriggerStateFromOtherState(Connection var1, TriggerKey var2, String var3, String var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(
            this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? AND TRIGGER_STATE = ?")
         );
         var5.setString(1, var3);
         var5.setString(2, var2.getName());
         var5.setString(3, var2.getGroup());
         var5.setString(4, var4);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public int updateTriggerGroupStateFromOtherState(Connection var1, GroupMatcher<TriggerKey> var2, String var3, String var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(
            this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ? AND TRIGGER_STATE = ?")
         );
         var5.setString(1, var3);
         var5.setString(2, this.toSqlLikeClause(var2));
         var5.setString(3, var4);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public int updateTriggerStatesForJob(Connection var1, JobKey var2, String var3) {
      PreparedStatement var4 = null;

      int var5;
      try {
         var4 = var1.prepareStatement(this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var4.setString(1, var3);
         var4.setString(2, var2.getName());
         var4.setString(3, var2.getGroup());
         var5 = var4.executeUpdate();
      } finally {
         closeStatement(var4);
      }

      return var5;
   }

   @Override
   public int updateTriggerStatesForJobFromOtherState(Connection var1, JobKey var2, String var3, String var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(
            this.rtp("UPDATE {0}TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ? AND TRIGGER_STATE = ?")
         );
         var5.setString(1, var3);
         var5.setString(2, var2.getName());
         var5.setString(3, var2.getGroup());
         var5.setString(4, var4);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   public int deleteBlobTrigger(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}BLOB_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public int deleteTrigger(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      this.deleteTriggerExtension(var1, var2);

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   protected void deleteTriggerExtension(Connection var1, TriggerKey var2) {
      for (TriggerPersistenceDelegate var4 : this.triggerPersistenceDelegates) {
         if (var4.deleteExtendedTriggerProperties(var1, var2) > 0) {
            return;
         }
      }

      this.deleteBlobTrigger(var1, var2);
   }

   @Override
   public int selectNumTriggersForJob(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT COUNT(TRIGGER_NAME) FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (!var4.next()) {
            return 0;
         }

         var5 = var4.getInt(1);
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public JobDetail selectJobForTrigger(Connection var1, ClassLoadHelper var2, TriggerKey var3) {
      return this.selectJobForTrigger(var1, var2, var3, true);
   }

   @Override
   public JobDetail selectJobForTrigger(Connection var1, ClassLoadHelper var2, TriggerKey var3, boolean var4) {
      PreparedStatement var5 = null;
      ResultSet var6 = null;

      JobDetailImpl var8;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "SELECT J.JOB_NAME, J.JOB_GROUP, J.IS_DURABLE, J.JOB_CLASS_NAME, J.REQUESTS_RECOVERY FROM {0}TRIGGERS T, {0}JOB_DETAILS J WHERE T.SCHED_NAME = {1} AND J.SCHED_NAME = {1} AND T.TRIGGER_NAME = ? AND T.TRIGGER_GROUP = ? AND T.JOB_NAME = J.JOB_NAME AND T.JOB_GROUP = J.JOB_GROUP"
            )
         );
         var5.setString(1, var3.getName());
         var5.setString(2, var3.getGroup());
         var6 = var5.executeQuery();
         if (!var6.next()) {
            if (this.logger.isDebugEnabled()) {
               this.logger.debug("No job for trigger '" + var3 + "'.");
            }

            return null;
         }

         JobDetailImpl var7 = new JobDetailImpl();
         var7.setName(var6.getString(1));
         var7.setGroup(var6.getString(2));
         var7.setDurability(this.getBoolean(var6, 3));
         if (var4) {
            var7.setJobClass(var2.loadClass(var6.getString(4), Job.class));
         }

         var7.setRequestsRecovery(this.getBoolean(var6, 5));
         var8 = var7;
      } finally {
         closeResultSet(var6);
         closeStatement(var5);
      }

      return var8;
   }

   @Override
   public List<OperableTrigger> selectTriggersForJob(Connection var1, JobKey var2) {
      LinkedList var3 = new LinkedList();
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      try {
         var4 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var4.setString(1, var2.getName());
         var4.setString(2, var2.getGroup());
         var5 = var4.executeQuery();

         while (var5.next()) {
            OperableTrigger var6 = this.selectTrigger(var1, TriggerKey.triggerKey(var5.getString("TRIGGER_NAME"), var5.getString("TRIGGER_GROUP")));
            if (var6 != null) {
               var3.add(var6);
            }
         }
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var3;
   }

   @Override
   public List<OperableTrigger> selectTriggersForCalendar(Connection var1, String var2) {
      LinkedList var3 = new LinkedList();
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      try {
         var4 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var4.setString(1, var2);
         var5 = var4.executeQuery();

         while (var5.next()) {
            var3.add(this.selectTrigger(var1, TriggerKey.triggerKey(var5.getString("TRIGGER_NAME"), var5.getString("TRIGGER_GROUP"))));
         }
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var3;
   }

   @Override
   public OperableTrigger selectTrigger(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      try {
         OperableTrigger var5 = null;
         var3 = var1.prepareStatement(this.rtp("SELECT * FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (var4.next()) {
            String var6 = var4.getString("JOB_NAME");
            String var7 = var4.getString("JOB_GROUP");
            String var8 = var4.getString("DESCRIPTION");
            long var9 = var4.getLong("NEXT_FIRE_TIME");
            long var11 = var4.getLong("PREV_FIRE_TIME");
            String var13 = var4.getString("TRIGGER_TYPE");
            long var14 = var4.getLong("START_TIME");
            long var16 = var4.getLong("END_TIME");
            String var18 = var4.getString("CALENDAR_NAME");
            int var19 = var4.getInt("MISFIRE_INSTR");
            int var20 = var4.getInt("PRIORITY");
            Object var21 = null;
            if (this.canUseProperties()) {
               var21 = this.getMapFromProperties(var4);
            } else {
               var21 = (Map)this.getObjectFromBlob(var4, "JOB_DATA");
            }

            Date var22 = null;
            if (var9 > 0L) {
               var22 = new Date(var9);
            }

            Date var23 = null;
            if (var11 > 0L) {
               var23 = new Date(var11);
            }

            Date var24 = new Date(var14);
            Date var25 = null;
            if (var16 > 0L) {
               var25 = new Date(var16);
            }

            if (var13.equals("BLOB")) {
               var4.close();
               var4 = null;
               var3.close();
               var3 = null;
               var3 = var1.prepareStatement(this.rtp("SELECT * FROM {0}BLOB_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
               var3.setString(1, var2.getName());
               var3.setString(2, var2.getGroup());
               var4 = var3.executeQuery();
               if (var4.next()) {
                  var5 = (OperableTrigger)this.getObjectFromBlob(var4, "BLOB_DATA");
               }
            } else {
               TriggerPersistenceDelegate var26 = this.findTriggerPersistenceDelegate(var13);
               if (var26 == null) {
                  throw new JobPersistenceException("No TriggerPersistenceDelegate for trigger discriminator type: " + var13);
               }

               Object var27 = null;

               try {
                  var27 = var26.loadExtendedTriggerProperties(var1, var2);
               } catch (IllegalStateException var33) {
                  if (this.isTriggerStillPresent(var3)) {
                     throw var33;
                  }

                  return null;
               }

               TriggerBuilder var28 = TriggerBuilder.newTrigger()
                  .withDescription(var8)
                  .withPriority(var20)
                  .startAt(var24)
                  .endAt(var25)
                  .withIdentity(var2)
                  .modifiedByCalendar(var18)
                  .withSchedule(((TriggerPersistenceDelegate.TriggerPropertyBundle)var27).getScheduleBuilder())
                  .forJob(JobKey.jobKey(var6, var7));
               if (null != var21) {
                  var28.usingJobData(new JobDataMap((Map<?, ?>)var21));
               }

               var5 = (OperableTrigger)var28.build();
               var5.setMisfireInstruction(var19);
               var5.setNextFireTime(var22);
               var5.setPreviousFireTime(var23);
               this.setTriggerStateProperties(var5, (TriggerPersistenceDelegate.TriggerPropertyBundle)var27);
            }
         }

         return var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }
   }

   private boolean isTriggerStillPresent(PreparedStatement var1) {
      ResultSet var2 = null;

      boolean var3;
      try {
         var2 = var1.executeQuery();
         var3 = var2.next();
      } finally {
         closeResultSet(var2);
      }

      return var3;
   }

   private void setTriggerStateProperties(OperableTrigger var1, TriggerPersistenceDelegate.TriggerPropertyBundle var2) {
      if (var2.getStatePropertyNames() != null) {
         Util.setBeanProps(var1, var2.getStatePropertyNames(), var2.getStatePropertyValues());
      }
   }

   @Override
   public JobDataMap selectTriggerJobDataMap(Connection var1, String var2, String var3) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      JobDataMap var7;
      try {
         var4 = var1.prepareStatement(this.rtp("SELECT JOB_DATA FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var4.setString(1, var2);
         var4.setString(2, var3);
         var5 = var4.executeQuery();
         if (!var5.next()) {
            return new JobDataMap();
         }

         Object var6 = null;
         if (this.canUseProperties()) {
            var6 = this.getMapFromProperties(var5);
         } else {
            var6 = (Map)this.getObjectFromBlob(var5, "JOB_DATA");
         }

         var5.close();
         var4.close();
         if (null == var6) {
            return new JobDataMap();
         }

         var7 = new JobDataMap((Map<?, ?>)var6);
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var7;
   }

   @Override
   public String selectTriggerState(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      String var6;
      try {
         String var5 = null;
         var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_STATE FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (var4.next()) {
            var5 = var4.getString("TRIGGER_STATE");
         } else {
            var5 = "DELETED";
         }

         var6 = var5.intern();
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var6;
   }

   @Override
   public TriggerStatus selectTriggerStatus(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      TriggerStatus var15;
      try {
         TriggerStatus var5 = null;
         var3 = var1.prepareStatement(
            this.rtp(
               "SELECT TRIGGER_STATE, NEXT_FIRE_TIME, JOB_NAME, JOB_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"
            )
         );
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (var4.next()) {
            String var6 = var4.getString("TRIGGER_STATE");
            long var7 = var4.getLong("NEXT_FIRE_TIME");
            String var9 = var4.getString("JOB_NAME");
            String var10 = var4.getString("JOB_GROUP");
            Date var11 = null;
            if (var7 > 0L) {
               var11 = new Date(var7);
            }

            var5 = new TriggerStatus(var6, var11);
            var5.setKey(var2);
            var5.setJobKey(JobKey.jobKey(var9, var10));
         }

         var15 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var15;
   }

   @Override
   public int selectNumTriggers(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(TRIGGER_NAME)  FROM {0}TRIGGERS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         if (var3.next()) {
            var4 = var3.getInt(1);
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public List<String> selectTriggerGroups(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      LinkedList var5;
      try {
         var2 = var1.prepareStatement(this.rtp("SELECT DISTINCT(TRIGGER_GROUP) FROM {0}TRIGGERS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         LinkedList var4 = new LinkedList();

         while (var3.next()) {
            var4.add(var3.getString(1));
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public List<String> selectTriggerGroups(Connection var1, GroupMatcher<TriggerKey> var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      LinkedList var6;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT DISTINCT(TRIGGER_GROUP) FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ?"));
         var3.setString(1, this.toSqlLikeClause(var2));
         var4 = var3.executeQuery();
         LinkedList var5 = new LinkedList();

         while (var4.next()) {
            var5.add(var4.getString(1));
         }

         var6 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var6;
   }

   @Override
   public Set<TriggerKey> selectTriggersInGroup(Connection var1, GroupMatcher<TriggerKey> var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      HashSet var6;
      try {
         if (this.isMatcherEquals(var2)) {
            var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP = ?"));
            var3.setString(1, this.toSqlEqualsClause(var2));
         } else {
            var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ?"));
            var3.setString(1, this.toSqlLikeClause(var2));
         }

         var4 = var3.executeQuery();
         HashSet var5 = new HashSet();

         while (var4.next()) {
            var5.add(TriggerKey.triggerKey(var4.getString(1), var4.getString(2)));
         }

         var6 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var6;
   }

   @Override
   public int insertPausedTriggerGroup(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("INSERT INTO {0}PAUSED_TRIGGER_GRPS (SCHED_NAME, TRIGGER_GROUP) VALUES({1}, ?)"));
         var3.setString(1, var2);
         int var4 = var3.executeUpdate();
         var5 = var4;
      } finally {
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int deletePausedTriggerGroup(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ?"));
         var3.setString(1, var2);
         int var4 = var3.executeUpdate();
         var5 = var4;
      } finally {
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int deletePausedTriggerGroup(Connection var1, GroupMatcher<TriggerKey> var2) {
      PreparedStatement var3 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP LIKE ?"));
         var3.setString(1, this.toSqlLikeClause(var2));
         int var4 = var3.executeUpdate();
         var5 = var4;
      } finally {
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int deleteAllPausedTriggerGroups(Connection var1) {
      PreparedStatement var2 = null;

      int var4;
      try {
         var2 = var1.prepareStatement(this.rtp("DELETE FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1}"));
         int var3 = var2.executeUpdate();
         var4 = var3;
      } finally {
         closeStatement(var2);
      }

      return var4;
   }

   @Override
   public boolean isTriggerGroupPaused(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT TRIGGER_GROUP FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         var5 = var4.next();
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public boolean isExistingTriggerGroup(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT COUNT(TRIGGER_NAME)  FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         if (var4.next()) {
            return var4.getInt(1) > 0;
         }

         var5 = false;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int insertCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(this.rtp("INSERT INTO {0}CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR)  VALUES({1}, ?, ?)"));
         var5.setString(1, var2);
         this.setBytes(var5, 2, var4);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public int updateCalendar(Connection var1, String var2, Calendar var3) {
      ByteArrayOutputStream var4 = this.serializeObject(var3);
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(this.rtp("UPDATE {0}CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         this.setBytes(var5, 1, var4);
         var5.setString(2, var2);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public boolean calendarExists(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT CALENDAR_NAME FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         if (!var4.next()) {
            return false;
         }

         var5 = true;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public Calendar selectCalendar(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      Calendar var7;
      try {
         String var5 = this.rtp("SELECT * FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?");
         var3 = var1.prepareStatement(var5);
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         Calendar var6 = null;
         if (var4.next()) {
            var6 = (Calendar)this.getObjectFromBlob(var4, "CALENDAR");
         }

         if (null == var6) {
            this.logger.warn("Couldn't find calendar with name '" + var2 + "'.");
         }

         var7 = var6;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var7;
   }

   @Override
   public boolean calendarIsReferenced(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      boolean var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT CALENDAR_NAME FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();
         if (!var4.next()) {
            return false;
         }

         var5 = true;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int deleteCalendar(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}CALENDARS WHERE SCHED_NAME = {1} AND CALENDAR_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public int selectNumCalendars(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(CALENDAR_NAME)  FROM {0}CALENDARS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         if (var3.next()) {
            var4 = var3.getInt(1);
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public List<String> selectCalendars(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      LinkedList var5;
      try {
         var2 = var1.prepareStatement(this.rtp("SELECT CALENDAR_NAME FROM {0}CALENDARS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();
         LinkedList var4 = new LinkedList();

         while (var3.next()) {
            var4.add(var3.getString(1));
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   /** @deprecated */
   @Override
   public long selectNextFireTime(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      long var4;
      try {
         var2 = var1.prepareStatement(
            this.rtp("SELECT MIN(NEXT_FIRE_TIME) AS ALIAS_NXT_FR_TM FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME >= 0")
         );
         var2.setString(1, "WAITING");
         var3 = var2.executeQuery();
         if (!var3.next()) {
            return 0L;
         }

         var4 = var3.getLong("ALIAS_NXT_FR_TM");
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var4;
   }

   public TriggerKey selectTriggerForFireTime(Connection var1, long var2) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      TriggerKey var6;
      try {
         var4 = var1.prepareStatement(
            this.rtp("SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME = ?")
         );
         var4.setString(1, "WAITING");
         var4.setBigDecimal(2, new BigDecimal(String.valueOf(var2)));
         var5 = var4.executeQuery();
         if (!var5.next()) {
            return null;
         }

         var6 = new TriggerKey(var5.getString("TRIGGER_NAME"), var5.getString("TRIGGER_GROUP"));
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var6;
   }

   /** @deprecated */
   @Override
   public List<TriggerKey> selectTriggerToAcquire(Connection var1, long var2, long var4) {
      return this.selectTriggerToAcquire(var1, var2, var4, 1);
   }

   @Override
   public List<TriggerKey> selectTriggerToAcquire(Connection var1, long var2, long var4, int var6) {
      PreparedStatement var7 = null;
      ResultSet var8 = null;
      LinkedList var9 = new LinkedList();

      LinkedList var10;
      try {
         var7 = var1.prepareStatement(
            this.rtp(
               "SELECT TRIGGER_NAME, TRIGGER_GROUP, NEXT_FIRE_TIME, PRIORITY FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME <= ? AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC"
            )
         );
         if (var6 < 1) {
            var6 = 1;
         }

         var7.setMaxRows(var6);
         var7.setFetchSize(var6);
         var7.setString(1, "WAITING");
         var7.setBigDecimal(2, new BigDecimal(String.valueOf(var2)));
         var7.setBigDecimal(3, new BigDecimal(String.valueOf(var4)));
         var8 = var7.executeQuery();

         while (var8.next() && var9.size() <= var6) {
            var9.add(TriggerKey.triggerKey(var8.getString("TRIGGER_NAME"), var8.getString("TRIGGER_GROUP")));
         }

         var10 = var9;
      } finally {
         closeResultSet(var8);
         closeStatement(var7);
      }

      return var10;
   }

   @Override
   public int insertFiredTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "INSERT INTO {0}FIRED_TRIGGERS (SCHED_NAME, ENTRY_ID, TRIGGER_NAME, TRIGGER_GROUP, INSTANCE_NAME, FIRED_TIME, SCHED_TIME, STATE, JOB_NAME, JOB_GROUP, IS_NONCONCURRENT, REQUESTS_RECOVERY, PRIORITY) VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
         );
         var5.setString(1, var2.getFireInstanceId());
         var5.setString(2, var2.getKey().getName());
         var5.setString(3, var2.getKey().getGroup());
         var5.setString(4, this.instanceId);
         var5.setBigDecimal(5, new BigDecimal(String.valueOf(System.currentTimeMillis())));
         var5.setBigDecimal(6, new BigDecimal(String.valueOf(var2.getNextFireTime().getTime())));
         var5.setString(7, var3);
         if (var4 != null) {
            var5.setString(8, var2.getJobKey().getName());
            var5.setString(9, var2.getJobKey().getGroup());
            this.setBoolean(var5, 10, var4.isConcurrentExectionDisallowed());
            this.setBoolean(var5, 11, var4.requestsRecovery());
         } else {
            var5.setString(8, null);
            var5.setString(9, null);
            this.setBoolean(var5, 10, false);
            this.setBoolean(var5, 11, false);
         }

         var5.setInt(12, var2.getPriority());
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public int updateFiredTrigger(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(
            this.rtp(
               "UPDATE {0}FIRED_TRIGGERS SET INSTANCE_NAME = ?, FIRED_TIME = ?, SCHED_TIME = ?, STATE = ?, JOB_NAME = ?, JOB_GROUP = ?, IS_NONCONCURRENT = ?, REQUESTS_RECOVERY = ? WHERE SCHED_NAME = {1} AND ENTRY_ID = ?"
            )
         );
         var5.setString(1, this.instanceId);
         var5.setBigDecimal(2, new BigDecimal(String.valueOf(System.currentTimeMillis())));
         var5.setBigDecimal(3, new BigDecimal(String.valueOf(var2.getNextFireTime().getTime())));
         var5.setString(4, var3);
         if (var4 != null) {
            var5.setString(5, var2.getJobKey().getName());
            var5.setString(6, var2.getJobKey().getGroup());
            this.setBoolean(var5, 7, var4.isConcurrentExectionDisallowed());
            this.setBoolean(var5, 8, var4.requestsRecovery());
         } else {
            var5.setString(5, null);
            var5.setString(6, null);
            this.setBoolean(var5, 7, false);
            this.setBoolean(var5, 8, false);
         }

         var5.setString(9, var2.getFireInstanceId());
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public List<FiredTriggerRecord> selectFiredTriggerRecords(Connection var1, String var2, String var3) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      LinkedList var11;
      try {
         LinkedList var6 = new LinkedList();
         if (var2 != null) {
            var4 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?"));
            var4.setString(1, var2);
            var4.setString(2, var3);
         } else {
            var4 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_GROUP = ?"));
            var4.setString(1, var3);
         }

         var5 = var4.executeQuery();

         while (var5.next()) {
            FiredTriggerRecord var7 = new FiredTriggerRecord();
            var7.setFireInstanceId(var5.getString("ENTRY_ID"));
            var7.setFireInstanceState(var5.getString("STATE"));
            var7.setFireTimestamp(var5.getLong("FIRED_TIME"));
            var7.setScheduleTimestamp(var5.getLong("SCHED_TIME"));
            var7.setPriority(var5.getInt("PRIORITY"));
            var7.setSchedulerInstanceId(var5.getString("INSTANCE_NAME"));
            var7.setTriggerKey(TriggerKey.triggerKey(var5.getString("TRIGGER_NAME"), var5.getString("TRIGGER_GROUP")));
            if (!var7.getFireInstanceState().equals("ACQUIRED")) {
               var7.setJobDisallowsConcurrentExecution(this.getBoolean(var5, "IS_NONCONCURRENT"));
               var7.setJobRequestsRecovery(var5.getBoolean("REQUESTS_RECOVERY"));
               var7.setJobKey(JobKey.jobKey(var5.getString("JOB_NAME"), var5.getString("JOB_GROUP")));
            }

            var6.add(var7);
         }

         var11 = var6;
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var11;
   }

   @Override
   public List<FiredTriggerRecord> selectFiredTriggerRecordsByJob(Connection var1, String var2, String var3) {
      PreparedStatement var4 = null;
      ResultSet var5 = null;

      LinkedList var11;
      try {
         LinkedList var6 = new LinkedList();
         if (var2 != null) {
            var4 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
            var4.setString(1, var2);
            var4.setString(2, var3);
         } else {
            var4 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND JOB_GROUP = ?"));
            var4.setString(1, var3);
         }

         var5 = var4.executeQuery();

         while (var5.next()) {
            FiredTriggerRecord var7 = new FiredTriggerRecord();
            var7.setFireInstanceId(var5.getString("ENTRY_ID"));
            var7.setFireInstanceState(var5.getString("STATE"));
            var7.setFireTimestamp(var5.getLong("FIRED_TIME"));
            var7.setScheduleTimestamp(var5.getLong("SCHED_TIME"));
            var7.setPriority(var5.getInt("PRIORITY"));
            var7.setSchedulerInstanceId(var5.getString("INSTANCE_NAME"));
            var7.setTriggerKey(TriggerKey.triggerKey(var5.getString("TRIGGER_NAME"), var5.getString("TRIGGER_GROUP")));
            if (!var7.getFireInstanceState().equals("ACQUIRED")) {
               var7.setJobDisallowsConcurrentExecution(this.getBoolean(var5, "IS_NONCONCURRENT"));
               var7.setJobRequestsRecovery(var5.getBoolean("REQUESTS_RECOVERY"));
               var7.setJobKey(JobKey.jobKey(var5.getString("JOB_NAME"), var5.getString("JOB_GROUP")));
            }

            var6.add(var7);
         }

         var11 = var6;
      } finally {
         closeResultSet(var5);
         closeStatement(var4);
      }

      return var11;
   }

   @Override
   public List<FiredTriggerRecord> selectInstancesFiredTriggerRecords(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      LinkedList var10;
      try {
         LinkedList var5 = new LinkedList();
         var3 = var1.prepareStatement(this.rtp("SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeQuery();

         while (var4.next()) {
            FiredTriggerRecord var6 = new FiredTriggerRecord();
            var6.setFireInstanceId(var4.getString("ENTRY_ID"));
            var6.setFireInstanceState(var4.getString("STATE"));
            var6.setFireTimestamp(var4.getLong("FIRED_TIME"));
            var6.setScheduleTimestamp(var4.getLong("SCHED_TIME"));
            var6.setSchedulerInstanceId(var4.getString("INSTANCE_NAME"));
            var6.setTriggerKey(TriggerKey.triggerKey(var4.getString("TRIGGER_NAME"), var4.getString("TRIGGER_GROUP")));
            if (!var6.getFireInstanceState().equals("ACQUIRED")) {
               var6.setJobDisallowsConcurrentExecution(this.getBoolean(var4, "IS_NONCONCURRENT"));
               var6.setJobRequestsRecovery(var4.getBoolean("REQUESTS_RECOVERY"));
               var6.setJobKey(JobKey.jobKey(var4.getString("JOB_NAME"), var4.getString("JOB_GROUP")));
            }

            var6.setPriority(var4.getInt("PRIORITY"));
            var5.add(var6);
         }

         var10 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var10;
   }

   @Override
   public Set<String> selectFiredTriggerInstanceNames(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      HashSet var5;
      try {
         HashSet var4 = new HashSet();
         var2 = var1.prepareStatement(this.rtp("SELECT DISTINCT INSTANCE_NAME FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();

         while (var3.next()) {
            var4.add(var3.getString("INSTANCE_NAME"));
         }

         var5 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var5;
   }

   @Override
   public int deleteFiredTrigger(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND ENTRY_ID = ?"));
         var3.setString(1, var2);
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public int selectJobExecutionCount(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT COUNT(TRIGGER_NAME) FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         var5 = var4.next() ? var4.getInt(1) : 0;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var5;
   }

   @Override
   public int insertSchedulerState(Connection var1, String var2, long var3, long var5) {
      PreparedStatement var7 = null;

      int var8;
      try {
         var7 = var1.prepareStatement(
            this.rtp("INSERT INTO {0}SCHEDULER_STATE (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) VALUES({1}, ?, ?, ?)")
         );
         var7.setString(1, var2);
         var7.setLong(2, var3);
         var7.setLong(3, var5);
         var8 = var7.executeUpdate();
      } finally {
         closeStatement(var7);
      }

      return var8;
   }

   @Override
   public int deleteSchedulerState(Connection var1, String var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(this.rtp("DELETE FROM {0}SCHEDULER_STATE WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ?"));
         var3.setString(1, var2);
         var4 = var3.executeUpdate();
      } finally {
         closeStatement(var3);
      }

      return var4;
   }

   @Override
   public int updateSchedulerState(Connection var1, String var2, long var3) {
      PreparedStatement var5 = null;

      int var6;
      try {
         var5 = var1.prepareStatement(this.rtp("UPDATE {0}SCHEDULER_STATE SET LAST_CHECKIN_TIME = ? WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ?"));
         var5.setLong(1, var3);
         var5.setString(2, var2);
         var6 = var5.executeUpdate();
      } finally {
         closeStatement(var5);
      }

      return var6;
   }

   @Override
   public List<SchedulerStateRecord> selectSchedulerStateRecords(Connection var1, String var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      LinkedList var10;
      try {
         LinkedList var5 = new LinkedList();
         if (var2 != null) {
            var3 = var1.prepareStatement(this.rtp("SELECT * FROM {0}SCHEDULER_STATE WHERE SCHED_NAME = {1} AND INSTANCE_NAME = ?"));
            var3.setString(1, var2);
         } else {
            var3 = var1.prepareStatement(this.rtp("SELECT * FROM {0}SCHEDULER_STATE WHERE SCHED_NAME = {1}"));
         }

         var4 = var3.executeQuery();

         while (var4.next()) {
            SchedulerStateRecord var6 = new SchedulerStateRecord();
            var6.setSchedulerInstanceId(var4.getString("INSTANCE_NAME"));
            var6.setCheckinTimestamp(var4.getLong("LAST_CHECKIN_TIME"));
            var6.setCheckinInterval(var4.getLong("CHECKIN_INTERVAL"));
            var5.add(var6);
         }

         var10 = var5;
      } finally {
         closeResultSet(var4);
         closeStatement(var3);
      }

      return var10;
   }

   protected final String rtp(String var1) {
      return Util.rtp(var1, this.tablePrefix, this.getSchedulerNameLiteral());
   }

   protected String getSchedulerNameLiteral() {
      if (this.schedNameLiteral == null) {
         this.schedNameLiteral = "'" + this.schedName + "'";
      }

      return this.schedNameLiteral;
   }

   protected ByteArrayOutputStream serializeObject(Object var1) {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      if (null != var1) {
         ObjectOutputStream var3 = new ObjectOutputStream(var2);
         var3.writeObject(var1);
         var3.flush();
      }

      return var2;
   }

   protected ByteArrayOutputStream serializeJobData(JobDataMap var1) {
      if (this.canUseProperties()) {
         return this.serializeProperties(var1);
      } else {
         try {
            return this.serializeObject(var1);
         } catch (NotSerializableException var3) {
            throw new NotSerializableException(
               "Unable to serialize JobDataMap for insertion into database because the value of property '"
                  + this.getKeyOfNonSerializableValue(var1)
                  + "' is not serializable: "
                  + var3.getMessage()
            );
         }
      }
   }

   protected Object getKeyOfNonSerializableValue(Map<?, ?> var1) {
      for (Entry var3 : var1.entrySet()) {
         ByteArrayOutputStream var4 = null;

         Object var6;
         try {
            var4 = this.serializeObject(var3.getValue());
            continue;
         } catch (IOException var16) {
            var6 = var3.getKey();
         } finally {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (IOException var15) {
               }
            }
         }

         return var6;
      }

      return null;
   }

   private ByteArrayOutputStream serializeProperties(JobDataMap var1) {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      if (null != var1) {
         Properties var3 = this.convertToProperty(var1.getWrappedMap());
         var3.store(var2, "");
      }

      return var2;
   }

   protected Map<?, ?> convertFromProperty(Properties var1) {
      return new HashMap<>(var1);
   }

   protected Properties convertToProperty(Map<?, ?> var1) {
      Properties var2 = new Properties();

      for (Entry var4 : var1.entrySet()) {
         Object var5 = var4.getKey();
         Object var6 = var4.getValue() == null ? "" : var4.getValue();
         if (!(var5 instanceof String)) {
            throw new IOException("JobDataMap keys/values must be Strings when the 'useProperties' property is set.  offending Key: " + var5);
         }

         if (!(var6 instanceof String)) {
            throw new IOException("JobDataMap values must be Strings when the 'useProperties' property is set.  Key of offending value: " + var5);
         }

         var2.put(var5, var6);
      }

      return var2;
   }

   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      Blob var4 = var1.getBlob(var2);
      if (var4 != null && var4.length() != 0L) {
         InputStream var5 = var4.getBinaryStream();
         if (null != var5 && (!(var5 instanceof ByteArrayInputStream) || ((ByteArrayInputStream)var5).available() != 0)) {
            ObjectInputStream var6 = new ObjectInputStream(var5);

            try {
               var3 = var6.readObject();
            } finally {
               var6.close();
            }
         }
      }

      return var3;
   }

   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         Blob var3 = var1.getBlob(var2);
         return var3 != null ? var3.getBinaryStream() : null;
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }

   @Override
   public Set<String> selectPausedTriggerGroups(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;
      HashSet var4 = new HashSet();

      HashSet var9;
      try {
         var2 = var1.prepareStatement(this.rtp("SELECT TRIGGER_GROUP FROM {0}PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = {1}"));
         var3 = var2.executeQuery();

         while (var3.next()) {
            String var5 = var3.getString("TRIGGER_GROUP");
            var4.add(var5);
         }

         var9 = var4;
      } finally {
         closeResultSet(var3);
         closeStatement(var2);
      }

      return var9;
   }

   protected static void closeResultSet(ResultSet var0) {
      if (null != var0) {
         try {
            var0.close();
         } catch (SQLException var2) {
         }
      }
   }

   protected static void closeStatement(Statement var0) {
      if (null != var0) {
         try {
            var0.close();
         } catch (SQLException var2) {
         }
      }
   }

   protected void setBoolean(PreparedStatement var1, int var2, boolean var3) {
      var1.setBoolean(var2, var3);
   }

   protected boolean getBoolean(ResultSet var1, String var2) {
      return var1.getBoolean(var2);
   }

   protected boolean getBoolean(ResultSet var1, int var2) {
      return var1.getBoolean(var2);
   }

   protected void setBytes(PreparedStatement var1, int var2, ByteArrayOutputStream var3) {
      var1.setBytes(var2, var3 == null ? new byte[0] : var3.toByteArray());
   }
}
