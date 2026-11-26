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
   public VersionHandler.AnvilGUI.AnvilContainer newAnvilContainer(Player var1, String var2) {
      return new GUIAnvil.AnvilContainer(var1, var2);
   }

   private int getRealNextContainerId(Player var1) {
      return this.toNMS(var1).nextContainerCounter();
   }

   @Override
   public int getNextContainerId(Player var1, Object var2) {
      return ((GUIAnvil.AnvilContainer)var2).getContainerId();
   }

   @Override
   public void handleInventoryCloseEvent(Player var1) {
      CraftEventFactory.handleInventoryCloseEvent(this.toNMS(var1));
   }

   @Override
   public void sendPacketOpenWindow(Player var1, int var2, String var3) {
      this.toNMS(var1).playerConnection.sendPacket(new PacketPlayOutOpenWindow(var2, Containers.ANVIL, new ChatMessage(var3)));
   }

   @Override
   public void sendPacketCloseWindow(Player var1, int var2) {
      this.toNMS(var1).playerConnection.sendPacket(new PacketPlayOutCloseWindow(var2));
   }

   @Override
   public void setActiveContainerDefault(Player var1) {
      this.toNMS(var1).activeContainer = this.toNMS(var1).defaultContainer;
   }

   @Override
   public void setActiveContainer(Player var1, Object var2) {
      this.toNMS(var1).activeContainer = (Container)var2;
   }

   @Override
   public void setActiveContainerId(Object var1, int var2) {
   }

   @Override
   public void addActiveContainerSlotListener(Object var1, Player var2) {
      ((Container)var1).addSlotListener(this.toNMS(var2));
   }

   private EntityPlayer toNMS(Player var1) {
      return ((CraftPlayer)var1).getHandle();
   }

   private class AnvilContainer extends ContainerAnvil implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player nullx, String nullxx) {
         super(
            GUIAnvil.this.getRealNextContainerId(nullx),
            ((CraftPlayer)nullx).getHandle().inventory,
            ContainerAccess.at(((CraftWorld)nullx.getWorld()).getHandle(), new BlockPosition(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(new ChatMessage(nullxx));
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
