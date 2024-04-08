package calypsox.tk.util.swiftparser;

import calypsox.tk.swift.formatter.CalypsoAppIdentifier;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.risk.util.AnalysisProgressUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.swiftparser.TagParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import static calypsox.tk.bo.workflow.rule.UpdateMxElectplatidKWMessageRule.MASTER_REFERENCE;

public class MT509Matcher extends com.calypso.tk.util.swiftparser.MT509Matcher {

    protected Object indexETCMS(SwiftMessage mess, Object dbCon, Vector errors) throws MessageParseException {
        SwiftFieldMessage swiftField = mess.getSwiftField(mess.getFields(), ":20C:", ":MAST//", null);
        if (swiftField == null) {
            throw new MessageParseException("Cannot find field :20C::MAST//");
        } else {
            TagParser tagParser = SwiftParserUtil.getTagParserClass("20", mess.getType());
            if (tagParser == null) {
                throw new MessageParseException("Cannot find a Tag20 Parser");
            } else {
                Object value = tagParser.parse(swiftField, "MT509");
                BOMessage indexedMessage = null;

                try {
                    //two scenarios message id in :20C::MAST// or KW Mx Electplatid
                    //try Msg Attr PORef = value

                    String incRef =value.toString().startsWith(CalypsoAppIdentifier.CYO.toString())?value.toString().substring(3):value.toString();

                    MessageArray found  = DSConnection.getDefault().getRemoteBO().getMessages("message_id in (select message_id from mess_attributes where attr_name =? and attr_value = ?)",
                            Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, MASTER_REFERENCE),
                                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, incRef)));
                    if (found != null && !found.isEmpty()) {
                        if (found.size()>1) {
                            SwiftFieldMessage tradeDateSwiftField = mess.getSwiftField(mess.getFields(), ":98C:", ":TRAD//", null);
                           final Date td = getTradeDate(tradeDateSwiftField);

                            indexedMessage =   Arrays.stream(found.getMessages()).filter(m-> {
                                if (m == null || !m.getTemplateName().startsWith("MT515") || !m.isOutgoingMessage() || m.getTradeLongId() <= 0)
                                    return false;

                                if (td == null)
                                    return true;

                                Trade t = null;
                                try {
                                    t = DSConnection.getDefault().getRemoteTrade().getTrade(m.getTradeLongId());
                                } catch (CalypsoServiceException e) {
                                   Log.error(this, e);
                                   return false;
                                }
                                if (t != null) {
                                    JDatetime from = new JDatetime(td.getTime()).add(-5, 0, 0, 0, 0);
                                    JDatetime to = new JDatetime(td.getTime()).add(5, 0, 0, 0, 0);
                                    return t.getTradeDate().after(from) && t.getTradeDate().before(to);
                                } else {
                                    Log.error(this, "Trade not found by id " + m.getTradeLongId());
                                    return false;
                                }
                            }).findFirst().orElse(null);


                        } else {
                            indexedMessage = found.get(0);
                        }
                    }

                    if (indexedMessage ==null)
                        indexedMessage = DSConnection.getDefault().getRemoteBO().getMessage(Long.parseLong(incRef));

                    AnalysisProgressUtil.logProgressDetail("Indexed outgoing " + indexedMessage);
                } catch (Exception var9) {
                    Log.error(this, var9);
                }

                if (indexedMessage == null) {
                    Log.error(this, "No Message matching for Id:" + value.toString());
                    errors.addElement("No Message matching for Id:" + value.toString());
                    return null;
                } else {
                    return indexedMessage;
                }
            }
        }
    }

    private Date getTradeDate(SwiftFieldMessage field) {
        try {
            return field == null || Util.isEmpty(field.getValue()) ? null : (new SimpleDateFormat("yyyyMMddHHmmss")).parse(field.getValue());
        } catch (ParseException e) {
            Log.error(this, e);
           return null;
        }
    }
}
