package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SybaseDelegate extends StdJDBCDelegate {
   public SybaseDelegate() {
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      InputStream var3 = var1.getBinaryStream(var2);
      if (var3 != null && var3.available() != 0) {
         Object var4 = null;
         ObjectInputStream var5 = new ObjectInputStream(var3);

         try {
            var4 = var5.readObject();
         } finally {
            var5.close();
         }

         return var4;
      } else {
         return null;
      }
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      return this.canUseProperties() ? var1.getBinaryStream(var2) : this.getObjectFromBlob(var1, var2);
   }

   @Override
   protected void setBytes(PreparedStatement var1, int var2, ByteArrayOutputStream var3) {
      var1.setBytes(var2, var3 == null ? null : var3.toByteArray());
   }
}
