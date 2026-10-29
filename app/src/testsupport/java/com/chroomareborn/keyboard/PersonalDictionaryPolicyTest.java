package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Random;

/** Entirely invented words; no personal typing or source corpus. */
public final class PersonalDictionaryPolicyTest {
    private static int n;
    private static void ok(boolean yes,String msg){n++;if(!yes)throw new AssertionError(msg);}
    public static void main(String[] args){
        for(String rejected:new String[]{"","x","two words","1234","hello_world","it's",
            "éclair","hello!","word1","x".repeat(25)}) {
            ok(PersonalDictionaryPolicy.canonical(rejected)==null,"invalid manual word");
        }
        ok("orivra".equals(PersonalDictionaryPolicy.canonical(" Orivra ")),"case-fold");
        ok("Orivra".equals(PersonalDictionaryPolicy.acceptedDisplay(" Orivra ")),"display case");
        ok(PersonalDictionaryPolicy.merge("hi",List.of("hi"),List.of("Orivra")).equals(List.of("hi")),
           "unrelated manual word cannot affect existing word");
        ok(PersonalDictionaryPolicy.merge("ori",List.of("orbit","origin"),List.of("Orivra"))
            .equals(List.of("Orivra","orbit","origin")),"saved word first on unfamiliar prefix");
        ok(PersonalDictionaryPolicy.merge("the",List.of("the","the car"),
           List.of("Theodore")).equals(List.of("the","Theodore","the car")),
           "known words remain top");
        ok(PersonalDictionaryPolicy.merge("Orivra",List.of("Orion","original"),
            List.of("Orivra")).get(0).equals("Orivra"),
            "saved exact proper word must be suggested");
        ok(PersonalDictionaryPolicy.merge("OR",List.of("or"),List.of("Orivra")).get(1)
            .equals("ORIVRA"),"uppercase display preserved after known literal");
        ok(PersonalDictionaryPolicy.merge("ori",List.of("orion","orion"),
            List.of("Orivra","orivra","Origin")).size()==3,
            "personal and engine candidate deduplicated");
        ok(PersonalDictionaryPolicy.merge("bad text",List.of("default"),List.of("Orivra"))
            .equals(List.of("default")),"no personal completion for sentences");
        ok(PersonalDictionaryPolicy.merge("ori",List.of("origin"),List.of("Orivra",
            "Orivria","Orin","Oriole","Orizaba","Orion","Orinda","Oribita",
            "Orianna","Oriflamme")).size()<=8,"candidate list cap");
        ok(PersonalDictionaryPolicy.merge("ora",List.of("orange"),List.of())
            .equals(List.of("orange")),"no entries preserve predictor");
        Random rng=new Random(285);
        for(int i=0;i<10000;i++){
            StringBuilder prefix=new StringBuilder("ab");
            for(int j=0;j<rng.nextInt(6);j++)prefix.append((char)('a'+rng.nextInt(26)));
            String word=prefix+"rocket";
            List<String> originals=List.of("about","above","absolutely","abortion");
            var out=PersonalDictionaryPolicy.merge(prefix.toString(),originals,List.of(word));
            ok(out.size()<=8&&out.get(0).equals(word),"manual candidate deterministic");
            ok(out.contains("about"),"underlying predictor first alternative retained somewhere");
            ok(out.stream().distinct().count()==out.size(),"no duplicate suggestions");
        }
        System.out.println("BUILD285_PERSONAL_DICTIONARY_POLICY checks="+n+" passed");
    }
}
