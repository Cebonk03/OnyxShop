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

   public SlimefunHook(OnyxShop var1) {
      this.plugin = var1;
      var1.getServer().getPluginManager().registerEvents(this, var1);
   }

   @EventHandler
   public void onItemsLoad(SlimefunItemRegistryFinalizedEvent var1) {
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
   public ItemStack getItem(String var1) {
      SlimefunItem var2 = SlimefunItem.getById(var1);
      return var2 != null ? var2.getItem().clone() : null;
   }

   @Override
   public String getItem(ItemStack var1) {
      SlimefunItem var2 = SlimefunItem.getByItem(var1);
      return var2 != null ? var2.getId() : null;
   }
}
