package calypsox.engine.bloomberg;

import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.util.ScheduledTaskSANT_BLOOMBERG_TAGGING;
import calypsox.tk.util.ScheduledTaskSANT_BLOOMBERG_TAGGING.ProcessMode;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;
import com.calypso.tk.service.RemoteTrade;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Project: Bloomberg tagging

public class SANT_BloombergUpdaterEngineTest {

    private SantBloombergUpdaterEngine testClass;

    protected DSConnection dsConnection;
    protected RemoteBackOffice remoteBackOffice;
    protected RemoteTrade remoteTrade;
    protected ScheduledTaskSANT_BLOOMBERG_TAGGING mockSt;

    public SANT_BloombergUpdaterEngineTest() {
    }

    @Test
    public void testProcess() {
        mockDsConnection();
        testClass = new SantBloombergUpdaterEngine(dsConnection, "eshost", 7090);

        PSEvent event = new PSEventBloombergUpdate("", 0);
        boolean result = this.testClass.process(event);

        reset();

        Assert.assertTrue(result);
    }

    /**
     * when a remote object is requested to the DSConnection return the mock
     */
    private void mockDsConnection() {
        // create a mock for the DSConnection
        this.dsConnection = mock(DSConnection.class);
        DSConnection.setDefault(this.dsConnection);

        // create object for the remote services
        mockSt = new ScheduledTaskSANT_BLOOMBERG_TAGGING();

        remoteBackOffice = mock(RemoteBackOffice.class);
        remoteTrade = mock(RemoteTrade.class);

        try {
            when(this.dsConnection.getDefault().getRemoteBackOffice()).thenReturn(remoteBackOffice);

            when(remoteBackOffice.getScheduledTaskByExternalReference(Mockito.anyString()))
                    .thenReturn(mockSt);
            mockSt.setAttribute(ScheduledTaskSANT_BLOOMBERG_TAGGING.FILEPATH, "");
            mockSt.setAttribute(
                    ScheduledTaskSANT_BLOOMBERG_TAGGING.PROCESS_MODE, ProcessMode.FULL.toString());

            when(this.dsConnection.getRemoteTrade()).thenReturn(remoteTrade);

            Mockito.doNothing()
                    .when(this.remoteTrade)
                    .eventProcessed(Mockito.anyInt(), Mockito.anyString());

        } catch (Exception e) {
            Log.error(this, e); // sonar
        }
    }

    /**
     * reset all the mocked objects as well as the caches
     */
    public void reset() {
        Mockito.reset(this.remoteBackOffice);
        Mockito.reset(remoteTrade);
        Mockito.reset(this.dsConnection);
    }
}
