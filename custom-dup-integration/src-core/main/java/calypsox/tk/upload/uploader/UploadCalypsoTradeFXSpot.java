package calypsox.tk.upload.uploader;

import calypsox.tk.ccp.ClearingTradeFilterAdapter;
import calypsox.tk.upload.validator.ReprocessKwdValidator;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.upload.jaxb.CalypsoObject;

import java.util.Vector;

import static calypsox.tk.upload.uploader.util.UploadBondMultiCCyUtil.beforeSaveUtil;

public class UploadCalypsoTradeFXSpot extends com.calypso.tk.upload.uploader.UploadCalypsoTradeFXSpot implements ClearingTradeFilterAdapter, ReprocessKwdValidator {

    @Override
    protected void validateInputData(Vector<BOException> errors) {
        errors.removeAllElements();
    }

    @Override
    protected void beforeSave(CalypsoObject calypsoObject, Vector<BOException> errors) {
        beforeSaveUtil(calypsoObject, errors, this.trade, "FX");
        super.beforeSave(calypsoObject, errors);
    }
}
