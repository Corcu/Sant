package calypsox.engine.dataimport.BondElements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PutCallDate;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.flowDefinition.FdnCashFlowDefinition;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.ConnectionUtil;

import calypsox.tk.core.CollateralStaticAttributes;

public class ImportBondElements {
	
    protected static final String IMPORT_DATE_FORMAT = "dd/MM/yyyy";
    
    public Exception importPoolFactor(ListaPoolFactor listaPoolFactor, Bond bond) {
    	Exception exception = null;
    	
    	// Schedule should always be specified in percent
    	String notionalType = bond.getNotionalType();
    	if (!"Notional Percent".equals(notionalType)) {
    		exception = new Exception("The Notional Type of the Bond is not Notional Percent : " + notionalType);
    		Log.error(this, exception.getMessage());
			return exception;
    	}
    	
    	Vector<NotionalDate> amortSchedule = (Vector<NotionalDate>)bond.getAmortSchedule();
    	
		List<PoolFactor> poolfactorList = listaPoolFactor.getPoolFactor();
		for (int i = 0; i < poolfactorList.size(); i++) {
			PoolFactor poolFactor = poolfactorList.get(i);

			double poolFactorValue = poolFactor.getPoolFactorValue().doubleValue() * 100.0;
			
			String poolFactorDateS = poolFactor.getPoolFactorDate();
			JDate poolFactorDate = null;
			try {
				poolFactorDate = JDate.valueOf(new SimpleDateFormat(IMPORT_DATE_FORMAT).parse(poolFactorDateS));
			} catch (ParseException e) {
				exception = e;
				Log.error(this, "Could not parse date " + poolFactorDateS +  " with format " + IMPORT_DATE_FORMAT);
				break;
			}
			
			boolean alreadyExistingDate = false;
			for (int j = 0; j < amortSchedule.size(); j++) {
				NotionalDate existingDate = amortSchedule.get(j);
				if (existingDate.getStartDate().equals(poolFactorDate)) {
					existingDate.setNotionalAmt(poolFactorValue);
					alreadyExistingDate = true;
					break;
				}
			}
			
			if (!alreadyExistingDate) {
				NotionalDate newDate = new NotionalDate(poolFactorDate, poolFactorValue);
				amortSchedule.add(newDate);
			}
		}
    	
    	return exception;
    }
    
    public Exception importCallSchedule(ListaCall listaCall, Bond bond) {
    	Exception exception = null;
    	
    	Vector schedule = bond.getSchedule();
    	
    	boolean callisPartial = false;
    	List<FecCall> fecCallList = listaCall.getFecCall();
    	for (int i = 0; i < fecCallList.size(); i++) {
    		FecCall fecCall = fecCallList.get(i); 

    		String fechaCallS = fecCall.getFechaCall();
    		String fechaCallFinS = fecCall.getFechaCallFin();
    		String fechaEjercicioS = fecCall.getFechaEjercicio();
    		JDate fechaCall = null;
    		JDate fechaCallFin = null;
    		JDate fechaEjercicio = null;

    		try {
    			fechaCall = JDate.valueOf(new SimpleDateFormat(IMPORT_DATE_FORMAT).parse(fechaCallS));
    			fechaCallFin = JDate.valueOf(new SimpleDateFormat(IMPORT_DATE_FORMAT).parse(fechaCallFinS));
    			if (!Util.isEmpty(fechaEjercicioS)) {
    				fechaEjercicio = JDate.valueOf(new SimpleDateFormat(IMPORT_DATE_FORMAT).parse(fechaEjercicioS));
    			}
    		} catch (ParseException e) {
    			exception = e;
    			Log.error(this, "Could not parse date with format " + IMPORT_DATE_FORMAT);
    			break;
    		}
    		
    		double percentageAmort = Math.max(fecCall.getPorcentajeNomOp(), fecCall.getFacAmortOp() * 100.0);
    		// It already happened that from AC they would send 100 instead of 1 in FacAmort...
    		percentageAmort = Math.min(100.0, percentageAmort);
    		
			PutCallDate call = bond.getDefaultPutCallDate();
			call.setInterestCleanupB(true);
			call.setDeliveryDate(fechaCallFin);
			call.setExpiryDate(fechaCall);
			if (fechaEjercicio != null) {
				call.setFirstExerciseDate(fechaEjercicio);
				call.setIsExercised(true);
				if (percentageAmort < 100.0) {
					CashFlowSet cfs;
					double priorNotional = 0.0;
					try {
						cfs = bond.getFlows(fechaEjercicio);

						for (int j = 0; j < cfs.size(); j++) {
							CashFlow cf = cfs.get(j);
							if (cf.getType().equals("INTEREST")) {
								CashFlowCoupon cfc = (CashFlowCoupon)cf;
								if (cfc.getEndDate().gte(fechaEjercicio)) {
									priorNotional = cfc.getNotional();
									break;
								}
							}
						}
					} catch (FlowGenerationException e) {
						priorNotional = bond.getTotalIssued();
					}
					
					call.setRedemptionAmount(priorNotional * percentageAmort / 100.0);
					callisPartial = true;
				}
			}
			call.setPrice(fecCall.getStrikePriceCall());
			
			for (int j = 0; j < schedule.size(); j++) {
				Object currentSchedule = schedule.get(j);
				if (currentSchedule instanceof PutCallDate && ((PutCallDate) currentSchedule).getExpiryDate().equals(fechaCall)) {
					schedule.remove(j);
				}
			}

			schedule.add(call);
    	}
    	
    	if (callisPartial) {
    		bond.setAllowedRedemptionType("Full and Partial");
    	}
    	
    	return exception;
    }
    
