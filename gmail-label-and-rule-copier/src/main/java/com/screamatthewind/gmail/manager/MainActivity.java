//
// there is no such thing as a named filter (rule).  To modify a rule, need to find it by search for it's action and critera, then delete it, then recreate it.
// same issue with renaming a label.  It has to be deleted.
// what happens to the emails that are currently under that rule?

package com.screamatthewind.gmail.manager;

import com.google.api.services.admin.directory.Directory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Filter;
import com.google.api.services.gmail.model.FilterAction;
import com.google.api.services.gmail.model.FilterCriteria;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListFiltersResponse;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity {
  
  private static String mMasterEmail;
  
  private static String mAdminEmail = "helpdesk@screamatthewind.com";

  private static String userCsvFilename;            
  private static String mAdminSpreadsheetId;;
  private static String mApplicationName = "gmail-label-and-rule-copier";
  private static String mServiceAccountEmail = "gmail-label-and-rule-copier@project-id-9999.iam.gserviceaccount.com";

  private static GoogleApiUtilities myUtils = new GoogleApiUtilities();
  
  private static Gmail mMasterGmailService;
  private static Sheets mAdminSheetsService;
  private static Directory mAdminDirectoryService;
  
  private static List<String> mAllGmailUsers;
  
  private static List<String> sfcgLabels = new ArrayList<>(Arrays.asList("1-IMPORTANT", "2-ACTION", "3-FYI", "*Statuses"));
  
  public static void main(String[] args) throws Throwable {

    System.out.println("DID YOU RESET THE SPREADHSEET?");
    
    // resetSpreadhsheetForProduction();
    // normalProductionDeployment();
    validateUser("Levet@screamatthewind.com");
    // validateUser("sidriss@screamatthewind.com");
    System.out.println("Done");
  }
  
  public static void resetSpreadhsheetForProduction() throws Throwable {
    System.out.println("Resetting spreadsheet for production");

    mMasterEmail = "gmail_syncruleslabels@screamatthewind.com";
    userCsvFilename = "C:\\Users\\Bob\\Documents\\Projects\\JPI\\SFCG\\Test.csv";            
    mAdminSpreadsheetId = "9999";

    initialize();

    // clear spreadsheet
    Sheets adminSheetsService = myUtils.getSheetsService(mAdminEmail);
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Filters", null).execute();
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Labels", null).execute();

    deleteAllRulesAndFilters("asearcher@screamatthewind.com");

    mAllGmailUsers = GetUsersFromCSV.getUsers(userCsvFilename); // contains only asearcher.screamatthewind.com
    
    performNormalDeployment();
  }

  public static void validateUser(String emailToValidate) throws Throwable {
    System.out.println("Validating user: " + emailToValidate);

    mMasterEmail = emailToValidate;
    userCsvFilename = "C:\\Users\\Bob\\Documents\\Projects\\JPI\\SFCG\\Test.csv";            
    mAdminSpreadsheetId = "1-9999-ooNagz4jgF4"; // scratch spreadsheet
    
    initialize();

    // clear spreadsheet
    Sheets adminSheetsService = myUtils.getSheetsService(mAdminEmail);
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Filters", null).execute();
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Labels", null).execute();
    
    deleteAllRulesAndFilters("asearcher@screamatthewind.com");

    mAllGmailUsers = GetUsersFromCSV.getUsers(userCsvFilename);  // contains only asearcher.screamatthewind.com
    
    performNormalDeployment();  // to asearcher.screamatthewind.com
  }
  
  public static void normalProductionDeployment() throws Throwable {
    System.out.println("Performing normal production deployment");
    
    mMasterEmail = "gmail_syncruleslabels@screamatthewind.com";
    userCsvFilename = "C:\\Users\\Bob\\Documents\\Projects\\JPI\\SFCG\\Email-Rules.csv";            
    mAdminSpreadsheetId = "9999";

    initialize();

    mAllGmailUsers = GetUsersFromCSV.getUsers(userCsvFilename);
    
    performNormalDeployment();
  }

  public static void variousCleanups() {
    // deleteSFCGRulesAndFilters("accountsetup@screamatthewind.com");
    // deleteFiltersWithNoAction(userEmail);

  }
  
  // delete all labels and filters in sfcgLabels list
  public static void deleteSFCGRulesAndFilters(String userEmail) throws Throwable {

    Gmail userGmailService = myUtils.getGmailService(userEmail);
    List<Filter> gmailFilters = getGmailFilters(userGmailService, userEmail);

    if (gmailFilters != null) {
    
      for (Filter gmailFilter: gmailFilters) {
        FilterAction action = gmailFilter.getAction();
  
        List<String> addLabels = action.getAddLabelIds();
  
        for (String addLabel: addLabels) {
          for (String sfcgLabel: sfcgLabels) {
            if (addLabel.contains(sfcgLabel)) {
              System.out.println("addLabel: " + gmailFilter.getId());
              try {
                userGmailService.users().settings().filters().delete(userEmail, gmailFilter.getId()).execute();
              } catch (Exception e){
                // ignore errors
              }
            }
          }
        }
      
        List<String> removeLabels = action.getRemoveLabelIds();
  
        for (String removeLabel: removeLabels) {
          for (String sfcgLabel: sfcgLabels) {
            if (removeLabel.contains(sfcgLabel)) {
              System.out.println("removeLabel: " + gmailFilter.getId());
              try {
                userGmailService.users().settings().filters().delete(userEmail, gmailFilter.getId()).execute();
              } catch (Exception e){
                // ignore errors
              }
            }
          }
        }      
      }
    }
    
    List<Label> gmailLabels = myUtils.getGmailLabels(userGmailService, userEmail);
    
    for (Label gmailLabel: gmailLabels) {
      String labelName = gmailLabel.getName();

      for (String sfcgLabel: sfcgLabels) {
        if (labelName.contains(sfcgLabel)) {
          System.out.println("labelName: " + gmailLabel.getId());
          try {
            userGmailService.users().labels().delete(userEmail, gmailLabel.getId()).execute();
          } catch (Exception e){
            // ignore errors
          }
        }
      }
    }            
  }
  
  public static void performNormalDeployment() throws Throwable {
    System.out.println("Getting user list");
    mAdminDirectoryService = myUtils.getDirectoryService(mAdminEmail);
  
    backupUserInfo();
    
/*    for (User user: mAllGmailUsers) {
      deleteSFCGRulesAndFilters(user.getPrimaryEmail());
    }*/
    
    updateExistingLabelsAndFilters();
    addNewLabelsAndFilters();
  }
  
  public static void deleteAllRulesAndFilters(String userEmail) throws Throwable {
    Gmail userGmailService = myUtils.getGmailService(userEmail);
    Sheets adminSheetsService = myUtils.getSheetsService(mAdminEmail);
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Filters", null).execute();
    adminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Labels", null).execute();

    List<Filter> gmailFilters = getGmailFilters(userGmailService, userEmail);

    if (gmailFilters != null) {
    
      for (Filter gmailFilter: gmailFilters) {
        try {
          userGmailService.users().settings().filters().delete(userEmail, gmailFilter.getId()).execute();
        } catch (Exception e){
          // ignore errors
        }
      }
    }
    
    List<Label> gmailLabels = myUtils.getGmailLabels(userGmailService, userEmail);
    
    if (gmailLabels != null) {
    
      for (Label gmailLabel: gmailLabels) {
        String labelName = gmailLabel.getName();
  
        try {
          
          if (!labelName.equalsIgnoreCase(gmailLabel.getId()))
            userGmailService.users().labels().delete(userEmail, gmailLabel.getId()).execute();
        } catch (Exception e){
          // ignore errors
        }
      }
    }
  }
  
  
  public static void addNewLabelsAndFilters() throws Throwable {

    List<MyFilterClass> filterAdditions = generateFilterAdditions(mMasterGmailService, mMasterEmail);
    List<MyLabelClass> labelAdditions = generateLabelAdditions(mMasterGmailService, mMasterEmail);
    
    try {
      
      System.out.println("Adding filters and labels...");
      for (String userEmail: mAllGmailUsers) {

        System.out.println(userEmail);

        Gmail userGmailService = myUtils.getGmailService(userEmail);

        List<Filter> userGmailFilters = getGmailFilters(userGmailService, userEmail);
        List<Label> userGmailLabels = myUtils.getGmailLabels(userGmailService, userEmail);

        deleteGmailFilters(userGmailService, userEmail, userGmailFilters, filterAdditions);
        deleteGmailLabels(userGmailService, userGmailLabels, labelAdditions,  userEmail);
        addGmailLabels(userGmailService, userGmailLabels, labelAdditions, userEmail);
        renameGmailLabels(userGmailService, userGmailLabels, labelAdditions, userEmail);
        addGmailFilters(userGmailService, userEmail, userGmailFilters, filterAdditions);
      }
      
    }
    catch (Throwable exception) {
      exception.printStackTrace();
    }
  }
  
  public static void updateExistingLabelsAndFilters() throws Throwable {
    
    List<MyFilterClass> filterDeltas = generateFilterDeltas(mMasterGmailService, mMasterEmail, mAdminSheetsService, mAdminSpreadsheetId);
    List<MyLabelClass> labelDeltas = myUtils.generateLabelDeltas(mMasterGmailService, mMasterEmail, mAdminSheetsService, mAdminSpreadsheetId);

    try {
      
      System.out.println("Propagating filters...");
      for (String userEmail: mAllGmailUsers) {

        System.out.println(userEmail);

        Gmail userGmailService = myUtils.getGmailService(userEmail);

        List<Filter> userGmailFilters = getGmailFilters(userGmailService, userEmail);
        List<Label> userGmailLabels = myUtils.getGmailLabels(userGmailService, userEmail);

        deleteGmailFilters(userGmailService, userEmail, userGmailFilters, filterDeltas);
        deleteGmailLabels(userGmailService, userGmailLabels, labelDeltas,  userEmail);
        addGmailLabels(userGmailService, userGmailLabels, labelDeltas, userEmail);
        renameGmailLabels(userGmailService, userGmailLabels, labelDeltas, userEmail);
        addGmailFilters(userGmailService, userEmail, userGmailFilters, filterDeltas);
      }
      
    }
    catch (Throwable exception) {
      exception.printStackTrace();
    }

    updateMasterSpreadsheetGmailFilters(filterDeltas);
    myUtils.updateMasterSpreadsheetGmailLabels(mAdminSheetsService, mAdminSpreadsheetId, labelDeltas);
  }
  
  // delete filters with no action
  public static void deleteFiltersWithNoAction(String userEmail) throws Throwable {
    List<Filter> filters;
    
    Gmail userGmailService = myUtils.getGmailService(userEmail);

    try {
      ListFiltersResponse listResponse = userGmailService.users().settings().filters().list(userEmail).execute();
      filters = listResponse.getFilter();
      
      if (filters == null)
        return;
      
      for (Filter filter: filters) {
        FilterAction action = filter.getAction();
        String filterlId = filter.getId();
        
        if (action == null) {
          userGmailService.users().settings().filters().delete(userEmail, filterlId).execute();
        }
        else {
          List<String> addLabelIds = action.getAddLabelIds();
          if (addLabelIds != null) {
            if ((addLabelIds.size() == 1) && (addLabelIds.get(0).equalsIgnoreCase("STARRED"))) {
                filterlId = filter.getId();
                userGmailService.users().settings().filters().delete(userEmail, filterlId).execute();
            }
          }
        }
      }
                  
    } catch (IOException exception) {
      if (exception.getMessage().contains("Internal Server Error"))
        System.out.println("Error during getGmailFilters: Internal Server Error");
      else
        exception.printStackTrace();
    } 

    return;
  }
  
  
  public static void backupUserInfo() throws Throwable {

    System.out.println("Backing up users");
    
    Gson gson = new Gson();
    
    File fout = new File("/Users/Bob/Documents/file1.txt");
    FileOutputStream fos = new FileOutputStream(fout);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
    
    List<MyUserBackupClass> userBackupData = new ArrayList<MyUserBackupClass>();
        
    for (String userEmail: mAllGmailUsers) {

      System.out.println(userEmail);

      Gmail userGmailService = myUtils.getGmailService(userEmail);
      
      MyUserBackupClass userBackupInfo = new MyUserBackupClass();
      
      List<Filter> usersGmailFilters = getGmailFilters(userGmailService, userEmail);
      List<MyFilterInfoClass> filterData = new ArrayList<MyFilterInfoClass>();      
      
      if (usersGmailFilters != null) {
      
        for (Filter filter: usersGmailFilters) {
  
          MyFilterInfoClass filterInfo = new MyFilterInfoClass();
  
          filterInfo.mFilterId = filter.getId();
          filterInfo.mFilterAction = filterAction2JSON(filter.getAction());
          filterInfo.mFilterCriteria = filterCriteria2JSON(filter.getCriteria());
          filterInfo.mFilterText = filter.toString();        
  
          filterData.add(filterInfo);              
        }
      }
      
      List<Label> gmailLabels = myUtils.getUsersGmailLabels(userGmailService, userEmail);
      userBackupInfo.mLabels = gmailLabels;

      userBackupInfo.mEmailAddress = userEmail;
      userBackupInfo.mFilterData = filterData;

      bw.write(gson.toJson(userBackupInfo));
      bw.newLine();
    }
    
    bw.close();
  }
  
  public static void initializeMasterFilters() {
      
    try {

      System.out.println("Processing...");
      
      List<Filter> masterFilters = getGmailFilters(myUtils.getGmailService(mMasterEmail), mMasterEmail);
      initializeMasterSpreadsheetFilters(mAdminSheetsService, mAdminSpreadsheetId, masterFilters);
      
    } catch (Throwable exception) {
      exception.printStackTrace();
    }
  }


 public static List<MyFilterClass> getFiltersFromSpreadsheet(Sheets spreadsheetService, String spreadsheetId) {

    List<MyFilterClass> results = new ArrayList<MyFilterClass>();
    
    List<List<Object>> values = myUtils.getSpreadsheetData(spreadsheetService, spreadsheetId, "Filters");
    
    if (values != null && values.size() != 0) {

      for (List<Object> row : values) {
        
        String entryType = myUtils.safeGet(row, 0);

        if (entryType.equalsIgnoreCase("filter")) {

          String filterStatus = myUtils.safeGet(row, 1);
          String filterId = myUtils.safeGet(row, 2);
          String filterAction = myUtils.safeGet(row, 3);
          String filterCriteria = myUtils.safeGet(row, 4);
          String filterText = myUtils.safeGet(row, 5);
          String filterTouched = myUtils.safeGet(row, 6);
          
            MyFilterClass newFilter = new MyFilterClass(filterStatus, filterId, filterAction, filterCriteria, filterText, filterTouched);
            results.add(newFilter);
        }        
      }
    }
    
    return results;
  }

  private static List<String> convertLabels(List<Label> gmailLabels, List<String> labelIds) throws Throwable {
    
    List<String> newLabels = new ArrayList<String>();
    
    for (String labelId: labelIds) {
          String labelName = myUtils.getGmailLabelName(gmailLabels, labelId);
          newLabels.add(labelName);
    }
    
    return newLabels;
  }

  public static List<Filter> getGmailFilters(Gmail gmailService, String userEmail) throws Throwable {
    List<Filter> filters;
    
    try {
      List<Label> gmailLabels = myUtils.getGmailLabels(gmailService, userEmail);
      
      ListFiltersResponse listResponse = gmailService.users().settings().filters().list(userEmail).execute();
      filters = listResponse.getFilter();
      
      if (filters == null)
        return null;
      
      for (Filter filter: filters) {
        FilterAction action = filter.getAction();
        
        if (action != null) {
  
          List<String> addLabelIds = action.getAddLabelIds();
          List<String> removeLabelIds = action.getRemoveLabelIds();
  
          List<String> newAddLabels = new ArrayList<String>();
          if (addLabelIds != null) {
            for (String labelId: addLabelIds) {
              String labelName = myUtils.getGmailLabelName(gmailLabels, labelId);
              newAddLabels.add(labelName);
            }
          }
  
          List<String> newRemoveLabels = new ArrayList<String>();
          if (removeLabelIds != null) {
            for (String labelId: removeLabelIds) {
              String labelName = myUtils.getGmailLabelName(gmailLabels, labelId);
              newRemoveLabels.add(labelName);
            }
          }
  
          Collections.sort(newAddLabels);
          Collections.sort(newRemoveLabels);
          
          action.setAddLabelIds(newAddLabels);        
          action.setRemoveLabelIds(newRemoveLabels);        
        }
      }
                  
    } catch (IOException exception) {
      if (exception.getMessage().contains("Internal Server Error"))
        System.out.println("Error during getGmailFilters: Internal Server Error");
      else
        exception.printStackTrace();
      return null;
    } 

    return filters;
  }
 
  public static String createGmailFilter(Gmail gmailService, String userEmail, String filterAction, String filterCriteria) throws Throwable {

    Gmail mUserGmailService = myUtils.getGmailService(userEmail);
    List<Label> gmailLabels = myUtils.getGmailLabels(gmailService, userEmail);

    FilterAction newFilterAction = JSON2FilterAction(filterAction);
    FilterCriteria newFilterCriteria = JSON2FilterCriteria(filterCriteria);
    
    if (newFilterAction != null) {
    
      List<String> addLabelNames = newFilterAction.getAddLabelIds();
      List<String> removeLabelNames = newFilterAction.getRemoveLabelIds();
  
      List<String> newAddLabelIds = new ArrayList<String>();
      List<String> newRemoveLabelIds = new ArrayList<String>();
  
      if (addLabelNames != null) {
        for (String labelName: addLabelNames) {
          String labelId = myUtils.findGmailLabelByName(gmailLabels, labelName);
          newAddLabelIds.add(labelId);
        }
      }
  
      if (removeLabelNames != null) {
        for (String labelName: removeLabelNames) {
          String labelId = myUtils.findGmailLabelByName(gmailLabels, labelName);
          newRemoveLabelIds.add(labelId);
        }
      }
  
      Collections.sort(newAddLabelIds);
      Collections.sort(newRemoveLabelIds);
  
      newFilterAction.setAddLabelIds(newAddLabelIds);
      newFilterAction.setRemoveLabelIds(newRemoveLabelIds);
    
      Filter filter = new Filter()
            .setCriteria(newFilterCriteria)
            .setAction(newFilterAction);

      Filter result = null;
      
      try {
        result = mUserGmailService.users().settings().filters().create(userEmail, filter).execute();
      } catch (Throwable ex) {
        return null;
      }
  
      return result.getId();
    }
    
    return null;
  }  
  
  public static String filterCriteria2JSON(FilterCriteria filterCriteria) {

    Gson gson = new Gson();
    String json = gson.toJson(filterCriteria);

    return json;
}

private static FilterCriteria JSON2FilterCriteria(String json) {

  GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setLenient();

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(FilterCriteria.class, new FilterCriteriaDeserializer())
        .create();
    
    FilterCriteria filterCriteria = gson.fromJson(json, FilterCriteria.class);

    return filterCriteria;
}

public static String filterAction2JSON(FilterAction filterAction) {

  Gson gson = new Gson();
  String json = gson.toJson(filterAction);

  return json;
}

private static FilterAction JSON2FilterAction(String json) {
  Gson gson = new Gson();

  FilterAction filterAction = gson.fromJson(json, FilterAction.class);

  return filterAction;
}

    
  public static void initialize() {
    myUtils.setApplicationName(mApplicationName);
    myUtils.setServiceAccountEmail(mServiceAccountEmail);

    mMasterGmailService = myUtils.getGmailService(mMasterEmail);
    mAdminSheetsService = myUtils.getSheetsService(mAdminEmail);
  }          

  public static void initializeMasterSpreadsheetFilters(Sheets sheetsService, String spreadsheetId, List<Filter> masterFilters) throws IOException, Throwable {
    
    List<List<Object>> values = new ArrayList<List<Object>>();
    
    int rows = 0;

    
    for (Filter filter: masterFilters) {
      
      List<Object> row = new ArrayList<Object>() {

        private static final long serialVersionUID = 1L;

        {
            String filterId = filter.getId();
            
            String saveFilterId = filter.getId();
            String filterText = filter.setId("").toString(); // normalize id for comparison
            filter.setId(saveFilterId);
            
            String filterAction = filterAction2JSON(filter.getAction());
            String filterCriteria = filterCriteria2JSON(filter.getCriteria());
             
            add(new String("Filter"));
            add(new String("Add"));
            add(new String(filterId)); 
            add(new String(filterAction));
            add(new String(filterCriteria));
            add(new String(filterText));
            add(new String("")); // old text
            add(new String("N")); // has row been touched?
        }
      };
      
      values.add(row);
      rows++;
    }

    ValueRange cellData = new ValueRange();
    cellData.setValues(values);
    
    
    try {
      sheetsService.spreadsheets().values().clear(spreadsheetId, "Filters", null).execute();
    } catch (IOException exception) {
      // unable to clear spreadhsheet
    }
    
    try {
      sheetsService.spreadsheets().values().append(spreadsheetId, "Filters!A1:H" + rows, cellData)
          .setValueInputOption("RAW")
          .execute();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    
    // System.out.println(result);
  }
  
/*  public static void replicateGmailFilters() {
    try {
      
      System.out.println("Propagating filters...");
      for (User user: mAllGmailUsers) {

        String userEmail = user.getPrimaryEmail();
        System.out.println(userEmail);
          
        propagateGmailFilters(userEmail, mAdminSheetsService, mAdminSpreadsheetId);
      }
      
    }
    catch (Throwable exception) {
      exception.printStackTrace();
    }
  }*/

  public static void addGmailLabels(Gmail userGmailService, List<Label> userGmailLabels, List<MyLabelClass> labelDeltas, String userEmail) throws Throwable {
    
    for (MyLabelClass label: labelDeltas) {
      
      String status = label.getStatus();
      if (!status.equalsIgnoreCase("ignore")) {
        
        String labelName = label.getName();
        String labelId = myUtils.findGmailLabelByName(userGmailLabels, labelName);
  
        if (status.equalsIgnoreCase("add")) {
          if (labelId == null)
            myUtils.createGmailLabel(userGmailService, userEmail, labelName);
          
        } 
      }                    
    }
  }  

  public static void deleteGmailLabels(Gmail userGmailService, List<Label> userGmailLabels, List<MyLabelClass> labelDeltas, String userEmail) throws Throwable {
    
    for (MyLabelClass label: labelDeltas) {
      
      String status = label.getStatus();
      if (!status.equalsIgnoreCase("ignore")) {
        
        String labelName = label.getName();
        String labelId = myUtils.findGmailLabelByName(userGmailLabels, labelName);
  
        if (status.equalsIgnoreCase("remove")) {
          if (labelId != null)
            myUtils.deleteGmailLabelByName(userGmailService, userEmail, labelName);
          
        } 
      }                    
    }
  }  

  public static void renameGmailLabels(Gmail userGmailService, List<Label> userGmailLabels, List<MyLabelClass> labelDeltas, String userEmail) throws Throwable {
    
    for (MyLabelClass label: labelDeltas) {
      
      String status = label.getStatus();
      if (!status.equalsIgnoreCase("ignore")) {
        
        String labelName = label.getName();
        String labelId = myUtils.findGmailLabelByName(userGmailLabels, labelName);
  
        if (status.equalsIgnoreCase("rename")) {
           labelId = myUtils.findGmailLabelByName(userGmailLabels, label.getOldName());
           if (labelId != null)
             myUtils.updateGmailLabel(userGmailService, userEmail, labelId, labelName);            
        }
      }                    
    }
  }  

  
  public static List<MyLabelClass> generateLabelAdditions(Gmail gmailService, String emailAddress) throws Throwable {
    
    List<Label> masterGmailLabels = myUtils.getGmailLabels(gmailService, emailAddress);
    List<MyLabelClass> labelAdditions = new ArrayList<MyLabelClass>();
  
    for (Label gmailLabel: masterGmailLabels) {
      
        MyLabelClass label = myUtils.myLabelClass.addLabel(labelAdditions, gmailLabel.getId(), gmailLabel.getName(), "Add");
            
        label.setTouched(true);        
    }
  
    return labelAdditions;
  }

  
  public static List<MyFilterClass> generateFilterAdditions(Gmail gmailService, String emailAddress) throws Throwable {
    
    List<Filter> masterGmailFilters = getGmailFilters(gmailService, emailAddress);
    List<MyFilterClass> filterAdditions = new ArrayList<MyFilterClass>();
  
    if (masterGmailFilters != null) {
    
      for (Filter gmailFilter: masterGmailFilters) {
        
        String saveFilterId = gmailFilter.getId();
        
        String filterAction = filterAction2JSON(gmailFilter.getAction());
        String filterCriteria = filterCriteria2JSON(gmailFilter.getCriteria());
        
        MyFilterClass masterFilter = MyFilterClass.addFilter(filterAdditions, gmailFilter.getId(), filterAction, filterCriteria, gmailFilter.setId("").toString(), "Add");
    
        gmailFilter.setId(saveFilterId);
      
        masterFilter.setTouched(true);        
      }
    }
    
    return filterAdditions;
  }
  
  public static List<MyFilterClass> generateFilterDeltas(Gmail gmailService, String emailAddress, Sheets sheetsService, String spreadsheetId) throws Throwable {
    
    List<Filter> masterGmailFilters = getGmailFilters(gmailService, emailAddress);
    List<MyFilterClass> masterSheetsAllFilters = getFiltersFromSpreadsheet(sheetsService, spreadsheetId);
  
    if (masterGmailFilters != null) {
    
      for (Filter gmailFilter: masterGmailFilters) {
        
        String saveFilterId = gmailFilter.getId();
        
        MyFilterClass masterFilter = MyFilterClass.findFilterById(masterSheetsAllFilters, gmailFilter.getId());
    
        if (masterFilter == null) {
          String filterAction = filterAction2JSON(gmailFilter.getAction());
          String filterCriteria = filterCriteria2JSON(gmailFilter.getCriteria());
          
          if (filterAction.contains("null"))
            masterFilter = MyFilterClass.addFilter(masterSheetsAllFilters, gmailFilter.getId(), filterAction, filterCriteria, gmailFilter.setId("").toString(), "Remove");
          else
            masterFilter = MyFilterClass.addFilter(masterSheetsAllFilters, gmailFilter.getId(), filterAction, filterCriteria, gmailFilter.setId("").toString(), "Add");
        }
        else {
            if (masterFilter.getText().equalsIgnoreCase(gmailFilter.setId("").toString())) 
              masterFilter.setStatus("Done");
            
             else {             
               masterFilter.setStatus("Rename");
               masterFilter.setOldText(masterFilter.getText());
               masterFilter.setText(gmailFilter.setId("").toString());
             }
          }
    
          gmailFilter.setId(saveFilterId);
        
          masterFilter.setTouched(true);        
      }
    }
    
    for (MyFilterClass filter: masterSheetsAllFilters) {      
      if (!filter.isTouched())
        filter.setStatus("Remove");
    }
  
    return masterSheetsAllFilters;
  }

  public static String findGmailFilterByText(List<Filter> userGmailFilters, String filterText) {

    if (userGmailFilters == null)
      return null;
    
    for (Filter filter : userGmailFilters) {
      String saveFilterId = filter.getId();
      String userFilterText = filter.setId("").toString();
      filter.setId(saveFilterId);
      
      if (userFilterText.equalsIgnoreCase(filterText)) {
        return filter.getId();
      }
    }

    return null;
  }


  public static void deleteGmailFilters(Gmail userGmailService, String userEmail, List<Filter> userGmailFilters, List<MyFilterClass> filterDeltas) throws Throwable {
    
    for (MyFilterClass filter: filterDeltas) {
      
      String status = filter.getStatus();
        
      String filterText = filter.getText();
      String filterId = findGmailFilterByText(userGmailFilters, filterText);

      if (status.equalsIgnoreCase("remove")) {
        if (filterId != null)
          deleteGmailFilterByText(userGmailService, userEmail, filterText);
      } 
    }                    
  }

  public static void addGmailFilters(Gmail userGmailService, String userEmail, List<Filter> userGmailFilters, List<MyFilterClass> filterDeltas) throws Throwable {
    
    for (MyFilterClass filter: filterDeltas) {
      
      String status = filter.getStatus();
        
      String filterText = filter.getText();
      String filterId = findGmailFilterByText(userGmailFilters, filterText);

      if (status.equalsIgnoreCase("add")) {
        if (filterId == null)
          createGmailFilter(userGmailService, userEmail, filter.getAction(), filter.getCriteria());
      } 
    }                    
  }


/*  public static void propagateGmailFilters(String userEmail, Sheets spreadsheetService, String spreadsheetId) throws Throwable {
    
    Gmail mUserGmailService = myUtils.getGmailService(userEmail);
    List<Filter> userGmailFilters = getGmailFilters(mUserGmailService, userEmail);
    
    for (MyFilterClass filter: mFilterDeltas) {
      
      String status = filter.getStatus();
        
      String filterText = filter.getText();
      String filterId = findGmailFilterByText(userGmailFilters, filterText);

      if (status.equalsIgnoreCase("add")) {
        if (filterId == null)
          createGmailFilter(mUserGmailService, userEmail, filter.getAction(), filter.getCriteria());
        
      } else if (status.equalsIgnoreCase("remove")) {
        if (filterId != null)
          deleteGmailFilterByText(mUserGmailService, userEmail, filterText);
        
      } else if (status.equalsIgnoreCase("rename")) {
//        filterId = findGmailFilterByText(userGmailFilters, filter.getOldText());
//         updateGmailFilter(mUserGmailService, userEmail, filterId, filterText);            
      }
    }                    
  }*/

  public static void updateMasterSpreadsheetGmailFilters(List<MyFilterClass> filters) {
    
    List<List<Object>> values = new ArrayList<List<Object>>();
    
    int rows = 0;

    for (MyFilterClass filter: filters) {
      
      List<Object> row = new ArrayList<Object>() {

        private static final long serialVersionUID = 1L;

        {
          add(new String("Filter"));
          add(filter.getStatus());
          add(filter.getId()); 
          add(filter.getAction());
          add(filter.getCriteria());
          add(filter.getText());
          add(filter.getOldText());
          add(new String("Y")); // has row been touched?
        }
      };
      
      values.add(row);
      rows++;
    }

    ValueRange cellData = new ValueRange();
    cellData.setValues(values);
    
    try {
      mAdminSheetsService.spreadsheets().values().clear(mAdminSpreadsheetId, "Filters", null).execute();
    } catch (IOException exception) {
      // can't clear sheet
    }

    try {
      mAdminSheetsService.spreadsheets().values().append(mAdminSpreadsheetId, "Filters!A1:H" + rows, cellData)
          .setValueInputOption("RAW")
          .execute();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
 }
    
  public static void deleteGmailFilterByText(Gmail gmailService, String userEmail, String filterText) throws Throwable{
    String filterlId = findGmailFilterByText(gmailService, userEmail, filterText);
    
    if (filterlId != null)
      gmailService.users().settings().filters().delete(userEmail, filterlId).execute();
  }

  public static String findGmailFilterByText(Gmail gmailService, String userEmail, String filterText) throws Throwable {
    String result = null;
   
    List<Filter> gmailFilters = getGmailFilters(gmailService, userEmail);
    
    if (gmailFilters != null) {
    
      if (gmailFilters.size() != 0) {
        for (Filter filter : gmailFilters) {

          String saveFilterId = filter.getId();
          String userFilterText = filter.setId("").toString();
          filter.setId(saveFilterId);
          
          if (userFilterText.equalsIgnoreCase(filterText)) {
            result = filter.getId();
            break;
          }
        }
      }
    }
    
    return result;
  }

  
/*  public static void replicateGmailLabels() {
    try {
      
      List<MyLabelClass> labelDeltas = myUtils.generateLabelDeltas(mMasterGmailService, mMasterEmail, mAdminSheetsService, mAdminSpreadsheetId);

      System.out.println("Propagating labels...");
      for (User user: mAllGmailUsers) {

        String userEmail = user.getPrimaryEmail();
        System.out.println(userEmail);
          
        myUtils.propagateGmailLabels(labelDeltas, userEmail, mAdminSheetsService, mAdminSpreadsheetId);
      }
      
      myUtils.updateMasterSpreadsheetGmailLabels(mAdminSheetsService, mAdminSpreadsheetId, labelDeltas);

    }
    catch (Throwable exception) {
      exception.printStackTrace();
    }
  }*/
}
