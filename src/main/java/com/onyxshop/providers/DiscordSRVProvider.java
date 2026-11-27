package com.onyxshop.providers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.EmbedType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.AuthorInfo;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.Footer;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class DiscordSRVProvider {
   private final OnyxShop plugin;
   private TextChannel transactionChannel;
   public boolean enabled;
   private String raw;
   private String title;
   private String description;
   private String authorTitle;
   private String authorImage;
   private String footerTitle;
   private String footerImage;
   private String skinURL;
   private ArrayList<DiscordSRVProvider.Field> fields;
   private int color;

   public DiscordSRVProvider(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public DiscordSRVProvider(OnyxShop plugin, ConfigurationSection section) {
      this.plugin = plugin;
      this.enabled = this.init(section);
   }

   public boolean init(ConfigurationSection section) {
      TextChannel textChannel = null;
      String text = section.getString("channel");
      if (text != null && !text.isEmpty()) {
         if (!text.chars().allMatch(Character::isDigit)) {
            textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(text);
         } else {
            textChannel = DiscordUtil.getTextChannelById(text);
         }
      }

      if (textChannel != null) {
         this.transactionChannel = textChannel;
         if (section.contains("embed.color")) {
            this.color = this.getRGBColor();
         }

         if (section.contains("embed.title")) {
            this.title = section.getString("embed.title");
         }

         if (section.contains("embed.description")) {
            this.description = section.getString("embed.description");
         }

         if (section.contains("embed.author.title")) {
            this.authorTitle = section.getString("embed.author.title");
         }

         if (section.contains("embed.author.imageURL")) {
            this.authorImage = section.getString("embed.author.imageURL");
         }

         if (section.contains("embed.footer.title")) {
            this.footerTitle = section.getString("embed.footer.title");
         }

         if (section.contains("embed.footer.imageURL")) {
            this.footerImage = section.getString("embed.footer.imageURL");
         }

         ArrayList values = new ArrayList();
         ConfigurationSection section2 = ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions.embed.fields");
         if (section2 != null) {
            for (String text2 : section2.getKeys(false)) {
               ConfigurationSection section3 = ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions.embed.fields." + text2);
               values.add(new DiscordSRVProvider.Field(section3.getString("title"), section3.getString("message")));
            }
         }

         this.fields = values;
         if (section.contains("raw")) {
            this.raw = section.getString("raw");
         }

         SendMessage.infoMessage("Successfully enabled discordSRV hook, transactions will be logged to #" + this.transactionChannel);
         return true;
      } else {
         SendMessage.warnMessage(
            "Failed to hook into DiscordSRV because no game channel with name '" + section.getString("channel") + "' was found inside the DiscordSRV config."
         );
         return false;
      }
   }

   @Subscribe
   public void discordReadyEvent(DiscordReadyEvent event) {
      this.enabled = this.init(ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions"));
   }

   private int getRGBColor() {
      String text = ConfigManager.getConfig().getString("discordsrv-transactions.embed.color");
      text = text.replace("#", "");
      if (!text.startsWith("0x")) {
         text = "0x" + text;
      }

      try {
         return Color.decode(text).getRGB();
      } catch (NumberFormatException exception) {
         SendMessage.logDebugMessage(Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", "discordsrv-transactions.embed.color"));
         SendMessage.errorItemConfig("discordsrv-transactions.embed.color");
         return 255;
      }
   }

   private String translateMessage(String message, Player player, int value, String mode, String text, String text2) {
      return PlaceholderAPIHook.translate(
         player,
         message.replace("%translations-discord-srv-transaction%", Lang.DISCORD_SRV_PLAYER_TRANSACTION.get().getLegacy())
            .replace("%player_name%", player.getName())
            .replace("%player_displayname%", ChatColor.stripColor(this.plugin.getDisplayName(player)))
            .replace("%amount%", String.valueOf(value))
            .replace("%bought/sold%", mode)
            .replace("%items%", ChatUtil.stripColor(text))
            .replace("%price%", ChatColor.stripColor(text2))
      );
   }

   private List<github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.Field> getFields(
      Player player, int value, String mode, String text, String text2
   ) {
      ArrayList values = new ArrayList();
      if (this.fields == null) {
         return values;
      } else {
         for (DiscordSRVProvider.Field field : this.fields) {
            String message = this.translateMessage(field.title, player, value, mode, text, text2);
            String text3 = this.translateMessage(field.message, player, value, mode, text, text2);
            values.add(new github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.Field(message, text3, false));
         }

         return values;
      }
   }

   public void logTransaction(Player player, int value, String mode, String text, String text2) {
      if (this.raw != null && !this.raw.isEmpty()) {
         this.transactionChannel.sendMessage(this.translateMessage(this.raw, player, value, mode, text, text2)).queue();
      } else {
         this.transactionChannel
            .sendMessageEmbeds(
               new MessageEmbed(
                  null,
                  this.title != null ? this.translateMessage(this.title, player, value, mode, text, text2) : null,
                  this.description != null && !this.description.isEmpty() ? this.translateMessage(this.description, player, value, mode, text, text2) : null,
                  EmbedType.RICH,
                  null,
                  this.color,
                  null,
                  null,
                  this.authorTitle != null && !this.authorTitle.isEmpty()
                     ? new AuthorInfo(this.translateMessage(this.authorTitle, player, value, mode, text, text2), null, this.getAuthorImage(player), null)
                     : null,
                  null,
                  this.footerTitle != null && !this.footerTitle.isEmpty()
                     ? new Footer(this.translateMessage(this.footerTitle, player, value, mode, text, text2), this.getFooterImage(player), null)
                     : null,
                  null,
                  this.getFields(player, value, mode, text, text2)
               ),
               new MessageEmbed[0]
            )
            .queue();
      }
   }

   private String getFooterImage(Player player) {
      return this.footerImage.replace("%player_name%", player.getName());
   }

   private String getAuthorImage(Player player) {
      return this.authorImage.replace("%player_name%", player.getName());
   }

   private class Field {
      final String title;
      final String message;

      public Field(String text, String text2) {
         this.title = text;
         this.message = text2;
      }
   }
}
