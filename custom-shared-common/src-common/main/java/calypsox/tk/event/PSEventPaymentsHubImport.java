package calypsox.tk.event;


import com.calypso.tk.event.PSEvent;


public class PSEventPaymentsHubImport extends PSEvent {


    private static final long serialVersionUID = 1L;
    private StringBuffer inputJson = null;


    public PSEventPaymentsHubImport() {
    }


    public PSEventPaymentsHubImport(StringBuffer inputJson) {
        this.inputJson = inputJson;
    }


    public StringBuffer getInputJson() {
        return inputJson;
    }


    public void setInputJson(StringBuffer inputJson) {
        this.inputJson = inputJson;
    }


}
