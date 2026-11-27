package com.onyxshop.util.serializer.components;

import java.util.ArrayList;
import org.json.simple.JSONArray;

public class ListComponent {
   final ArrayList<StringComponent> components = new ArrayList<>();

   public ListComponent() {
   }

   public void addComponent(StringComponent stringComponent) {
      this.components.add(stringComponent);
   }

   public JSONArray serialize() {
      JSONArray jsonArray = new JSONArray();

      for (StringComponent stringComponent : this.components) {
         jsonArray.add(stringComponent.serialize());
      }

      return jsonArray;
   }
}
