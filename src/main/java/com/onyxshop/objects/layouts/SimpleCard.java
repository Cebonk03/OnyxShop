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

   public void setAuthMethod(AuthRequest.AuthMethod authMethod) {
      this.authMethod = authMethod;
   }

   public void setToken(long value) {
      this.token = value;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public void setDescription(String text) {
      this.description = text;
   }

   public void setTags(List<String> values) {
      this.tags = values;
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
      HashMap map = new HashMap();
      map.put("authMethod", this.authMethod.name());
      map.put("token", this.token);
      map.put("title", this.title);
      map.put("description", this.description);
      map.put("tags", this.tags);
      map.put("plVer", OnyxShop.getInstance().getDescription().getVersion());
      map.put("mcVer", Bukkit.getBukkitVersion().split("-")[0]);
      return new JSONObject(map).toString();
   }
}
