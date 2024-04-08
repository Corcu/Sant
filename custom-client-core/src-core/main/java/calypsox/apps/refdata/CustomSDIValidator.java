/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.refdata;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.calypso.apps.refdata.SDIValidator;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.SDIRelationShip;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.refdata.User;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.ReferenceDataServerImpl;
import com.calypso.tk.service.RemoteReferenceData;

import javax.swing.*;

public class CustomSDIValidator implements SDIValidator {

	public static final String SDI_ATTR_IS_TECHNICAL = "Is Technical";
	public static final String EFFECTIVE_FROM = "Effective From";
	public static final String EFFECTIVE_TO	= "Effective To";

	@SuppressWarnings("rawtypes")
	@Override
	public int getRegistrationId(SettleDeliveryInstruction sdi, Frame frame, Vector messages) {
		return 0;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public boolean isValidInput(SettleDeliveryInstruction sdi, Frame frame, Vector messages) {
		User user = DSConnection.getDefault().getUserInfo();

		if (isUserRestricted(user)){
			if (!filterAgainstPEBCAK(sdi, frame, messages)) {
				return false;
			}
		}
		if ((!AccessUtil.isAdmin(user)) && !isStaticDataUser(user)) {
			messages.add("Static Data Users are not allowed to create/update/delete Technical SDIs.");
			return false;
		}
		return true;
	}

	public boolean filterAgainstPEBCAK(SettleDeliveryInstruction sdi, Frame frame, Vector messages) {

		if (sdi.getId() == 0) {
			SettleDeliveryInstruction oldSDI = ((BOSettlDeliveryWindow) frame).getOldSdi();
			if (oldSDI != null) {
				if (oldSDI.getEffectiveDateTo() == null) {
					if (!sdiDateControl(frame,oldSDI,EFFECTIVE_TO)){
						return false;
					}

					try {
						DSConnection.getDefault().getRemoteReferenceData().save(oldSDI);
						AppUtil.displayMessage("The existing SDI with id: " + oldSDI.getId() + " has been saved.", frame);
					} catch (CalypsoServiceException e) {
						AppUtil.displayMessage("Something went wrong when saving the existing SDI with id: " + oldSDI.getId() + ". Please try again", frame);
						return false;
					}
				}
			}
			if (!checkPossibleInterferenceExistingSDIs(sdi,oldSDI, frame)){
				return false;
			}
			if (sdi.getEffectiveDateFrom() == null) {
				if (!sdiDateControl(frame,sdi,EFFECTIVE_FROM)){
					return false;
				}
			}
		}

		if (sdi.getId() != 0) {
			try {
				RemoteReferenceData rf = DSConnection.getDefault().getRemoteReferenceData();
				if (rf.isUsed(sdi)) {
					String comment = "This SDI is already used. You can only modify field 'To Effective', if you want to modify it please click 'YES'.\nAny other modification is not allowed, please choose option 'Save As New' instead of 'Save'";
					boolean choise = AppUtil.displayQuestion(comment, frame);
					if (choise == true){
						if (sdiDateControl(frame,sdi,EFFECTIVE_TO)){
							return true;
						}
					}
					return false;

				}

				if (!checkPossibleInterferenceExistingSDIs(sdi,null, frame)){
					return false;
				}

				if (sdi.getEffectiveDateFrom() == null) {
					if (!sdiDateControl(frame,sdi,EFFECTIVE_FROM)){
						return false;
					}
				}
			} catch (CalypsoServiceException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
		// domain value sdiVersionNoChanges
	}

	private boolean sdiDateControl(Frame frame, SettleDeliveryInstruction sdi, String date){
		String dateString = null;
		JDate dateJDate = null;
		while (dateJDate == null) {
			String coment = "";
			if (sdi.getId()==0){
				coment = "Please place a '" + date + "' date on the new SDI";
			} else{
				coment = "Please place a '" + date + "' date on the existing SDI " + sdi.toString() + " with id " + sdi.getId();
			}
			dateString = (String) JOptionPane.showInputDialog(frame, coment,
					frame.getTitle(), 2, null, null, JDate.getNow().toString());
			dateJDate = Util.stringToJDate(dateString);
			if (dateString == null) {
				return false;
			}
			if (dateJDate == null) {
				AppUtil.displayError("The entered date was invalid, please place a '"+date+"' date (dd/mm/yyyy)", frame);
			}
		}

		//save the SDI with Effective To/From To Date
		if (date.equalsIgnoreCase(EFFECTIVE_TO)){
			sdi.setEffectiveDateTo(dateJDate);
		} else if (date.equals(EFFECTIVE_FROM)){
			sdi.setEffectiveDateFrom(dateJDate);
		}

		return true;
	}

	protected boolean checkPossibleInterferenceExistingSDIs(SettleDeliveryInstruction newSDI, SettleDeliveryInstruction oldSDI, Frame frame) {
		HashMap<String,SettleDeliveryInstruction> interferenceExistingSDIs = new HashMap<>();
		if (newSDI.getPreferredB()) {
			Vector instructions = null;
			try {
				String w = "bene_le =? AND le_role = ?";
				List<CalypsoBindVariable> bindVariables = new ArrayList();
				bindVariables.add(new CalypsoBindVariable(4, newSDI.getBeneficiaryId()));
				bindVariables.add(new CalypsoBindVariable(12, newSDI.getRole()));
				instructions = DSConnection.getDefault().getRemoteReferenceData().getInterfereSDI(newSDI, w, bindVariables);
			} catch (Exception e) {
				Log.error("SDIWindow", e.getMessage());
			}
			if (!Util.isEmpty(instructions)) {
				String interferedSDIs = "";
				for (int i = 0; i < instructions.size(); ++i) {
					SettleDeliveryInstruction si = (SettleDeliveryInstruction) instructions.elementAt(i);
					if (oldSDI!=null){
						if (si.getId()==oldSDI.getId()){
							continue;
						}
					}
					if (si.getId() > 0) {
						if (si.getEffectiveDateTo()==null) {
							String key = si.toString() + " (id:" + si.getId() + ")";
							interferenceExistingSDIs.put(key, si);
							interferedSDIs += key + "\n";
						}
					}
				}
				if (!interferenceExistingSDIs.isEmpty() && !Util.isEmpty(interferedSDIs)){
					if (!AppUtil.displayQuestion("The next SDI could interfere with the SDI you are trying to create:\n" + interferedSDIs +
							"\nYou should place the 'Effective To' field before continue\nDo you want to continue?", frame)) {
						return false;
					} else{
						for (HashMap.Entry<String,SettleDeliveryInstruction> entry : interferenceExistingSDIs.entrySet()) {
							SettleDeliveryInstruction sdiInterfered = entry.getValue();
							if (!sdiDateControl(frame,sdiInterfered,EFFECTIVE_TO)){
								return false;
							}
							try {
								DSConnection.getDefault().getRemoteReferenceData().save(sdiInterfered);
								AppUtil.displayMessage("The existing SDI with id: " + sdiInterfered.getId() + " has been saved.", frame);
							} catch (CalypsoServiceException e) {
								AppUtil.displayMessage("Something went wrong when saving the existing SDI with id: " + sdiInterfered.getId() + ". Please try again", frame);
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}

	@SuppressWarnings({"rawtypes"})
	private boolean isStaticDataUser(User user) {
		boolean isStaticDataUser = false;

		Vector groupVect = user.getGroups();
		for (int i = 0; i < groupVect.size(); i++) {
			String groupName = (String) groupVect.get(i);
			if (groupName.startsWith("sd")) {
				isStaticDataUser = true;
				break;
			}
		}

		return isStaticDataUser;
	}

	private boolean isTechnicalSDI(SettleDeliveryInstruction sdi) {
		if (sdi.getMethod().equals("Direct") || ("true".equalsIgnoreCase(sdi.getAttribute(SDI_ATTR_IS_TECHNICAL)))) {
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean updateSDIRelationShip(SDIRelationShip sdiRelationShip, SettleDeliveryInstruction sdi, Frame frame,
										 Vector messages) {
		return true;
	}

	public boolean isUserRestricted(User user) {
		try {
			Vector<String> noSDISaveRestrictions = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("sdiSavingUnrestrictedUsers");
			if (noSDISaveRestrictions!=null) {
				return !noSDISaveRestrictions.stream().anyMatch(s -> s.contains(user.getName()));
			}
			return true;
		} catch (CalypsoServiceException e) {
			return true;
		}
	}
}