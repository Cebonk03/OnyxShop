package com.onyxshop.providers.items;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class SlimefunHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.EXECUTEABLE_ITEMS);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public SlimefunHook(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onItemsLoad(SlimefunItemRegistryFinalizedEvent event) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
      SlimefunItemRegistryFinalizedEvent.getHandlerList().unregister(this);
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
   public ItemStack getItem(String itemId) {
      SlimefunItem slimefunItem = SlimefunItem.getById(itemId);
      return slimefunItem != null ? slimefunItem.getItem().clone() : null;
   }

   @Override
   public String getItem(ItemStack itemStack) {
      SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
      return slimefunItem != null ? slimefunItem.getId() : null;
   }
}
