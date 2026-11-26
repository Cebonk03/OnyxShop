package org.quartz.impl.jdbcjobstore;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.ResultSet;

public class WebLogicDelegate extends StdJDBCDelegate {
   public WebLogicDelegate() {
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      Blob var4 = var1.getBlob(var2);
      InputStream var5 = null;

      try {
         if (null != var4 && var4.length() > 0L) {
            var5 = var4.getBinaryStream();
         }
      } catch (Exception var11) {
      }

      if (null != var5) {
         ObjectInputStream var6 = new ObjectInputStream(var5);

         try {
            var3 = var6.readObject();
         } finally {
            var6.close();
         }
      }

      return var3;
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         Blob var3 = var1.getBlob(var2);
         InputStream var4 = null;

         try {
            if (null != var3 && var3.length() > 0L) {
               var4 = var3.getBinaryStream();
            }
         } catch (Exception var6) {
         }

         return var4;
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }
}
