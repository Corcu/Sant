package calypsox.tk.lch.util;

import com.calypso.tk.core.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author acd
 */
public class LCHLogGenerator {

    String file = "";

    private static LCHLogGenerator lchLogGenerator = null;

    public LCHLogGenerator() {

    }

    public void initLog(String logRoute, String logFileName, String log){
        File directory = new File(logRoute);
        if(directory.exists()){
            try {
                this.file = logRoute+logFileName;
                Files.write(Paths.get(this.file), log.getBytes());
            } catch (IOException e) {
                Log.error(this,"Cannot wirte log." );
            }
        }
    }

    public void writeLog(String log){
        try {
            Files.write(Paths.get(this.file), log.getBytes());
        } catch (IOException e) {
            Log.error(this,"Cannot wirte log." + e);
        }

    }
}
