package org.quartz.simpl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map.Entry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

public class PropertySettingJobFactory extends SimpleJobFactory {
   private boolean warnIfNotFound = false;
   private boolean throwIfNotFound = false;

   public PropertySettingJobFactory() {
   }

   @Override
   public Job newJob(TriggerFiredBundle var1, Scheduler var2) {
      Job var3 = super.newJob(var1, var2);
      JobDataMap var4 = new JobDataMap();
      var4.putAll(var2.getContext());
      var4.putAll(var1.getJobDetail().getJobDataMap());
      var4.putAll(var1.getTrigger().getJobDataMap());
      this.setBeanProps(var3, var4);
      return var3;
   }

   protected void setBeanProps(Object var1, JobDataMap var2) {
      BeanInfo var3 = null;

      try {
         var3 = Introspector.getBeanInfo(var1.getClass());
      } catch (IntrospectionException var15) {
         this.handleError("Unable to introspect Job class.", var15);
      }

      PropertyDescriptor[] var4 = var3.getPropertyDescriptors();

      for (Entry var6 : var2.getWrappedMap().entrySet()) {
         String var7 = (String)var6.getKey();
         String var8 = var7.substring(0, 1).toUpperCase(Locale.US);
         String var9 = "set" + var8 + var7.substring(1);
         Method var10 = this.getSetMethod(var9, var4);
         Class var11 = null;
         Object var12 = null;

         try {
            if (var10 == null) {
               this.handleError("No setter on Job class " + var1.getClass().getName() + " for property '" + var7 + "'");
            } else {
               var11 = var10.getParameterTypes()[0];
               var12 = var6.getValue();
               Object var13 = null;
               if (var11.isPrimitive()) {
                  if (var12 == null) {
                     this.handleError("Cannot set primitive property '" + var7 + "' on Job class " + var1.getClass().getName() + " to null.");
                     continue;
                  }

                  if (var11.equals(int.class)) {
                     if (var12 instanceof String) {
                        var13 = Integer.valueOf((String)var12);
                     } else if (var12 instanceof Integer) {
                        var13 = var12;
                     }
                  } else if (var11.equals(long.class)) {
                     if (var12 instanceof String) {
                        var13 = Long.valueOf((String)var12);
                     } else if (var12 instanceof Long) {
                        var13 = var12;
                     }
                  } else if (var11.equals(float.class)) {
                     if (var12 instanceof String) {
                        var13 = Float.valueOf((String)var12);
                     } else if (var12 instanceof Float) {
                        var13 = var12;
                     }
                  } else if (var11.equals(double.class)) {
                     if (var12 instanceof String) {
                        var13 = Double.valueOf((String)var12);
                     } else if (var12 instanceof Double) {
                        var13 = var12;
                     }
                  } else if (var11.equals(boolean.class)) {
                     if (var12 instanceof String) {
                        var13 = Boolean.valueOf((String)var12);
                     } else if (var12 instanceof Boolean) {
                        var13 = var12;
                     }
                  } else if (var11.equals(byte.class)) {
                     if (var12 instanceof String) {
                        var13 = Byte.valueOf((String)var12);
                     } else if (var12 instanceof Byte) {
                        var13 = var12;
                     }
                  } else if (var11.equals(short.class)) {
                     if (var12 instanceof String) {
                        var13 = Short.valueOf((String)var12);
                     } else if (var12 instanceof Short) {
                        var13 = var12;
                     }
                  } else if (var11.equals(char.class)) {
                     if (var12 instanceof String) {
                        String var14 = (String)var12;
                        if (var14.length() == 1) {
                           var13 = var14.charAt(0);
                        }
                     } else if (var12 instanceof Character) {
                        var13 = var12;
                     }
                  }
               } else if (var12 != null && var11.isAssignableFrom(var12.getClass())) {
                  var13 = var12;
               }

               if (var12 != null && var13 == null) {
                  this.handleError(
                     "The setter on Job class "
                        + var1.getClass().getName()
                        + " for property '"
                        + var7
                        + "' expects a "
                        + var11
                        + " but was given "
                        + var12.getClass().getName()
                  );
               } else {
                  var10.invoke(var1, var13);
               }
            }
         } catch (NumberFormatException var16) {
            this.handleError(
               "The setter on Job class "
                  + var1.getClass().getName()
                  + " for property '"
                  + var7
                  + "' expects a "
                  + var11
                  + " but was given "
                  + var12.getClass().getName(),
               var16
            );
         } catch (IllegalArgumentException var17) {
            this.handleError(
               "The setter on Job class "
                  + var1.getClass().getName()
                  + " for property '"
                  + var7
                  + "' expects a "
                  + var11
                  + " but was given "
                  + var12.getClass().getName(),
               var17
            );
         } catch (IllegalAccessException var18) {
            this.handleError("The setter on Job class " + var1.getClass().getName() + " for property '" + var7 + "' could not be accessed.", var18);
         } catch (InvocationTargetException var19) {
            this.handleError("The setter on Job class " + var1.getClass().getName() + " for property '" + var7 + "' could not be invoked.", var19);
         }
      }
   }

   private void handleError(String var1) {
      this.handleError(var1, null);
   }

   private void handleError(String var1, Exception var2) {
      if (this.isThrowIfPropertyNotFound()) {
         throw new SchedulerException(var1, var2);
      } else {
         if (this.isWarnIfPropertyNotFound()) {
            if (var2 == null) {
               this.getLog().warn(var1);
            } else {
               this.getLog().warn(var1, var2);
            }
         }
      }
   }

   private Method getSetMethod(String var1, PropertyDescriptor[] var2) {
      for (int var3 = 0; var3 < var2.length; var3++) {
         Method var4 = var2[var3].getWriteMethod();
         if (var4 != null && var4.getParameterTypes().length == 1 && var4.getName().equals(var1)) {
            return var4;
         }
      }

      return null;
   }

   public boolean isThrowIfPropertyNotFound() {
      return this.throwIfNotFound;
   }

   public void setThrowIfPropertyNotFound(boolean var1) {
      this.throwIfNotFound = var1;
   }

   public boolean isWarnIfPropertyNotFound() {
      return this.warnIfNotFound;
   }

   public void setWarnIfPropertyNotFound(boolean var1) {
      this.warnIfNotFound = var1;
   }
}
