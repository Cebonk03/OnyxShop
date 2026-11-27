package com.onyxshop.versions.v1_16_R3;

import com.onyxshop.VersionHandler;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.Container;
import net.minecraft.server.v1_16_R3.ContainerAccess;
import net.minecraft.server.v1_16_R3.ContainerAnvil;
import net.minecraft.server.v1_16_R3.Containers;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PacketPlayOutCloseWindow;
import net.minecraft.server.v1_16_R3.PacketPlayOutOpenWindow;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.event.CraftEventFactory;
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
      CraftEventFactory.handleInventoryCloseEvent(this.toNMS(player));
   }

   @Override
   public void sendPacketOpenWindow(Player player, int value, String playerName) {
      this.toNMS(player).playerConnection.sendPacket(new PacketPlayOutOpenWindow(value, Containers.ANVIL, new ChatMessage(playerName)));
   }

   @Override
   public void sendPacketCloseWindow(Player player, int value) {
      this.toNMS(player).playerConnection.sendPacket(new PacketPlayOutCloseWindow(value));
   }

   @Override
   public void setActiveContainerDefault(Player player) {
      this.toNMS(player).activeContainer = this.toNMS(player).defaultContainer;
   }

   @Override
   public void setActiveContainer(Player player, Object value) {
      this.toNMS(player).activeContainer = (Container)value;
   }

   @Override
   public void setActiveContainerId(Object value, int id) {
   }

   @Override
   public void addActiveContainerSlotListener(Object value, Player player) {
      ((Container)value).addSlotListener(this.toNMS(player));
   }

   private EntityPlayer toNMS(Player player) {
      return ((CraftPlayer)player).getHandle();
   }

   private class AnvilContainer extends ContainerAnvil implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player player, String playerName) {
         super(
            GUIAnvil.this.getRealNextContainerId(player),
            ((CraftPlayer)player).getHandle().inventory,
            ContainerAccess.at(((CraftWorld)player.getWorld()).getHandle(), new BlockPosition(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(new ChatMessage(playerName));
      }

      public void e() {
         super.e();
         this.levelCost.set(0);
      }

      public int getContainerId() {
         return this.windowId;
      }

      @Override
      public Inventory getBukkitInventory() {
         return this.getBukkitView().getTopInventory();
      }
   }
}
