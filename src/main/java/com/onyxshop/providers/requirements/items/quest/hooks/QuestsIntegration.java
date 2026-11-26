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

   public QuestsIntegration(OnyxShop var1) {
      this.questsAPI = (Quests)var1.getServer().getPluginManager().getPlugin("Quests");
   }

   @Override
   public void init(OnyxShop var1, QuestManager var2) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         var1.runTaskLater(() -> {
            if (var1.getServer().getPluginManager().isPluginEnabled("Quests")) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            var2.onLoad();
         }, 5L);
      } else {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
      }
   }

   @Override
   public boolean isReady() {
      com.leonardobishop.quests.common.quest.QuestManager var1 = this.questsAPI.getQuestManager();
      return var1 != null && !var1.getQuests().isEmpty();
   }

   @Override
   public String getName() {
      return "Quests by LMBishop";
   }

   @Override
   public boolean isLoaded(String var1) {
      return this.questsAPI.getQuestManager().getQuestById(var1) != null;
   }

   @Override
   public String getQuestName(String var1) {
      Quest var2 = this.questsAPI.getQuestManager().getQuestById(var1);
      QItemStack var3 = ((BukkitQuestsPlugin)this.questsAPI).getQItemStackRegistry().getQuestItemStack(var2);
      return var3.getName();
   }

   @Override
   public boolean hasCompleted(Player var1, String var2) {
      Quest var3 = this.questsAPI.getQuestManager().getQuestById(var2);
      return this.questsAPI.getPlayerManager().getPlayer(var1.getUniqueId()).getQuestProgressFile().getQuestProgress(var3).isCompleted();
   }
}
