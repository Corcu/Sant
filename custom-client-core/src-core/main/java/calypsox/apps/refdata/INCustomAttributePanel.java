package calypsox.apps.refdata;

import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTextField;

import calypsox.util.TradeInterfaceUtils;

import com.calypso.apps.refdata.CustomAttributePanel;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

@SuppressWarnings("serial")
public class INCustomAttributePanel extends JPanel implements CustomAttributePanel {
	public INCustomAttributePanel() {
		setLayout(null);
		setSize(200, 100);
		this.inText = new JTextField();
		add(this.inText);
		this.inText.setBounds(20, 20, 700, 34);
	}

	@Override
	public JPanel getPanel() {
		return this;
	}

	@Override
	public boolean doesImplement(String attrName) {
		if (attrName.equals("KEYWORD.BO_REFERENCE")) {
			return true;
		}
		return false;
	}

	@Override
	public String showAttribute(StaticDataFilterElement e) {
		@SuppressWarnings("unchecked")
		Vector<String> values = e.getValues();
		if (!Util.isEmpty(values)) {
			String valStr = Util.collectionToString(values);
			this.inText.setText(valStr);
			return valStr;
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	@Override
	public String buildAttribute(StaticDataFilterElement e) {
		Vector<String> oldValues = new Vector<String>();
		if (!Util.isEmpty(e.getValues())) {
			oldValues.addAll(e.getValues());
		}

		String val = this.inText.getText();
		if (!Util.isEmpty(val)) {
			Vector<String> string2Vector = Util.string2Vector(val);
			e.setValues(string2Vector);
		}

		Vector<String> newValues = new Vector<String>();
		newValues.addAll(e.getValues());
		newValues.removeAll(oldValues);
		oldValues.removeAll(e.getValues());
		Vector<String> deltaValues = new Vector<String>();

		deltaValues.addAll(newValues);
		deltaValues.addAll(oldValues);

		if (!Util.isEmpty(deltaValues)) {
			for (String boRef : deltaValues) {
				TradeArray existingTrades = TradeInterfaceUtils.getTradeByBORef(boRef);
				if ((existingTrades != null) && (existingTrades.size() > 0)) {
					for (int i = 0; i < existingTrades.size(); i++) {
						Trade trade = existingTrades.get(i);
						trade.setAction(Action.AMEND);
						trade.addKeyword("UPDATE_REASON", "SDF Exl-Incl modifs");
						try {
							DSConnection.getDefault().getRemoteTrade().save(trade);
						} catch (RemoteException ex) {
							Log.error(this, ex);
						}
					}
				}
			}
		}
		return val;
	}

	protected JTextField inText = null; //sonar
}
