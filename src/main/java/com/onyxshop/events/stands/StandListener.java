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

   public StandListener(OnyxShop var1, StandProvider var2) {
      this.plugin = var1;
      this.provider = var2;
   }

   @EventHandler
   public void onChunkLoad(ChunkLoadEvent var1) {
      this.provider.loadStands(new SimpleChunk(var1.getChunk()));
   }

   @EventHandler
   public void onChunkUnload(ChunkUnloadEvent var1) {
      this.provider.checkAndUnload(new SimpleChunk(var1.getChunk()));
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent var1) {
      if (StandType.isValidType(var1.getBlock().getType())) {
         Stand var2 = this.provider.getStand(new StandLoc(var1.getBlock()));
         if (var2 != null) {
            Player var3 = var1.getPlayer();
            if (!PermissionsCache.hasPermission(var3, "OnyxShop.editshop.shopstands.edit")) {
               SendMessage.chatToPlayer(var3, Lang.NO_PERMISSIONS.get());
               var1.setCancelled(true);
            } else {
               new StandManagement(this.plugin, var3, var2);
               var1.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent var1) {
      if (StandType.isValidType(var1.getBlock().getType())) {
         String var2 = this.plugin.versionHandler.getNBTString(var1.getItemInHand(), "shopstand_item");
         if (var2 != null) {
            Player var3 = var1.getPlayer();
            if (!PermissionsCache.hasPermission(var3, "OnyxShop.editshop.shopstands.create")) {
               SendMessage.chatToPlayer(var3, Lang.NO_PERMISSIONS.get());
               var1.setCancelled(true);
            } else {
               try {
                  this.provider.createStand(var1.getBlock(), var2, StandType.fromType(var1.getBlock()));
                  SendMessage.chatToPlayer(var3, Lang.CREATED_NEW_SHOP_STAND.get().replace("%item%", var2));
               } catch (StandLoadException var5) {
                  SendMessage.chatToPlayer(var3, Lang.ERROR_CREATING_NEW_SHOP_STAND.get().replace("%reason%", var5.getMessage()));
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent var1) {
      if (var1.getEntity() instanceof ArmorStand && var1.getDamager() instanceof Player) {
         ArmorStand var2 = (ArmorStand)var1.getEntity();
         Stand var3 = this.provider.getStand(var2.getUniqueId());
         if (var3 != null) {
            ShopItem var4 = this.plugin.getShopItem(var3.getItem());
            if (var4 == null) {
               SendMessage.warnMessage(Lang.CANNOT_OPEN_SHOP_STAND.get().replace("%player%", var1.getEntity().getName()).replace("%item%", var3.getItem()));
            } else {
               Player var5 = (Player)var1.getDamager();
               if (var5.isSneaking() && PermissionsCache.hasPermission(var5, "OnyxShop.editshop.shopstands.edit")) {
                  new StandManagement(this.plugin, var5, var3);
               } else if (var4.isBuyAble()) {
                  if (!this.isAllowedGamemode(var5)) {
                     return;
                  }

                  if (!PermissionsCache.hasPermission(var5, "OnyxShop.shop." + var3.getItem().split("\\.")[0])) {
                     SendMessage.chatToPlayer(var5, Lang.NO_PERMISSIONS.get());
                     var1.setCancelled(true);
                     return;
                  }

                  if (!var4.canPurchase(var5, "shop", false)) {
                     return;
                  }

                  if (!var4.meetsRequirements(var5, false)) {
                     return;
                  }

                  new TransactionMenu(var5, var4, null, true, Transaction.Mode.BUY, Transaction.Type.SHOPSTAND_BUY_SCREEN, 1).open(false);
               } else {
                  SendMessage.chatToPlayer(var5, Lang.CANNOT_PURCHASE_ITEM.get());
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent var1) {
      if (var1.getRightClicked() instanceof ArmorStand) {
         ArmorStand var2 = (ArmorStand)var1.getRightClicked();
         Stand var3 = this.provider.getStand(var2.getUniqueId());
         if (var3 != null) {
            ShopItem var4 = this.plugin.getShopItem(var3.getItem());
            if (var4 == null) {
               SendMessage.warnMessage(Lang.CANNOT_OPEN_SHOP_STAND.get().replace("%player%", var1.getPlayer().getName()).replace("%item%", var3.getItem()));
            } else {
               Player var5 = var1.getPlayer();
               if (this.isAllowedGamemode(var5)) {
                  if (!PermissionsCache.hasPermission(var5, "OnyxShop.shop." + var3.getItem().split("\\.")[0])) {
                     SendMessage.chatToPlayer(var5, Lang.NO_PERMISSIONS.get());
                     var1.setCancelled(true);
                     return;
                  }

                  if (!var4.isSellAble()) {
                     SendMessage.chatToPlayer(var5, Lang.CANNOT_SELL_ITEM.get());
                  } else if (var4.canPurchase(var5, "shop", false)) {
                     if (var4.meetsRequirements(var5, false)) {
                        new TransactionMenu(var5, var4, null, true, Transaction.Mode.SELL, Transaction.Type.SHOPSTAND_SELL_SCREEN, 1).open(false);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isAllowedGamemode(Player var1) {
      if (this.plugin.bannedGamemodes.contains(var1.getGameMode()) && !PermissionsCache.hasPermission(var1, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(var1, Lang.CANNOT_ENTER_SHOP_BANNED_GAMEMODE.get().replace("%gamemode%", var1.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }
}
