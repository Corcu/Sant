package calypsox.tk.collateral.service.impl;

import calypsox.tk.collateral.service.LocalPLMarkHist;
import calypsox.tk.collateral.service.RemotePLMarkHist;
import calypsox.tk.collateral.service.impl.plMark.PLMarkHistSQL;
import calypsox.tk.collateral.service.impl.plMarkMock.PLMarkHistSQLMock;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.AuditSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PLMark;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.*;

@Stateless(name = "calypsox.tk.collateral.service.RemotePLMarkHist")
@Remote(RemotePLMarkHist.class)
@Local(LocalPLMarkHist.class)
public class RemotePLMarkHistImpl implements RemotePLMarkHist, LocalPLMarkHist {

    @Override
    public Collection<PLMark> getPLMarks(final String paramString, final JDate paramJDate) throws RemoteException {
        try {
            return PLMarkHistSQLMock.get(paramString, paramJDate);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public Set<PLMark> getPLMarks(final Map<Integer, HashSet<String>> paramMap, final String paramString,
                                  final JDate paramJDate) throws RemoteException {
        try {
            return PLMarkHistSQLMock.load(paramMap, paramString, paramJDate);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public Collection<PLMark> getPLMarks(final String where) throws RemoteException {
        try {
            return PLMarkHistSQLMock.get(where);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public Collection<PLMark> getPLMarksTableSwitch(final String where, boolean useTestTable) throws RemoteException {
        try {
            if (!useTestTable)
                return PLMarkHistSQL.get(where);
            else
                return PLMarkHistSQLMock.get(where);
        } catch (final PersistenceException e) {
            throw new CalypsoServiceException(Log.exceptionToString(e), e);
        }
    }

    @Override
    public Vector<Integer> getPLMarkIds(final String paramString) throws RemoteException {
        Connection localConnection = null;
        try {
            localConnection = ioSQL.getConnection();
            return PLMarkHistSQLMock.getPLMarkIds(localConnection, paramString);
        } catch (final Throwable localThrowable) {
            Log.error(Log.SQL, localThrowable);
            ioSQL.rollback(localConnection);
            throw new RemoteException(Log.exceptionToString(localThrowable), localThrowable);
        } finally {
            ioSQL.releaseConnection(localConnection);
        }
    }

    @Override
    public PLMark getPLMark(final int paramInt, final String paramString1, final String paramString2,
                            final JDate paramJDate) throws RemoteException {
        try {
            return PLMarkHistSQLMock.get(paramInt, paramString1, paramString2, paramJDate);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public PLMark getLatestPLMark(final int paramInt, final String paramString1, final String paramString2,
                                  final JDate paramJDate, final String paramString3, final String paramString4, final String paramString5)
            throws RemoteException {
        try {
            return PLMarkHistSQLMock.getLatest(paramInt, paramString1, paramString2, paramJDate, paramString3,
                    paramString4, paramString5);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public Collection<PLMark> getMaxDatedPLMarks(final String paramString, final boolean paramBoolean)
            throws RemoteException {
        try {
            return PLMarkHistSQLMock.getMaxDatedPLMarks(paramString, paramBoolean);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public PLMark getPLMark(final PLMark paramPLMark, final JDatetime paramJDatetime) throws RemoteException {
        try {
            return (PLMark) AuditSQL.undo(paramPLMark, paramPLMark.getId(), "PLMark", paramJDatetime);
        } catch (final Exception localException) {
            throw new RemoteException(Log.exceptionToString(localException), localException);
        }
    }

    @Override
    public boolean checkMarkTime(final int paramInt, final String paramString, final JDate paramJDate,
                                 final JDatetime paramJDatetime) throws RemoteException {
        try {
            return PLMarkHistSQLMock.checkMarkTime(paramInt, paramString, paramJDate, paramJDatetime);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public int countPLMarks(final String paramString) throws RemoteException {
        Connection localConnection = null;
        int i = 0;
        try {
            localConnection = ioSQL.getConnection();
            i = PLMarkHistSQLMock.countPLMarks(paramString, localConnection);
        } catch (final Throwable localThrowable) {
            Log.error(Log.SQL, localThrowable);
            ioSQL.rollback(localConnection);
            throw new RemoteException(Log.exceptionToString(localThrowable), localThrowable);
        } finally {
            ioSQL.releaseConnection(localConnection);
        }
        return i;
    }

    public void checkUPLMarks(String plMarkName, String plMarkValueName, Object nameBindVariable, Object valueBindVariable) {
        try {
            if (valueBindVariable != null) {
                new com.calypso.tk.refdata.sql.UserSQL().save((com.calypso.tk.refdata.User) valueBindVariable);
            }
            if (nameBindVariable != null) {
                new com.calypso.tk.refdata.sql.GroupSQL().save((com.calypso.tk.refdata.Group) nameBindVariable);
            }
        } catch (PersistenceException exc) {
            Log.error(this, "");
        }
    }


    @Override
    public PLMark getLatestPLMark(final int paramInt, final String paramString1, final String paramString2,
                                  final JDate paramJDate) throws RemoteException {
        try {
            return PLMarkHistSQLMock.getLatest(paramInt, paramString1, paramString2, paramJDate);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }

    @Override
    public Set<PLMark> getPLMarksForPLArchiveTrades(final List<Integer> paramList, final String paramString,
                                                    final JDate paramJDate) throws RemoteException {
        try {
            return PLMarkHistSQLMock.loadLatestPLMarks(paramList, paramString, paramJDate);
        } catch (final PersistenceException localPersistenceException) {
            throw new RemoteException(Log.exceptionToString(localPersistenceException), localPersistenceException);
        }
    }
}
