package org.quartz.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;

public class ClassUtils {
   public ClassUtils() {
   }

   public static boolean isAnnotationPresent(Class<?> var0, Class<? extends Annotation> var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         if (var2.isAnnotationPresent(var1)) {
            return true;
         }

         if (isAnnotationPresentOnInterfaces(var2, var1)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isAnnotationPresentOnInterfaces(Class<?> var0, Class<? extends Annotation> var1) {
      for (Class var5 : var0.getInterfaces()) {
         if (var5.isAnnotationPresent(var1)) {
            return true;
         }

         if (isAnnotationPresentOnInterfaces(var5, var1)) {
            return true;
         }
      }

      return false;
   }

   public static <T extends Annotation> T getAnnotation(Class<?> var0, Class<T> var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         Annotation var3 = var2.getAnnotation(var1);
         if (var3 != null) {
            return (T)var3;
         }
      }

      LinkedList var5 = new LinkedList();
      var5.add(var0);

      while (!var5.isEmpty()) {
         Class var6 = (Class)var5.remove();
         if (var6 != null) {
            if (var6.isInterface()) {
               Annotation var4 = var6.getAnnotation(var1);
               if (var4 != null) {
                  return (T)var4;
               }
            } else {
               var5.add(var6.getSuperclass());
            }

            var5.addAll(Arrays.asList(var6.getInterfaces()));
         }
      }

      return null;
   }
}
