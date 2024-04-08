package calypsox.tk.collateral.allocation.importer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.executor.ExternalAllocationLoggerFactory;
import calypsox.tk.collateral.allocation.executor.ExternalAllocationMapperFactory;
import calypsox.tk.collateral.allocation.executor.ExternalAllocationPersistorFactory;
import calypsox.tk.collateral.allocation.executor.ExternalAllocationReaderFactory;
import calypsox.tk.collateral.allocation.mapper.ExternalAllocationMapper;
import calypsox.tk.collateral.allocation.persistor.ExternalAllocationPersistor;
import calypsox.tk.collateral.allocation.reader.ExternalAllocationReader;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorLauncher;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;
import com.sun.xml.bind.StringInputStream;

/**
 * 
 * 
 * @author aela
 * 
 */
public class ExternalAllocationsImporter {

	protected ExternalAllocationImportContext context;

	protected ExternalAllocationReader allocationReader;

	protected ExternalAllocationMapper allocationMapper;

	protected ExternalAllocationPersistor allocationPersistor;
	
//noticeSubject.format(new String[] { "Collateral Position", mcc.getName(), po.getName() })
	//4140610|NACK|Error while processing trade: ISIN ES12345678 not found in the system
	//4140609|ACK|
	/**
	 * @param context
	 */
	public ExternalAllocationsImporter(ExternalAllocationImportContext context) {
		this.context = context;
	}

	public ExternalAllocationsImporter() {
	}

	/**
	 * 
	 * @param fileToProcess
	 * @return
	 */
	public boolean importFileAllocations(JDatetime processingDate,
			int numberMappers, int numberPersistors, int mapperQueueSize,
			int persistorQueueSize, final int persistorBufferSize,
			InputStream is) {

		if (context == null) {

			// PricingEnv pe = PricingEnv.loadPE("DirtyPrice", processingDate);

			final int calculationOffSet = ServiceRegistry.getDefaultContext()
					.getValueDateDays() * -1;
			final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
					processingDate.getJDate(TimeZone.getDefault()),
					DSConnection.getDefault().getUserDefaults().getHolidays(),
					calculationOffSet);

			context = new ExternalAllocationImportContext("|", false);
			try {
				context.init(processingDate, new JDatetime(valuatioDate, TimeZone.getDefault()),
						"DirtyPrice");
			}
			catch (Exception e) {
				Log.error(this, e);
				return false;
			}
		}

		// String path = "";
		// final String fullFileName = path + fileToProcess;
		BlockingQueue<ExternalAllocationBean> recordsList = new LinkedBlockingQueue<ExternalAllocationBean>(
				mapperQueueSize);
		BlockingQueue<MarginCallEntry> marginCallEntriesList = new LinkedBlockingQueue<MarginCallEntry>(
				persistorQueueSize);
		BlockingQueue<Task> loggingQueue = new LinkedBlockingQueue<Task>(
				persistorBufferSize);

		try {

			// start the file reader
			ProcessExecutorLauncher allocationsFileReaderExecutor = new DefaultProcessExecutorLauncher<Object, ExternalAllocationBean, Task>(
					1, null, recordsList, loggingQueue, context,null);
			allocationsFileReaderExecutor
					.setExecutorFactory(new ExternalAllocationReaderFactory(is));
			
			// start the mapper
			ProcessExecutorLauncher allocationsMapperExecutor = new DefaultProcessExecutorLauncher<ExternalAllocationBean, MarginCallEntry, Task>(
					numberMappers, recordsList, marginCallEntriesList,
					loggingQueue, this.context,Arrays.asList(allocationsFileReaderExecutor));

			allocationsMapperExecutor
					.setExecutorFactory(new ExternalAllocationMapperFactory());

			// start the persistor
			DefaultProcessExecutorLauncher<MarginCallEntry, Object, Task> allocationsPersistorExecutor = new DefaultProcessExecutorLauncher<MarginCallEntry, Object, Task>(
					numberPersistors, marginCallEntriesList, null,
					loggingQueue, this.context,Arrays.asList(allocationsMapperExecutor));

			allocationsPersistorExecutor
					.setExecutorFactory(new ExternalAllocationPersistorFactory());

			// start the logger
			DefaultProcessExecutorLauncher<Task, Object, Task> allocationsLoggerExecutor = new DefaultProcessExecutorLauncher<Task, Object, Task>(
					1, loggingQueue, null, loggingQueue, this.context,Arrays.asList(allocationsFileReaderExecutor,allocationsMapperExecutor,allocationsPersistorExecutor));
			allocationsLoggerExecutor
					.setExecutorFactory(new ExternalAllocationLoggerFactory());

			// start the process
			allocationsLoggerExecutor.start();
			allocationsFileReaderExecutor.start();
			allocationsMapperExecutor.start();
			allocationsPersistorExecutor.start();
			// initiate the shutdown process
			allocationsFileReaderExecutor.shutDown();
			allocationsMapperExecutor.shutDown();
			allocationsPersistorExecutor.shutDown();
			allocationsLoggerExecutor.shutDown();
		}
		catch (Exception e) {
			Log.error(this, e);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		try {
			DSConnection dsConDevCo4 = ConnectionUtil
					.connect(args, "MainEntry");

			// OptimAllocsImportContext context = new OptimAllocsImportContext(
			// "|", true);
			final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
					new JDatetime().getJDate(TimeZone.getDefault()),
					DSConnection.getDefault().getUserDefaults().getHolidays(),
					-1);
			// context.init(new JDatetime(), new JDatetime(valuatioDate),
			// "DirtyPrice");
			InputStream is = new StringInputStream(
					"NEW|MRXFI|414071|BSNY|BBIL|COLLAT_CASH|COLATMAD|19/08/2014|14/08/2014|23/12/2016|Borrower|10000|EUR|877.589703|EUR|19/09/2014|MRXFI|414071|ISIN|IT0005030504|102.3445|||||101.15408|||0||0||||||||||||1000|.07337|FOP|IsFinancement|");

			new ExternalAllocationsImporter().importFileAllocations(
					new JDatetime(), 10, 10, 100, 100, 100, is);
		}
		catch (Exception e) {
			Log.error(ExternalAllocationsImporter.class, e); //sonar
		}
		System.out.println("------");
		System.exit(0);
	}

}
