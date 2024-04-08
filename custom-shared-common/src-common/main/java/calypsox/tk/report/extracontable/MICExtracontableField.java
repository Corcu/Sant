package calypsox.tk.report.extracontable;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author aalonsop
 * Host demands fixed width formatted columns
 */
public class MICExtracontableField<T> {

    static final char NUMBER_PAD_CHAR = '0';
    static final char DEC_SEPARATOR = '.';
    static final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    NumberFormatterWrapper formatterWrapper;

    int lenght;
    int decimalLenght;
    boolean isSignedNumber;
    String content;

    /**
     * @param lenght
     */
    public MICExtracontableField(int lenght) {
        this(lenght, 0);
    }

    /**
     * @param lenght
     * @param isSignedNumber
     */
    public MICExtracontableField(int lenght, boolean isSignedNumber) {
        this(lenght, 0, isSignedNumber);
    }

    /**
     * @param lenght
     * @param decimalLenght For doubles only
     */
    public MICExtracontableField(int lenght, int decimalLenght) {
        this(lenght, decimalLenght, false);
    }

    /**
     * @param lenght
     * @param decimalLenght
     * @param isSignedNumber
     */
    public MICExtracontableField(int lenght, int decimalLenght, boolean isSignedNumber) {
        this.lenght = lenght;
        this.decimalLenght = decimalLenght;
        this.isSignedNumber = isSignedNumber;
        this.content = formatStringContent("");
        this.formatterWrapper = new NumberFormatterWrapper();
    }

    public void setContent(T rawContent) {
        this.content = formatContent(rawContent);
    }

    public String getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return this.content;
    }

    private String formatContent(T rawContent) {
        String formattedContent;
        if (rawContent instanceof String) {
            formattedContent = formatStringContent((String) rawContent);
        } else if (rawContent instanceof JDate) {
            formattedContent = formatJDateContent((JDate) rawContent);
        } else if (rawContent instanceof Number) {
            formattedContent = formatterWrapper.formatNumber((Number) rawContent);
        } else if (rawContent instanceof Amount) {
            formattedContent = formatterWrapper.formatNumber(((Amount) rawContent).get());
        } else{
            formattedContent=formatStringContent("");
        }
        return cropFieldIfExcedsLength(formattedContent);
    }

    private String formatStringContent(String rawContent) {
        return StringUtils.rightPad(rawContent, this.lenght);
    }

    private String formatJDateContent(JDate rawContent) {
        LocalDate date = Optional.ofNullable(rawContent)
                .map(ld -> LocalDate.of(ld.getYear(), ld.getMonth(), ld.getDayOfMonth()))
                .orElseGet(() -> LocalDate.of(1, 1, 1901));
        return datePattern.format(date);
    }

    private String cropFieldIfExcedsLength(String formattedContent) {
        String croppedContent = formattedContent;
        if (formattedContent.length() > this.lenght) {
            croppedContent = formattedContent.substring(0, this.lenght);
        }
        return croppedContent;
    }

    public static void main(String[] args) {
        MICExtracontableField<Long> fieldI = new MICExtracontableField<>(15, true);
        fieldI.setContent(-2200006200L);
        System.out.println(fieldI.toString());
        MICExtracontableField<Integer> fieldIp = new MICExtracontableField<>(15, true);
        fieldIp.setContent(12325469);
        System.out.println(fieldIp.toString());
    }

    /**
     * Encapsulates number formatting logic
     */
    private class NumberFormatterWrapper {

        DecimalFormat formatter;

        private NumberFormatterWrapper() {
            this.formatter = new DecimalFormat(buildFormatterPattern());
            this.formatter.setNegativePrefix("");
        }

        private String buildFormatterPattern() {
            String integerPattern = buildIntegerFormatterPattern();
            if (decimalLenght > 0) {
                String decimalPattern = StringUtils.leftPad("", decimalLenght, NUMBER_PAD_CHAR);
                integerPattern = integerPattern + DEC_SEPARATOR + decimalPattern;
            }
            return integerPattern;
        }

        private String buildIntegerFormatterPattern() {
            return StringUtils.leftPad("", lenght - decimalLenght, NUMBER_PAD_CHAR);
        }

        private String formatNumber(Number rawContent) {

            String formattedNumber = Optional.ofNullable(rawContent).map(formatter::format).orElse("0");
            formattedNumber = formattedNumber
                    .replace(String.valueOf(formatter.getDecimalFormatSymbols().getDecimalSeparator()), "");
            return StringUtils.leftPad(formattedNumber, lenght, String.valueOf(0));
        }
    }
}
