package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;

public class DB2v7Delegate extends StdJDBCDelegate {
   public DB2v7Delegate() {
   }

   @Override
   protected void setBytes(PreparedStatement var1, int var2, ByteArrayOutputStream var3) {
      var1.setObject(var2, var3 == null ? null : var3.toByteArray(), 2004);
   }

   @Override
   protected void setBoolean(PreparedStatement var1, int var2, boolean var3) {
      var1.setString(var2, var3 ? "1" : "0");
   }
}
