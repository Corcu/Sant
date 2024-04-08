package calypsox.tk.util;

public class LagoEquitySwapLeg{

    private String action;
    private String fo_system;
    private int num_front_id;
    private String owner;
    private String counterparty;
    private String instrument;
    private String portfolio;
    private String value_date;
    private String trade_date;
    private String maturity_date;
    private String direction;
    private String nominal;
    private String nominal_ccy;
    private String mtm;
    private String mtm_ccy;
    private String mtm_date;
    private String bo_system;
    private String bo_reference;
    private String underlying_type;
    private String underlying;
    private String closing_price;
    private String structure_id;
    private String independent_amount;
    private String independent_amount_ccy;
    private String independent_amount_pay_receive;
    private String closing_price_at_start;
    private String nominal_sec;
    private String nominal_sec_ccy;
    private String haircut;
    private String haircut_direction;
    private String repo_rate;
    private String call_put;
    private String last_modified;
    private String trade_version;
    private String usi;
    private String sd_msp;
    private String us_party;
    private String dfa;
    private String fc_nfc_nfc;
    private String emir;
    private String uti;
    private String rig_code;
    private String lot_size;
    private String accrued_coupon;
    private String delivery_type;
    private String is_financement;
    private String upi;
    private String sbsd_msbsd;
    private String sbs_product;
    private String day_count_convention;
    private String swap_agent_id;
    private String swap_agent;

    public LagoEquitySwapLeg(String action, String fo_system, int num_front_id, String owner, String counterparty, String instrument, String portfolio,
                             String value_date, String trade_date, String maturity_date, String direction, String nominal, String nominal_ccy, String mtm,
                             String mtm_ccy, String mtm_date, String bo_system, String bo_reference, String underlying_type, String underlying,
                             String closing_price, String structure_id, String independent_amount, String independent_amount_ccy,
                             String independent_amount_pay_receive, String closing_price_at_start, String nominal_sec, String nominal_sec_ccy,
                             String haircut, String haircut_direction, String repo_rate, String call_put, String last_modified, String trade_version,
                             String usi, String sd_msp, String us_party, String dfa, String fc_nfc_nfc, String emir, String uti, String rig_code,
                             String lot_size, String accrued_coupon, String delivery_type, String is_financement, String upi, String sbsd_msbsd,
                             String sbs_product, String day_count_convention, String swap_agent_id, String swap_agent) {
        this.fo_system = fo_system;
        this.num_front_id = num_front_id;
        this.owner = owner;
        this.counterparty = counterparty;
        this.instrument = instrument;
        this.portfolio = portfolio;
        this.value_date = value_date;
        this.trade_date = trade_date;
        this.maturity_date = maturity_date;
        this.direction = direction;
        this.nominal = nominal;
        this.nominal_ccy = nominal_ccy;
        this.mtm = mtm;
        this.mtm_ccy = mtm_ccy;
        this.mtm_date = mtm_date;
        this.bo_system = bo_system;
        this.bo_reference = bo_reference;
        this.underlying_type = underlying_type;
        this.underlying = underlying;
        this.closing_price = closing_price;
        this.structure_id = structure_id;
        this.independent_amount = independent_amount;
        this.independent_amount_ccy = independent_amount_ccy;
        this.independent_amount_pay_receive = independent_amount_pay_receive;
        this.closing_price_at_start = closing_price_at_start;
        this.nominal_sec = nominal_sec;
        this.nominal_sec_ccy = nominal_sec_ccy;
        this.haircut = haircut;
        this.haircut_direction = haircut_direction;
        this.repo_rate = repo_rate;
        this.call_put = call_put;
        this.last_modified = last_modified;
        this.trade_version = trade_version;
        this.usi = usi;
        this.sd_msp = sd_msp;
        this.us_party = us_party;
        this.dfa = dfa;
        this.fc_nfc_nfc = fc_nfc_nfc;
        this.emir = emir;
        this.uti = uti;
        this.rig_code = rig_code;
        this.lot_size = lot_size;
        this.accrued_coupon = accrued_coupon;
        this.delivery_type = delivery_type;
        this.is_financement = is_financement;
        this.upi = upi;
        this.sbsd_msbsd = sbsd_msbsd;
        this.sbs_product = sbs_product;
        this.day_count_convention = day_count_convention;
        this.swap_agent_id = swap_agent_id;
        this.swap_agent = swap_agent;
    }

