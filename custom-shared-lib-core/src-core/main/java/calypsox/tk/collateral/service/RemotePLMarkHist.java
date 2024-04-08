package calypsox.tk.collateral.service;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.service.CalypsoMonitorableServer;

import java.rmi.RemoteException;
import java.util.*;

public abstract interface RemotePLMarkHist extends CalypsoMonitorableServer {

    public static final String SERVER_NAME = "PLMarkHistServer";
    public static final String SERVER_NICKNAME = "PLMark Hist Data";

    public abstract Collection<PLMark> getPLMarks(String paramString, JDate paramJDate) throws RemoteException;

    public abstract Set<PLMark> getPLMarks(Map<Integer, HashSet<String>> paramMap, String paramString, JDate paramJDate)
            throws RemoteException;

    public abstract PLMark getPLMark(int paramInt, String paramString1, String paramString2, JDate paramJDate)
            throws RemoteException;

    public abstract PLMark getLatestPLMark(int paramInt, String paramString1, String paramString2, JDate paramJDate,
                                           String paramString3, String paramString4, String paramString5) throws RemoteException;

    public abstract Collection<PLMark> getMaxDatedPLMarks(String paramString, boolean paramBoolean)
            throws RemoteException;

    public abstract PLMark getPLMark(PLMark paramPLMark, JDatetime paramJDatetime) throws RemoteException;

    public abstract Collection<PLMark> getPLMarks(String paramString) throws RemoteException;

    public abstract Vector<Integer> getPLMarkIds(String paramString) throws RemoteException;

    public abstract int countPLMarks(String paramString) throws RemoteException;

    public abstract boolean checkMarkTime(int paramInt, String paramString, JDate paramJDate, JDatetime paramJDatetime)
            throws RemoteException;

    public void checkUPLMarks(String plMarkName, String plMarkValueName, Object nameBindVariable, Object valueBindVariable);

    public abstract PLMark getLatestPLMark(int paramInt, String paramString1, String paramString2, JDate paramJDate)
            throws RemoteException;

    public abstract Set<PLMark> getPLMarksForPLArchiveTrades(List<Integer> paramList, String paramString,
                                                             JDate paramJDate) throws RemoteException;

    public abstract Collection<PLMark> getPLMarksTableSwitch(String where, boolean useTestTable) throws RemoteException;
}