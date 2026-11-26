package org.quartz.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import com.zaxxer.hikari.HikariDataSource;

public class HikariCpPoolingConnectionProvider implements PoolingConnectionProvider {
   public static final String POOLING_PROVIDER_NAME = "hikaricp";
   private static final String DB_DISCARD_IDLE_CONNECTIONS_SECONDS = "discardIdleConnectionsSeconds";
   private HikariDataSource datasource;

   public HikariCpPoolingConnectionProvider(String var1, String var2, String var3, String var4, int var5, String var6) {
      this.initialize(var1, var2, var3, var4, var5, var6, 0);
   }

   public HikariCpPoolingConnectionProvider(Properties var1) {
      PropertiesParser var2 = new PropertiesParser(var1);
      this.initialize(
         var2.getStringProperty("driver"),
         var2.getStringProperty("URL"),
         var2.getStringProperty("user", ""),
         var2.getStringProperty("password", ""),
         var2.getIntProperty("maxConnections", 10),
         var2.getStringProperty("validationQuery"),
         var2.getIntProperty("discardIdleConnectionsSeconds", 0)
      );
   }

   private void initialize(String var1, String var2, String var3, String var4, int var5, String var6, int var7) {
      if (var2 == null) {
         throw new SQLException("DBPool could not be created: DB URL cannot be null");
      } else if (var1 == null) {
         throw new SQLException("DBPool '" + var2 + "' could not be created: " + "DB driver class name cannot be null!");
      } else if (var5 < 0) {
         throw new SQLException("DBPool '" + var2 + "' could not be created: " + "Max connections must be greater than zero!");
      } else {
         this.datasource = new HikariDataSource();
         this.datasource.setDriverClassName(var1);
         this.datasource.setJdbcUrl(var2);
         this.datasource.setUsername(var3);
         this.datasource.setPassword(var4);
         this.datasource.setMaximumPoolSize(var5);
         this.datasource.setIdleTimeout(var7);
         if (var6 != null) {
            this.datasource.setConnectionTestQuery(var6);
         }
      }
   }

   public HikariDataSource getDataSource() {
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
