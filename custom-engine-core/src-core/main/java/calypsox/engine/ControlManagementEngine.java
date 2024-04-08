package calypsox.engine;

import calypsox.engine.management.EventWatcherService;
import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/** @author aalonsop */

public class ControlManagementEngine extends Engine {

  public static final String ENGINE_NAME = "ControlManagementEngine";

  private static final String WATCHERSERVICE_JNDI_NAME =
      "java:global/engineserver/EventWatcherService";

  public static final String LOG_CATEGORY="calypsox.engine.ControlManagementEngine";

  /** v11 */
  /* private final ScheduledExecutorService scheduler =
  Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());*/

  /** v14 & v16 */
  EventWatcherService watcherService;

  /**
   * Method that invokes the constructor of class Engine with parameter dsCon, hostName, port.
   *
   * @param dsCon
   * @param hostName
   * @param port
   */
  public ControlManagementEngine(DSConnection dsCon, String hostName, int port) {
    super(dsCon, hostName, port);
  }

  @Override
  public boolean process(PSEvent event) {
    return true;
  }

  /**
   *
   * @param dobatch
   * @throws ConnectException
   * @return this method returns true if the engine has been started.
   */
  @Override
  public boolean start(boolean dobatch) throws ConnectException {
    boolean returnedValue = super.start(dobatch);
    initWatcherService();
    if (watcherService != null) {
      watcherService.start();
    } else {
      returnedValue = false;
    }
    return returnedValue;
  }

  /**
   *
   */
  @Override
  public void stop() {
    if (watcherService != null) {
      watcherService.stop();
    }
    super.stop();
  }

  private void initWatcherService() {
    Context context = null;
    try {
      context = new InitialContext();
      watcherService = (EventWatcherService) context.lookup(WATCHERSERVICE_JNDI_NAME);
    } catch (NamingException exc) {
      Log.error(this, "Couldn't load timer service", exc.getCause());
    }
  }
}
