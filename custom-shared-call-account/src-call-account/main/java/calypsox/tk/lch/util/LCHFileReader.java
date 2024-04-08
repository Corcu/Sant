package calypsox.tk.lch.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author acd
 */
public class LCHFileReader {

    private static LCHFileReader lchFileReader = null;
    private static final String LCH_DELIMITER = "\t";

    public static synchronized LCHFileReader getInstance(){
            if(lchFileReader==null){
                return new LCHFileReader();
            }else{
                return lchFileReader;
            }
    }

    /**
     *This method read any file from LCH delimit by "\t"
     * Return list of HashMap - <ColumnName,Value>
     *
     * @param @{@link String filePath}
     *
     */
    public List<HashMap<String,String>> readFile(String filePath,String fileName,Boolean move){
        List<HashMap<String,String>> beans = new ArrayList<>();

        try {
            //Get File
            String file = getFile(filePath,fileName);

            if(!Util.isEmpty(file)){
                //Read file and put on String list
                List<String> allLines = Files.readAllLines(Paths.get(filePath+file));

                //Move to copy folder after read file
                if(move){
                    moveToCopy(filePath,file);
                }

                if(!Util.isEmpty(allLines)){
                    //This is needed to access the stream several times
                    Supplier<Stream<String>> supplier = allLines::stream;

                    //Get headers form file
                    final List<String> headers = Arrays.stream(supplier.get().findFirst()
                            .filter(l -> l.contains(LCH_DELIMITER))
                            .map(line -> line.split(LCH_DELIMITER))
                            .orElseThrow(Exception::new))
                            .collect(Collectors.toList());

                    //Read rest of lines from file
                    final List<String[]> lines = supplier
                            .get().skip(1) //Skip the headers line
                            .filter(l -> l.contains(LCH_DELIMITER))
                            .map(line -> line.split(LCH_DELIMITER))
                            .collect(Collectors.toList());

                    //Create beans
                    if(!Util.isEmpty(lines) && !Util.isEmpty(headers)){
                        for(String[] line : lines){
                            HashMap<String,String> bean = new HashMap<>();
                            for(int i = 0;i<=headers.size();i++){
                                if(i<line.length){
                                    bean.put(headers.get(i), line[i]);
                                }
                            }
                            beans.add(bean);
                        }
                    }
                }
            }else{
                Log.system(LCHFileReader.class.getName(),"File " +  fileName + " not found.");
            }

        } catch (Exception e) {
            Log.error(LCHFileReader.class.getName(),e);
        }

        return beans;
    }

    /**
     * Move file from import directory to .../import/copy/
     * @param filePath
     * @param fileName
     */
    private void moveToCopy(String filePath,String fileName){
        //copy route
        String copyRute = filePath+"copy/";
        try{
            //Get procces date
            JDateFormat format = new JDateFormat("ddMMyyyy");
            String date = format.format(JDate.getNow());

            File fileToMove = new File(filePath+fileName);
            fileName += "_"+date;
            File directory = new File(copyRute);
            if(directory.exists()){
                fileToMove.renameTo(new File(copyRute+fileName));
            }
        }catch (Exception e){
            Log.error(this,"Cannot move file: " + fileName + " to copy directory.");
        }
    }


    /**
     * Get list of files from directory and return the first file with contains te fileName
     * @param filePath
     * @param fileName
     * @return
     */
    private String getFile(String filePath,String fileName){
        String fileN = "";
        try {
            final Optional<Path> first = Files.list(Paths.get(filePath))
                    .filter(file -> file.getFileName().toString().contains(fileName))
                    .findFirst();
            if(first.isPresent()){
                fileN = first.get().getFileName().toString();
            }
        } catch (IOException e) {
            Log.error(this,"Cannot get list of files form route: " + filePath);
        }
        return fileN;
    }


}
