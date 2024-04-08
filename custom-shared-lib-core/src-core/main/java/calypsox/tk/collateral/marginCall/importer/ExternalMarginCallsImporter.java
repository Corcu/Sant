package calypsox.tk.collateral.marginCall.importer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.executor.ExternalMarginCallLoggerFactory;
import calypsox.tk.collateral.marginCall.executor.ExternalMarginCallMapperFactory;
import calypsox.tk.collateral.marginCall.executor.ExternalMarginCallPersistorFactory;
import calypsox.tk.collateral.marginCall.executor.ExternalMarginCallReaderFactory;
import calypsox.tk.collateral.marginCall.mapper.ExternalMarginCallMapper;
import calypsox.tk.collateral.marginCall.persistor.ExternalMarginCallPersistor;
import calypsox.tk.collateral.marginCall.reader.ExternalMarginCallReader;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorLauncher;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;
import com.sun.xml.bind.StringInputStream;

public class ExternalMarginCallsImporter {

	protected ExternalMarginCallImportContext context;
	protected ExternalMarginCallReader mcReader;
	protected ExternalMarginCallMapper mcMapper;
	protected ExternalMarginCallPersistor mcPersistor;

	// Constructors
	public ExternalMarginCallsImporter() {
	}

	public ExternalMarginCallsImporter(ExternalMarginCallImportContext context) {
		this.context = context;
	}

	/**
	 * 
	 * @param
	 * @return
	 */
	public boolean importFileMarginCalls(JDatetime processingDate,
			int numberMappers, int numberPersistors, int mapperQueueSize,
			int persistorQueueSize, final int persistorBufferSize,
			InputStream is) {

		// TODO
		if (this.context == null) {

			final int calculationOffSet = ServiceRegistry.getDefaultContext()
					.getValueDateDays() * -1;
			final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
					processingDate.getJDate(TimeZone.getDefault()),
					DSConnection.getDefault().getUserDefaults().getHolidays(),
					calculationOffSet);

			this.context = new ExternalMarginCallImportContext("|", false);
			try {
				this.context.init(processingDate, new JDatetime(valuatioDate,
						TimeZone.getDefault()), "DirtyPrice");
			} catch (Exception e) {
				Log.error(this, e);
				return false;
			}
		}

		BlockingQueue<ExternalMarginCallBean> recordsList = new LinkedBlockingQueue<ExternalMarginCallBean>(
				mapperQueueSize);
		BlockingQueue<Trade> marginCallList = new LinkedBlockingQueue<Trade>(
				persistorQueueSize);
		BlockingQueue<Task> loggingQueue = new LinkedBlockingQueue<Task>(
				persistorBufferSize);

		try {

			// start the file reader
			ProcessExecutorLauncher marginCallFileReaderExecutor = new DefaultProcessExecutorLauncher<Object, ExternalMarginCallBean, Task>(
					1, null, recordsList, loggingQueue, this.context, null);
			marginCallFileReaderExecutor
					.setExecutorFactory(new ExternalMarginCallReaderFactory(is));

			// start the mapper
			ProcessExecutorLauncher marginCallMapperExecutor = new DefaultProcessExecutorLauncher<ExternalMarginCallBean, Trade, Task>(
					numberMappers, recordsList, marginCallList, loggingQueue,
					this.context, Arrays.asList(marginCallFileReaderExecutor));
			marginCallMapperExecutor
					.setExecutorFactory(new ExternalMarginCallMapperFactory());

			// start the persistor
			DefaultProcessExecutorLauncher<Trade, Object, Task> marginCallPersistorExecutor = new DefaultProcessExecutorLauncher<Trade, Object, Task>(
					numberPersistors, marginCallList, null, loggingQueue,
					this.context, Arrays.asList(marginCallMapperExecutor));
			marginCallPersistorExecutor
					.setExecutorFactory(new ExternalMarginCallPersistorFactory(
							persistorBufferSize));

			// start the logger
			DefaultProcessExecutorLauncher<Task, Object, Task> marginCallsLoggerExecutor = new DefaultProcessExecutorLauncher<Task, Object, Task>(
					1, loggingQueue, null, loggingQueue, this.context,
					Arrays.asList(marginCallFileReaderExecutor,
							marginCallMapperExecutor));
			marginCallsLoggerExecutor
					.setExecutorFactory(new ExternalMarginCallLoggerFactory());

			// start the process
			marginCallFileReaderExecutor.start();
			marginCallMapperExecutor.start();
			marginCallPersistorExecutor.start();
			marginCallsLoggerExecutor.start();
			// initiate the shutdown process
			marginCallFileReaderExecutor.shutDown();
			marginCallMapperExecutor.shutDown();
			marginCallPersistorExecutor.shutDown();
			marginCallsLoggerExecutor.shutDown();

		} catch (Exception e) {
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

			final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
					new JDatetime().getJDate(TimeZone.getDefault()),
					DSConnection.getDefault().getUserDefaults().getHolidays(),
					-1);
			InputStream is = new StringInputStream(
					"NEW|MRXFI|647919|1067064|BSTE|SGAR|COLLAT_SECURITY|ESSFCOLATTIT|27/03/2014|27/03/2014|Loan|10700|EUR|ISIN|ES00000120I0|100");

			new ExternalMarginCallsImporter().importFileMarginCalls(
					new JDatetime(), 10, 10, 100, 100, 1, is);
		} catch (Exception e) {
			Log.error(ExternalMarginCallsImporter.class, e); // sonar
		}
		System.out.println("------");
		System.exit(0);
	}

}
