package calypsox.tk.refdata;

import com.calypso.tk.core.Attributable;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.Auditable;
import com.calypso.tk.service.DSConnection;

import java.io.Serializable;
import java.util.Vector;

/**
 * @author x865229
 * date 02/01/2023
 * @see CustomLeveragePercentageAttr
 */
public class CustomLeveragePercentageAttr implements Serializable, Auditable, Attributable, Cloneable {
    private static final long serialVersionUID = -9037269757622861692L;

    public static final String ENTITY_TYPE = "Product";

    int entityId;
    int version;

    private Attributes attributes = new Attributes(this.getEntityType());

    public CustomLeveragePercentageAttr(int entityId, int version) {
        this.entityId = entityId;
        this.version = version;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Attributes a) {
        attributes = a;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void doAudit(Auditable other, Vector audits) {

    }

    @Override
    public void undo(DSConnection ds, AuditValue av) {

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            CustomLeveragePercentageAttr cloneMapping = (CustomLeveragePercentageAttr) super.clone();
            cloneMapping.attributes = this.attributes.clone();
            return cloneMapping;
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int i) {
        version = i;
    }

}
