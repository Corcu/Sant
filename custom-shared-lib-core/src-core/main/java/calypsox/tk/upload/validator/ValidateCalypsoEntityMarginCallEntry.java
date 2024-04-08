package calypsox.tk.upload.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.CalypsoEntity;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.validator.ValidateCalypsoEntity;

public class ValidateCalypsoEntityMarginCallEntry extends ValidateCalypsoEntity {
	@Override
	public void validate(CalypsoObject object, Vector<BOException> errors) {
		CalypsoEntity entity = (CalypsoEntity) object;
		List<Integer> lstEntries = new ArrayList<Integer>();
		lstEntries.add(entity.getEntityId().intValue());
		if (!Util.isEmpty(lstEntries)) {
			try {

				List<MarginCallEntryDTO> dtos = ServiceRegistry.getDefault().getCollateralServer()
						.loadEntries(lstEntries);
				if (Util.isEmpty(dtos)) {
					BOException e = ErrorExceptionUtils.createException("21007",
							"MC Entry  :" + entity.getEntityId().intValue(), "54630", "Error getting MC Entry.");
					Log.error(this, e.getMessage());
					errors.addElement(e);
				}
			} catch (CollateralServiceException e) {
				Log.error(this, e);
				errors.addElement(ErrorExceptionUtils.createException("21007",
						"MC Entry  :" + entity.getEntityId().intValue(), "54630", e.getMessage()));
			}
		}

	}

}
