package com.onyxshop.providers.requirements;

import org.bukkit.entity.Player;

public interface ItemRequirement {
   boolean isMet(Player player);

   String[] getLore(Player player, boolean flag, boolean flag2);

   void sendNotMetMessage(Player player);
}
