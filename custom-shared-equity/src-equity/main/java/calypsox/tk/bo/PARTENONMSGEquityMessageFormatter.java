package calypsox.tk.bo;

import calypsox.util.partenon.PartenonUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.io.StringWriter;
import java.util.Optional;
import java.util.Vector;

public class PARTENONMSGEquityMessageFormatter extends MessageFormatter {
    public String parsePARTENON_ROW(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        String partenonMessage = "";
        if(null!=trade){
            partenonMessage = generateMsg(trade);
        }
        return partenonMessage;
    }

    /**
     * Generate Message
     *
     * @param trade
     * @return
     */
    private String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        //0:Alta 1:Baja 2:Modificacion
        sw.append("0").append(";");//sw.append(PartenonUtil.getInstance().getAction(trade)).append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getProductSubType(trade)).append(";");

        return sw.toString();
    }

    /**
     * Get Product SubType
     *
     * @return
     */
    private String getProductSubType(Trade trade){
        Equity product = (Equity) trade.getProduct();
        String type = product.getSecurity()!=null ? product.getSecurity().getType() : "";
        String equityType = product.getSecCode("EQUITY_TYPE")!=null ? product.getSecCode("EQUITY_TYPE") : "";
        String issuerName = product.getIssuer().getCode()!=null ? product.getIssuer().getCode() : "";
        Book book = trade.getBook();
        String accountingLink = "";
        String result = "";
        if(null!=book) {
            accountingLink = trade.getBook().getAccountingBook().getName();
            if("Equity".equalsIgnoreCase(type)){
                if("Negociacion".equalsIgnoreCase(accountingLink)) {
                    if ("CS".equalsIgnoreCase(equityType)) {
                        if ("BSTE".equalsIgnoreCase(issuerName)){
                            if (isInternal(trade)) {
                                result = "RVAUINAONE";
                            } else {
                                result = "RVAUCOAONE";
                            }
                        } else{
                            if (isInternal(trade)) {
                                result = "RVCAINAONE";
                            } else {
                                result = "RVCACOAONE";
                            }
                        }
                    } else if ("DERSUS".equalsIgnoreCase(equityType)) {
                        if ("BSTE".equalsIgnoreCase(issuerName)){
                            if (isInternal(trade)) {
                                result = "RVCACINDSNE";
                            } else {
                                result = "RVAUCODSNE";
                            }
                        } else{
                            if (isInternal(trade)) {
                                result = "RVCAINDSNE";
                            } else {
                                result = "RVCACODSNE";
                            }
                        }
                    } else if ("PS".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINPPNE";
                        } else {
                            result = "RVCACOPPNE";
                        }
                    } else if ("INSW".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINCINE";
                        } else {
                            result = "RVCACOCINE";
                        }
                    } else if ("ADR".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINADNE";
                        } else {
                            result = "RVCACOADNE";
                        }
                    } else if ("PFI".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINPFAM";
                        } else {
                            result = "RVCACOPFAM";
                        }
                    } else if ("CO2".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAIND2NE";
                        } else {
                            result = "RVCACOD2NE";
                        }
                    } else if ("VCO2".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINV2NE";
                        } else {
                            result = "RVCACOV2NE";
                        }
                    } else if ("ETF".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINETNE";
                        } else {
                            result = "RVCACOETNE";
                        }
                    }
                } else if ("Inversion crediticia".equalsIgnoreCase(accountingLink)) {
                    if ("PS".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINPPAM";
                        } else {
                            result = "RVCACOPPAM";
                        }
                    } else if ("PEGROP".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINEGAM";
                        } else {
                            result = "RVCACOEGAM";
                        }
                    }
                } else if ("Otros a valor razonable".equalsIgnoreCase(accountingLink)) {
                    if ("CS".equalsIgnoreCase(equityType)) {
                        if ("BSTE".equalsIgnoreCase(issuerName)){
                            if (isInternal(trade)) {
                                result = "RVAUINAOOV";
                            } else {
                                result = "RVAUCOAOOV";
                            }
                        } else{
                            if (isInternal(trade)) {
                                result = "RVCAINAOOV";
                            } else {
                                result = "RVCACOAOOV";
                            }
                        }
                    } else if ("DERSUS".equalsIgnoreCase(equityType)) {
                        if ("BSTE".equalsIgnoreCase(issuerName)){
                            if (isInternal(trade)) {
                                result = "RVCACINDSOV";
                            } else {
                                result = "RVAUCODSOV";
                            }
                        } else{
                            if (isInternal(trade)) {
                                result = "RVCAINDSOV";
                            } else {
                                result = "RVCACODSOV";
                            }
                        }
                    } else if ("PS".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINPPOV";
                        } else {
                            result = "RVCACOPPOV";
                        }
                    } else if ("INSW".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINCIOV";
                        } else {
                            result = "RVCACOCIOV";
                        }
                    } else if ("ADR".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINADOV";
                        } else {
                            result = "RVCACOADOV";
                        }
                    } else if ("PFI".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINPFOV";
                        } else {
                            result = "RVCACOPFOV";
                        }
                    } else if ("CO2".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAIND2OV";
                        } else {
                            result = "RVCACOD2OV";
                        }
                    }  else if ("VCO2".equalsIgnoreCase(equityType)) {
                        if (isInternal(trade)) {
                            result = "RVCAINV2OV";
                        } else {
                            result = "RVCACOV2OV";
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean isInternal(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }
}
