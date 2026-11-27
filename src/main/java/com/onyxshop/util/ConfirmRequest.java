package com.onyxshop.util;

import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import org.bukkit.ChatColor;
import org.bukkit.conversations.BooleanPrompt;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfirmRequest {
   public ConfirmRequest(OnyxShop plugin, String shopId, Player player, Consumer<Boolean> task) {
      ConversationFactory conversationFactory = new ConversationFactory(plugin);
      Conversation conversation = conversationFactory.withFirstPrompt(this.getPrompt(shopId, task))
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(element -> Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ")
         .addConversationAbandonedListener(element -> {
            if (element.gracefulExit()) {
               task.accept((Boolean)element.getContext().getSessionData("input"));
            } else {
               task.accept(false);
            }
         })
         .buildConversation(player);
      conversation.begin();
   }

   private Prompt getPrompt(final String text, Consumer<Boolean> task) {
      return new BooleanPrompt() {
         @Nullable
         protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, boolean flag) {
            conversationContext.setSessionData("input", flag);
            return Prompt.END_OF_CONVERSATION;
         }

         public String getPromptText(ConversationContext conversationContext) {
            return text;
         }
      };
   }
}
