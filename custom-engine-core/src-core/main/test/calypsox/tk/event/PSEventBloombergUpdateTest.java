package calypsox.tk.event;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Project: Bloomberg tagging

public class PSEventBloombergUpdateTest {

  @Before
  public void setUp() {}

  @Test
  public void testGetTituloId() {
    PSEventBloombergUpdate test = new PSEventBloombergUpdate("titulo", 0);
    assertEquals("titulo", test.getTituloId());
  }

  @Test
  public void testSetTituloId() {
    PSEventBloombergUpdate test = new PSEventBloombergUpdate("titulo", 0);
    test.setTituloId("titulo2");
    assertEquals("titulo2", test.getTituloId());
  }

  @Test
  public void testGetTipo() {
    PSEventBloombergUpdate test = new PSEventBloombergUpdate("titulo", 0);
    assertEquals(0, test.getTipo());
  }

  @Test
  public void testSetTipo() {
    PSEventBloombergUpdate test = new PSEventBloombergUpdate("titulo", 0);
    test.setTipo(1);
    assertEquals(1, test.getTipo());
  }

  @Test
  public void testGetEventType() {
    PSEventBloombergUpdate test = new PSEventBloombergUpdate("titulo", 0);
    assertEquals("SEND_OPTIM_POSITION", test.getEventType());
  }
}
