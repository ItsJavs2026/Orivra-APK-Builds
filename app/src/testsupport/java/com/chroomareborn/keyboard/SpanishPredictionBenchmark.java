package com.chroomareborn.keyboard;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Synthetic Spanish accuracy diagnostics. This is NOT device acceptance. */
public final class SpanishPredictionBenchmark {
    private static final String[] PHRASES={
        "buenos días","buenas noches","hola cómo estás",
        "muchas gracias","estoy en casa","quiero saber",
        "necesito ayuda","gracias por tu ayuda","hasta mañana",
        "el teclado funciona","la pantalla está bien",
        "estoy trabajando","quiero aprender","quiero ver",
        "la batería funciona","mi teléfono está bien",
        "vamos a casa","voy a trabajar",
        "el mensaje está bien","la aplicación funciona",
        "me gusta mucho","te quiero mucho",
        "el coche funciona","estoy aquí",
        "muy buenos días","está muy bien",
        "el teléfono funciona","el trabajo está bien",
        "quiero mejorar","gracias por todo",
        "la casa está bien","tu teléfono funciona",
        "mi amigo trabaja","mi familia está bien",
        "vamos a probar","quiero hacer algo",
        "necesito saber","la pantalla es grande",
        "el teclado es nuevo","el programa funciona"
    };
    private static final String[] WORDS={
        "hola","gracias","estoy","quiero","teclado",
        "teléfono","pantalla","casa","mañana","trabajo",
        "batería","aplicación","mensaje","mejor","grande",
        "amigo","familia","funciona","probar","café",
        "pregunta","respuesta","días","noches","español",
        "necesito","ayuda","correcto","bien","nuevo"
    };
    private static final class Stats {
        final String group;int count,one,three;final List<String> miss=new ArrayList<>();
        Stats(String group){this.group=group;}
        void read(String typed,String target,List<String> result){
            count++;
            if(!result.isEmpty() && norm(result.get(0)).equals(norm(target)))one++;
            boolean ok=false;
            for(int i=0;i<Math.min(3,result.size());i++)
                if(norm(result.get(i)).equals(norm(target))){ok=true;break;}
            if(ok)three++;
            else if(miss.size()<5)miss.add(typed+" => "+
                    (result.isEmpty()?"(none)":result.get(0))+
                    " [expected "+target+"]");
        }
    }
    private static String norm(String s){
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");
    }
    private static String compact(String s){
        return s.replaceAll("[^\\p{L}]","");
    }
    private static String typo(String s,int index,int count){
        char[] arr=s.toCharArray();
        for(int i=0;i<count;i++) {
            int at=1+Math.floorMod(index*13+i*17+3,arr.length-2);
            char old=arr[at];
            arr[at]=old=='e'?'r':old=='a'?'s':old=='o'?'i':
                    old=='i'?'u':old=='t'?'r':old=='n'?'m':'e';
        }
        return new String(arr);
    }
    private static void record(Map<String,Stats> all,String group,
                               String input,String expected) {
        List<String> guesses=BilingualPredictionRouter.suggestions(input,
                BilingualPredictionRouter.Language.SPANISH,false);
        all.computeIfAbsent(group,Stats::new).read(input,expected,guesses);
    }
    public static void main(String[] args){
        Map<String,Stats> all=new LinkedHashMap<>();
        for(int i=0;i<PHRASES.length;i++) {
            String target=PHRASES[i],raw=compact(target);
            record(all,"joined_exact",raw,target);
            record(all,"joined_1_typo",typo(raw,i+3,1),target);
            record(all,"joined_2_typos",typo(raw,i+7,2),target);
        }
        for(int i=0;i<WORDS.length;i++) {
            record(all,"whole_word",WORDS[i],WORDS[i]);
            record(all,"word_1_typo",typo(WORDS[i],i+9,1),WORDS[i]);
        }
        int total=0,one=0,three=0;
        for(Stats s:all.values()){
            total+=s.count;one+=s.one;three+=s.three;
            System.out.printf(Locale.US,
                    "SPANISH %-17s n=%3d top1=%3d (%.1f%%) top3=%3d (%.1f%%)%n",
                    s.group,s.count,s.one,100.0*s.one/s.count,
                    s.three,100.0*s.three/s.count);
            for(String miss:s.miss)System.out.println(" SPANISH_MISS "+miss);
        }
        System.out.printf(Locale.US,
                "SPANISH ALL                 n=%3d top1=%3d (%.1f%%) top3=%3d (%.1f%%)%n",
                total,one,100.0*one/total,three,100.0*three/total);
        if(total<160)throw new AssertionError("Spanish research coverage dropped");
    }
}
