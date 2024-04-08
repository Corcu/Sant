package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.MarginCallDetailEntryDTOReportTemplate;

/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
public class SantMCPortfolioReportTemplate extends MarginCallDetailEntryDTOReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		// Set default columns
		Vector<String> columns = new Vector<String>();
		columns.add(SantMCPortfolioReportStyle.COLLATERAL_AGREE);
		columns.add(SantMCPortfolioReportStyle.COLLATERAL_AGREE_TYPE);
		columns.add(SantMCPortfolioReportStyle.COUNTERPARTY);
		columns.add(SantMCPortfolioReportStyle.TRADE_ID);
		columns.add(SantMCPortfolioReportStyle.FRONT_OFFICE_ID);
		columns.add(SantMCPortfolioReportStyle.CLOSE_OF_BUSINESS);
		columns.add(SantMCPortfolioReportStyle.STRUCTURE);
		columns.add(SantMCPortfolioReportStyle.TRADE_DATE);
		columns.add(SantMCPortfolioReportStyle.VALUE_DATE);
		columns.add(SantMCPortfolioReportStyle.MATURITY_DATE);
		columns.add(SantMCPortfolioReportStyle.VALUATION_AGENT);
		columns.add(SantMCPortfolioReportStyle.PORTFOLIO);
		columns.add(SantMCPortfolioReportStyle.OWNER);
		columns.add(SantMCPortfolioReportStyle.DEAL_OWNER);
		columns.add(SantMCPortfolioReportStyle.INSTRUMENT);
		columns.add(SantMCPortfolioReportStyle.UNDERLYING);
		columns.add(SantMCPortfolioReportStyle.PRINCIPAL);
		columns.add(SantMCPortfolioReportStyle.PRINCIPAL_CCY);
		columns.add(SantMCPortfolioReportStyle.PRINCIPAL_2);
		columns.add(SantMCPortfolioReportStyle.PRINCIPAL_CCY_2);
		columns.add(SantMCPortfolioReportStyle.INDEPENDENT_AMOUNT);
		columns.add(SantMCPortfolioReportStyle.RATE);
		columns.add(SantMCPortfolioReportStyle.RATE_2);
		columns.add(SantMCPortfolioReportStyle.BUY_SELL);
		columns.add(SantMCPortfolioReportStyle.BASE_CCY);
		columns.add(SantMCPortfolioReportStyle.MTM_BASE_CCY);
		columns.add(SantMCPortfolioReportStyle.SANT_EXPOSURE);

		setColumns(columns.toArray(new String[columns.size()]));
	}

}
