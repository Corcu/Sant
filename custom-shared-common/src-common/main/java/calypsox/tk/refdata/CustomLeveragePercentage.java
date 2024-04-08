package calypsox.tk.refdata;

import com.calypso.tk.core.Attributes;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author x865229
 * date 02/01/2023
 * @see calypsox.tk.refdata.CustomLeveragePercentage
 */
public class CustomLeveragePercentage implements Serializable {
    private static final long serialVersionUID = 6069787023349266668L;

    public static final String BINARY_ATTR_NAME = "CustomLeverage";

    private final List<CustomLeveragePercentageItem> items = new ArrayList<>();

    public CustomLeveragePercentage() {
    }


    public void addPercentage(CustomLeveragePercentageItem item) {
        items.add(item);
    }

    public List<CustomLeveragePercentageItem> getItems() {
        return items;
    }

    public List<CustomLeveragePercentageAttr> toAttrList() {
        Map<Integer, List<CustomLeveragePercentageItem>> grouped = items.stream()
                .sorted(Comparator.comparingInt(CustomLeveragePercentageItem::getProductId))
                .collect(Collectors.groupingBy(CustomLeveragePercentageItem::getProductId));

        List<CustomLeveragePercentageAttr> result = new ArrayList<>();
        grouped.keySet().forEach(prodId -> {
            CustomLeveragePercentageAttr attr = new CustomLeveragePercentageAttr(prodId, 0);
            attr.getAttributes().add(BINARY_ATTR_NAME, grouped.get(prodId));
            result.add(attr);
        });

        return result;
    }

    public static CustomLeveragePercentage fromAttributes(Attributes attributes) {
        CustomLeveragePercentage result = new CustomLeveragePercentage();
        if (attributes != null) {
            List<CustomLeveragePercentageItem> itemList = attributes.get(BINARY_ATTR_NAME);
            if (itemList != null) {
                itemList.forEach(result::addPercentage);
            }
        }
        return result;
    }

    public static CustomLeveragePercentage fromAttributesMultiple(Collection<Attributes> attributesList) {
        CustomLeveragePercentage result = new CustomLeveragePercentage();
        if (attributesList != null) {
            attributesList.forEach(attributes -> {
                List<CustomLeveragePercentageItem> itemList = attributes.get(BINARY_ATTR_NAME);
                if (itemList != null) {
                    itemList.forEach(result::addPercentage);
                }
            });
        }
        return result;
    }

    public static class CustomLeveragePercentageItem implements Serializable {
        private static final long serialVersionUID = -3634520293965981964L;
        private int productId;
        private int legalEntityId;
        private double percentage;

        public static int ALL_ID = -1;

        public static int ERROR_ID = -2;

        public static String ALL_CODE = "ALL";


        public CustomLeveragePercentageItem(int productId, int legalEntityId, double percentage) {
            this.productId = productId;
            this.legalEntityId = legalEntityId;
            this.percentage = percentage;
        }

        public int getProductId() {
            return productId;
        }

        public void setProductId(int productId) {
            this.productId = productId;
        }

        public int getLegalEntityId() {
            return legalEntityId;
        }

        public void setLegalEntityId(int legalEntityId) {
            this.legalEntityId = legalEntityId;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }

}
