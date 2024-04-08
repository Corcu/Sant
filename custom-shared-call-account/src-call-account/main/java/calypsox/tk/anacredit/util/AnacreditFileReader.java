package calypsox.tk.anacredit.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

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

public class AnacreditFileReader {

    private static AnacreditFileReader jMinFileReader = null;
    private String external_Ref = "";
    private String j_minorista = "";

    public static synchronized AnacreditFileReader getInstance() {
        if (jMinFileReader == null) {
            return new AnacreditFileReader();
        } else {
            return jMinFileReader;
        }
    }

    public String splitLeft(String s) {
        external_Ref = "";
        for (int i = 0; i < s.length() / 2; i++) {
            external_Ref = external_Ref + s.charAt(i);
        }
        return external_Ref;
    }

    public String splitright(String s) {
        j_minorista = "";
        for (int i = s.length() / 2; i < s.length(); i++) {
            j_minorista = j_minorista + s.charAt(i);
        }
        return j_minorista;
    }

    public boolean checkLine(String line) {
        if ((splitLeft(line).contains("F") || splitLeft(line).contains("J"))
                && (splitright(line).contains("F") || splitright(line).contains("J"))) {
            return true;
        }
        return false;
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

    public List<AnacreditBean> readFile(InputStream fullPath) {
        ArrayList<AnacreditBean> beans = new ArrayList<>();
        try {

            //Read file and put on String list
            List<String> allLines = readInputStream(fullPath);

            if (!allLines.isEmpty()) {
                //This is needed to access the stream several times
                Supplier<Stream<String>> supplier = allLines::stream;

                //Read rest of lines from file
                final List<String> lines = supplier
                        .get().filter(l -> l.length() == 20)
                        .collect(Collectors.toList());

                //Create beans
                if (!lines.isEmpty()) {
                    for (String line : lines) {
                        if (checkLine(line)) {
                            AnacreditBean bean = new AnacreditBean(external_Ref, j_minorista);
                            beans.add(bean);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return beans;
    }

    public List<AnacreditBean> readFile(String filePath, String fileName, boolean move) {
        ArrayList<AnacreditBean> beans = new ArrayList<>();
        try {
            //Get File
            String file = getFile(filePath, fileName);

            //Read file and put on String list
            List<String> allLines = Files.readAllLines(Paths.get(filePath+file));

            if(move){
                moveToCopy(filePath,file);
            }
            if (!allLines.isEmpty()) {
                //This is needed to access the stream several times
                Supplier<Stream<String>> supplier = allLines::stream;

                //Read rest of lines from file
                final List<String> lines = supplier
                        .get().filter(l -> l.length() == 20)
                        .collect(Collectors.toList());

                //Create beans
                if (!lines.isEmpty()) {
                    for (String line : lines) {
                        if (checkLine(line)) {
                            AnacreditBean bean = new AnacreditBean(external_Ref, j_minorista);
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
    private String getFile(String filePath, String fileName) {
        String fileN = "";
        try {
            final Optional<Path> first = Files.list(Paths.get(filePath))
                    .filter(file -> file.getFileName().toString().contains(fileName))
                    .findFirst();
            if (first.isPresent()) {
                fileN = first.get().getFileName().toString();
            }
        } catch (IOException e) {
            Log.error(this, "Cannot get list of files form route: " + filePath);
        }
        return fileN;
    }

}
