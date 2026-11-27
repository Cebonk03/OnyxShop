package com.onyxshop.util.serializer.components.components;

import com.onyxshop.util.serializer.components.Component;

public class TextComponent implements Component {
   public String text;

   public TextComponent(String text) {
      this.text = text;
   }

   @Override
   public Object serialize() {
      return this.text;
   }
}
