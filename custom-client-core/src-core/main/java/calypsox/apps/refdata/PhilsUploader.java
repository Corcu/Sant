package calypsox.apps.refdata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.calypso.tk.core.DateRoll;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.DayCount;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PeriodRule;
import com.calypso.tk.core.StubRule;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.FutureContract;
import com.calypso.tk.refdata.FXReset;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.ui.component.button.JButton;
import com.calypso.ui.image.ImageUtilities;

import calypsox.apps.refdata.pmm.PMMCommon;
import calypsox.apps.refdata.pmm.PMMHandlerInterface;
import calypsox.apps.refdata.pmm.SettersData;
import calypsox.apps.refdata.pmm.UploaderData;


/**
 * @author Phil
 */
public class PhilsUploader extends JFrame {
	private static final long serialVersionUID = 1L;

	static JPanel MAIN_PANEL;
	static JTextPane TEXT_AREA;
	static JButton LOAD_FILE_BUTTON;
	static JButton LOAD_ELEMENTS_BUTTON;
	static JButton MODIFY_ELEMENTS_BUTTON;
	static JButton SAVE_ELEMENTS_BUTTON;
	static JButton DELETE_ELEMENTS_BUTTON;
	static JTextField FILE_TEXT_FIELD;
	static JButton HELP_BUTTON;
	static JButton REFRESH_BUTTON;
	static JCheckBox DELETE_MODE_BUTTON;
	static JLabel LOGO;
	private static final String LOGO_PATH = "/calypsox/apps/icons/pmm.png";
	private static final String ICON_PATH = "/calypsox/apps/icons/pmm_icon.png";
	static String LAST_USED_PATH = System.getProperty("user.home");
	static final String CONFIG_SHEET_NAME = "config";
	static final String APP_NAME = "PMM";
	static final String WINDOW_TITLE = APP_NAME + " - Phil's Massive Modifier";
	File SELECTED_FILE = null;
	
	PMMHandlerInterface PMMHandler = null;
	
	public PhilsUploader(boolean exitOnClose) {
		launch(null);
	}

	public PhilsUploader(String[] args) {
		launch(args);
	}
	
	public PhilsUploader() {
		this(true);
	}
	
	public PhilsUploader(String title) throws Exception {
		this(true);
		this.setTitle(title);
	}
	
	
	private JButton getButton(String iconPath) {
		ImageIcon icon = ImageUtilities.getIcon(iconPath);
        final JButton button = new JButton();
        button.setBorderPainted(false);
        button.setBorder(null);
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setIcon((icon));
        
        return button;
    }

	private void launch(String[] args) {
		setTitle(WINDOW_TITLE);
		getContentPane().setLayout((LayoutManager)null);
		setSize(685, 340);
		setVisible(false);
		setResizable(false);
		setPreferredSize(new Dimension(450, 110));
		
		URL iconurl = this.getClass().getResource(ICON_PATH);
        ImageIcon icon = new ImageIcon(iconurl);
        setIconImage(icon.getImage());
		
		// Create All
		MAIN_PANEL = new JPanel();
		LOAD_FILE_BUTTON = new JButton();
		LOAD_ELEMENTS_BUTTON = new JButton();
		MODIFY_ELEMENTS_BUTTON = new JButton();
		SAVE_ELEMENTS_BUTTON = new JButton();
		DELETE_ELEMENTS_BUTTON = new JButton();
		HELP_BUTTON = getButton("com/calypso/icons/custom/help24.gif");
		REFRESH_BUTTON = getButton("com/calypso/icons/refresh.png");
		DELETE_MODE_BUTTON = new JCheckBox("Delete Mode");
		FILE_TEXT_FIELD = new JTextField();
		TEXT_AREA = new JTextPane();
		LOGO = new JLabel();
		JScrollPane scrollPane = new JScrollPane(TEXT_AREA);
		
		// Main Panel
		MAIN_PANEL.setLayout((LayoutManager)null);
		MAIN_PANEL.setBounds(0, 0, 675, 300);
		MAIN_PANEL.setBackground(Color.WHITE);
		
		// Load Button
		LOAD_FILE_BUTTON.setText("1. File");
		LOAD_FILE_BUTTON.setBounds(5, 5, 90, 24);
		LOAD_FILE_BUTTON.addActionListener(this::loadInputFileButton_actionPerformed);
		
		// Load elements Button
		LOAD_ELEMENTS_BUTTON.setText("2. Load");
		LOAD_ELEMENTS_BUTTON.setEnabled(false);
		LOAD_ELEMENTS_BUTTON.setBounds(5, 30, 90, 24);
		LOAD_ELEMENTS_BUTTON.addActionListener(this::loadElementsButton_actionPerformed);
		
		// Modify Button
		MODIFY_ELEMENTS_BUTTON.setText("3. Modify");
		MODIFY_ELEMENTS_BUTTON.setEnabled(false);
		MODIFY_ELEMENTS_BUTTON.setBounds(105, 30, 90, 24);
		MODIFY_ELEMENTS_BUTTON.addActionListener(this::modifyElementsButton_actionPerformed);
		
		// Save Button
		SAVE_ELEMENTS_BUTTON.setText("4. Save");
		SAVE_ELEMENTS_BUTTON.setEnabled(false);
		SAVE_ELEMENTS_BUTTON.setBounds(205, 30, 90, 24);
		SAVE_ELEMENTS_BUTTON.addActionListener(this::saveElementsButton_actionPerformed);
		
		// Delete Button
		DELETE_ELEMENTS_BUTTON.setText("2. Delete");
		DELETE_ELEMENTS_BUTTON.setEnabled(false);
		DELETE_ELEMENTS_BUTTON.setVisible(false);
		DELETE_ELEMENTS_BUTTON.setBounds(5, 30, 90, 24);
		DELETE_ELEMENTS_BUTTON.setBackground(Color.RED);
		DELETE_ELEMENTS_BUTTON.setContentAreaFilled(false);
		DELETE_ELEMENTS_BUTTON.setOpaque(true);
		DELETE_ELEMENTS_BUTTON.addActionListener(this::deleteElementsButton_actionPerformed);
		
		// Help Button
		HELP_BUTTON.setBounds(640, 5, 24, 24);
		HELP_BUTTON.addActionListener(this::helpButton_actionPerformed);
		
		// File Text Field
		FILE_TEXT_FIELD.setBounds(105, 5, 200, 24);
		FILE_TEXT_FIELD.setEditable(false);
		
		// Refresh Button
		REFRESH_BUTTON.setBounds(305, 5, 24, 24);
		REFRESH_BUTTON.addActionListener(this::refreshButton_actionPerformed);
		
		
		// Delete Mode Button
		DELETE_MODE_BUTTON.setBounds(450, 5, 90, 24);
		DELETE_MODE_BUTTON.setContentAreaFilled(false);
		DELETE_MODE_BUTTON.addActionListener(this::deleteModeButton_actionPerformed);

		// Text Area
		TEXT_AREA.setEditable(false);
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		scrollPane.setBounds(5, 60, 660, 235);
		
		// Logo
		URL logourl = this.getClass().getResource(LOGO_PATH);
        ImageIcon logo = new ImageIcon(logourl);
        LOGO.setIcon(logo);
        LOGO.setBounds(250, 70, 200, 200);
        
		// Add All 
		getContentPane().add(MAIN_PANEL);
		MAIN_PANEL.add(FILE_TEXT_FIELD);
		MAIN_PANEL.add(LOAD_FILE_BUTTON);
		MAIN_PANEL.add(LOAD_ELEMENTS_BUTTON); 
		MAIN_PANEL.add(MODIFY_ELEMENTS_BUTTON);
		MAIN_PANEL.add(SAVE_ELEMENTS_BUTTON);
		MAIN_PANEL.add(DELETE_ELEMENTS_BUTTON);
		MAIN_PANEL.add(HELP_BUTTON);
		MAIN_PANEL.add(REFRESH_BUTTON);
		MAIN_PANEL.add(DELETE_MODE_BUTTON);
		MAIN_PANEL.add(scrollPane, -1);
		MAIN_PANEL.add(LOGO, 1);
		
		PMMCommon.DS = DSConnection.getDefault();
	}

