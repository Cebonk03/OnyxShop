package org.quartz.utils.counter;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class CounterImpl implements Counter, Serializable {
   private static final long serialVersionUID = -1529134342654953984L;
   private AtomicLong value;

   public CounterImpl() {
      this(0L);
   }

   public CounterImpl(long var1) {
      this.value = new AtomicLong(var1);
   }

   @Override
   public long increment() {
      return this.value.incrementAndGet();
   }

   @Override
   public long decrement() {
      return this.value.decrementAndGet();
   }

   @Override
   public long getAndSet(long var1) {
      return this.value.getAndSet(var1);
   }

   @Override
   public long getValue() {
      return this.value.get();
   }

   @Override
   public long increment(long var1) {
      return this.value.addAndGet(var1);
   }

   @Override
   public long decrement(long var1) {
      return this.value.addAndGet(var1 * -1L);
   }

   @Override
   public void setValue(long var1) {
      this.value.set(var1);
   }
}
