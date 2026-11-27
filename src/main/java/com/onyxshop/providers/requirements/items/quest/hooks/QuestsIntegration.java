package com.onyxshop.providers.requirements.items.quest.hooks;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.menu.itemstack.QItemStack;
import com.leonardobishop.quests.common.plugin.Quests;
import com.leonardobishop.quests.common.quest.Quest;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.quest.QuestManager;
import com.onyxshop.providers.requirements.items.quest.QuestProvider;
import org.bukkit.entity.Player;

public class QuestsIntegration implements QuestProvider {
   private final Quests questsAPI;

   public QuestsIntegration(OnyxShop plugin) {
      this.questsAPI = (Quests)plugin.getServer().getPluginManager().getPlugin("Quests");
   }

   @Override
   public void init(OnyxShop plugin, QuestManager questManager) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         plugin.runTaskLater(() -> {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Quests")) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            questManager.onLoad();
         }, 5L);
      } else {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
      }
   }

   @Override
   public boolean isReady() {
      com.leonardobishop.quests.common.quest.QuestManager questManager = this.questsAPI.getQuestManager();
      return questManager != null && !questManager.getQuests().isEmpty();
   }

   @Override
   public String getName() {
      return "Quests by LMBishop";
   }

   @Override
   public boolean isLoaded(String text) {
      return this.questsAPI.getQuestManager().getQuestById(text) != null;
   }

   @Override
   public String getQuestName(String name) {
      Quest quest = this.questsAPI.getQuestManager().getQuestById(name);
      QItemStack qItemStack = ((BukkitQuestsPlugin)this.questsAPI).getQItemStackRegistry().getQuestItemStack(quest);
      return qItemStack.getName();
   }

   @Override
   public boolean hasCompleted(Player player, String questId) {
      Quest quest = this.questsAPI.getQuestManager().getQuestById(questId);
      return this.questsAPI.getPlayerManager().getPlayer(player.getUniqueId()).getQuestProgressFile().getQuestProgress(quest).isCompleted();
   }
}
