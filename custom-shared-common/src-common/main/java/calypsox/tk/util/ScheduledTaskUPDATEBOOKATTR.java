package calypsox.tk.util;

import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

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
public class ScheduledTaskUPDATEBOOKATTR extends ScheduledTask {

    private final String filePathAttr="File Path";
    private final String fileNameAttr="File Name";
    private final String attrToUpdate="Book Attribute to update";

    @Override
    public String getTaskInformation() {
        return "Updates defined book's attribute. Input file must be a ';' delimited CSV, with two columns (Book Name and Attribute Value)";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String filePath=getAttribute(filePathAttr);
        String fileName=getAttribute(fileNameAttr);
        String attributeName=getAttribute(attrToUpdate);
        List<BookBean> mappedLines=read(filePath+fileName);
        updateBooks(mappedLines,attributeName);
        return super.process(ds,ps);
    }

    public List<BookBean> read(String filePath){
        Path path = Paths.get(filePath);

        List<BookBean> mappedLines = new ArrayList<>();
        try {
            mappedLines = Files.lines(path).map(this::mapLine)
                    .collect(Collectors.toList());
        } catch (IOException exc) {
            Log.error(this,exc);
        }
        return mappedLines;
    }

    private BookBean mapLine(String line){
        BookBean bb=null;
        if(!Util.isEmpty(line)){
            String[] columns=line.split(";");
            if(columns.length==2){
                String bookName=columns[0];
                String attrValue=columns[1];
                if(!Util.isEmpty(bookName)){
                    bb= new BookBean(bookName, attrValue);
                }
            }
        }
        return bb;
    }

    private void updateBooks(List<BookBean> mappedLines,String attributeToUpdate){
        Log.system(ScheduledTask.LOG_CATEGORY,"Book initial size: "+mappedLines.size());
        int updatedBooks=0;
        for (BookBean mappedLine : mappedLines) {
            try {
                Book book = DSConnection.getDefault().getRemoteReferenceData().getBook(mappedLine.getBookName());
                if (book != null) {
                    book.setAttribute(attributeToUpdate, mappedLine.getAttrValue());
                    DSConnection.getDefault().getRemoteReferenceData().save(book);
                    updatedBooks++;
                } else {
                    Log.system(ScheduledTask.LOG_CATEGORY, "Book " + mappedLine.getBookName() + " not found in Calypso");
                }
            } catch (CalypsoServiceException e) {
                Log.error(ScheduledTask.LOG_CATEGORY, "Could not update book: " + mappedLine.getBookName());
            }
        }
        Log.system(ScheduledTask.LOG_CATEGORY,"Processed "+updatedBooks+" of "+mappedLines.size()+" books");
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(fileNameAttr));
        attributeList.add(attribute(filePathAttr));
        attributeList.add(attribute(attrToUpdate));
        return attributeList;
    }

    private static class BookBean{
        String bookName;
        String attrValue;

        BookBean(String bookName,String attrValue){
            this.bookName=bookName;
            this.attrValue=attrValue;
        }

        String getBookName(){
            return bookName;
        }

        String getAttrValue(){
            return attrValue;
        }
    }
}
