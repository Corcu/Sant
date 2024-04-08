package calypsox.tk.util;

import calypsox.tk.csdr.CSDRFiFlowFileReader;
import calypsox.tk.csdr.CSDRFiFlowLineBean;
import calypsox.tk.csdr.CSDRFiFlowTradeBuilder;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * @author aalonsop
 */
public class ScheduledTaskCSDR_FIFLOW extends ScheduledTask {

    private final String filePathAttr="File Path";
    private final String fileNameAttr="File Name";
    private final String dummyCptyAttr="Dummy Ctpy Short Name";

    @Override
    public String getTaskInformation() {
        return "Import CSDR file from FIFlow and creates related failed SimpleTransfer trades";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String filePath=getAttribute(filePathAttr);
        String fileName=parseFileName(getAttribute(fileNameAttr));
        String dummyCtpyName=getAttribute(dummyCptyAttr);
        List<CSDRFiFlowLineBean> mappedLines=new CSDRFiFlowFileReader(filePath+fileName).read();
        ExternalArray trades=new CSDRFiFlowTradeBuilder(mappedLines,dummyCtpyName,getValuationDatetime().getJDate(TimeZone.getDefault())).build();
        return super.process(ds,ps)&& saveTrades(trades);
    }

        //NEED TO CHECK IF TRADES WITH SAME REFERENCE EXIST IN THE SYSTEM
    private boolean saveTrades(ExternalArray trades){
        boolean success=true;
        if(trades!=null&&trades.size()>0) {
            try {
                DSConnection.getDefault().getRemoteTrade().saveTrades(trades);
                //BOSimpleTransferHandler handler=new BOSimpleTransferHandler();
            } catch (CalypsoServiceException exc) {
                success = false;
                Log.error(this, exc.getCause());
            }
        }
        return success;
    }
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(fileNameAttr));
        attributeList.add(attribute(filePathAttr));
        attributeList.add(attribute(dummyCptyAttr));
        return attributeList;
    }

    private String parseFileName(String fileName){
        String pattern="YYYYMMdd";
        String parsedFileName=fileName;
        SimpleDateFormat formatter=new SimpleDateFormat(pattern);
        parsedFileName=parsedFileName.replace(pattern,formatter.format(getValuationDatetime()));
        return parsedFileName;
    }
}
