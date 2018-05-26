package com.screamatthewind.gmail.manager;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class GetUsersFromCSV  {
	
		public static List<String> getUsers(String filename) {
	
		List<String> result = new ArrayList<String>();
			
		Reader in;
		try {
			in = new FileReader(filename);
		
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			for (CSVRecord record : records) {
			    String name = record.get("Name");
			    String email = record.get("Mail");
			    
			    result.add(email);
			    
			    // System.out.println(email);
			}
	
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;
		}
		return result;
	}
}
