package com.onyxshop.versions.v1_17_R1;

import com.onyxshop.VersionHandler;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.Containers;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.event.CraftEventFactory;
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
      this.toNMS(player).b.sendPacket(new PacketPlayOutOpenWindow(value, Containers.h, new ChatMessage(playerName)));
   }

   @Override
   public void sendPacketCloseWindow(Player player, int value) {
      this.toNMS(player).b.sendPacket(new PacketPlayOutCloseWindow(value));
   }

   @Override
   public void setActiveContainerDefault(Player player) {
      this.toNMS(player).bV = this.toNMS(player).bU;
   }

   @Override
   public void setActiveContainer(Player player, Object value) {
      this.toNMS(player).bV = (Container)value;
   }

   @Override
   public void setActiveContainerId(Object value, int id) {
   }

   @Override
   public void addActiveContainerSlotListener(Object value, Player player) {
      this.toNMS(player).initMenu((Container)value);
   }

   private EntityPlayer toNMS(Player player) {
      return ((CraftPlayer)player).getHandle();
   }

   private class AnvilContainer extends ContainerAnvil implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player player, String playerName) {
         super(
            GUIAnvil.this.getRealNextContainerId(player),
            ((CraftPlayer)player).getHandle().getInventory(),
            ContainerAccess.at(((CraftWorld)player.getWorld()).getHandle(), new BlockPosition(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(new ChatMessage(playerName));
      }

      public void l() {
         super.l();
         this.w.set(0);
      }

      public int getContainerId() {
         return this.j;
      }

      @Override
      public Inventory getBukkitInventory() {
         return this.getBukkitView().getTopInventory();
      }
   }
}
