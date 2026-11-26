package org.quartz.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircularLossyQueue<T> {
   private final AtomicReference<T>[] circularArray;
   private final int maxSize;
   private final AtomicLong currentIndex = new AtomicLong(-1L);

   public CircularLossyQueue(int var1) {
      this.circularArray = new AtomicReference[var1];

      for (int var2 = 0; var2 < var1; var2++) {
         this.circularArray[var2] = new AtomicReference<>();
      }

      this.maxSize = var1;
   }

   public void push(T var1) {
      int var2 = (int)(this.currentIndex.incrementAndGet() % this.maxSize);
      this.circularArray[var2].set((T)var1);
   }

   public T[] toArray(T[] var1) {
      System.getProperties();
      if (var1.length > this.maxSize) {
         throw new IllegalArgumentException("Size of array passed in cannot be greater than " + this.maxSize);
      } else {
         int var2 = this.getCurrentIndex();

         for (int var3 = 0; var3 < var1.length; var3++) {
            int var4 = this.getIndex(var2 - var3);
            var1[var3] = this.circularArray[var4].get();
         }

         return (T[])var1;
      }
   }

   private int getIndex(int var1) {
      return var1 < 0 ? var1 + this.maxSize : var1;
   }

   public T peek() {
      return this.depth() == 0 ? null : this.circularArray[this.getIndex(this.getCurrentIndex())].get();
   }

   public boolean isEmtpy() {
      return this.depth() == 0;
   }

   private int getCurrentIndex() {
      return (int)(this.currentIndex.get() % this.maxSize);
   }

   public int depth() {
      long var1 = this.currentIndex.get() + 1L;
      return var1 >= this.maxSize ? this.maxSize : (int)var1;
   }
}
