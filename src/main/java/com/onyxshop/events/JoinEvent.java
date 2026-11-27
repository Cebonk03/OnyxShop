package com.onyxshop.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.SkullUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinEvent implements Listener {
   private final OnyxShop plugin;
   private final boolean joinMessage;
   private final Set<UUID> toLoad = new HashSet<>();

   public JoinEvent(OnyxShop plugin, boolean flag) {
      this.plugin = plugin;
      this.joinMessage = flag;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.updateChecker != null && PermissionsCache.hasPermission(player, "OnyxShop.notifyUpdate")) {
         this.checkForUpdates(player);
      }

      if (this.joinMessage) {
         this.sendJoinMessage(player);
      }

      SkullUtil.updateTexture(player);
      this.toLoad.add(player.getUniqueId());
      this.plugin.runTaskLater(() -> {
         if (this.toLoad.contains(player.getUniqueId())) {
            UserManager.getUser(player);
         }
      }, 5L);
      this.plugin.runTaskLaterAsync(() -> {
         if (this.plugin.getModifierManager() != null) {
            this.plugin.getModifierManager().loadCommandModifiersForPlayer(player);
         }
      }, 5L);
   }

   @EventHandler
   public void onPlayerLeave(PlayerQuitEvent event) {
      UUID uniqueId = event.getPlayer().getUniqueId();
      this.toLoad.remove(uniqueId);
      UserManager.invalidate(uniqueId);
      this.plugin.runTaskAsync(() -> {
         if (this.plugin.getModifierManager() != null) {
            this.plugin.getModifierManager().unloadAndSaveCommandModifiers(uniqueId);
         }
      });
   }

   private void checkForUpdates(Player player) {
      if (this.plugin.updateChecker.updateAvailable) {
         this.plugin
            .runTaskLater(
               () -> {
                  SendMessage.infoMessage(
                     player,
                     Lang.UPDATE_AVAILABLE
                        .get(player)
                        .replace("%plugin_version%", this.plugin.updateChecker.CURRENT_VERSION.getVer())
                        .replace("%latest_version%", this.plugin.updateChecker.LATEST_VERSION.getVer())
                  );
                  SendMessage.infoMessage(
                     player,
                     "An update is available: v" + this.plugin.updateChecker.LATEST_VERSION.getVer()
                  );
               },
               5L
            );
      }
   }

   private void sendJoinMessage(Player player) {
      OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(player.getUniqueId());
      if (offlinePlayer.hasPlayedBefore()) {
         SendMessage.chatToPlayer(
            player,
            Lang.JOIN_MESSAGE
               .get(player)
               .replace("%balance%", this.plugin.formatPrice((EcoType)null, this.plugin.getEcoHandler().getDefaultProvider().getBalance(player)))
         );
      }
   }
}
