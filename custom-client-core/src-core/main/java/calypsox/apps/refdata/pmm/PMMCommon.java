package calypsox.apps.refdata.pmm;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.calypso.tk.service.DSConnection;

public class PMMCommon {
	public static DSConnection DS = null;
	
	public static Set<String> ELEMENTS_IDENTIFIERS;
	public static Map<SettersData, List<UploaderData>> UPLOADER_DATA_MAP;
	public static Map<String, Object> MODIFIED_ELEMENTS;
	public static Map<String, Object> MODIFIABLE_ELEMENTS;
	public static final String DEFAULT_OBJECT_ACTION_NAME = "DEFAULT_OBJECT_ACTION";
	public static final String DEFAULT_ACTION = "AMEND";
	public static String DEFAULT_OBJECT_ACTION;
	
	
	public final static String TYPE_ERROR = "ERROR";
	public final static String TYPE_WARNING = "WARNING"; 
	public final static String TYPE_INFO = "INFO";
	
	public static String IDENTIFIER_NAME = null;
	
	public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
	    return from.stream().map(func).collect(Collectors.toList());
	}
	
	public static <T, U> U[] convertArray(T[] from, Function<T, U> func, IntFunction<U[]> generator) {
		return Arrays.stream(from).map(func).toArray(generator);
	}
	
	public static String getCurrentDateTime() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return formatter.format(date);
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static Class[] getClasses(String packageName)
	        throws ClassNotFoundException, IOException {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    assert classLoader != null;
	    String path = packageName.replace('.', '/');
	    Enumeration<URL> resources = classLoader.getResources(path);
	    List<File> dirs = new ArrayList<File>();
	    while (resources.hasMoreElements()) {
	        URL resource = resources.nextElement();
	        dirs.add(new File(resource.getFile()));
	    }
	    ArrayList<Class> classes = new ArrayList<Class>();
	    for (File directory : dirs) {
	        classes.addAll(findClasses(directory, packageName));
	    }
	    return classes.toArray(new Class[classes.size()]);
	}
	
	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
	    List<Class> classes = new ArrayList<Class>();
	    if (!directory.exists()) {
	        return classes;
	    }
	    File[] files = directory.listFiles();
	    for (File file : files) {
	        if (file.isDirectory()) {
	            assert !file.getName().contains(".");
	            classes.addAll(findClasses(file, packageName + "." + file.getName()));
	        } else if (file.getName().endsWith(".class")) {
	            classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
	        }
	    }
	    return classes;
	}
	
	
	/**
     * Splits the given Collection in to sub Lists with each sublist contains atmost limit no of items
     */
    public static <T> List<List<T>> splitCollection(Collection<T> collection, int limit) {
        List<List<T>> finalList = new ArrayList<List<T>>();

        List<T> list = new ArrayList<T>(collection);
        int start = 0;

        for (int i = 0; i <= (list.size() / limit); i++) {
            int end = (i + 1) * limit;
            if (end > list.size()) {
                end = list.size();
            }
            List<T> subList = list.subList(start, end);
            finalList.add(subList);
            start = end;
        }

        return finalList;
    }
}
