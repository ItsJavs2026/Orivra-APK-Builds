package com.chroomareborn.keyboard;
public final class NumericKeypadPolicy323Test {
    static int checks;
    static void ok(boolean b,String text) {
        checks++;if(!b)throw new AssertionError(text);
    }
    public static void main(String[] args) {
        for(int kind:new int[]{2,3,4})
            ok(NumericKeypadPolicy323.isNumericField(kind),"numeric classes recognized");
        for(int kind:new int[]{0,1,0x20001,0x80001,0x81})
            ok(!NumericKeypadPolicy323.isNumericField(kind),
                    "regular/credential text is NOT auto-numeric");
        ok(NumericKeypadPolicy323.isNumericField(2|0x2000),
                "bank amount decimal flagged numeric");
        ok(".".equals(NumericKeypadPolicy323.secondarySymbol(2|0x2000)),
                "currency amount has decimal");
        ok(".".equals(NumericKeypadPolicy323.secondarySymbol(2)),
                "amount without decimal flag still offers dot");
        ok("".equals(NumericKeypadPolicy323.secondarySymbol(2|0x10)),
                "PIN password never offers dot");
        ok("+".equals(NumericKeypadPolicy323.secondarySymbol(3)),
                "phone allows plus");
        ok("/".equals(NumericKeypadPolicy323.secondarySymbol(4)),
                "date editor offers slash");
        ok(NumericKeypadPolicy323.isSigned(2|0x1000),
                "negative balance/payment supported when host allows");
        ok(!NumericKeypadPolicy323.isSigned(2),"positive-only amount");
        ok(!NumericKeypadPolicy323.isSigned(3|0x1000),
                "phone does not inherit numeric flags");
        String[] lines=NumericKeypadPolicy323.NUMBER_ROWS;
        ok(lines.length==3&&String.join("",lines).equals("123456789"),
                "nine reachable digit keys plus dedicated zero");
        System.out.println("PASS NumericKeypadPolicy323Test "+checks+" checks");
    }
}