    @Override
    public String toString() {
        return  this.action
                + "|" + this.fo_system
                + "|" + this.num_front_id
                + "|" + this.owner
                + "|" + this.counterparty
                + "|" + this.instrument
                + "|" + this.portfolio
                + "|" + this.value_date
                + "|" + this.trade_date
                + "|" + this.maturity_date
                + "|" + this.direction
                + "|" + this.nominal
                + "|" + this.nominal_ccy
                + "|" + this.mtm
                + "|" + this.mtm_ccy
                + "|" + this.mtm_date
                + "|" + this.bo_system
                + "|" + this.bo_reference
                + "|" + this.underlying_type
                + "|" + this.underlying
                + "|" + this.closing_price
                + "|" + this.structure_id
                + "|" + this.independent_amount
                + "|" + this.independent_amount_ccy
                + "|" + this.independent_amount_pay_receive
                + "|" + this.closing_price_at_start
                + "|" + this.nominal_sec
                + "|" + this.nominal_sec_ccy
                + "|" + this.haircut
                + "|" + this.haircut_direction
                + "|" + this.repo_rate
                + "|" + this.call_put
                + "|" + this.last_modified
                + "|" + this.trade_version
                + "|" + this.usi
                + "|" + this.sd_msp
                + "|" + this.us_party
                + "|" + this.dfa
                + "|" + this.fc_nfc_nfc
                + "|" + this.emir
                + "|" + this.uti
                + "|" + this.rig_code
                + "|" + this.lot_size
                + "|" + this.accrued_coupon
                + "|" + this.delivery_type
                + "|" + this.is_financement
                + "|" + this.upi
                + "|" + this.sbsd_msbsd
                + "|" + this.sbs_product
                + "|" + this.day_count_convention
                + "|" + this.swap_agent_id
                + "|" + this.swap_agent;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFo_system() {
        return fo_system;
    }

    public void setFo_system(String fo_system) {
        this.fo_system = fo_system;
    }

    public int getNum_front_id() {
        return num_front_id;
    }

    public void setNum_front_id(int num_front_id) {
        this.num_front_id = num_front_id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    public String getValue_date() {
        return value_date;
    }

    public void setValue_date(String value_date) {
        this.value_date = value_date;
    }

    public String getTrade_date() {
        return trade_date;
    }

    public void setTrade_date(String trade_date) {
        this.trade_date = trade_date;
    }

    public String getMaturity_date() {
        return maturity_date;
    }

    public void setMaturity_date(String maturity_date) {
        this.maturity_date = maturity_date;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getNominal() {
        return nominal;
    }

    public void setNominal(String nominal) {
        this.nominal = nominal;
    }

    public String getNominal_ccy() {
        return nominal_ccy;
    }

    public void setNominal_ccy(String nominal_ccy) {
        this.nominal_ccy = nominal_ccy;
    }

    public String getMtm() {
        return mtm;
    }

    public void setMtm(String mtm) {
        this.mtm = mtm;
    }

    public String getMtm_ccy() {
        return mtm_ccy;
    }

    public void setMtm_ccy(String mtm_ccy) {
        this.mtm_ccy = mtm_ccy;
    }

    public String getMtm_date() {
        return mtm_date;
    }

    public void setMtm_date(String mtm_date) {
        this.mtm_date = mtm_date;
    }

    public String getBo_system() {
        return bo_system;
    }

    public void setBo_system(String bo_system) {
        this.bo_system = bo_system;
    }

    public String getBo_reference() {
        return bo_reference;
    }

    public void setBo_reference(String bo_reference) {
        this.bo_reference = bo_reference;
    }

    public String getUnderlying_type() {
        return underlying_type;
    }

    public void setUnderlying_type(String underlying_type) {
        this.underlying_type = underlying_type;
    }

    public String getUnderlying() {
        return underlying;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public String getClosing_price() {
        return closing_price;
    }

    public void setClosing_price(String closing_price) {
        this.closing_price = closing_price;
    }

    public String getStructure_id() {
        return structure_id;
    }

    public void setStructure_id(String structure_id) {
        this.structure_id = structure_id;
    }

    public String getIndependent_amount() {
        return independent_amount;
    }

    public void setIndependent_amount(String independent_amount) {
        this.independent_amount = independent_amount;
    }

    public String getIndependent_amount_ccy() {
        return independent_amount_ccy;
    }

    public void setIndependent_amount_ccy(String independent_amount_ccy) {
        this.independent_amount_ccy = independent_amount_ccy;
    }

    public String getIndependent_amount_pay_receive() {
        return independent_amount_pay_receive;
    }

    public void setIndependent_amount_pay_receive(String independent_amount_pay_receive) {
        this.independent_amount_pay_receive = independent_amount_pay_receive;
    }

    public String getClosing_price_at_start() {
        return closing_price_at_start;
    }

    public void setClosing_price_at_start(String closing_price_at_start) {
        this.closing_price_at_start = closing_price_at_start;
    }

    public String getNominal_sec() {
        return nominal_sec;
    }

    public void setNominal_sec(String nominal_sec) {
        this.nominal_sec = nominal_sec;
    }

    public String getNominal_sec_ccy() {
        return nominal_sec_ccy;
    }

    public void setNominal_sec_ccy(String nominal_sec_ccy) {
        this.nominal_sec_ccy = nominal_sec_ccy;
    }

    public String getHaircut() {
        return haircut;
    }

    public void setHaircut(String haircut) {
        this.haircut = haircut;
    }

    public String getHaircut_direction() {
        return haircut_direction;
    }

    public void setHaircut_direction(String haircut_direction) {
        this.haircut_direction = haircut_direction;
    }

    public String getRepo_rate() {
        return repo_rate;
    }

    public void setRepo_rate(String repo_rate) {
        this.repo_rate = repo_rate;
    }

    public String getCall_put() {
        return call_put;
    }

    public void setCall_put(String call_put) {
        this.call_put = call_put;
    }

    public String getLast_modified() {
        return last_modified;
    }

    public void setLast_modified(String last_modified) {
        this.last_modified = last_modified;
    }

    public String getTrade_version() {
        return trade_version;
    }

    public void setTrade_version(String trade_version) {
        this.trade_version = trade_version;
    }

    public String getUsi() {
        return usi;
    }

    public void setUsi(String usi) {
        this.usi = usi;
    }

    public String getSd_msp() {
        return sd_msp;
    }

    public void setSd_msp(String sd_msp) {
        this.sd_msp = sd_msp;
    }

    public String getUs_party() {
        return us_party;
    }

    public void setUs_party(String us_party) {
        this.us_party = us_party;
    }

    public String getDfa() {
        return dfa;
    }

    public void setDfa(String dfa) {
        this.dfa = dfa;
    }

    public String getFc_nfc_nfc() {
        return fc_nfc_nfc;
    }

    public void setFc_nfc_nfc(String fc_nfc_nfc) {
        this.fc_nfc_nfc = fc_nfc_nfc;
    }

    public String getEmir() {
        return emir;
    }

    public void setEmir(String emir) {
        this.emir = emir;
    }

    public String getUti() {
        return uti;
    }

    public void setUti(String uti) {
        this.uti = uti;
    }

    public String getRig_code() {
        return rig_code;
    }

    public void setRig_code(String rig_code) {
        this.rig_code = rig_code;
    }

    public String getLot_size() {
        return lot_size;
    }

    public void setLot_size(String lot_size) {
        this.lot_size = lot_size;
    }

    public String getAccrued_coupon() {
        return accrued_coupon;
    }

    public void setAccrued_coupon(String accrued_coupon) {
        this.accrued_coupon = accrued_coupon;
    }

    public String getDelivery_type() {
        return delivery_type;
    }

    public void setDelivery_type(String delivery_type) {
        this.delivery_type = delivery_type;
    }

    public String getIs_financement() {
        return is_financement;
    }

    public void setIs_financement(String is_financement) {
        this.is_financement = is_financement;
    }

    public String getUpi() {
        return upi;
    }

    public void setUpi(String upi) {
        this.upi = upi;
    }

    public String getSbsd_msbsd() {
        return sbsd_msbsd;
    }

    public void setSbsd_msbsd(String sbsd_msbsd) {
        this.sbsd_msbsd = sbsd_msbsd;
    }

    public String getSbs_product() {
        return sbs_product;
    }

    public void setSbs_product(String sbs_product) {
        this.sbs_product = sbs_product;
    }

    public String getDay_count_convention() {
        return day_count_convention;
    }

    public void setDay_count_convention(String day_count_convention) {
        this.day_count_convention = day_count_convention;
    }

    public String getSwap_agent_id() {
        return swap_agent_id;
    }

    public void setSwap_agent_id(String swap_agent_id) {
        this.swap_agent_id = swap_agent_id;
    }

    public String getSwap_agent() {
        return swap_agent;
    }

    public void setSwap_agent(String swap_agent) {
        this.swap_agent = swap_agent;
    }

}