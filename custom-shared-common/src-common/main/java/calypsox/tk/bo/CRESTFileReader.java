package calypsox.tk.bo;

import com.calypso.tk.bo.SWIFTFileReader;

import java.util.Map;
import java.util.Vector;

@SuppressWarnings("unused")
public class CRESTFileReader extends SWIFTFileReader {
    public CRESTFileReader() {
        super();
    }

    @Override
    public Vector<ExternalFile> readExternalFile(Map<String, ?> attributes) {
        return super.readExternalFile(attributes);
    }

    @Override
    protected ExternalFile readFile(String fileName, String dirName, String delimiter, boolean eofAsDelimiter) {
        return super.readFile(fileName, dirName, delimiter, eofAsDelimiter);
    }

    @Override
    protected ExternalFile createExternalFile(String fileName, String delimiter, Vector<String> fileLines) {
        return super.createExternalFile(fileName, delimiter, fileLines);
    }

    @Override
    protected ExternalFile createExternalFile(String fileName, String delimiter, Vector<String> fileLines, boolean eofDelimiter) {
        return super.createExternalFile(fileName, delimiter, fileLines, eofDelimiter);
    }
}
