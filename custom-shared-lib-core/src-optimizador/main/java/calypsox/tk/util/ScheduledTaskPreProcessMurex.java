package calypsox.tk.util;

/**
 * Alberto Dufort
 * 
 * Scheduled Task para el calculo del campo ACTION en los .dat de murex CASH y SECURE, fichero sin cabeceras
 * 
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.ErrorCodeEnum;
import calypsox.util.FileUtility;

public class ScheduledTaskPreProcessMurex extends ScheduledTask {

	// ACTION EVENT
	private static final String CANCEL = "CANCEL";
	private static final String NEW = "NEW";
	private static final String AMEND = "AMEND";
	private static final String MATURE = "MATURE";
	
	// INSTRUMENT TYPE
	private static final String COLLAT_SEC = "COLLAT_SECURITY";

	// DIRECTION TYPE
	private static final String LOAN = "Loan";
	private static final String BORROW = "Borrow";

	// unique class id, important to avoid problems
	private static final long serialVersionUID = 2233295722857754L;
	
	// file's records 

	private String M_TP_STATUS2 = "";// campo 1
 	private String CNTLIMTSD2 = "";// campo2
	private String TPDTELST = "";// campo 3
	@SuppressWarnings("unused")
	private String CNTLEVTCL2 = "";// campo 4
	private String DTESYS = "";// campo 5
	private String ACTION = "";// campo 0 en el de salida
	private String FO_SYSTEM = "";// campo 6
	private String N_FRONT_ID = "";// campo 7
	private String COLL_ID ="";// campo 8
	private String OWNER = "";// campo 9
	private String CPTY = "";// campo 10
	private String INSTRUMENT = "";// campo 11
	private String PORT = "";// campo 12
	private String VALUE = "";// campo 13
	private String TRADE = "";// campo 14
	private String DIRECTION = "";// campo 15
	private String AMOUNT = "";// campo 16
	private String AMOUNT_CCY = "";// campo 17
	private String UNDER_TYPE = "";// campo 18 - only for SEC file
	private String UNDERLYNG = "";// campo 19 - only for SEC file
	private String CLOSINGPRICE = "";// campo 18 for CASH, campo 20 for SEC
	private String FEE_AMOUT = ""; // campo 19 for CASH, campo 21 for SEC
	
	
	private static BufferedReader reader;
	private static BufferedWriter writer;
	private static BufferedWriter writerLog;
	private static FileWriter fileWriter;
	private static FileWriter fileWriterLog;
	private static File fileOut;
	private static File fileIn;
	private static File fileLog;
	private static boolean bStatus= false;
	private String fecha;
	
	private static final String TASK_INFORMATION = "Scheduled Task calculate files CASH and SECUR, Murex";
	protected static SimpleDateFormat dateFormatL = new SimpleDateFormat("dd/MM/yyyy");
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	public enum DOMAIN_ATTRIBUTES {

		FILENAME("File Name in:"),
		FILENAME_LOG("File Name log:"),
		PATH_IN("In File Path:"),
		PATH_OUT("Out File Path:"),
		PATH_LOG("Log File Path:"),
		SEPARATOR("Records separator");
		
		private final String desc;

		// add description
		private DOMAIN_ATTRIBUTES(String d) {
			this.desc = d;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

		// list with domain values descriptions
		public static List<String> getDomainDescr() {
			ArrayList<String> a = new ArrayList<String>(DOMAIN_ATTRIBUTES.values().length);
			for (DOMAIN_ATTRIBUTES domain : DOMAIN_ATTRIBUTES.values()) {
				a.add(domain.getDesc());
			}
			return a;

		}
	} // end ENUM

	/**
	 * Main method to be executed in this Scheduled task
	 * 
	 * @param connection
	 *            to DS
	 * @param connection
	 *            to PS
	 * @return result of the process
	 */
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {
		bStatus = false;
		final JDatetime valDate = getValuationDatetime();
		fecha = dateFormatL.format(valDate);
		// fichero 
		buildFile(getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc()),getAttribute(DOMAIN_ATTRIBUTES.FILENAME_LOG.getDesc()));
		
		return bStatus;
	}

	/**
	 * @return a vector with all the domain attributes for this schedule task
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getDomainAttributes() {

		final Vector<String> result = new Vector<String>(DOMAIN_ATTRIBUTES.values().length);
		result.addAll(DOMAIN_ATTRIBUTES.getDomainDescr());
		return result;
	}
	
	/*
	 * Creaci?n de los .dat
	 */
	private void buildFile(String filename, String filelog){
		
		String line = null;
		int iLineIn = 0; 
		int iNewLine = 0;
		String separator = getAttribute(DOMAIN_ATTRIBUTES.SEPARATOR.getDesc());
		
		try{
			// comprobamos que el fichero existe
			if (iniFile(filename, filelog)){
				StringBuffer line_new = new StringBuffer();
				int j = 1;
				String campo = "";
				String scampo = "";
					while ((line = reader.readLine()) != null) {
						line_new = new StringBuffer();
						// contamos un nuevo registro del fichero inicial
						iLineIn++;
						if (iLineIn >= 1) {
						int iControl = 0;
						while (iControl < line.length()) {
							// trozo de la linea que quedar por tratar
							scampo = line.substring(iControl);
							// Obtenci?n del campo
							if (scampo.indexOf(separator) < 0) {
								campo = scampo; 
								iControl = iControl + scampo.length();
								} else {
									campo = scampo.substring(0,scampo.indexOf(separator));
								}
							// inserta el valor del registro en su posici?n
							setAttribute(campo,j);
							// Inicializa el String para el campo y pasamos a un nuevo registro
							j++; 
							iControl = iControl + scampo.indexOf(separator) +1;
						}
						
						// Calcula el campo ACTION del fichero de salida
						setAction();
						// Montamos la l?nea con el formato establecido
						
						if (MATURE.equals(ACTION)) {
							setAttributesMature(ACTION, AMOUNT, FEE_AMOUT, DIRECTION);
						}
						
						line_new.append(ACTION);
						line_new.append(separator);
						line_new.append(FO_SYSTEM);
						line_new.append(separator);
						line_new.append(N_FRONT_ID);
						line_new.append(separator);
						line_new.append(COLL_ID);
						line_new.append(separator);
						line_new.append(OWNER);
						line_new.append(separator);
						line_new.append(CPTY);
						line_new.append(separator);
						line_new.append(INSTRUMENT);
						line_new.append(separator);
						line_new.append(PORT);
						line_new.append(separator);
						line_new.append(VALUE);
						line_new.append(separator);
						line_new.append(TRADE);
						line_new.append(separator);
						line_new.append(DIRECTION);
						line_new.append(separator);
						line_new.append(AMOUNT);
						line_new.append(separator);
						line_new.append(AMOUNT_CCY);
						line_new.append(separator);
						line_new.append(UNDER_TYPE);
						line_new.append(separator);
						line_new.append(UNDERLYNG);
						line_new.append(separator);
						line_new.append(CLOSINGPRICE);
						 
						// escribimos en el fichero de salida y contamos una fila
						if (line_new.length()> 0){
							if(iNewLine>0) writer.newLine();
							writer.write(line_new.toString());
							iNewLine ++;
						}
						// Siguiente l?nea
						j= 1;
					}
				}
			}else{
				// si el fichero no existe, paramos y lo comunicamos en el log
				bStatus = false;
				iniFileLog(filelog);
				wLog("Error: KO: file " + filename + " not successfully processed, file no exists");
				Log.error(ScheduledTaskPreProcessMurex.class, "Error: KO: file " + filename + " not successfully processed, file no exists");
			}
		
		}catch(Exception e){
			bStatus = false;
			Log.error(ScheduledTaskPreProcessMurex.class, e);
		}finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {
					bStatus = false;
					Log.error(ScheduledTaskPreProcessMurex.class, e);
				}
			}
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {
					bStatus = false;
					Log.error(ScheduledTaskPreProcessMurex.class, e);
				}
			}
			// comprueba si los dos ficheros tienen el mismo n?mero de l?neas, si no es as? dar? el aviso
			compareToLines(iLineIn, iNewLine, filename);
			if (writerLog != null) {
				try {
					writerLog.close();
				}
				catch (IOException e) {
					bStatus= false;
					Log.error(ScheduledTaskPreProcessMurex.class, e);
				}
			}
			
		}
		
	}
	

	private void setAttributesMature(String action, String amount, String feeAmount, String direction) {
		if (MATURE.equals(action)) {
			if (!Util.isEmpty(amount) && !Util.isEmpty(feeAmount) && !Util.isEmpty(direction)) {
				 double amountReversedSigned = Double.valueOf(AMOUNT) * (BORROW.equals(DIRECTION)? (-1) : 1) + Double.valueOf(feeAmount);
				 AMOUNT = String.valueOf(Math.abs(amountReversedSigned));
				 DIRECTION = (amountReversedSigned < 0) ? LOAN : BORROW;
			} else if (!Util.isEmpty(direction)) {
				DIRECTION = (BORROW.equals(DIRECTION)) ? LOAN : BORROW; 
			}
			// MATURE EVENTS ARE HANDLED AS NEW EVENTS
			ACTION = NEW;
		}
	}

	/*
	 * Inicializa los ficheros a leer, el nuevo 
	 */
	private boolean iniFile(String filename, String filelog) throws IOException{
		
		fileIn = new File(getAttribute(DOMAIN_ATTRIBUTES.PATH_IN.getDesc()) + filename);
		if (!fileIn.exists()) return false;
		fileOut = new File(getAttribute(DOMAIN_ATTRIBUTES.PATH_OUT.getDesc()) + filename);	
		iniFileLog(filelog);
		
		fileWriter = new FileWriter(fileOut);
		writer = new BufferedWriter(fileWriter);
		reader = new BufferedReader(new FileReader(fileIn));
		return true;
	}
	
	/*
	 * Inicializa el fichero log 
	 */
	private void iniFileLog(String filelog) throws IOException{
		String time = "";
		synchronized (timeFormat) {
			time = timeFormat.format(getValuationDatetime());
		}
		fileLog = new File(getAttribute(DOMAIN_ATTRIBUTES.PATH_LOG.getDesc()) + filelog + time);
		fileWriterLog = new FileWriter(fileLog);
		writerLog = new BufferedWriter(fileWriterLog);
	}
	
	/*
	 * Escribe en el log
	 */
	private void wLog(String msg)throws IOException{
		writerLog.write(msg);
		writerLog.newLine();
	}
	/*
	 * Proceso para cargar el campo ACTION, inicial del registro 
	 */
	private void setAction(){
		// trim
		DTESYS = DTESYS.trim();
		M_TP_STATUS2 = M_TP_STATUS2.trim();
		CNTLIMTSD2 =  CNTLIMTSD2.trim();
		TPDTELST = TPDTELST.trim();
		
		ACTION = AMEND;
		
		// C?lculo para el ACTION
		JDate dteSysAddDay = JDate.valueOf(DTESYS).addBusinessDays(1, Util.string2Vector("SYSTEM"));
		String dteSysAddDayStr = dateFormatL.format(dteSysAddDay.getDate(TimeZone.getDefault()));
		if ((!Util.isEmpty(DTESYS)) && dteSysAddDayStr.equals(fecha) && ( M_TP_STATUS2.equals("LIVE"))) {
			ACTION = NEW;
		} else if((!Util.isEmpty(CNTLIMTSD2)) && (CNTLIMTSD2.equals(fecha)) && (M_TP_STATUS2.equals("DEAD"))) {
			ACTION = CANCEL;
		}
			
		if ((!Util.isEmpty(TPDTELST)) && (TPDTELST.equals(fecha))) {
			ACTION = MATURE;
		}
		
		//if ((CNTLEVTCL2=="" ) && (CNTLEVTCL2 == "MfNux73071") && (CNTLIMTSD2=="") && (CNTLIMTSD2 == fecha )) ACTION="AMEND";
		
	}
	
	/*
	 * Verificaci?n entre el fichero de lectura y el nuevo;
	 * comprueba que tengan el mismo n?mero de filas
	 */
	private void compareToLines(int iIn, int iOut, String filename){
		
		if (iIn == iOut){
			bStatus = true;
			try{
				wLog("OK: file " + filename + " successfully processed");
				// moving files
				copyAndRemoveFile(filename, "/ok/");
			}catch(IOException e){
				Log.error(ScheduledTaskPreProcessMurex.class, e);
			}
		}else{
			bStatus = false;
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileInvalidFormat, "Error in the number of lines.");
			try{
				wLog("Error: KO: file " + filename + " not successfully processed, Invalid format file");
				copyAndRemoveFile(filename, "/fail/");
			}catch(IOException e){
				Log.error(ScheduledTaskPreProcessMurex.class, e);
			}
		}
	}

	private void copyAndRemoveFile(String filename, String status) {
		File fileIn = new File(getAttribute(DOMAIN_ATTRIBUTES.PATH_IN.getDesc()) + filename);
		File directoryOk = new File(new File(fileIn.getParent()).getParent()+status);
		if (fileIn.exists() && directoryOk.exists()) {
			FileUtility.copyFileToDirectory(fileIn, directoryOk);
			
			// delete file
			fileIn.delete();
		}
	}
	
	/*
	 * Asignaci?n de los valores en la posici?n correspondiente
	 * conservo la denominaci?n original de los scripts, mejor comprensi?n
	 */
	private void setAttribute(String campo, int sPosition){
		
		// En caso de eliminar un campo s?lo hay que sacarlo de switch al igual que si hay que
		//incluir un nuevo campo 
		switch (sPosition) {
			case 1:
				M_TP_STATUS2 = campo;
				break;
			case 2:
				CNTLIMTSD2 = campo;
				break;
			case 3:
				TPDTELST = campo;
				break;
			case 4:
				CNTLEVTCL2 = campo;
				break;
			case 5:
				DTESYS = campo;
				break;
			case 6:
				FO_SYSTEM = campo;
				break;
			case 7:
				N_FRONT_ID = campo;
				break;
			case 8:
				COLL_ID = campo;
				break;
			case 9:
				OWNER = campo;
				break;
			case 10:
				CPTY = campo;
				break;
			case 11:
				INSTRUMENT = campo;
				break;
			case 12:
				PORT = campo;
				break;
			case 13:
				VALUE = campo;
				break;
			case 14:
				TRADE = campo;
				break;
			case 15:
				DIRECTION = campo;
				break;
			case 16:
				AMOUNT = campo;
				break;
			case 17:
				AMOUNT_CCY = campo;
				break;
			case 18:
				if (COLLAT_SEC.equals(INSTRUMENT)) {
					UNDER_TYPE = campo; 
				} else {
					CLOSINGPRICE = campo;
				}
				break;
			case 19:
				if (COLLAT_SEC.equals(INSTRUMENT)) {
					UNDERLYNG = campo; 
				} else {
					FEE_AMOUT = campo;
				}
				break;
			case 20:
				CLOSINGPRICE= campo;
				break;
			case 21:
				FEE_AMOUT= campo;
				break;
			default:
		}
	}
	
	
	/**
	 * @param attribute
	 *            name
	 * @param hastable
	 *            with the attributes declared
	 * @return a vector with the values for the attribute name
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {

		Vector<String> vector = new Vector<String>();

		
		return vector;
	}

	/**
	 * Ensures that the attributes have a value introduced by who has setup the schedule task
	 * 
	 * @return if the attributes are ok
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

		boolean retVal = super.isValidInput(messages);

		for (DOMAIN_ATTRIBUTES attribute : DOMAIN_ATTRIBUTES.values()) {

			final String value = super.getAttribute(attribute.getDesc());

			if (Util.isEmpty(value)) {

				messages.addElement(attribute.getDesc() + " attribute not specified.");
				retVal = false;
			}

		}
		
		return retVal;
	}

	/**
	 * @return this task information, gathered from the constant TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}	
}
