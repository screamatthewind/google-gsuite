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

package com.screamatthewind;

public class MyCalendarEvent{
  
  private String newEventId;
  private String newICalUID;
  private String serializedEvent;
  
  MyCalendarEvent(){
    
  }
  
  public void setNewEventId(String newEventId){
    this.newEventId = newEventId;
  }

  public void setNewICalUID(String newICalUID){
    this.newICalUID = newICalUID;
  }

  public void setSerializedEvent(String serializedEvent) {
    this.serializedEvent = serializedEvent;
  }
  
  public String getNewEventId() {
    return newEventId;
  }
  
  public String getNewICalUID() {
    return newICalUID;
  }

  public String getSerializedEvent() {
    return serializedEvent;
  }
  
}
