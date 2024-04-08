package calypsox.engine.management;

import calypsox.engine.ControlManagementEngine;
import calypsox.engine.management.command.EngineEventsControlCommand;
import calypsox.engine.management.command.EventQueueIncrementCheckCommand;
import calypsox.engine.management.command.EventQueueSizeCheckCommand;
import calypsox.engine.management.model.ExtendedEngineDescription;
import com.calypso.engine.configuration.EngineDescription;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.RemoteEngineManagerService;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.*;
import java.util.*;

/** @author aalonsop */


@Singleton
@Startup
public class EventWatcherService {

  /**
   * Inherited from OLD ControlManagementEngine
   */
  private static final String CONTROL_MANAGEMENT_ENGINELIST =
      "ControlManagementEngineListOfEngines";

/**
* In miliseconds
*/
  private static final String ENGINE_CHECK_INTERVAL_DV_NAME= ControlManagementEngine.ENGINE_NAME+".CHECKING_INTERVAL";
  private static final int DEFAULT_CHECKOUT_INTERVAL=60000;

  private Vector<String> engineList;

  @Resource TimerService timerService;

  List<EngineEventsControlCommand> commandList= new LinkedList<>();

  Map<String, ExtendedEngineDescription> engineSnapshotMap=new HashMap<>();


  public void start() {
    destroyTimers();
    engineList =
        LocalCache.getDomainValues(DSConnection.getDefault(), CONTROL_MANAGEMENT_ENGINELIST);
    commandList.add(new EventQueueSizeCheckCommand());
    commandList.add(new EventQueueIncrementCheckCommand());
    if(Util.isEmpty(timerService.getTimers())){
      TimerConfig timerConfig = new TimerConfig(this.getClass().getSimpleName(), false);
      timerService.createIntervalTimer(60000, getCheckingIntervalValue(), timerConfig);
    }
  }



  @PreDestroy
  public void stop() {
   destroyTimers();
  }

  private void destroyTimers(){
    if (timerService != null) {
      Collection<Timer> timers = timerService.getTimers();
      for (Timer timer : timers) {
        timer.cancel();
      }
    }
  }

  @Timeout
  public void engineCheck() {
    getUpdatedEngineSnapshotMap();
    for (String engineName : engineList) {
      ExtendedEngineDescription engineBean = engineSnapshotMap.get(engineName);
      if (engineBean != null) {
        for(EngineEventsControlCommand eventsControlCommand: commandList){
          eventsControlCommand.checkEngineMetrics(engineBean);
        }
      }
    }
    Log.debug(this, "Engine's Event Watcher Timer iteration");
  }


  private void getUpdatedEngineSnapshotMap() {
    RemoteEngineManagerService managerService = DSConnection.getDefault().getService(RemoteEngineManagerService.class);
    try {
      for(EngineDescription engineDescription: managerService.getAllEngineDescriptions()){
        ExtendedEngineDescription extendedEngineDescription=engineSnapshotMap.get(engineDescription.getName());
        engineSnapshotMap.put(engineDescription.getName(),ExtendedEngineDescription.valueOf(extendedEngineDescription, engineDescription));
      }
    } catch (CalypsoServiceException exc) {
      Log.warn(this, "Could't get engine's description", exc.getCause());
    }
  }

  private int getCheckingIntervalValue(){
  int checkOutInterval=DEFAULT_CHECKOUT_INTERVAL;
    Vector<String> domainValues=LocalCache.getDomainValues(DSConnection.getDefault(), ENGINE_CHECK_INTERVAL_DV_NAME);
    if(!Util.isEmpty(domainValues)&& !Util.isEmpty(domainValues.get(0))){
        try {
          checkOutInterval = Integer.valueOf(domainValues.get(0));
        } catch (NumberFormatException exc) {
          Log.system(
                  ControlManagementEngine.LOG_CATEGORY,
                  "Couldn't parse " + ENGINE_CHECK_INTERVAL_DV_NAME + " domainValue",
                  exc.getCause());
        }
      }
     return checkOutInterval;
  }
}
