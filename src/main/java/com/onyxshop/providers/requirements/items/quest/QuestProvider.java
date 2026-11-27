package com.onyxshop.providers.requirements.items.quest;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface QuestProvider {
   void init(OnyxShop plugin, QuestManager questManager);

   boolean isReady();

   String getName();

   boolean isLoaded(String text);

   String getQuestName(String name);

   boolean hasCompleted(Player player, String questId);
}
