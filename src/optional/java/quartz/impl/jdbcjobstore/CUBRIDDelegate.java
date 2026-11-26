package org.quartz.impl.jdbcjobstore;

import com.mchange.v2.c3p0.C3P0ProxyConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CUBRIDDelegate extends StdJDBCDelegate {
   public CUBRIDDelegate() {
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Object var3 = null;
      Blob var5 = var1.getBlob(var2);
      byte[] var6 = var5.getBytes(1L, (int)var5.length());
      if (var6 != null && var6.length != 0) {
         ByteArrayInputStream var4 = new ByteArrayInputStream(var6);
         ObjectInputStream var7 = new ObjectInputStream(var4);

         try {
            var3 = var7.readObject();
         } finally {
            var7.close();
         }
      }

      return var3;
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         Blob var4 = var1.getBlob(var2);
         byte[] var5 = var4.getBytes(1L, (int)var4.length());
         return var5 != null && var5.length != 0 ? new ByteArrayInputStream(var5) : null;
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }

   @Override
   protected void setBytes(PreparedStatement var1, int var2, ByteArrayOutputStream var3) {
      byte[] var4;
      if (var3 == null) {
         var4 = new byte[0];
      } else {
         var4 = var3.toByteArray();
      }

      Connection var5 = var1.getConnection();
      if (var5 instanceof C3P0ProxyConnection) {
         try {
            C3P0ProxyConnection var6 = (C3P0ProxyConnection)var5;
            Method var7 = Connection.class.getMethod("createBlob");
            Object[] var8 = new Object[0];
            Blob var9 = (Blob)var6.rawConnectionOperation(var7, C3P0ProxyConnection.RAW_CONNECTION, var8);
            var9.setBytes(1L, var4);
            var1.setBlob(var2, var9);
         } catch (Exception var10) {
            var10.printStackTrace();
         }
      } else {
         Blob var11 = var1.getConnection().createBlob();
         var11.setBytes(1L, var4);
         var1.setBlob(var2, var11);
      }
   }
}
