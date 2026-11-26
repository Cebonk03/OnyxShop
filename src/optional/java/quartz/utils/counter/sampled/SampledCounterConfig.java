package org.quartz.utils.counter.sampled;

import org.quartz.utils.counter.Counter;
import org.quartz.utils.counter.CounterConfig;

public class SampledCounterConfig extends CounterConfig {
   private final int intervalSecs;
   private final int historySize;
   private final boolean isReset;

   public SampledCounterConfig(int var1, int var2, boolean var3, long var4) {
      super(var4);
      if (var1 < 1) {
         throw new IllegalArgumentException("Interval (" + var1 + ") must be greater than or equal to 1");
      } else if (var2 < 1) {
         throw new IllegalArgumentException("History size (" + var2 + ") must be greater than or equal to 1");
      } else {
         this.intervalSecs = var1;
         this.historySize = var2;
         this.isReset = var3;
      }
   }

   public int getHistorySize() {
      return this.historySize;
   }

   public int getIntervalSecs() {
      return this.intervalSecs;
   }

   public boolean isResetOnSample() {
      return this.isReset;
   }

   @Override
   public Counter createCounter() {
      return new SampledCounterImpl(this);
   }
}
