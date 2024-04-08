package calypsox.tk.collateral.pdv.importer.processor;


import java.io.File;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.tk.util.interfaceImporter.ImportContext;
import calypsox.tk.util.interfaceImporter.InterfaceFileProcessor;

public class InterfacePDVFileProcessor extends InterfaceFileProcessor {

	public InterfacePDVFileProcessor(File f,
			BlockingQueue<InterfaceTradeBean> inWorkQueue,
			BlockingQueue<InterfaceTradeBean> outWorkQueue,
			ImportContext context, boolean useControlLine) {
		// set PDV flag
		super(f, inWorkQueue, outWorkQueue, context, useControlLine, true);
	}
}
