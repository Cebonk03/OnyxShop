package org.quartz.utils.counter.sampled;

import org.quartz.utils.counter.Counter;

public class SampledRateCounterConfig extends SampledCounterConfig {
   private final long initialNumeratorValue;
   private final long initialDenominatorValue;

   public SampledRateCounterConfig(int var1, int var2, boolean var3) {
      this(var1, var2, var3, 0L, 0L);
   }

   public SampledRateCounterConfig(int var1, int var2, boolean var3, long var4, long var6) {
      super(var1, var2, var3, 0L);
      this.initialNumeratorValue = var4;
      this.initialDenominatorValue = var6;
   }

   @Override
   public Counter createCounter() {
      SampledRateCounterImpl var1 = new SampledRateCounterImpl(this);
      var1.setValue(this.initialNumeratorValue, this.initialDenominatorValue);
      return var1;
   }
}
