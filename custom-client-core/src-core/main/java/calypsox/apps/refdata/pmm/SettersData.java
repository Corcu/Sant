package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;

public class SettersData {
	String configName;
	String configNiceName;
	String getterMethodName;
	Method getterMethod;
	String setterMethodName;
	Method setterMethod;
	String setterDataTypeName;
	Class<?> setterDataTypeClass;
	Class<?> setterDataSubTypeClass;
	String basicDataType;
	boolean isPrim = false;
	
	public String getGetterMethodName() {
		return getterMethodName;
	}
	public void setGetterMethodName(String getterMethodName) {
		this.getterMethodName = getterMethodName;
	}
	public Method getGetterMethod() {
		return getterMethod;
	}
	public void setGetterMethod(Method getterMethod) {
		this.getterMethod = getterMethod;
	}

	public String getSetterMethodName() {
		return setterMethodName;
	}
	public void setSetterMethodName(String setterMethodName) {
		this.setterMethodName = setterMethodName;
	}
	public Method getSetterMethod() {
		return setterMethod;
	}
	public void setSetterMethod(Method setterMethod) {
		this.setterMethod = setterMethod;
	}
	public String getSetterDataTypeName() {
		return setterDataTypeName;
	}
	public void setSetterDataTypeName(String setterDataTypeName) {
		this.setterDataTypeName = setterDataTypeName;
	}
	public Class<?> getSetterDataTypeClass() {
		return setterDataTypeClass;
	}
	public void setSetterDataTypeClass(Class<?> setterDataTypeClass) {
		this.setterDataTypeClass = setterDataTypeClass;
	}
	public Class<?> getSetterDataSubTypeClass() {
		return setterDataSubTypeClass;
	}
	public void setSetterDataSubTypeClass(Class<?> setterDataSubTypeClass) {
		this.setterDataSubTypeClass = setterDataSubTypeClass;
	}
	public String getConfigName() {
		return configName;
	}
	public void setConfigName(String configName) {
		this.configName = configName;
	}
	public String getConfigNiceName() {
		return configNiceName;
	}
	public void setConfigNiceName(String configNiceName) {
		this.configNiceName = configNiceName;
	}
	public String getBasicDataType() {
		return basicDataType;
	}
	public void setBasicDataType(String basicDataType) {
		this.basicDataType = basicDataType;
	}
	public boolean isPrim() {
		return isPrim;
	}
	public void setPrim(boolean isPrim) {
		this.isPrim = isPrim;
	}
}