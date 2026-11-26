package com.onyxshop.util;

import java.util.List;

public interface ItemLayout {
   boolean containsKey(int var1);

   void add(String var1);

   void set(int var1, String var2);

   List<String> build(Integer var1, int var2);
}
