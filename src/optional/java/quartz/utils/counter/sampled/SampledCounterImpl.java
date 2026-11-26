package org.quartz.utils.counter.sampled;

import java.util.TimerTask;
import org.quartz.utils.CircularLossyQueue;
import org.quartz.utils.counter.CounterImpl;

public class SampledCounterImpl extends CounterImpl implements SampledCounter {
   private static final long serialVersionUID = -3605369302464131521L;
   private static final int MILLIS_PER_SEC = 1000;
   protected final CircularLossyQueue<TimeStampedCounterValue> history;
   protected final boolean resetOnSample;
   private final TimerTask samplerTask;
   private final long intervalMillis;

   public SampledCounterImpl(SampledCounterConfig var1) {
      super(var1.getInitialValue());
      this.intervalMillis = var1.getIntervalSecs() * 1000;
      this.history = new CircularLossyQueue<>(var1.getHistorySize());
      this.resetOnSample = var1.isResetOnSample();
      this.samplerTask = new TimerTask() {
         @Override
         public void run() {
            SampledCounterImpl.this.recordSample();
         }
      };
      this.recordSample();
   }

   @Override
   public TimeStampedCounterValue getMostRecentSample() {
      return this.history.peek();
   }

   @Override
   public TimeStampedCounterValue[] getAllSampleValues() {
      return this.history.toArray(new TimeStampedCounterValue[this.history.depth()]);
   }

   @Override
   public void shutdown() {
      if (this.samplerTask != null) {
         this.samplerTask.cancel();
      }
   }

   public TimerTask getTimerTask() {
      return this.samplerTask;
   }

   public long getIntervalMillis() {
      return this.intervalMillis;
   }

   void recordSample() {
      long var1;
      if (this.resetOnSample) {
         var1 = this.getAndReset();
      } else {
         var1 = this.getValue();
      }

      long var3 = System.currentTimeMillis();
      TimeStampedCounterValue var5 = new TimeStampedCounterValue(var3, var1);
      this.history.push(var5);
   }

   @Override
   public long getAndReset() {
      return this.getAndSet(0L);
   }
}
