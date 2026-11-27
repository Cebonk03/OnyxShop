package com.onyxshop.util;

public class SimplePair<K, V> {
   public final K key;
   public final V value;

   public SimplePair(K value, V value2) {
      this.key = (K)value;
      this.value = (V)value2;
   }
}
