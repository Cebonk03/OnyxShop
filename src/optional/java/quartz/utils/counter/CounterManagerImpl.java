package org.quartz.utils.counter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import org.quartz.utils.counter.sampled.SampledCounter;
import org.quartz.utils.counter.sampled.SampledCounterImpl;

public class CounterManagerImpl implements CounterManager {
   private Timer timer;
   private boolean shutdown;
   private List<Counter> counters = new ArrayList<>();

   public CounterManagerImpl(Timer var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Timer cannot be null");
      } else {
         this.timer = var1;
      }
   }

   @Override
   public synchronized void shutdown(boolean var1) {
      if (!this.shutdown) {
         try {
            for (Counter var3 : this.counters) {
               if (var3 instanceof SampledCounter) {
                  ((SampledCounter)var3).shutdown();
               }
            }

            if (var1) {
               this.timer.cancel();
            }
         } finally {
            this.shutdown = true;
         }
      }
   }

   @Override
   public synchronized Counter createCounter(CounterConfig var1) {
      if (this.shutdown) {
         throw new IllegalStateException("counter manager is shutdown");
      } else if (var1 == null) {
         throw new NullPointerException("config cannot be null");
      } else {
         Counter var2 = var1.createCounter();
         if (var2 instanceof SampledCounterImpl) {
            SampledCounterImpl var3 = (SampledCounterImpl)var2;
            this.timer.schedule(var3.getTimerTask(), var3.getIntervalMillis(), var3.getIntervalMillis());
         }

         this.counters.add(var2);
         return var2;
      }
   }

   @Override
   public void shutdownCounter(Counter var1) {
      if (var1 instanceof SampledCounter) {
         SampledCounter var2 = (SampledCounter)var1;
         var2.shutdown();
      }
   }
}
