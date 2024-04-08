package calypsox.tk.export;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.util.InstantiateUtil;

/**
 * @author aalonsop
 */
public class UploaderXMLDataExporter extends com.calypso.tk.export.UploaderXMLDataExporter {
    public UploaderXMLDataExporter(DataExporterConfig exporterConfig) {
        super(exporterConfig);
    }

	@Override
    public void createUpddateBOMessage() {
        createAndSaveAdviceDocument();
        super.createUpddateBOMessage();
    }

    /**
     * Also used to set boMessage type
     * @param boMessage
     * @return
     */
    @Override
    protected boolean checkPendingMessage(BOMessage boMessage) {
        BOMessage boMess = this.getBoMessage();
        String messageType=Optional.ofNullable(getExporterConfig()).map(conf->conf.getProperty("messagetype")).map(Object::toString).orElse("");
        if (!Util.isEmpty(messageType)) {
            boMess.setMessageType(messageType);
        }
        
        linkBOMessageWithExportedIndex(boMess);
        this.setBoMessage(boMess);
        
        return super.checkPendingMessage(boMessage);
    }
    
    private void linkBOMessageWithExportedIndex(BOMessage boMessage) {
    	try {
    		Field f = getClass().getSuperclass().getSuperclass().getDeclaredField("exporterObject"); // Shit needed because field is private
    		f.setAccessible(true);
    		AbstractUploaderXMLDataExporter instanceClass = getExporterInstanceClass(f.get(this));
    		if (instanceClass != null) {
    			instanceClass.linkBOMessage(this.getCalypsoObject(), this.getBoMessage());
    		}
    	}	
    	catch (NoSuchFieldException e1) {
    	} 
    	catch (SecurityException e1) {
    	}
    	catch (IllegalArgumentException e) {
    	} 
    	catch (IllegalAccessException e) {
    	}
    }

    private void createAndSaveAdviceDocument() {
        try {
            if (this.getBoMessage()!=null&&this.getAdviceDocument(this.getBoMessage()) == null && !Util.isEmpty(this.getDataToSend())) {
                AdviceDocument adviceDocument = new AdviceDocument(this.getBoMessage(), new JDatetime(new Date()));
                adviceDocument.setDocument(new StringBuffer(this.getDataToSend()));
                DSConnection.getDefault().getRemoteBO().save(adviceDocument);
            }
        } catch (Exception exc) {
            Log.error(this, "Couldn't create or retrieve adviceDocument");
        }
    }
    
    private AbstractUploaderXMLDataExporter getExporterInstanceClass(Object sourceObject) {
    	AbstractUploaderXMLDataExporter instanceClass = null;
    	Class<?> targetClass = sourceObject.getClass();
    	StringBuilder className = new StringBuilder();
    	while (instanceClass == null && !targetClass.getSimpleName().equals("Object")) {
    		try {
    			className.setLength(0);
    			className.append(this.getClass().getPackage().getName());
    			className.append(".");
    			className.append(targetClass.getSimpleName());
    			className.append(this.getClass().getSimpleName());
    			instanceClass =  (AbstractUploaderXMLDataExporter)InstantiateUtil.getInstance(className.toString());
    		} catch (InstantiationException | IllegalAccessException e) {
//    			System.out.println(e.toString());
    		}
    		if (instanceClass == null) {
    			targetClass = targetClass.getSuperclass();
    		}
    	}
    	
    	return instanceClass;
    }

    @Override
    public CalypsoObject exportObject(Object sourceObject, Class<?> classType, String type) {
    	AbstractUploaderXMLDataExporter instanceClass = getExporterInstanceClass(sourceObject);
    	if (instanceClass != null) {
    		instanceClass.fillInfo(sourceObject, this, this.getBoMessage());
    	}
    	
        return super.exportObject(sourceObject, classType, type);
    }
    
    @Override
    public void exportObject(Object sourceObject){
    	AbstractUploaderXMLDataExporter instanceClass = getExporterInstanceClass(sourceObject);
    	
    	String data = null;
    	String objectIdentifier = "";
    	if (instanceClass != null) {
    		// Call first so that exporterObject is not null subsequently
        	super.exportObject(sourceObject);
        	// Reset Errors met during exporting unknown object
        	this.setErrors(new ArrayList<String>());
        	
    		data = instanceClass.export(sourceObject, this);
    		
    		objectIdentifier = instanceClass.getIdentifier(sourceObject);
    	}
    	
    	if (data != null) {
    		this.setDataToSend(data);
    		this.setFileName(instanceClass.getClass().getSimpleName() + "-" + objectIdentifier);
    	}
    	else {
    		super.exportObject(sourceObject);
    	}
    }
}
