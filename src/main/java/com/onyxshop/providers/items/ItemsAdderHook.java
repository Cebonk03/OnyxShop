package com.onyxshop.providers.items;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderFirstLoadEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent.Cause;
import java.lang.reflect.Field;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.ITEMS_ADDER);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public ItemsAdderHook(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onIALoad(ItemsAdderFirstLoadEvent event) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "ItemsAdder"));
      this.isReady = true;
      this.plugin.getItemManager().check();
      ItemsAdderFirstLoadEvent.getHandlerList().unregister(this);
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   private Cause getCause(ItemsAdderLoadDataEvent event) {
      try {
         Field field = event.getClass().getDeclaredField("cause");
         field.setAccessible(true);
         return (Cause)field.get(event);
      } catch (SecurityException | IllegalAccessException | NoSuchFieldException exception) {
         SendMessage.warnMessage("Failed to subscribe ItemsAdder load event");
         exception.printStackTrace();
         return Cause.FIRST_LOAD;
      }
   }

   @Override
   public ItemStack getItem(String itemId) throws ItemLoadException {
      CustomStack customStack = CustomStack.getInstance(itemId);
      if (customStack == null) {
         throw new ItemLoadException(Lang.INVALID_ITEMSADDER_ITEM.get().replace("%id%", itemId));
      } else {
         return customStack.getItemStack().clone();
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      CustomStack customStack = CustomStack.byItemStack(itemStack);
      return customStack == null ? null : customStack.getNamespacedID();
   }

   @Override
   public String getID(String text) {
      return text.contains(":") ? text.split(":")[1] : text;
   }
}
