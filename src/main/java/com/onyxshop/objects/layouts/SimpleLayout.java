package com.onyxshop.objects.layouts;

import org.json.simple.parser.ParseException;
import java.util.ArrayList;
import com.onyxshop.util.JsonUtil;
import org.json.simple.JSONObject;

public class SimpleLayout {
   public String title;
   public String authMethod;
   public String description;
   public ArrayList<String> tags;
   public ArrayList<String> files;
   public String plVer;
   public String mcVer;
   public boolean requiresAuthorization;
   public long creation;
   public int downloads;

   public SimpleLayout(String var1) throws ParseException {
      JSONObject var2 = JsonUtil.parseJson(var1);
      this.title = JsonUtil.getString(var2, "title");
      this.authMethod = JsonUtil.getString(var2, "authMethod");
      this.description = JsonUtil.getString(var2, "description");
      this.tags = JsonUtil.getArray(var2, "tags");
      this.files = JsonUtil.getArray(var2, "files");
      this.plVer = JsonUtil.getString(var2, "plVer");
      this.mcVer = JsonUtil.getString(var2, "mcVer");
      this.requiresAuthorization = (Boolean)var2.get("requiresAuthorization");
      this.creation = (Long)var2.get("creation");
      this.downloads = ((Long)var2.get("downloads")).intValue();
   }
}
