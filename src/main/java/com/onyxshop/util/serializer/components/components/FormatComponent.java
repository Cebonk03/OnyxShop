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

   public FormatComponent(int var1) {
      this.useItalic = var1 > 1;
   }

   public FormatComponent(String var1, String var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7) {
      this.text = var1;
      this.color = var2;
      this.bold = var3;
      this.italic = var4;
      this.underlined = var5;
      this.obfuscated = var6;
      this.strikethrough = var7;
   }

   public void setText(String var1) {
      this.text = var1;
   }

   public void setColor(String var1) {
      this.color = var1;
   }

   public void setHex(String var1) {
      this.color = "#" + var1;
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
      HashMap var1 = new HashMap();
      var1.put("text", this.text);
      if (!this.color.isEmpty()) {
         var1.put("color", this.color);
      }

      if (this.bold) {
         var1.put("bold", true);
      }

      if (this.useItalic) {
         var1.put("italic", this.italic);
      }

      if (this.underlined) {
         var1.put("underlined", true);
      }

      if (this.obfuscated) {
         var1.put("obfuscated", true);
      }

      if (this.strikethrough) {
         var1.put("strikethrough", true);
      }

      return new JSONObject(var1);
   }
}
