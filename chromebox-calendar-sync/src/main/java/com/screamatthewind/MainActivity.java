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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;

import com.screamatthewind.google.utils.GoogleApiUtilities;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity {

  /** Global instance of the event datastore. */
  private static DataStore<String> eventDataStore;

  /** Global instance of the sync settings datastore. */
  private static DataStore<String> syncSettingsDataStore;

  /** The key in the sync settings datastore that holds the current sync token. */
  private static final String SYNC_TOKEN_KEY = "syncToken";

  private static String mApplicationName = "chromebox-calendar-sync";

  // private static String mAdminEmail = "helpdesk@screamatthewind.com";
  private static String mServiceAccountEmail =
      "gmail-label-and-rule-copier@project-id-999.iam.gserviceaccount.com";

  private static Calendar m_sourceCalendarService;
  private static Calendar m_destCalendarService;

  private static String m_sourceCalendarId = "asearcher@screamatthewind.com";
  private static String m_destCalendarId = "aaclark@screamatthewind.com";

  private static GoogleApiUtilities myUtils = new GoogleApiUtilities();

  private static Gson gson = new Gson();

  public static void main(String[] args) {
    try {

      myUtils.setApplicationName(mApplicationName);
      myUtils.setServiceAccountEmail(mServiceAccountEmail);

      m_sourceCalendarService = myUtils.getCalendarService(m_sourceCalendarId);
      m_destCalendarService = myUtils.getCalendarService(m_destCalendarId);

      eventDataStore = Utils.getDataStoreFactory().getDataStore("EventStore");
      syncSettingsDataStore = Utils.getDataStoreFactory().getDataStore("SyncSettings");

      run();

    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  private static void run() throws IOException {

    // handleDeletions(); -- delete events show up as 'cancelled'

    // Construct the {@link Calendar.Events.List} request, but don't execute it yet.
    Calendar.Events.List request = m_sourceCalendarService.events().list(m_sourceCalendarId);

    // Load the sync token stored from the last execution, if any.
    String syncToken = syncSettingsDataStore.get(SYNC_TOKEN_KEY);

    if (syncToken == null) {
      System.out.println("Performing full sync.");

      Date oneMonthAgo = Utils.getRelativeDate(java.util.Calendar.MONTH, -1);
      request.setTimeMin(new DateTime(oneMonthAgo, TimeZone.getTimeZone("UTC")));

    } else {
      System.out.println("Performing incremental sync.");
      request.setSyncToken(syncToken);
    }

    // Retrieve the events, one page at a time.
    String pageToken = null;
    Events events = null;

    do {
      request.setPageToken(pageToken);

      try {
        events = request.execute();

      } catch (GoogleJsonResponseException e) {

        if (e.getStatusCode() == 410) {

          // A 410 status code, "Gone", indicates that the sync token is invalid.
          System.out.println("Invalid sync token, clearing event store and re-syncing.");

          syncSettingsDataStore.delete(SYNC_TOKEN_KEY);
          eventDataStore.clear();

          run();
        } else {
          throw e;
        }
      }

      List<Event> items = events.getItems();

      if (items.size() == 0) {
        System.out.println("No new events to sync.");

      } else {
        for (Event event : items) {
          syncEvent(event);
        }
      }

      pageToken = events.getNextPageToken();

    } while (pageToken != null);

    // Store the sync token from the last request to be used during the next execution.
    syncSettingsDataStore.set(SYNC_TOKEN_KEY, events.getNextSyncToken());

    System.out.println("Sync complete.");
  }

  // -- deleted events show up as canceled
  // private static void handleDeletions() throws IOException {
  // Set<String> eventIds = eventDataStore.keySet();
  //
  // for (String eventId : eventIds) {
  // Event event = m_destCalendarService.events().get(m_sourceCalendarId, eventId).execute();
  //
  // if (event == null) {
  // m_destCalendarService.events().delete(m_destCalendarId, newEventId).setSendNotifications(false)
  // .execute();
  //
  // eventDataStore.delete(eventId);
  // }
  // }
  // }

  private static void syncEvent(Event event) throws IOException {

    // always try to delete it in case it has been modified
    // DeleteEvent(event);

    if (event.getStatus().equals("cancelled")) {
      System.out.println(String.format("Canceling event: ID=%s", event.getId()));

    } else {

      String storedEvent = eventDataStore.get(event.getId());
      if (storedEvent == null) {
        
        CreateEvent(event);
        System.out.println(
            String.format("Creatingevent: ID=%s, Name=%s", event.getId(), event.getSummary()));
        
      } else {

        UpdateEvent(event);
        System.out.println(
            String.format("Updating event: ID=%s, Name=%s", event.getId(), event.getSummary()));
        
      }
    }
  }

  private static void CreateEvent(Event event) throws IOException {

    Event newEvent = null;
    String saveEventId = event.getId();
    String saveICalUID = event.getICalUID();

    try {

      event.setId("");
      event.setICalUID("");

      newEvent = m_destCalendarService.events().insert(m_destCalendarId, event)
          .setSendNotifications(false).execute();

    } catch (GoogleJsonResponseException ex) {

      // event already exists
      if (ex.getStatusCode() == 409) {

        if (!eventDataStore.containsKey(event.getId())) System.out.println("does not exsist");

      }
    }

    event.setId(saveEventId);
    event.setICalUID(saveICalUID);

    MyCalendarEvent myCalendarEvent = new MyCalendarEvent();
    myCalendarEvent.setNewEventId(newEvent.getId());
    myCalendarEvent.setNewICalUID(newEvent.getICalUID());
    myCalendarEvent.setSerializedEvent(event.toString());

    String serializedEvent = new Gson().toJson(myCalendarEvent);
    eventDataStore.set(event.getId(), serializedEvent);
  }

  private static void UpdateEvent(Event event) throws IOException {

    String storedEvent = eventDataStore.get(event.getId());
    if (storedEvent == null) return;

    Event newEvent = null;
    String saveEventId = event.getId();
    String saveICalUID = event.getICalUID();

    MyCalendarEvent myCalendarEvent = gson.fromJson(storedEvent, MyCalendarEvent.class);

    if (myCalendarEvent != null) {
      String newEventId = myCalendarEvent.getNewEventId();
      String newICalUID = myCalendarEvent.getNewICalUID();

      try {

        event.setId(newEventId);
        event.setICalUID(newICalUID);

        newEvent = m_destCalendarService.events().update(m_destCalendarId, newEventId, event)
            .setSendNotifications(false).execute();

      } catch (GoogleJsonResponseException ex) {

        // event already exists
        if (ex.getStatusCode() == 409) {

          if (!eventDataStore.containsKey(event.getId())) System.out.println("does not exsist");

        }
      }

      event.setId(saveEventId);
      event.setICalUID(saveICalUID);


      String serializedEvent = new Gson().toJson(myCalendarEvent);

      eventDataStore.delete(event.getId());
      eventDataStore.set(event.getId(), serializedEvent);

    }
  }

  private static void DeleteEvent(Event event) throws IOException {

    String storedEvent = eventDataStore.get(event.getId());
    if (storedEvent == null) return;

    MyCalendarEvent myCalendarEvent = gson.fromJson(storedEvent, MyCalendarEvent.class);

    if (myCalendarEvent != null) {
      String newEventId = myCalendarEvent.getNewEventId();

      Event validateEvent =
          m_destCalendarService.events().get(m_destCalendarId, newEventId).execute();

      if ((validateEvent != null) && (!validateEvent.getStatus().equals("cancelled")))
        m_destCalendarService.events().delete(m_destCalendarId, newEventId)
            .setSendNotifications(false).execute();
    }

    if (eventDataStore.containsKey(event.getId())) eventDataStore.delete(event.getId());
  }
}
