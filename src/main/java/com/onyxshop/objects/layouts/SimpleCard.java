package com.onyxshop.objects.layouts;

import java.util.HashMap;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.inventorys.AuthRequest;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;

public class SimpleCard {
   private AuthRequest.AuthMethod authMethod;
   private boolean prem = true;
   private long token;
   private String title;
   private String description;
   private List<String> tags;

   public SimpleCard() {
   }

   public void setAuthMethod(AuthRequest.AuthMethod var1) {
      this.authMethod = var1;
   }

   public void setToken(long var1) {
      this.token = var1;
   }

   public void setTitle(String var1) {
      this.title = var1;
   }

   public void setDescription(String var1) {
      this.description = var1;
   }

   public void setTags(List<String> var1) {
      this.tags = var1;
   }

   public String getTitle() {
      return this.title;
   }

   public String getDescription() {
      return this.description;
   }

   public List<String> getTags() {
      return this.tags;
   }

   public AuthRequest.AuthMethod getAuthMethod() {
      return this.authMethod;
   }

   @Override
   public String toString() {
      HashMap var1 = new HashMap();
      var1.put("authMethod", this.authMethod.name());
      var1.put("token", this.token);
      var1.put("title", this.title);
      var1.put("description", this.description);
      var1.put("tags", this.tags);
      var1.put("plVer", OnyxShop.getInstance().getDescription().getVersion());
      var1.put("mcVer", Bukkit.getBukkitVersion().split("-")[0]);
      return new JSONObject(var1).toString();
   }
}
