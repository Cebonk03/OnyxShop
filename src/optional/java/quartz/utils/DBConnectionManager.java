package org.quartz.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

public class DBConnectionManager {
   public static final String DB_PROPS_PREFIX = "org.quartz.db.";
   private static DBConnectionManager instance = new DBConnectionManager();
   private HashMap<String, ConnectionProvider> providers = new HashMap<>();

   private DBConnectionManager() {
   }

   public void addConnectionProvider(String var1, ConnectionProvider var2) {
      this.providers.put(var1, var2);
   }

   public Connection getConnection(String var1) {
      ConnectionProvider var2 = this.providers.get(var1);
      if (var2 == null) {
         throw new SQLException("There is no DataSource named '" + var1 + "'");
      } else {
         return var2.getConnection();
      }
   }

   public static DBConnectionManager getInstance() {
      return instance;
   }

   public void shutdown(String var1) {
      ConnectionProvider var2 = this.providers.get(var1);
      if (var2 == null) {
         throw new SQLException("There is no DataSource named '" + var1 + "'");
      } else {
         var2.shutdown();
      }
   }

   ConnectionProvider getConnectionProvider(String var1) {
      return this.providers.get(var1);
   }
}
