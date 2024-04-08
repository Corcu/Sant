package calypsox.tk.util.optimizer.position;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.service.DSConnection;

public class OptimizerPositions {

	public static final String EQUITY = "Equity";

	public static final String BOND = "Bond";
	public static final String BALANCEPLEDGEDOUT = "Balance PledgedOut";
	public static final String BALANCEUNAVAILABLE = "Balance Unvailable";

	public static final int POSITIONS_NB_DAYS = 10;

	private OptimizerPositionsKey optimizerPosKey = null;

	private Product security = null;
	private String productType = null;

	// HashMap containing EnumPositionType positions
	protected HashMap<EnumPositionType, HashMap<EnumBalanceType, double[]>> positions = new HashMap<EnumPositionType, HashMap<EnumBalanceType, double[]>>();

	public HashMap<EnumPositionType, HashMap<EnumBalanceType, double[]>> getPositions() {
		return positions;
	}

	public void setPositions(
			HashMap<EnumPositionType, HashMap<EnumBalanceType, double[]>> positions) {
		this.positions = positions;
	}

	public enum EnumPositionType {
		ACTUAL, THEORETICAL, NOT_SETLLED;

		public String toString(EnumPositionType elem) {
			switch (elem) {
			case ACTUAL:
				return InventorySecurityPosition.ACTUAL_TYPE;
			case THEORETICAL:
				return InventorySecurityPosition.THEORETICAL_TYPE;
			case NOT_SETLLED:
				return "NOT SETLLED";
			default:
				break;
			}
			return "";
		}
	}

	public enum EnumBalanceType {
		BALANCE, BALANCE_PLEDGE_OUT, BALANCE_UNAVAILABLE;

		public String toString(EnumBalanceType elem) {
			switch (elem) {
			case BALANCE:
				return InventorySecurityPosition.BALANCE_DEFAULT;
			case BALANCE_PLEDGE_OUT:
				return BALANCEPLEDGEDOUT;
			case BALANCE_UNAVAILABLE:
				return BALANCEUNAVAILABLE;
			default:
				break;
			}
			return "";
		}
	}

	public OptimizerPositions(JDate initialPositionDate, String legalEntity,
			int securityId, int bookId) {
		super();
		this.initialPositionDate = initialPositionDate;

		this.optimizerPosKey = new OptimizerPositionsKey(securityId, bookId);

		initSecurity();
		initPositions();
	}

	private void initSecurity() {
		try {
			security = DSConnection.getDefault().getRemoteProduct()
					.getProduct(this.getSecurityId());
			productType = security.getType();
		} catch (RemoteException e) {
			Log.error(OptimizerPositions.class.getName(), e);
		}
	}

	public OptimizerPositions(JDate initialPositionDate, String legalEntity,
			OptimizerPositionsKey optPosKey) {
		super();
		this.initialPositionDate = initialPositionDate;

		this.optimizerPosKey = optPosKey;

		initSecurity();
		initPositions();
	}

	private void initPositions() {
		for (EnumPositionType positionType : EnumPositionType.values()) {
			for (EnumBalanceType balanceType : EnumBalanceType.values()) {

				HashMap<EnumBalanceType, double[]> balancePositions = this.positions
						.get(positionType);
				if (balancePositions == null) {
					balancePositions = new HashMap<EnumBalanceType, double[]>();
				}
				balancePositions
						.put(balanceType, new double[POSITIONS_NB_DAYS]);
				this.positions.put(positionType, balancePositions);
				for (int i = 0; i < POSITIONS_NB_DAYS; i++) {
					this.positions.get(positionType).get(balanceType)[i] = Double.NaN;
				}
			}
		}
	}

	private JDate initialPositionDate = null;

	public JDate getInitialPositionDate() {
		return initialPositionDate;
	}

	public void setInitialPositionDate(JDate valDate) {
		this.initialPositionDate = valDate;
	}

	public int getSecurityId() {
		return this.optimizerPosKey.getSecurityId();
	}

	public int getBookId() {
		return this.optimizerPosKey.getBookId();
	}

	public void addPosition(InventorySecurityPosition invSecPos,
			JDate positionDate) {
		int positionDateIdx = (int) JDate.diff(getInitialPositionDate(),
				positionDate);
		this.positions.get(getEnumPositionType(invSecPos.getPositionType()))
				.get(EnumBalanceType.BALANCE)[positionDateIdx] = invSecPos
				.getTotalSecurity();
		this.positions.get(getEnumPositionType(invSecPos.getPositionType()))
				.get(EnumBalanceType.BALANCE_PLEDGE_OUT)[positionDateIdx] = invSecPos
				.getTotalPledgedOut();
		this.positions.get(getEnumPositionType(invSecPos.getPositionType()))
				.get(EnumBalanceType.BALANCE_UNAVAILABLE)[positionDateIdx] = invSecPos
				.getTotalUnavailable();
	}

