package calypsox.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.NavigableSet;
import java.util.TreeMap;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

public class ForexClearFileReader {
	
	private static final String FAIL = "fail";
	private static final String OK = "ok";
	private static final String EMPTY = "";
	private static final String EMPTY_VALUE = "#EMPTY";
	protected HashMap<String, ArrayList<String>> map; // sonar 03/01/2018
	protected ArrayList<String> keys; // sonar 03/01/2018
	
	public ForexClearFileReader(String path, String fileName, JDate valueDate, String separator, ArrayList<String> errors) {
		map = new HashMap<String, ArrayList<String>>();
		keys = new ArrayList<String>();
		
		File file = getFile(path, fileName, errors);
		if(file != null){
			processFile(file,separator,errors);
		}
	}
	
	public HashMap<String, ArrayList<String>> getMap(){
		return map;
	}
	
	public ArrayList<String> getKeys(){
		return keys;
	}
	
	public HashMap<String,String> getLine(int position){
		HashMap<String,String> line = new HashMap<String,String>();
		
		ArrayList<String> auxList = new ArrayList<String>();
		
		for(String key : keys){
			auxList = map.get(key);
			if(auxList != null && auxList.size() >= position){
				line.put(key, auxList.get(position));
			}			
		}		
		return line;
	}
	
	public int getLinesSize(){
		return map.get(keys.get(0)).size();
	}
	
	public String getValue(String columnName, int line){		
		ArrayList<String> list = map.get(columnName);		
		return !Util.isEmpty(list) && list.size() > line ? list.get(line) : null;
	}
	
	private File getFile(String path, String fileName, ArrayList<String> errors){		
		
		final String fileNameFilter = fileName;
		// name filter
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				return fileName.toLowerCase().startsWith(fileNameFilter.toLowerCase());
			}
		};

		final File directory = new File(path);
		final File[] listFiles = directory.listFiles(filter);
		
		TreeMap<Long, File> map = new TreeMap<Long, File>();

		if(listFiles != null && listFiles.length > 0){
			for (File file : listFiles) {
				if(file.isFile()){
					final Long dateFileMilis = file.lastModified();
					map.put(dateFileMilis, file);
				}			
			}
			
			if(!map.isEmpty()){
				NavigableSet<Long> set = map.descendingKeySet();
				Long key = set.first();			
				File file = (File)map.get(key);
				return file;
			}
		}else{
			errors.add("Error. No se ha encontrado el fichero con nombre:" + fileName);
		}		
		return null;
	}
	
	private void processFile(File file, String separator, ArrayList<String> errors) {
		BufferedReader inputFileStream = null;
		String line = null;
		
		try {
			inputFileStream = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8")); // sonar
																											// 04/01/2018
			boolean first = true;
			while ((line = inputFileStream.readLine()) != null) {
				if (first) {
					processHeader(line, separator);
					first = false;
				} else {
					processLine(line, separator);
				}

			}
		} catch (FileNotFoundException e) {
			Log.error(this, e); //sonar
			errors.add("Error getting file: " + e.getMessage() + "\n");
		} catch (IOException e) {
			Log.error(this, e); //sonar
			errors.add("Error reading file: " + e.getMessage() + "\n");
		} finally {
			if (inputFileStream != null) {
				try {
					inputFileStream.close();
				} catch (final IOException e) {
					Log.error(this, e); //sonar
					errors.add("Error closing file: " + e.getMessage() + "\n");
				}
			}
		}
	}
	
	private void processHeader(String line, String separator) {
		String[] values = line.split("\\" + separator, -1);
		for(String s : values){
			map.put(s, new ArrayList<String>());
			keys.add(s);
		}
	}
	
	private void processLine(String line, String separator) {
		String[] values = line.split("\\" + separator, -1);
		for(int i = 0; i < values.length; i++){
			ArrayList<String> val = map.get(keys.get(i));
			if(EMPTY_VALUE.equals(values[i])){
				val.add(EMPTY);
			}else{
				val.add(values[i]);
			}			
		}
	}
	
	public static void postProcess(final boolean success, JDate d, String fileName, String filePath) throws IOException {
		String time = "";
		synchronized (ForexClearSTUtil.timeFormat) {
			final Date da = new Date();
            time = ForexClearSTUtil.timeFormat.format(da);
		}
		final String inputFileName = filePath + fileName;

		if (success) {
			Log.info(ForexClearFileReader.class, "Feed has been processed successfully. No bad entries found.");
			final String outputFileName = filePath + OK + File.separator + fileName + "_" + time;
			try {
				FileUtility.moveFile(inputFileName, outputFileName);
			} catch (final IOException e) {
				Log.error(ForexClearFileReader.class, e.getMessage(), e);
				throw e;
			}
		} else {
			Log.info(ForexClearFileReader.class, "Failed to process the file.");
			final String badFileName = filePath + FAIL + File.separator + fileName + "_" + time;
			try {
				FileUtility.moveFile(inputFileName, badFileName);
			} catch (final IOException e) {
				Log.error(ForexClearFileReader.class, e.getMessage(), e);
				throw e;
			}
		}
	}
	
	public static boolean copyFile(String path, String fileName){		
		try {
			final String outputFileName = path + File.separator + "copy" + File.separator + fileName;
			FileUtility.copyFile(path + File.separator + fileName, outputFileName);
		} catch (IOException e) {
			Log.error(ForexClearFileReader.class, e);
			return false;
		}
		return true;
	}
}
