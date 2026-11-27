package com.onyxshop.providers.items;

import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class CraftEngineHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.CRAFT_ENGINE);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public CraftEngineHook(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onItemsLoad(CraftEngineReloadEvent event) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
      CraftEngineReloadEvent.getHandlerList().unregister(this);
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
      if (!this.isReady) {
         return null;
      } else {
         try {
            BukkitItemDefinition bukkitItemDefinition = CraftEngineItems.byId(Key.of(itemId));
            if (bukkitItemDefinition == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", "CraftEngine"));
            } else {
               return bukkitItemDefinition.buildBukkitItem();
            }
         } catch (Exception exception) {
            String text;
            if (!(exception instanceof ItemLoadException)) {
               text = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", itemId).replace("%plugin%", "CraftEngine");
               exception.printStackTrace();
            } else {
               text = exception.getMessage();
            }

            throw new ItemLoadException("Failed to load CraftEngine item with reason: " + text);
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return Optional.ofNullable(CraftEngineItems.getCustomItemId(itemStack)).<String>map(Key::asString).orElse(null);
   }

   public static ItemStack tag(ItemStack itemStack, Player player) {
      return CraftEngineItems.byItemStack(itemStack).buildBukkitItem(ItemBuildContext.of(BukkitAdaptor.adapt(player)), itemStack.getAmount());
   }
}
