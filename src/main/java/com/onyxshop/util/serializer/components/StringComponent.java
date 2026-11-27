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

   public void addComponent(Component component) {
      this.extra.add(component);
   }

   public ArrayList<Component> getAll() {
      return this.extra;
   }

   public JSONObject serialize() {
      HashMap map = new HashMap();
      map.put("text", "");
      JSONArray jsonArray = new JSONArray();

      for (Component component : this.extra) {
         jsonArray.add(component.serialize());
      }

      map.put("extra", jsonArray);
      return new JSONObject(map);
   }
}
