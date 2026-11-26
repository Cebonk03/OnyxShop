package org.quartz.utils.counter;

public class CounterConfig {
   private final long initialValue;

   public CounterConfig(long var1) {
      this.initialValue = var1;
   }

   public final long getInitialValue() {
      return this.initialValue;
   }

   public Counter createCounter() {
      return new CounterImpl(this.initialValue);
   }
}
