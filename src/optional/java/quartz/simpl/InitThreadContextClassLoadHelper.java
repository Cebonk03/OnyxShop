package org.quartz.simpl;

import java.io.InputStream;
import java.net.URL;
import org.quartz.spi.ClassLoadHelper;

public class InitThreadContextClassLoadHelper implements ClassLoadHelper {
   private ClassLoader initClassLoader;

   public InitThreadContextClassLoadHelper() {
   }

   @Override
   public void initialize() {
      this.initClassLoader = Thread.currentThread().getContextClassLoader();
   }

   @Override
   public Class<?> loadClass(String var1) {
      return this.initClassLoader.loadClass(var1);
   }

   @Override
   public <T> Class<? extends T> loadClass(String var1, Class<T> var2) {
      return (Class<? extends T>)this.loadClass(var1);
   }

   @Override
   public URL getResource(String var1) {
      return this.initClassLoader.getResource(var1);
   }

   @Override
   public InputStream getResourceAsStream(String var1) {
      return this.initClassLoader.getResourceAsStream(var1);
   }

   @Override
   public ClassLoader getClassLoader() {
      return this.initClassLoader;
   }
}
