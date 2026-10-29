package com.chroomareborn.keyboard;

import java.util.Random;

/** Regression checks for delayed swipe edits inside an unchanged editor. */
public final class BackspaceSwipeSnapshotGuardTest {
    private static long assertions=0;
    private static void check(boolean ok,String message) {
        assertions++;
        if(!ok)throw new AssertionError(message);
    }
    private static void accepts(String before,String selected,String after,
                                boolean hasSelection,int words) {
        BackspaceSwipeSnapshotGuard.Ticket ticket=
                BackspaceSwipeSnapshotGuard.capture(
                        before,selected,after,hasSelection,words);
        check(ticket!=null,"snapshot unexpectedly rejected");
        check(BackspaceSwipeSnapshotGuard.isCurrent(
                ticket,before,selected,after,hasSelection,words),
                "same editor snapshot rejected");
        check(!BackspaceSwipeSnapshotGuard.isCurrent(
                ticket,before+"x",selected,after,hasSelection,words),
                "changed preceding context accepted");
        check(!BackspaceSwipeSnapshotGuard.isCurrent(
                ticket,before,selected,after+"x",hasSelection,words),
                "changed following context accepted");
        check(!BackspaceSwipeSnapshotGuard.isCurrent(
                ticket,before,selected,after,hasSelection,
                words==8?7:words+1), "changed word count accepted");
        check(!BackspaceSwipeSnapshotGuard.isCurrent(
                ticket,before,hasSelection?selected+"!":"other",after,
                hasSelection,words),"changed selection accepted");
    }
    public static void main(String[] args) {
        accepts("heythere","","",false,1);
        accepts("likethis","","",false,1);
        BackspaceSwipeSnapshotGuard.Ticket unspaced=
                BackspaceSwipeSnapshotGuard.capture("likethis",null,"",false,1);
        check("likethis".equals(BackspaceSwipeSnapshotGuard.previewText(unspaced,38)),
                "unspaced highlighted preview must show full likethis word");
        check("…this".equals(BackspaceSwipeSnapshotGuard.previewText(unspaced,4)),
                "truncated highlight must preserve trailing context");
        check(!BackspaceSwipeSnapshotGuard.previewsSelection(unspaced),
                "unspaced word must not be displayed as selection");
        check("".equals(BackspaceSwipeSnapshotGuard.previewText(null,38)),
                "invalid snapshot must never reveal a preview");
        BackspaceSwipeSnapshotGuard.Ticket selected=
                BackspaceSwipeSnapshotGuard.capture("before ","like this"," after",true,1);
        check(BackspaceSwipeSnapshotGuard.previewsSelection(selected),
                "selected text must be marked as selection in preview");
        check("like this".equals(BackspaceSwipeSnapshotGuard.previewText(selected,38)),
                "selected multi-word phrase should be highlighted as a whole");
        accepts("hello world","","",false,1);
        accepts("hello ","world"," after",true,1);
        accepts("","heythere","",true,1);
        accepts("abc😊","","there",false,1);
        accepts("a\r\nb\t中文","","😊",false,2);
        check(BackspaceSwipeSnapshotGuard.capture(null,null,"",false,1)==null,
                "null before accepted");
        check(BackspaceSwipeSnapshotGuard.capture("heythere",null,null,false,1)==null,
                "null after accepted");
        check(BackspaceSwipeSnapshotGuard.capture("likethis",null,"",true,1)==null,
                "selection without selected text accepted");
        check(BackspaceSwipeSnapshotGuard.capture("likethis",null,"",false,0)==null,
                "zero words accepted");
        check(BackspaceSwipeSnapshotGuard.capture("likethis",null,"",false,9)==null,
                "too many words accepted");
        check(BackspaceSwipeSnapshotGuard.capture("a".repeat(2048),null,"",false,1)==null,
                "truncated uninterrupted text accepted");
        check(BackspaceSwipeSnapshotGuard.capture("",null,"",false,1)==null,
                "empty word target accepted");
        check(BackspaceSwipeSnapshotGuard.capture("a", "b".repeat(2049),"",true,1)==null,
                "oversized selection accepted");
        check(BackspaceSwipeSnapshotGuard.capture("abc",null,"z".repeat(97),false,1)==null,
                "oversized after-context accepted");
        Random random=new Random(2921029L);
        String[] words={"likethis","heythere","word","one.two","123456",
                "x","hello_world","中文","café","😊","👍🏽"};
        String[] gaps={" ","  ","\r\n","\t","\n"};
        for(int i=0;i<40000;i++) {
            StringBuilder before=new StringBuilder();
            int n=random.nextInt(12);
            for(int k=0;k<n;k++)before.append(words[random.nextInt(words.length)])
                    .append(gaps[random.nextInt(gaps.length)]);
            String last=words[random.nextInt(words.length)];
            before.append(last);
            String trailing=random.nextBoolean()?"":" tail";
            int count=1+random.nextInt(8);
            accepts(before.toString(),"",trailing,false,count);
            BackspaceSwipeSnapshotGuard.Ticket previewTicket=
                    BackspaceSwipeSnapshotGuard.capture(before,null,trailing,false,count);
            String expectedPreview=BackspaceSwipePolicy.previewForSwipe(before,count,38);
            check(expectedPreview.equals(
                    BackspaceSwipeSnapshotGuard.previewText(previewTicket,38)),
                    "highlight preview differs from exact deletion snapshot");
            int delete=BackspaceSwipePolicy.safeCharsToDelete(before,last,count);
            check(delete>=last.length(),"unspaced token deletion cut short");
            // A text change during swipe must not apply a delayed deletion.
            BackspaceSwipeSnapshotGuard.Ticket ticket=
                BackspaceSwipeSnapshotGuard.capture(before,null,trailing,false,count);
            check(ticket!=null,"random snapshot unavailable");
            check(!BackspaceSwipeSnapshotGuard.isCurrent(ticket,before+"!",null,
                    trailing,false,count),"changed draft accepted");
        }
        System.out.println("BackspaceSwipeSnapshotGuardTest passed: 40000 seeded cases, "+
                assertions+" checks");
    }
}
