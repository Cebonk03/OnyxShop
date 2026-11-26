package com.onyxshop.util;

import java.util.function.Supplier;

public class Replacement {
   public String key;
   public Supplier<String> value;

   public Replacement(String var1, Supplier<String> var2) {
      this.key = var1;
      this.value = var2;
   }
}
