package com.onyxshop.util.serializer.components;

import java.util.ArrayList;
import org.json.simple.JSONArray;

public class ListComponent {
   final ArrayList<StringComponent> components = new ArrayList<>();

   public ListComponent() {
   }

   public void addComponent(StringComponent var1) {
      this.components.add(var1);
   }

   public JSONArray serialize() {
      JSONArray var1 = new JSONArray();

      for (StringComponent var3 : this.components) {
         var1.add(var3.serialize());
      }

      return var1;
   }
}
