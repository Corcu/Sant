package calypsox.repoccp.reader;

import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPTrade;
import io.reactivex.FlowableSubscriber;
import org.reactivestreams.Subscription;

/**
 * @author aalonsop
 * IN DEV
 */
public class FileReaderSubscriber implements FlowableSubscriber<ReconCCP> {

    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println("On subscription");
    }

    @Override
    public void onNext(ReconCCP reconCCP) {
        System.out.println("On next"+reconCCP.toString());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("On error"+throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("On complete");
    }
}
