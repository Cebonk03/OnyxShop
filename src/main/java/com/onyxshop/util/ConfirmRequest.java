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
   public ConfirmRequest(OnyxShop var1, String var2, Player var3, Consumer<Boolean> var4) {
      ConversationFactory var5 = new ConversationFactory(var1);
      Conversation var6 = var5.withFirstPrompt(this.getPrompt(var2, var4))
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(var0 -> Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ")
         .addConversationAbandonedListener(var1x -> {
            if (var1x.gracefulExit()) {
               var4.accept((Boolean)var1x.getContext().getSessionData("input"));
            } else {
               var4.accept(false);
            }
         })
         .buildConversation(var3);
      var6.begin();
   }

   private Prompt getPrompt(final String var1, Consumer<Boolean> var2) {
      return new BooleanPrompt() {
         @Nullable
         protected Prompt acceptValidatedInput(@NotNull ConversationContext var1x, boolean var2x) {
            var1x.setSessionData("input", var2x);
            return Prompt.END_OF_CONVERSATION;
         }

         public String getPromptText(ConversationContext var1x) {
            return var1;
         }
      };
   }
}
