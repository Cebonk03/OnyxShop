package org.quartz.simpl;

import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.net.URL;
import org.quartz.spi.ClassLoadHelper;

public class SimpleClassLoadHelper implements ClassLoadHelper {
   public SimpleClassLoadHelper() {
   }

   @Override
   public void initialize() {
   }

   @Override
   public Class<?> loadClass(String var1) {
      return Class.forName(var1);
   }

   @Override
   public <T> Class<? extends T> loadClass(String var1, Class<T> var2) {
      return (Class<? extends T>)this.loadClass(var1);
   }

   @Override
   public URL getResource(String var1) {
      return this.getClassLoader().getResource(var1);
   }

   @Override
   public InputStream getResourceAsStream(String var1) {
      return this.getClassLoader().getResourceAsStream(var1);
   }

   @Override
   public ClassLoader getClassLoader() {
      try {
         ClassLoader var1 = this.getClass().getClassLoader();
         Method var2 = ClassLoader.class.getDeclaredMethod("getCallerClassLoader");
         AccessibleObject.setAccessible(new AccessibleObject[]{var2}, true);
         return (ClassLoader)var2.invoke(var1);
      } catch (Throwable var3) {
         return this.getClass().getClassLoader();
      }
   }
}