	public static void addTextToTextArea(String msg, String type) {
		addTextToTextArea(msg, type, 0);
	}
	
	public static void addTextToTextArea(String msg, String type, int padding) {
		switch(type) {
		case PMMCommon.TYPE_INFO:
			Log.info(APP_NAME, msg);
			break;
		case PMMCommon.TYPE_WARNING:
			Log.warn(APP_NAME, msg);
			break;
		case PMMCommon.TYPE_ERROR:
			Log.error(APP_NAME, msg);
			break;
		}
		
		if (TEXT_AREA != null) {
			StringBuilder sb = new StringBuilder();
			while (sb.length() < padding) {
		        sb.append(' ');
		    }
			if (!Util.isEmpty(type)) {
				sb.append("[");
				sb.append(type);
				sb.append("] ");
			}
			sb.append(msg);
			sb.append("\n");
			
			try {
				StyledDocument doc = TEXT_AREA.getStyledDocument();
				doc.insertString(doc.getLength(), sb.toString(), getAttributeSet(type) );
			} catch (BadLocationException e) {
				// nothing
			}
			
			TEXT_AREA.setCaretPosition(TEXT_AREA.getDocument().getLength());
		}
	}
	
	private static SimpleAttributeSet getAttributeSet (String type) {
		SimpleAttributeSet sas = new SimpleAttributeSet();
		
		switch (type) {
		case PMMCommon.TYPE_ERROR:
			StyleConstants.setForeground(sas, Color.RED);
			StyleConstants.setBackground(sas, Color.YELLOW);
			StyleConstants.setBold(sas, true);
			break;
		
		case PMMCommon.TYPE_WARNING:
			StyleConstants.setForeground(sas, Color.MAGENTA);
			StyleConstants.setBackground(sas, Color.WHITE);
			StyleConstants.setBold(sas, true);
			break;
		
		case PMMCommon.TYPE_INFO:
		default:
			StyleConstants.setForeground(sas, Color.BLACK);
			StyleConstants.setBackground(sas, Color.WHITE);
			StyleConstants.setBold(sas, false);
			break;
		}
		
		return sas;
	}

