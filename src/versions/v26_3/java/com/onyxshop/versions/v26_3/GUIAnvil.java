package com.onyxshop.versions.v26_3;

import com.onyxshop.VersionHandler;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ReflectUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GUIAnvil implements VersionHandler.AnvilGUI {
   @Override
   public VersionHandler.AnvilGUI.AnvilContainer newAnvilContainer(Player player, String playerName) {
      return new GUIAnvil.AnvilContainer(player, playerName);
   }

   private int getRealNextContainerId(Player player) {
      return this.toNMS(player).nextContainerCounter();
   }

   @Override
   public int getNextContainerId(Player player, Object value) {
      return ((GUIAnvil.AnvilContainer)value).getContainerId();
   }

   @Override
   public void handleInventoryCloseEvent(Player player) {
      try {
         CraftEventFactory.handleInventoryCloseEvent(this.toNMS(player));
      } catch (NoSuchMethodError noSuchMethodError) {
         try {
            Class type = Class.forName("org.bukkit.event.inventory.InventoryCloseEvent$Reason");
            ReflectUtil.invokeStaticLegacyMethod(
               "handleInventoryCloseEvent",
               CraftEventFactory.class,
               new Class[]{net.minecraft.world.entity.player.Player.class, type},
               this.toNMS(player),
               type.getField("PLUGIN").get(null)
            );
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to open anvil GUI for player");
            throw new RuntimeException(exception);
         }
      }
   }

   @Override
   public void sendPacketOpenWindow(Player player, int value, String playerName) {
      this.toNMS(player).connection.send(new ClientboundOpenScreenPacket(value, MenuType.ANVIL, Component.literal(playerName)));
   }

   @Override
   public void sendPacketCloseWindow(Player player, int value) {
      this.toNMS(player).connection.send(new ClientboundContainerClosePacket(value));
   }

   @Override
   public void setActiveContainerDefault(Player player) {
      this.toNMS(player).containerMenu = this.toNMS(player).inventoryMenu;
   }

   @Override
   public void setActiveContainer(Player player, Object value) {
      this.toNMS(player).containerMenu = (AbstractContainerMenu)value;
   }

   @Override
   public void setActiveContainerId(Object value, int id) {
   }

   @Override
   public void addActiveContainerSlotListener(Object value, Player player) {
      this.toNMS(player).initMenu((AbstractContainerMenu)value);
   }

   private ServerPlayer toNMS(Player player) {
      return ((CraftPlayer)player).getHandle();
   }

   private class AnvilContainer extends AnvilMenu implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player player, String playerName) {
         super(
            GUIAnvil.this.getRealNextContainerId(player),
            GUIAnvil.this.toNMS(player).getInventory(),
            ContainerLevelAccess.create(((CraftWorld)player.getWorld()).getHandle(), new BlockPos(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(Component.literal(playerName));
      }

      public void createResult() {
         super.createResult();
         this.cost.set(0);
      }

      public int getContainerId() {
         return this.containerId;
      }

      @Override
      public Inventory getBukkitInventory() {
         return this.getBukkitView().getTopInventory();
      }
   }
}
