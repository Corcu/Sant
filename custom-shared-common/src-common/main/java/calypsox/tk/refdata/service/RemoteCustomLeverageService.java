package calypsox.tk.refdata.service;

import calypsox.tk.refdata.CustomLeveragePercentage;
import com.calypso.tk.service.CalypsoMonitorableServer;

import java.rmi.RemoteException;

/**
 * @author x865229
 * date 02/01/2023
 * @see RemoteCustomLeverageService
 */
public interface RemoteCustomLeverageService extends CalypsoMonitorableServer {

    CustomLeveragePercentage loadAll() throws RemoteException;

    CustomLeveragePercentage loadByProduct(int productId) throws RemoteException;

    void save(CustomLeveragePercentage leveragePercentage) throws RemoteException;

    void save(CustomLeveragePercentage leveragePercentage, boolean removeNotIncluded) throws RemoteException;
}
