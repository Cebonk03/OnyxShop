package org.quartz.utils.counter.sampled;

import java.io.Serializable;

public class TimeStampedCounterValue implements Serializable {
   private static final long serialVersionUID = 1931111347823687672L;
   private final long counterValue;
   private final long timestamp;

   public TimeStampedCounterValue(long var1, long var3) {
      this.timestamp = var1;
      this.counterValue = var3;
   }

   public long getCounterValue() {
      return this.counterValue;
   }

   public long getTimestamp() {
      return this.timestamp;
   }

   @Override
   public String toString() {
      return "value: " + this.counterValue + ", timestamp: " + this.timestamp;
   }
}
