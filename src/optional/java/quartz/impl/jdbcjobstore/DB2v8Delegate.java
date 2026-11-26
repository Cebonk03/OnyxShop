package org.quartz.impl.jdbcjobstore;

import java.sql.PreparedStatement;

public class DB2v8Delegate extends StdJDBCDelegate {
   public DB2v8Delegate() {
   }

   @Override
   protected void setBoolean(PreparedStatement var1, int var2, boolean var3) {
      var1.setInt(var2, var3 ? 1 : 0);
   }
}
