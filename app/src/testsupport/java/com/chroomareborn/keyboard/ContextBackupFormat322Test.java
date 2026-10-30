package com.chroomareborn.keyboard;
import java.util.Map;
import java.util.List;
public final class ContextBackupFormat322Test {
    private static int n;
    static void ok(boolean v,String msg){n++;if(!v)throw new AssertionError(msg);}
    static void reject(Runnable fn,String msg) {
        boolean failed=false;
        try{fn.run();}catch(IllegalArgumentException expected){failed=true;}
        ok(failed,msg);
    }
    public static void main(String[] args) {
        String v1=LearningTransferCodec.encode(List.of("Customword"),
                Map.of("tekk:tell",2));
        ok(ContextBackupFormat322.decode(v1).contexts.isEmpty(),
                "Old V1 transfers still readable");
        Map<String,ContextBackupFormat322.Counts> pairs=Map.of(
                "please:tell",new ContextBackupFormat322.Counts(2,4),
                "like:customword",new ContextBackupFormat322.Counts(0,3));
        String text=ContextBackupFormat322.encode(List.of("Customword"),
                Map.of("tekk:tell",2),pairs);
        ok(text.startsWith(ContextBackupFormat322.HEADER+"\n"),
                "version 2 explicit header");
        ok(text.contains("CTX\tplease\ttell\t2\t4\n"),
                "compact counts and no full sentence");
        var decoded=ContextBackupFormat322.decode(text);
        ok(decoded.legacy.words.equals(List.of("Customword")),
                "legacy saved words preserved");
        ok(decoded.legacy.learnedPairs.get("tekk:tell")==2,
                "legacy correction count preserved");
        ok(decoded.contexts.get("please:tell").used==4,
                "used-word evidence retained");
        ok(decoded.contexts.get("like:customword").accepted==0,
                "zero tap, positive used count permitted");
        ok(text.equals(ContextBackupFormat322.encode(
                decoded.legacy.words,decoded.legacy.learnedPairs,
                decoded.contexts)),"deterministic reexport");
        reject(()->ContextBackupFormat322.decode(text.replace("tell","tall")),
                "tampering refused by checksum");
        reject(()->ContextBackupFormat322.encode(List.of(),Map.of(),
                Map.of("personName:tell",new ContextBackupFormat322.Counts(1,1))),
                "private names not context keys");
        reject(()->ContextBackupFormat322.encode(List.of(),Map.of(),
                Map.of("please:tell",new ContextBackupFormat322.Counts(0,0))),
                "no empty evidence");
        reject(()->ContextBackupFormat322.encode(List.of(),Map.of(),
                Map.of("please:hello@example.com",
                        new ContextBackupFormat322.Counts(1,0))),
                "emails never a contextual word");
        reject(()->ContextBackupFormat322.decode(
                text.replace("CTX\tplease\ttell\t2\t4","CTX\tplease\ttell\t3\t4")),
                "corrupted counters refused");
        ok(LearningTransferCodec.decode(v1).contextualPairs==null,
                "never alter existing v1 Bundle layout");
        System.out.println("PASS ContextBackupFormat322Test "+n+" checks");
    }
}
