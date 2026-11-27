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

   public ReservedLayout(String text) {
      this.section = text;
   }

   public void reserve(int value) {
      this.reserved.put(value, new ReservedLayout.Reserved());
   }

   @Override
   public boolean containsKey(int value) {
      ReservedLayout.Reserved reserved = this.reserved.get(value);
      return reserved != null && reserved.index != null;
   }

   @Override
   public void add(String text) {
      for (ReservedLayout.Reserved reserved : this.reserved.values()) {
         if (reserved.getIndex() == null) {
            reserved.setIndex(text);
            return;
         }
      }

      this.outOfBounds.add(new ReservedLayout.Reserved(text));
   }

   @Override
   public void set(int value, String text) {
      ReservedLayout.Reserved reserved = this.reserved.get(value);
      if (reserved != null && reserved.index != null) {
         this.shift(value, text);
      } else {
         this.reserved.put(value, new ReservedLayout.Reserved(text));
      }
   }

   private void shift(int value, String text) {
      LinkedList<String> values = new LinkedList<>();
      ArrayList<Integer> values2 = new ArrayList<>(this.reserved.keySet());
      Collections.sort(values2);
      int value2 = (Integer)values2.get(values2.size() - 1);

      for (int index = value; index < value2; index++) {
         ReservedLayout.Reserved reserved = this.reserved.get(index);
         if (reserved != null) {
            if (values.isEmpty() && reserved.index == null) {
               reserved.setIndex(text);
               return;
            }

            values.add(reserved.index);
            reserved.setIndex(null);
         }
      }

      for (String text2 : values) {
         this.add(text2);
      }
   }

   @Override
   public List<String> build(Integer value, int value2) {
      int value3 = this.reserved
         .entrySet()
         .stream()
         .filter(entry -> entry.getValue().index != null)
         .map(Entry::getKey)
         .collect(Collectors.toList())
         .stream()
         .max(Integer::compareTo)
         .orElse(value2);
      int value4 = Math.max(9, Math.min((int)Math.ceil(value3 / 9.0) * 9, value2));
      int value5 = value == null ? value4 : value - (OnyxShop.getInstance().navBar.isEnabled(this.section) ? 9 : 0);
      ArrayList values = new ArrayList();

      for (int index = 0; index < value5; index++) {
         ReservedLayout.Reserved reserved = this.reserved.remove(index);
         if (reserved != null && reserved.index != null) {
            values.add(reserved.getIndex());
         } else {
            values.add(null);
         }
      }

      while (values.size() < value5) {
         values.add(null);
      }

      this.outOfBounds.addAll(this.reserved.values());
      if (!this.outOfBounds.isEmpty()) {
         for (ReservedLayout.Reserved reserved : this.outOfBounds) {
            if (reserved.index != null) {
               SendMessage.warnMessage(
                  "Cannot load item '%itemPath%' because it is out of range for page size %size%"
                     .replace("%itemPath%", this.section + "." + reserved.index)
                     .replace("%size%", String.valueOf(value5))
               );
            }
         }
      }

      return values;
   }

   public static final class Reserved {
      private String index;

      public Reserved() {
      }

      public Reserved(String text) {
         this.index = text;
      }

      public void setIndex(String text) {
         this.index = text;
      }

      public String getIndex() {
         return this.index;
      }
   }
}
