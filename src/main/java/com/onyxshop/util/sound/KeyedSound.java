package com.onyxshop.util.sound;

import org.bukkit.entity.Player;

public class KeyedSound implements Sound {
   private final String sound;

   public KeyedSound(String var1) {
      this.sound = var1;
   }

   @Override
   public void play(Player var1, float var2, float var3) {
      var1.playSound(var1.getLocation(), this.sound, var2, var3);
   }
}
