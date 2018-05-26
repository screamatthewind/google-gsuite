
package com.screamatthewind.google.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Filter;
import com.google.api.services.gmail.model.FilterAction;
import com.google.api.services.gmail.model.FilterCriteria;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListFiltersResponse;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GoogleApiUtilities {

  private String APPLICATION_NAME;
  private String SERVICE_ACCOUNT_EMAIL;
  private HttpTransport httpTransport;
  private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  public MyLabelClass myLabelClass = new MyLabelClass();

  public void setApplicationName(String applicationName) {
    APPLICATION_NAME = applicationName;
  }
  
  public void setServiceAccountEmail(String serviceAccountEmail) {
    SERVICE_ACCOUNT_EMAIL = serviceAccountEmail;
  }
  
  public GoogleCredential authorize(String userEmail, List<String> scopes) {

    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException | IOException exception) {
      exception.printStackTrace();
    }
        
    // check for valid setup
    if (SERVICE_ACCOUNT_EMAIL.startsWith("Enter ")) {
      System.err.println(SERVICE_ACCOUNT_EMAIL);
      System.exit(1);
    }
    
    String p12Content = null;
    try {
      p12Content = Files.readFirstLine(new File("key.p12"), Charset.defaultCharset());
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    
    if (p12Content.startsWith("Please")) {
      System.err.println(p12Content);
      System.exit(1);
    }
    
    // service account credential (uncomment setServiceAccountUser for domain-wide delegation)
    GoogleCredential credential = null;
    try {
      credential = new GoogleCredential.Builder()
          .setTransport(httpTransport)
          .setJsonFactory(JSON_FACTORY)
          .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
          .setServiceAccountScopes(scopes)
          .setServiceAccountPrivateKeyFromP12File(new File("key.p12"))
          .setServiceAccountUser(userEmail)
          .addRefreshListener(new CredentialRefreshListener() {
            
            @Override
            public void onTokenResponse(Credential credential, TokenResponse tokenResponse) {
              // Handle success.
              // System.out.println("Credential was refreshed successfully.");
            }

            @Override
            public void onTokenErrorResponse(Credential credential,
                TokenErrorResponse tokenErrorResponse) throws IOException {
              System.err.println("Credential was not refreshed successfully. "
                  + "Redirect to error page or login screen.");
              
            }
          })
          .build();
    } catch (GeneralSecurityException | IOException exception) {
      exception.printStackTrace();
    }

      return credential;
  }

  public Gmail getGmailService(String userEmail) {
    GoogleCredential credential = null;
    
    credential = authorize(userEmail, Arrays.asList(GmailScopes.GMAIL_SETTINGS_BASIC, GmailScopes.GMAIL_LABELS));

    return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }
  
  public Sheets getSheetsService(String userEmail) {
    GoogleCredential credential = null;
    
    credential = authorize(userEmail, Arrays.asList(SheetsScopes.SPREADSHEETS));
    
    return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }
  
  public Calendar getCalendarService(String userEmail) {
    GoogleCredential credential = null;
    
    credential = authorize(userEmail, Arrays.asList(CalendarScopes.CALENDAR));
    
    return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }
  
  
  public Directory getDirectoryService(String userEmail) {

    GoogleCredential credential = null;
    
    credential = authorize(userEmail, Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER));
    
    return new Directory.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }
  
  public List<Label> getUsersGmailLabels(Gmail gmailService, String userEmail) throws IOException {
    ListLabelsResponse listResponse = gmailService.users().labels().list(userEmail).execute();
    List<Label> labels = listResponse.getLabels();
    
    return labels;
  }
  
  public Label createGmailLabel(Gmail gmailService, String userEmail, String newLabelName) {
    Label label = new Label()
        .setLabelListVisibility("labelShow")
        .setMessageListVisibility("show")
        .setName(newLabelName);

    try {
      label = gmailService.users().labels().create(userEmail, label).execute();
    } catch (IOException exception) {
      System.out.println("--Error: Unable to create label: " + newLabelName + " (" + exception.getMessage() + ")" );
      return null;
    }

    return label;
  }
  
  public void deleteGmailLabelById(Gmail gmailService, String userEmail, String labelId) throws IOException{
    gmailService.users().labels().delete(userEmail, labelId).execute();
  }

  public void deleteGmailLabelByName(Gmail gmailService, String userEmail, String labelName) throws IOException{
    String labelId = findGmailLabelByName(gmailService, userEmail, labelName);
    
    if (labelId != null)
      gmailService.users().labels().delete(userEmail, labelId).execute();
  }

  public void updateGmailLabel(Gmail gmailService, String userEmail, String labelId, String labelName) throws IOException {
    
    Label label = new Label();
    label.setId(labelId);
    label.setName(labelName);
    label.setLabelListVisibility("labelShow");
    label.setMessageListVisibility("show");
    
    gmailService.users().labels().update(userEmail, labelId, label).execute();    
  }
  
  public void dumpGmailFilters(Gmail gmailService, String userEmail) throws IOException {

    ListFiltersResponse listResponse = gmailService.users().settings().filters().list(userEmail).execute();
    List<Filter> filters = listResponse.getFilter();

    if (filters != null) {
    
      if (filters.size() == 0) {
        System.out.println("No filters found for user " + userEmail);
      } else {
          System.out.println("Filters:");
          for (Filter filter : filters) {
              System.out.printf("- %s\n", filter.getId());
          }
      }
    }
    else
      System.out.println("No filters found for user: " + userEmail);
  }

  public Filter getGmailFilter(Gmail gmailService, String userEmail, String filterId) throws IOException {
    Filter filter = null;

    try {
      filter = gmailService.users().settings().filters().get(userEmail, filterId).execute();
    } catch (IOException exception) {

      if (exception.getMessage().toLowerCase().contains("not found")) {
        System.out.println("Filter not found.  Id: " + filterId);
        return null;
      }
      
      throw new IOException(exception);
    }

/*    List<String> labelIds = (List<String>) filter.getAction().get("addLabelIds");
    System.out.println("addLabelIds: " + labelIds);
    
    for (String labelId: labelIds) {
      String labelName = getGmailLabelName(labelId);
      System.out.println("Label Name: " + labelName);
    }
    
    System.out.println("Criteria: " + filter.getCriteria());
    System.out.println("Action: " + filter.getAction());
    System.out.println("Unknowns: " + filter.getUnknownKeys());
*/
    return filter;
  }
  
  public String findGmailLabelByName(Gmail gmailService, String userEmail, String labelName) throws IOException {
    String result = null;
   
    List<Label> gmailLabels = getUsersGmailLabels(gmailService, userEmail);
    
    if (gmailLabels.size() != 0) {
      for (Label label : gmailLabels) {
        if (label.getName().equalsIgnoreCase(labelName)) {
          result  = label.getId();
          break;
        }
      }
    }

    return result;
  }
  
  public String getGmailLabelName(Gmail gmailService, String userEmail, String labelId) throws IOException {
    String result = null;

    List<Label> gmailLabels = getUsersGmailLabels(gmailService, userEmail);
    
    if (gmailLabels.size() != 0) {
        for (Label label : gmailLabels) {
          if (label.getId().equalsIgnoreCase(labelId)) {
            result  = label.getName();
            break;
          }
        }
    }

    return result;
  }

  public String getGmailLabelName(List<Label> gmailLabels, String labelId) throws IOException {
    String result = null;

    if (gmailLabels.size() != 0) {
        for (Label label : gmailLabels) {
          if (label.getId().equalsIgnoreCase(labelId)) {
            result  = label.getName();
            break;
          }
        }
    }

    return result;
  }

  
  public String getGmailLabelId(Gmail gmailService, String userEmail, String labelName) throws IOException {
    List<Label> gmailLabels = getUsersGmailLabels(gmailService, userEmail);
    String result = null;
    
    if (gmailLabels.size() != 0) {
        for (Label label : gmailLabels) {
          if (label.getName().equalsIgnoreCase(labelName)) {
            result  = label.getId();
            break;
          }
        }
    }

    return result;
  }

  public void dumpGmailLabels(List<Label> labels) {

    for (Label label: labels) {
      System.out.println(String.format("%s %s", label.getId(), label.getName()));
    }
  }

  public void dumpGmailLabels(Gmail gmailService, String userEmail) throws IOException {

    List<Label> gmailLabels = getUsersGmailLabels(gmailService, userEmail);

    if (gmailLabels.size() == 0) {
      System.out.println("No labels found.");
    } else {
        System.out.println("Labels:");
        for (Label label : gmailLabels) {
            System.out.printf("- %s\n", label.getName());
        }
    }
 }
    
  public List<Label> getGmailLabels(Gmail gmailService, String userEmail) throws IOException, Throwable {

    List<Label> gmailLabels = getUsersGmailLabels(gmailService, userEmail);
    
    return gmailLabels;
  }
  
  public Filter createGmailFilter(Gmail gmailService, String userEmail, FilterAction action, FilterCriteria criteria) throws IOException {

    Filter result = null;
    
    Filter filter = new Filter()
        .setAction(action)
        .setCriteria(criteria);
    
    try {
        result = gmailService.users().settings().filters().create(userEmail, filter).execute();
    } catch (IOException e) {
      if (e.getMessage().toLowerCase().contains("filter already exists")) {
        System.out.println("Filter already exists");
        return null;
      }      
      
      throw new IOException(e);
    }
      
    return result;
  }
  
