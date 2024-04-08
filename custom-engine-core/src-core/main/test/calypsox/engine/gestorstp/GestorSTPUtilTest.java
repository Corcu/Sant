package calypsox.engine.gestorstp;

import com.calypso.tk.bo.swift.SwiftMessage;
import org.junit.Assert;
import org.junit.Test;

public class GestorSTPUtilTest {

    @Test
    public void testFixEndOfLineCharacters() {
        String incomingMessage = "first_line123456   5555\r\nsecond_line:4: ::;;\n\r\n";

        String fixedMessage = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessage);
        Assert.assertEquals("first_line123456   5555" + SwiftMessage.END_OF_LINE + "second_line:4: ::;;"
                + SwiftMessage.END_OF_LINE + SwiftMessage.END_OF_LINE, fixedMessage);


        incomingMessage = "first_line123456   5555\r\nsecond_line:4: ::;;\n\r\nlast_line";

        fixedMessage = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessage);
        Assert.assertEquals("first_line123456   5555" + SwiftMessage.END_OF_LINE + "second_line:4: ::;;"
                + SwiftMessage.END_OF_LINE + SwiftMessage.END_OF_LINE + "last_line", fixedMessage);


        incomingMessage = "first_line123456   5555\nsecond_line:4: ::;;\r\n\n\rlast_line";

        fixedMessage = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessage);
        Assert.assertEquals("first_line123456   5555" + SwiftMessage.END_OF_LINE + "second_line:4: ::;;"
                + SwiftMessage.END_OF_LINE + SwiftMessage.END_OF_LINE + "last_line", fixedMessage);

        incomingMessage = "first_line123456   5555\n\rsecond_line:4: ::;;\n\n\rlast_line";

        fixedMessage = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessage);
        Assert.assertEquals("first_line123456   5555" + SwiftMessage.END_OF_LINE + "second_line:4: ::;;"
                + SwiftMessage.END_OF_LINE + SwiftMessage.END_OF_LINE + "last_line", fixedMessage);
    }

}
