/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.BondCrossReferencesBean;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.*;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImport_ISIN_SEDOL_REF_INTERNA extends AbstractProcessFeedScheduledTask {

  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  private File file;

  // START OA 27/11/2013
  // Oracle recommendation : declare a serialVersionUID for each Serializable
  // class in order to avoid
  // InvalidClassExceptions.
  // Please refer to Serializable javadoc for more details
  private static final long serialVersionUID = 54246518454654L;

  // START CALYPCROSS-38 - mromerod
  /** Attribute that indicates the number of threads to use */
  private static final int NUM_THREADS = 4; // everis

  private static final double NUMBER_THREADS_DOUBLE = 4; // everis P
  // END CALYPCROSS-38 - mromerod

  // END OA OA 27/11/2013

  // private static final String ATT_SEPERATOR = "Separator";

  @Override
  public Vector<String> getDomainAttributes() {
    final Vector<String> attr = super.getDomainAttributes();
    return attr;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean isValidInput(final Vector messages) {
    boolean retVal = super.isValidInput(messages);
    return retVal;
  }

  @Override
  public boolean process(final DSConnection conn, final PSConnection connPS) {
    boolean result = true;

    Date iniST = new Date();
    Log.info(
        LOG_CATEGORY_SCHEDULED_TASK,
        ">>>>>>>>>> LOG Optimizacion Procesos: Inicio del proceso (" + iniST + ") <<<<<<<<<<");

    try {
      // JRL 21/04/2016 Migration 14.4
      final String path = getAttribute(FILEPATH);
      final String startFileName = getAttribute(STARTFILENAME);
      JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());

      // BAU - Use different way to import data in order to avoid multiple
      // files problem
      this.file = lookForFile(path, startFileName, fileDate);

      if (this.file != null) {

        // Just after file verifications, this method will make a copy
        // into the
        // ./import/copy/ directory
        FileUtility.copyFileToDirectory(path + this.file.getName(), path + "/copy/");

        result = processFile(path + this.file.getName());

      } else {
        Log.error(
            LOG_CATEGORY_SCHEDULED_TASK, "No matches found for filename in the path specified.");
        ControlMErrorLogger.addError(
            ErrorCodeEnum.InputFileNotFound,
            "No matches found for filename in the path specified.");
        result = false;
      }

    } catch (Exception exc) {
      Log.error(this, exc); // sonar
    } finally {
      try {
        feedPostProcess();
      } catch (Exception e) {
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while moving the files to OK/Fail folder");
        Log.error(this, e); // sonar
        ControlMErrorLogger.addError(
            ErrorCodeEnum.UndefinedException, "Error while moving the files to OK/Fail folder");
        result = false;
      }
    }

    Date finST = new Date();
    Log.info(
        LOG_CATEGORY_SCHEDULED_TASK,
        ">>>>>>>>>> LOG Optimizacion Procesos: Fin del proceso. El proceso ha tardado "
            + (finST.getTime() - iniST.getTime())
            + " milisecs ("
            + finST
            + ") <<<<<<<<<<");

    return result;
  }
  // JRL 21/04/2016 Migration 14.4
  private boolean processFile(String file) {
    BufferedReader reader = null;
    try {

      Date iniSelect = new Date();
      Log.info(
          LOG_CATEGORY_SCHEDULED_TASK,
          ">>>>>>>>>> LOG Optimizacion Procesos: Comienza la consulta ("
              + iniSelect
              + ") <<<<<<<<<<");

      final Map<String, Bond> allBondsMap = loadBondsMap();
      Date finSelect = new Date();
      Log.info(
          LOG_CATEGORY_SCHEDULED_TASK,
          ">>>>>>>>>> LOG Optimizacion Procesos: Finaliza la consulta. Se ha ejecutado en "
              + (finSelect.getTime() - iniSelect.getTime())
              + " milisecs obteniendo "
              + allBondsMap.size()
              + " resultados ("
              + iniSelect
              + ") <<<<<<<<<<");
      final Map<String, Bond> bondsWithIntRef = new HashMap<String, Bond>();

      reader = new BufferedReader(new FileReader(file));
      String line = null;
      int lineNumber = -1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if ((lineNumber == 0) || Util.isEmpty(line)) {
          continue;
        }

        String errorMsg = "";
        BondCrossReferencesBean bean = buildBean(line, errorMsg);
        if (!Util.isEmpty(errorMsg)) {
          // Invalid Line
          Log.error(
              LOG_CATEGORY_SCHEDULED_TASK,
              "Line number-" + lineNumber + " : " + line + " : Line=" + line);
          continue;
        }

        // JRL 21/04/2016 Migration 14.4
        if (bean != null) {
          Bond bond = allBondsMap.get(bean.getIsin());
          if (bond == null) {
            Log.error(
                LOG_CATEGORY_SCHEDULED_TASK,
                "Line number-" + lineNumber + " : Bond not found with ISIN=" + bean.getIsin());
            continue;
          }

          processLine(bond, bean);
          bondsWithIntRef.put(bean.getIsin(), bond); // Just add the bond to the map
          /*try {
          	bondsWithIntRef.put(bean.getIsin(), bond);
          	getDSConnection().getRemoteProduct().saveBond(bond, true);
          } catch (RemoteException re) {
          	Log.error(LOG_CATEGORY_SCHEDULED_TASK,
          			"Line number-" + lineNumber + " : Error while saving the Bond with REF_INTERNA="
          					+ bean.getInternalRef() + " : ISIN=" + bean.getIsin() + ", Error="
          					+ re.getLocalizedMessage());
          }*/
        }
      }

      // Save bondsWithIntRef bonds
      SegmentThread[] intRefThread = manageSavingThreads(bondsWithIntRef.values(), false);

      // Control of the trades we are saving
      StringBuffer bondSaved =
          new StringBuffer("*****All BondIds saved: Total: " + bondsWithIntRef.size());
      for (Bond bond : bondsWithIntRef.values()) {
        bondSaved.append(" BdId: " + bond.getId() + ";");
      }
      System.err.println(bondSaved.toString());
      Log.info(LOG_CATEGORY_SCHEDULED_TASK, bondSaved.toString());

      // // Remove ECB Attributes for the remaining Bonds.
      Collection<Bond> bondsWithNoIntRef = allBondsMap.values();
      bondsWithNoIntRef.removeAll(bondsWithIntRef.values());

      // Save bondsWithNoIntRef bonds
      SegmentThread[] noIntRefThread = manageSavingThreads(bondsWithNoIntRef, true);

      Log.info(
          LOG_CATEGORY_SCHEDULED_TASK,
          ">>>>>>>>>> LOG Optimizacion Procesos: se han guardado "
              + bondsWithIntRef.size()
              + " en bondsWithIntRef y "
              + bondsWithNoIntRef.size()
              + " en bondsWithNoIntRef ("
              + new Date()
              + ") <<<<<<<<<<");

      joinThreads(intRefThread);
      joinThreads(noIntRefThread);

    } catch (Exception exc) {
      Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
      return false;
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          Log.error(this, e); // sonar
        }
      }
    }
    return true;
  }

  private void processLine(Bond bond, BondCrossReferencesBean bean) {
    bond.setSecCode(BOND_SEC_CODE_REF_INTERNA, bean.getInternalRef());
    if (!Util.isEmpty(bean.getCusip())) {
      bond.setSecCode(BOND_SEC_CODE_CUSIP, bean.getCusip());
    } else {
      bond.setSecCode(BOND_SEC_CODE_CUSIP, null);
    }

    if (!Util.isEmpty(bean.getSedol())) {
      bond.setSecCode(BOND_SEC_CODE_SEDOL, bean.getSedol());
    } else {
      bond.setSecCode(BOND_SEC_CODE_SEDOL, null);
    }
  }

  // START CALYPCROSS-38 - mromerod
  private boolean removeCrossRefs(Bond bond) {
    if ((bond.getMaturityDate() == null)
        || bond.getMaturityDate().after(JDate.getNow())
            && (bond.getSecCode(BOND_SEC_CODE_REF_INTERNA) != null
                || bond.getSecCode(BOND_SEC_CODE_CUSIP) != null
                || bond.getSecCode(BOND_SEC_CODE_SEDOL) != null)) {
      bond.setSecCode(BOND_SEC_CODE_REF_INTERNA, null);
      bond.setSecCode(BOND_SEC_CODE_CUSIP, null);
      bond.setSecCode(BOND_SEC_CODE_SEDOL, null);
      return true;
    } else {
      return false;
    }
  }
  // END CALYPCROSS-38 - mromerod

  @SuppressWarnings({"unchecked"})
  private Map<String, Bond> loadBondsMap() throws RemoteException {
    Map<String, Bond> bondsMap = new HashMap<String, Bond>();
    // START CALYPCROSS-420 - fperezur
    String from = null;
    String where =
        " product_desc.product_family='Bond' ";
    // END CALYPCROSS-420 - fperezur
    long TInicio, TFin, tiempo;
    /** */
    TInicio = System.currentTimeMillis();
    /** */
    Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(from, where, null);
    TFin = System.currentTimeMillis();
    /** */
    tiempo = TFin - TInicio;
    /** */
    System.err.println("*******Tiempo total para Bonds: " + tiempo + ". Total: " + allBonds.size());
    /** */
    Log.info(Log.OLD_TRACE, "All bonds have been loaded: " + allBonds.size());
    /** */
    for (Bond bond : allBonds) {
      String isin = bond.getSecCode(BOND_SEC_CODE_ISIN);
      if (Util.isEmpty(isin)) {
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Bond doesn't have ISIN. Bond Id =" + bond.getId());
      } else {
        // Check if the Bond already exists in the map. If so it is
        // duplicate REF_INTERNA
        if (bondsMap.get(isin) != null) {
          Log.error(
              LOG_CATEGORY_SCHEDULED_TASK,
              "Duplicate ISIN for Bond ids =" + bondsMap.get(isin).getId() + ", " + bond.getId());
        } else {
          bondsMap.put(isin, bond);
        }
      }
    }
    System.err.println("*******" + allBonds.size() + " Bonds totales");
    /** */
    return bondsMap;
  }

  /*
   * The file format is a bit strange. The fields are seperated by spaces. So
   * the fileds are taken from the line by a fixed columns sizes.
   */
  private BondCrossReferencesBean buildBean(String line, String errorMsg) {
    BondCrossReferencesBean bean = new BondCrossReferencesBean();
    // if (line.length() < 214) {
    // errorMsg = "Invalid line. It has fewer than 214 characters.";
    // return null;
    // }

    String refInterna = line.substring(0, 20);
    if (Util.isEmpty(refInterna) || Util.isEmpty(refInterna.trim())) {
      errorMsg = "Invalid line. REF_INTERNA is empty.";
      return null;
    } else {
      bean.setInternalRef(refInterna.trim());
    }

    //		line.substring(20, 141);// ingore

    // REF1, Usually this is ISIN
    String secCode = line.substring(141, 143);
    String value = "";
    if (!Util.isEmpty(secCode) && null != secCode) {
      if (line.length() >= 172) {
        value = line.substring(143, 173);
      } else {
        value = line.substring(143);
      }

      if (Util.isEmpty(secCode)
          || Util.isEmpty(value)
          || Util.isEmpty(secCode.trim())
          || Util.isEmpty(value.trim())) {
        errorMsg = "Invalid REF#1.";
        return null;
      } else if (!bean.addReferences(secCode, value.trim())) {
        errorMsg = "Invalid REF#1.";
        return null;
      }
    }

    secCode = line.substring(173, 175);
    if (!Util.isEmpty(secCode) && null != secCode) {
      if (line.length() >= 204) {
        value = line.substring(175, 205);
      } else {
        value = line.substring(175);
      }
      if (Util.isEmpty(secCode)
          || Util.isEmpty(value)
          || Util.isEmpty(secCode.trim())
          || Util.isEmpty(value.trim())) {
        errorMsg = "Invalid REF#2.";
        return null;
      } else if (!bean.addReferences(secCode, value.trim())) {
        errorMsg = "Invalid REF#2.";
        return null;
      }
    }

    secCode = line.substring(205, 207);

    if (!Util.isEmpty(secCode) && null != secCode) {
      if (line.length() >= 213) {
        value = line.substring(207, 214);
      } else {
        value = line.substring(207);
      }
      if (Util.isEmpty(secCode)
          || Util.isEmpty(value)
          || Util.isEmpty(secCode.trim())
          || Util.isEmpty(value.trim())) {
        errorMsg = "Invalid REF#3.";
        return null;
      } else if (!bean.addReferences(secCode, value.trim())) {
        errorMsg = "Invalid REF#3.";
        return null;
      }
    }

    return bean;
  }

  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  /**
   * @param path
   * @param fileName
   * @param date
   * @return the file to import
   */
  public File lookForFile(String path, String fileName, JDate date) {

    final String fileNameFilter = fileName;
    // name filter
    FilenameFilter filter =
        new FilenameFilter() {
          @Override
          public boolean accept(File directory, String fileName) {
            return fileName.startsWith(fileNameFilter);
          }
        };

    final File directory = new File(path);
    final File[] listFiles = directory.listFiles(filter);

    for (File file : listFiles) {

      final Long dateFileMilis = file.lastModified();
      final Date dateFile = new Date(dateFileMilis);
      final JDate jdateFile = JDate.valueOf(dateFile);

      if (JDate.diff(date, jdateFile) == 0) {
        return file;
      }
    }

    return null;
  }

  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  @Override
  public String getFileName() {
    return this.file.getName();
  }

  @Override
  public String getTaskInformation() {
    return "Imports Bond REF_INTERNA, CUSIP, SEDOL from an asset control.";
  }

  /**
   * Inner class to manage massive saving Bond threads
   *
   * @author everis
   */
  class SegmentThread extends Thread {

    private Object[] tradeArray;
    private boolean noIntRefs = false;
    private int start;
    private double start_double;
    private int end;
    private double end_double;

    public SegmentThread(int segment, Object[] tradeArray, boolean noIntRefs) {
      this.tradeArray = tradeArray;
      this.noIntRefs = noIntRefs;

      /*Cambios en la l?gica -> c?lculos con doubles, resultados parseados a int. P
       * this.start = (this.tradeArray.length / NUMBER_THREADS) * segment;
      this.end = Math.min(this.start + this.tradeArray.length/ NUMBER_THREADS, this.tradeArray.length);*/
      this.start_double = (this.tradeArray.length / NUMBER_THREADS_DOUBLE) * segment;
      this.end_double =
          Math.min(
              this.start_double + this.tradeArray.length / NUMBER_THREADS_DOUBLE,
              this.tradeArray.length);
      this.start = (int) this.start_double;
      this.end = (int) this.end_double;
    }

    public SegmentThread(int segment, Object[] tradeArray) {
      this(segment, tradeArray, false);
    }

    @Override
    public void run() {
      //  Iterate and save
      for (int i = start; i < end; i++) {
        Bond bond = (Bond) tradeArray[i];
        boolean saveRemove = true;
        if (bond != null) {
          try {
            if (noIntRefs) {
              saveRemove = removeCrossRefs(bond);
            }
            if (saveRemove) {
              getDSConnection().getRemoteProduct().saveBond(bond, true);
            }
          } catch (RemoteException re) {
            if (noIntRefs) {
              Log.error(
                  LOG_CATEGORY_SCHEDULED_TASK,
                  "Error saving Bond after removing the cross references Attribute, ISIN="
                      + bond.getSecCode("ISIN")
                      + ", Currency="
                      + bond.getCurrency()
                      + ", Error="
                      + re.getLocalizedMessage());
            } else {
              Log.error(
                  LOG_CATEGORY_SCHEDULED_TASK,
                  "Error while saving the Bond with REF_INTERNA="
                      + bond.getSecCode(BOND_SEC_CODE_REF_INTERNA)
                      + ", Error="
                      + re.getLocalizedMessage());
            }
            Log.error(this, re); // sonar
          }
        }
      }
    }
  }

  /**
   * Manage Saving threads
   *
   * @author everis
   */
  private SegmentThread[] manageSavingThreads(Collection<Bond> collection, boolean noIntRefs) {
    SegmentThread threads[] = new SegmentThread[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i] = null;

      if (collection.size() >= NUM_THREADS || i == 0) {
        threads[i] = new SegmentThread(i, collection.toArray(), noIntRefs);
      }
    }

    for (int i = 0; i < NUM_THREADS; i++) {
      if (threads[i] != null) {
        threads[i].start();
      }
    }

    return threads;
  }

  private void joinThreads(SegmentThread[] threads) {
    try {
      for (int i = 0; i < NUM_THREADS; i++) {
        if (threads[i] != null) {
          threads[i].join();
        }
      }

    } catch (InterruptedException e) {
      Log.error(this, "Thread interruption building threads");
    }
  }
}
