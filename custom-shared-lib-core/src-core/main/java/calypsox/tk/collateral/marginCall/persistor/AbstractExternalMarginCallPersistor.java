package calypsox.tk.collateral.marginCall.persistor;

import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.List;

public class AbstractExternalMarginCallPersistor implements
        ExternalMarginCallPersistor {

    protected ExternalMarginCallImportContext context;

    public AbstractExternalMarginCallPersistor() {
    }

    /**
     * @param context
     */
    public AbstractExternalMarginCallPersistor(
            ExternalMarginCallImportContext context) {
        this.context = context;
    }

    @Override
    public void persistEntry(List<Trade> tradeToSave, List<String> errors)
            throws Exception {

        if (Util.isEmpty(tradeToSave)) {
            return;
        }

        try {
            saveTrades(tradeToSave, errors);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Failed to save the Margin Call Trade", e);
            errors.add("Failed to save the Margin Call Trade");
        }
    }

    private void saveTrades(List<Trade> tradeToSave, List<String> errors)
            throws CalypsoServiceException {

        for (Trade trade : tradeToSave) {
            long idTrade = DSConnection.getDefault().getRemoteTrade()
                    .save(trade);

            if (idTrade <= 0) {
                errors.add("Failed to save the Margin Call Trade");
            }
        }
    }
}
