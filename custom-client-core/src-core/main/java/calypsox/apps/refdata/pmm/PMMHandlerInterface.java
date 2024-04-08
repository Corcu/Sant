package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;

public interface PMMHandlerInterface {
	boolean additionalProcessing();
	boolean saveElements();
	boolean deleteElements();
	Vector<?> loadElements(final List<String> list) throws CalypsoServiceException;
	void checkLoadedElements(final List<String> wantedElements, final Vector<?> foundElements);
	Class<?> getObjectClass(); 
	Object cloneObject(Object objectToClone) throws CloneNotSupportedException;
	Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException;
}
