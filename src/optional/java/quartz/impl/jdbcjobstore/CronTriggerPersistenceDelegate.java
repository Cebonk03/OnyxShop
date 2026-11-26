package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class CronTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {
   protected String tablePrefix;
   protected String schedNameLiteral;

   public CronTriggerPersistenceDelegate() {
   }

   @Override
   public void initialize(String var1, String var2) {
      this.tablePrefix = var1;
      this.schedNameLiteral = "'" + var2 + "'";
   }

   @Override
   public String getHandledTriggerTypeDiscriminator() {
      return "CRON";
   }

   @Override
   public boolean canHandleTriggerType(OperableTrigger var1) {
      return var1 instanceof CronTriggerImpl && !((CronTriggerImpl)var1).hasAdditionalProperties();
   }

   @Override
   public int deleteExtendedTriggerProperties(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(
            Util.rtp("DELETE FROM {0}CRON_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral)
         );
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeUpdate();
      } finally {
         Util.closeStatement(var3);
      }

      return var4;
   }

   @Override
   public int insertExtendedTriggerProperties(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      CronTrigger var5 = (CronTrigger)var2;
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "INSERT INTO {0}CRON_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID)  VALUES({1}, ?, ?, ?, ?)",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setString(1, var2.getKey().getName());
         var6.setString(2, var2.getKey().getGroup());
         var6.setString(3, var5.getCronExpression());
         var6.setString(4, var5.getTimeZone().getID());
         var7 = var6.executeUpdate();
      } finally {
         Util.closeStatement(var6);
      }

      return var7;
   }

   @Override
   public TriggerPersistenceDelegate.TriggerPropertyBundle loadExtendedTriggerProperties(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      TriggerPersistenceDelegate.TriggerPropertyBundle var8;
      try {
         var3 = var1.prepareStatement(
            Util.rtp(
               "SELECT * FROM {0}CRON_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral
            )
         );
         var3.setString(1, var2.getName());
         var3.setString(2, var2.getGroup());
         var4 = var3.executeQuery();
         if (!var4.next()) {
            throw new IllegalStateException(
               "No record found for selection of Trigger with key: '"
                  + var2
                  + "' and statement: "
                  + Util.rtp(
                     "SELECT * FROM {0}CRON_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?",
                     this.tablePrefix,
                     this.schedNameLiteral
                  )
            );
         }

         String var5 = var4.getString("CRON_EXPRESSION");
         String var6 = var4.getString("TIME_ZONE_ID");
         CronScheduleBuilder var7 = CronScheduleBuilder.cronSchedule(var5);
         if (var6 != null) {
            var7.inTimeZone(TimeZone.getTimeZone(var6));
         }

         var8 = new TriggerPersistenceDelegate.TriggerPropertyBundle(var7, null, null);
      } finally {
         Util.closeResultSet(var4);
         Util.closeStatement(var3);
      }

      return var8;
   }

   @Override
   public int updateExtendedTriggerProperties(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      CronTrigger var5 = (CronTrigger)var2;
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "UPDATE {0}CRON_TRIGGERS SET CRON_EXPRESSION = ?, TIME_ZONE_ID = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setString(1, var5.getCronExpression());
         var6.setString(2, var5.getTimeZone().getID());
         var6.setString(3, var2.getKey().getName());
         var6.setString(4, var2.getKey().getGroup());
         var7 = var6.executeUpdate();
      } finally {
         Util.closeStatement(var6);
      }

      return var7;
   }
}
