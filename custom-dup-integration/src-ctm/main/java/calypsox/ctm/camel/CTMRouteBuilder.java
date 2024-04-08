package calypsox.ctm.camel;

import calypsox.camel.log.CamelLogBean;
import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.camel.AbstractCamelRouteBuilder;

/**
 * @author paisanu
 */
public class CTMRouteBuilder extends AbstractCamelRouteBuilder {

    @Override
    public void configure() {
        String logFilePath = "/calypso_interfaces/reports/export";
        String receivedFileName = "/?fileName=ctmToCalypsoReceived_${date:now:yyyyMMdd}.log&fileExist=Append";
        String sentFileName = "/?fileName=ionAckSent_${date:now:yyyyMMdd}.log&fileExist=Append";
        String ionStr = "ION_";
        String receiveStr = "RECEIVE";
        String logSystemStr = "logSystem";
        String fileStr = "file:";
        String componentName=getCamelComponentName();

        from(CTMUploaderConstants.ION_ACK_ROUTE_NAME)
                .routeId(buildRouteName(ionStr + "SENT"))
                .to(getCamelComponentName() + "." + "{{jms.type}}:topic:{{ion.output.queue.name}}")
                .bean(CamelLogBean.getNewInstance(), logSystemStr)
                .toF(fileStr + logFilePath + sentFileName);

        from(componentName + "." + "{{jms.type}}:{{ctm.input.queue.name}}")
                .routeId(buildRouteName(receiveStr))
                .bean(CamelLogBean.getNewInstance(), logSystemStr)
                .toF(fileStr + logFilePath + receivedFileName)
                .process(new PSEventUploadCTMCamelPublisher());

        from(componentName + "." + "{{jms.type}}:{{ion.input.queue.name}}")
                .routeId(buildRouteName(ionStr + receiveStr))
                .bean(CamelLogBean.getNewInstance(), logSystemStr)
                .toF(fileStr + logFilePath + receivedFileName)
                .process(new PSEventUploadIONCamelPublisher());
    }
}
