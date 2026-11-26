package org.quartz.impl.jdbcjobstore;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class SimpleTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {
   protected String tablePrefix;
   protected String schedNameLiteral;

   public SimpleTriggerPersistenceDelegate() {
   }

   @Override
   public void initialize(String var1, String var2) {
      this.tablePrefix = var1;
      this.schedNameLiteral = "'" + var2 + "'";
   }

   @Override
   public String getHandledTriggerTypeDiscriminator() {
      return "SIMPLE";
   }

   @Override
   public boolean canHandleTriggerType(OperableTrigger var1) {
      return var1 instanceof SimpleTriggerImpl && !((SimpleTriggerImpl)var1).hasAdditionalProperties();
   }

   @Override
   public int deleteExtendedTriggerProperties(Connection var1, TriggerKey var2) {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(
            Util.rtp(
               "DELETE FROM {0}SIMPLE_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral
            )
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
      SimpleTrigger var5 = (SimpleTrigger)var2;
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "INSERT INTO {0}SIMPLE_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED)  VALUES({1}, ?, ?, ?, ?, ?)",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setString(1, var2.getKey().getName());
         var6.setString(2, var2.getKey().getGroup());
         var6.setInt(3, var5.getRepeatCount());
         var6.setBigDecimal(4, new BigDecimal(String.valueOf(var5.getRepeatInterval())));
         var6.setInt(5, var5.getTimesTriggered());
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

      TriggerPersistenceDelegate.TriggerPropertyBundle var12;
      try {
         var3 = var1.prepareStatement(
            Util.rtp(
               "SELECT * FROM {0}SIMPLE_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral
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
                     "SELECT * FROM {0}SIMPLE_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?",
                     this.tablePrefix,
                     this.schedNameLiteral
                  )
            );
         }

         int var5 = var4.getInt("REPEAT_COUNT");
         long var6 = var4.getLong("REPEAT_INTERVAL");
         int var8 = var4.getInt("TIMES_TRIGGERED");
         SimpleScheduleBuilder var9 = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(var5).withIntervalInMilliseconds(var6);
         String[] var10 = new String[]{"timesTriggered"};
         Object[] var11 = new Object[]{var8};
         var12 = new TriggerPersistenceDelegate.TriggerPropertyBundle(var9, var10, var11);
      } finally {
         Util.closeResultSet(var4);
         Util.closeStatement(var3);
      }

      return var12;
   }

   @Override
   public int updateExtendedTriggerProperties(Connection var1, OperableTrigger var2, String var3, JobDetail var4) {
      SimpleTrigger var5 = (SimpleTrigger)var2;
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "UPDATE {0}SIMPLE_TRIGGERS SET REPEAT_COUNT = ?, REPEAT_INTERVAL = ?, TIMES_TRIGGERED = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setInt(1, var5.getRepeatCount());
         var6.setBigDecimal(2, new BigDecimal(String.valueOf(var5.getRepeatInterval())));
         var6.setInt(3, var5.getTimesTriggered());
         var6.setString(4, var5.getKey().getName());
         var6.setString(5, var5.getKey().getGroup());
         var7 = var6.executeUpdate();
      } finally {
         Util.closeStatement(var6);
      }

      return var7;
   }
}
