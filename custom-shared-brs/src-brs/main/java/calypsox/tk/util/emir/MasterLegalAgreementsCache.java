package calypsox.tk.util.emir;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalAgreement;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class MasterLegalAgreementsCache {
  private static MasterLegalAgreementsCache instance = null;

  private Map<Integer, LegalAgreement> legalAgreementsMap = null;

  private MasterLegalAgreementsCache() {
    legalAgreementsMap = new TreeMap<Integer, LegalAgreement>();
  }

  public static MasterLegalAgreementsCache getInstance() {
    if (instance == null) {
      instance = new MasterLegalAgreementsCache();
    }

    return instance;
  }

  public void init(Collection<Trade> trades) {
    final Set<Integer> cptyIds = new TreeSet<Integer>();
    for (final Trade trade : trades) {
      cptyIds.add(trade.getCounterParty().getId());
    }

    final Queue<Integer> cptyIdsQueue = new LinkedList<Integer>(cptyIds);
    boolean queueIsEmpty = cptyIdsQueue.isEmpty();
    while (!queueIsEmpty) {
      final StringBuilder cptyIdsClause = new StringBuilder();

      for (int iCptyId = 0; iCptyId < 1000; iCptyId++) {

        if (!queueIsEmpty) {
          final Integer cptyId = cptyIdsQueue.poll();
          if (iCptyId > 0) {
            cptyIdsClause.append(',');
          }
          cptyIdsClause.append(cptyId);
        } else {
          break;
        }

        queueIsEmpty = cptyIdsQueue.isEmpty();
      }

      // int iCptyId = 0;
      // boolean isCptyId = iCptyId < 1000;
      // while (!(queueIsEmpty && isCptyId)) {
      // Integer cptyId = cptyIdsQueue.poll();
      // if (iCptyId > 0) {
      // cptyIdsClause.append(',');
      // }
      // cptyIdsClause.append(cptyId);
      // iCptyId++;
      //
      // queueIsEmpty = cptyIdsQueue.isEmpty();
      // isCptyId = iCptyId < 1000;
      // }

      final StringBuilder where = new StringBuilder();
      where.append("le_legal_agreement.legal_entity_id IN (");
      where.append(cptyIdsClause);
      where.append(") AND le_legal_agreement.product_type = 'ALL' AND le_legal_agreement.is_Master_B = 1");

      try {
        final Vector<?> rawLegalAgreements = DSConnection.getDefault()
            .getRemoteReferenceData()
            .getLegalAgreements(where.toString(), null);
        for (final Object rawLegalAgreement : rawLegalAgreements) {
          if (rawLegalAgreement instanceof LegalAgreement) {
            final LegalAgreement legalAgreement = (LegalAgreement) rawLegalAgreement;
            final int leId = legalAgreement.getLegalEntityId();
            if (!legalAgreementsMap.containsKey(leId)) {
              legalAgreementsMap.put(leId, legalAgreement);
            }
          }
        }

      } catch (final RemoteException e) {
        Log.error(this, "Could not retrieve Legal Agreements", e);
      }

      queueIsEmpty = cptyIdsQueue.isEmpty();
    }
  }

  public LegalAgreement getMasterLegalAgreement(Trade trade) {
    final int leId = trade.getCounterParty().getId();

    return legalAgreementsMap.get(leId);
  }
}
