/**
 *
 */
package calypsox.tk.collateral.service.impl;

import calypsox.tk.collateral.service.LocalSantNotificationService;
import calypsox.tk.collateral.service.RemoteSantNotificationService;
import com.calypso.tk.collateral.service.impl.RMICollateralServer;

import javax.ejb.*;

/**
 * @author aela
 *
 */
@Stateless(name = "calypsox.tk.collateral.service.RemoteSantNotificationService")
@Remote(RemoteSantNotificationService.class)
@Local(LocalSantNotificationService.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SantNotificationService extends RMICollateralServer implements RemoteSantNotificationService, LocalSantNotificationService {
}
