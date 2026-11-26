package com.onyxshop.providers.items;

import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import com.ssomar.score.api.executableitems.events.AddItemInPlayerInventoryEvent;
import com.ssomar.score.api.executableitems.load.ExecutableItemsPostLoadEvent;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ExecutableItemsHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.EXECUTEABLE_ITEMS);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public ExecutableItemsHook(OnyxShop var1) {
      this.plugin = var1;
      if (Bukkit.getPluginManager().getPlugin("ExecutableItems").isEnabled()) {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "ExecutableItems"));
         this.isReady = true;
         this.plugin.getItemManager().check();
      } else {
         var1.getServer().getPluginManager().registerEvents(this, var1);
      }
   }

   @EventHandler
   public void onItemsLoad(ExecutableItemsPostLoadEvent var1) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "ExecutableItems"));
      this.isReady = true;
      this.plugin.getItemManager().check();
      ExecutableItemsPostLoadEvent.getHandlerList().unregister(this);
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   @Override
   public ItemStack getItem(String var1) throws ItemLoadException {
      Optional var2 = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(var1);
      if (var2.isPresent()) {
         ItemStack var3 = ((ExecutableItemInterface)var2.get()).buildItem(1, Optional.empty(), Optional.empty());
         if (var3 == null) {
            throw new ItemLoadException(Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", "ExecutableItems"));
         } else {
            return var3;
         }
      } else {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", "ExecutableItems"));
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      Optional var2 = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(var1);
      return !var2.isPresent() ? null : ((ExecutableItemInterface)var2.get()).getId();
   }

   public static ItemStack tag(ItemStack var0, Player var1) {
      Optional var2 = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(var0);
      if (var2.isPresent()) {
         return ((ExecutableItemInterface)var2.get()).addExecutableItemInfos(var0, Optional.of(var1));
      } else {
         AddItemInPlayerInventoryEvent var3 = new AddItemInPlayerInventoryEvent(var1, var0, var1.getInventory().firstEmpty());
         Bukkit.getPluginManager().callEvent(var3);
         return var0;
      }
   }
}
