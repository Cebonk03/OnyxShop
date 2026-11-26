package com.onyxshop.versions.v1_21_R6;

import com.onyxshop.VersionHandler;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ReflectUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.Containers;
import org.bukkit.craftbukkit.v1_21_R6.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R6.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R6.event.CraftEventFactory;
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
      try {
         CraftEventFactory.handleInventoryCloseEvent(this.toNMS(var1));
      } catch (NoSuchMethodError var5) {
         try {
            Class var3 = Class.forName("org.bukkit.event.inventory.InventoryCloseEvent$Reason");
            ReflectUtil.invokeStaticLegacyMethod(
               "handleInventoryCloseEvent", CraftEventFactory.class, new Class[]{EntityHuman.class, var3}, this.toNMS(var1), var3.getField("PLUGIN").get(null)
            );
         } catch (Exception var4) {
            SendMessage.errorMessage("Failed to open anvil GUI for player");
            throw new RuntimeException(var4);
         }
      }
   }

   @Override
   public void sendPacketOpenWindow(Player var1, int var2, String var3) {
      this.toNMS(var1).g.b(new PacketPlayOutOpenWindow(var2, Containers.i, IChatBaseComponent.b(var3)));
   }

   @Override
   public void sendPacketCloseWindow(Player var1, int var2) {
      this.toNMS(var1).g.b(new PacketPlayOutCloseWindow(var2));
   }

   @Override
   public void setActiveContainerDefault(Player var1) {
      this.toNMS(var1).cl = this.toNMS(var1).ck;
   }

   @Override
   public void setActiveContainer(Player var1, Object var2) {
      this.toNMS(var1).cl = (Container)var2;
   }

   @Override
   public void setActiveContainerId(Object var1, int var2) {
   }

   @Override
   public void addActiveContainerSlotListener(Object var1, Player var2) {
      this.toNMS(var2).a((Container)var1);
   }

   private EntityPlayer toNMS(Player var1) {
      return ((CraftPlayer)var1).getHandle();
   }

   private class AnvilContainer extends ContainerAnvil implements VersionHandler.AnvilGUI.AnvilContainer {
      public AnvilContainer(Player nullx, String nullxx) {
         super(
            GUIAnvil.this.getRealNextContainerId(nullx),
            GUIAnvil.this.toNMS(nullx).gB(),
            ContainerAccess.a(((CraftWorld)nullx.getWorld()).getHandle(), new BlockPosition(0, 0, 0))
         );
         this.checkReachable = false;
         this.setTitle(IChatBaseComponent.b(nullxx));
      }

      public void l() {
         super.l();
         this.y.a(0);
      }

      public int getContainerId() {
         return this.l;
      }

      @Override
      public Inventory getBukkitInventory() {
         return this.getBukkitView().getTopInventory();
      }
   }
}
