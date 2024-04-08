package calypsox.apps.reporting;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.calypso.apps.reporting.LatestGenericCommentEditor;
import com.calypso.apps.reporting.MessageReportWindowHandler;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.ReportTemplateTradeBundleRetrieverPanel;
import com.calypso.apps.reporting.ReportTemplateTradeRetrieverPanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.ui.component.dialog.DualListDialog;

public class MessageSwiftReportTemplatePanel extends ReportTemplatePanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	CalypsoCheckBox internalOnlyCheck = new CalypsoCheckBox();
	CalypsoCheckBox externalOnlyCheck = new CalypsoCheckBox();
	JLabel groupingTypeLabel = new JLabel();
	JLabel descriptionLabel = new JLabel();
	JLabel tradeIdLabel = new JLabel();
	JLabel transferIdLabel = new JLabel();
	JLabel swiftContractIdLabel = new JLabel();
	JLabel messageIdLabel = new JLabel();
	JLabel filterSetLabel = new JLabel();
	JLabel methodLabel = new JLabel();
	JLabel templateLabel = new JLabel();
	JLabel contactIdLabel = new JLabel();
	JLabel productFamilyLabel = new JLabel();
	JLabel productTypeLabel = new JLabel();
	JLabel statusLabel = new JLabel();
	JLabel messageTypeLabel = new JLabel();
	JTextField templateText = new JTextField();
	JTextField transferIdText = new JTextField();
	JTextField swiftContractIdText = new JTextField();
	JTextField messageIdText = new JTextField();
	JTextField descriptionText = new JTextField();
	JTextField filterSetText = new JTextField();
	JTextField methodText = new JTextField();
	JTextField contactIdText = new JTextField();
	JTextField productFamilyText = new JTextField();
	JTextField productTypeText = new JTextField();
	JTextField groupingTypeText = new JTextField();
	JTextField statusText = new JTextField();
	JTextField messageTypeText = new JTextField();
	JButton templateButton = new JButton();
	JButton setFilterSetButton = new JButton();
	JButton methodButton = new JButton();
	JButton productFamilyButton = new JButton();
	JButton productTypeButton = new JButton();
	JButton statusButton = new JButton();
	JButton setMessageTypeButton = new JButton();
	JButton setGroupingTypeButton = new JButton();
	JButton msgAttributeButton = new JButton();
	protected CalypsoComboBox dateTypeChoice = new CalypsoComboBox();
	JTextField msgLinkedIdText = new JTextField();
	protected Vector _selectedProducts;
	protected static final String CREATION_DATE = "CreationDate";
	protected static final String MESSAGE_DATE = "MessageDate";
	protected static final String RESET_DATE = "ResetDate";
	LatestGenericCommentEditor commentEditor = null;
	protected ReportTemplate _template;
	ReportTemplateDatePanel _startDate = null;
	ReportTemplateDatePanel _endDate = null;
	LegalEntityTextPanel _receiverPanel = new LegalEntityTextPanel();
	LegalEntityTextPanel _messageLEPanel = new LegalEntityTextPanel();
	Vector _typesList = null;
	JLabel jLabel1 = new JLabel();
	JTextField actionText = new JTextField();
	JButton actionButton = new JButton();
	JLabel jLabel2 = new JLabel();
	JComboBox processingOrgChoice = new JComboBox();
	JLabel statementIdLabel = new JLabel();
	JTextField statementIdText = new JTextField();
	ReportTemplateTradeRetrieverPanel _tradeRetrieverPanel = new ReportTemplateTradeRetrieverPanel();
	ReportTemplateTradeBundleRetrieverPanel _tradeBundleRetrieverPanel = new ReportTemplateTradeBundleRetrieverPanel();

	private void jbInit() throws Exception {
		this.setLayout((LayoutManager) null);
		this.setSize(new Dimension(833, 250));
		this.descriptionLabel.setHorizontalAlignment(2);
		this.descriptionLabel.setText("Template Description");
		this.jLabel1.setHorizontalAlignment(4);
		this.jLabel1.setText("Action");
		this.jLabel1.setBounds(new Rectangle(595, 199, 90, 24));
		this.actionText.setText("");
		this.actionText.setBounds(new Rectangle(690, 198, 151, 27));
		this.actionButton.setBounds(new Rectangle(841, 200, 33, 25));
		this.actionButton.setText("...");
		this.jLabel2.setHorizontalAlignment(11);
		this.jLabel2.setText("Processing Org");
		this.jLabel2.setBounds(new Rectangle(352, 143, 98, 25));
		this.processingOrgChoice.setBounds(new Rectangle(453, 143, 154, 24));
		this.statementIdLabel.setBounds(new Rectangle(16, 143, 80, 24));
		this.statementIdLabel.setText("Statement Id");
		this.statementIdLabel.setHorizontalAlignment(4);
		this.statementIdText.setBounds(new Rectangle(100, 143, 90, 24));
		this.add(this.descriptionLabel);
		this.descriptionLabel.setBounds(16, 8, 128, 24);
		this.add(this.descriptionText);
		this.descriptionText.setBounds(144, 8, 350, 24);
		this.internalOnlyCheck.setText("Internal");
		this.internalOnlyCheck.setActionCommand("Internal");
		this.internalOnlyCheck.setBounds(new Rectangle(699, 8, 75, 24));
		this.externalOnlyCheck.setText("External");
		this.externalOnlyCheck.setActionCommand("External");
		this.externalOnlyCheck.setBounds(new Rectangle(775, 8, 69, 24));
		this.add(this.dateTypeChoice);
		this.dateTypeChoice.setBounds(305, 35, 108, 24);
		this._startDate = ReportTemplateDatePanel.getStart(true);
		this.add(this._startDate);
		this._startDate.setBounds(16, 35, 289, 24);
		this._endDate = ReportTemplateDatePanel.getEnd(true);
		this.add(this._endDate);
		this._endDate.setBounds(16, 62, 289, 24);
		this._endDate.setDependency(this._startDate);
		this.tradeIdLabel.setHorizontalAlignment(4);
		this.tradeIdLabel.setText("Trade Id");
		this.add(this.tradeIdLabel);
		this.tradeIdLabel.setBounds(16, 89, 80, 24);
		this.add(this._tradeRetrieverPanel);
		this._tradeRetrieverPanel.setBounds(100, 89, 170, 24);
		this.transferIdLabel.setHorizontalAlignment(4);
		this.transferIdLabel.setText("Transfer Id");
		this.add(this.transferIdLabel);
		this.transferIdLabel.setBounds(16, 116, 80, 24);
		this.add(this.transferIdText);
		this.transferIdText.setBounds(100, 116, 90, 24);
		this.swiftContractIdLabel.setHorizontalAlignment(4);
		this.swiftContractIdLabel.setText("Swift Contract Id");
		this.add(this.swiftContractIdLabel);
		this.swiftContractIdLabel.setBounds(new Rectangle(16, 250, 100, 24));
		this.add(this.swiftContractIdText);
		this.swiftContractIdText.setBounds(new Rectangle(140, 250, 90, 24));
		this._tradeBundleRetrieverPanel.setBounds(60, 223, 370, 24);
		this.add(this._tradeBundleRetrieverPanel);
		this.templateLabel.setHorizontalAlignment(4);
		this.templateLabel.setText("Template");
		this.templateLabel.setBounds(new Rectangle(16, 196, 80, 24));
		this.templateText.setBounds(new Rectangle(100, 196, 90, 24));
		this.templateButton.setText("...");
		this.templateButton.setActionCommand("...");
		this.templateButton.setBounds(new Rectangle(195, 196, 32, 24));
		this.messageIdLabel.setHorizontalAlignment(4);
		this.messageIdLabel.setText("Message Id");
		this.add(this.messageIdLabel);
		this.messageIdLabel.setBounds(new Rectangle(16, 169, 80, 24));
		this.add(this.messageIdText);
		this.messageIdText.setBounds(new Rectangle(100, 169, 90, 24));
		this.messageTypeLabel.setHorizontalAlignment(4);
		this.messageTypeLabel.setText("Type");
		this.messageTypeLabel.setBounds(new Rectangle(368, 35, 80, 24));
		this.messageTypeText.setBounds(new Rectangle(453, 35, 113, 24));
		this.setMessageTypeButton.setText("...");
		this.setMessageTypeButton.setActionCommand("...");
		this.setMessageTypeButton.setBounds(new Rectangle(571, 35, 32, 24));
		this._receiverPanel = new LegalEntityTextPanel();
		this._receiverPanel.setBounds(new Rectangle(362, 62, 242, 24));
		this._receiverPanel.setRole("CounterParty", "Receiver", true);
		this._receiverPanel.allowMultiple(true);
		this.methodLabel.setHorizontalAlignment(4);
		this.methodLabel.setText("Method");
		this.methodLabel.setBounds(new Rectangle(368, 89, 80, 24));
		this.methodText.setBounds(new Rectangle(453, 89, 113, 24));
		this.methodButton.setText("...");
		this.methodButton.setActionCommand("...");
		this.methodButton.setBounds(new Rectangle(571, 89, 32, 24));
		this.contactIdLabel.setHorizontalAlignment(4);
		this.contactIdLabel.setText("Contact Id");
		this.contactIdLabel.setBounds(new Rectangle(368, 116, 80, 24));
		this.contactIdText.setBounds(new Rectangle(453, 116, 113, 24));
		this.filterSetLabel.setHorizontalAlignment(4);
		this.filterSetLabel.setText("Filter Set");
		this.filterSetLabel.setBounds(new Rectangle(595, 62, 90, 24));
		this.filterSetText.setBounds(new Rectangle(690, 62, 150, 24));
		this.setFilterSetButton.setText("...");
		this.setFilterSetButton.setActionCommand("...");
		this.setFilterSetButton.setBounds(new Rectangle(841, 62, 32, 24));
		this.productFamilyLabel.setHorizontalAlignment(4);
		this.productFamilyLabel.setText("Product Family");
		this.productFamilyLabel.setBounds(new Rectangle(595, 89, 90, 24));
		this.productTypeLabel.setHorizontalAlignment(4);
		this.productTypeLabel.setText("Product Type");
		this.productTypeLabel
				.setToolTipText("Dbl-Click to choose Products Ids");
		this.productTypeLabel.setBounds(new Rectangle(595, 116, 90, 24));
		this.productFamilyText.setBounds(new Rectangle(690, 89, 150, 24));
		this.productTypeText.setBounds(new Rectangle(690, 116, 150, 24));
		this.productFamilyButton.setText("...");
		this.productFamilyButton.setActionCommand("jbutton");
		this.productFamilyButton.setBounds(new Rectangle(841, 89, 32, 24));
		this.productTypeButton.setText("...");
		this.productTypeButton.setActionCommand("...");
		this.productTypeButton.setBounds(new Rectangle(841, 116, 32, 24));
		this.statusLabel.setHorizontalAlignment(4);
		this.statusLabel.setText("Status");
		this.statusLabel.setBounds(new Rectangle(595, 143, 90, 24));
		this.statusText.setBounds(new Rectangle(690, 143, 150, 24));
		this.statusButton.setText("...");
		this.statusButton.setActionCommand("...");
		this.statusButton.setBounds(new Rectangle(841, 143, 32, 24));
		this.groupingTypeLabel.setHorizontalAlignment(4);
		this.groupingTypeLabel.setText("Grouping");
		this.groupingTypeLabel.setBounds(new Rectangle(595, 170, 90, 24));
		this.groupingTypeText.setBounds(new Rectangle(690, 170, 150, 24));
		this.setGroupingTypeButton.setText("...");
		this.setGroupingTypeButton.setActionCommand("...");
		this.setGroupingTypeButton.setBounds(new Rectangle(841, 170, 32, 24));
		this._messageLEPanel = new LegalEntityTextPanel();
		this._messageLEPanel.setBounds(new Rectangle(362, 169, 242, 24));
		this._messageLEPanel.setRole("ALL", "Message LE", false, true);
		this._messageLEPanel.allowMultiple(true);
		this._messageLEPanel.setEditable(true);
		JLabel msgLinkedIdLabel = new JLabel();
		msgLinkedIdLabel.setText("Msg Linked Id");
		msgLinkedIdLabel.setHorizontalAlignment(4);
		msgLinkedIdLabel.setBounds(368, 196, 80, 24);
		this.msgLinkedIdText.setBounds(453, 196, 90, 24);
		this.msgAttributeButton.setText("Attributes");
		this.add(this.messageTypeLabel);
		this.add(this.messageTypeText);
		this.add(this.setMessageTypeButton);
		this.add(this.methodLabel);
		this.add(this.methodText);
		this.add(this.methodButton);
		this.add(this.contactIdLabel);
		this.add(this.contactIdText);
		this.add(this.processingOrgChoice, (Object) null);
		this.add(this.jLabel2, (Object) null);
		this.add(this.messageIdText);
		this.add(this.messageIdLabel);
		this.add(this.swiftContractIdText);
		this.add(this.swiftContractIdLabel);
		this.add(this.templateLabel);
		this.add(this.templateText);
		this.add(this.templateButton);
		this.add(this.statementIdLabel, (Object) null);
		this.add(this.statementIdText, (Object) null);
		this.add(this.productTypeText);
		this.add(this.internalOnlyCheck);
		this.add(this.externalOnlyCheck);
		this.add(this.filterSetLabel);
		this.add(this.filterSetText);
		this.add(this.setFilterSetButton);
		this.add(this.productFamilyLabel);
		this.add(this.productTypeLabel);
		this.add(this.productFamilyText);
		this.add(this.productFamilyButton);
		this.add(this.productTypeButton);
		this.add(this.statusLabel);
		this.add(this.statusText);
		this.add(this.statusButton);
		this.add(this.groupingTypeLabel);
		this.add(this.groupingTypeText);
		this.add(this.setGroupingTypeButton);
		this.add(this.msgAttributeButton);
		this.add(this.jLabel1, (Object) null);
		this.add(this.actionText, (Object) null);
		this.add(this.actionButton, (Object) null);
		this.add(this._receiverPanel);
		this.add(this._messageLEPanel);
		this.add(msgLinkedIdLabel);
		this.add(this.msgLinkedIdText);
		this.msgAttributeButton.setBounds(new Rectangle(693, 35, 144, 24));
		MessageSwiftReportTemplatePanel.SymAction lSymAction = new MessageSwiftReportTemplatePanel.SymAction();
		this.statusButton.addActionListener(lSymAction);
		this.actionButton.addActionListener(lSymAction);
		this.methodButton.addActionListener(lSymAction);
		this.templateButton.addActionListener(lSymAction);
		this.productFamilyButton.addActionListener(lSymAction);
		this.productTypeButton.addActionListener(lSymAction);
		this.setFilterSetButton.addActionListener(lSymAction);
		this.setGroupingTypeButton.addActionListener(lSymAction);
		this.setMessageTypeButton.addActionListener(lSymAction);
		this.dateTypeChoice.addActionListener(lSymAction);
		MessageSwiftReportTemplatePanel.SymMouse aSymMouse = new MessageSwiftReportTemplatePanel.SymMouse();
		this.productTypeLabel.addMouseListener(aSymMouse);
		this.msgAttributeButton.addActionListener(lSymAction);
	}

	public MessageSwiftReportTemplatePanel() {
		try {
			this.jbInit();
		} catch (Exception arg1) {
			Log.error(this, arg1);
		}

		this.initDomains();
	}

	protected void initDomains() {
		Vector v = null;
		this._startDate.init("StartDate", "StartPlus", "StartTenor",
				"startTimeDate");
		this._endDate.init("EndDate", "EndPlus", "EndTenor", "endTimeDate");
		AppUtil.addStartEndDatesActionListener(this._startDate, this._endDate);
		this._tradeRetrieverPanel.init("TradeId", "INTERNAL__REF",
				"ExternalRef");
		v = new Vector();
		v.insertElementAt("CreationDate", 0);
		v.insertElementAt("MessageDate", 1);
		v.insertElementAt("ResetDate", 2);
		AppUtil.set(this.dateTypeChoice, v);
		Vector statuses = LocalCache.cloneDomainValues(
				DSConnection.getDefault(), "messageStatus");
		statuses.removeElement("CANCELED");
		this.statusText.setText(Util.vector2String(statuses));
		Vector actions = LocalCache.cloneDomainValues(
				DSConnection.getDefault(), "messageAction");
		this.actionText.setText(Util.vector2String(actions));
		Vector pos = AccessUtil.getAccessiblePONames(false, true);
		AppUtil.set(this.processingOrgChoice, pos);
		this.internalOnlyCheck.setSelected(true);
		this.externalOnlyCheck.setSelected(true);
	}

	void dateTypeChoice_actionPerformed() {
		try {
			if (this.dateTypeChoice.getSelectedItem() == "CreationDate") {
				this._startDate.setTimeEnable(true);
				this._endDate.setTimeEnable(true);
			} else {
				this._startDate.setTimeEnable(false);
				this._startDate.setTimeText("");
				this._endDate.setTimeEnable(false);
				this._endDate.setTimeText("");
			}
		} catch (Exception arg1) {
			Log.error(this, arg1);
		}

	}

	void msgAttributeButton_actionPerformed(ActionEvent event) {
		this.showAttributableWindow("MsgAttributes", this._template);
	}

	void statusButton_ActionPerformed(ActionEvent event) {
		try {
			Vector statuses = LocalCache.getDomainValues(
					DSConnection.getDefault(), "messageStatus");
			JTextField arg3 = this.statusText;
			Vector myVector = Util.string2Vector(getText_aroundBody1$advice(
					this, arg3, arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					statuses, myVector, "Select Message Status", true, true);
			if (myVector != null) {
				this.statusText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void actionButton_ActionPerformed(ActionEvent event) {
		try {
			Vector e = LocalCache.getDomainValues(DSConnection.getDefault(),
					"messageAction");
			JTextField arg3 = this.actionText;
			Vector myVector = Util.string2Vector(getText_aroundBody3$advice(
					this, arg3,  arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					e, myVector, "Select Message Action", true, true);
			if (myVector != null) {
				this.actionText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void methodButton_ActionPerformed(ActionEvent event) {
		try {
			Vector e = LEContact.getAdressMethodsFromDomain();
			JTextField arg3 = this.methodText;
			Vector myVector = Util.string2Vector(getText_aroundBody5$advice(
					this, arg3, arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					e, myVector);
			if (myVector != null) {
				this.methodText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void templateButton_ActionPerformed(ActionEvent event) {
		try {
			Vector templates = new Vector();
			Vector templateNames = LocalCache.getDomainValues(
					DSConnection.getDefault(), "SWIFT.Templates");
			if (templateNames != null) {
				templates.addAll(templateNames);
			}

			templateNames = LocalCache.getDomainValues(
					DSConnection.getDefault(), "MESSAGE.Templates");
			if (templateNames != null) {
				templates.addAll(templateNames);
			}

			Vector formatTypes = LocalCache.getDomainValues(
					DSConnection.getDefault(), "formatType");
			Iterator i$ = formatTypes.iterator();

			while (true) {
				do {
					Object formatType;
					do {
						do {
							if (!i$.hasNext()) {
								JTextField arg8 = this.templateText;
								Vector i$2 = Util
										.string2Vector(getText_aroundBody7$advice(
												this, arg8,

												arg8));
								i$2 = (Vector) DualListDialog.chooseList(
										new Vector(), this, templates, i$2,
										"Select Template", true, true);
								if (i$2 != null) {
									this.templateText.setText(Util
											.vector2String(i$2));
								}

								return;
							}

							formatType = i$.next();
						} while ("SWIFT".equals(formatType));
					} while ("HTML".equals(formatType));

					templateNames = LocalCache.getDomainValues(
							DSConnection.getDefault(), formatType
									+ ".Templates");
				} while (templateNames == null);

				Iterator i$1 = templateNames.iterator();

				while (i$1.hasNext()) {
					String templateS = (String) i$1.next();
					if (!templates.contains(templateS)) {
						templates.add(templateS);
					}
				}
			}
		} catch (Exception arg9) {
			Log.error(this, arg9);
		}
	}

	void setFilterSetButton_actionPerformed(ActionEvent event) {
		Vector v = null;
		String name = null;

		try {
			v = AccessUtil.getAllNames(4);
			if (v == null || v.size() <= 0) {
				AppUtil.displayWarning("No filter set in DB", this);
				return;
			}

			name = AppUtil.chooseValue("Select", "FilterSet Name", v,
					(String) null, false, this);
			if (name == null) {
				return;
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

		this.filterSetText.setText(name);
	}

	void setMessageTypeButton_actionPerformed(ActionEvent event) {
		try {
			Vector e = LocalCache.getDomainValues(DSConnection.getDefault(),
					"messageType");
			JTextField arg3 = this.messageTypeText;
			Vector myVector = Util.string2Vector(getText_aroundBody9$advice(
					this, arg3,  arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					e, myVector, "Select Message Type", true, true);
			if (myVector != null) {
				this.messageTypeText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void setGroupingTypeButton_actionPerformed(ActionEvent event) {
		try {
			Vector e = LocalCache.getDomainValues(DSConnection.getDefault(),
					"messageGrouping");
			JTextField arg3 = this.groupingTypeText;
			Vector myVector = Util.string2Vector(getText_aroundBody11$advice(
					this, arg3,  arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					e, myVector, "Select Message Group", true, true);
			if (myVector != null) {
				this.groupingTypeText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void productFamilyButton_ActionPerformed(ActionEvent event) {
		try {
			Vector e = LocalCache.getDomainValues(DSConnection.getDefault(),
					"productFamily");
			JTextField arg3 = this.productFamilyText;
			Vector myVector = Util.string2Vector(getText_aroundBody13$advice(
					this, arg3,  arg3));
			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					e, myVector, "Select Product Family", true, true);
			if (myVector != null) {
				this.productFamilyText.setText(Util.vector2String(myVector));
			}
		} catch (Exception arg4) {
			Log.error(this, arg4);
		}

	}

	void productTypeButton_ActionPerformed(ActionEvent event) {
		try {
			Vector productGroups = LocalCache.cloneDomainValues(
					DSConnection.getDefault(), "productGroup");
			Vector products = LocalCache.cloneDomainValues(
					DSConnection.getDefault(), "productType");
			if (productGroups != null) {
				products.addAll(0, productGroups);
			}

			Vector myVector = null;
			if (this._selectedProducts == null) {
				JTextField arg5 = this.productTypeText;
				myVector = Util.string2Vector(getText_aroundBody15$advice(this,
						arg5,  arg5));
			}

			myVector = (Vector) DualListDialog.chooseList(new Vector(), this,
					products, myVector, "Select Product Type", true, true);
			if (myVector != null) {
				this.productTypeText.setText(Util.vector2String(myVector));
				this.productTypeLabel.setText("Product Type");
				this._selectedProducts = null;
				this.productTypeLabel
						.setToolTipText("Dbl-Click to choose Products Ids");
			}
		} catch (Exception arg6) {
			Log.error(this, arg6);
		}

	}

	public ReportTemplate getTemplate() {
		boolean external = this.externalOnlyCheck.isSelected();
		boolean internal = this.internalOnlyCheck.isSelected();
		String from = "BOTH";
		if (external && !internal) {
			from = "EXTERNAL";
		} else if (internal && !external) {
			from = "INTERNAL";
		}

		this._startDate.read(this._template);
		this._endDate.read(this._template);
		this._tradeRetrieverPanel.read(this._template);
		JTextField arg4 = this.transferIdText;
		this._template.put(
				"PaymentId",
				getText_aroundBody17$advice(this, arg4, arg4));
		JTextField arg5 = this.statementIdText;
		this._template.put(
				"StatementId",
				getText_aroundBody19$advice(this, arg5, arg5));
		JTextField arg6 = this.contactIdText;
		this._template.put(
				"ContactId",
				getText_aroundBody21$advice(this, arg6, arg6));
		JTextField arg7 = this.messageIdText;
		this._template.put(
				"MessageId",
				getText_aroundBody23$advice(this, arg7, arg7));
		this._template.put("CptyName", this._receiverPanel.getLEIdsStr());
		JTextField arg8 = this.statusText;
		this._template.put(
				"Status",
				getText_aroundBody25$advice(this, arg8, arg8));
		JTextField arg9 = this.actionText;
		this._template.put(
				"Action",
				getText_aroundBody27$advice(this, arg9,
						arg9));
		JTextField arg10 = this.messageTypeText;
		this._template.put(
				"TransferType",
				getText_aroundBody29$advice(this, arg10,
						 arg10));
		JTextField arg11 = this.groupingTypeText;
		this._template.put(
				"GroupingType",
				getText_aroundBody31$advice(this, arg11,
						 arg11));
		JTextField arg12 = this.productTypeText;
		this._template.put(
				"ProductType",
				getText_aroundBody33$advice(this, arg12,
						 arg12));
		JTextField arg13 = this.productFamilyText;
		this._template.put(
				"ProductFamily",
				getText_aroundBody35$advice(this, arg13,
						 arg13));
		JTextField arg14 = this.methodText;
		this._template.put(
				"SettleMethod",
				getText_aroundBody37$advice(this, arg14,
						arg14));
		this._template.put("DateType", this.dateTypeChoice.getSelectedItem());
		JTextField arg15 = this.filterSetText;
		this._template.put(
				"FilterSet",
				getText_aroundBody39$advice(this, arg15,
						 arg15));
		JTextField arg16 = this.descriptionText;
		this._template.put(
				"Description",
				getText_aroundBody41$advice(this, arg16,
						arg16));
		this._template.put("Message From", from);
		JTextField arg17 = this.templateText;
		this._template.put(
				"Template Name",
				getText_aroundBody43$advice(this, arg17,
						arg17));
		this._template.put("MessageLegalEntity",
				this._messageLEPanel.getLEIdsStr());
		JTextField arg18 = this.msgLinkedIdText;
		this._template.put(
				"LinkedId",
				getText_aroundBody45$advice(this, arg18,
					arg18));
		JTextField arg21 = this.swiftContractIdText;
		this._template.put(
				"SwiftContractId",
				getText_aroundBody47$advice(this, arg21,
					 arg21));
		
		if (!this.processingOrgChoice.getSelectedItem().equals("ALL")) {
			String poName = (String) this.processingOrgChoice.getSelectedItem();
			this._template.put("POName", poName);
		} else {
			this._template.remove("POName");
		}

		this._template.remove("Product Type");
		this._template.remove("Family");
		this._tradeBundleRetrieverPanel.read(this._template, "BundleType",
				"BundleId", "BundleName");
		return this._template;
	}

	private static final String getText_aroundBody1$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody3$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody5$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody7$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody9$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody11$advice(
			MessageSwiftReportTemplatePanel messageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody13$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody15$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody17$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody19$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody21$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody23$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody25$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody27$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody29$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody31$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody33$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody35$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody37$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody39$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody41$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody43$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody45$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}

	private static final String getText_aroundBody47$advice(
			MessageSwiftReportTemplatePanel MessageSwiftReportTemplatePanel,
			JTextField component,
			JTextComponent jTextComponent) {
		JTextComponent jTextComponent2 = jTextComponent;
		return ((JTextField) jTextComponent2).getText();
	}
	
	public void setTemplate(ReportTemplate template) {
		this._template = template;
		this._startDate.setTemplate(this._template);
		this._endDate.setTemplate(this._template);
		this._tradeRetrieverPanel.write(this._template);
		this.transferIdText.setText("");
		String s = (String) this._template.get("PaymentId");
		if (s != null) {
			this.transferIdText.setText(s);
		}

		this.statementIdText.setText("");
		s = (String) this._template.get("StatementId");
		if (s != null) {
			this.statementIdText.setText(s);
		}

		this.contactIdText.setText("");
		s = (String) this._template.get("ContactId");
		if (s != null) {
			this.contactIdText.setText(s);
		}

		this.messageIdText.setText("");
		s = (String) this._template.get("MessageId");
		if (s != null) {
			this.messageIdText.setText(s);
		}

		this.swiftContractIdText.setText("");
		s = (String) this._template.get("SwiftContractcId");
		if (s != null) {
			this.swiftContractIdText.setText(s);
		}
		
		this._receiverPanel.setLE("");
		s = (String) this._template.get("CptyName");
		if (s != null) {
			boolean oldWay = false;

			try {
				LegalEntity e = BOCache.getLegalEntity(
						DSConnection.getDefault(), s);
				if (e != null) {
					oldWay = true;
					this._receiverPanel.setLE(e.getCode());
				}
			} catch (Exception arg4) {
				Log.error(this, arg4);
			}

			if (!oldWay) {
				this._receiverPanel.setLEIdsStr(s);
			}
		}

		this.statusText.setText("");
		s = (String) this._template.get("Status");
		if (s != null) {
			this.statusText.setText(s);
		}

		this.actionText.setText("");
		s = (String) this._template.get("Action");
		if (s != null) {
			this.actionText.setText(s);
		}

		this.templateText.setText("");
		s = (String) this._template.get("Template Name");
		if (s != null) {
			this.templateText.setText(s);
		}

		this.messageTypeText.setText("");
		s = (String) this._template.get("TransferType");
		if (s != null) {
			this.messageTypeText.setText(s);
		}

		this.groupingTypeText.setText("");
		s = (String) this._template.get("GroupingType");
		if (s != null) {
			this.groupingTypeText.setText(s);
		}

		this.methodText.setText("");
		s = (String) this._template.get("SettleMethod");
		if (s != null) {
			this.methodText.setText(s);
		}

		this.productTypeText.setText("");
		s = (String) this._template.get("ProductType");
		if (s != null) {
			this.productTypeText.setText(s);
		}

		this.productFamilyText.setText("");
		s = (String) this._template.get("ProductFamily");
		if (s != null) {
			this.productFamilyText.setText(s);
		}

		this.dateTypeChoice.setSelectedIndex(0);
		s = (String) this._template.get("DateType");
		if (s != null && s.indexOf("ResetDate") >= 0) {
			this.dateTypeChoice.setSelectedIndex(2);
		}

		if (s != null && s.indexOf("MessageDate") >= 0) {
			this.dateTypeChoice.setSelectedIndex(1);
		}

		if (s != null && s.indexOf("CreationDate") >= 0) {
			this.dateTypeChoice.setSelectedIndex(0);
		}

		this.filterSetText.setText("");
		s = (String) this._template.get("FilterSet");
		if (s != null) {
			this.filterSetText.setText(s);
		}

		this._startDate.write(this._template);
		this._endDate.write(this._template);
		this.externalOnlyCheck.setSelected(true);
		this.internalOnlyCheck.setSelected(true);
		s = (String) this._template.get("Message From");
		if (s != null) {
			if (s.equals("INTERNAL")) {
				this.externalOnlyCheck.setSelected(false);
			} else if (s.equals("EXTERNAL")) {
				this.internalOnlyCheck.setSelected(false);
			}
		}

		s = (String) this._template.get("Description");
		if (s != null) {
			this.descriptionText.setText(s);
		} else {
			this.descriptionText.setText("");
		}

		s = (String) this._template.get("POName");
		if (s != null) {
			this.processingOrgChoice.setSelectedItem(s);
		} else {
			this.processingOrgChoice.setSelectedItem("ALL");
		}

		this._messageLEPanel.setLE("");
		s = (String) this._template.get("MessageLegalEntity");
		if (s != null) {
			this._messageLEPanel.setLEIdsStr(s);
		}

		this.msgLinkedIdText.setText("");
		s = (String) this._template.get("LinkedId");
		if (s != null) {
			this.msgLinkedIdText.setText(s);
		}

		this._tradeBundleRetrieverPanel.write(this._template, "BundleType",
				"BundleId", "BundleName");
	}

	protected void productTypeLabel_mouseClicked(MouseEvent event) {
	}

	protected Vector getTypesList() {
		if (this._typesList != null) {
			return this._typesList;
		} else {
			Vector list = new Vector();
			Vector v = LocalCache.getDomainValues(DSConnection.getDefault(),
					"productType");

			for (int i = 0; i < v.size(); ++i) {
				String s = (String) v.elementAt(i);
				Product p = getInstance(s);
				if (p != null && p.hasSecondaryMarket()) {
					list.addElement(p.getType());
				}
			}

			this._typesList = list;
			return this._typesList;
		}
	}

	protected static Product getInstance(String type) {
		String className = "tk.product." + type;

		try {
			Product e = (Product) InstantiateUtil.getInstance(className);
			return e;
		} catch (Exception arg2) {
			return null;
		}
	}

	public void setValDatetime(JDatetime valDatetime) {
		this._startDate.setValDatetime(valDatetime);
		this._endDate.setValDatetime(valDatetime);
	}

	public void callBeforeLoad(ReportPanel panel) {
		ReportTemplate template = panel.getTemplate();
		if (template != null) {
			boolean externalMsg = DefaultReportOutput.needsReportStyle(
					template, ReportStyle.getReportStyle("ExternalMessage"));
			Boolean loadExternal = (Boolean) template
					.get("GenerateExternalMsg");
			if (externalMsg
					&& (loadExternal == null || !loadExternal.booleanValue())) {
				String question = "You requested fields from the External Message.";
				question = question
						+ "\nDo you want to generate the External Messages which have not been created yet ?";
				boolean answer = AppUtil.displayQuestion(question, this);
				if (answer) {
					template.put("GenerateExternalMsg", Boolean.TRUE);
				} else {
					template.put("GenerateExternalMsg", Boolean.FALSE);
				}
			}

			boolean question1 = false;
			ReportStyle answer1 = ReportStyle
					.getReportStyle("LatestGenericComment");
			if (answer1 != null
					&& DefaultReportOutput.needsReportStyle(
							panel.getTemplate(), "Message.Comment.", answer1,
							new String[0])) {
				question1 = true;
			}

			MessageReportWindowHandler handler;
			if (!question1) {
				if (this.commentEditor != null) {
					panel.removeReportPanelListener(this.commentEditor);
				}

				if (panel.getReportWindowHandler() instanceof MessageReportWindowHandler) {
					handler = (MessageReportWindowHandler) panel
							.getReportWindowHandler();
					handler.setCanEditGenericComment(false);
				}
			} else {
				if (this.commentEditor == null) {
					this.commentEditor = new LatestGenericCommentEditor(
							"Message.Comment.");
				}

				panel.addReportPanelListener(this.commentEditor);
				if (panel.getReportWindowHandler() instanceof MessageReportWindowHandler) {
					handler = (MessageReportWindowHandler) panel
							.getReportWindowHandler();
					handler.setCanEditGenericComment(true);
				}
			}

		}
	}

	public boolean isValidLoad(ReportPanel panel) {
		ReportStyle genericCommentStyle = ReportStyle
				.getReportStyle("LatestGenericComment");
		if (genericCommentStyle != null
				&& DefaultReportOutput.needsReportStyle(panel.getTemplate(),
						"Message.Comment.", genericCommentStyle, new String[0])
				&& panel.isRealTime()) {
			String error1 = "Can not display Comment columns in Real Time mode.";
			AppUtil.displayError(error1, this);
			return false;
		} else {
			Map error = panel.getReport().getPotentialSize();
			return this.displayLargeListWarningMessage(this, error);
		}
	}

	class SymMouse extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			Object object = event.getSource();
			if (object == MessageSwiftReportTemplatePanel.this.productTypeLabel) {
				MessageSwiftReportTemplatePanel.this
						.productTypeLabel_mouseClicked(event);
			}

		}
	}

	class SymAction implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				MessageSwiftReportTemplatePanel.this.setCursor(new Cursor(3));
				Object object = event.getSource();
				if (object == MessageSwiftReportTemplatePanel.this.statusButton) {
					MessageSwiftReportTemplatePanel.this
							.statusButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.actionButton) {
					MessageSwiftReportTemplatePanel.this
							.actionButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.methodButton) {
					MessageSwiftReportTemplatePanel.this
							.methodButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.templateButton) {
					MessageSwiftReportTemplatePanel.this
							.templateButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.productFamilyButton) {
					MessageSwiftReportTemplatePanel.this
							.productFamilyButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.productTypeButton) {
					MessageSwiftReportTemplatePanel.this
							.productTypeButton_ActionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.setFilterSetButton) {
					MessageSwiftReportTemplatePanel.this
							.setFilterSetButton_actionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.setMessageTypeButton) {
					MessageSwiftReportTemplatePanel.this
							.setMessageTypeButton_actionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.setGroupingTypeButton) {
					MessageSwiftReportTemplatePanel.this
							.setGroupingTypeButton_actionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.msgAttributeButton) {
					MessageSwiftReportTemplatePanel.this
							.msgAttributeButton_actionPerformed(event);
				} else if (object == MessageSwiftReportTemplatePanel.this.dateTypeChoice) {
					MessageSwiftReportTemplatePanel.this
							.dateTypeChoice_actionPerformed();
				}
			} finally {
				MessageSwiftReportTemplatePanel.this.setCursor(new Cursor(0));
			}

		}
	}
}
