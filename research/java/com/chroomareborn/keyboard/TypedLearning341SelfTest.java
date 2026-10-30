package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Map;

/** Standalone pure-Java regression executable; no Android or test deps. */
public final class TypedLearning341SelfTest {
    private static int assertions;
    private static void check(boolean yes,String message){
        assertions++;
        if(!yes)throw new AssertionError(message);
    }
    private static void reject(Runnable call,String label){
        assertions++;
        try { call.run(); }
        catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected rejection: "+label);
    }
    private static String oldV1(){
        return LearningTransferCodec.encode(List.of("Orivra"),Map.of());
    }
    private static String oldV3(){
        ContextBackupFormat322.Data v2=ContextBackupFormat322.decode(
                ContextBackupFormat322.encode(
                        List.of("Orivra"),Map.of(),Map.of()));
        return FrequencyBackupFormat330.encode(v2,Map.of("continue",4));
    }
    private static void verifyEvidence(){
        TypedSequenceArchive341 a=new TypedSequenceArchive341();
        for(int repeat=0;repeat<3;repeat++){
            a.resetEditor();
            for(String w:new String[]{"I","need","to","continue"}){
                a.observeVerifiedSpace(w,true,true,true,true);
            }
        }
        check(a.eligibleWords().size()==4,"I, need, to, continue learned");
        check(a.eligibleWords().get("continue")==3,"three repeats");
        check(a.eligiblePairs().get("to continue")==3,"pair evidence");
        check(a.nextWords("to",3).equals(List.of("continue")),"next-word suggestion");
        check(a.nextWords("need",3).equals(List.of("to")),"short word learned");
        check(a.nextWords("continue",3).isEmpty(),"no cross-session candidate");
        a.resetEditor();
        a.observeVerifiedSpace("private",true,true,false,true);
        a.observeVerifiedSpace("nosave",true,false,true,true);
        a.observeVerifiedSpace("nosave",false,true,true,true);
        a.observeVerifiedSpace("nosave",true,true,true,false);
        check(!a.eligibleWords().containsKey("private"),"private editor not retained");
        check(!a.eligibleWords().containsKey("nosave"),"unconsented/rejected not retained");
        for(String unsafe:new String[]{"123456","name@example.com","https://site",
                "zip/code","x y","dont.","O'Neil","\\passwd","🙂","A1"}){
            check(TypedSequenceArchive341.canonical(unsafe)==null,
                    "unsafe lexical form rejected: "+unsafe);
        }
        check(TypedSequenceArchive341.canonical("a").equals("a"),"a accepted");
        check(TypedSequenceArchive341.canonical("I").equals("i"),"I accepted");
        check(TypedSequenceArchive341.canonical("Continue").equals("continue"),
                "ordinary capitalization accepted");
        // This lexical acceptance deliberately does not guarantee that a
        // capitalized word is not a private name. Separate consent is required.
        check(TypedSequenceArchive341.canonical("Greg").equals("greg"),
                "privacy needs separate consent beyond lexical normalization");
        check(TypedSequenceArchive341.canonical("can't").equals("can't"),
                "public contraction");
        check(TypedSequenceArchive341.canonical("Greg's")==null,
                "unknown possessive never stored");
        check(TypedSequenceArchive341.canonical("x")==null,
                "one-letter word except a/i not retained");
        reject(()->a.mergeValidated(Map.of("email@x",3),Map.of()),
                "invalid new word import");
        check(a.eligibleWords().get("continue")==3,
                "invalid import cannot mutate archive");
        reject(()->a.mergeValidated(Map.of("safe",3),
                Map.of("to example",2)),"invalid pair preflight");
        check(!a.eligibleWords().containsKey("safe"),
                "all import parts validated before changes");
        a.mergeValidated(Map.of("continue",5),
                Map.of("to continue",6));
        check(a.eligibleWords().get("continue")==5,"merge uses max");
        check(a.eligiblePairs().get("to continue")==6,"pair max");
        a.mergeValidated(Map.of("continue",3),
                Map.of("to continue",3));
        check(a.eligibleWords().get("continue")==5,
                "duplicate backup cannot inflate frequency");
        reject(()->a.mergeValidated(Map.of("unrelated",3),
                Map.of("unrelated phrase",3)),
                "rejected pair must not introduce unsupported successor");
        check(!a.eligibleWords().containsKey("unrelated"),
                "invalid pair import is fully rolled back");
        a.clearAll();
        check(a.observedVocabularyCount()==0 && a.observedPairCount()==0,
                "clear removes all archive state");
    }
    private static void verifyBackup(){
        String base=oldV3();
        Map<String,Integer> w=Map.of("continue",5,"orivra",3,"i",4,
                "need",3,"to",4);
        Map<String,Integer> p=Map.of("i need",3,"need to",4,"to continue",5);
        String out=TypedLearningV4Codec341.encode(base,w,p);
        check(out.startsWith(TypedLearningV4Codec341.HEADER+"\n"),
                "V4 labeled as new, not pretending to be V3");
        TypedLearningV4Codec341.Data saved=TypedLearningV4Codec341.decode(out);
        check(saved.words.equals(w),"roundtrip vocabulary");
        check(saved.pairs.equals(p),"roundtrip pairs");
        check(saved.previous.frequencies.get("continue")==4,
                "V3 frequency retained");
        check(saved.previous.earlier.legacy.words.contains("Orivra"),
                "old manually saved word retained");
        check(out.equals(TypedLearningV4Codec341.encode(base,w,p)),
                "deterministic bytes");
        check(TypedLearningV4Codec341.decode(oldV1()).words.isEmpty(),
                "old V1 is imported safely");
        check(TypedLearningV4Codec341.decode(base).pairs.isEmpty(),
                "old V3 imports without new context");
        String oldV2=ContextBackupFormat322.encode(
                List.of("Orivra"),Map.of(),Map.of());
        check(TypedLearningV4Codec341.decode(oldV2).words.isEmpty(),
                "old V2 imports");
        reject(()->TypedLearningV4Codec341.decode(out.replace(
                "NXT\tto\tcontinue","NXT\tto\tcontinued")),
                "tampered V4 checksum");
        reject(()->TypedLearningV4Codec341.decode(
                out.replace("\n","\r\n")),"reject CRLF");
        reject(()->TypedLearningV4Codec341.decode(out.substring(0,out.length()-1)),
                "truncated backup");
        reject(()->TypedLearningV4Codec341.encode(base,
                Map.of("person@host",3),Map.of()),"invalid identity payload");
        reject(()->TypedLearningV4Codec341.encode(base,
                Map.of("i",3),Map.of("i need",3)),
                "pair without repeated evidence for both words");
        reject(()->TypedLearningV4Codec341.encode(base,
                Map.of("continue",2),Map.of()),"insufficient evidence");
        reject(()->TypedLearningV4Codec341.encode(base,
                Map.of(),Map.of("to continue",1001)),"invalid count");
        reject(()->TypedLearningV4Codec341.encode(base,
                Map.of(),Map.of("to continue too",3)),"no raw 3-word sentence");
        reject(()->TypedLearningV4Codec341.decode("ORIVRA_LOCAL_LEARNING\t4\n"),
                "empty V4 cannot pass checksum");
    }
    public static void main(String[] args){
        verifyEvidence();verifyBackup();
        System.out.println("PASS: TypedLearning341SelfTest ("+assertions+
                " evidence, consent, migration, checksum and bounded-merge assertions)");
    }
}
