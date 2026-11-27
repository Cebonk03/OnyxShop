package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RotateHistory {
   private final int rotations;
   private final AtomicInteger currentRotation = new AtomicInteger(0);
   private final ArrayList<String> history = new ArrayList<>();

   public RotateHistory(int value) {
      this.rotations = value;
   }

   public ArrayList<String> getHistory() {
      return this.history;
   }

   public int getCurrentRotation() {
      return this.currentRotation.get();
   }

   public void rotate(List<String> values) {
      this.history.addAll(values);
      if (this.currentRotation.incrementAndGet() == this.rotations) {
         this.currentRotation.set(0);
         this.history.clear();
      }
   }
}
