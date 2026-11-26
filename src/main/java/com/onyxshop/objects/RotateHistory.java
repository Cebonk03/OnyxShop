package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RotateHistory {
   private final int rotations;
   private final AtomicInteger currentRotation = new AtomicInteger(0);
   private final ArrayList<String> history = new ArrayList<>();

   public RotateHistory(int var1) {
      this.rotations = var1;
   }

   public ArrayList<String> getHistory() {
      return this.history;
   }

   public int getCurrentRotation() {
      return this.currentRotation.get();
   }

   public void rotate(List<String> var1) {
      this.history.addAll(var1);
      if (this.currentRotation.incrementAndGet() == this.rotations) {
         this.currentRotation.set(0);
         this.history.clear();
      }
   }
}
