package org.quartz.utils.weblogic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.quartz.utils.ConnectionProvider;
import weblogic.jdbc.jts.Driver;

public class WeblogicConnectionProvider implements ConnectionProvider {
   private String poolName;
   private Driver driver;

   public WeblogicConnectionProvider(String var1) {
      this.poolName = var1;
   }

   @Override
   public Connection getConnection() {
      return this.driver.connect("jdbc:weblogic:jts:" + this.poolName, (Properties)null);
   }

   @Override
   public void initialize() {
      try {
         this.driver = Driver.class.newInstance();
      } catch (Exception var2) {
         throw new SQLException(
            "Could not get weblogic pool connection with name '" + this.poolName + "': " + var2.getClass().getName() + ": " + var2.getMessage()
         );
      }
   }

   @Override
   public void shutdown() {
   }
}
