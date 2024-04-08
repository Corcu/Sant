package calypsox.util.binding;

import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class provides basic operations to work with bind variables (CalypsoBindVariable object)
 *
 * @author aalonsop
 */
public class CustomBindVariablesUtil {

    /**
     *
     */
    private static final String STRING_STR = "STRING";
    /**
     *
     */
    private static final String VARCHAR_STR = "VARCHAR";

    /**
     *
     */
    private CustomBindVariablesUtil() {
        //Util class so its empty
    }

    /**
     * @param bindValue
     * @return
     */
    public static List<CalypsoBindVariable> createNewBindVariable(final Object bindValue) {
        return addNewBindVariableToList(bindValue, null);
    }

    /**
     * @param bindValue
     * @param variableList
     * @return
     */
    public static List<CalypsoBindVariable> addNewBindVariableToList(final Object bindValue, final List<CalypsoBindVariable> variableList) {
        CalypsoBindVariable bindVariable = null;
        try {
            bindVariable = createBindVariableBasedOnType(bindValue);
        } catch (IllegalAccessException | NoSuchFieldException exc) {
            Log.error(CustomBindVariablesUtil.class, exc.getCause());
        }
        return checkAndCreateNewCalypsoBindVariableList(variableList, bindVariable);
    }


    /**
     * @param c
     * @param bindVariables
     * @return The sql String to be ready for the bindVariables assignment. Same logic as the Util core class one,
     * but this handles all SQL types
     */
    public static String collectionToPreparedInString(Collection<?> variableList, List<CalypsoBindVariable> bindVariables) {
        if (Util.isEmpty(variableList)) {
            return "";
        } else {
            StringBuilder strBuilder = new StringBuilder(variableList.size() * 20);
            Iterator iterator = variableList.iterator();
            if (bindVariables == null) {
                bindVariables = new ArrayList<>();
            }
            while (iterator.hasNext()) {
                processCollectionToPreparedInStringIteration(strBuilder, iterator, bindVariables);

            }
            return strBuilder.toString();
        }
    }

    /**
     * Method needed to reduce collectionToPreparedInString cognitive complexity
     */
    private static void processCollectionToPreparedInStringIteration(StringBuilder strBuilder, Iterator iterator, List<CalypsoBindVariable> bindVariables) {
        Object item = iterator.next();
        if (item != null) {
            strBuilder.append("?");
            if (iterator.hasNext()) {
                strBuilder.append(",");
            }
            try {
                bindVariables.add(createBindVariableBasedOnType(item));
            } catch (IllegalAccessException | NoSuchFieldException exc) {
                Log.error(CustomBindVariablesUtil.class, exc.getCause());
            }
        }
    }

    /**
     * @param objectToBind
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static CalypsoBindVariable createBindVariableBasedOnType(Object objectToBind) throws NoSuchFieldException, IllegalAccessException {
        CalypsoBindVariable bindVariable = null;
        if (objectToBind != null) {
            String bindVariableClassName = convertStringValue(objectToBind.getClass().getSimpleName().toUpperCase());
            Field field = CalypsoBindVariable.class.getField(bindVariableClassName);
            int bindVariableType;
            if (field != null) {
                bindVariableType = field.getInt(null);
                bindVariable = new CalypsoBindVariable(bindVariableType, objectToBind);
            }
        }
        return bindVariable;
    }

    /**
     * @param bindVariableList
     * @param bindVariable
     * @return
     */
    private static List<CalypsoBindVariable> checkAndCreateNewCalypsoBindVariableList(List<CalypsoBindVariable> bindVariableList, CalypsoBindVariable bindVariable) {
        List<CalypsoBindVariable> list = bindVariableList;
        if (Util.isEmpty(bindVariableList)) {
            list = new ArrayList<>();
        }
        list.add(bindVariable);
        return list;
    }

    /**
     * @param value
     * @return If the value is equals to "STRING"
     */
    private static String convertStringValue(String value) {
        String res = value;
        if (value.equalsIgnoreCase(STRING_STR)) {
            res = VARCHAR_STR;
        }
        return res;
    }
    //Util.collectionToPreparedInString
}

