package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;

public class PostgreSQLDelegate extends StdJDBCDelegate {
   public PostgreSQLDelegate() {
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      byte[] var4 = var1.getBytes(var2);
      Object var5 = null;
      if (var4 != null && var4.length != 0) {
         var3 = new ByteArrayInputStream(var4);
         ObjectInputStream var6 = new ObjectInputStream((InputStream)var3);

         try {
            var5 = var6.readObject();
         } finally {
            var6.close();
         }
      }

      return var5;
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         Object var3 = null;
         byte[] var4 = var1.getBytes(var2);
         return var4 != null && var4.length != 0 ? new ByteArrayInputStream(var4) : null;
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }
}
