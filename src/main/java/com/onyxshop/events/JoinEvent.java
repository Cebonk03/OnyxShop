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

   public JoinEvent(OnyxShop var1, boolean var2) {
      this.plugin = var1;
      this.joinMessage = var2;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent var1) {
      Player var2 = var1.getPlayer();
      if (this.plugin.updateChecker != null && PermissionsCache.hasPermission(var2, "OnyxShop.notifyUpdate")) {
         this.checkForUpdates(var2);
      }

      if (this.joinMessage) {
         this.sendJoinMessage(var2);
      }

      SkullUtil.updateTexture(var2);
      this.toLoad.add(var2.getUniqueId());
      this.plugin.runTaskLater(() -> {
         if (this.toLoad.contains(var2.getUniqueId())) {
            UserManager.getUser(var2);
         }
      }, 5L);
      this.plugin.runTaskLaterAsync(() -> {
         if (this.plugin.getModifierManager() != null) {
            this.plugin.getModifierManager().loadCommandModifiersForPlayer(var2);
         }
      }, 5L);
   }

   @EventHandler
   public void onPlayerLeave(PlayerQuitEvent var1) {
      UUID var2 = var1.getPlayer().getUniqueId();
      this.toLoad.remove(var2);
      UserManager.invalidate(var2);
      this.plugin.runTaskAsync(() -> {
         if (this.plugin.getModifierManager() != null) {
            this.plugin.getModifierManager().unloadAndSaveCommandModifiers(var2);
         }
      });
   }

   private void checkForUpdates(Player var1) {
      if (this.plugin.updateChecker.updateAvailable) {
         this.plugin
            .runTaskLater(
               () -> {
                  SendMessage.infoMessage(
                     var1,
                     Lang.UPDATE_AVAILABLE
                        .get(var1)
                        .replace("%plugin_version%", this.plugin.updateChecker.CURRENT_VERSION.getVer())
                        .replace("%latest_version%", this.plugin.updateChecker.LATEST_VERSION.getVer())
                  );
                  SendMessage.infoMessage(
                     var1,
                     "An update is available: v" + this.plugin.updateChecker.LATEST_VERSION.getVer()
                  );
               },
               5L
            );
      }
   }

   private void sendJoinMessage(Player var1) {
      OfflinePlayer var2 = this.plugin.getServer().getOfflinePlayer(var1.getUniqueId());
      if (var2.hasPlayedBefore()) {
         SendMessage.chatToPlayer(
            var1,
            Lang.JOIN_MESSAGE
               .get(var1)
               .replace("%balance%", this.plugin.formatPrice((EcoType)null, this.plugin.getEcoHandler().getDefaultProvider().getBalance(var1)))
         );
      }
   }
}
