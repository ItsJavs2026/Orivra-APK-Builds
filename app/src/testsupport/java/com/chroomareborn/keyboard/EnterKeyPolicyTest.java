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
        // Build 310: Search/Go must NOT insert a newline, even if a custom
        // web search field incorrectly also declares MULTI_LINE.
        check(EnterKeyPolicy.tapEditorAction(EnterKeyPolicy.IME_ACTION_SEARCH)
                ==EnterKeyPolicy.IME_ACTION_SEARCH,"SEARCH on a normal tap");
        check(EnterKeyPolicy.tapEditorAction(EnterKeyPolicy.IME_ACTION_GO)
                ==EnterKeyPolicy.IME_ACTION_GO,"GO on a normal tap");
        check(EnterKeyPolicy.tapEditorAction(0x10000000 |
                EnterKeyPolicy.IME_ACTION_SEARCH)==EnterKeyPolicy.IME_ACTION_SEARCH,
                "unrelated editor flags do not suppress explicit SEARCH");
        for(int action:new int[]{0,1,4,5,6,7,0xFF}) {
            check(EnterKeyPolicy.tapEditorAction(action)==0,
                    "Never implicitly send/chat/submit arbitrary editor: "+action);
        }
        // Android's TextView may automatically add NO_ENTER_ACTION to a
        // multiline-capable search editor. An EXPLICIT SEARCH/GO still wins.
        for(int action:new int[]{EnterKeyPolicy.IME_ACTION_SEARCH,
                EnterKeyPolicy.IME_ACTION_GO}) {
            check(EnterKeyPolicy.tapEditorAction(
                    action|EnterKeyPolicy.IME_FLAG_NO_ENTER_ACTION)==action,
                    "Explicit Search/Go intent wins over generic multiline flag");
        }
        check(EnterKeyPolicy.tapEditorAction(
                EnterKeyPolicy.IME_ACTION_SEND|
                EnterKeyPolicy.IME_FLAG_NO_ENTER_ACTION)==0,
                "Even with NO_ENTER_ACTION, SEND is strictly held");
        check("⌕".equals(EnterKeyPolicy.keyLabel(
                EnterKeyPolicy.IME_ACTION_SEARCH)),"Search icon visible");
        check("Go".equals(EnterKeyPolicy.keyLabel(
                EnterKeyPolicy.IME_ACTION_GO)),"Go label visible");
        check("↵".equals(EnterKeyPolicy.keyLabel(
                EnterKeyPolicy.IME_ACTION_SEND)),"chat remains normal Enter");
        check("⌕".equals(EnterKeyPolicy.keyLabel(
                EnterKeyPolicy.IME_ACTION_SEARCH|
                EnterKeyPolicy.IME_FLAG_NO_ENTER_ACTION)),
                "Explicit Search key remains recognizable even in multiline editor");
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
