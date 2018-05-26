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

import com.google.api.services.gmail.model.Label;

import java.util.List;

public class MyUserBackupClass {

  public String mEmailAddress;
  public List<Label> mLabels;
  public List<MyFilterInfoClass> mFilterData;
  
/*  
  public MyUserBackupClass(String filterStatus, String filterId, String filterAction, String filterCriteria, String filterText, String filterTouched) {

    mFilterId = filterId;
    mFilterAction = filterAction;
    mFilterCriteria = filterCriteria;
    mFilterText = filterText;
  }
  
  public String getFilterId() {
    return mFilterId;
  }

  public String getFilterAction() {
    return mFilterAction;
  }
  
  public String getFilterCriteria() {
    return mFilterCriteria;
  }
  
  public String getFilterText() {
    return mFilterText;
  }

  public void setFilterText(String filterText) {
    mFilterText = filterText;      
  }
  
  public static MyUserBackupClass addFilter(List<MyUserBackupClass> filters, String filterId, String filterAction, String filterCriteria, String filterText, String filterStatus) {

    MyUserBackupClass newFilter = new MyUserBackupClass(filterStatus, filterId, filterAction, filterCriteria, filterText);
    filters.add(newFilter);
    
    return newFilter;
  }
  */

}


