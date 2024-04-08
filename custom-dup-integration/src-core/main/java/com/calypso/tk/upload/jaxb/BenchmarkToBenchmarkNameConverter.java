package com.calypso.tk.upload.jaxb;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import com.github.dozermapper.core.DozerConverter;

public class BenchmarkToBenchmarkNameConverter extends DozerConverter<String, Benchmark> {

	public BenchmarkToBenchmarkNameConverter() {
        super(String.class, Benchmark.class);
    }

	@Override
	public String convertFrom(Benchmark bench, String benchName) {
		if (bench == null) {
			return benchName;
		}
		
		return bench.getBenchmarkName();
	}

	@Override
	public Benchmark convertTo(String benchName, Benchmark bench) {
		if (Util.isEmpty(benchName)) {
			return bench;
		}
		
		if (bench == null) {
			bench = new Benchmark();
		}
		
		bench.setBenchmarkName(benchName);
		
		return bench;
	}

}
