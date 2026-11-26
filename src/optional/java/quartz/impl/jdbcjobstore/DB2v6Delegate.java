package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.quartz.JobKey;

public class DB2v6Delegate extends StdJDBCDelegate {
   public static final String SELECT_NUM_JOBS = "SELECT COUNT(*) FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1}";
   public static final String SELECT_NUM_TRIGGERS_FOR_JOB = "SELECT COUNT(*) FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?";
   public static final String SELECT_NUM_TRIGGERS = "SELECT COUNT(*) FROM {0}TRIGGERS WHERE SCHED_NAME = {1}";
   public static final String SELECT_NUM_CALENDARS = "SELECT COUNT(*) FROM {0}CALENDARS WHERE SCHED_NAME = {1}";

   public DB2v6Delegate() {
   }

   @Override
   public int selectNumJobs(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(*) FROM {0}JOB_DETAILS WHERE SCHED_NAME = {1}"));
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
   public int selectNumTriggersForJob(Connection var1, JobKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      int var5;
      try {
         var3 = var1.prepareStatement(this.rtp("SELECT COUNT(*) FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?"));
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
   public int selectNumTriggers(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(*) FROM {0}TRIGGERS WHERE SCHED_NAME = {1}"));
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
   public int selectNumCalendars(Connection var1) {
      PreparedStatement var2 = null;
      ResultSet var3 = null;

      int var5;
      try {
         int var4 = 0;
         var2 = var1.prepareStatement(this.rtp("SELECT COUNT(*) FROM {0}CALENDARS WHERE SCHED_NAME = {1}"));
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
}
