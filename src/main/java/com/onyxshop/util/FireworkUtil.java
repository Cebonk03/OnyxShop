package com.onyxshop.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkUtil {
   public FireworkUtil() {
   }

   public static ItemStack addEffect(ItemStack itemStack, ConfigurationSection section) throws ItemLoadException {
      FireworkMeta fireworkMeta = (FireworkMeta)itemStack.getItemMeta();
      if (section.getInt("duration", 3) != 0) {
         fireworkMeta.setPower(section.getInt("duration", 3));
      }

      if (section.contains("flicker") || section.contains("trail") || section.contains("shape") || section.contains("fade-colors")) {
         if (section.getStringList("colors").isEmpty()) {
            throw new ItemLoadException("To apply a flicker, trail or firework type you need to give the firework at least one color using the 'colors' option");
         }

         Builder builder = FireworkEffect.builder();
         List<FireworkUtil.FireworkColor> values = FireworkUtil.FireworkColor.matchColorsNames(section.getStringList("colors"));
         if (!values.isEmpty()) {
            values.forEach(entry -> builder.withColor(entry.parseColor()));
         } else {
            builder.withColor(FireworkUtil.FireworkColor.WHITE.parseColor());
         }

         if (!section.getStringList("fade-colors").isEmpty()) {
            FireworkUtil.FireworkColor.matchColorsNames(section.getStringList("fade-colors")).forEach(entry -> builder.withFade(entry.parseColor()));
         }

         if (section.getBoolean("flicker", false)) {
            builder.withFlicker();
         }

         if (section.getBoolean("trail", false)) {
            builder.trail(true);
         }

         if (section.getString("shape") != null) {
            try {
               builder.with(Type.valueOf(section.getString("shape").toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException exception) {
               SendMessage.errorMessage("Could not add the firework shape '" + section.getString("shape") + "'");
            }
         }

         fireworkMeta.addEffect(builder.build());
      }

      itemStack.setItemMeta(fireworkMeta);
      return itemStack;
   }

   public static Map<String, Object> serialize(FireworkMeta fireworkMeta) {
      HashMap map = new HashMap();
      map.put("duration", fireworkMeta.getPower());
      if (!fireworkMeta.hasEffects()) {
         return map;
      } else {
         FireworkEffect fireworkEffect = (FireworkEffect)fireworkMeta.getEffects().get(0);
         map.put("colors", fireworkEffect.getColors().stream().map(element -> FireworkUtil.FireworkColor.matchColor(element.asRGB()).name()).collect(Collectors.toList()));
         map.put(
            "fade-colors", fireworkEffect.getFadeColors().stream().map(element -> FireworkUtil.FireworkColor.matchColor(element.asRGB()).name()).collect(Collectors.toList())
         );
         map.put("shape", fireworkEffect.getType().name());
         map.put("trail", fireworkEffect.hasTrail());
         map.put("flicker", fireworkEffect.hasFlicker());
         return map;
      }
   }

   public static enum FireworkColor {
      WHITE(0, 15790320),
      ORANGE(1, 15435844),
      MAGENTA(2, 12801229),
      LIGHT_BLUE(3, 6719955),
      YELLOW(4, 14602026),
      LIME(5, 4312372),
      PINK(6, 14188952),
      GRAY(7, 4408131),
      LIGHT_GRAY(8, 11250603),
      CYAN(9, 2651799),
      PURPLE(10, 8073150),
      BLUE(11, 2437522),
      BROWN(12, 5320730),
      GREEN(13, 3887386),
      RED(14, 11743532),
      BLACK(15, 1973019);

      int id;
      int rgb;

      private FireworkColor(int value, int value2) {
         this.id = value;
         this.rgb = value2;
      }

      public static FireworkUtil.FireworkColor matchColor(String text) {
         for (FireworkUtil.FireworkColor fireworkColor : values()) {
            if (fireworkColor.name().equalsIgnoreCase(text)) {
               return fireworkColor;
            }
         }

         return null;
      }

      public static List<FireworkUtil.FireworkColor> matchColorsNames(List<String> values) {
         ArrayList values2 = new ArrayList();

         for (String text : values) {
            for (FireworkUtil.FireworkColor fireworkColor : values()) {
               if (fireworkColor.name().equalsIgnoreCase(text)) {
                  values2.add(fireworkColor);
               }
            }
         }

         return values2;
      }

      public static List<FireworkUtil.FireworkColor> matchColorsIds(String text) {
         ArrayList values = new ArrayList();

         for (String text2 : text.split(",")) {
            try {
               for (FireworkUtil.FireworkColor fireworkColor : values()) {
                  if (fireworkColor.id == Integer.parseInt(text2)) {
                     values.add(fireworkColor);
                  }
               }
            } catch (NumberFormatException exception) {
            }
         }

         return values;
      }

      public static FireworkUtil.FireworkColor getFromID(int value) {
         for (FireworkUtil.FireworkColor fireworkColor : values()) {
            if (fireworkColor.id == value) {
               return fireworkColor;
            }
         }

         return null;
      }

      public static FireworkUtil.FireworkColor matchColor(int value) {
         for (FireworkUtil.FireworkColor fireworkColor : values()) {
            if (fireworkColor.rgb == value) {
               return fireworkColor;
            }
         }

         return null;
      }

      public Color getColor() {
         return Color.decode(String.valueOf(this.rgb));
      }

      public org.bukkit.Color parseColor() {
         return org.bukkit.Color.fromRGB(this.rgb);
      }

      public int getId() {
         return this.id;
      }
   }
}
