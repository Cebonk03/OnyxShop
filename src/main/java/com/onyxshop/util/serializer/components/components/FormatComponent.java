package com.onyxshop.util.serializer.components.components;

import java.util.HashMap;
import com.onyxshop.util.serializer.components.Component;
import org.json.simple.JSONObject;

public class FormatComponent implements Component {
   boolean useItalic;
   boolean reset;
   String text = "";
   String color = "";
   boolean bold;
   boolean italic;
   boolean underlined;
   boolean obfuscated;
   boolean strikethrough;

   public FormatComponent(int value) {
      this.useItalic = value > 1;
   }

   public FormatComponent(String text, String text2, boolean flag, boolean flag2, boolean flag3, boolean flag4, boolean flag5) {
      this.text = text;
      this.color = text2;
      this.bold = flag;
      this.italic = flag2;
      this.underlined = flag3;
      this.obfuscated = flag4;
      this.strikethrough = flag5;
   }

   public void setText(String text) {
      this.text = text;
   }

   public void setColor(String text) {
      this.color = text;
   }

   public void setHex(String text) {
      this.color = "#" + text;
   }

   public void setBold() {
      this.bold = true;
   }

   public void setItalic() {
      this.italic = true;
   }

   public void setUnderlined() {
      this.underlined = true;
   }

   public void setStrikethrough() {
      this.strikethrough = true;
   }

   public void setObfuscated() {
      this.obfuscated = true;
   }

   public void setReset() {
      this.reset = true;
   }

   public boolean isReset() {
      return this.reset;
   }

   public void useItalic() {
      this.useItalic = true;
   }

   @Override
   public Object serialize() {
      HashMap map = new HashMap();
      map.put("text", this.text);
      if (!this.color.isEmpty()) {
         map.put("color", this.color);
      }

      if (this.bold) {
         map.put("bold", true);
      }

      if (this.useItalic) {
         map.put("italic", this.italic);
      }

      if (this.underlined) {
         map.put("underlined", true);
      }

      if (this.obfuscated) {
         map.put("obfuscated", true);
      }

      if (this.strikethrough) {
         map.put("strikethrough", true);
      }

      return new JSONObject(map);
   }
}
