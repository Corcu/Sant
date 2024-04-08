package calypsox.tk.product;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventBloombergUpdate;
import com.calypso.apps.product.ShowProduct;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.service.RemoteTrade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Vector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Project: Bloomberg tagging

public class EquityProductValidatorTest {

  protected EquityProductValidator test = null;

  protected DSConnection dsConnection;
  protected RemoteProduct remoteProduct;
  protected RemoteTrade remoteTrade;

  @Before
  public void setUp() throws Exception {}

  /** Test isValidInput to be saved. */
  @SuppressWarnings({"rawtypes", "deprecation"})
  @Test
  public void testIsValidInput() {
    EquityProductValidator fixture = new EquityProductValidator();

    mockDsConnection();

    String literal = "AAA";

    Equity product = new Equity();
    product.setQuoteType("Price");
    product.setCorporateName(literal);
    product.setName(literal);
    product.setCurrency(literal);

    product.setSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN, "1");

    ShowProduct mockShow = mock(ShowProduct.class);

    Vector<String> vector = new Vector<String>();

    Vector<Product> matchingProducts = new Vector<Product>();
    matchingProducts.add(product);

    try {
      when((DSConnection.getDefault()
              .getRemoteProduct()
              .getProductsByCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN, "1")))
          .thenReturn(matchingProducts);

    } catch (final Exception e) {
      Log.error(this, e); // sonar
    }

    boolean result = fixture.isValidInput(product, mockShow, vector);

    reset();
    assert (result);
  }

  /** when a remote object is requested to the DSConnection return the mock */
  private void mockDsConnection() {
    // create a mock for the DSConnection
    this.dsConnection = mock(DSConnection.class);
    DSConnection.setDefault(this.dsConnection);

    // create mock object for the remote services
    this.remoteProduct = mock(RemoteProduct.class);

    this.remoteTrade = mock(RemoteTrade.class);

    when(this.dsConnection.getRemoteProduct()).thenReturn(this.remoteProduct);

    when(this.dsConnection.getRemoteTrade()).thenReturn(this.remoteTrade);

    PSEventBloombergUpdate mockEvent = new PSEventBloombergUpdate("", 0);

    try {
      Mockito.doReturn(1).when(this.dsConnection.getRemoteTrade().saveAndPublish(mockEvent));
    } catch (Exception e) {
      Log.error(this, e); // sonar
    }
  }

  /** reset all the mocked objects as well as the caches */
  public void reset() {
    Mockito.reset(this.remoteProduct);
    Mockito.reset(this.dsConnection);
    Mockito.reset(this.remoteTrade);
  }
}
