package org.quartz.impl.matchers;

import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

public class NameMatcher<T extends Key<?>> extends StringMatcher<T> {
   private static final long serialVersionUID = -33104959459613480L;

   protected NameMatcher(String var1, StringMatcher.StringOperatorName var2) {
      super(var1, var2);
   }

   public static <T extends Key<?>> NameMatcher<T> nameEquals(String var0) {
      return new NameMatcher<>(var0, StringMatcher.StringOperatorName.EQUALS);
   }

   public static NameMatcher<JobKey> jobNameEquals(String var0) {
      return nameEquals(var0);
   }

   public static NameMatcher<TriggerKey> triggerNameEquals(String var0) {
      return nameEquals(var0);
   }

   public static <U extends Key<?>> NameMatcher<U> nameStartsWith(String var0) {
      return new NameMatcher<>(var0, StringMatcher.StringOperatorName.STARTS_WITH);
   }

   public static NameMatcher<JobKey> jobNameStartsWith(String var0) {
      return nameStartsWith(var0);
   }

   public static NameMatcher<TriggerKey> triggerNameStartsWith(String var0) {
      return nameStartsWith(var0);
   }

   public static <U extends Key<?>> NameMatcher<U> nameEndsWith(String var0) {
      return new NameMatcher<>(var0, StringMatcher.StringOperatorName.ENDS_WITH);
   }

   public static NameMatcher<JobKey> jobNameEndsWith(String var0) {
      return nameEndsWith(var0);
   }

   public static NameMatcher<TriggerKey> triggerNameEndsWith(String var0) {
      return nameEndsWith(var0);
   }

   public static <U extends Key<?>> NameMatcher<U> nameContains(String var0) {
      return new NameMatcher<>(var0, StringMatcher.StringOperatorName.CONTAINS);
   }

   public static NameMatcher<JobKey> jobNameContains(String var0) {
      return nameContains(var0);
   }

   public static NameMatcher<TriggerKey> triggerNameContains(String var0) {
      return nameContains(var0);
   }

   @Override
   protected String getValue(T var1) {
      return var1.getName();
   }
}
