package com.onyxshop.util.sound;

import com.onyxshop.util.ReflectUtil;
import org.bukkit.entity.Player;

public class BukkitSound implements Sound {
   private final org.bukkit.Sound sound;

   public BukkitSound(org.bukkit.Sound sound) {
      this.sound = sound;
   }

   public static BukkitSound valueOf(String text) {
      try {
         return new BukkitSound((org.bukkit.Sound)ReflectUtil.invokeStaticLegacyVersionMethod("valueOf", "org.bukkit.Sound", false, text));
      } catch (Exception exception) {
         return null;
      }
   }

   @Override
   public void play(Player player, float amount, float amount2) {
      player.playSound(player.getLocation(), this.sound, amount, amount2);
   }
}
