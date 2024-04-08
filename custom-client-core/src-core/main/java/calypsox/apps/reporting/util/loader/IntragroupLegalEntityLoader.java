/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting.util.loader;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class IntragroupLegalEntityLoader extends SantLoader<Integer, String> {

    public static final String INTRAGROUP_ATTRIBUTE_NAME = "INTRAGROUP";
    public static final String INTRAGROUP_ATTRIBUTE_YES_VALUE = "YES";

    @SuppressWarnings("unchecked")
    public Map<Integer, String> load() {

        Vector<LegalEntity> intragroupLeVector = new Vector<LegalEntity>();

        // from clause
        StringBuilder fromClause = new StringBuilder();
        fromClause.append("legal_entity_role, le_attribute");

        // where clause
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("legal_entity.legal_entity_id = legal_entity_role.legal_entity_id");
        whereClause.append(" AND legal_entity_role.role_name = ");
        whereClause.append(Util.string2SQLString(LegalEntity.COUNTERPARTY));
        whereClause.append(" AND legal_entity.legal_entity_id = le_attribute.legal_entity_id");
        whereClause.append(" AND le_attribute.attribute_type = ");
        whereClause.append(Util.string2SQLString(INTRAGROUP_ATTRIBUTE_NAME));
        whereClause.append(" AND le_attribute.attribute_value = ");
        whereClause.append(Util.string2SQLString(INTRAGROUP_ATTRIBUTE_YES_VALUE));

        // get le from db
        try {
            intragroupLeVector = DSConnection.getDefault().getRemoteReferenceData()
                    .getAllLE(fromClause.toString(), whereClause.toString(), false, null);
        } catch (RemoteException e) {
            Log.error(this, "Error getting intragroup legal entities.", e);
            // return null;
        }

        // create map id-code
        if (!Util.isEmpty(intragroupLeVector)) {
            // this.map.put(-1, "");
            for (final LegalEntity le : intragroupLeVector) {
                this.map.put(le.getId(), le.getCode());
            }
        }

        // sort map
        final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(new ValueComparator(this.map));
        sortedMap.putAll(this.map);

        return sortedMap;

    }
}
