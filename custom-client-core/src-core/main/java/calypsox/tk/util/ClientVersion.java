package calypsox.tk.util;

import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.ClientVersionInterface;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * @author aalonsop
 */
public class ClientVersion implements ClientVersionInterface {
    private final Properties properties = new Properties();
    private static final String PROPERTIES_FILENAME = "clientversion.properties";
    private static final int PANEL_HEIGTH = 62;
    private static final int PANEL_WIDTH = 360;
    private static final String SANT_LOGO_PROPERTY = "SANTANDER_LOGO";

    /**
     *
     */
    public ClientVersion() {
        this.loadProperties();
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return Defaults.getEnvName();
    }

    /**
     * @return
     */
    @Override
    public String getVersion() {
        return getProperty("CUSTOM_CODE_VERSION");
    }

    /**
     * @return
     */
    @Override
    public String getVersionDate() {
        return getProperty("COMPILATION_DATE");
    }

    /**
     * @return
     */
    private String getClientLogo() {
        return getProperty(SANT_LOGO_PROPERTY);
    }

    /**
     * @param filename
     */
    private void loadProperties() {
        try {
            InputStream propsStream = this.getClass().getResourceAsStream(PROPERTIES_FILENAME);
            if (propsStream != null) {
                this.properties.load(propsStream);
            }
        } catch (IOException exc) {
            Log.error(this, "Error while loading clientversion.properties", exc);
        }

    }

    /**
     * @param propName
     * @return
     */
    private String getProperty(String propName) {
        String res = "";
        if (!Util.isEmpty(this.properties.getProperty(propName))) {
            res = this.properties.getProperty(propName);
        }
        return res;
    }

    /**
     * @return
     */
    public Component getGUIComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setSize(PANEL_WIDTH, PANEL_HEIGTH);
        panel.setOpaque(false);
        try {
            URL clientLogo = this.getClass().getResource(this.getClientLogo());
            if (clientLogo != null) {
                BufferedImage image = ImageIO.read(clientLogo);
                JLabel santLogo = new JLabel(new ImageIcon(image));
                panel.add(santLogo, "Center");
            }
        } catch (IOException exc) {
            Log.info(this, exc);
        }
        return panel;
    }
}

