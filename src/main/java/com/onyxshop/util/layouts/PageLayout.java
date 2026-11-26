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

   public PageLayout(String var1) {
      this.section = var1;
   }

   @Override
   public boolean containsKey(int var1) {
      return this.staticItems.containsKey(var1);
   }

   @Override
   public void add(String var1) {
      this.softItems.add(var1);
   }

   @Override
   public void set(int var1, String var2) {
      this.staticItems.put(var1, var2);
   }

   @Override
   public List<String> build(Integer var1, int var2) {
      int var3 = Math.max(9, Math.min((int)Math.ceil((this.softItems.size() + this.staticItems.size()) / 9.0) * 9, var2));
      int var4 = var1 == null ? var3 : var1 - (OnyxShop.getInstance().navBar.isEnabled(this.section) ? 9 : 0);
      ArrayList var5 = new ArrayList();

      for (int var6 = 0; var6 < Math.min(this.softItems.size(), var4); var6++) {
         var5.add(this.softItems.get(var6));
      }

      this.softItems.removeAll(var5);
      ArrayList<Integer> var9 = new ArrayList<>(this.staticItems.keySet());
      Collections.sort(var9);

      for (Integer var8 : var9) {
         if (var8 <= var4) {
            while (var5.size() < var8) {
               var5.add(null);
            }

            var5.add(var8, this.staticItems.remove(var8));
         }
      }

      this.softItems.addAll(this.staticItems.values());

      while (var5.size() < var4) {
         var5.add(null);
      }

      if (!this.softItems.isEmpty()) {
         for (String var11 : this.softItems) {
            if (var11 != null) {
               SendMessage.warnMessage(
                  "Cannot load item '%itemPath%' because it is out of range for page size %size%"
                     .replace("%itemPath%", this.section + "." + var11)
                     .replace("%size%", String.valueOf(var4))
               );
            }
         }
      }

      return var5;
   }
}
