package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class LegalEntityAttributesCache {
  private static char KEY_SEPARATOR = '#';

  private static LegalEntityAttributesCache instance = null;

  private Map<String, LegalEntityAttribute> attributesMap = null;

  private LegalEntityAttributesCache() {
    loadAttributes();
  }

  public static String ALL_ROLES = "ALL";

  public static synchronized LegalEntityAttributesCache getInstance() {
    if (instance == null) {
      instance = new LegalEntityAttributesCache();
    }

    return instance;
  }

  public String getAttributeValue(Trade trade, String attributeType,
                                  boolean isCptyAttr) {
    String attributeValue = null;

    final LegalEntityAttribute leAttribute = getAttribute(trade, attributeType,
        isCptyAttr);
    if (leAttribute != null) {
      attributeValue = leAttribute.getAttributeValue();
    }

    return attributeValue;
  }

  public LegalEntityAttribute getAttribute(Trade trade, String attributeType,
                                           boolean isCptyAttr) {
    final int poId = trade.getBook().getLegalEntity().getId();
    int leId = poId;
    if (isCptyAttr) {
      leId = trade.getCounterParty().getId();
    }

    return getAttribute(poId, leId, ALL_ROLES, attributeType);
  }

  public LegalEntityAttribute getAttribute(int poId, int leId,
                                           String legalEntityRole, String attributeType) {
    LegalEntityAttribute leAttribute = null;

    final String strictKey = getKey(poId, leId, legalEntityRole, attributeType);
    leAttribute = attributesMap.get(strictKey);

    if (leAttribute == null) {
      final String allPOsKey = getKey(0, leId, legalEntityRole, attributeType);
      leAttribute = attributesMap.get(allPOsKey);
    }

    if (leAttribute == null) {
      final String allRolesKey = getKey(poId, leId, ALL_ROLES, attributeType);
      leAttribute = attributesMap.get(allRolesKey);
    }

    if (leAttribute == null) {
      final String allPOsAllRolesKey = getKey(0, leId, ALL_ROLES,
          attributeType);
      leAttribute = attributesMap.get(allPOsAllRolesKey);
    }

    return leAttribute;
  }

  private void loadAttributes() {
    try {
      final Vector<?> rawAttributes = DSConnection.getDefault()
          .getRemoteReferenceData().getLegalEntityAttributes("");

      attributesMap = new HashMap<String, LegalEntityAttribute>();

      if (!Util.isEmpty(rawAttributes)) {
        for (final Object rawAttribute : rawAttributes) {
          if (rawAttribute instanceof LegalEntityAttribute) {
            final LegalEntityAttribute leAttribute = (LegalEntityAttribute) rawAttribute;
            final String key = getKey(leAttribute);
            attributesMap.put(key, leAttribute);
          }
        }
      }

    } catch (final RemoteException e) {
      Log.error(this, "Could not retrieve LegalEntity attributes from database", e);
    }
  }

  private String getKey(LegalEntityAttribute attribute) {
    final int poId = attribute.getProcessingOrgId();
    final int leId = attribute.getLegalEntityId();
    final String legalEntityRole = attribute.getLegalEntityRole();
    final String attributeType = attribute.getAttributeType();

    return getKey(poId, leId, legalEntityRole, attributeType);
  }

  private String getKey(int poId, int leId, String legalEntityRole,
      String attributeType) {
    final StringBuilder key = new StringBuilder();

    key.append(poId);
    key.append(KEY_SEPARATOR);
    key.append(leId);
    key.append(KEY_SEPARATOR);
    key.append(legalEntityRole);
    key.append(KEY_SEPARATOR);
    key.append(attributeType);

    return key.toString();
  }

}
