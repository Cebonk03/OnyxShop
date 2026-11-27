package com.onyxshop.util;

import java.util.List;

public interface ItemLayout {
   boolean containsKey(int value);

   void add(String text);

   void set(int value, String text);

   List<String> build(Integer value, int value2);
}
