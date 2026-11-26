package org.quartz.impl.matchers;

import org.quartz.Matcher;
import org.quartz.utils.Key;

public abstract class StringMatcher<T extends Key<?>> implements Matcher<T> {
   private static final long serialVersionUID = -2757924162611145836L;
   protected String compareTo;
   protected StringMatcher.StringOperatorName compareWith;

   protected StringMatcher(String var1, StringMatcher.StringOperatorName var2) {
      if (var1 == null) {
         throw new IllegalArgumentException("CompareTo value cannot be null!");
      } else if (var2 == null) {
         throw new IllegalArgumentException("CompareWith operator cannot be null!");
      } else {
         this.compareTo = var1;
         this.compareWith = var2;
      }
   }

   protected abstract String getValue(T var1);

   @Override
   public boolean isMatch(T var1) {
      return this.compareWith.evaluate(this.getValue((T)var1), this.compareTo);
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      int var2 = 1;
      var2 = 31 * var2 + (this.compareTo == null ? 0 : this.compareTo.hashCode());
      return 31 * var2 + (this.compareWith == null ? 0 : this.compareWith.hashCode());
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
         StringMatcher var2 = (StringMatcher)var1;
         if (this.compareTo == null) {
            if (var2.compareTo != null) {
               return false;
            }
         } else if (!this.compareTo.equals(var2.compareTo)) {
            return false;
         }

         if (this.compareWith == null) {
            if (var2.compareWith != null) {
               return false;
            }
         } else if (!this.compareWith.equals(var2.compareWith)) {
            return false;
         }

         return true;
      }
   }

   public String getCompareToValue() {
      return this.compareTo;
   }

   public StringMatcher.StringOperatorName getCompareWithOperator() {
      return this.compareWith;
   }

   public static enum StringOperatorName {
      EQUALS {
         @Override
         public boolean evaluate(String var1, String var2) {
            return var1.equals(var2);
         }
      },
      STARTS_WITH {
         @Override
         public boolean evaluate(String var1, String var2) {
            return var1.startsWith(var2);
         }
      },
      ENDS_WITH {
         @Override
         public boolean evaluate(String var1, String var2) {
            return var1.endsWith(var2);
         }
      },
      CONTAINS {
         @Override
         public boolean evaluate(String var1, String var2) {
            return var1.contains(var2);
         }
      },
      ANYTHING {
         @Override
         public boolean evaluate(String var1, String var2) {
            return true;
         }
      };

      private StringOperatorName() {
      }

      public abstract boolean evaluate(String var1, String var2);
   }
}
