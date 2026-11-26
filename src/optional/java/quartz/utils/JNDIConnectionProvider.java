package org.quartz.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNDIConnectionProvider implements ConnectionProvider {
   private String url;
   private Properties props;
   private Object datasource;
   private boolean alwaysLookup = false;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public JNDIConnectionProvider(String var1, boolean var2) {
      this.url = var1;
      this.alwaysLookup = var2;
      this.init();
   }

   public JNDIConnectionProvider(String var1, Properties var2, boolean var3) {
      this.url = var1;
      this.props = var2;
      this.alwaysLookup = var3;
      this.init();
   }

   protected Logger getLog() {
      return this.log;
   }

   private void init() {
      if (!this.isAlwaysLookup()) {
         InitialContext var1 = null;

         try {
            var1 = this.props != null ? new InitialContext(this.props) : new InitialContext();
            this.datasource = (DataSource)var1.lookup(this.url);
         } catch (Exception var11) {
            this.getLog().error("Error looking up datasource: " + var11.getMessage(), var11);
         } finally {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Exception var10) {
               }
            }
         }
      }
   }

   @Override
   public Connection getConnection() throws SQLException {
      InitialContext var1 = null;

      Connection var3;
      try {
         Object var2 = this.datasource;
         if (var2 == null || this.isAlwaysLookup()) {
            var1 = this.props != null ? new InitialContext(this.props) : new InitialContext();
            var2 = var1.lookup(this.url);
            if (!this.isAlwaysLookup()) {
               this.datasource = var2;
            }
         }

         if (var2 == null) {
            throw new SQLException("There is no object at the JNDI URL '" + this.url + "'");
         }

         if (var2 instanceof XADataSource) {
            return ((XADataSource)var2).getXAConnection().getConnection();
         }

         if (!(var2 instanceof DataSource)) {
            throw new SQLException("Object at JNDI URL '" + this.url + "' is not a DataSource.");
         }

         var3 = ((DataSource)var2).getConnection();
      } catch (Exception var13) {
         this.datasource = null;
         throw new SQLException("Could not retrieve datasource via JNDI url '" + this.url + "' " + var13.getClass().getName() + ": " + var13.getMessage());
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Exception var12) {
            }
         }
      }

      return var3;
   }

   public boolean isAlwaysLookup() {
      return this.alwaysLookup;
   }

   public void setAlwaysLookup(boolean var1) {
      this.alwaysLookup = var1;
   }

   @Override
   public void shutdown() {
   }

   @Override
   public void initialize() {
   }
}
