package org.quartz.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.quartz.SchedulerException;

public class C3p0PoolingConnectionProvider implements PoolingConnectionProvider {
   public static final String DB_MAX_CACHED_STATEMENTS_PER_CONNECTION = "maxCachedStatementsPerConnection";
   public static final String DB_IDLE_VALIDATION_SECONDS = "idleConnectionValidationSeconds";
   public static final String DB_VALIDATE_ON_CHECKOUT = "validateOnCheckout";
   private static final String DB_DISCARD_IDLE_CONNECTIONS_SECONDS = "maxIdleTime";
   public static final int DEFAULT_DB_MAX_CACHED_STATEMENTS_PER_CONNECTION = 120;
   private ComboPooledDataSource datasource;

   public C3p0PoolingConnectionProvider(String var1, String var2, String var3, String var4, int var5, String var6) {
      this.initialize(var1, var2, var3, var4, var5, 120, var6, false, 50, 0);
   }

   public C3p0PoolingConnectionProvider(Properties var1) {
      PropertiesParser var2 = new PropertiesParser(var1);
      this.initialize(
         var2.getStringProperty("driver"),
         var2.getStringProperty("URL"),
         var2.getStringProperty("user", ""),
         var2.getStringProperty("password", ""),
         var2.getIntProperty("maxConnections", 10),
         var2.getIntProperty("maxCachedStatementsPerConnection", 120),
         var2.getStringProperty("validationQuery"),
         var2.getBooleanProperty("validateOnCheckout", false),
         var2.getIntProperty("idleConnectionValidationSeconds", 50),
         var2.getIntProperty("maxIdleTime", 0)
      );
   }

   private void initialize(String var1, String var2, String var3, String var4, int var5, int var6, String var7, boolean var8, int var9, int var10) {
      if (var2 == null) {
         throw new SQLException("DBPool could not be created: DB URL cannot be null");
      } else if (var1 == null) {
         throw new SQLException("DBPool '" + var2 + "' could not be created: " + "DB driver class name cannot be null!");
      } else if (var5 < 0) {
         throw new SQLException("DBPool '" + var2 + "' could not be created: " + "Max connections must be greater than zero!");
      } else {
         this.datasource = new ComboPooledDataSource();

         try {
            this.datasource.setDriverClass(var1);
         } catch (PropertyVetoException var12) {
            throw new SchedulerException("Problem setting driver class name on datasource: " + var12.getMessage(), var12);
         }

         this.datasource.setJdbcUrl(var2);
         this.datasource.setUser(var3);
         this.datasource.setPassword(var4);
         this.datasource.setMaxPoolSize(var5);
         this.datasource.setMinPoolSize(1);
         this.datasource.setMaxIdleTime(var10);
         this.datasource.setMaxStatementsPerConnection(var6);
         if (var7 != null) {
            this.datasource.setPreferredTestQuery(var7);
            if (!var8) {
               this.datasource.setTestConnectionOnCheckin(true);
            } else {
               this.datasource.setTestConnectionOnCheckout(true);
            }

            this.datasource.setIdleConnectionTestPeriod(var9);
         }
      }
   }

   public ComboPooledDataSource getDataSource() {
      return this.datasource;
   }

   @Override
   public Connection getConnection() {
      return this.datasource.getConnection();
   }

   @Override
   public void shutdown() {
      this.datasource.close();
   }

   @Override
   public void initialize() {
   }
}
