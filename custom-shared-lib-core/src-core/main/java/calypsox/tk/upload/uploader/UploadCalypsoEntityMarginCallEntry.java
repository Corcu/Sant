package calypsox.tk.upload.uploader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.Attribute;
import com.calypso.tk.upload.jaxb.Attributes;
import com.calypso.tk.upload.jaxb.CalypsoEntity;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.Field;
import com.calypso.tk.upload.jaxb.Fields;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.uploader.UploadCalypsoEntity;

public class UploadCalypsoEntityMarginCallEntry extends UploadCalypsoEntity {
	private static final long serialVersionUID = 5763217125873589054L;
	MarginCallEntryDTO marginCallEntryDTO = null;

	@Override
	public void upload(CalypsoObject object, Vector<BOException> errors) {
		CalypsoEntity entity = (CalypsoEntity) object;
		if (marginCallEntryDTO != null) {
			Attributes jaxbAttributes = entity.getAttributes();
			if (jaxbAttributes != null) {
				List<Attribute> jaxbAttributeList = jaxbAttributes.getAttribute();

				for (Attribute jaxbAttribute : jaxbAttributeList) {
					((MarginCallEntryDTO) getUploadObject()).addAttribute(jaxbAttribute.getAttributeName(),
							jaxbAttribute.getAttributeValue());
					
				}				
			}
			Fields fld= entity.getFields();
			if(fld!=null && !Util.isEmpty(fld.getField())) {
				for (Field field : fld.getField()) {

					String name = field.getName();
					String value = field.getValue();
					try {
						Method m = Class.forName(MarginCallEntryDTO.class.getName()).getMethod("set" + name,
								String.class);

						m.invoke(((MarginCallEntryDTO) getUploadObject()), value);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
							| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
						Log.error(this, "Field: " + name, e);
					}
					
				}
			}
		}
//		super.upload(object, errors);
	}

	@Override
	public Object getUploadObject() {
		return marginCallEntryDTO;
	}

	@Override
	public long saveObject(CalypsoObject calypsoObject, Vector<BOException> errors) {
		CalypsoEntity entity = (CalypsoEntity) calypsoObject;
		if (marginCallEntryDTO != null) {
			try {
				String action = entity.getAction();
				MarginCallEntryDTO mceDTO = ServiceRegistry.getDefault().getCollateralServer()
						.saveWithReturn(((MarginCallEntryDTO) getUploadObject()), action, TimeZone.getDefault(), false);
				if (mceDTO != null) {
					entity.setEntityName(String.valueOf(marginCallEntryDTO.getId()));
					return marginCallEntryDTO.getId();
				}
				Log.error("UPLOADER", "Error applying action.");
				errors.addElement(ErrorExceptionUtils.createException("21007",
						"MC Entry  :" + ((MarginCallEntryDTO) getUploadObject()).getId(), "54630",
						"Error applying action."));
			} catch (Exception e) {
				Log.error("UPLOADER", e.getMessage());
				errors.addElement(ErrorExceptionUtils.createException("21007",
						"MC Entry  :" + ((MarginCallEntryDTO) getUploadObject()).getId(), "54630", e.getMessage()));
			}
		}
		return 0L;
	}

	@Override
	public Object createObject(CalypsoObject calypsoObject, Vector<BOException> errors) {
		CalypsoEntity entity = (CalypsoEntity) calypsoObject;

		List<Integer> lstEntries = new ArrayList<Integer>();
		lstEntries.add(entity.getEntityId().intValue());
		if (!Util.isEmpty(lstEntries)) {
			try {

				List<MarginCallEntryDTO> dtos = ServiceRegistry.getDefault().getCollateralServer()
						.loadEntries(lstEntries);
				for (MarginCallEntryDTO marginCallEntryDTO : dtos) {
					this.marginCallEntryDTO = marginCallEntryDTO;
					return marginCallEntryDTO;
				}

			} catch (CollateralServiceException e) {
				Log.error(this, e);
				errors.addElement(ErrorExceptionUtils.createException("21007",
						"MC Entry  :" + entity.getEntityId().intValue(), "54630", e.getMessage()));
			}
		}

		return super.createObject(calypsoObject, errors);
	}
}
