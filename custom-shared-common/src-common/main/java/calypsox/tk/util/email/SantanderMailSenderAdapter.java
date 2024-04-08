package calypsox.tk.util.email;

import com.calypso.tk.core.Log;
import com.calypso.tk.util.email.*;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.SantanderEmailSender;


import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aalonsop
 */
public class SantanderMailSenderAdapter implements MailSenderInterface {

    SantanderEmailSender customMailSender;

    public SantanderMailSenderAdapter(){
        try {
            customMailSender=new SantanderEmailSender();
        } catch (MessagingException exc) {
            Log.error(SantanderMailSenderAdapter.class,
                    "Exception while initializing SantanderMailSender",exc.getCause());
        }
    }

    @Override
    public void setSMTPUsername(String username) throws MailException {
        //EMPTY
    }

    @Override
    public void setSMTPPassword(String password) throws MailException {
        //EMPTY
    }

    @Override
    public void setSMTPAuthRequired(boolean authRequired) throws MailException {
        //EMPTY
    }

    @Override
    public void setSMTPHost(String hostname) throws MailException {
        //EMPTY
    }

    @Override
    public void setSMTPPort(int port) throws MailException {
        //EMPTY
    }

    @Override
    public void send(Message msg) throws MailException {
        EmailMessage msgWrapper=new EmailMessage();
        msgWrapper.setFrom(msg.getFrom().getAddress());
        msgWrapper.setTo(adaptTo(msg.getTo()));
        msgWrapper.setToCc(adaptCC(msg.getCC()));
        msgWrapper.setSubject(msg.getSubject());
        msgWrapper.setText(msg.getText());
        Attachment attachment=msg.getAttachment();
        if(attachment!=null) {
            msgWrapper.setContentType(attachment.getContentType());
            msgWrapper.addAttachment(attachment.getContentType(),attachment.getName(),attachment.getContents());
        }
        customMailSender.send(msgWrapper);
    }

    private List<String> adaptCC(Address[] addresses){
        List<String> mails=new ArrayList<>();
        for(Address address:addresses){
            mails.add(address.getAddress());
        }
        return mails;
    }

    private List<String> adaptTo(Address toAddress){
        List<String> mails=new ArrayList<>();
        mails.add(toAddress.getAddress());
        return mails;
    }
}
