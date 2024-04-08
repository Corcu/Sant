package calypsox.tk.anacredit.util.acesscontrol;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnacreditFileACReader {
    private static AnacreditFileACReader _instance = null;
    private final Layout RENTA_VARIABLE = new RentaVariable();
    private final Layout RENTA_FIJA  = new RentaFija();

    public static synchronized AnacreditFileACReader getInstance() {
        if (_instance == null) {
            return new AnacreditFileACReader();
        } else {
            return _instance;
        }
    }

    public List<String> readInputStream(InputStream fullPath) throws IOException {

        BufferedReader bf = new BufferedReader(new InputStreamReader(fullPath));
        List<String> allLines = new ArrayList<>();
        String fila;
        if (null != bf) {
            while ((fila=bf.readLine()) != null) {
                allLines.add(fila);
            }
        }
        bf.close();
        return allLines;
    }
    public List<AnacreditACBean> readFileRV(InputStream fullPath, Layout layout) {
        return readFile(fullPath, RENTA_VARIABLE);
    }
    public List<AnacreditACBean> readFileRF(InputStream fullPath, Layout layout) {
        return readFile(fullPath, RENTA_FIJA);
    }
    public List<AnacreditACBean> readFileRV(String filePath, String fileName, boolean move) {
        return readFile(filePath, fileName, move, RENTA_VARIABLE);
    }
    public List<AnacreditACBean> readFileRF(String filePath, String fileName, boolean move) {
        return readFile(filePath, fileName, move, RENTA_FIJA);
    }

    public List<AnacreditACBean> readFile(InputStream fullPath, Layout layout) {
        ArrayList<AnacreditACBean> beans = new ArrayList<>();
        try {
            //Read file and put on String list
            List<String> allLines = readInputStream(fullPath);
            if (!allLines.isEmpty()) {
                //This is needed to access the stream several times
                Supplier<Stream<String>> supplier = allLines::stream;

                //Read rest of lines from file
                final List<String> lines = supplier
                        .get().filter(l -> l.length() == layout.getRecordLength())
                        .collect(Collectors.toList());
               //Create beans
                if (!lines.isEmpty()) {
                    for (String line : lines) {
                        int recNum = -1;
                            AnacreditACBean bean = buildBean(recNum++, line, RENTA_VARIABLE);
                            if (bean != null){
                                beans.add(bean);
                            }
                    }
                }
            }
        } catch (Exception e) {
        }
        return beans;
    }


    private AnacreditACBean buildBean(int recNum, String line, Layout layout) {
        String[] cols = line.split(";");
        String isin = cols[layout.getIdxSIN()];
        if (12 == isin.length()) {
            // we have a good ISIN
            if (!Util.isEmpty(cols[layout.getIdxSIN()])) {
                // cotiza viene
                String cotiza = cols[layout.getIdxCotiza()];
                if (!Util.isEmpty(cols[layout.getIdxJerarquia()])) {
                    String jearquia = cols[layout.getIdxJerarquia()];
                    AnacreditACBean bean = new AnacreditACBean(layout.getType(), recNum, isin, cotiza, jearquia);
                    return bean;
                }
            }
        }
        return null;
    }

    private List<AnacreditACBean> readFile(String filePath, String fileName, boolean move, Layout layout ) {
        ArrayList<AnacreditACBean> beans = new ArrayList<>();
        try {
            //Get File
            Path file = getFile(filePath, fileName);

            //Read file and put on String list
            List<String> allLines = Files.readAllLines(file);

            if(move){
                moveToCopy(filePath,file.getFileName().toString());
            }
            if (!allLines.isEmpty()) {
                //This is needed to access the stream several times
                Supplier<Stream<String>> supplier = allLines::stream;

                //Read rest of lines from file
                final List<String> lines = supplier
                        .get().filter(l -> l.length() == layout.getRecordLength())
                        .collect(Collectors.toList());

                //Create beans
                if (!lines.isEmpty()) {
                    int recNum = -1;
                    for (String line : lines) {

                        AnacreditACBean bean = buildBean(recNum++, line, layout );
                        if (bean != null){
                            beans.add(bean);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return beans;
    }

    /**
     * Move file from import directory to .../import/copy/
     *
     * @param filePath
     * @param fileName
     */
    private void moveToCopy(String filePath, String fileName) {
        //copy route
        String copyRute = filePath + "copy/";
        try {
            //Get procces date
            JDateFormat format = new JDateFormat("ddMMyyyy");
            String date = format.format(JDate.getNow());

            File fileToMove = new File(filePath + fileName);
            fileName += "_" + date;
            File directory = new File(copyRute);
            if (directory.exists()) {
                fileToMove.renameTo(new File(copyRute + fileName));
            }
        } catch (Exception e) {
            Log.error(this, "Cannot move file: " + fileName + " to copy directory.");
        }
    }

    /**
     * Get list of files from directory and return the first file with contains te fileName
     *
     * @param filePath
     * @param fileName
     * @return
     */

    private Path getFile(String filePath, String fileName) {
        String defaultDir = Util.getUserDir();
        String fileN = "";
        Path result = null;
        Path pathToFile = Paths.get(filePath);

        if (!pathToFile.toFile().exists())  {
            pathToFile = Paths.get(Util.getUserDir() + File.separator + filePath);
        }

        try {
            final Optional<Path> first = Files.list(pathToFile)
                    .filter(file -> file.getFileName().toString().contains(fileName))
                    .findFirst();
            if (first.isPresent()) {
                result  = first.get();
            }
        } catch (IOException e) {
            Log.error(this, "Cannot get list of files form route: " + filePath);
        }
        return result;
    }


    interface Layout {
        String getType();
        int getIdxSIN();
        int getIdxCotiza();
        int getIdxJerarquia();
        int getRecordLength();
    }
    class RentaVariable implements Layout {
        @Override
        public String getType() { return "RV";}
        @Override
        public int getIdxSIN() { return 1;}
        @Override
        public int getIdxCotiza() { return 3;}
        @Override
        public int getIdxJerarquia() {return 4;}
        @Override
        public int getRecordLength() {return 36;}
    }
    class RentaFija implements Layout {
        @Override
        public String getType() { return "RF";}
        @Override
        public int getIdxSIN() { return 1;}
        @Override
        public int getIdxCotiza() { return 2;}
        @Override
        public int getIdxJerarquia() {return 3;}
        @Override
        public int getRecordLength() {return 25;}
    }

}
