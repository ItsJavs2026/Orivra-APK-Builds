package com.chroomareborn.keyboard;

public final class KeyboardHapticStyleTest {
    private static void check(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int m=KeyboardHapticStyle.OFF;
        String[] names={"OFF","LIGHT","STANDARD","CRISP"};
        for(int i=0;i<4;i++) {
            check(KeyboardHapticStyle.normalize(m)==i,"mode "+i);
            check(KeyboardHapticStyle.label(m).equals(names[i]),"label "+i);
            check(KeyboardHapticStyle.enabled(m)==(i!=0),"enabled "+i);
            m=KeyboardHapticStyle.next(m);
        }
        check(m==KeyboardHapticStyle.OFF,"cycle wraps");
        check(KeyboardHapticStyle.normalize(-1)==KeyboardHapticStyle.STANDARD,
                "invalid negative mode defaults to standard");
        check(KeyboardHapticStyle.normalize(99)==KeyboardHapticStyle.STANDARD,
                "invalid high mode defaults to standard");
        System.out.println("BUILD287_HAPTIC_STYLE checks passed");
    }
}
