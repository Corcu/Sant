package com.calypso.tk.collateral.trade;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.trade.impl.DefaultTradeLoaderFilter;
import com.calypso.tk.core.Product;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class NonFwdDefaultTradeLoaderFilter extends DefaultTradeLoaderFilter {


    public NonFwdDefaultTradeLoaderFilter(List<MarginCallEntry> entries) {
        super(entries);
    }

    @Override
    public Set<String> getProductList() {
        Set<String> nonFwdProductList= super.getProductList().stream()
                    .filter(type->!type.contains(Product.BOND))
                    .collect(Collectors.toSet());
        if(nonFwdProductList.isEmpty()){
            nonFwdProductList.add("NONE");
        }
        return nonFwdProductList;
    }
}
