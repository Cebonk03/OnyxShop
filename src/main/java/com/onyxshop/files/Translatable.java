package com.onyxshop.files;

import java.util.List;
import org.bukkit.entity.Player;

public interface Translatable {
   char BLACK = '0';
   char DARK_BLUE = '1';
   char DARK_GREEN = '2';
   char DARK_AQUA = '3';
   char DARK_RED = '4';
   char DARK_PURPLE = '5';
   char GOLD = '6';
   char GRAY = '7';
   char DARK_GRAY = '8';
   char BLUE = '9';
   char GREEN = 'a';
   char AQUA = 'b';
   char RED = 'c';
   char LIGHT_PURPLE = 'd';
   char YELLOW = 'e';
   char WHITE = 'f';

   Translatable replace(String var1, String var2);

   Translatable replace(String var1, Translatable var2);

   String getLegacy();

   String getBedrockLegacy();

   @Override
   String toString();

   Object get();

   List<Translatable> getLines();

   Translatable translate(Player var1);

   Translatable get(Player var1);

   boolean isEmpty();

   Translatable.Builder builder();

   public interface Builder {
      Translatable.Builder append(Translatable var1);

      Translatable.Builder append(String var1);

      Translatable.Builder color(char var1);

      Translatable build();
   }
}
