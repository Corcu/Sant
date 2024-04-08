package com.santander.restservices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

public class UtilRestServices 
{
    private static Logger Log = LogManager.getLogger(UtilRestServices.class);

    public static Properties getPropertyFile(String nombreFichero)
    {
        Properties props = null;
        InputStream fichStr = null;

        try
        {
            /*
             * String scritturaHome = System.getProperty("scrittura.home");
             * 
             * File fichero = new File(scritturaHome + File.separator + "config" + File.separator + nombreFichero); fichStr = new FileInputStream(fichero);
             * props = new Properties(); props.load(fichStr);
             */
            props = ResourceLoader.getPropertiesFromClassPath(UtilRestServices.class, nombreFichero);
        }
        catch (Exception e)
        {
            Log.error("Error al leer el fichero property " + nombreFichero);
        }
        finally
        {
            try
            {
                if (fichStr != null)
                    fichStr.close();
            }
            catch (Exception ex)
            {
                Log.error("Error al cerrar el fichero property " + nombreFichero);
            }
        }

        return props;
    }
}
