/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.screamatthewind.google.utils;

import com.google.api.services.gmail.model.FilterCriteria;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FilterCriteriaDeserializer implements JsonDeserializer {

  @Override
  public Object deserialize(JsonElement jsonElement, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {

    final FilterCriteria criteria = new FilterCriteria();
    
    JsonObject jsonObject = jsonElement.getAsJsonObject();    
    
    Set<Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
    for(Map.Entry<String,JsonElement> entry : entrySet){
      
      String key = entry.getKey();
      
      if (key.equalsIgnoreCase("excludeChats"))
          criteria.setExcludeChats(entry.getValue().getAsBoolean());
            
      else if (key.equalsIgnoreCase("hasAttachment"))
        criteria.setHasAttachment(entry.getValue().getAsBoolean());

      else if (key.equalsIgnoreCase("size"))
        criteria.setSize(entry.getValue().getAsInt());

      else if (key.equalsIgnoreCase("from"))
        criteria.setFrom(entry.getValue().getAsString());

      else if (key.equalsIgnoreCase("negatedQuery"))
        criteria.setNegatedQuery(entry.getValue().getAsString());

      else if (key.equalsIgnoreCase("query"))
        criteria.setQuery(entry.getValue().getAsString());

      else if (key.equalsIgnoreCase("sizeComparison"))
        criteria.setSizeComparison(entry.getValue().getAsString());

      else if (key.equalsIgnoreCase("subject"))
        criteria.setSubject(entry.getValue().getAsString());

      else if (key.equalsIgnoreCase("to"))
        criteria.setTo(entry.getValue().getAsString());
    
      else
        System.out.println("FilterCriteriaDeserializer: cannot find value: " + key);
    }
    
    return criteria; 
  }

}
