package com.onyxshop.objects.inventorys;

import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

public class UpdateMessage {
   private ConversationFactory factory;
   private Conversation conv;
   private final Player player;
   private final Consumer<String> onComplete;

   public UpdateMessage(Player var1, Consumer<String> var2) {
      this.onComplete = var2;
      this.player = var1;
      this.startConversation(var1);
   }

   private void startConversation(Player var1) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      this.conv = this.factory
         .withFirstPrompt(new StringPrompt() {
            public String getPromptText(ConversationContext var1) {
               UpdateMessage.this.player.sendTitle("§bEnter update message", "", 5, 80, 5);
               return "§aEnter a update message of the changes made in this update, maximum 128 characters.";
            }

            public Prompt acceptInput(ConversationContext var1, String var2) {
               var1.setSessionData("result", var2);
               return Prompt.END_OF_CONVERSATION;
            }
         })
         .addConversationAbandonedListener(new UpdateMessage.ConvAbandon())
         .withEscapeSequence("exit")
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(new UpdateMessage.ConvPrefix())
         .buildConversation(var1);
      this.conv.begin();
   }

   private final class ConvAbandon implements ConversationAbandonedListener {
      private ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent var1) {
         Player var2 = (Player)var1.getContext().getForWhom();
         if (var1.gracefulExit()) {
            String var3 = (String)var1.getContext().getSessionData("result");
            if (var3.length() < 16) {
               SendMessage.chatToPlayer(var2, "§cMessage must be longer as 16 characters, please write a descriptive explanation of your update");
               UpdateMessage.this.startConversation(var2);
               return;
            }

            if (var3.length() > 128) {
               SendMessage.chatToPlayer(var2, "§cMessage cannot be longer as 128 characters, please write a descriptive explanation of your update");
               UpdateMessage.this.startConversation(var2);
               return;
            }

            UpdateMessage.this.onComplete.accept(var3);
         } else {
            SendMessage.chatToPlayer(var2, Lang.EXITED_CONVERSATION.get());
         }
      }
   }

   private final class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext var1) {
         return Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ";
      }
   }
}
