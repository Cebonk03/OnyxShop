package org.quartz.utils.counter.sampled;

public class SampledRateCounterImpl extends SampledCounterImpl implements SampledRateCounter {
   private static final long serialVersionUID = 6531350452676920607L;
   private static final String OPERATION_NOT_SUPPORTED_MSG = "This operation is not supported. Use SampledCounter Or Counter instead";
   private long numeratorValue;
   private long denominatorValue;

   public SampledRateCounterImpl(SampledRateCounterConfig var1) {
      super(var1);
   }

   @Override
   public synchronized void setValue(long var1, long var3) {
      this.numeratorValue = var1;
      this.denominatorValue = var3;
   }

   @Override
   public synchronized void increment(long var1, long var3) {
      this.numeratorValue += var1;
      this.denominatorValue += var3;
   }

   @Override
   public synchronized void decrement(long var1, long var3) {
      this.numeratorValue -= var1;
      this.denominatorValue -= var3;
   }

   @Override
   public synchronized void setDenominatorValue(long var1) {
      this.denominatorValue = var1;
   }

   @Override
   public synchronized void setNumeratorValue(long var1) {
      this.numeratorValue = var1;
   }

   @Override
   public synchronized long getValue() {
      return this.denominatorValue == 0L ? 0L : this.numeratorValue / this.denominatorValue;
   }

   @Override
   public synchronized long getAndReset() {
      long var1 = this.getValue();
      this.setValue(0L, 0L);
      return var1;
   }

   @Override
   public long getAndSet(long var1) {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   @Override
   public synchronized void setValue(long var1) {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   @Override
   public long decrement() {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   @Override
   public long decrement(long var1) {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   public long getMaxValue() {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   public long getMinValue() {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   @Override
   public long increment() {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }

   @Override
   public long increment(long var1) {
      throw new UnsupportedOperationException("This operation is not supported. Use SampledCounter Or Counter instead");
   }
}
