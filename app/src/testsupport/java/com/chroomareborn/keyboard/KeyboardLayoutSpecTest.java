package com.chroomareborn.keyboard;
import java.util.HashSet;
import java.util.Set;
public final class KeyboardLayoutSpecTest {
    private static int checks;
    private static void test(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        String[] alpha=KeyboardLayoutSpec.rows(KeyboardLayoutSpec.ABC);
        test(alpha.length==3,"three QWERTY rows");
        test(alpha[0].equals("qwertyuiop")&&alpha[1].equals("asdfghjkl")&&
            alpha[2].equals("zxcvbnm"),"screenshot ABC matrix");
        String[] symbols=KeyboardLayoutSpec.rows(KeyboardLayoutSpec.SYMBOLS);
        test(symbols[0].equals("1234567890"),"Gboard digit order");
        test(symbols[1].equals("@#$_&-+()/"),"underscore in screenshot position");
        test(symbols[2].equals("*\"':;!?"),"Gboard symbol row without overflow");
        for(int i=0;i<3;i++) {
            test(alpha[i].length()==KeyboardLayoutSpec.LETTER_HINTS[i].length(),
                 "hint row aligns with each letter");
            for(int j=0;j<alpha[i].length();j++){
                String hint=KeyboardLayoutSpec.longPressHint(i,j);
                test(hint!=null && hint.length()==1,"all alphabet hints usable");
                test(KeyboardLayoutSpec.containsPrimarySymbol(hint.charAt(0)),
                    "secondary letter hint also exists in primary symbol screen");
            }
        }
        test(KeyboardLayoutSpec.longPressHint(-1,1)==null,"negative row rejected");
        test(KeyboardLayoutSpec.longPressHint(2,20)==null,"hint overflow rejected");
        test(KeyboardLayoutSpec.bottomModeLabel(0).equals("?123"),"ABC toggle");
        test(KeyboardLayoutSpec.bottomModeLabel(1).equals("ABC"),"symbol toggle");
        test(KeyboardLayoutSpec.thirdRowPrefix(1).equals("=\\<"),"extra page key");
        test(KeyboardLayoutSpec.thirdRowPrefix(2).equals("?123"),"return extra page");
        test(KeyboardLayoutSpec.toggleMainMode(0)==1,"ABC to symbols");
        test(KeyboardLayoutSpec.toggleMainMode(1)==0,"symbols to ABC");
        test(KeyboardLayoutSpec.toggleSymbolPage(1)==2,"primary to extra");
        test(KeyboardLayoutSpec.toggleSymbolPage(2)==1,"extra to primary");
        test(KeyboardLayoutSpec.EXTRA_SYMBOLS.length==3,"extra pages well formed");
        test(KeyboardLayoutSpec.EXTRA_SYMBOLS[1].contains("\\"),"backslash accessible");
        test(KeyboardLayoutSpec.PRIMARY_SYMBOLS[1].contains("_"),"underscore accessible");
        System.out.println("BUILD286_GBOARD_REFERENCE checks="+checks+" passed");
    }
}
