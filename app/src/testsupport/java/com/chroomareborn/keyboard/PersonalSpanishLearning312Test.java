package com.chroomareborn.keyboard;
import java.util.List;
import java.util.Map;
/** Synthetic English/Spanish learning tests, without real user typing. */
public final class PersonalSpanishLearning312Test {
    private static int n;
    static void yes(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        for(String[] a:new String[][]{
            {"NIÑO","niño"},{"mañana","mañana"},{"Camión","camión"},
            {"camio\u0301n","camión"},{"CORAZÓN","corazón"},
            {"D’Arcy","d'arcy"},{"CAN’T","can't"},{"pingüino","pingüino"}})
            yes(a[1].equals(PersonalWordCanonical.canonical(a[0])),"NFC EN/ES words");
        for(String bad:new String[]{"","a","hello world","1234","http://site",
            "a@b.com","5'10","can''t","-mañana","mañana!","camión\nsecret",
            "camión\tsecret","camión🙂","abc_def","hello-world",
            "mañana:hello","字典","x".repeat(33)})
            yes(PersonalWordCanonical.canonical(bad)==null,"reject unsafe word");
        yes("can'".equals(PersonalWordCanonical.prefix("can’")),"apostrophe prefix");
        yes("cam".equals(PersonalWordCanonical.prefix("cam")),"Spanish prefix");
        yes(PersonalWordCanonical.prefix("can''")==null,"reject repeated marks");
        yes(PersonalDictionaryPolicy.merge("cam",List.of("cam"),
            List.of("camión")).equals(List.of("cam","camión")),"tap-only save");
        LearningV2Policy.Pair one=LearningV2Policy.extract("camion","camión");
        yes(one!=null&&one.typed.equals("camion")&&
            one.accepted.equals("camión"),"explicit Spanish pair");
        LearningV2Policy.Pair sentence=LearningV2Policy.extract(
            "quiero el camion mañana","quiero el camión mañana");
        yes(sentence!=null&&sentence.typed.equals("camion")&&
            sentence.accepted.equals("camión"),"one edited token only");
        yes(LearningV2Policy.extract("mañana voy","tarde vamos")==null,
            "two changed words excluded");
        yes(LearningV2Policy.extract("camionmañana","camión mañana")==null,
            "joined phrase not learned");
        yes(LearningV2Policy.preferred("camion",Map.of("camión",1))==null,
            "one acceptance never promotes");
        yes("camión".equals(LearningV2Policy.preferred(
            "camion",Map.of("camión",2))),"reinforced Spanish correction");
        yes("CAMIÓN".equals(LearningV2Policy.preferred(
            "CAMION",Map.of("camión",2))),"Spanish upper case");
        yes(LearningV2Policy.preferred("camion",Map.of(
            "camión",2,"camino",2))==null,"tie cannot auto-promote");
        yes("can't".equals(LearningV2Policy.preferred(
            "cant",Map.of("can't",2))),"English V1 unchanged");
        String backup=LearningTransferCodec.encode(
            List.of("Niño"),Map.of("camion:camión",2));
        var parsed=LearningTransferCodec.decode(backup);
        yes(parsed.words.contains("Niño")&&
            parsed.learnedPairs.get("camion:camión")==2,
            "backward-compatible v1 backup");
        yes(!backup.contains("quiero el camion"),"no sentence storage");
        System.out.println("PASS: PersonalSpanishLearning312Test "+n+" cases");
    }
}
