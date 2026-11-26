package org.quartz.impl.jdbcjobstore.oracle.weblogic;

import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.quartz.impl.jdbcjobstore.oracle.OracleDelegate;
import weblogic.jdbc.vendor.oracle.OracleThinBlob;

public class WebLogicOracleDelegate extends OracleDelegate {
   public WebLogicOracleDelegate() {
   }

   @Override
   protected Blob writeDataToBlob(ResultSet var1, int var2, byte[] var3) {
      Blob var4 = var1.getBlob(var2);
      if (var4 == null) {
         throw new SQLException("Driver's Blob representation is null!");
      } else if (var4 instanceof OracleThinBlob) {
         ((OracleThinBlob)var4).putBytes(1L, var3);
         return var4;
      } else if (var4.getClass().getPackage().getName().startsWith("weblogic.")) {
         try {
            Method var5 = var4.getClass().getMethod("putBytes", long.class, byte[].class);
            var5.invoke(var4, new Long(1L), var3);
         } catch (Exception var8) {
            try {
               Method var6 = var4.getClass().getMethod("setBytes", long.class, byte[].class);
               var6.invoke(var4, new Long(1L), var3);
            } catch (Exception var7) {
               throw new SQLException("Unable to find putBytes(long,byte[]) or setBytes(long,byte[]) methods on blob: " + var7);
            }
         }

         return var4;
      } else {
         return super.writeDataToBlob(var1, var2, var3);
      }
   }
}
