package calypsox.tk.anacredit.api.copys;

import java.util.TreeMap;

public abstract class Record<T extends Enum> {

    private TreeMap<T , String> _parsedValues = new TreeMap<>();
    protected  TreeMap<String , Object> _keeper = new TreeMap<>();
    protected boolean _isOK  = true;


    protected abstract String parseInternal(T column, Object value);
    protected abstract void initializeDefaults();
    protected abstract void blankFields();

    public boolean isOK() {
        return _isOK ;
    }

    public Object getValue(T column) {
        Object value = _parsedValues.get(column);
        if (null == value) {
            return "";
        }
        return value;
    }


    public void keep(String label, Object value)  {
        _keeper.put(label, value);
    }

    public Object retrieve(String label)  {
        return _keeper.get(label);
    }

    public  boolean setValue(T column, Object value) {

        String parsedValue = parseInternal(column, value);
        if (null == parsedValue  || parsedValue.length() == 0) {
            System.out.println("Return value is empty : " + column.toString());
        } else {
            _parsedValues.put(column, parsedValue);
            return true;
        }
        return false;
    }



    
}
