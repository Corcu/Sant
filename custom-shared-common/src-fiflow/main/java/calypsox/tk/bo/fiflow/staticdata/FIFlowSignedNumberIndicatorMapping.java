package calypsox.tk.bo.fiflow.staticdata;

/**
 * @author aalonsop
 */
public enum FIFlowSignedNumberIndicatorMapping {

    POSITIVE_0('{'),
    POSITIVE_1('A'),
    POSITIVE_2('B'),
    POSITIVE_3('C'),
    POSITIVE_4('D'),
    POSITIVE_5('E'),
    POSITIVE_6('F'),
    POSITIVE_7('G'),
    POSITIVE_8('H'),
    POSITIVE_9('I'),
    NEGATIVE_0('}'),
    NEGATIVE_1('J'),
    NEGATIVE_2('K'),
    NEGATIVE_3('L'),
    NEGATIVE_4('M'),
    NEGATIVE_5('N'),
    NEGATIVE_6('O'),
    NEGATIVE_7('P'),
    NEGATIVE_8('Q'),
    NEGATIVE_9('R');

    static final String POSITIVE_STR = "POSITIVE_";
    static final String NEGATIVE_STR = "NEGATIVE_";

    char targetChar;

    FIFlowSignedNumberIndicatorMapping(char targetChar) {
        this.targetChar = targetChar;
    }

    public char getValue() {
        return this.targetChar;
    }

    public static FIFlowSignedNumberIndicatorMapping getTargetChar(char numberLastChar, boolean isPositive) {
        String key = NEGATIVE_STR;
        if (isPositive) {
            key = POSITIVE_STR;
        }
        return valueOf(key+numberLastChar);
    }
}
