package calypsox.tk.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Contatenation of PdV files and other reports send to SUSI
 */
public class ScheduledTaskSANT_CONCAT_REPORTS extends ScheduledTask {
    // TODO - Move it to common projects (7.2.1 blocks this action currently)
    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMdd");
    private static final String REPORT_ROOT = "Report Root";
    private static final String REPORT_INPUT_PREFIX = "Input Prefix";
    private static final String REPORT_OUTPUT_FILENAME = "Output Filename";
    private static final String REPORT_MOVE_TO_ARCHIVE = "Archive Files";
    private static final String REPORT_REMOVE_SOURCES = "Remove Source Files";


    private String _reportDirectory = "";

    public ScheduledTaskSANT_CONCAT_REPORTS() {
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        try {
            return concatenateFiles();
        } catch (Exception e) {
            Log.error(this, "Error collecting Reports to generate final file. ", e);
        }
       return false;
    }

    public boolean concatenateFiles() throws IOException {

        _reportDirectory = getReportDirectory();
        Log.system(Log.CALYPSOX, "searching for files in folder :" + _reportDirectory);

        String inputPrefix = this.getAttribute(REPORT_INPUT_PREFIX);
        String outputPrefix = this.getAttribute(REPORT_OUTPUT_FILENAME);

        return  walkThruPathAndCollect(_reportDirectory, inputPrefix, outputPrefix);
    }

    public boolean walkThruPathAndCollect(String outputFolder, String inputPrefix, String outputPrefix) throws IOException {

        SimpleDateFormat sf = new SimpleDateFormat(("yyyyMMdd"));
        String fileSufix =  "_" + sf.format(getValuationDatetime()) + ".csv";

        List<Path> touchedFiles = new ArrayList<>();
        // create master output
        File master = new File(DisplayInBrowser.getFileFullName("csv", outputFolder + outputPrefix));
        master.createNewFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master))) {
            Path root = Paths.get(master.getParent());
            Files.walkFileTree(root, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.toAbsolutePath().toString().contains("archive")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (dir.getParent().equals(root))  {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toFile().isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (file.toFile().getName().equals(master.getName())) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (file.toFile().getName().endsWith(fileSufix)) {
                        append(file.toFile(), bw);
                        touchedFiles.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });

            if (touchedFiles.isEmpty()) {
                Log.system(Log.CALYPSOX, "NO files found in directory :" + getReportDirectory());
            }


            String strMoveToArchive = getAttribute(REPORT_MOVE_TO_ARCHIVE);
            if (Boolean.valueOf(strMoveToArchive) == Boolean.TRUE) {
                archive(root , touchedFiles);
            }


            String strRemoveSources = getAttribute(REPORT_REMOVE_SOURCES);
            if (Boolean.valueOf(strRemoveSources) == Boolean.TRUE) {
                remove(root , touchedFiles);
            }

            touchedFiles.stream().forEach(file -> Log.system(Log.CALYPSOX, "files processed : " + file.getFileName()));

            return true;
        } catch (Exception exp) {
            Log.error(this, exp);
        }
        return false;
    }

    private void remove(Path root, List<Path> touchedFiles) {
        touchedFiles.stream().forEach(path -> {
            try {
                path.toFile().delete();
            } catch (Exception e) {
                Log.error(this, "Error removing source file :" +path.toString() , e);
            }
        });
    }

    private void archive(Path root, List<Path> touchedFiles) throws IOException {
        Path archivePath = Paths.get(root.toAbsolutePath() + "/archive");
        if (!archivePath.toFile().exists()) {
            // fallback
            Files.createDirectories(archivePath);
        }

        if (archivePath.toFile().exists()) {
            touchedFiles.stream().forEach(path ->{
                try {
                    JDateFormat format = new JDateFormat("yyyyMMdd");
                    String fileName = path.toFile().getName() + "_" + System.currentTimeMillis();
                    Path toFile  = Paths.get(archivePath +File.separator + fileName);
                    Files.move(path, toFile);
                } catch (IOException e) {
                    Log.error(this, "Error moving file to archive folder :" +path.toString() , e);
                }

            });
        };

    }

    private static void append(File file, BufferedWriter bw) throws IOException {
        Log.system(Log.CALYPSOX, "found :" + file.getPath());
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String text = null;
            while ((text = br.readLine()) != null) {
                if (!Util.isEmpty(text)
                         && !text.equalsIgnoreCase("No Records"))  {
                    bw.write(text);
                    bw.newLine();
                    bw.flush();
                }
            }
        } finally {
            //file.delete();
            //Log.system(Log.CALYPSOX, "file processed and removed successfully.");
        }
    }

    private String appendDateTime(JDatetime valDatetime, ScheduledTask st, String fileName) {
        String result = fileName;
        String date = Util.datetimeToString(valDatetime, "yyyyMMdd", st.getTimeZone() == null ? Calendar.getInstance().getTimeZone() : st.getTimeZone());
        result =  fileName + date;
        return result;

    }

    @Override
    public Vector getDomainAttributes() {
        Vector domainAttributes = super.getDomainAttributes();
        domainAttributes.add(REPORT_ROOT);
        domainAttributes.add(REPORT_INPUT_PREFIX);
        domainAttributes.add(REPORT_OUTPUT_FILENAME);
        domainAttributes.add(REPORT_MOVE_TO_ARCHIVE);
        domainAttributes.add(REPORT_REMOVE_SOURCES);
        return domainAttributes;
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(REPORT_MOVE_TO_ARCHIVE).booleanType());
        attributeList.add(attribute(REPORT_REMOVE_SOURCES).booleanType());
        return attributeList;
    }

    private String getReportDirectory() {
        String rootFolder = this.getAttribute(REPORT_ROOT);
         if (!rootFolder.endsWith("/")) {
             rootFolder = rootFolder.concat("/");
        }
         return rootFolder;
    }

    @Override
    public String getTaskInformation() {
        return "Search and concatenate reports Disponible RV";
    }


    public static void main(String[] args) {
        try {

            ScheduledTaskSANT_CONCAT_REPORTS st = new ScheduledTaskSANT_CONCAT_REPORTS();
            st.setAttribute(ScheduledTaskSANT_CONCAT_REPORTS.REPORT_ROOT, "c:\\tmp\\basura\\export\\");
            //CALYP_RV_TRI_POSITION_BILAT_ddMMyyyy.csv
            st.setAttribute(ScheduledTaskSANT_CONCAT_REPORTS.REPORT_INPUT_PREFIX , "CALYP_RV_TRI_POSITION");
            st.setAttribute(ScheduledTaskSANT_CONCAT_REPORTS.REPORT_OUTPUT_FILENAME, "CALYP_RV_TRI_POSITION.csv");
            st.setAttribute(ScheduledTaskSANT_CONCAT_REPORTS.REPORT_MOVE_TO_ARCHIVE, "true");
            st.setAttribute(ScheduledTaskSANT_CONCAT_REPORTS.REPORT_REMOVE_SOURCES, "true");
            st.setCurrentDate(JDate.getNow().addDays(-1));
            st.setDatetime(new JDatetime());
            st.concatenateFiles();


        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
