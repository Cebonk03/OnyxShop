package com.onyxshop.util.serializer.components;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StringComponent {
   final String text = "";
   final ArrayList<Component> extra = new ArrayList<>();

   public StringComponent() {
   }

   public void addComponent(Component var1) {
      this.extra.add(var1);
   }

   public ArrayList<Component> getAll() {
      return this.extra;
   }

   public JSONObject serialize() {
      HashMap var1 = new HashMap();
      var1.put("text", "");
      JSONArray var2 = new JSONArray();

      for (Component var4 : this.extra) {
         var2.add(var4.serialize());
      }

      var1.put("extra", var2);
      return new JSONObject(var1);
   }
}
