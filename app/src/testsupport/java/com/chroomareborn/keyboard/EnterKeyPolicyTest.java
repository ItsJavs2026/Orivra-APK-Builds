package com.chroomareborn.keyboard;

public final class EnterKeyPolicyTest {
    private static void check(boolean outcome, String message) {
        if(!outcome) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(EnterKeyPolicy.supportsLongPressSend(4), "plain SEND editor");
        check(EnterKeyPolicy.supportsLongPressSend(4 | 0x10000000),
                "SEND with unrelated flags");
        int[] notSend={0,1,2,3,5,6,7,0xFF};
        for(int options:notSend)
            check(!EnterKeyPolicy.supportsLongPressSend(options),
                    "Non-SEND action must not submit: " + options);
        check(EnterKeyPolicy.supportsLongPressSend(
                4 | EnterKeyPolicy.IME_FLAG_NO_ENTER_ACTION),
                "Dedicated hold may send even when inline Send is forbidden");
        check(EnterKeyPolicy.supportsNewline(0x20001),
                "multiline text input supports newline");
        check(!EnterKeyPolicy.supportsNewline(0x1),
                "single-line text must not submit through newline");
        check(!EnterKeyPolicy.supportsNewline(0x2),
                "numeric editor must not get newline");
        check(!EnterKeyPolicy.supportsNewline(0),
                "unspecified editor must fail closed");
        check(EnterKeyPolicy.gestureWithinSlop(3,4,18), "small thumb movement");
        check(!EnterKeyPolicy.gestureWithinSlop(30,1,18), "large horizontal drag");
        check(!EnterKeyPolicy.gestureWithinSlop(1,-30,18), "large vertical drag");
        check(!EnterKeyPolicy.gestureWithinSlop(Float.NaN,0,18), "NaN motion");
        check(!EnterKeyPolicy.gestureWithinSlop(0,0,Float.NaN), "NaN threshold");
        check(!EnterKeyPolicy.gestureWithinSlop(0,0,0), "zero threshold");
        int checked=0;
        for(int mask=0;mask<32;mask++) {
            boolean release=(mask&1)!=0;
            boolean armed=(mask&2)!=0;
            boolean cancelled=(mask&4)!=0;
            boolean sameSession=(mask&8)!=0;
            boolean sendAvailable=(mask&16)!=0;
            boolean expectedSend=release && armed && !cancelled &&
                    sameSession && sendAvailable;
            boolean expectedNewline=release && !armed && !cancelled && sameSession;
            boolean send=EnterKeyPolicy.shouldSendOnRelease(
                    release,armed,cancelled,sameSession,sendAvailable);
            boolean newline=EnterKeyPolicy.shouldInsertNewline(
                    release,armed,cancelled,sameSession);
            check(expectedSend==send,"incorrect send decision for mask "+mask);
            check(expectedNewline==newline,"incorrect newline decision for mask "+mask);
            check(!(send&&newline),"one gesture cannot both send and insert newline");
            checked++;
        }
        // Regression: the original Orivra Enter called performEditorAction(SEND)
        // on a single tap. The new policy must reject every non-armed tap.
        check(!EnterKeyPolicy.shouldSendOnRelease(true,false,false,true,true),
                "single-tap must never submit SEND");
        check(EnterKeyPolicy.shouldInsertNewline(true,false,false,true),
                "single-tap must request newline");
        check(!EnterKeyPolicy.shouldSendOnRelease(true,true,false,false,true),
                "focus switch must not submit SEND");
        check(!EnterKeyPolicy.shouldSendOnRelease(false,true,false,true,true),
                "finger down alone must never submit SEND");
        check(!EnterKeyPolicy.shouldSendOnRelease(true,true,true,true,true),
                "cancelled long press must not send");
        check(!EnterKeyPolicy.shouldSendOnRelease(true,true,false,true,false),
                "app lost SEND capability");
        System.out.println("EnterKeyPolicyTest passed: " + checked +
                " release/hold/editor combinations and action flags");
    }
}