    public Exception importBondElements(String xml) {
    	Exception exception = null;
    	
    	RemoteProduct remoteProduct = DSConnection.getDefault().getRemoteProduct();
    	if (remoteProduct == null) {
    		exception = new Exception("Could not get Remote Product.");
    		Log.error(this, exception.getMessage());
			return exception;
    	}
    	
    	
    	JAXBContext context;
    	RDFlowTransaction rdFlowTransaction = null;
    	
    	try {
    		context = JAXBContext.newInstance(RDFlowTransaction.class);
    	
    		Unmarshaller unMarshaller = context.createUnmarshaller();
			InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
			
			rdFlowTransaction = (RDFlowTransaction) unMarshaller.unmarshal(stream);
			
			if (rdFlowTransaction != null) {
				ServicePoolFactor servicePoolFactor = rdFlowTransaction.getServicePoolFactor();
				ServiceOptionExercise serviceOptionExercise = rdFlowTransaction.getServiceOptionExercise();
				Product product = remoteProduct.getProductByCode(CollateralStaticAttributes.BOND_SEC_CODE_REF_INTERNA, rdFlowTransaction.getSymbol());
				
				if (product != null && product instanceof Bond) {
					if (servicePoolFactor != null) {
						exception = importPoolFactor(servicePoolFactor.getListaPoolFactor(), (Bond)product);
					}
					if (serviceOptionExercise != null) {
						exception = importCallSchedule(serviceOptionExercise.getListaCall(), (Bond)product);
					}

					if (exception == null) {
						remoteProduct.saveBond((Bond)product, false);
					}
				}
				else {
					exception = new Exception("Could not get Bond Definition with ID " + rdFlowTransaction.getSymbol());
		    		Log.error(this, exception.getMessage());
				}
			}
			else {
				exception = new Exception("RDFlowTransaction is null");
	    		Log.error(this, exception.getMessage());
			}
		} catch (JAXBException e) {
			Log.error(this, "Could not convert Bond Element to Object: " + e.toString());
			exception = e;
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error retrieving Bond Product: " + e.toString());
			exception = e;
		}
        
    	return exception;
    }

