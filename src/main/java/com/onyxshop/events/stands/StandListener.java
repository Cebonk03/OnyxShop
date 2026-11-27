package com.onyxshop.events.stands;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.inventorys.StandManagement;
import com.onyxshop.objects.stands.Stand;
import com.onyxshop.objects.stands.StandLoc;
import com.onyxshop.objects.stands.StandType;
import com.onyxshop.providers.StandProvider;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.SimpleChunk;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.exceptions.StandLoadException;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class StandListener implements Listener {
   private final OnyxShop plugin;
   private final StandProvider provider;

   public StandListener(OnyxShop plugin, StandProvider standProvider) {
      this.plugin = plugin;
      this.provider = standProvider;
   }

   @EventHandler
   public void onChunkLoad(ChunkLoadEvent event) {
      this.provider.loadStands(new SimpleChunk(event.getChunk()));
   }

   @EventHandler
   public void onChunkUnload(ChunkUnloadEvent event) {
      this.provider.checkAndUnload(new SimpleChunk(event.getChunk()));
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (StandType.isValidType(event.getBlock().getType())) {
         Stand stand = this.provider.getStand(new StandLoc(event.getBlock()));
         if (stand != null) {
            Player player = event.getPlayer();
            if (!PermissionsCache.hasPermission(player, "OnyxShop.editshop.shopstands.edit")) {
               SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
               event.setCancelled(true);
            } else {
               new StandManagement(this.plugin, player, stand);
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      if (StandType.isValidType(event.getBlock().getType())) {
         String nbtString = this.plugin.versionHandler.getNBTString(event.getItemInHand(), "shopstand_item");
         if (nbtString != null) {
            Player player = event.getPlayer();
            if (!PermissionsCache.hasPermission(player, "OnyxShop.editshop.shopstands.create")) {
               SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
               event.setCancelled(true);
            } else {
               try {
                  this.provider.createStand(event.getBlock(), nbtString, StandType.fromType(event.getBlock()));
                  SendMessage.chatToPlayer(player, Lang.CREATED_NEW_SHOP_STAND.get().replace("%item%", nbtString));
               } catch (StandLoadException exception) {
                  SendMessage.chatToPlayer(player, Lang.ERROR_CREATING_NEW_SHOP_STAND.get().replace("%reason%", exception.getMessage()));
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof ArmorStand && event.getDamager() instanceof Player) {
         ArmorStand armorStand = (ArmorStand)event.getEntity();
         Stand stand = this.provider.getStand(armorStand.getUniqueId());
         if (stand != null) {
            ShopItem shopItem = this.plugin.getShopItem(stand.getItem());
            if (shopItem == null) {
               SendMessage.warnMessage(Lang.CANNOT_OPEN_SHOP_STAND.get().replace("%player%", event.getEntity().getName()).replace("%item%", stand.getItem()));
            } else {
               Player player = (Player)event.getDamager();
               if (player.isSneaking() && PermissionsCache.hasPermission(player, "OnyxShop.editshop.shopstands.edit")) {
                  new StandManagement(this.plugin, player, stand);
               } else if (shopItem.isBuyAble()) {
                  if (!this.isAllowedGamemode(player)) {
                     return;
                  }

                  if (!PermissionsCache.hasPermission(player, "OnyxShop.shop." + stand.getItem().split("\\.")[0])) {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
                     event.setCancelled(true);
                     return;
                  }

                  if (!shopItem.canPurchase(player, "shop", false)) {
                     return;
                  }

                  if (!shopItem.meetsRequirements(player, false)) {
                     return;
                  }

                  new TransactionMenu(player, shopItem, null, true, Transaction.Mode.BUY, Transaction.Type.SHOPSTAND_BUY_SCREEN, 1).open(false);
               } else {
                  SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
      if (event.getRightClicked() instanceof ArmorStand) {
         ArmorStand armorStand = (ArmorStand)event.getRightClicked();
         Stand stand = this.provider.getStand(armorStand.getUniqueId());
         if (stand != null) {
            ShopItem shopItem = this.plugin.getShopItem(stand.getItem());
            if (shopItem == null) {
               SendMessage.warnMessage(Lang.CANNOT_OPEN_SHOP_STAND.get().replace("%player%", event.getPlayer().getName()).replace("%item%", stand.getItem()));
            } else {
               Player player = event.getPlayer();
               if (this.isAllowedGamemode(player)) {
                  if (!PermissionsCache.hasPermission(player, "OnyxShop.shop." + stand.getItem().split("\\.")[0])) {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
                     event.setCancelled(true);
                     return;
                  }

                  if (!shopItem.isSellAble()) {
                     SendMessage.chatToPlayer(player, Lang.CANNOT_SELL_ITEM.get());
                  } else if (shopItem.canPurchase(player, "shop", false)) {
                     if (shopItem.meetsRequirements(player, false)) {
                        new TransactionMenu(player, shopItem, null, true, Transaction.Mode.SELL, Transaction.Type.SHOPSTAND_SELL_SCREEN, 1).open(false);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isAllowedGamemode(Player player) {
      if (this.plugin.bannedGamemodes.contains(player.getGameMode()) && !PermissionsCache.hasPermission(player, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(player, Lang.CANNOT_ENTER_SHOP_BANNED_GAMEMODE.get().replace("%gamemode%", player.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }
}
