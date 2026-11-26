package org.quartz.impl.matchers;

import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

public class GroupMatcher<T extends Key<?>> extends StringMatcher<T> {
   private static final long serialVersionUID = -3275767650469343849L;

   protected GroupMatcher(String var1, StringMatcher.StringOperatorName var2) {
      super(var1, var2);
   }

   public static <T extends Key<T>> GroupMatcher<T> groupEquals(String var0) {
      return new GroupMatcher<>(var0, StringMatcher.StringOperatorName.EQUALS);
   }

   public static GroupMatcher<JobKey> jobGroupEquals(String var0) {
      return groupEquals(var0);
   }

   public static GroupMatcher<TriggerKey> triggerGroupEquals(String var0) {
      return groupEquals(var0);
   }

   public static <T extends Key<T>> GroupMatcher<T> groupStartsWith(String var0) {
      return new GroupMatcher<>(var0, StringMatcher.StringOperatorName.STARTS_WITH);
   }

   public static GroupMatcher<JobKey> jobGroupStartsWith(String var0) {
      return groupStartsWith(var0);
   }

   public static GroupMatcher<TriggerKey> triggerGroupStartsWith(String var0) {
      return groupStartsWith(var0);
   }

   public static <T extends Key<T>> GroupMatcher<T> groupEndsWith(String var0) {
      return new GroupMatcher<>(var0, StringMatcher.StringOperatorName.ENDS_WITH);
   }

   public static GroupMatcher<JobKey> jobGroupEndsWith(String var0) {
      return groupEndsWith(var0);
   }

   public static GroupMatcher<TriggerKey> triggerGroupEndsWith(String var0) {
      return groupEndsWith(var0);
   }

   public static <T extends Key<T>> GroupMatcher<T> groupContains(String var0) {
      return new GroupMatcher<>(var0, StringMatcher.StringOperatorName.CONTAINS);
   }

   public static GroupMatcher<JobKey> jobGroupContains(String var0) {
      return groupContains(var0);
   }

   public static GroupMatcher<TriggerKey> triggerGroupContains(String var0) {
      return groupContains(var0);
   }

   public static <T extends Key<T>> GroupMatcher<T> anyGroup() {
      return new GroupMatcher<>("", StringMatcher.StringOperatorName.ANYTHING);
   }

   public static GroupMatcher<JobKey> anyJobGroup() {
      return anyGroup();
   }

   public static GroupMatcher<TriggerKey> anyTriggerGroup() {
      return anyGroup();
   }

   @Override
   protected String getValue(T var1) {
      return var1.getGroup();
   }
}