	private void saveElements() {
		if (PMMCommon.DS != null) {
			addTextToTextArea("", "");
			addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
			addTextToTextArea("Step 5 : Saving elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
				
			PMMHandler.saveElements();
			
			addTextToTextArea("Finished saving elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		}
	}
	
	private void deleteElements() {
		if (PMMCommon.DS != null) {
			addTextToTextArea("", "");
			addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
			addTextToTextArea("Step 5 : Deleting elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
				
			PMMHandler.deleteElements();
			
			addTextToTextArea("Finished deleting elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		}
	}

	private void modifyElements() {
		addTextToTextArea("", ""); 
		addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
		addTextToTextArea("Step 4 : Modifying elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		PMMCommon.MODIFIED_ELEMENTS = new HashMap<String, Object>();

		for (Map.Entry<SettersData, List<UploaderData>> entry : PMMCommon.UPLOADER_DATA_MAP.entrySet()) {
			SettersData settersData = entry.getKey();
			List<UploaderData> uploaderDataList = entry.getValue();
			
			StringBuilder sb = new StringBuilder();
			sb.append("Modifying ");
			sb.append(settersData.getConfigNiceName());
			sb.append(" for ");
			sb.append(uploaderDataList.size());
			sb.append(" elements.");
			addTextToTextArea(sb.toString(), PMMCommon.TYPE_INFO, 2);
			
			List<String> notFoundElements = new ArrayList<String>();

			for (UploaderData uploaderData : uploaderDataList) {
				String elementIdentifier = uploaderData.getElementString();
				Object element = PMMCommon.MODIFIED_ELEMENTS.get(elementIdentifier);
				
				if (element == null) {
					try {
						element = PMMCommon.MODIFIABLE_ELEMENTS.get(elementIdentifier);
						if (element == null) {
							notFoundElements.add(elementIdentifier);
							continue;
						}
						// We clone the element for later modifications
						element = PMMHandler.cloneObject(element);
					} catch (CloneNotSupportedException e) {
						String msg = settersData.getConfigNiceName() + ": Error cloning element " + elementIdentifier;
						addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 4);
						continue;
					}
					if (element == null) {
						notFoundElements.add(elementIdentifier);
						continue;
					}
				}
				
				try {
					if (settersData.getBasicDataType().equals("HASHTABLE")) {
						Hashtable origHT = (Hashtable)settersData.getGetterMethod().invoke(PMMHandler.getObjectClass().cast(element));
						Hashtable newHT = (Hashtable)uploaderData.getData();
						Hashtable finalHT = new Hashtable();
						if (origHT != null) {
							finalHT.putAll(origHT);
						}
						if (newHT != null) {
							finalHT.putAll(newHT);
						}
						uploaderData.setData(finalHT);
					}
					else if (settersData.getBasicDataType().equals("LIST")) {
						Vector origL = (Vector)settersData.getGetterMethod().invoke(PMMHandler.getObjectClass().cast(element));
						List newL = (List)uploaderData.getData();
						List finalL = new ArrayList();
						if (origL != null) {
							finalL.addAll(origL);
						}
						if (newL != null) {
							finalL.addAll(newL);
						}
						uploaderData.setData(finalL);
					}
					if (settersData.isPrim()) {
						if (settersData.getSetterDataTypeClass() == int.class) {
							int param = (int)((Double)uploaderData.getData()).intValue();
							settersData.getSetterMethod().invoke(PMMHandler.getObjectClass().cast(element), param);
						}
						else if (settersData.getSetterDataTypeClass() == long.class) {
							long param = (long)((Double)uploaderData.getData()).longValue();
							settersData.getSetterMethod().invoke(PMMHandler.getObjectClass().cast(element), param);
						}
						else if (settersData.getSetterDataTypeClass() == double.class) {
							double param = (double)uploaderData.getData();
							settersData.getSetterMethod().invoke(PMMHandler.getObjectClass().cast(element), param);
						}
						else if (settersData.getSetterDataTypeClass() == boolean.class) {
							settersData.getSetterMethod().invoke(PMMHandler.getObjectClass().cast(element), uploaderData.getData());
						}
					}
					else {
						settersData.getSetterMethod().invoke(PMMHandler.getObjectClass().cast(element), uploaderData.getData());
					}
				}
				catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
					String msg = settersData.getConfigName() + ": Error invoking method :" + settersData.getSetterMethodName() + " - " + settersData.getSetterDataTypeName() + " - " + elementIdentifier + " - " + e.toString();
					addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
					continue;
				}
				PMMCommon.MODIFIED_ELEMENTS.put(elementIdentifier, element);
			}
			
			if (notFoundElements.size() > 0) {
				addTextToTextArea("The following elements could not be found for modification: " + notFoundElements.toString(), PMMCommon.TYPE_WARNING, 4);
			}
		}
		addTextToTextArea("Finished modifying elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
	}



	private void loadElements() {
		addTextToTextArea("", "");
		addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
		addTextToTextArea("Step 3 : Loading elements to modify @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		
		PMMCommon.MODIFIABLE_ELEMENTS = new HashMap<String, Object>();
		if (PMMCommon.DS != null) {
			int roundCount = 1;
			int totalWanted = PMMCommon.ELEMENTS_IDENTIFIERS.size();
			
			try {
				List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
				
				for (List<String> wantedElementsList : splitCollection) {
					if (Util.isEmpty(wantedElementsList)) {
						continue;
					}
					
					Vector<?> foundElements = PMMHandler.loadElements(wantedElementsList);

					StringBuilder sb = new StringBuilder();
					sb.append("Round ");
					sb.append(roundCount);
					sb.append("/");
					sb.append(splitCollection.size()); 
					sb.append(" - ");
					sb.append("Loaded ");
					sb.append(foundElements.size());
					sb.append(" elements.");
					addTextToTextArea(sb.toString(), PMMCommon.TYPE_INFO, 2);
					
					List<String> wantedElementsListCopy = wantedElementsList.stream()
	                           .collect(Collectors.toList());
					PMMHandler.checkLoadedElements(wantedElementsListCopy, foundElements);
					
					roundCount++;
				}
			} catch (RemoteException e) {
				String msg = "Cannot get products";
				addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
				return;
			}
			
			int totalFound =  PMMCommon.MODIFIABLE_ELEMENTS.size();
			addTextToTextArea("Found and loaded a total of " + totalFound + " elements.", PMMCommon.TYPE_INFO);
			
			int notFoundTotal = totalWanted - totalFound;
			if (notFoundTotal > 0) {
				addTextToTextArea(notFoundTotal + " elements could not be found.", PMMCommon.TYPE_WARNING);
			}
			
			addTextToTextArea("Finished loading elements @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		}
	}
	
	private boolean isValidInputFileName(String inputFileName) {
		if (!inputFileName.startsWith(APP_NAME)) {
			addTextToTextArea("File name should begin with: " + APP_NAME + ". Check Help if necessary.", PMMCommon.TYPE_ERROR);
			return false;
		}
		
		String[] fileNameParts = inputFileName.split("_");
		if (fileNameParts.length != 3) {
			addTextToTextArea("Incorrect File name. Check Help if necessary", PMMCommon.TYPE_ERROR);
			return false;
		}
		
		String objectTypeAndIdentifier = getObjectAndIdentifierFromFileName(inputFileName);
		int indexOfDash = objectTypeAndIdentifier.indexOf("-");
		if (indexOfDash < 0) {
			addTextToTextArea("Incorrect File name. Check Help if necessary", PMMCommon.TYPE_ERROR);
			return false;
		}
		
		return true;
	}
	
	private String getObjectAndIdentifierFromFileName(String inputFileName) {
		return inputFileName.substring(inputFileName.indexOf("_") + 1, inputFileName.lastIndexOf("_"));
	}

	private void loadInputFile() {
		PMMCommon.UPLOADER_DATA_MAP = new HashMap<SettersData, List<UploaderData>>();
		PMMCommon.ELEMENTS_IDENTIFIERS = new HashSet<String>();
		PMMCommon.DEFAULT_OBJECT_ACTION= ""; 
		
		String inputFileName = SELECTED_FILE.getName();
		FileInputStream excelFile;
		
		if (!isValidInputFileName(inputFileName)) {
			addTextToTextArea("Input File is not correct.", PMMCommon.TYPE_ERROR);
			return;
		}
		
		String objectTypeAndIdentifier = getObjectAndIdentifierFromFileName(inputFileName);
		int indexOfDash = objectTypeAndIdentifier.indexOf("-");
		String objectType = objectTypeAndIdentifier.substring(0, indexOfDash);
		PMMCommon.IDENTIFIER_NAME = objectTypeAndIdentifier.substring(indexOfDash + 1);
		if (Util.isEmpty(PMMCommon.IDENTIFIER_NAME)) {
			addTextToTextArea("Identifier is not set in file name.", PMMCommon.TYPE_ERROR);
			addTextToTextArea("Check Help if necessary.", PMMCommon.TYPE_INFO);
			return;
		}
		else if (DELETE_MODE_BUTTON.isSelected() && !PMMCommon.IDENTIFIER_NAME.equals("ID")) {
			addTextToTextArea("Identifier must be set to ID in Delete Mode. Currently set in file name to " + PMMCommon.IDENTIFIER_NAME, PMMCommon.TYPE_ERROR);
			addTextToTextArea("Check Help if necessary.", PMMCommon.TYPE_INFO);
			return;
		}
		
		PMMHandler = null;
		try {
			PMMHandler = (PMMHandlerInterface)InstantiateUtil.getInstance("apps.refdata.pmm." + objectType + "PMM");
		} catch (InstantiationException | IllegalAccessException e1) {
			Log.error(this, "Could not find Handler for " + objectType);
			addTextToTextArea("Unrecognized element type: " + objectType, PMMCommon.TYPE_ERROR);
			addTextToTextArea("Check Help if necessary.", PMMCommon.TYPE_INFO);
			return;
		}
		
		
		try {
			excelFile = new FileInputStream(SELECTED_FILE);
		} catch (FileNotFoundException e) {
			String msg = "Could not open input file " + inputFileName + " - " + e.toString();
			addTextToTextArea(msg, PMMCommon.TYPE_ERROR);
			return;
		}
		
		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook(excelFile);
		} catch (Exception e) {
			String msg = "Error reading input file " + SELECTED_FILE.getName() + " - " + e.toString();
			addTextToTextArea(msg, PMMCommon.TYPE_ERROR);
			return;
		}
		
		Map<String, SettersData> settersDataMap = new HashMap<String, SettersData>();

		int step_count = 1;
		if (!DELETE_MODE_BUTTON.isSelected()) {
			addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
			addTextToTextArea("Step " + step_count + " : Loading configuration data @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
			step_count++;
			
			Sheet configSheet = workbook.getSheet(CONFIG_SHEET_NAME);
			for (int j = 0; j < configSheet.getPhysicalNumberOfRows(); j++) {
				int rowNum = j + 1;
				Row row = configSheet.getRow(j);
				Cell fieldNameCell = row.getCell(0);
				Cell fieldGetMethodCell = row.getCell(1);
				Cell fieldSetMethodCell = row.getCell(2);
				Cell fieldDataTypeCell = row.getCell(3);
				Cell fieldBasicDataTypeCell = row.getCell(4);
				Cell fieldDataTypePrimitiveCell = row.getCell(5);

				if (fieldNameCell == null || fieldSetMethodCell == null || fieldDataTypeCell == null) {
					String msg = "Ignoring bad config at line (null values) " + row.getRowNum() + 1;
					addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
					continue;
				}

				if (fieldNameCell.getStringCellValue().equals(PMMCommon.DEFAULT_OBJECT_ACTION_NAME)) {
					PMMCommon.DEFAULT_OBJECT_ACTION = fieldGetMethodCell.getStringCellValue();
					continue;
				}

				SettersData settersData = new SettersData();
				settersData.setConfigName(fieldNameCell.getStringCellValue());
				settersData.setGetterMethodName(fieldGetMethodCell.getStringCellValue());
				settersData.setSetterMethodName(fieldSetMethodCell.getStringCellValue());
				settersData.setSetterDataTypeName(fieldDataTypeCell.getStringCellValue());
				settersData.setBasicDataType(fieldBasicDataTypeCell.getStringCellValue());

				if (!Util.isEmpty(settersData.getGetterMethodName())) {
					try {
						Method getterMethod = PMMHandler.getMethod(settersData.getGetterMethodName(), null);
						if (getterMethod == null) {
							String msg = "Ignoring bad config at line " + rowNum + " : Could not get getter method for " + settersData.getGetterMethodName();
							addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
							continue;
						}
						settersData.setGetterMethod(getterMethod);
					}
					catch(NoSuchMethodException e) {
						String msg = "Ignoring bad config at line " + rowNum + " : Could not get getter method for " + settersData.getGetterMethodName();
						addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
						continue;
					}
				}

				try {
					String setterDataTypeName = settersData.getSetterDataTypeName();
					if (Util.isEmpty(setterDataTypeName)) { // Should never be null, but just in case
						String msg = "Ignoring bad config at line " + rowNum + " : Setter Data Name is empty - " + settersData.getConfigNiceName();
						addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
						continue;
					}
					Class<?> dataTypeClass = null;
					if (setterDataTypeName.contains("<") && setterDataTypeName.contains(">")) {
						String subClassName = setterDataTypeName.substring(setterDataTypeName.indexOf("<") + 1, setterDataTypeName.indexOf(">"));
						Class<?> dataSubTypeClass = Class.forName(subClassName);
						if (dataSubTypeClass == null) {
							String msg = "Ignoring bad config at line " + rowNum + " : Could not get setter data type class for " + setterDataTypeName + " - " + settersData.getConfigNiceName();
							addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
							continue;
						}
						settersData.setSetterDataSubTypeClass(dataSubTypeClass);
						
						String mainClassName = setterDataTypeName.substring(0, setterDataTypeName.indexOf("<"));
						dataTypeClass = Class.forName(mainClassName);
					}
					else {
						dataTypeClass = Class.forName(settersData.getSetterDataTypeName());
					}
					if (dataTypeClass == null) {
						String msg = "Ignoring bad config at line " + rowNum + " : Could not get setter data type class for " + setterDataTypeName + " - " + settersData.getConfigNiceName();
						addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
						continue;
					}
					if (fieldDataTypePrimitiveCell != null && fieldDataTypePrimitiveCell.getStringCellValue().equals("PRIM")) {
						dataTypeClass = (Class<?>)dataTypeClass.getField("TYPE").get(null);
						settersData.setPrim(true);
					}
					settersData.setSetterDataTypeClass(dataTypeClass);

				}
				catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
					String msg = "Ignoring bad config at line " + rowNum + " : Could not get setter data type class for " + settersData.getSetterDataTypeName() + " - " + settersData.getConfigNiceName() + " - " + e.toString();
					addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
					continue;
				}

				try {
					Method setterMethod = PMMHandler.getMethod(settersData.getSetterMethodName(), settersData.getSetterDataTypeClass());
					if (setterMethod == null) {
						String msg = "Ignoring bad config at line " + rowNum + " : Could not get setter method for " + settersData.getSetterMethodName();
						addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
						continue;
					}
					settersData.setSetterMethod(setterMethod);
				}
				catch(NoSuchMethodException e) {
					String msg = "Ignoring bad config at line " + rowNum + " : Could not get setter method for " + settersData.getSetterMethodName();
					addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
					continue;
				}

				settersDataMap.put(fieldNameCell.getStringCellValue(), settersData);
			}
			addTextToTextArea("Finished loading configuration @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		}

		addTextToTextArea("", "");
		addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
		addTextToTextArea("Step " + step_count + " : Loading data @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		step_count++;
		
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet dataSheet = workbook.getSheetAt(i);
			if (dataSheet.getSheetName().equals(CONFIG_SHEET_NAME)) {
				continue;
			}
			String originalSheetName = dataSheet.getSheetName();
			addTextToTextArea("Loading " + originalSheetName, PMMCommon.TYPE_INFO, 2);
			String configItemName = originalSheetName.replaceAll(" ", "").toLowerCase();

			SettersData currentSettersData = null;
			
			if (!DELETE_MODE_BUTTON.isSelected()) {
				currentSettersData = settersDataMap.get(configItemName);
				if (currentSettersData == null) {
					String msg = originalSheetName + ": Could not find configuration.";
					addTextToTextArea(msg, PMMCommon.TYPE_WARNING, 4);
					continue;
				}
				currentSettersData.setConfigNiceName(originalSheetName);
			}
			
			int numberOfPhysicalRows = dataSheet.getPhysicalNumberOfRows();
			if (numberOfPhysicalRows == 0) {
				addTextToTextArea(originalSheetName + ": No data, ignoring field.", PMMCommon.TYPE_WARNING, 4);
				continue;
			}

			List<UploaderData> uploaderDataList = new ArrayList<UploaderData>();
			for (int j = 0; j < numberOfPhysicalRows; j++) {
				Row row = dataSheet.getRow(j);
				if (row == null) {
					String msg = originalSheetName + ": Empty line at line " + (j + 1) + ". Check if any other row is after this one because it has been ignored.";
					addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 4);
					continue;
				}
				Cell keyCell = row.getCell(0);
				Cell valueCell = row.getCell(1);
				
				if (keyCell == null) {
					addTextToTextArea("Invalid empty identifier cell at line " + (j + 1), PMMCommon.TYPE_ERROR, 4);
					continue;
				}
				
				if (!DELETE_MODE_BUTTON.isSelected()) {
					if (valueCell == null) {
						addTextToTextArea("Invalid empty value cell at line " + (j + 1), PMMCommon.TYPE_ERROR, 4);
						continue;
					}
				}

				UploaderData uploaderData = new UploaderData();
				uploaderData.setConfigName(configItemName);
				
				try {
					switch(keyCell.getCellType()) {
					case Cell.CELL_TYPE_NUMERIC:
						uploaderData.setElementString(String.valueOf((long)keyCell.getNumericCellValue()));
						break;
					case Cell.CELL_TYPE_STRING:
						uploaderData.setElementString(keyCell.getStringCellValue());
						break;
					default:
						StringBuilder sb = new StringBuilder();
						sb.append(originalSheetName);
						sb.append(": Unknown Identifier type, nor String nor Numeric... ");
						sb.append(valueCell.getStringCellValue());
						sb.append(" for ");
						sb.append(uploaderData.getElementString());
						addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 2);
						continue;
					}
				}
				catch (IllegalStateException e) {
					StringBuilder sb = new StringBuilder();
					sb.append(originalSheetName);
					sb.append(": Error reading Identifier field ");
					sb.append(valueCell.getStringCellValue());
					sb.append(" for ");
					sb.append(uploaderData.getElementString());
					addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 2);
					continue;
				}
				
				if (DELETE_MODE_BUTTON.isSelected()) {
					PMMCommon.ELEMENTS_IDENTIFIERS.add(uploaderData.getElementString());
					uploaderDataList.add(uploaderData);
					continue;
				}
				
				uploaderData.setDataString(valueCell.toString());
				
				try {
					if (currentSettersData != null) {
						switch(currentSettersData.getBasicDataType()) {
						case "DATE":
							uploaderData.setData(JDate.valueOf(valueCell.getDateCellValue()));
							break;
						case "BOOL":
							try {
								uploaderData.setData(valueCell.getBooleanCellValue());
							}
							catch (IllegalStateException e) {
								if (valueCell.getStringCellValue().equalsIgnoreCase("true") ||
										valueCell.getStringCellValue().equalsIgnoreCase("false")) {
									uploaderData.setData(Boolean.valueOf(valueCell.getStringCellValue()));
								}
								else {
									StringBuilder sb = new StringBuilder();
									sb.append(originalSheetName);
									sb.append(": Cannot read boolean value from field ");
									sb.append(valueCell.getStringCellValue());
									sb.append(" for ");
									sb.append(uploaderData.getElementString());
									addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 4);
									continue;
								}
							}
							break;
						case "NUM":
							uploaderData.setData(valueCell.getNumericCellValue());
							break;
						case "VECTOR":
							String[] cellValueSplitted = valueCell.getStringCellValue().split(",");
							final Vector<String> valuesVector = new Vector<String>();
							for (String currentCellValue : cellValueSplitted) {
								valuesVector.add(currentCellValue.trim());
							}
							uploaderData.setData(valuesVector);
							break;
						case "FREQ":
							uploaderData.setData(Frequency.valueOf(valueCell.getStringCellValue()));
							break;
						case "DRULE":
							uploaderData.setData(DateRule.valueOf(valueCell.getStringCellValue()));
							break;
						case "PRULE":
							uploaderData.setData(PeriodRule.valueOf(valueCell.getStringCellValue()));
							break;
						case "SRULE":
							uploaderData.setData(StubRule.valueOf(valueCell.getStringCellValue()));
							break;
						case "DROLL":
							uploaderData.setData(DateRoll.valueOf(valueCell.getStringCellValue()));
							break;
						case "DCOUNT":
							uploaderData.setData(DayCount.valueOf(valueCell.getStringCellValue()));
							break;
						case "FXRESET":
							uploaderData.setData(FXReset.fromString(valueCell.getStringCellValue()));
							break;
						case "TENOR":
							uploaderData.setData(Tenor.valueOf(valueCell.getStringCellValue()));
							break;
						case "FCONTRACT":
							uploaderData.setData(FutureContract.getFutureContract(valueCell.getStringCellValue()));
							break;
						case "RATEIDX":
							String riString = valueCell.getStringCellValue();
							String[] riSplit = riString.split("#");
							RateIndex ri = LocalCache.getRateIndex(PMMCommon.DS, riSplit[0], riSplit[1], Tenor.valueOf(riSplit[2]), riSplit[3]);
							if (ri == null) {
								StringBuilder sb = new StringBuilder();
								sb.append(originalSheetName);
								sb.append(": Cannot find Rate Index ");
								sb.append(valueCell.getStringCellValue());
								sb.append(" for ");
								sb.append(uploaderData.getElementString());
								addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 4);
								continue;
							}
							uploaderData.setData(ri);
							
							break;
						case "HASHTABLE":
							String codesString = valueCell.getStringCellValue();
							String[] codesSplit = codesString.split("#");
							
							Hashtable<String, String> finalCodes = new Hashtable<>();
							for (String currCode : codesSplit) {
								String[] currCodeSplit = currCode.split("=");
								if (currCodeSplit.length != 2) {
									StringBuilder sb = new StringBuilder();
									sb.append(originalSheetName);
									sb.append(": Invalid field format ");
									sb.append(codesString);
									sb.append(" for ");
									sb.append(uploaderData.getElementString());
									addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 4);
									continue;
								}
								finalCodes.put(currCodeSplit[0], currCodeSplit[1]);
							}
							
							uploaderData.setData(finalCodes);
							break;
						case "LIST":
							String cellString = valueCell.getStringCellValue();
							String[] codesSplitL = cellString.split("\n");
							
							List finalCodesL = new ArrayList();
							SettersData settersData = settersDataMap.get(uploaderData.getConfigName());
							if (settersData.getSetterDataSubTypeClass() != null) {
								for (String currCode : codesSplitL) {
									Object o = settersData.getSetterDataSubTypeClass().newInstance();
	
									String[] objectFields = currCode.split(",");
									for (String objectField : objectFields) {
										String[] fieldDetails = objectField.split("=");
										String fieldSetter = fieldDetails[0];
										String fieldValue = fieldDetails[1];
	
										Method setterM = settersData.getSetterDataSubTypeClass().getMethod("set" + fieldSetter, String.class);
										setterM.invoke(o, fieldValue);
									}
									finalCodesL.add(o);
								}
							}
							
							uploaderData.setData(finalCodesL);
							break;
						case "STRING":
						default:
							uploaderData.setData(valueCell.getStringCellValue());
						}
						
						uploaderDataList.add(uploaderData);
					}
				}
				catch (Exception e) {
					StringBuilder sb = new StringBuilder();
					sb.append(originalSheetName);
					sb.append(": Exception while reading a field: ");
					sb.append(e.toString());
					sb.append(" (");
					sb.append("Element : ");
					sb.append(uploaderData.getElementString());
					sb.append(", Value : ");
					sb.append(uploaderData.getDataString());
					sb.append(")");
					
					addTextToTextArea(sb.toString(), PMMCommon.TYPE_ERROR, 4);
					continue;
				}
				
				PMMCommon.ELEMENTS_IDENTIFIERS.add(uploaderData.getElementString());
			}
			
			StringBuilder sb = new StringBuilder();
			if (uploaderDataList.size() != numberOfPhysicalRows) {
				sb.append(originalSheetName);
				sb.append(": ");
				sb.append(uploaderDataList.size());
				sb.append(" element identifiers loaded from a total of ");
				sb.append(numberOfPhysicalRows);
				sb.append(".");
			}
			else {
				sb.append(originalSheetName);
				sb.append(": Correctly loaded all ");
				sb.append(numberOfPhysicalRows);
				sb.append(" element identifiers.");
			}
			
			addTextToTextArea(sb.toString(), PMMCommon.TYPE_INFO, 2);

			PMMCommon.UPLOADER_DATA_MAP.put(currentSettersData, uploaderDataList);
		}

		try {
			excelFile.close();
		} catch (IOException e) {
			String msg = "Could not close input file " + SELECTED_FILE.getName();
			addTextToTextArea(msg, PMMCommon.TYPE_ERROR);
		}
		
		StringBuilder sb = new StringBuilder("Loaded :\n");
		if (!DELETE_MODE_BUTTON.isSelected()) {
			sb.append(" - ");
			sb.append(PMMCommon.UPLOADER_DATA_MAP.size());
			sb.append(" fields.");
			sb.append("\n");
		}
		sb.append(" - ");
		sb.append(PMMCommon.ELEMENTS_IDENTIFIERS.size());
		sb.append(" element identifiers.");
		addTextToTextArea(sb.toString(), PMMCommon.TYPE_INFO);
		
		addTextToTextArea("Finished loading data @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
	}

	void launcheInputFileRead() {
		if (SELECTED_FILE == null) {
			return;
		}
		if (!SELECTED_FILE.canRead()) {
			addTextToTextArea("Impossible to read input file.", PMMCommon.TYPE_ERROR);
			return;
		}
		else {
			TEXT_AREA.setText("");
			LOGO.setVisible(false);
			// define a SwingWorker to run in background
			SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
			{
				public String doInBackground()
				{
					
					setButtonsEnabled(false, false, false, false, false, false, false, false);
					
					loadInputFile();
					if (PMMCommon.UPLOADER_DATA_MAP.size() == 0) {
						addTextToTextArea("No data, aborting.", PMMCommon.TYPE_ERROR);
						setButtonsEnabled(true, false, false, false, false, true, true, true);
						return "";
					}
					
					setButtonsEnabled(true, true, true, false, false, true, true, true);
					
					return "";
				}
			};

			// execute the background thread
			worker.execute();
		}
	}
	
	void loadInputFileButton_actionPerformed(ActionEvent event) {
		
		JFileChooser fileChooser = new JFileChooser();
		
		fileChooser.setCurrentDirectory(new File(LAST_USED_PATH));
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			SELECTED_FILE = fileChooser.getSelectedFile();
			FILE_TEXT_FIELD.setText(SELECTED_FILE.getPath());
			LAST_USED_PATH = SELECTED_FILE.getPath();
		}
		else {
			addTextToTextArea("Please select a file.", PMMCommon.TYPE_ERROR);
			return;
		}
		
		launcheInputFileRead();
	}

	void loadElementsButton_actionPerformed(ActionEvent event) {
		// define a SwingWorker to run in background
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
        {
            public String doInBackground()
            {
            	setButtonsEnabled(false, false, false, false, false, false, false, false);
            	
            	loadElements();
            	if (PMMCommon.MODIFIABLE_ELEMENTS.size() == 0) {
        			addTextToTextArea("File processed but found nothing to modify, aborting.", PMMCommon.TYPE_ERROR);
        			setButtonsEnabled(true, false, false, false, false, true, true, true);
        			return "";
        		}
            	
            	setButtonsEnabled(true, true, true, true, false, true, true, true);
            	
                return "";
            }
        };
            
       // execute the background thread
       worker.execute();
	}
	
	void modifyElementsButton_actionPerformed(ActionEvent event) {
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
		{
			public String doInBackground()
			{
				setButtonsEnabled(false, false, false, false, false, false, false, false);
				
				modifyElements();

				setButtonsEnabled(true, true, true, true, true, true, true, true);
				
				return "";
			}
		};

		// execute the background thread
		worker.execute();
	}
	
	void saveElementsButton_actionPerformed(ActionEvent event) {
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
		{
			public String doInBackground()
			{
				setButtonsEnabled(false, false, false, false, false, false, false, false);
				
				saveElements();
				
				PMMHandler.additionalProcessing();

				setButtonsEnabled(true, false, false, false, false, true, true, true);
				
				return "";
			}
		};

		// execute the background thread
		worker.execute();
	}
	
	void deleteElementsButton_actionPerformed(ActionEvent event) {
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
		{
			public String doInBackground()
			{
				setButtonsEnabled(false, false, false, false, false, false, false, false);
				
				deleteElements();
				
				setButtonsEnabled(true, false, false, false, false, true, true, true);
				
				return "";
			}
		};

		// execute the background thread
		worker.execute();
	}
	
	void setButtonsEnabled(boolean load_file, boolean load_elements, boolean delete_elements, boolean modify_elements, boolean save_elements, boolean help, boolean refresh, boolean deleteMode) {
		LOAD_FILE_BUTTON.setEnabled(load_file);
    	LOAD_ELEMENTS_BUTTON.setEnabled(load_elements);
    	DELETE_ELEMENTS_BUTTON.setEnabled(delete_elements);
		MODIFY_ELEMENTS_BUTTON.setEnabled(modify_elements);
		SAVE_ELEMENTS_BUTTON.setEnabled(save_elements);
		HELP_BUTTON.setEnabled(help);
		REFRESH_BUTTON.setEnabled(refresh);
		DELETE_MODE_BUTTON.setEnabled(deleteMode);
	}
	
	void refreshButton_actionPerformed(ActionEvent event) {
		launcheInputFileRead();
	}
	
	private void deleteModeButton_actionPerformed(ActionEvent actionevent1) {
		setButtonsEnabled(true, false, false, false, false, true, true, true);
		
		if (DELETE_MODE_BUTTON.isSelected()) {
			TEXT_AREA.setBackground(Color.red);
			LOAD_ELEMENTS_BUTTON.setVisible(false);
			MODIFY_ELEMENTS_BUTTON.setVisible(false);
			SAVE_ELEMENTS_BUTTON.setVisible(false);
			
			DELETE_ELEMENTS_BUTTON.setVisible(true);
			
			TEXT_AREA.setText("");
			
			StringBuilder sb = new StringBuilder();
			sb.append("********************************************\n");
			sb.append("             !!! Beware of the Delete Mode !!!\n");
			sb.append("********************************************\n");
			sb.append("\nIn the Delete Mode, the Excel Configuration is not used.\n");
			sb.append("Only the first column is taken into account.\n");
			sb.append("Delete Mode only works with object IDs.\n");
			sb.append("\nPlease be aware that deleting is IRREVERSIBLE.\n");
			
			addTextToTextArea(sb.toString(), "");
			// Only for help, reset area to top
			TEXT_AREA.setCaretPosition(0);
		}
		else {
			TEXT_AREA.setBackground(Color.white);
			LOAD_ELEMENTS_BUTTON.setVisible(true);
			MODIFY_ELEMENTS_BUTTON.setVisible(true);
			SAVE_ELEMENTS_BUTTON.setVisible(true);
			
			DELETE_ELEMENTS_BUTTON.setVisible(false);
		}
	}
	
	void helpButton_actionPerformed(ActionEvent event) {
		TEXT_AREA.setText("");
		
		StringBuilder sb = new StringBuilder();
		sb.append("********************************************\n");
		sb.append("Welcome to PMM - Phil's Massive Uploader !\n");
		sb.append("********************************************\n");
		sb.append("\n");
		sb.append("This is a dangerous tool, remember that with great power comes great responsibility !\n");
		sb.append("\n");
		sb.append("PMM works in various independent separated steps.\n");
		sb.append("Each step can be checked by the user before proceeding to the next one.\n");
		sb.append("Note : Nothing is really modified in Calypso until the last step.\n");
		sb.append("The steps are :\n");
		sb.append("  1. File : The input file is loaded and processed to check for data.\n");
		sb.append("  2. Load : The elements to be modified are retrieved from Calypso.\n");
		sb.append("  3. Modify : The elements are modified based on data loaded in step 1.\n");
		sb.append("  4. Save : The changes are saved in Calypso.\n");
		sb.append("       Note : This is the most time consuming step.\n");
		sb.append("\n");
		sb.append("PMM will output needed logs in order to revise any type of errors that could occur.\n");
		sb.append("\n");
		sb.append("PMM works with a carefully crafted Excel file.\n");
		sb.append("The input Excel file, in format XSLX, must be named as follows:\n");
		sb.append("\"PMM_\" + [Object Type] + \"-\" + [Identifier Type] + \"_\" + [Anything] + \".xlsx\"\n");
		sb.append("\n");
		sb.append("Some examples:\n");
		sb.append(" > PMM_Bond-ISIN_FileToModifyFieldsBasedOnISIN.xlsx\n");
		sb.append(" > PMM_Bond-ID_IWantToDoItByID.xlsx\n");
		sb.append(" > PMM_Equity-Common_ThisWorksGreatForSecCodeCommon.xlsx\n");
		sb.append(" > PMM_Equity-ID_ThisTooWorksByID.xlsx\n");
		sb.append("\n");
		sb.append("Inside the Excel input file, the tabs must be correctly named based on the fields to be modified.\n");
		sb.append("In each tab, there must be two, and only two columns: \n");
		sb.append("Object Identifier - Field Value\n");
		sb.append("The Object Identifier type is set in the file name as explained before (ID, etc...).\n");
		sb.append("Each row is dedicated to one object, and there will be as many rows as there are objects to be modified.\n");
		sb.append("Empty rows are not accepted.\n");
		sb.append("\n");
		sb.append("The reference Excel files have some predefined fields examples, with the needed format explained in comments. \n");
		
		sb.append("\n");
		sb.append("Currently supported object types are :\n");
		try {
			Class[] classes = PMMCommon.getClasses("calypsox.apps.refdata.pmm");
			for (Class clazz : classes) {
				String className = clazz.getSimpleName();
				if (className.endsWith(APP_NAME)) {
					sb.append("  - ");
					sb.append(className.replace(APP_NAME, ""));
					sb.append("\n");
				}
			}
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sb.append("\n");
		sb.append("Note: There is a special hidden tab named \"config\" to configure the available fields to be modified.\n");
		sb.append("/!\\ Important : This special tab should only be modified by someone from Calypso.\n");
		sb.append("The columns in this tab are:\n");
		sb.append("  - Field Name (lower case, no space).\n");
		sb.append("  - Getter Method, usually empty.\n");
		sb.append("  - Setter Method.\n");
		sb.append("  - Setter parameter data type. We can only use one-parameter methods.\n");
		sb.append("  - High level type.\n");
		sb.append("  - Primary type indicator.\n");
		sb.append("\n");
		sb.append("A special line allows to set the action to be used for objects that have a Workflow (Trades, Message, Transfers, ...).\n");
		sb.append("The columns in this case are : \n");
		sb.append("  - DEFAULT_OBJECT_ACTION.\n");
		sb.append("  - The action to be used.\n");
		sb.append("  => If empty, the following action will be used (only if necessary) : " + PMMCommon.DEFAULT_ACTION + " \n");

		sb.append("\n");
		sb.append("\n");
		sb.append("********************************************\n");
		sb.append("Delete Mode\n");
		sb.append("********************************************\n");
		sb.append("\n");
		sb.append("This is an even more dangerous tool, remember that with the greatest power comes the greatest responsibility !\n");
		sb.append("\n");
		sb.append("If Delete Mode is selected, only the first column is read in all tabs, and the configuration tab named \"config\" is ignored.\n");
		sb.append("Delete Mode only works with object IDs, so the file should always specify this kind of identifier type.\n");
		sb.append("The input Excel file, in format XSLX, must be named as follows:\n");
		sb.append("\"PMM_\" + [Object Type] + \"-ID_\" + [Anything] + \".xlsx\"\n");
		sb.append("In this mode, the steps are :\n");
		sb.append("  1. File : The input file is loaded. No data check is made.\n");
		sb.append("  2. Delete : The elements are permanently deleted from Calypso.\n");
		sb.append("\n");
		sb.append("Note : Due to Calypso's API, some objects can be removed all at once, but others have to be deleted one by one, so it may take more time.\n");
		
		
		addTextToTextArea(sb.toString(), "");
		// Only for help, reset area to top
		TEXT_AREA.setCaretPosition(0);
	}

	
}
