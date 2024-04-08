/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.mmoo;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

/**
 * Class for loading mmoo log trades
 * 
 * @author jmontanon
 * 
 */
public class LogUtilMMOO {

	private int numOftradeProcessed;
	private int numOfTradeOK;
	private int numOfTradeERROR;

	private final String process = "PROCESS: Load of ";
	private String processText = "";

	private final String fileNameText = "FILE:  ";
	private String fileName = "";

	private Date date = null;
	private List<String> attachedDetailLog = null;
	private FileWriter fileWriter = null;
	private PrintWriter printWriter = null;

	/**
	 * Constructor
	 * 
	 * @param file
	 *            - String: path where you want to save the file: EXAMPLE --> D:/Temp/Ejemplo.txt
	 * 
	 * @param fileWithDate
	 *            - boolean: true - If we want to save multiple files concatenated with the date at the end false: If
	 *            you only want to save a file
	 * 
	 * @param processText
	 *            - String: text of the line PROCESS: Load of...
	 * 
	 * @param fileNameText
	 *            - String: name of the file for the text of the line: File: XXX.dat
	 * 
	 */
	public LogUtilMMOO(String file, boolean fileWithDate, String processText, String fileName) {
		try {
			// Inicialilizamos numero de OK, Errores, warnings, excluded:
			this.numOftradeProcessed = 0;
			this.numOfTradeOK = 0;
			this.numOfTradeERROR = 0;

			// formamos el tecto de la segunda y tercera linea:
			this.processText = this.process + processText;
			this.fileName = this.fileNameText + fileName;

			this.date = new Date();

			// Escritura de varios ficheros con la fecha concatenada al final:
			if (fileWithDate == true) {
				// String fechaParaFichero = this.date.toLocaleString();
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
				String fechaParaFichero = sdf.format(new Date());
				// fechaParaFichero = fechaParaFichero.replaceAll(":", "");
				fechaParaFichero = fechaParaFichero.trim();
				file = file.replace(".txt", "").trim(); // borramos .txt
				// montamos la ruta
				String ruta = file + "_" + fechaParaFichero + ".txt";

				this.fileWriter = new FileWriter(ruta); // file: ruta del fichero donde queremos escribir + fecha
														// concatenada
			} else {
				// Escritura de un solo fichero
				this.fileWriter = new FileWriter(file); // file: ruta del fichero donde queremos escribir
			}

			this.printWriter = new PrintWriter(this.fileWriter);

		} catch (Exception e) {
			Log.error(e, e.toString());
		}
	}

	/**
	 * Attachs extra details in the log
	 * 
	 * @param ldetail
	 */
	public void attachLogDetails(List<String> ldetail) {
		this.attachedDetailLog = ldetail;
	}

	/**
	 * Increase the number of TRADES PROCESSED
	 */
	public void IncrementNumberOfTradeProcessed() {
		this.numOftradeProcessed++;
	}

	/**
	 * Increase the number of TRADES PROCESSED
	 */
	public void IncrementNumberOfTradeOK() {
		this.numOfTradeOK++;
	}

	/**
	 * Increase the number of TRADES ERROR
	 */
	public void IncrementNumberOfTradeERROR() {
		this.numOfTradeERROR++;
	}

	/**
	 * Write the text in the file and close it
	 */
	public void WriteLog() {
		try {
			// Escribimos fecha
			this.printWriter.println(this.date.toString() + ". DS Host: " + DSConnection.getDefault().getServiceURL());
			// Escribimos PROCESS: Load of XXX
			this.printWriter.println(this.processText);
			// Escribimos FILE: XXX.dat
			this.printWriter.println(this.fileName);

			// -------------------------------------------------------
			// Status:
			if ((this.numOfTradeERROR != 0) || (this.numOftradeProcessed == 0)) {
				this.printWriter.println("STATUS: ERROR");
			} else {
				this.printWriter.println("STATUS: OK");
			}
			// -------------------------------------------------------

			// Salto de linea:
			this.printWriter.println();

			// Escribimos numero de coincidencias:
			this.printWriter.println("NUMBER OF TRADES PROCESSED: " + this.numOftradeProcessed);
			this.printWriter.println("NUMBER OF TRADES OK: " + this.numOfTradeOK);
			this.printWriter.println("NUMBER OF TRADES ERROR: " + this.numOfTradeERROR);

			// Escribimos detalle del log si hubo alg?n error:
			this.printWriter.println();
			this.printWriter.println("LOG DETAILS:");
			if (this.attachedDetailLog != null) {
				for (String s : this.attachedDetailLog) {
					s = s.replaceAll("\n", "");
					if (s.trim().isEmpty()) {
						continue;
					}
					this.printWriter.println(s);
				}
			}

		} catch (Exception e) {
			Log.error(e, e.toString());
		} finally {
			try {
				// Cerramos el fichero:
				if (null != this.fileWriter) {
					this.fileWriter.close();
				}
			} catch (Exception e2) {
				Log.error(e2, e2.toString());
			}
		}
	}
}