    public static void main(String[] args) throws Exception {
//        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n" + 
//        		"<RDFlowXML>\n" + 
//        		"    <RDFlowTransaction>\n" + 
//        		"        <transactionKey>152027</transactionKey>\n" + 
//        		"        <action>NEW</action>\n" + 
//        		"        <symbol>C0.RD.LST.14507</symbol>\n" + 
//        		"        <listSubscribers>\n" + 
//        		"            <subscriber>ESTRUCTURALES</subscriber>\n" + 
//        		"        </listSubscribers>\n" + 
//        		"        <servicePoolFactor>\n" + 
//        		"            <listaPoolFactor>\n" + 
//        		"                <poolFactor>\n" + 
//        		"                    <poolFactorValue>0.6667</poolFactorValue>\n" + 
//        		"                    <poolFactorDate>20/04/2018</poolFactorDate>\n" + 
//        		"                    <couponRedemptionPaymentPF>RC</couponRedemptionPaymentPF>\n" + 
//        		"                </poolFactor>\n" + 
//        		"                <poolFactor>\n" + 
//        		"                    <poolFactorValue>0.7776</poolFactorValue>\n" + 
//        		"                    <poolFactorDate>20/04/2020</poolFactorDate>\n" + 
//        		"                    <couponRedemptionPaymentPF>RC</couponRedemptionPaymentPF>\n" + 
//        		"                </poolFactor>\n" +
//        		"                <poolFactor>\n" + 
//        		"                    <poolFactorValue>0.8889</poolFactorValue>\n" + 
//        		"                    <poolFactorDate>20/04/2021</poolFactorDate>\n" + 
//        		"                    <couponRedemptionPaymentPF>RC</couponRedemptionPaymentPF>\n" + 
//        		"                </poolFactor>\n" + 
//        		"            </listaPoolFactor>\n" + 
//        		"        </servicePoolFactor>\n" + 
//        		"    </RDFlowTransaction>\n" + 
//        		"</RDFlowXML>\n";
        
    	
    	
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n" + 
        		"<RDFlowXML>\n" + 
        		"    <RDFlowTransaction>\n" + 
        		"        <transactionKey>152048</transactionKey>\n" + 
        		"        <action>NEW</action>\n" + 
        		"        <symbol>C0.RD.LST.14507</symbol>\n" + 
        		"        <listSubscribers>\n" + 
        		"            <subscriber>ESTRUCTURALES</subscriber>\n" + 
        		"        </listSubscribers>\n" + 
        		"        <serviceOptionExercise>\n" + 
        		"            <divisaLiquid>USD</divisaLiquid>\n" + 
        		"            <formaEjercicioOp>PERCENTAGE</formaEjercicioOp>\n" + 
        		"            <listaCall>\n" + 
        		"                <fecCall>\n" + 
        		"                        <fechaCall>17/03/2022</fechaCall>\r\n" + 
        		"                        <fechaCallFin>17/03/2023</fechaCallFin>\r\n" + 
        		"                        <fechaEjercicio></fechaEjercicio>\r\n" + 
        		"                        <strikePriceCall>100</strikePriceCall>\r\n" + 
        		"                        <ejercicioOp>FULL OR PART</ejercicioOp>\r\n" + 
        		"                        <facAmortOp></facAmortOp>\r\n" + 
        		"                        <porcentajeNomOp></porcentajeNomOp>\r\n" + 
        		"                        <cantidadAmortOp></cantidadAmortOp>\r\n" + 
        		"                        <numTitulosCall></numTitulosCall>\r\n" + 
        		"                        <couponRedemptionPaymentCall>RC</couponRedemptionPaymentCall>" +
        		"                </fecCall>\n" + 
        		"            </listaCall>\n" + 
        		"        </serviceOptionExercise>\n" + 
        		"    </RDFlowTransaction>\n" + 
        		"</RDFlowXML>\n" ;
    	
//        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n" + 
//        		"<RDFlowXML>\n" + 
//        		"    <RDFlowTransaction>\n" + 
//        		"        <transactionKey>152048</transactionKey>\n" + 
//        		"        <action>NEW</action>\n" + 
//        		"        <symbol>C0.RD.LST.14507</symbol>\n" + 
//        		"        <listSubscribers>\n" + 
//        		"            <subscriber>ESTRUCTURALES</subscriber>\n" + 
//        		"        </listSubscribers>\n" + 
//        		"        <serviceOptionExercise>\n" + 
//        		"            <divisaLiquid>USD</divisaLiquid>\n" + 
//        		"            <formaEjercicioOp>PERCENTAGE</formaEjercicioOp>\n" + 
//        		"            <listaCall>\n" + 
//        		"                <fecCall>\n" + 
//        		"                    <fechaCall>17/03/2022</fechaCall>\n" + 
//        		"                    <fechaCallFin>17/03/2023</fechaCallFin>\n" + 
//        		"                    <fechaEjercicio>17/03/2022</fechaEjercicio>\n" + 
//        		"                    <strikePriceCall>100</strikePriceCall>\n" + 
//        		"                    <ejercicioOp>FULL OR PART</ejercicioOp>\n" + 
//        		"                    <facAmortOp></facAmortOp>\n" + 
//        		"                    <porcentajeNomOp>100</porcentajeNomOp>\n" + 
//        		"                    <cantidadAmortOp></cantidadAmortOp>\n" + 
//        		"                    <numTitulosCall></numTitulosCall>\n" + 
//        		"                    <couponRedemptionPaymentCall>RC</couponRedemptionPaymentCall>\n" + 
//        		"                </fecCall>\n" + 
//        		"            </listaCall>\n" + 
//        		"        </serviceOptionExercise>\n" + 
//        		"    </RDFlowTransaction>\n" + 
//        		"</RDFlowXML>\n" ;
        
        
//String xml ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
//		"<calypso:calypsoDocument xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:calypso=\"http://www.calypso.com/xml\">\n" + 
//		"  <calypso:calypsoObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"calypso:Bond\" version=\"12-0\" action=\"SAVE\">\n" + 
//		"    <calypso:definition></calypso:definition>\n" + 
//		"  </calypso:calypsoObject>\n" + 
//		"</calypso:calypsoDocument>";
        
		ConnectionUtil.connect("phil", "phil", "Navigator", "COLv16DEV8_CERTA");

		ImportBondElements dummy = new ImportBondElements();
		dummy.importBondElements(xml);
        
        Log.system(null, "");
    }

}
