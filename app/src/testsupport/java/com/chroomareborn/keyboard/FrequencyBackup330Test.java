package com.chroomareborn.keyboard;

import java.util.Map;
import java.util.List;
public final class FrequencyBackup330Test {
    static int assertions;
    static void ok(boolean v,String reason){assertions++;
        if(!v)throw new AssertionError(reason);}
    static void reject(Runnable fn,String reason){
        boolean fails=false;
        try{fn.run();}catch(IllegalArgumentException ex){fails=true;}
        ok(fails,reason);
    }
    public static void main(String[] args) {
        String v1=LearningTransferCodec.encode(
                List.of("Customword"),Map.of("tekk:tell",2));
        String v2=ContextBackupFormat322.encode(
                List.of("Customword"),Map.of("tekk:tell",2),
                Map.of("please:tell",new ContextBackupFormat322.Counts(2,4)));
        var before=FrequencyBackupFormat330.decode(v1);
        ok(before.frequencies.isEmpty(),"V1 imports without frequencies");
        ok(FrequencyBackupFormat330.decode(v2).frequencies.isEmpty(),
                "V2 imports without frequencies");
        var previous=ContextBackupFormat322.decode(v2);
        String v3=FrequencyBackupFormat330.encode(previous,
                Map.of("continue",7,"my",9,"can't",5));
        ok(v3.startsWith(FrequencyBackupFormat330.HEADER+"\n"),
                "separate V3 format header");
        ok(v3.contains("FRQ\tcontinue\t7\n"),"completed-word evidence survives");
        ok(v3.contains("FRQ\tmy\t9\n"),"short words survive");
        ok(v3.contains("FRQ\tcan't\t5\n"),"contractions survive");
        var decoded=FrequencyBackupFormat330.decode(v3);
        ok(decoded.frequencies.equals(Map.of(
                "continue",7,"my",9,"can't",5)),"frequency roundtrip");
        ok(decoded.earlier.legacy.words.equals(List.of("Customword")),
                "V1 dictionary survives V3");
        ok(decoded.earlier.legacy.learnedPairs.get("tekk:tell")==2,
                "accepted corrections survive V3");
        ok(decoded.earlier.contexts.get("please:tell").used==4,
                "V2 contextual usage survives V3");
        ok(v3.equals(FrequencyBackupFormat330.encode(
                decoded.earlier,decoded.frequencies)),
                "V3 is deterministic across devices");
        reject(()->FrequencyBackupFormat330.decode(
                v3.replace("FRQ\tcontinue\t7","FRQ\tcontinue\t9")),
                "tamper with counts rejected");
        reject(()->FrequencyBackupFormat330.encode(previous,
                Map.of("account@example.com",6)),
                "email / account cannot be a word");
        reject(()->FrequencyBackupFormat330.encode(previous,
                Map.of("continue",0)),"zero evidence rejected");
        reject(()->FrequencyBackupFormat330.encode(previous,
                Map.of("continue",1001)),"overlimit counts rejected");
        reject(()->FrequencyBackupFormat330.encode(previous,
                Map.of("Continue",3)),"canonical lowercase required");
        ok(LearningTransferCodec.decode(v1).words.size()==1,
                "old V1 decoder remains untouched");
        ok(ContextBackupFormat322.decode(v2).contexts.size()==1,
                "old V2 decoder remains untouched");
        System.out.println("PASS FrequencyBackup330Test "+assertions+" checks");
    }
}
