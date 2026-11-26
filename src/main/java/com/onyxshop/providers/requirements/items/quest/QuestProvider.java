package com.onyxshop.providers.requirements.items.quest;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface QuestProvider {
   void init(OnyxShop var1, QuestManager var2);

   boolean isReady();

   String getName();

   boolean isLoaded(String var1);

   String getQuestName(String var1);

   boolean hasCompleted(Player var1, String var2);
}
