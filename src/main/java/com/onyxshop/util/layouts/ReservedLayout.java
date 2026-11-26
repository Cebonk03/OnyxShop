package com.onyxshop.util.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ItemLayout;

public class ReservedLayout implements ItemLayout {
   private final String section;
   private final Map<Integer, ReservedLayout.Reserved> reserved = new HashMap<>();
   private final ArrayList<ReservedLayout.Reserved> outOfBounds = new ArrayList<>();

   public ReservedLayout(String var1) {
      this.section = var1;
   }

   public void reserve(int var1) {
      this.reserved.put(var1, new ReservedLayout.Reserved());
   }

   @Override
   public boolean containsKey(int var1) {
      ReservedLayout.Reserved var2 = this.reserved.get(var1);
      return var2 != null && var2.index != null;
   }

   @Override
   public void add(String var1) {
      for (ReservedLayout.Reserved var3 : this.reserved.values()) {
         if (var3.getIndex() == null) {
            var3.setIndex(var1);
            return;
         }
      }

      this.outOfBounds.add(new ReservedLayout.Reserved(var1));
   }

   @Override
   public void set(int var1, String var2) {
      ReservedLayout.Reserved var3 = this.reserved.get(var1);
      if (var3 != null && var3.index != null) {
         this.shift(var1, var2);
      } else {
         this.reserved.put(var1, new ReservedLayout.Reserved(var2));
      }
   }

   private void shift(int var1, String var2) {
      LinkedList<String> var3 = new LinkedList<>();
      ArrayList<Integer> var4 = new ArrayList<>(this.reserved.keySet());
      Collections.sort(var4);
      int var5 = (Integer)var4.get(var4.size() - 1);

      for (int var6 = var1; var6 < var5; var6++) {
         ReservedLayout.Reserved var7 = this.reserved.get(var6);
         if (var7 != null) {
            if (var3.isEmpty() && var7.index == null) {
               var7.setIndex(var2);
               return;
            }

            var3.add(var7.index);
            var7.setIndex(null);
         }
      }

      for (String var9 : var3) {
         this.add(var9);
      }
   }

   @Override
   public List<String> build(Integer var1, int var2) {
      int var3 = this.reserved
         .entrySet()
         .stream()
         .filter(var0 -> var0.getValue().index != null)
         .map(Entry::getKey)
         .collect(Collectors.toList())
         .stream()
         .max(Integer::compareTo)
         .orElse(var2);
      int var4 = Math.max(9, Math.min((int)Math.ceil(var3 / 9.0) * 9, var2));
      int var5 = var1 == null ? var4 : var1 - (OnyxShop.getInstance().navBar.isEnabled(this.section) ? 9 : 0);
      ArrayList var6 = new ArrayList();

      for (int var7 = 0; var7 < var5; var7++) {
         ReservedLayout.Reserved var8 = this.reserved.remove(var7);
         if (var8 != null && var8.index != null) {
            var6.add(var8.getIndex());
         } else {
            var6.add(null);
         }
      }

      while (var6.size() < var5) {
         var6.add(null);
      }

      this.outOfBounds.addAll(this.reserved.values());
      if (!this.outOfBounds.isEmpty()) {
         for (ReservedLayout.Reserved var10 : this.outOfBounds) {
            if (var10.index != null) {
               SendMessage.warnMessage(
                  "Cannot load item '%itemPath%' because it is out of range for page size %size%"
                     .replace("%itemPath%", this.section + "." + var10.index)
                     .replace("%size%", String.valueOf(var5))
               );
            }
         }
      }

      return var6;
   }

   public static final class Reserved {
      private String index;

      public Reserved() {
      }

      public Reserved(String var1) {
         this.index = var1;
      }

      public void setIndex(String var1) {
         this.index = var1;
      }

      public String getIndex() {
         return this.index;
      }
   }
}
