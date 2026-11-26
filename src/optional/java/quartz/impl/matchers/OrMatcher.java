package org.quartz.impl.matchers;

import org.quartz.Matcher;
import org.quartz.utils.Key;

public class OrMatcher<T extends Key<?>> implements Matcher<T> {
   private static final long serialVersionUID = -2867392824539403712L;
   protected Matcher<T> leftOperand;
   protected Matcher<T> rightOperand;

   protected OrMatcher(Matcher<T> var1, Matcher<T> var2) {
      if (var1 != null && var2 != null) {
         this.leftOperand = var1;
         this.rightOperand = var2;
      } else {
         throw new IllegalArgumentException("Two non-null operands required!");
      }
   }

   public static <U extends Key<?>> OrMatcher<U> or(Matcher<U> var0, Matcher<U> var1) {
      return new OrMatcher<>(var0, var1);
   }

   @Override
   public boolean isMatch(T var1) {
      return this.leftOperand.isMatch((T)var1) || this.rightOperand.isMatch((T)var1);
   }

   public Matcher<T> getLeftOperand() {
      return this.leftOperand;
   }

   public Matcher<T> getRightOperand() {
      return this.rightOperand;
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      int var2 = 1;
      var2 = 31 * var2 + (this.leftOperand == null ? 0 : this.leftOperand.hashCode());
      return 31 * var2 + (this.rightOperand == null ? 0 : this.rightOperand.hashCode());
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
         OrMatcher var2 = (OrMatcher)var1;
         if (this.leftOperand == null) {
            if (var2.leftOperand != null) {
               return false;
            }
         } else if (!this.leftOperand.equals(var2.leftOperand)) {
            return false;
         }

         if (this.rightOperand == null) {
            if (var2.rightOperand != null) {
               return false;
            }
         } else if (!this.rightOperand.equals(var2.rightOperand)) {
            return false;
         }

         return true;
      }
   }
}
