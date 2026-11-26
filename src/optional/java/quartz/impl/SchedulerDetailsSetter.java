package org.quartz.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SchedulerDetailsSetter {
   private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerDetailsSetter.class);

   private SchedulerDetailsSetter() {
   }

   static void setDetails(Object var0, String var1, String var2) {
      set(var0, "setInstanceName", var1);
      set(var0, "setInstanceId", var2);
   }

   private static void set(Object var0, String var1, String var2) {
      Method var3;
      try {
         var3 = var0.getClass().getMethod(var1, String.class);
      } catch (SecurityException var7) {
         LOGGER.error("A SecurityException occured: " + var7.getMessage(), var7);
         return;
      } catch (NoSuchMethodException var8) {
         LOGGER.warn(var0.getClass().getName() + " does not contain public method " + var1 + "(String)");
         return;
      }

      if (Modifier.isAbstract(var3.getModifiers())) {
         LOGGER.warn(var0.getClass().getName() + " does not implement " + var1 + "(String)");
      } else {
         try {
            var3.invoke(var0, var2);
         } catch (InvocationTargetException var5) {
            throw new SchedulerException(var5.getTargetException());
         } catch (Exception var6) {
            throw new SchedulerException(var6);
         }
      }
   }
}
