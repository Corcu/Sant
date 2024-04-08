package calypsox.tk.anacredit.util;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Repo;

import java.sql.Time;
import java.util.TimeZone;

public class RepoTypeIdentifier {
        private Trade trade;
        private JDatetime valDatetime;
        private JDate valDate;
        private boolean isRepoRPPLZ;
        private boolean isCTA;
        private boolean isATA;
        private boolean isTERM;
        private boolean isExtendable;


        public RepoTypeIdentifier(Trade trade, JDatetime valDatetime) {
            this.trade = trade;
            this.valDatetime = valDatetime;
            this.valDate = valDatetime.getJDate(TimeZone.getDefault());
        }


        public boolean isRepoRPPLZ() {
            return isRepoRPPLZ;
        }

        public boolean isCTA() {
            return isCTA;
        }

        public boolean isATA() {
            return isATA;
        }

        public boolean isTERM() {
        return isTERM;
    }

        public boolean isExtendable() {
        return isExtendable;
    }


    public RepoTypeIdentifier invoke() {
            isRepoRPPLZ = false;
            if (trade.getProduct() instanceof Repo) {
                if (trade.getTradeDate().lte(valDatetime)
                        && ((Repo)trade.getProduct()).getStartDate().after(valDatetime.getJDate(TimeZone.getDefault()))) {
                    isRepoRPPLZ = true;
                }

                isTERM = ((Repo)trade.getProduct()).getMaturityType().equals("TERM");
                isExtendable = ((Repo)trade.getProduct()).getMaturityType().equals("EXTENDABLE");
            }
            isCTA = trade.computeNominal(valDate) < 0 && !isRepoRPPLZ;
            isATA = !isCTA && !isRepoRPPLZ;
            return this;
        }
    }