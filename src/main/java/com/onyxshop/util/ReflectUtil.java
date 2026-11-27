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

   public static Object getPaperFeature(String text) {
      try {
         Class type = Class.forName("com.onyxshop." + text);
         return type.newInstance();
      } catch (IllegalAccessException | InstantiationException | ClassNotFoundException exception) {
         SendMessage.errorMessage("Failed to initialize paper feature, this is a bug");
         exception.printStackTrace();
         return null;
      }
   }

   public static Object getPaperFeature(String text, Object... values) {
      try {
         Class type = Class.forName("com.onyxshop." + text);
         Class[] type2 = new Class[values.length];

         for (int index = 0; index < values.length; index++) {
            type2[index] = values[index].getClass();
         }

         return type.getDeclaredConstructor(type2).newInstance(values);
      } catch (InvocationTargetException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static Object invokeModernVersionMethod(String text, Object... values) throws Exception {
      Class[] type = new Class[values.length];

      for (int index = 0; index < values.length; index++) {
         type[index] = values[index].getClass();
      }

      return invokeModernVersionMethod(text, type, values);
   }

   public static Object invokeModernVersionMethod(String text, Class<?>[] type, Object... values) throws Exception {
      Class type2 = OnyxShop.getInstance().versionHandler.getClass();
      Method method = type2.getDeclaredMethod(text, type);
      method.setAccessible(true);
      return method.invoke(OnyxShop.getInstance().versionHandler, values);
   }

   public static Object invokeLegacyVersionMethod(String text, String text2, Object... values) throws Exception {
      Class type = Class.forName(text2);
      Constructor constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object value = constructor.newInstance();
      return invokeLegacyVersionMethod(text, value, values);
   }

   public static Object invokeLegacyVersionMethod(String text, Object value, Object... values) throws Exception {
      Class[] type = new Class[values.length];

      for (int index = 0; index < values.length; index++) {
         type[index] = values[index].getClass();
      }

      Method method = value.getClass().getDeclaredMethod(text, type);
      method.setAccessible(true);
      return method.invoke(value, values);
   }

   public static Object invokeLegacyMethod(String text, Object value, String text2, Object... values) throws Exception {
      Class[] type = new Class[values.length];

      for (int index = 0; index < values.length; index++) {
         type[index] = values[index].getClass();
      }

      Method method = Class.forName(text2).getDeclaredMethod(text, type);
      method.setAccessible(true);
      return method.invoke(value, values);
   }

   public static Object invokeLegacyMethod(String text, Object value, Class<?>[] type, Object... values) throws Exception {
      Method method = value.getClass().getDeclaredMethod(text, type);
      method.setAccessible(true);
      return method.invoke(value, values);
   }

   public static Object invokeStaticLegacyVersionMethod(String text, String text2, boolean flag, Object... values) throws Exception {
      return invokeStaticLegacyMethod(text, flag ? CB_PACKAGE + text2 : text2, values);
   }

   public static Object invokeStaticLegacyMethod(String text, String text2, Object... values) throws Exception {
      Class type = Class.forName(text2);
      Class[] type2 = new Class[values.length];

      for (int index = 0; index < values.length; index++) {
         type2[index] = values[index].getClass();
      }

      Method method = type.getDeclaredMethod(text, type2);
      method.setAccessible(true);
      return method.invoke(null, values);
   }

   public static Object invokeStaticLegacyMethod(String text, String text2, Class<?>[] type, Object... values) throws Exception {
      return invokeStaticLegacyMethod(text, Class.forName(text2), type, values);
   }

   public static Object invokeStaticLegacyMethod(String text, Class<?> type, Class<?>[] type2, Object... values) throws Exception {
      Method method = type.getDeclaredMethod(text, type2);
      method.setAccessible(true);
      return method.invoke(null, values);
   }

   public static void setMetaValue(String text, Object value, Object value2) throws Exception {
      Field field = Class.forName(CB_PACKAGE + ".inventory.CraftMetaItem").getDeclaredField(text);
      field.setAccessible(true);
      field.set(value, value2);
   }

   public static boolean supportsPaperNativeComponents() {
      try {
         Field field = Class.forName(CB_PACKAGE + ".inventory.CraftMetaItem").getDeclaredField("displayName");
         field.setAccessible(true);
         return field.getType() == Class.forName("net.minecraft.network.chat.Component");
      } catch (ClassNotFoundException | NoSuchFieldException exception) {
         return false;
      }
   }

   public static boolean supportsMiniMessageBukkit() {
      try {
         Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudiences");
         return true;
      } catch (ClassNotFoundException exception) {
         return false;
      }
   }

   public static boolean isClassPresent(String text) {
      try {
         Class.forName(text);
         return true;
      } catch (ClassNotFoundException exception) {
         return false;
      }
   }
}
