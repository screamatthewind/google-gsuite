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

public class MyLabelClass {

    private String mId;
    private String mName, mOldName;
    private String mStatus;
    private String mTouched;

    public MyLabelClass(String labelId, String labelName, String labelStatus) {

      mId = labelId;
      mName = labelName;
      mOldName = "";
      mStatus = labelStatus;
      mTouched = "N";
    }
    
    public MyLabelClass() {
    }

    public void setName(String name) {
      mName = name;      
    }

    public void setOldName(String name) {
      mOldName = name;      
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

    public MyLabelClass findLabelById(List<MyLabelClass> labels, String labelId) {

      for (MyLabelClass label: labels) {
        if (label.getId().equalsIgnoreCase(labelId)) 
            return label;
      }

      return null;
    }

    public MyLabelClass addLabel(List<MyLabelClass> labels, String labelId, String labelName, String labelStatus) {

      MyLabelClass newLabel = new MyLabelClass(labelId, labelName, labelStatus);
      labels.add(newLabel);
      
      return newLabel;
    }
    
    public String getId() {
      return mId;
    }

    public String getName() {
      return mName;
    }

    public String getOldName() {
      return mOldName;
    }

    public String getStatus() {
      return mStatus;
    }

    public void setStatus(String labelStatus) {
      mStatus = labelStatus;
    }
  
}
