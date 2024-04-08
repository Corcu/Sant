package calypsox.tk.report;

import com.calypso.tk.core.Product;

import java.util.HashMap;

public class PdvPriceCache {

    private HashMap<Product, HashMap<String,Double>> priceCache = new HashMap<Product,HashMap<String,Double>>();

    public void addPrice(Product sec, String type, Double price) {
        HashMap<String,Double>  priceHash = priceCache.get(sec);
        if(priceHash==null) {
            priceHash = new HashMap<String,Double>();
        }
        priceHash.put(type, price);
        priceCache.put(sec, priceHash);
    }


    public Double getPrice(Product sec, String type) {
        HashMap<String,Double>  priceHash = priceCache.get(sec);
        if(priceHash!=null) {
            return priceHash.get(type);
        }
        return null;
    }

}