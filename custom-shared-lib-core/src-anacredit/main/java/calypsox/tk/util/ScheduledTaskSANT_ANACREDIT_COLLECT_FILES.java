package calypsox.tk.util;

import calypsox.tk.anacredit.util.AnacreditFilenameUtil;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduledTaskSANT_ANACREDIT_COLLECT_FILES extends ScheduledTask {

    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMdd");
    private static final String REPORT_DIRECTORY = "Report Directory";
    private static final String SEPARATOR = "_";

    private String _reportDirectory = "";
    private String _executionSufixLabel  = "";

    public ScheduledTaskSANT_ANACREDIT_COLLECT_FILES() {
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        try {
            return concatenateFiles();
        } catch (Exception e) {
            Log.error(this, "Error collecting ANACREDIT Reports to generate final files. ", e);
        }
        return false;
    }

    public boolean concatenateFiles() throws IOException {

        _reportDirectory = getReportDirectory();
        _executionSufixLabel =  new SimpleDateFormat("yyyyMMddHHmmss").format(new JDatetime());



        Map<String, String> map = AnacreditFilenameUtil.getOutputFilesMap();
        for (Map.Entry<String, String> entryReport : map.entrySet()) {
            boolean ok = walkThruPathAndCollect(_reportDirectory, entryReport.getKey(), entryReport.getValue());
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public boolean walkThruPathAndCollect(String _outputFolder, String type, String fileRoot) throws IOException {

        String prefix = type + _sdf.format(getValuationDatetime()) + SEPARATOR;
        final String fileNameOut = appendDateTime(getValuationDatetime(),this,  fileRoot);
        List<Path> touchedFiles = new ArrayList<>();
        // create master output
        File master = new File(DisplayInBrowser.getFileFullName("txt", _outputFolder + fileNameOut));
        master.createNewFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master))) {
            Path root = Paths.get(master.getParent());
            Files.walkFileTree(root, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    //if (dir.toAbsolutePath().toString().contains("archive")) {
                    if (!dir.toAbsolutePath().toString().equals(root.toAbsolutePath().toString())) {
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
                    if (file.toFile().getName().contains(prefix)) {
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

            // All is fine lets rename files
            archive(root , touchedFiles);

            return true;
        } catch (Exception exp) {
            Log.error(this, exp);
        }
        return false;
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
                    String fileName = path.toFile().getName() + SEPARATOR + _executionSufixLabel;
                    Path toFile  = Paths.get(archivePath +File.separator + fileName);
                    Files.move(path, toFile);
                } catch (IOException e) {
                    Log.error(this, "Error moving file to archive folder :" +path.toString() , e);
                }

            });
        };

    }

    private static void append(File file, BufferedWriter bw) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String text = null;
            while ((text = br.readLine()) != null) {
                bw.write(text);
                bw.newLine();
            }
        } finally {
            //file.delete();
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
        domainAttributes.add(REPORT_DIRECTORY);
        return domainAttributes;
    }

    private String getReportDirectory() {
        String folder = this.getAttribute(REPORT_DIRECTORY);

        if (Util.isEmpty(folder)) {
            folder = "/calypso_interfaces/anacredit/data";
        }
        if (!folder.endsWith("/")) {
            folder = folder.concat("/");
        }
        return folder;
    }

    @Override
    public String getTaskInformation() {
        return "Search and concatenate generated files to send to ANACREDIT";
    }

}
