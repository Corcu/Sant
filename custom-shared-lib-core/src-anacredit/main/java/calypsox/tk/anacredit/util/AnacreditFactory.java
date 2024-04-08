package calypsox.tk.anacredit.util;

import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.formatter.IOperacionesFormatter;
import calypsox.tk.anacredit.formatter.IPersonaFormatter;
import calypsox.tk.anacredit.loader.AnacreditLoader;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.InstantiateUtil;

import java.util.HashMap;

public class AnacreditFactory {

    private static AnacreditFactory _instance;

    public static AnacreditFactory instance() {
        if (_instance ==null) {
            _instance = new AnacreditFactory();
        }
        return _instance;
    }

    public AnacreditLoader getLoader(String type) {
        String className = "tk.anacredit.loader.AnacreditLoader" + type;
        try {
            AnacreditLoader loader = (AnacreditLoader) InstantiateUtil.getInstance(className, true);
            return loader;
        } catch (Exception e) {
            Log.info(this, className + " not found.");
        }
        return null;

    }

    private HashMap<String, AnacreditFormatter> _fmtCash = new HashMap<>();

    public IOperacionesFormatter getOperacionesFormatter(String type) {
        AnacreditFormatter fmt = getFormatter(type);
        if (fmt instanceof IOperacionesFormatter) {
            return (IOperacionesFormatter) fmt;
        }
        return null;
    }

    public IPersonaFormatter getPersonaFormatter(String type) {
        AnacreditFormatter fmt = getFormatter(type);
        if (fmt instanceof IPersonaFormatter) {
            return (IPersonaFormatter) fmt;
        }
        return null;

    }


    private  AnacreditFormatter getFormatter(String type) {

        if (_fmtCash.containsKey(type)) {
            return _fmtCash.get(type);
        }

        String className = "tk.anacredit.formatter.AnacreditFormatter" + type;
        try {
            AnacreditFormatter formatter = (AnacreditFormatter) InstantiateUtil.getInstance(className, true);
            synchronized (_fmtCash) {
                _fmtCash.put(type, formatter);
            }
          } catch (Exception e) {
            Log.error(this, className + " not found.");
          }

        return _fmtCash.get(type);

    }
}
