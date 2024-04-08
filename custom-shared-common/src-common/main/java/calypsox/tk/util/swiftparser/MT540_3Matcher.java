package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.GenericPlatformMatcher;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.sql.SQLQuery;

import java.util.Collections;

public class MT540_3Matcher extends GenericPlatformMatcher {

    @Override
    protected SQLQuery buildMT540toMT543TransfersQuery(SwiftMessage message) {
        SQLQuery query = super.buildMT540toMT543TransfersQuery(message);
        switch(message.getType()) {
            case "MT540":
            case "MT541":
               return filterByPayReceive(query,"RECEIVE");
            case "MT542":
            case "MT543":
                return filterByPayReceive(query,"PAY");

        }
        return query;
    }

    private SQLQuery filterByPayReceive(SQLQuery query, String payReceiveType) {
        query.appendWhereClause("  bo_transfer.payreceive_type = ? ", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, payReceiveType)));
        return query;
    }
}
