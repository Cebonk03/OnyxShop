package com.onyxshop.util.sound;

import org.bukkit.entity.Player;

public class KeyedSound implements Sound {
   private final String sound;

   public KeyedSound(String text) {
      this.sound = text;
   }

   @Override
   public void play(Player player, float amount, float amount2) {
      player.playSound(player.getLocation(), this.sound, amount, amount2);
   }
}
