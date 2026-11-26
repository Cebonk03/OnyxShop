package com.onyxshop.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.Bukkit;

public class ReflectUtil {
   private static final String CB_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();

   public static Object getPaperFeature(String var0) {
      try {
         Class var1 = Class.forName("com.onyxshop." + var0);
         return var1.newInstance();
      } catch (IllegalAccessException | InstantiationException | ClassNotFoundException var2) {
         SendMessage.errorMessage("Failed to initialize paper feature, this is a bug");
         var2.printStackTrace();
         return null;
      }
   }

   public static Object getPaperFeature(String var0, Object... var1) {
      try {
         Class var2 = Class.forName("com.onyxshop." + var0);
         Class[] var3 = new Class[var1.length];

         for (int var4 = 0; var4 < var1.length; var4++) {
            var3[var4] = var1[var4].getClass();
         }

         return var2.getDeclaredConstructor(var3).newInstance(var1);
      } catch (InvocationTargetException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException var5) {
         var5.printStackTrace();
         return null;
      }
   }

   public static Object invokeModernVersionMethod(String var0, Object... var1) throws Exception {
      Class[] var2 = new Class[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3].getClass();
      }

      return invokeModernVersionMethod(var0, var2, var1);
   }

   public static Object invokeModernVersionMethod(String var0, Class<?>[] var1, Object... var2) throws Exception {
      Class var3 = OnyxShop.getInstance().versionHandler.getClass();
      Method var4 = var3.getDeclaredMethod(var0, var1);
      var4.setAccessible(true);
      return var4.invoke(OnyxShop.getInstance().versionHandler, var2);
   }

   public static Object invokeLegacyVersionMethod(String var0, String var1, Object... var2) throws Exception {
      Class var3 = Class.forName(var1);
      Constructor var4 = var3.getDeclaredConstructor();
      var4.setAccessible(true);
      Object var5 = var4.newInstance();
      return invokeLegacyVersionMethod(var0, var5, var2);
   }

   public static Object invokeLegacyVersionMethod(String var0, Object var1, Object... var2) throws Exception {
      Class[] var3 = new Class[var2.length];

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3[var4] = var2[var4].getClass();
      }

      Method var5 = var1.getClass().getDeclaredMethod(var0, var3);
      var5.setAccessible(true);
      return var5.invoke(var1, var2);
   }

   public static Object invokeLegacyMethod(String var0, Object var1, String var2, Object... var3) throws Exception {
      Class[] var4 = new Class[var3.length];

      for (int var5 = 0; var5 < var3.length; var5++) {
         var4[var5] = var3[var5].getClass();
      }

      Method var6 = Class.forName(var2).getDeclaredMethod(var0, var4);
      var6.setAccessible(true);
      return var6.invoke(var1, var3);
   }

   public static Object invokeLegacyMethod(String var0, Object var1, Class<?>[] var2, Object... var3) throws Exception {
      Method var4 = var1.getClass().getDeclaredMethod(var0, var2);
      var4.setAccessible(true);
      return var4.invoke(var1, var3);
   }

   public static Object invokeStaticLegacyVersionMethod(String var0, String var1, boolean var2, Object... var3) throws Exception {
      return invokeStaticLegacyMethod(var0, var2 ? CB_PACKAGE + var1 : var1, var3);
   }

   public static Object invokeStaticLegacyMethod(String var0, String var1, Object... var2) throws Exception {
      Class var3 = Class.forName(var1);
      Class[] var4 = new Class[var2.length];

      for (int var5 = 0; var5 < var2.length; var5++) {
         var4[var5] = var2[var5].getClass();
      }

      Method var6 = var3.getDeclaredMethod(var0, var4);
      var6.setAccessible(true);
      return var6.invoke(null, var2);
   }

   public static Object invokeStaticLegacyMethod(String var0, String var1, Class<?>[] var2, Object... var3) throws Exception {
      return invokeStaticLegacyMethod(var0, Class.forName(var1), var2, var3);
   }

   public static Object invokeStaticLegacyMethod(String var0, Class<?> var1, Class<?>[] var2, Object... var3) throws Exception {
      Method var4 = var1.getDeclaredMethod(var0, var2);
      var4.setAccessible(true);
      return var4.invoke(null, var3);
   }

   public static void setMetaValue(String var0, Object var1, Object var2) throws Exception {
      Field var3 = Class.forName(CB_PACKAGE + ".inventory.CraftMetaItem").getDeclaredField(var0);
      var3.setAccessible(true);
      var3.set(var1, var2);
   }

   public static boolean supportsPaperNativeComponents() {
      try {
         Field var0 = Class.forName(CB_PACKAGE + ".inventory.CraftMetaItem").getDeclaredField("displayName");
         var0.setAccessible(true);
         return var0.getType() == Class.forName("net.minecraft.network.chat.Component");
      } catch (ClassNotFoundException | NoSuchFieldException var1) {
         return false;
      }
   }

   public static boolean supportsMiniMessageBukkit() {
      try {
         Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudiences");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   public static boolean isClassPresent(String var0) {
      try {
         Class.forName(var0);
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }
}
