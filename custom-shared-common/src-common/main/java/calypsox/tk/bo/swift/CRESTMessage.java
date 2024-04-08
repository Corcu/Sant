package calypsox.tk.bo.swift;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.util.MessageParseException;

@SuppressWarnings("unused")
public class CRESTMessage extends com.calypso.tk.bo.CRESTMessage {

    public void parseCRESTMessage(String msgString) throws CalypsoServiceException, MessageParseException {

        int startPos = msgString.indexOf(":16R:GENL");
        int endPos = msgString.indexOf("</Msg>");

        if (startPos==-1){
            int mt940startPos = msgString.indexOf(":20:");
            String msgStringMT940 = msgString.substring(mt940startPos, endPos);
            this.setFields(this.parseFields("4:" + msgStringMT940));

        } else {

            if (msgString.indexOf(":95P::PSET//") > 0) {
                int tag95Finder = msgString.indexOf(":95P::PSET//");
                String msgStringPartial = msgString.substring(startPos, tag95Finder + 20).concat("XXX");
                String msgStringFull = msgStringPartial.concat(msgString.substring(tag95Finder + 20, endPos));
                this.setFields(this.parseFields("4:" + msgStringFull));

            } else {
                String msgStringFull2 = msgString.substring(startPos, endPos);
                this.setFields(this.parseFields("4:" + msgStringFull2));
            }
        }

        String receiver = "BSCHESMM";
        if (this.getInputOutput().equals("I")) {
            this.setReceiver(receiver);
            this.setSender("BSCHGB2LXXX");
            this.setIncoming(true);
        } else {
            this.setSender(receiver);
        }

        int findMessageType = msgString.indexOf("type") + 6;
        String messageType = "MT".concat(msgString.substring(findMessageType,findMessageType+3));

        /*
        int findMessageTag20C = msgString.split(":20C::RELA//CYO").length;
        if (findMessageTag20C>1){
            long swiftMessageLongId = Long.parseLong(String.valueOf(msgString.split(":20C::RELA//CYO")[1].split("\r\n")[0]));
        } else {
            int findNTRF = msgString.split("NTRFCYO").length;
            if (findNTRF>1){
                long swiftMessageLongId = Long.parseLong(msgString.split("NTRFCYO")[1].split("//")[0]);
            }
        }
        */

        this.setType(messageType);
        String gateway = "SWIFT";
        this.setGateway(gateway);

    }

}