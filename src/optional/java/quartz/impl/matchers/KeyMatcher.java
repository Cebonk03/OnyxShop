package org.quartz.impl.matchers;

import org.quartz.Matcher;
import org.quartz.utils.Key;

public class KeyMatcher<T extends Key<?>> implements Matcher<T> {
   private static final long serialVersionUID = 1230009869074992437L;
   protected T compareTo;

   protected KeyMatcher(T var1) {
      this.compareTo = (T)var1;
   }

   public static <U extends Key<?>> KeyMatcher<U> keyEquals(U var0) {
      return new KeyMatcher<>((U)var0);
   }

   @Override
   public boolean isMatch(T var1) {
      return this.compareTo.equals(var1);
   }

   public T getCompareToValue() {
      return this.compareTo;
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      byte var2 = 1;
      return 31 * var2 + (this.compareTo == null ? 0 : this.compareTo.hashCode());
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
         KeyMatcher var2 = (KeyMatcher)var1;
         if (this.compareTo == null) {
            if (var2.compareTo != null) {
               return false;
            }
         } else if (!this.compareTo.equals(var2.compareTo)) {
            return false;
         }

         return true;
      }
   }
}
