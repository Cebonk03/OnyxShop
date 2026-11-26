package org.quartz.impl.jdbcjobstore;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

public abstract class SimplePropertiesTriggerPersistenceDelegateSupport implements TriggerPersistenceDelegate, StdJDBCConstants {
   protected static final String TABLE_SIMPLE_PROPERTIES_TRIGGERS = "SIMPROP_TRIGGERS";
   protected static final String COL_STR_PROP_1 = "STR_PROP_1";
   protected static final String COL_STR_PROP_2 = "STR_PROP_2";
   protected static final String COL_STR_PROP_3 = "STR_PROP_3";
   protected static final String COL_INT_PROP_1 = "INT_PROP_1";
   protected static final String COL_INT_PROP_2 = "INT_PROP_2";
   protected static final String COL_LONG_PROP_1 = "LONG_PROP_1";
   protected static final String COL_LONG_PROP_2 = "LONG_PROP_2";
   protected static final String COL_DEC_PROP_1 = "DEC_PROP_1";
   protected static final String COL_DEC_PROP_2 = "DEC_PROP_2";
   protected static final String COL_BOOL_PROP_1 = "BOOL_PROP_1";
   protected static final String COL_BOOL_PROP_2 = "BOOL_PROP_2";
   protected static final String SELECT_SIMPLE_PROPS_TRIGGER = "SELECT * FROM {0}SIMPROP_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   protected static final String DELETE_SIMPLE_PROPS_TRIGGER = "DELETE FROM {0}SIMPROP_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   protected static final String INSERT_SIMPLE_PROPS_TRIGGER = "INSERT INTO {0}SIMPROP_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, STR_PROP_1, STR_PROP_2, STR_PROP_3, INT_PROP_1, INT_PROP_2, LONG_PROP_1, LONG_PROP_2, DEC_PROP_1, DEC_PROP_2, BOOL_PROP_1, BOOL_PROP_2)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
   protected static final String UPDATE_SIMPLE_PROPS_TRIGGER = "UPDATE {0}SIMPROP_TRIGGERS SET STR_PROP_1 = ?, STR_PROP_2 = ?, STR_PROP_3 = ?, INT_PROP_1 = ?, INT_PROP_2 = ?, LONG_PROP_1 = ?, LONG_PROP_2 = ?, DEC_PROP_1 = ?, DEC_PROP_2 = ?, BOOL_PROP_1 = ?, BOOL_PROP_2 = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?";
   protected String tablePrefix;
   protected String schedNameLiteral;

   public SimplePropertiesTriggerPersistenceDelegateSupport() {
   }

   @Override
   public void initialize(String var1, String var2) {
      this.tablePrefix = var1;
      this.schedNameLiteral = "'" + var2 + "'";
   }

