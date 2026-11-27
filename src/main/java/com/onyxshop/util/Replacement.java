package com.onyxshop.util;

import java.util.function.Supplier;

public class Replacement {
   public String key;
   public Supplier<String> value;

   public Replacement(String text, Supplier<String> task) {
      this.key = text;
      this.value = task;
   }
}
