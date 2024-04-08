package calypsox.tk.refdata.service.impl;

import calypsox.tk.refdata.CustomLeveragePercentage;
import calypsox.tk.refdata.CustomLeveragePercentageAttr;
import calypsox.tk.refdata.service.RemoteCustomLeverageService;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.sql.AttributesSQL;
import com.calypso.tk.core.sql.ConnectionManager;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import static com.calypso.tk.core.sql.ioSQL.*;
import static com.calypso.tk.core.sql.ioSQLBase.releaseConnection;

/**
 * @author x865229
 * date 02/01/2023
 * @see calypsox.tk.refdata.service.RemoteCustomLeverageService
 */
@SuppressWarnings("unused")
@Stateless(name = "calypsox.tk.refdata.service.RemoteCustomLeverageService")
@Remote(calypsox.tk.refdata.service.RemoteCustomLeverageService.class)
@Local(calypsox.tk.refdata.service.LocalCustomLeverageService.class)
public class CustomLeverageServiceImpl implements RemoteCustomLeverageService {


    @Override
    public CustomLeveragePercentage loadAll() throws RemoteException {
        ConnectionManager cm = ConnectionManager.getInstance(null);
        try {
            Hashtable<Integer, Attributes> hash = loadAllInternal(cm.getConnection());
            return CustomLeveragePercentage.fromAttributesMultiple(hash.values());
        } catch (PersistenceException e) {
            throw new RemoteException(e.getMessage(), e);
        } finally {
            cm.release();
        }
    }

    private Hashtable<Integer, Attributes> loadAllInternal(Connection con) throws PersistenceException {
        @SuppressWarnings("unchecked")
        Hashtable<Integer, Attributes> hash = AttributesSQL.getAll(CustomLeveragePercentageAttr.ENTITY_TYPE,
                null, "entity_attributes.attr_name = ?", con,
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, CustomLeveragePercentage.BINARY_ATTR_NAME)));
        return hash;
    }

    @Override
    public CustomLeveragePercentage loadByProduct(int productId) throws RemoteException {
        try {
            CustomLeveragePercentageAttr attr = new CustomLeveragePercentageAttr(productId, 0);
            AttributesSQL.get(attr);
            return CustomLeveragePercentage.fromAttributes(attr.getAttributes());
        } catch (PersistenceException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public void save(CustomLeveragePercentage leveragePercentage) throws RemoteException {
        List<CustomLeveragePercentageAttr> attrList = leveragePercentage.toAttrList();
        Connection con = null;
        try {
            con = getConnection();
            //current set
            Hashtable<Integer, Attributes> toRemove = loadAllInternal(con);
            // save new / update existing
            for (CustomLeveragePercentageAttr attr : attrList) {
                if (toRemove.remove(attr.getEntityId()) != null) {
                    AttributesSQL.update(attr, con);
                } else {
                    AttributesSQL.save(attr, con);
                }
            }
            // remove missing
            for (int entityId: toRemove.keySet()) {
                CustomLeveragePercentageAttr attr = new CustomLeveragePercentageAttr(entityId, 0);
                AttributesSQL.get(attr, con);
                attr.getAttributes().remove(CustomLeveragePercentage.BINARY_ATTR_NAME);
                AttributesSQL.update(attr, con);
            }
            commit(con);
        } catch (Throwable e) {
            rollback(con);
            throw new RemoteException(e.getLocalizedMessage(), e);
        } finally {
            releaseConnection(con);
        }
    }

    @Override
    public void save(CustomLeveragePercentage leveragePercentage, boolean removeNotIncluded) throws RemoteException {
        if(removeNotIncluded){
            save(leveragePercentage);
        }else{
            List<CustomLeveragePercentageAttr> attrList = leveragePercentage.toAttrList();
            Connection con = null;
            try {
                con = getConnection();
                //current set
                Hashtable<Integer, Attributes> toRemove = loadAllInternal(con);
                // save new / update existing
                for (CustomLeveragePercentageAttr attr : attrList) {
                    if (toRemove.remove(attr.getEntityId()) != null) {
                        AttributesSQL.update(attr, con);
                    } else {
                        AttributesSQL.save(attr, con);
                    }
                }
                commit(con);
            } catch (Throwable e) {
                rollback(con);
                throw new RemoteException(e.getLocalizedMessage(), e);
            } finally {
                releaseConnection(con);
            }
        }
    }
}