	private EnumPositionType getEnumPositionType(String positionType) {
		if (InventorySecurityPosition.ACTUAL_TYPE.equals(positionType)) {
			return EnumPositionType.ACTUAL;

		} else if (InventorySecurityPosition.THEORETICAL_TYPE
				.equals(positionType)) {
			return EnumPositionType.THEORETICAL;
		}
		return null;
	}

	@Override
	public String toString() {
		if (hasPositions()) {
			List<OptimizerExportPosition> optimizerExportPositions = OptimizerPositionsFormatter
					.format(this);
			if (!Util.isEmpty(optimizerExportPositions)) {
				StringBuffer sb = new StringBuffer();
				for (OptimizerExportPosition optimizerExportPosition : optimizerExportPositions) {
					sb.append(optimizerExportPosition.getValuePos());
				}
			}
		}
		return "";
	}

	public boolean hasPositions() {
		for (EnumPositionType positionType : this.positions.keySet()) {
			for (EnumBalanceType balanceType : this.positions.get(positionType)
					.keySet()) {
				if (hasPositions(positionType, balanceType)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasPositions(EnumPositionType positionType,
			EnumBalanceType balanceType) {
		for (int i = 0; i < OptimizerPositions.POSITIONS_NB_DAYS; i++) {
			if (!Double.isNaN(positions.get(positionType).get(balanceType)[i])
			/* && positions.get(positionType).get(balanceType)[i] != 0.0 */) {
				return true;
			}
		}
		return false;
	}

	private double getPositionValue(double posValue) {
		return Double.isNaN(posValue) ? 0.00 : posValue;
	}

	public void completePositions() {
		// fill intermediary ACT and THEOR positions
		fillIntermediaryPositions(Arrays.asList(EnumPositionType.THEORETICAL,
				EnumPositionType.ACTUAL), Arrays.asList(
				EnumBalanceType.BALANCE, EnumBalanceType.BALANCE_PLEDGE_OUT,
				EnumBalanceType.BALANCE_UNAVAILABLE));
		// fill NOT_SETTLED positions
		for (EnumBalanceType balanceType : EnumBalanceType.values()) {
			for (int i = 0; i < OptimizerPositions.POSITIONS_NB_DAYS; i++) {
				this.positions.get(EnumPositionType.NOT_SETLLED).get(
						balanceType)[i] = getPositionValue(this.positions.get(
						EnumPositionType.THEORETICAL).get(balanceType)[i])
						- getPositionValue(this.positions.get(
								EnumPositionType.ACTUAL).get(balanceType)[i]);
			}
		}
	}

	private void fillIntermediaryPositions(
			List<EnumPositionType> listPositionTypes,
			List<EnumBalanceType> listBalanceTypes) {
		for (EnumPositionType positionType : listPositionTypes) {
			for (EnumBalanceType balanceType : listBalanceTypes) {
				for (int i = 1; i < OptimizerPositions.POSITIONS_NB_DAYS; i++) {
					if (!Double.isNaN(this.positions.get(positionType).get(
							balanceType)[i - 1])
							&& Double.isNaN(this.positions.get(positionType)
									.get(balanceType)[i])) {
						this.positions.get(positionType).get(balanceType)[i] = this.positions
								.get(positionType).get(balanceType)[i - 1];
					}
				}
			}
		}
	}

	public static class OptimizerPositionsKey {

		@Override
		public String toString() {
			return "OptimizerPositionsKey [securityId=" + securityId
					+ ", bookId=" + bookId + "]";
		}

		public OptimizerPositionsKey(int securityId, int bookId) {
			super();
			this.securityId = securityId;
			this.bookId = bookId;
		}

		public int getSecurityId() {
			return securityId;
		}

		public void setSecurityId(int securityId) {
			this.securityId = securityId;
		}

		public int getBookId() {
			return bookId;
		}

		public void setBookId(int bookId) {
			this.bookId = bookId;
		}

		private int securityId = 0;
		private int bookId = 0;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + this.bookId;
			result = (prime * result) + this.securityId;

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			OptimizerPositionsKey other = (OptimizerPositionsKey) obj;
			if (this.securityId != other.securityId) {
				return false;
			}
			if (this.bookId != other.bookId) {
				return false;
			}
			return true;
		}
	}

	public String getProductType() {
		return productType;
	}

	public Product getSecurity() {
		return security;
	}

	public String getISIN() {
		return security.getSecCode("ISIN");
	}

	public String getCurrency() {
		return security.getCurrency();
	}

	public String getProductFamily() {
		return security.getProductFamily();
	}

	public static class OptimizerPositionBalanceKey {

		public EnumPositionType getPositionType() {
			return positionType;
		}

		public void setPositionType(EnumPositionType positionType) {
			this.positionType = positionType;
		}

		public EnumBalanceType getBalanceType() {
			return balanceType;
		}

		public void setBalanceType(EnumBalanceType balanceType) {
			this.balanceType = balanceType;
		}

		private EnumPositionType positionType = null;
		private EnumBalanceType balanceType = null;

		public OptimizerPositionBalanceKey(EnumPositionType positionType,
				EnumBalanceType balanceType) {
			super();
			this.positionType = positionType;
			this.balanceType = balanceType;
		}
	}
}