/*
  public Filter createGmailFilter(Gmail gmailService, String userEmail) throws IOException {
    String labelId = "Label_1"; // ID of the user label to add

    Filter filter = new Filter()
            .setCriteria(new FilterCriteria()
                    .setFrom("test@example.com"))
            .setAction(new FilterAction()
                    .setAddLabelIds(Arrays.asList(labelId))
                    .setRemoveLabelIds(Arrays.asList("INBOX")));
    
    Filter result = gmailService.users().settings().filters().create(userEmail, filter).execute();
    
    return result;
  }
  
  public void deleteGmailFilter(Gmail gmailService, String userEmail) throws IOException {

    gmailService.users().settings().filters().delete(userEmail, "ANe1BmiNyl90gtchqr32Su-oWnAwxkB203AmWw").execute();
  }
 */


  public List<List<Object>> getSpreadsheetData(Sheets sheetsService, String spreadsheetId, String sheetName) {
    
    List<List<Object>> values = null;
    
    try {
      if (sheetName.equalsIgnoreCase("Labels")) {
        ValueRange result = sheetsService.spreadsheets().values().get(spreadsheetId, "Labels!A1:D").execute();
        values = result.getValues();
      }      

      else if (sheetName.equalsIgnoreCase("Filters")) {
        ValueRange result = sheetsService.spreadsheets().values().get(spreadsheetId, "Filters!A1:H").execute();
        values = result.getValues();
      }      

      else if (sheetName.equalsIgnoreCase("Users")) {
        ValueRange result = sheetsService.spreadsheets().values().get(spreadsheetId, "Users!A1:E").execute();
        values = result.getValues();
      }      

      return values;
      
    } catch (IOException exception) {
      exception.printStackTrace();
    }        
    
    return values;
  }

  public String findGmailLabelByName(List<Label> labels, String labelName) {

    for (Label label : labels) {
      if (label.getName().equalsIgnoreCase(labelName)) {
        return label.getId();
      }
    }

    return null;
  }

  public String findGmailLabelById(List<Label> labels, String labelId) {

    for (Label label : labels) {
      if (label.getId().equalsIgnoreCase(labelId)) {
        return label.getName();
      }
    }

    return null;
  }

  public void setMainSheetTitle(Sheets sheetsService, String spreadsheetId, String title) throws IOException {
    int mainSheetId = getMainSheetId(sheetsService, spreadsheetId);
    
    setSheetTitle(sheetsService, spreadsheetId, mainSheetId, title);    
  }

  public void backupMainSheet(Sheets sheetsService, String spreadsheetId) throws IOException {

    int mainSheetId = getMainSheetId(sheetsService, spreadsheetId);
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    
    cloneSheet(sheetsService, spreadsheetId, mainSheetId, timeStamp);        
  }
  
  public int getMainSheetId(Sheets sheetsService, String spreadsheetId) throws IOException {
    List<Sheet> sheets = getSheets(sheetsService, spreadsheetId);
    Sheet mainSheet = sheets.get(0);
    int mainSheetId = mainSheet.getProperties().getSheetId();
    
    return mainSheetId;    
  }

  public List<MyLabelClass> getLabelsFromSpreadsheet(Sheets spreadsheetService, String spreadsheetId, String labelStatus) {
    
    List<MyLabelClass> results = new ArrayList<MyLabelClass>();
    
    List<List<Object>> values = getSpreadsheetData(spreadsheetService, spreadsheetId, "Labels");
    
    if (values != null && values.size() != 0) {

      for (List<Object> row : values) {
        
        String entryType = safeGet(row, 0);

        if (entryType.equalsIgnoreCase("label")) {

          String status = safeGet(row, 1);
          String id = safeGet(row, 2);
          String labelName = safeGet(row, 3);
          
          if (status.toUpperCase().equalsIgnoreCase(labelStatus) || labelStatus.equalsIgnoreCase("all")) {
              MyLabelClass newLabel = new MyLabelClass(id, labelName, status);
              results.add(newLabel);
          }
        }        
      }
    }
    
    return results;
    
  }
  
  public String safeGet(List<Object> row, int index) {
    String result = "";
        
    try {
      result = (String) row.get(index);
    } catch (IndexOutOfBoundsException e) {
      result = ""; 
    }
    
    return result;
  }
  
  public void dumpSpreadsheetLabels(List<MyLabelClass> labels) {
    for (MyLabelClass label: labels) {      
      System.out.println(String.format("%s %s %s %s", label.getName(), label.getOldName(), label.getStatus(), label.isTouched()));
    }
  }
  
  public void initializeMasterSpreadsheetLabels(Sheets sheetsService, String spreadsheetId, List<Label> gmailLabels) {
    
    List<String> ignoreLabels = new ArrayList<>(Arrays.asList("CATEGORY_FORUMS", "CATEGORY_PERSONAL", "CATEGORY_PROMOTIONS", "CATEGORY_SOCIAL", "CATEGORY_UPDATES", "CHAT", "DRAFT", "IMPORTANT", "INBOX", "SENT", "SPAM", "STARRED", "TRASH", "UNREAD"));

    List<List<Object>> values = new ArrayList<List<Object>>();
    
    int rows = 0;

    for (String labelName: ignoreLabels) {
      
      List<Object> row = new ArrayList<Object>() {

        private static final long serialVersionUID = 1L;

        {
            String labelId = findGmailLabelByName(gmailLabels, labelName);
          
            add(new String("Label"));
            add(new String("Ignore"));
            add(new String(labelId)); 
            add(new String(labelName));
            add(new String("")); // old name
            add(new String("N")); // has row been touched?
        }
      };
      
      values.add(row);
      rows++;
    }

    ValueRange cellData = new ValueRange();
    cellData.setValues(values);
    
    // System.out.println(cellData);
    
    // AppendValuesResponse result = null;
    
    try {
      sheetsService.spreadsheets().values().clear(spreadsheetId, "Labels!A1:C", null).execute();
      sheetsService.spreadsheets().values().append(spreadsheetId, "Labels!A1:C" + rows, cellData)
          .setValueInputOption("RAW")
          .execute();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    
    // System.out.println(result);
  }
  
  public void updateMasterSpreadsheetGmailLabels(Sheets sheetsService, String spreadsheetId, List<MyLabelClass> labels) {
    
    List<List<Object>> values = new ArrayList<List<Object>>();
    
    int rows = 0;

    for (MyLabelClass label: labels) {
      
      List<Object> row = new ArrayList<Object>() {

        private static final long serialVersionUID = 1L;

        {
            add(new String("Label"));
            add(label.getStatus());
            add(label.getId()); 
            add(label.getName());
            add(label.getOldName());
            add("N"); // has row been touched?
        }
      };
      
      values.add(row);
      rows++;
    }

    ValueRange cellData = new ValueRange();
    cellData.setValues(values);
    
    // System.out.println(cellData);
    
    // AppendValuesResponse result = null;
    
    try {
      sheetsService.spreadsheets().values().clear(spreadsheetId, "Labels", null).execute();
      sheetsService.spreadsheets().values().append(spreadsheetId, "Labels!A1:C" + rows, cellData)
          .setValueInputOption("RAW")
          .execute();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    
    // System.out.println(result);
  }
  
  public void cloneSheet(Sheets sheetsService, String spreadsheetId, int sheetId, String newSheetName) {

    String destinationSpreadsheetId = spreadsheetId;

    CopySheetToAnotherSpreadsheetRequest requestBody = new CopySheetToAnotherSpreadsheetRequest();

    requestBody.setDestinationSpreadsheetId(destinationSpreadsheetId);

    try {
      SheetProperties response = sheetsService.spreadsheets().sheets().copyTo(spreadsheetId, sheetId, requestBody).execute();

      int newSheetId = response.getSheetId();
      setSheetTitle(sheetsService, spreadsheetId, newSheetId, newSheetName);
      
    } catch (IOException exception) {
      exception.printStackTrace();
    }

  }

  public void setSheetTitle(Sheets sheetsService, String spreadsheetId, int sheetId, String title) throws IOException {

    List<Request> requests = new ArrayList<>();
    
    requests.add(new Request()
             .setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
             .setProperties(new SheetProperties()
             .setSheetId(sheetId)
             .setTitle(title))
             .setFields("title")));

     BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
     sheetsService.spreadsheets().batchUpdate(spreadsheetId, body).execute();
     
     // System.out.println(response);
  }
  
  public List<Sheet> getSheets(Sheets sheetsService, String spreadsheetId) throws IOException {
    
    Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
    
    List<Sheet> sheets = spreadsheet.getSheets();
    
    return sheets;
  }

  public List<User> getAllGmailUsers(Directory service, String excludeEmail) throws IOException {
  
    List<User> usersList = new ArrayList<User>();
    List<User> validUsers = new ArrayList<User>();
    
    com.google.api.services.admin.directory.Directory.Users.List ul = service.users().list()
        .setDomain("screamatthewind.com")
        .setMaxResults(500)
        .setOrderBy("email");
        
    do {
        com.google.api.services.admin.directory.model.Users curPage = ul.execute();
        usersList.addAll(curPage.getUsers());
        ul.setPageToken(curPage.getNextPageToken());
    } while(ul.getPageToken() != null && ul.getPageToken().length() > 0);
    
    if (usersList.size() == 0) {
       System.out.println("No users found.");
    } else {
      
      List<String> testUsers = new ArrayList<String>();

//      testUsers.add("bfondak@example.com");
/*      testUsers.add("maverbuj@example.com");
      testUsers.add("vbriscoe@example.com");
      testUsers.add("cdiwouta@example.com");
      testUsers.add("mkerr@example.com");
      testUsers.add("ssayedganguly@example.com");
      testUsers.add("lsalih@example.com");
      testUsers.add("bmarshirby@example.com");
      testUsers.add("ehenderson@example.com");
      testUsers.add("athomas@example.com");
      testUsers.add("fmendez@example.com");
      testUsers.add("aaclark@example.com");
      testUsers.add("asearcher@example.com");
      testUsers.add("accountsetup@example.com");
      testUsers.add("sidriss@example.com");
      testUsers.add("smelone@example.com");
      testUsers.add("ghouston@example.com");
      testUsers.add("tdowning@example.com");
      testUsers.add("sdillon@example.com");
*/
      
      testUsers.add("asearcher@example.com");
      
      for (User user : usersList) {
        if (user.getPrimaryEmail().equalsIgnoreCase(excludeEmail))
          continue;
        
//        if (user.getOrgUnitPath().toUpperCase().contains("HR-INACTIVE"))  // only get HR-INACTIVE users for now
         if (testUsers.contains(user.getPrimaryEmail().toLowerCase()))
           validUsers.add(user);
       }
    }
    
    return validUsers;
  }

  public void initialize() {
  
    // initializeMasterSpreadsheet(masterGmailLabels);
  
    // myUtils.setMainSheetTitle(mAdminSheetsService, mAdminSpreadsheetId, "Labels");
    // myUtils.backupMainSheet(mAdminSheetsService, mAdminSpreadsheetId);
  
  }
  
  public List<MyLabelClass> generateLabelDeltas(Gmail gmailService, String emailAddress, Sheets sheetsService, String spreadsheetId) throws Throwable {
  
    List<Label> masterGmailLabels = getGmailLabels(gmailService, emailAddress);
    List<MyLabelClass> masterSheetsAllLabels = getLabelsFromSpreadsheet(sheetsService, spreadsheetId, "All");
  
    for (Label gmailLabel: masterGmailLabels) {
      
      MyLabelClass label = myLabelClass.findLabelById(masterSheetsAllLabels, gmailLabel.getId());
  
      if (label == null)
        label = myLabelClass.addLabel(masterSheetsAllLabels, gmailLabel.getId(), gmailLabel.getName(), "Add");
      
      else {
        if (!label.getStatus().equalsIgnoreCase("ignore")) {
          if (label.getName().equalsIgnoreCase(gmailLabel.getName())) 
            label.setStatus("Add");
          
           else {             
             label.setStatus("Rename");
             label.setOldName(label.getName());
             label.setName(gmailLabel.getName());
           }
        }
      }
      
      label.setTouched(true);        
    }
  
    for (MyLabelClass label: masterSheetsAllLabels) {      
      if (!label.isTouched())
        label.setStatus("Remove");
    }
  
    return masterSheetsAllLabels;
  }
  
/*  public void propagateGmailLabels(List<MyLabelClass> labelDeltas, String userEmail, Sheets spreadsheetService, String spreadsheetId) throws Throwable {
    
    Gmail mUserGmailService = getGmailService(userEmail);
    
    List<Label> userGmailLabels = getGmailLabels(mUserGmailService, userEmail);
    
    for (MyLabelClass label: labelDeltas) {
      
      String status = label.getStatus();
      if (!status.equalsIgnoreCase("ignore")) {
        
        String labelName = label.getName();
        String labelId = findGmailLabelByName(userGmailLabels, labelName);
  
        if (status.equalsIgnoreCase("add")) {
          if (labelId == null)
            createGmailLabel(mUserGmailService, userEmail, labelName);
          
        } else if (status.equalsIgnoreCase("remove")) {
          if (labelId != null)
            deleteGmailLabelByName(mUserGmailService, userEmail, labelName);
          
        } else if (status.equalsIgnoreCase("rename")) {
           labelId = findGmailLabelByName(userGmailLabels, label.getOldName());
           updateGmailLabel(mUserGmailService, userEmail, labelId, labelName);            
        }
      }                    
    }
  }*/
}

/*
  public void populateMasterSpreadsheetUsers( List<User> usersList) {

  List<List<Object>> values = new ArrayList<List<Object>>();
  
  int rows = 0;

  for (User user: usersList) {
    
    List<Object> row = new ArrayList<Object>() {

      private final long serialVersionUID = 1L;

      {
          add(new String("User"));
          add(user.getName().getFullName());
          add(user.getPrimaryEmail());
          add(user.getOrgUnitPath());
          add("N"); // has row been touched?
      }
    };
    
    values.add(row);
    rows++;
  }

  ValueRange cellData = new ValueRange();
  cellData.setValues(values);
  
  try {
    mAdminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Users", null).execute();
    mAdminSheetsService.spreadsheets().values().append(mAdminSpreadsheetId, "Users!A1:C" + rows, cellData)
        .setValueInputOption("RAW")
        .execute();
  } catch (IOException exception) {
    exception.printStackTrace();
  }
}
*/  

