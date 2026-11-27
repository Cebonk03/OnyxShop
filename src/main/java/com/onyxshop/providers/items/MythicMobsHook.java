package com.onyxshop.providers.items;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractPlayer;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.drops.DropMetadataImpl;
import io.lumine.mythic.core.items.MythicItem;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MythicMobsHook implements ItemProvider {
   private static final ItemType TYPE = new ItemType(ItemProviderType.MYTHIC_MOBS);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public MythicMobsHook(OnyxShop plugin) {
      this.plugin = plugin;
      if (plugin.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
         this.enable();
      } else {
         plugin.runTaskLater(() -> {
            if (!plugin.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getType().getName()));
            } else {
               this.enable();
            }
         }, 20L);
      }
   }

   private void enable() {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
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
      Optional optional = MythicProvider.get().getItemManager().getItem(itemId);
      if (!optional.isPresent()) {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", this.getType().getName()));
      } else {
         return BukkitAdapter.adapt(((MythicItem)optional.get()).generateItemStack(1));
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return MythicProvider.get().getItemManager().getMythicTypeFromItem(itemStack);
   }

   public static ItemStack tag(ItemStack itemStack, Player player) {
      String mythicTypeFromItem = MythicProvider.get().getItemManager().getMythicTypeFromItem(itemStack);
      Optional optional = MythicProvider.get().getItemManager().getItem(mythicTypeFromItem);
      if (!optional.isPresent()) {
         throw new NullPointerException(
            Lang.UNKNOWN_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", mythicTypeFromItem).replace("%plugin%", ItemProviderType.MYTHIC_MOBS.getName())
         );
      } else {
         AbstractPlayer abstractPlayer = BukkitAdapter.adapt(player);
         SkillCaster skillCaster = MythicProvider.get().getSkillManager().getCaster(abstractPlayer);
         DropMetadataImpl dropMetadataImpl = new DropMetadataImpl(skillCaster, abstractPlayer);
         return BukkitAdapter.adapt(((MythicItem)optional.get()).generateItemStack(dropMetadataImpl, itemStack.getAmount()));
      }
   }
}
