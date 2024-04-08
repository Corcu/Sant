package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.PDVUtil.EnumMessageType;

import com.calypso.apps.reporting.ExceptionSummaryPanel;
import com.calypso.apps.reporting.TaskGetInfo;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class EX_PDV_ALLOCExceptionSummaryPanel extends JPanel implements
		ExceptionSummaryPanel, PDVConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8454889377989106255L;

	protected JPanel allocDetailPanel = null;

	protected JPanel contractInfoPanel = new JPanel();
	protected JPanel allocInfoPanel = new JPanel();

	// CONTRACT NAME
	protected JLabel contractNameLabel = new JLabel();
	protected JLabel contractNameValueLabel = new JLabel();

	// CONTRACT ID
	protected JLabel contractIdLabel = new JLabel();
	protected JLabel contractIdValueLabel = new JLabel();

	// COLLAT_VALUE_DATE
	protected JLabel valueDateLabel = new JLabel();
	protected JLabel valueDateValueLabel = new JLabel();
	// COLLAT_ PORTFOLIO
	protected JLabel portFolioLabel = new JLabel();
	protected JLabel portFolioValueLabel = new JLabel();
	// COLLAT_UNDERLYNG_TYPE
	protected JLabel underlyingTypeLabel = new JLabel();
	protected JLabel underlyingTypeValueLabel = new JLabel();
	// COLLAT_ UNDERLYING
	protected JLabel underlyingLabel = new JLabel();
	protected JLabel underlyingValueLabel = new JLabel();
	// COLLAT_CURRENCY
	protected JLabel currencyLabel = new JLabel();
	protected JLabel currencyValueLabel = new JLabel();
	// COLLAT_AMOUNT
	protected JLabel amountLabel = new JLabel();
	protected JLabel amountValueLabel = new JLabel();

	@Override
	public void clean() {
		if (contractNameValueLabel != null) {
			contractNameValueLabel.setText("");
		}
		if (contractIdValueLabel != null) {
			contractIdValueLabel.setText("");
		}
		if (valueDateValueLabel != null) {
			valueDateValueLabel.setText("");
		}
		if (portFolioValueLabel != null) {
			portFolioValueLabel.setText("");
		}
		if (underlyingTypeValueLabel != null) {
			underlyingTypeValueLabel.setText("");
		}
		if (underlyingValueLabel != null) {
			underlyingValueLabel.setText("");
		}
		if (currencyValueLabel != null) {
			currencyValueLabel.setText("");
		}
		if (amountValueLabel != null) {
			amountValueLabel.setText("");
		}
	}

	@Override
	public JPanel getPanel() {
		if (allocDetailPanel == null) {
			initAllocDetailPanel();
		}
		return allocDetailPanel;
	}

	private JPanel initAllocDetailPanel() {
		allocDetailPanel = new JPanel();
		GridLayout gridLayoutAllocDetail = new GridLayout(2, 1, 5, 5);
		allocDetailPanel.setLayout(gridLayoutAllocDetail);
		allocDetailPanel.setBorder(new TitledBorder(new EtchedBorder(1, null,
				null), "PDV Message Details", 4, 2, null, null));

		contractInfoPanel.setBorder(new TitledBorder(new EtchedBorder(
				Color.BLUE, Color.gray), "Contract", 4, 2, null, Color.blue));
		contractInfoPanel.setOpaque(true);
		contractInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));

		allocInfoPanel.setBorder(new TitledBorder(new EtchedBorder(Color.PINK,
				Color.blue), "Allocations Details", 4, 2, null, Color.BLUE));
		allocInfoPanel.setOpaque(true);
		allocInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));

		contractInfoPanel.add(Box.createHorizontalStrut(15));

		// start contractNameLabel properties
		contractNameLabel.setText("Contract Name");
		contractNameLabel.setEnabled(true);
		contractNameLabel.setMaximumSize(new Dimension(120, 24));
		contractNameLabel.setMinimumSize(new Dimension(80, 24));
		contractNameLabel.setPreferredSize(new java.awt.Dimension(90, 24));
		contractNameLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		contractNameValueLabel
				.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		// end contractNameLabel properties
		contractInfoPanel.add(contractNameLabel);

		contractInfoPanel.add(Box.createHorizontalStrut(5));

		// start contractNameValueLabel properties
		contractNameValueLabel.setText("");
		contractNameValueLabel.setEnabled(true);
		contractNameValueLabel.setEnabled(true);
		contractNameValueLabel.setFont(new Font(getFont().getName(),
				Font.PLAIN, getFont().getSize()));
		contractNameValueLabel.setMaximumSize(new Dimension(300, 24));
		contractNameValueLabel.setMinimumSize(new Dimension(180, 24));
		contractNameValueLabel
				.setPreferredSize(new java.awt.Dimension(220, 24));
		contractNameValueLabel.setBorder(BorderFactory.createEtchedBorder());
		contractNameValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		contractNameValueLabel
				.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		// end contractNameValueLabel properties
		contractInfoPanel.add(contractNameValueLabel);

		contractInfoPanel.add(Box.createHorizontalStrut(10));

		// start contractIdLabel properties
		contractIdLabel.setText("Contract Id");
		contractIdLabel.setEnabled(true);
		contractIdLabel.setMaximumSize(new Dimension(100, 24));
		contractIdLabel.setMinimumSize(new Dimension(70, 24));
		contractIdLabel.setPreferredSize(new java.awt.Dimension(80, 24));
		contractIdLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		contractIdLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		// end contractIdLabel properties
		contractInfoPanel.add(contractIdLabel);

		contractInfoPanel.add(Box.createHorizontalStrut(5));

		// start contractIdValueLabel properties
		contractIdValueLabel.setText("");
		contractIdValueLabel.setEnabled(true);
		contractIdValueLabel.setEnabled(true);
		contractIdValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		contractIdValueLabel.setMaximumSize(new Dimension(100, 24));
		contractIdValueLabel.setMinimumSize(new Dimension(70, 24));
		contractIdValueLabel.setPreferredSize(new java.awt.Dimension(80, 24));
		contractIdValueLabel.setBorder(BorderFactory.createEtchedBorder());
		contractIdValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		contractIdValueLabel
				.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		// end contractIdValueLabel properties
		contractInfoPanel.add(contractIdValueLabel);

		allocDetailPanel.add(contractInfoPanel, BorderLayout.CENTER);

		// start valueDateLabel properties
		valueDateLabel.setText("Value Date");
		valueDateLabel.setEnabled(true);
		valueDateLabel.setMaximumSize(new Dimension(90, 24));
		valueDateLabel.setMinimumSize(new Dimension(70, 24));
		valueDateLabel.setPreferredSize(new java.awt.Dimension(80, 24));
		valueDateLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end valueDateLabel properties
		allocInfoPanel.add(valueDateLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start valueDateValueLabel properties
		valueDateValueLabel.setText("");
		valueDateValueLabel.setEnabled(true);
		valueDateValueLabel.setEnabled(true);
		valueDateValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		valueDateValueLabel.setMaximumSize(new Dimension(100, 24));
		valueDateValueLabel.setMinimumSize(new Dimension(80, 24));
		valueDateValueLabel.setPreferredSize(new java.awt.Dimension(80, 24));
		valueDateValueLabel.setBorder(BorderFactory.createEtchedBorder());
		valueDateValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end valueDateValueLabel properties
		allocInfoPanel.add(valueDateValueLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(10));

		// start portFolioLabel properties
		portFolioLabel.setText("Portfolio");
		portFolioLabel.setEnabled(true);
		portFolioLabel.setMaximumSize(new Dimension(80, 24));
		portFolioLabel.setMinimumSize(new Dimension(60, 24));
		portFolioLabel.setPreferredSize(new java.awt.Dimension(70, 24));
		portFolioLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end portFolioLabel properties
		allocInfoPanel.add(portFolioLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start portFolioValueLabel properties
		portFolioValueLabel.setText("");
		portFolioValueLabel.setEnabled(true);
		portFolioValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		portFolioValueLabel.setMaximumSize(new Dimension(150, 24));
		portFolioValueLabel.setMinimumSize(new Dimension(100, 24));
		portFolioValueLabel.setPreferredSize(new java.awt.Dimension(120, 24));
		portFolioValueLabel.setBorder(BorderFactory.createEtchedBorder());
		portFolioValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end portFolioValueLabel properties
		allocInfoPanel.add(portFolioValueLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(10));

		// start underlyingTypeLabel properties
		underlyingTypeLabel.setText("Underlying Type");
		underlyingTypeLabel.setEnabled(true);
		underlyingTypeLabel.setMaximumSize(new Dimension(100, 24));
		underlyingTypeLabel.setMinimumSize(new Dimension(7080, 24));
		underlyingTypeLabel.setPreferredSize(new java.awt.Dimension(90, 24));
		underlyingTypeLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end underlyingTypeLabel properties
		allocInfoPanel.add(underlyingTypeLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start underlyingTypeValueLabel properties
		underlyingTypeValueLabel.setText("");
		underlyingTypeValueLabel.setEnabled(true);
		underlyingTypeValueLabel.setFont(new Font(getFont().getName(),
				Font.PLAIN, getFont().getSize()));
		underlyingTypeValueLabel.setMaximumSize(new Dimension(50, 24));
		underlyingTypeValueLabel.setMinimumSize(new Dimension(30, 24));
		underlyingTypeValueLabel
				.setPreferredSize(new java.awt.Dimension(40, 24));
		underlyingTypeValueLabel.setBorder(BorderFactory.createEtchedBorder());
		underlyingTypeValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end underlyingTypeValueLabel properties
		allocInfoPanel.add(underlyingTypeValueLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(10));

		// start underlyingLabel properties
		underlyingLabel.setText("Underlying");
		underlyingLabel.setEnabled(true);
		underlyingLabel.setMaximumSize(new Dimension(80, 24));
		underlyingLabel.setMinimumSize(new Dimension(50, 24));
		underlyingLabel.setPreferredSize(new java.awt.Dimension(70, 24));
		underlyingLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end underlyingLabel properties
		allocInfoPanel.add(underlyingLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start underlyingValueLabel properties
		underlyingValueLabel.setText("");
		underlyingValueLabel.setEnabled(true);
		underlyingValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		underlyingValueLabel.setMaximumSize(new Dimension(110, 24));
		underlyingValueLabel.setMinimumSize(new Dimension(90, 24));
		underlyingValueLabel.setPreferredSize(new java.awt.Dimension(100, 24));
		underlyingValueLabel.setBorder(BorderFactory.createEtchedBorder());
		underlyingValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end underlyingValueLabel properties
		allocInfoPanel.add(underlyingValueLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(10));

		// start currencyLabel properties
		currencyLabel.setText("Currency");
		currencyLabel.setEnabled(true);
		currencyLabel.setMaximumSize(new Dimension(70, 24));
		currencyLabel.setMinimumSize(new Dimension(50, 24));
		currencyLabel.setPreferredSize(new java.awt.Dimension(60, 24));
		currencyLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end currencyLabel properties
		allocInfoPanel.add(currencyLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start currencyValueLabel properties
		currencyValueLabel.setText("");
		currencyValueLabel.setEnabled(true);
		currencyValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		currencyValueLabel.setMaximumSize(new Dimension(50, 24));
		currencyValueLabel.setMinimumSize(new Dimension(40, 24));
		currencyValueLabel.setPreferredSize(new java.awt.Dimension(40, 24));
		currencyValueLabel.setBorder(BorderFactory.createEtchedBorder());
		currencyValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end currencyValueLabel properties
		allocInfoPanel.add(currencyValueLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(10));

		// start amountLabel properties
		amountLabel.setText("Amount");
		amountLabel.setEnabled(true);
		amountLabel.setMaximumSize(new Dimension(60, 24));
		amountLabel.setMinimumSize(new Dimension(40, 24));
		amountLabel.setPreferredSize(new java.awt.Dimension(50, 24));
		amountLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		// end allocAmountLabel properties
		allocInfoPanel.add(amountLabel);

		allocInfoPanel.add(Box.createHorizontalStrut(5));

		// start amountLabel properties
		amountValueLabel.setText("");
		amountValueLabel.setEnabled(true);
		amountValueLabel.setFont(new Font(getFont().getName(), Font.PLAIN,
				getFont().getSize()));
		amountValueLabel.setMaximumSize(new Dimension(100, 24));
		amountValueLabel.setMinimumSize(new Dimension(70, 24));
		amountValueLabel.setPreferredSize(new java.awt.Dimension(80, 24));
		amountValueLabel.setBorder(BorderFactory.createEtchedBorder());
		amountValueLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end allocAmountLabel properties
		allocInfoPanel.add(amountValueLabel);

		allocDetailPanel.add(allocInfoPanel, BorderLayout.CENTER);

		return allocDetailPanel;
	}

	@Override
	public void showException(Task task, TaskGetInfo taskgetinfo) {
		if (allocDetailPanel == null) {
			initAllocDetailPanel();
		}
		displayAllocationDetails(task);
	}

	private void displayAllocationDetails(Task task) {
		if (PDVUtil.isMoreThanOneEligibleContract(task)) {
			List<String> fields = Arrays.asList(
					COLLAT_VALUE_DATE_FIELD,
					COLLAT_PORTFOLIO_FIELD,
					COLLAT_UNDERLYING_TYPE_FIELD,
					COLLAT_UNDERLYING_FIELD,
					COLLAT_AMOUNT_CCY_FIELD, COLLAT_AMOUNT_FIELD);

			HashMap<String, String> values = (HashMap<String, String>) PDVUtil
					.getFieldValues(EnumMessageType.COLLAT_MESSAGE,
							task.getInternalReference(), fields);

			// contract name
			if (task.getLinkId() != 0) {
				contractIdValueLabel.setText(String.valueOf((int) (task
						.getLinkId())));
				CollateralConfig mcc = CacheCollateralClient
						.getCollateralConfig(DSConnection.getDefault(),
								(int) task.getLinkId());
				if (mcc != null) {
					contractNameValueLabel.setText(mcc.getName());
				}

			} else {
				contractIdValueLabel.setText("");
				contractNameValueLabel.setText("");
			}

			// fill alloc fields
			if (!Util.isEmpty(values.keySet())) {
				valueDateValueLabel.setText(values
						.get(COLLAT_VALUE_DATE_FIELD));
				portFolioValueLabel.setText(values
						.get(COLLAT_PORTFOLIO_FIELD));
				underlyingTypeValueLabel.setText(values
						.get(COLLAT_UNDERLYING_TYPE_FIELD));
				underlyingValueLabel.setText(values
						.get(COLLAT_UNDERLYING_FIELD));
				currencyValueLabel.setText(values
						.get(COLLAT_AMOUNT_CCY_FIELD));
				amountValueLabel.setText(values
						.get(COLLAT_AMOUNT_FIELD));
			}
		} else {
			clean();
		}
	}

	@Override
	public void stateChanged(String s) {
		if (getParent() != null) {
			setSize(new Dimension((int) getParent().getSize().getWidth() - 22,
					(int) getParent().getSize().getHeight() - 30));
		}
	}

	@Override
	public void show(Task selectedTask, TaskGetInfo info) {
		showException(selectedTask, info);
	}
}
