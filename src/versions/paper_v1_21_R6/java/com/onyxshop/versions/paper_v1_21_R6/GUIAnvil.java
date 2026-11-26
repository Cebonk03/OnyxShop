package com.onyxshop.versions.paper_v1_21_R6;

import com.onyxshop.VersionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
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
      CBClassWrapper.eventFactoryHandleInventoryClose(this.toNMS(var1));
   }

   @Override
   public void sendPacketOpenWindow(Player var1, int var2, String var3) {
      this.toNMS(var1).connection.send(new ClientboundOpenScreenPacket(var2, MenuType.ANVIL, Component.literal(var3)));
   }

   @Override
   public void sendPacketCloseWindow(Player var1, int var2) {
      this.toNMS(var1).connection.send(new ClientboundContainerClosePacket(var2));
   }

   @Override
   public void setActiveContainerDefault(Player var1) {
      this.toNMS(var1).containerMenu = this.toNMS(var1).inventoryMenu;
   }

   @Override
   public void setActiveContainer(Player var1, Object var2) {
      this.toNMS(var1).containerMenu = (AbstractContainerMenu)var2;
   }

   @Override
   public void setActiveContainerId(Object var1, int var2) {
   }

   @Override
   public void addActiveContainerSlotListener(Object var1, Player var2) {
      this.toNMS(var2).initMenu((AbstractContainerMenu)var1);
   }

   private ServerPlayer toNMS(Player var1) {
      return CBClassWrapper.getPlayerHandle(var1);
   }

   private class AnvilContainer extends AnvilMenu implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player nullx, String nullxx) {
         super(
            GUIAnvil.this.getRealNextContainerId(nullx),
            GUIAnvil.this.toNMS(nullx).getInventory(),
            ContainerLevelAccess.create(CBClassWrapper.getWorldHandle(nullx.getWorld()), new BlockPos(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(Component.literal(nullxx));
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
