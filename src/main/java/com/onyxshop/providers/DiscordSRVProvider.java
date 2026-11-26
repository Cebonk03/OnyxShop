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

   public DiscordSRVProvider(OnyxShop var1) {
      this.plugin = var1;
   }

   public DiscordSRVProvider(OnyxShop var1, ConfigurationSection var2) {
      this.plugin = var1;
      this.enabled = this.init(var2);
   }

   public boolean init(ConfigurationSection var1) {
      TextChannel var2 = null;
      String var3 = var1.getString("channel");
      if (var3 != null && !var3.isEmpty()) {
         if (!var3.chars().allMatch(Character::isDigit)) {
            var2 = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(var3);
         } else {
            var2 = DiscordUtil.getTextChannelById(var3);
         }
      }

      if (var2 != null) {
         this.transactionChannel = var2;
         if (var1.contains("embed.color")) {
            this.color = this.getRGBColor();
         }

         if (var1.contains("embed.title")) {
            this.title = var1.getString("embed.title");
         }

         if (var1.contains("embed.description")) {
            this.description = var1.getString("embed.description");
         }

         if (var1.contains("embed.author.title")) {
            this.authorTitle = var1.getString("embed.author.title");
         }

         if (var1.contains("embed.author.imageURL")) {
            this.authorImage = var1.getString("embed.author.imageURL");
         }

         if (var1.contains("embed.footer.title")) {
            this.footerTitle = var1.getString("embed.footer.title");
         }

         if (var1.contains("embed.footer.imageURL")) {
            this.footerImage = var1.getString("embed.footer.imageURL");
         }

         ArrayList var4 = new ArrayList();
         ConfigurationSection var5 = ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions.embed.fields");
         if (var5 != null) {
            for (String var7 : var5.getKeys(false)) {
               ConfigurationSection var8 = ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions.embed.fields." + var7);
               var4.add(new DiscordSRVProvider.Field(var8.getString("title"), var8.getString("message")));
            }
         }

         this.fields = var4;
         if (var1.contains("raw")) {
            this.raw = var1.getString("raw");
         }

         SendMessage.infoMessage("Successfully enabled discordSRV hook, transactions will be logged to #" + this.transactionChannel);
         return true;
      } else {
         SendMessage.warnMessage(
            "Failed to hook into DiscordSRV because no game channel with name '" + var1.getString("channel") + "' was found inside the DiscordSRV config."
         );
         return false;
      }
   }

   @Subscribe
   public void discordReadyEvent(DiscordReadyEvent var1) {
      this.enabled = this.init(ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions"));
   }

   private int getRGBColor() {
      String var1 = ConfigManager.getConfig().getString("discordsrv-transactions.embed.color");
      var1 = var1.replace("#", "");
      if (!var1.startsWith("0x")) {
         var1 = "0x" + var1;
      }

      try {
         return Color.decode(var1).getRGB();
      } catch (NumberFormatException var3) {
         SendMessage.logDebugMessage(Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", "discordsrv-transactions.embed.color"));
         SendMessage.errorItemConfig("discordsrv-transactions.embed.color");
         return 255;
      }
   }

   private String translateMessage(String var1, Player var2, int var3, String var4, String var5, String var6) {
      return PlaceholderAPIHook.translate(
         var2,
         var1.replace("%translations-discord-srv-transaction%", Lang.DISCORD_SRV_PLAYER_TRANSACTION.get().getLegacy())
            .replace("%player_name%", var2.getName())
            .replace("%player_displayname%", ChatColor.stripColor(this.plugin.getDisplayName(var2)))
            .replace("%amount%", String.valueOf(var3))
            .replace("%bought/sold%", var4)
            .replace("%items%", ChatUtil.stripColor(var5))
            .replace("%price%", ChatColor.stripColor(var6))
      );
   }

   private List<github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.Field> getFields(
      Player var1, int var2, String var3, String var4, String var5
   ) {
      ArrayList var6 = new ArrayList();
      if (this.fields == null) {
         return var6;
      } else {
         for (DiscordSRVProvider.Field var8 : this.fields) {
            String var9 = this.translateMessage(var8.title, var1, var2, var3, var4, var5);
            String var10 = this.translateMessage(var8.message, var1, var2, var3, var4, var5);
            var6.add(new github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed.Field(var9, var10, false));
         }

         return var6;
      }
   }

   public void logTransaction(Player var1, int var2, String var3, String var4, String var5) {
      if (this.raw != null && !this.raw.isEmpty()) {
         this.transactionChannel.sendMessage(this.translateMessage(this.raw, var1, var2, var3, var4, var5)).queue();
      } else {
         this.transactionChannel
            .sendMessageEmbeds(
               new MessageEmbed(
                  null,
                  this.title != null ? this.translateMessage(this.title, var1, var2, var3, var4, var5) : null,
                  this.description != null && !this.description.isEmpty() ? this.translateMessage(this.description, var1, var2, var3, var4, var5) : null,
                  EmbedType.RICH,
                  null,
                  this.color,
                  null,
                  null,
                  this.authorTitle != null && !this.authorTitle.isEmpty()
                     ? new AuthorInfo(this.translateMessage(this.authorTitle, var1, var2, var3, var4, var5), null, this.getAuthorImage(var1), null)
                     : null,
                  null,
                  this.footerTitle != null && !this.footerTitle.isEmpty()
                     ? new Footer(this.translateMessage(this.footerTitle, var1, var2, var3, var4, var5), this.getFooterImage(var1), null)
                     : null,
                  null,
                  this.getFields(var1, var2, var3, var4, var5)
               ),
               new MessageEmbed[0]
            )
            .queue();
      }
   }

   private String getFooterImage(Player var1) {
      return this.footerImage.replace("%player_name%", var1.getName());
   }

   private String getAuthorImage(Player var1) {
      return this.authorImage.replace("%player_name%", var1.getName());
   }

   private class Field {
      final String title;
      final String message;

      public Field(String nullx, String nullxx) {
         this.title = nullx;
         this.message = nullxx;
      }
   }
}
