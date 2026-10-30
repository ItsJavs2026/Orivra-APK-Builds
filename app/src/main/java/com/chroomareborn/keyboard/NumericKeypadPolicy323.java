package com.chroomareborn.keyboard;

/**
 * Editor-declared numeric layout, independent of prediction/privacy policy.
 * Android InputType constants are duplicated here for pure-Java regression
 * without Android or a connection to a real banking app.
 */
final class NumericKeypadPolicy323 {
    private NumericKeypadPolicy323(){}
    static final String[] NUMBER_ROWS={"123","456","789"};
    static final int CLASS_MASK=0x0000000f;
    static final int CLASS_NUMBER=0x00000002;
    static final int CLASS_PHONE=0x00000003;
    static final int CLASS_DATETIME=0x00000004;
    static final int NUMBER_SIGNED=0x00001000;
    static final int NUMBER_DECIMAL=0x00002000;
    static final int NUMBER_PASSWORD=0x00000010;
    static boolean isNumericField(int inputType) {
        int cls=inputType & CLASS_MASK;
        return cls==CLASS_NUMBER || cls==CLASS_PHONE || cls==CLASS_DATETIME;
    }
    static boolean isSigned(int inputType) {
        return (inputType & CLASS_MASK)==CLASS_NUMBER &&
                (inputType & NUMBER_SIGNED)!=0;
    }
    /** Decimal is offered for ordinary numeric money fields even when a
     * third-party app omitted the DECIMAL hint. It remains the host's
     * choice whether to accept the character. Never offer punctuation to
     * numeric-password/PIN fields.
     */
    static String secondarySymbol(int inputType) {
        int cls=inputType & CLASS_MASK;
        if(cls==CLASS_PHONE)return "+";
        if(cls==CLASS_DATETIME)return "/";
        if(cls!=CLASS_NUMBER)return "";
        if((inputType & 0x00000ff0)==NUMBER_PASSWORD)return "";
        return ".";
    }
}
