package org.quartz.impl.matchers;

import org.quartz.Matcher;
import org.quartz.utils.Key;

public class NotMatcher<T extends Key<?>> implements Matcher<T> {
   private static final long serialVersionUID = -2856769076151741391L;
   protected Matcher<T> operand;

   protected NotMatcher(Matcher<T> var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Non-null operand required!");
      } else {
         this.operand = var1;
      }
   }

   public static <U extends Key<?>> NotMatcher<U> not(Matcher<U> var0) {
      return new NotMatcher<>(var0);
   }

   @Override
   public boolean isMatch(T var1) {
      return !this.operand.isMatch((T)var1);
   }

   public Matcher<T> getOperand() {
      return this.operand;
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      byte var2 = 1;
      return 31 * var2 + (this.operand == null ? 0 : this.operand.hashCode());
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
         NotMatcher var2 = (NotMatcher)var1;
         if (this.operand == null) {
            if (var2.operand != null) {
               return false;
            }
         } else if (!this.operand.equals(var2.operand)) {
            return false;
         }

         return true;
      }
   }
}
