package calypsox.apps.refdata.pmm;

public class UploaderData {
	String configName;
	String elementString;
	String dataString;
	Object data;

	public String getElementString() {
		return elementString;
	}
	public void setElementString(String elementString) {
		this.elementString = elementString;
	}
	public String getDataString() {
		return dataString;
	}
	public void setDataString(String dataString) {
		this.dataString = dataString;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public String getConfigName() {
		return configName;
	}
	public void setConfigName(String configName) {
		this.configName = configName;
	}
}