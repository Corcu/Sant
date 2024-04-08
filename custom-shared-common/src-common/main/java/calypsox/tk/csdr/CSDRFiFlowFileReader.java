package calypsox.tk.csdr;

import com.calypso.tk.core.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class CSDRFiFlowFileReader {

    final String filePath;

    public CSDRFiFlowFileReader(String filePath){
        this.filePath=filePath;
    }

    /**
     *
     * @return Mapped file lines without duplicates. Duplicate criteria is based on CSDRFiFlowLineBean equals() method.
     */
    public List<CSDRFiFlowLineBean> read(){
        Path path = Paths.get(filePath);

        List<CSDRFiFlowLineBean> mappedLines = new ArrayList<>();
        try {
            mappedLines = Files.lines(path).map(this::createLineBean)
                    .distinct().collect(Collectors.toList());
        } catch (IOException exc) {
            Log.error(this,exc);
        }
        return mappedLines;
    }

    private CSDRFiFlowLineBean createLineBean(String line){
        CSDRFiFlowLineBean lineBean=null;
        List<String> tokenizedLine=new ArrayList<>();
        int cropPosition=0;
        for(FileFixedWidthColumns column:FileFixedWidthColumns.values()){
            int endCropPosition=cropPosition+column.columnLenght;
            tokenizedLine.add(line.substring(cropPosition,endCropPosition).trim());
            cropPosition = endCropPosition;
        }
        if(tokenizedLine.size()==FileFixedWidthColumns.values().length){
            lineBean=new CSDRFiFlowLineBean(tokenizedLine);
        }
        return lineBean;
    }


    enum FileFixedWidthColumns{

        TRN(16),
        BOOK(15),
        TRADEID(20),
        FOTRADEID(20),
        XFERID(25),
        GLCS(6);

        int columnLenght;

        FileFixedWidthColumns(int columnLenght){
            this.columnLenght=columnLenght;
        }
    }
}
