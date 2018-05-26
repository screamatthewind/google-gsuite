/*
 * Copyright (c) 2014 Google Inc.
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

import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.util.Date;

/**
 * A collection of utility methods used by these samples.
 */
public class Utils {

  /** Directory to store user credentials. */
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".store/calendar-sync");

  /** Global instance of the {@link DataStoreFactory}. */
  private static FileDataStoreFactory dataStoreFactory;

  /** Global instance of the JSON factory. */
  static {
    try {
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  /** Gets the datastore factory used in these samples. */
  public static DataStoreFactory getDataStoreFactory() {
    return dataStoreFactory;
  }

  /**
   * Gets a new {@link java.util.Date} relative to the current date and time.
   *
   * @param field the field identifier from {@link java.util.Calendar} to increment
   * @param amount the amount of the field to increment
   * @return the new date
   */
  public static Date getRelativeDate(int field, int amount) {
    Date now = new Date();
    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime(now);
    cal.add(field, amount);
    return cal.getTime();
  }
}
