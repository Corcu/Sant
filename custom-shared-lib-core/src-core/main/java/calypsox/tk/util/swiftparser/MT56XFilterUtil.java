package calypsox.tk.util.swiftparser;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public final class MT56XFilterUtil {
    /**
     * accept swift MSG
     *
     * @param swiftMessage swiftMessage
     * @return {@link boolean}
     */
    public static boolean acceptSwiftMsg(SwiftMessage swiftMessage){
        boolean accept = true;
        if(Optional.ofNullable(swiftMessage).isPresent()){
            String messageType = swiftMessage.getType();
            String dvName = messageType + ".SwiftMsgFilter";
            List<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), dvName);

            List<MT56XMsgFilter> swiftFilters = genSwiftFilters(dvName, domainValues);
            for (MT56XMsgFilter filter : swiftFilters) {
                accept = filter.accept(swiftMessage);
            }
        }
        return accept;
    }

    /**
     * generate swift filters
     *
     * @param dvName dvName
     * @param domainValues domainValues
     * @return {@link List}
     * @see List
     * @see MT56XMsgFilter
     */
    private static  List<MT56XMsgFilter> genSwiftFilters(String dvName, List<String> domainValues){
        List<String> includeList = new ArrayList<>();
        HashMap<String, MT56XMsgFilter> swiftFilters = new HashMap<>();

        for(String domain : domainValues) {
            if (domain.contains("EX_")) {
                MT56XMsgFilter filter = new MT56XMsgFilter();
                filter.setType(fillComment(dvName, domain));
                filter.setValue(domain.substring(3));
                swiftFilters.put(filter.getValue(),filter);
            }else {
                includeList.add(domain);
            }
        }

        fillIncludeExceptions(dvName,includeList,swiftFilters);

        return new ArrayList<>(swiftFilters.values());
    }


    /**
     * fill include exceptions
     *
     * @param dvName dvName
     * @param includeList includeList
     * @param swiftFilters swiftFilters
     */
    private static void fillIncludeExceptions(String dvName, List<String> includeList, HashMap<String, MT56XMsgFilter> swiftFilters){
        for(String domain : includeList){
            if(domain.contains("IN_")){
                String substring = domain.substring(3);
                if(substring.contains(".")) {
                    String[] parts = substring.split("\\.");
                    String filterType = fillComment(dvName, domain);
                    String bicCode = parts[0];
                    String value = parts[1];

                    if(null!=swiftFilters.get(bicCode)){
                        MT56XMsgFilter filter = swiftFilters.get(bicCode);
                        List<MT56XInclude> includeExceptions1 = filter.getIncludeExceptions();
                        if(!Util.isEmpty(includeExceptions1)){
                            boolean updated = false;
                            for(MT56XInclude inc : includeExceptions1){
                                if(inc.getType().equalsIgnoreCase(filterType)){
                                    inc.getValues().add(value);
                                    updated = true;
                                }
                            }
                            if(!updated){
                                MT56XInclude includeExceptions = new MT56XInclude();
                                includeExceptions.setBic(bicCode);
                                includeExceptions.setType(filterType);
                                List<String> values = new ArrayList<>();
                                values.add(value);
                                includeExceptions.setValues(values);
                                includeExceptions1.add(includeExceptions);
                                filter.setIncludeExceptions(includeExceptions1);
                            }
                        }else {
                            MT56XInclude includeExceptions = new MT56XInclude();
                            includeExceptions.setBic(bicCode);
                            includeExceptions.setType(filterType);
                            List<String> values = new ArrayList<>();
                            values.add(value);
                            includeExceptions.setValues(values);
                            List<MT56XInclude> incl = new ArrayList<>();
                            incl.add(includeExceptions);
                            filter.setIncludeExceptions(incl);
                        }
                    }
                }
            }
        }
    }

    /**
     * fill comment
     *
     * @param dvName dvName
     * @param dvValue dvValue
     * @return {@link String}
     * @see String
     */
    private static String fillComment(String dvName, String dvValue){
       return LocalCache.getDomainValueComment(DSConnection.getDefault(), dvName, dvValue);
    }
}
