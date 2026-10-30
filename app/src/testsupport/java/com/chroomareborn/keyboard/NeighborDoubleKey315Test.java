package com.chroomareborn.keyboard;

import java.util.Map;
import java.util.List;

/** Synthetic error cases, including the user-reported Build 310 "tekk". */
public final class NeighborDoubleKey315Test {
    static int cases;
    static void ok(boolean p,String message){cases++;if(!p)throw new AssertionError(message);}
    public static void main(String[] args){
        Map<String,Integer> dictionary=Map.of(
                "tell",18,"will",50,"sill",16,"well",60,
                "test",30,"text",30,"world",40,"book",40);
        ok("tell".equals(NeighborDoubleKeyCorrectionPolicy.unique(
                "tekk",dictionary)),"neighbor k->l twice yields tell");
        ok("will".equals(NeighborDoubleKeyCorrectionPolicy.unique(
                "wikk",dictionary)),"general doubled-key neighbor correction");
        ok(NeighborDoubleKeyCorrectionPolicy.unique("tell",dictionary)==null,
                "known correct word protected");
        ok(NeighborDoubleKeyCorrectionPolicy.unique("tekk",Map.of(
                "tell",18,"tejj",18))==null,"competing neighboring words abstain");
        ok(NeighborDoubleKeyCorrectionPolicy.unique("tekk",Map.of(
                "tell",2))==null,"rare unsupported words not promoted");
        for(String raw:new String[]{
                "hello world","x","te","tekk1","test@example.com",
                "httpssite","tekl","book","te\u0301kk","abcdefghijklmno",
                "tekk "})ok(NeighborDoubleKeyCorrectionPolicy.unique(raw,dictionary)==null,
                "never correct invalid or non-double words");
        List<String> predicted=PredictionLite.suggestions("tekk");
        ok(!predicted.isEmpty() && "tell".equalsIgnoreCase(predicted.get(0)),
                "end-to-end Build 310 user case top suggestion = tell");
        ok(predicted.contains("tekk"),"typed literal retained as option");
        List<String> editing=PredictionLite.editingSuggestions("tekk");
        ok(editing.size()>1 && editing.contains("tell") &&
                "tekk".equals(editing.get(0)),
                "committed-text search editor: optional tell, raw spelling first");
        ok("tell".equalsIgnoreCase(
                PredictionLite.suggestions("tell").get(0)),
                "correctly spelled tell never split");
        ok("will".equalsIgnoreCase(
                PredictionLite.suggestions("will").get(0)),
                "known English words remain stable");
        System.out.println("PASS: NeighborDoubleKey315Test "+cases+" checks");
    }
}
