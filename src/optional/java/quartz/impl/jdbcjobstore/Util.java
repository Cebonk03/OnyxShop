package org.quartz.impl.jdbcjobstore;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Locale;
import org.quartz.JobPersistenceException;

public final class Util {
   private Util() {
   }

   public static String rtp(String var0, String var1, String var2) {
      return MessageFormat.format(var0, var1, var2);
   }

   static String getJobNameKey(String var0, String var1) {
      return (var1 + "_$x$x$_" + var0).intern();
   }

   static String getTriggerNameKey(String var0, String var1) {
      return (var1 + "_$x$x$_" + var0).intern();
   }

   public static void closeResultSet(ResultSet var0) {
      if (null != var0) {
         try {
            var0.close();
         } catch (SQLException var2) {
         }
      }
   }

   public static void closeStatement(Statement var0) {
      if (null != var0) {
         try {
            var0.close();
         } catch (SQLException var2) {
         }
      }
   }

   public static void setBeanProps(Object var0, String[] var1, Object[] var2) {
      if (var1 != null && var1.length != 0) {
         if (var1.length != var2.length) {
            throw new IllegalArgumentException("propNames[].lenght != propValues[].length");
         } else {
            String var3 = null;

            try {
               BeanInfo var4 = Introspector.getBeanInfo(var0.getClass());
               PropertyDescriptor[] var5 = var4.getPropertyDescriptors();

               for (int var6 = 0; var6 < var1.length; var6++) {
                  var3 = var1[var6];
                  String var7 = var3.substring(0, 1).toUpperCase(Locale.US);
                  String var8 = "set" + var7 + var3.substring(1);
                  Method var9 = getSetMethod(var8, var5);
                  if (var9 == null) {
                     throw new NoSuchMethodException("No setter for property '" + var3 + "'");
                  }

                  Class[] var10 = var9.getParameterTypes();
                  if (var10.length != 1) {
                     throw new NoSuchMethodException("No 1-argument setter for property '" + var3 + "'");
                  }

                  var9.invoke(var0, var2[var6]);
               }
            } catch (Exception var11) {
               throw new JobPersistenceException("Unable to set property named: " + var3 + " of object of type: " + var0.getClass().getCanonicalName(), var11);
            }
         }
      }
   }

   private static Method getSetMethod(String var0, PropertyDescriptor[] var1) {
      for (int var2 = 0; var2 < var1.length; var2++) {
         Method var3 = var1[var2].getWriteMethod();
         if (var3 != null && var3.getName().equals(var0)) {
            return var3;
         }
      }

      return null;
   }
}
