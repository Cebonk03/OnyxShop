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

   public UpdateMessage(Player player, Consumer<String> task) {
      this.onComplete = task;
      this.player = player;
      this.startConversation(player);
   }

   private void startConversation(Player player) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      this.conv = this.factory
         .withFirstPrompt(new StringPrompt() {
            public String getPromptText(ConversationContext conversationContext) {
               UpdateMessage.this.player.sendTitle("§bEnter update message", "", 5, 80, 5);
               return "§aEnter a update message of the changes made in this update, maximum 128 characters.";
            }

            public Prompt acceptInput(ConversationContext conversationContext, String text) {
               conversationContext.setSessionData("result", text);
               return Prompt.END_OF_CONVERSATION;
            }
         })
         .addConversationAbandonedListener(new UpdateMessage.ConvAbandon())
         .withEscapeSequence("exit")
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(new UpdateMessage.ConvPrefix())
         .buildConversation(player);
      this.conv.begin();
   }

   private final class ConvAbandon implements ConversationAbandonedListener {
      private ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent event) {
         Player player = (Player)event.getContext().getForWhom();
         if (event.gracefulExit()) {
            String text = (String)event.getContext().getSessionData("result");
            if (text.length() < 16) {
               SendMessage.chatToPlayer(player, "§cMessage must be longer as 16 characters, please write a descriptive explanation of your update");
               UpdateMessage.this.startConversation(player);
               return;
            }

            if (text.length() > 128) {
               SendMessage.chatToPlayer(player, "§cMessage cannot be longer as 128 characters, please write a descriptive explanation of your update");
               UpdateMessage.this.startConversation(player);
               return;
            }

            UpdateMessage.this.onComplete.accept(text);
         } else {
            SendMessage.chatToPlayer(player, Lang.EXITED_CONVERSATION.get());
         }
      }
   }

   private final class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext conversationContext) {
         return Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ";
      }
   }
}
