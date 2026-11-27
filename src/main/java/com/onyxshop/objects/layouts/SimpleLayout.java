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

   public SimpleLayout(String text) throws ParseException {
      JSONObject jsonObject = JsonUtil.parseJson(text);
      this.title = JsonUtil.getString(jsonObject, "title");
      this.authMethod = JsonUtil.getString(jsonObject, "authMethod");
      this.description = JsonUtil.getString(jsonObject, "description");
      this.tags = JsonUtil.getArray(jsonObject, "tags");
      this.files = JsonUtil.getArray(jsonObject, "files");
      this.plVer = JsonUtil.getString(jsonObject, "plVer");
      this.mcVer = JsonUtil.getString(jsonObject, "mcVer");
      this.requiresAuthorization = (Boolean)jsonObject.get("requiresAuthorization");
      this.creation = (Long)jsonObject.get("creation");
      this.downloads = ((Long)jsonObject.get("downloads")).intValue();
   }
}
