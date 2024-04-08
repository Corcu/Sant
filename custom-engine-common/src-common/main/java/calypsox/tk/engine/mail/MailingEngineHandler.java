package calypsox.tk.engine.mail;

import calypsox.tk.event.PSEventMailingAlert;

/**
 * Created to extract mailing alerts processing from TaskEngine.
 * Single senders were not heavily refactored, so they are lacking a bit of consistency.
 *
 */
public class MailingEngineHandler{

    RepoMissingSDIMailSender repoSDITaskEngineListener=new RepoMissingSDIMailSender();
    BondMissingCptyMailSender bondAllocCptyTaskEngineListener=new BondMissingCptyMailSender();

    /**
     * Processing of PSEventMailingAlert events
     * @param event the PSEventTask event
     */
    public void processEvent(PSEventMailingAlert event) {
        repoSDITaskEngineListener.sendEmail(event);
        bondAllocCptyTaskEngineListener.sendEmail(event);
    }

}
