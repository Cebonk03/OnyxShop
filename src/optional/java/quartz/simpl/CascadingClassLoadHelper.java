package org.quartz.simpl;

import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import org.quartz.spi.ClassLoadHelper;

public class CascadingClassLoadHelper implements ClassLoadHelper {
   private LinkedList<ClassLoadHelper> loadHelpers;
   private ClassLoadHelper bestCandidate;

   public CascadingClassLoadHelper() {
   }

   @Override
   public void initialize() throws ClassNotFoundException {
      this.loadHelpers = new LinkedList<>();
      this.loadHelpers.add(new LoadingLoaderClassLoadHelper());
      this.loadHelpers.add(new SimpleClassLoadHelper());
      this.loadHelpers.add(new ThreadContextClassLoadHelper());
      this.loadHelpers.add(new InitThreadContextClassLoadHelper());

      for (ClassLoadHelper var2 : this.loadHelpers) {
         var2.initialize();
      }
   }

   @Override
   public Class<?> loadClass(String var1) {
      if (this.bestCandidate != null) {
         try {
            return this.bestCandidate.loadClass(var1);
         } catch (Throwable var8) {
            this.bestCandidate = null;
         }
      }

      Throwable var2 = null;
      Class var3 = null;
      ClassLoadHelper var4 = null;
      Iterator var5 = this.loadHelpers.iterator();

      while (var5.hasNext()) {
         var4 = (ClassLoadHelper)var5.next();

         try {
            var3 = var4.loadClass(var1);
            break;
         } catch (Throwable var7) {
            var2 = var7;
         }
      }

      if (var3 == null) {
         if (var2 instanceof ClassNotFoundException) {
            throw (SQLException)var2;
         } else {
            throw new ClassNotFoundException(String.format("Unable to load class %s by any known loaders.", var1), var2);
         }
      } else {
         this.bestCandidate = var4;
         return var3;
      }
   }

   @Override
   public <T> Class<? extends T> loadClass(String var1, Class<T> var2) {
      return (Class<? extends T>)this.loadClass(var1);
   }

   @Override
   public URL getResource(String var1) {
      URL var2 = null;
      if (this.bestCandidate != null) {
         var2 = this.bestCandidate.getResource(var1);
         if (var2 != null) {
            return var2;
         }

         this.bestCandidate = null;
      }

      ClassLoadHelper var3 = null;
      Iterator var4 = this.loadHelpers.iterator();

      while (var4.hasNext()) {
         var3 = (ClassLoadHelper)var4.next();
         var2 = var3.getResource(var1);
         if (var2 != null) {
            break;
         }
      }

      this.bestCandidate = var3;
      return var2;
   }

   @Override
   public InputStream getResourceAsStream(String var1) {
      InputStream var2 = null;
      if (this.bestCandidate != null) {
         var2 = this.bestCandidate.getResourceAsStream(var1);
         if (var2 != null) {
            return var2;
         }

         this.bestCandidate = null;
      }

      ClassLoadHelper var3 = null;
      Iterator var4 = this.loadHelpers.iterator();

      while (var4.hasNext()) {
         var3 = (ClassLoadHelper)var4.next();
         var2 = var3.getResourceAsStream(var1);
         if (var2 != null) {
            break;
         }
      }

      this.bestCandidate = var3;
      return var2;
   }

   @Override
   public ClassLoader getClassLoader() {
      return this.bestCandidate == null ? Thread.currentThread().getContextClassLoader() : this.bestCandidate.getClassLoader();
   }
}
