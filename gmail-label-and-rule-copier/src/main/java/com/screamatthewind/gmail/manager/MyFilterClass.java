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

package com.screamatthewind.gmail.manager;

import java.util.List;

public class MyFilterClass {

  public String mStatus;
  public String mId;
  public String mAction;
  public String mCriteria;
  public String mText, mOldText;
  public String mTouched;
  
  public MyFilterClass(String filterStatus, String filterId, String filterAction, String filterCriteria, String filterText, String filterTouched) {

    mStatus = filterStatus;
    mId = filterId;
    mAction = filterAction;
    mCriteria = filterCriteria;
    mText = filterText;
    mOldText = "";
    mTouched = filterTouched;
  }

  public static MyFilterClass findFilterById(List<MyFilterClass> filters, String filterId) {

    for (MyFilterClass filter: filters) {
      if (filter.getId().equalsIgnoreCase(filterId)) 
          return filter;
    }

    return null;
  }

  public String getId() {
    return mId;
  }

  public String getAction() {
    return mAction;
  }
  
  public String getCriteria() {
    return mCriteria;
  }
  
  public String getText() {
    return mText;
  }

  public String getStatus() {
    return mStatus;
  }
  
  public void setStatus(String filterStatus) {
    mStatus = filterStatus;
  }

  public void setText(String text) {
    mText = text;      
  }

  public void setOldText(String text) {
    mOldText = text;      
  }

  public boolean isTouched() {
    
    if (mTouched.equalsIgnoreCase("Y"))
      return true;
    
    return false;
  }

  public void setTouched(boolean b) {
    if (b)
      mTouched = "Y";
  }

  public static MyFilterClass addFilter(List<MyFilterClass> filters, String filterId, String filterAction, String filterCriteria, String filterTExt, String filterStatus) {

    MyFilterClass newFilter = new MyFilterClass(filterStatus, filterId, filterAction, filterCriteria, filterTExt, "N");
    filters.add(newFilter);
    
    return newFilter;
  }

  public String getOldText() {
    return mOldText;
  }
}

