package calypsox.tk.util;

import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.ScheduledTaskMESSAGE_MATCHING;
import org.jfree.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ScheduledTaskSANT_IMPORT_SWIFT_MESSAGES extends ScheduledTask {

    protected final List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributes = Arrays.asList(attribute("InputDir").description("The directory where incoming message text file is saved").mandatory(),
                //attribute("File Rename").domain(Arrays.asList("True", "False")).description("Rename the incoming message text file after import"),
                attribute("Move File").domain(Arrays.asList("True", "False")).description("Move the incoming message text file after import to Copy folder"),
                attribute("ExternalMessageType").domainName("formatType").description("The incoming message format. Default (if missing) is SWIFT. Choices are formatType domain values."));
        return attributes;
    }

    @Override
    public String getTaskInformation() {
        return "Import Swift Messages from specific directory";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        String filePath = this.getAttribute("InputDir");
        String moveToCopy = this.getAttribute("Move File");

        try {
            Files.list(Paths.get(filePath)).forEach(s -> {
                ScheduledTaskMESSAGE_MATCHING matchingST = new ScheduledTaskMESSAGE_MATCHING();
                fillMatchingSTAttributes(matchingST, s.getFileName().toString());
                boolean result = matchingST.process(ds, ps);
                if (Boolean.parseBoolean(moveToCopy))
                    moveToCopy(s);
                if (!result) {
                    Log.error("Can't import Swift Message: " + s.toString());
                }
            });
        } catch (IOException e) {
            Log.error("Cant find the path: " + filePath, e);
        }
        return true;
    }

    private void fillMatchingSTAttributes(ScheduledTaskMESSAGE_MATCHING matchingST, String fileName) {

        matchingST.setAttribute("Swift File", fileName);
        matchingST.setAttribute("InputDir", getAttribute("InputDir"));
        matchingST.setAttribute("File Rename", getAttribute("File Rename"));
        if (this.getDatetime() != null)
            matchingST.setAttribute("DateTime", this.getValuationDatetime().toString());
        matchingST.setExecuteB(this.getExecuteB());
        matchingST.setAttribute("ExternalMessageType", getAttribute("ExternalMessageType"));
        matchingST.setDatetime(this.getDatetime());
        matchingST.setCurrentDate(this.getCurrentDate());
        matchingST.setValuationTime(this.getValuationTime());
    }

    private void createDirectoryIfnotExist(Path sourcePath) {
        String destFile = sourcePath.getParent().toString() + "/copy/";
        Path destPath = Paths.get(destFile);
        if (!Files.exists(destPath)) {
            try {
                Files.createDirectory(destPath);
            } catch (IOException e) {
                Log.error("Cant create the path: " + destPath.toString(), e);
            }
        }
    }

    private void moveToCopy(Path sourcePath) {
        createDirectoryIfnotExist(sourcePath);
        String destFile = sourcePath.getParent().toString() + "/copy/" + sourcePath.getFileName().toString();
        Path destPath = Paths.get(destFile);

        try {
            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Cant move the file to path: " + destPath, e);
        }
    }
}
