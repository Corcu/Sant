package calypsox.tk.upload.customizer;

import calypsox.tk.bo.UploaderMessageHandler;
import calypsox.tk.report.GlobalMTACollateralReport;
import calypsox.tk.report.GlobalMTACollateralReportTemplate;
import calypsox.tk.report.globalmta.CollateralConfigMTAGroup;
import calypsox.tk.upload.validator.ValidateMarginCallContract;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.customizer.DefaultUploadCustomizer;
import com.calypso.tk.upload.jaxb.MarginCallContract;
import com.calypso.tk.upload.uploader.UploadObject;

import java.util.Optional;
import java.util.Vector;

public class LyncsUploadCustomizer extends DefaultUploadCustomizer {

	@Override
	public void postSave(UploadObject uploadObject) {
		if (uploadObject == null || uploadObject.getJaxbObject() == null || !(uploadObject.getJaxbObject() instanceof MarginCallContract)) {
			return;
		}
		MarginCallContract jaxbMarginCall = (MarginCallContract)uploadObject.getJaxbObject();
		
		String mccName = jaxbMarginCall.getMarginCallConfigName();
		if (mccName != null && mccName.contains(ValidateMarginCallContract.RAND_NAME_SEPARATOR)) { // Means name has been forced
			jaxbMarginCall.setMarginCallConfigName(mccName.substring(0, mccName.indexOf(ValidateMarginCallContract.RAND_NAME_SEPARATOR)));

			StringBuilder sb = new StringBuilder();
			sb.append("MC Contract Name ");
			sb.append(jaxbMarginCall.getMarginCallConfigName());
			sb.append(" already exists - Saved this one as ");
			sb.append(mccName);
			String infoDesc = sb.toString();
			Log.info(this, "Changed MC Contract Name while importing : " + infoDesc);
			buildAndSaveTask(infoDesc);
		}
		isValidGlobalMTAIfActivated(jaxbMarginCall);
	}

	private void buildAndSaveTask(String taskInfo){
		Task task = new UploaderMessageHandler().buildTask(taskInfo, "", "Uploader");
		try {
			DSConnection.getDefault().getRemoteBO().save(task);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not save Task : " + e.toString());
		}
	}
	public void isValidGlobalMTAIfActivated(MarginCallContract margincallconfig){
		String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","ACTIVATE_GLOBALMTA_VALIDATOR1");
		if(Boolean.parseBoolean(activationFlag)){
			isValidGlobalMTA(margincallconfig);
		}
	}
	private void isValidGlobalMTA(MarginCallContract margincallconfig){
		CollateralConfigMTAGroup mtaGroup=getGlobalMTAData(margincallconfig);
		if(mtaGroup!=null && mtaGroup.isThresholdCptyExceeded()){
			String msg="Contract's global MTA is being exceeded. Total LEI's MTA: "+mtaGroup.getTotalMTACpty()+ "USD";
			buildAndSaveTask(msg);
		}
	}
	private CollateralConfigMTAGroup getGlobalMTAData(MarginCallContract margincallconfig){
		Vector<Integer> leId=new Vector<>();
		LegalEntity le=BOCache.getLegalEntity(DSConnection.getDefault(),margincallconfig.getLegalEntityPartyDefinition().getLegalEntity());
		leId.add(Optional.ofNullable(le).map(LegalEntity::getId).orElse(0));
		Vector<Integer> poId=new Vector<>();
		LegalEntity po=BOCache.getLegalEntity(DSConnection.getDefault(),margincallconfig.getProcessingOrgPartyDefinition().getLegalEntity());
		poId.add(Optional.ofNullable(po).map(LegalEntity::getId).orElse(0));
		GlobalMTACollateralReportTemplate template=new GlobalMTACollateralReportTemplate();
		template.put("LEGAL_ENTITY_IDS", leId);
		template.put("PROCESSING_ORG_IDS", poId);
		GlobalMTACollateralReport report=new GlobalMTACollateralReport();
		report.setValuationDatetime(new JDatetime());
		report.setReportTemplate(template);
		DefaultReportOutput output= (DefaultReportOutput) report.load(new Vector());
		return (CollateralConfigMTAGroup) Optional.ofNullable(output.getRows())
				.map(reportRows -> reportRows[0]).map(row->row.getProperty(CollateralConfigMTAGroup.class.getSimpleName()))
				.orElse(null);
	}
}