   protected abstract SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger var1);

   protected abstract TriggerPersistenceDelegate.TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties var1);

   @Override
   public int deleteExtendedTriggerProperties(Connection var1, TriggerKey var2) throws SQLException {
      PreparedStatement var3 = null;

      int var4;
      try {
         var3 = var1.prepareStatement(
            Util.rtp(
               "DELETE FROM {0}SIMPROP_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral
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
   public int insertExtendedTriggerProperties(Connection var1, OperableTrigger var2, String var3, JobDetail var4) throws SQLException {
      SimplePropertiesTriggerProperties var5 = this.getTriggerProperties(var2);
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "INSERT INTO {0}SIMPROP_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, STR_PROP_1, STR_PROP_2, STR_PROP_3, INT_PROP_1, INT_PROP_2, LONG_PROP_1, LONG_PROP_2, DEC_PROP_1, DEC_PROP_2, BOOL_PROP_1, BOOL_PROP_2)  VALUES({1}, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setString(1, var2.getKey().getName());
         var6.setString(2, var2.getKey().getGroup());
         var6.setString(3, var5.getString1());
         var6.setString(4, var5.getString2());
         var6.setString(5, var5.getString3());
         var6.setInt(6, var5.getInt1());
         var6.setInt(7, var5.getInt2());
         var6.setLong(8, var5.getLong1());
         var6.setLong(9, var5.getLong2());
         var6.setBigDecimal(10, var5.getDecimal1());
         var6.setBigDecimal(11, var5.getDecimal2());
         var6.setBoolean(12, var5.isBoolean1());
         var6.setBoolean(13, var5.isBoolean2());
         var7 = var6.executeUpdate();
      } finally {
         Util.closeStatement(var6);
      }

      return var7;
   }

   @Override
   public TriggerPersistenceDelegate.TriggerPropertyBundle loadExtendedTriggerProperties(Connection var1, TriggerKey var2) throws SQLException {
      PreparedStatement var3 = null;
      ResultSet var4 = null;

      TriggerPersistenceDelegate.TriggerPropertyBundle var6;
      try {
         var3 = var1.prepareStatement(
            Util.rtp(
               "SELECT * FROM {0}SIMPROP_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?", this.tablePrefix, this.schedNameLiteral
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

         SimplePropertiesTriggerProperties var5 = new SimplePropertiesTriggerProperties();
         var5.setString1(var4.getString("STR_PROP_1"));
         var5.setString2(var4.getString("STR_PROP_2"));
         var5.setString3(var4.getString("STR_PROP_3"));
         var5.setInt1(var4.getInt("INT_PROP_1"));
         var5.setInt2(var4.getInt("INT_PROP_2"));
         var5.setLong1(var4.getInt("LONG_PROP_1"));
         var5.setLong2(var4.getInt("LONG_PROP_2"));
         var5.setDecimal1(var4.getBigDecimal("DEC_PROP_1"));
         var5.setDecimal2(var4.getBigDecimal("DEC_PROP_2"));
         var5.setBoolean1(var4.getBoolean("BOOL_PROP_1"));
         var5.setBoolean2(var4.getBoolean("BOOL_PROP_2"));
         var6 = this.getTriggerPropertyBundle(var5);
      } finally {
         Util.closeResultSet(var4);
         Util.closeStatement(var3);
      }

      return var6;
   }

   @Override
   public int updateExtendedTriggerProperties(Connection var1, OperableTrigger var2, String var3, JobDetail var4) throws SQLException {
      SimplePropertiesTriggerProperties var5 = this.getTriggerProperties(var2);
      PreparedStatement var6 = null;

      int var7;
      try {
         var6 = var1.prepareStatement(
            Util.rtp(
               "UPDATE {0}SIMPROP_TRIGGERS SET STR_PROP_1 = ?, STR_PROP_2 = ?, STR_PROP_3 = ?, INT_PROP_1 = ?, INT_PROP_2 = ?, LONG_PROP_1 = ?, LONG_PROP_2 = ?, DEC_PROP_1 = ?, DEC_PROP_2 = ?, BOOL_PROP_1 = ?, BOOL_PROP_2 = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?",
               this.tablePrefix,
               this.schedNameLiteral
            )
         );
         var6.setString(1, var5.getString1());
         var6.setString(2, var5.getString2());
         var6.setString(3, var5.getString3());
         var6.setInt(4, var5.getInt1());
         var6.setInt(5, var5.getInt2());
         var6.setLong(6, var5.getLong1());
         var6.setLong(7, var5.getLong2());
         var6.setBigDecimal(8, var5.getDecimal1());
         var6.setBigDecimal(9, var5.getDecimal2());
         var6.setBoolean(10, var5.isBoolean1());
         var6.setBoolean(11, var5.isBoolean2());
         var6.setString(12, var2.getKey().getName());
         var6.setString(13, var2.getKey().getGroup());
         var7 = var6.executeUpdate();
      } finally {
         Util.closeStatement(var6);
      }

      return var7;
   }
}
