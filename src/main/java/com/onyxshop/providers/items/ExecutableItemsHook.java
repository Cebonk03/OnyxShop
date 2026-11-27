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

   public ExecutableItemsHook(OnyxShop plugin) {
      this.plugin = plugin;
      if (Bukkit.getPluginManager().getPlugin("ExecutableItems").isEnabled()) {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "ExecutableItems"));
         this.isReady = true;
         this.plugin.getItemManager().check();
      } else {
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
   }

   @EventHandler
   public void onItemsLoad(ExecutableItemsPostLoadEvent event) {
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
   public ItemStack getItem(String itemId) throws ItemLoadException {
      Optional optional = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(itemId);
      if (optional.isPresent()) {
         ItemStack itemStack = ((ExecutableItemInterface)optional.get()).buildItem(1, Optional.empty(), Optional.empty());
         if (itemStack == null) {
            throw new ItemLoadException(Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", "ExecutableItems"));
         } else {
            return itemStack;
         }
      } else {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", "ExecutableItems"));
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      Optional optional = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(itemStack);
      return !optional.isPresent() ? null : ((ExecutableItemInterface)optional.get()).getId();
   }

   public static ItemStack tag(ItemStack itemStack, Player player) {
      Optional optional = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(itemStack);
      if (optional.isPresent()) {
         return ((ExecutableItemInterface)optional.get()).addExecutableItemInfos(itemStack, Optional.of(player));
      } else {
         AddItemInPlayerInventoryEvent addItemInPlayerInventoryEvent = new AddItemInPlayerInventoryEvent(player, itemStack, player.getInventory().firstEmpty());
         Bukkit.getPluginManager().callEvent(addItemInPlayerInventoryEvent);
         return itemStack;
      }
   }
}
