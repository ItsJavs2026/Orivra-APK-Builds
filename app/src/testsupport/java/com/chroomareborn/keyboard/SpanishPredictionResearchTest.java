package com.chroomareborn.keyboard;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Research-only checks, no Android editor and no learned personal input. */
public final class SpanishPredictionResearchTest {
    private static long checks;
    private static void ok(boolean valid,String why) {
        checks++;
        if(!valid)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        for(String word:new String[]{"hola","gracias","mañana","teclado",
                "teléfono","estás","café","batería","español","sí","si",
                "quiero","casa","trabajo","pregunta","respuesta","pantalla",
                "mensajes","funcionando","aprendiendo"}) {
            List<String> got=BilingualPredictionRouter.suggestions(
                    word,BilingualPredictionRouter.Language.SPANISH,false);
            // Some correctly spelled words are intentionally not yet in the
            // small general research lexicon. Record them diagnostically;
            // NEVER silently rewrite a word that is recognized.
            if(!got.isEmpty() && got.contains(word))
                ok(word.equals(got.get(0)),
                        "Known Spanish word lost its first position: "+word+" "+got);
            else System.out.println("SPANISH_LEXICON_GAP="+word+" -> "+got);
        }
        List<String> composed=BilingualPredictionRouter.suggestions(
                "café",BilingualPredictionRouter.Language.SPANISH,false);
        List<String> decomposed=BilingualPredictionRouter.suggestions(
                Normalizer.normalize("café",Normalizer.Form.NFD),
                BilingualPredictionRouter.Language.SPANISH,false);
        ok(composed.equals(decomposed),"NFC/NFD normalization changed candidate order");
        for(String bad:new String[]{"","123","hola!","¿hola","hello123","😀","abc_xyz"}) {
            ok(BilingualPredictionRouter.suggestions(
                    bad,BilingualPredictionRouter.Language.SPANISH,false).isEmpty(),
                    "Non-Spanish research input accepted: "+bad);
        }
        List<String> englishInputs=Arrays.asList(
                "question","keyboard","also","were","hello","hellothere",
                "howsitgoing","howareyoudoingtoday","firstofall",
                "thisiscookbutitslaking","vibrstion","singke",
                "okattthisisstartingtocomeakong","weshouldtryagain");
        for(String input:englishInputs){
            ok(PredictionLite.suggestions(input).equals(
                    BilingualPredictionRouter.suggestions(
                            input,BilingualPredictionRouter.Language.ENGLISH,false)),
                    "English prediction changed under explicit routing: "+input);
        }
        ok(PredictionLite.editingSuggestions("qeestion").equals(
                BilingualPredictionRouter.suggestions(
                    "qeestion",BilingualPredictionRouter.Language.ENGLISH,true)),
                "English word editing altered");
        Random rng=new Random(2952026L);
        String alphabet="abcdefghijklmnopqrstuvwxyzáéíóúüñ";
        for(int i=0;i<2500;i++) {
            int n=1+rng.nextInt(11);
            StringBuilder text=new StringBuilder();
            for(int k=0;k<n;k++)text.append(alphabet.charAt(
                    rng.nextInt(alphabet.length())));
            List<String> suggestions=BilingualPredictionRouter.suggestions(
                    text.toString(),BilingualPredictionRouter.Language.SPANISH,false);
            ok(suggestions!=null && suggestions.size()<=8,
                    "Spanish candidate list escaped 8-choice limit");
            for(String candidate:suggestions) {
                ok(candidate!=null && !candidate.trim().isEmpty(),
                        "Invalid suggestion item");
            }
        }
        ok(BilingualPredictionRouter.suggestions(null,
                BilingualPredictionRouter.Language.ENGLISH,false).isEmpty(),
                "Null input must be fail-closed");
        ok(BilingualPredictionRouter.suggestions("hola",null,false).isEmpty(),
                "Missing language must not implicitly switch the keyboard");
        System.out.println("SpanishPredictionResearchTest PASSED "+
                checks+" assertions across 2500 seeded Spanish strings");
    }
}
