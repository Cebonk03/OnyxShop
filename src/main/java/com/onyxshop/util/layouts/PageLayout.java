package com.onyxshop.util.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ItemLayout;

public class PageLayout implements ItemLayout {
   private final String section;
   private final HashMap<Integer, String> staticItems = new HashMap<>();
   private final List<String> softItems = new LinkedList<>();

   public PageLayout(String text) {
      this.section = text;
   }

   @Override
   public boolean containsKey(int value) {
      return this.staticItems.containsKey(value);
   }

   @Override
   public void add(String text) {
      this.softItems.add(text);
   }

   @Override
   public void set(int value, String text) {
      this.staticItems.put(value, text);
   }

   @Override
   public List<String> build(Integer value, int value2) {
      int size = Math.max(9, Math.min((int)Math.ceil((this.softItems.size() + this.staticItems.size()) / 9.0) * 9, value2));
      int value3 = value == null ? size : value - (OnyxShop.getInstance().navBar.isEnabled(this.section) ? 9 : 0);
      ArrayList values = new ArrayList();

      for (int index = 0; index < Math.min(this.softItems.size(), value3); index++) {
         values.add(this.softItems.get(index));
      }

      this.softItems.removeAll(values);
      ArrayList<Integer> values2 = new ArrayList<>(this.staticItems.keySet());
      Collections.sort(values2);

      for (Integer value4 : values2) {
         if (value4 <= value3) {
            while (values.size() < value4) {
               values.add(null);
            }

            values.add(value4, this.staticItems.remove(value4));
         }
      }

      this.softItems.addAll(this.staticItems.values());

      while (values.size() < value3) {
         values.add(null);
      }

      if (!this.softItems.isEmpty()) {
         for (String text : this.softItems) {
            if (text != null) {
               SendMessage.warnMessage(
                  "Cannot load item '%itemPath%' because it is out of range for page size %size%"
                     .replace("%itemPath%", this.section + "." + text)
                     .replace("%size%", String.valueOf(value3))
               );
            }
         }
      }

      return values;
   }
}
