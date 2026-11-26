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

   public static ItemStack addEffect(ItemStack var0, ConfigurationSection var1) throws ItemLoadException {
      FireworkMeta var2 = (FireworkMeta)var0.getItemMeta();
      if (var1.getInt("duration", 3) != 0) {
         var2.setPower(var1.getInt("duration", 3));
      }

      if (var1.contains("flicker") || var1.contains("trail") || var1.contains("shape") || var1.contains("fade-colors")) {
         if (var1.getStringList("colors").isEmpty()) {
            throw new ItemLoadException("To apply a flicker, trail or firework type you need to give the firework at least one color using the 'colors' option");
         }

         Builder var3 = FireworkEffect.builder();
         List<FireworkUtil.FireworkColor> var4 = FireworkUtil.FireworkColor.matchColorsNames(var1.getStringList("colors"));
         if (!var4.isEmpty()) {
            var4.forEach(var1x -> var3.withColor(var1x.parseColor()));
         } else {
            var3.withColor(FireworkUtil.FireworkColor.WHITE.parseColor());
         }

         if (!var1.getStringList("fade-colors").isEmpty()) {
            FireworkUtil.FireworkColor.matchColorsNames(var1.getStringList("fade-colors")).forEach(var1x -> var3.withFade(var1x.parseColor()));
         }

         if (var1.getBoolean("flicker", false)) {
            var3.withFlicker();
         }

         if (var1.getBoolean("trail", false)) {
            var3.trail(true);
         }

         if (var1.getString("shape") != null) {
            try {
               var3.with(Type.valueOf(var1.getString("shape").toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException var6) {
               SendMessage.errorMessage("Could not add the firework shape '" + var1.getString("shape") + "'");
            }
         }

         var2.addEffect(var3.build());
      }

      var0.setItemMeta(var2);
      return var0;
   }

   public static Map<String, Object> serialize(FireworkMeta var0) {
      HashMap var1 = new HashMap();
      var1.put("duration", var0.getPower());
      if (!var0.hasEffects()) {
         return var1;
      } else {
         FireworkEffect var2 = (FireworkEffect)var0.getEffects().get(0);
         var1.put("colors", var2.getColors().stream().map(var0x -> FireworkUtil.FireworkColor.matchColor(var0x.asRGB()).name()).collect(Collectors.toList()));
         var1.put(
            "fade-colors", var2.getFadeColors().stream().map(var0x -> FireworkUtil.FireworkColor.matchColor(var0x.asRGB()).name()).collect(Collectors.toList())
         );
         var1.put("shape", var2.getType().name());
         var1.put("trail", var2.hasTrail());
         var1.put("flicker", var2.hasFlicker());
         return var1;
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

      private FireworkColor(int nullxx, int nullxxx) {
         this.id = nullxx;
         this.rgb = nullxxx;
      }

      public static FireworkUtil.FireworkColor matchColor(String var0) {
         for (FireworkUtil.FireworkColor var4 : values()) {
            if (var4.name().equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return null;
      }

      public static List<FireworkUtil.FireworkColor> matchColorsNames(List<String> var0) {
         ArrayList var1 = new ArrayList();

         for (String var3 : var0) {
            for (FireworkUtil.FireworkColor var7 : values()) {
               if (var7.name().equalsIgnoreCase(var3)) {
                  var1.add(var7);
               }
            }
         }

         return var1;
      }

      public static List<FireworkUtil.FireworkColor> matchColorsIds(String var0) {
         ArrayList var1 = new ArrayList();

         for (String var5 : var0.split(",")) {
            try {
               for (FireworkUtil.FireworkColor var9 : values()) {
                  if (var9.id == Integer.parseInt(var5)) {
                     var1.add(var9);
                  }
               }
            } catch (NumberFormatException var10) {
            }
         }

         return var1;
      }

      public static FireworkUtil.FireworkColor getFromID(int var0) {
         for (FireworkUtil.FireworkColor var4 : values()) {
            if (var4.id == var0) {
               return var4;
            }
         }

         return null;
      }

      public static FireworkUtil.FireworkColor matchColor(int var0) {
         for (FireworkUtil.FireworkColor var4 : values()) {
            if (var4.rgb == var0) {
               return var4;
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
