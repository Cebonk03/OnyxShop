package com.onyxshop.util.sound;

import com.onyxshop.util.ReflectUtil;
import org.bukkit.entity.Player;

public class BukkitSound implements Sound {
   private final org.bukkit.Sound sound;

   public BukkitSound(org.bukkit.Sound var1) {
      this.sound = var1;
   }

   public static BukkitSound valueOf(String var0) {
      try {
         return new BukkitSound((org.bukkit.Sound)ReflectUtil.invokeStaticLegacyVersionMethod("valueOf", "org.bukkit.Sound", false, var0));
      } catch (Exception var2) {
         return null;
      }
   }

   @Override
   public void play(Player var1, float var2, float var3) {
      var1.playSound(var1.getLocation(), this.sound, var2, var3);
   }
}
