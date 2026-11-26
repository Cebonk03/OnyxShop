package org.quartz.utils;

import java.io.Serializable;
import java.util.UUID;

public class Key<T> implements Serializable, Comparable<Key<T>> {
   private static final long serialVersionUID = -7141167957642391350L;
   public static final String DEFAULT_GROUP = "DEFAULT";
   private final String name;
   private final String group;

   public Key(String var1, String var2) {
      if (var1 == null) {
         throw new IllegalArgumentException("Name cannot be null.");
      } else {
         this.name = var1;
         if (var2 != null) {
            this.group = var2;
         } else {
            this.group = "DEFAULT";
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public String getGroup() {
      return this.group;
   }

   @Override
   public String toString() {
      return this.getGroup() + '.' + this.getName();
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      int var2 = 1;
      var2 = 31 * var2 + (this.group == null ? 0 : this.group.hashCode());
      return 31 * var2 + (this.name == null ? 0 : this.name.hashCode());
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         Key var2 = (Key)var1;
         if (this.group == null) {
            if (var2.group != null) {
               return false;
            }
         } else if (!this.group.equals(var2.group)) {
            return false;
         }

         if (this.name == null) {
            if (var2.name != null) {
               return false;
            }
         } else if (!this.name.equals(var2.name)) {
            return false;
         }

         return true;
      }
   }

   public int compareTo(Key<T> var1) {
      if (this.group.equals("DEFAULT") && !var1.group.equals("DEFAULT")) {
         return -1;
      } else if (!this.group.equals("DEFAULT") && var1.group.equals("DEFAULT")) {
         return 1;
      } else {
         int var2 = this.group.compareTo(var1.getGroup());
         return var2 != 0 ? var2 : this.name.compareTo(var1.getName());
      }
   }

   public static String createUniqueName(String var0) {
      if (var0 == null) {
         var0 = "DEFAULT";
      }

      String var1 = UUID.randomUUID().toString();
      String var2 = UUID.nameUUIDFromBytes(var0.getBytes()).toString();
      return String.format("%s-%s", var2.substring(24), var1);
   }
}
