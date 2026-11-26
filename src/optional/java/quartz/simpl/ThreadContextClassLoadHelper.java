package org.quartz.simpl;

import java.io.InputStream;
import java.net.URL;
import org.quartz.spi.ClassLoadHelper;

public class ThreadContextClassLoadHelper implements ClassLoadHelper {
   public ThreadContextClassLoadHelper() {
   }

   @Override
   public void initialize() {
   }

   @Override
   public Class<?> loadClass(String var1) {
      return this.getClassLoader().loadClass(var1);
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
      return Thread.currentThread().getContextClassLoader();
   }
}
