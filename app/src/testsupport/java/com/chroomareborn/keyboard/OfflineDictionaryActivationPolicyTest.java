package com.chroomareborn.keyboard;

/**
 * Synthetic gate matrix. This tests opt-in, privacy and stale-loader policy,
 * never actual editor contents or any user-supplied dictionary.
 */
public final class OfflineDictionaryActivationPolicyTest {
    private static int checks=0;
    private static void yes(boolean condition,String message){
        checks++;if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        yes("keyboard_feedback".equals(OfflineDictionaryActivationPolicy.PREFS),
                "existing settings namespace");
        yes("orivra_offline_exact_dictionary_v1".equals(
                OfflineDictionaryActivationPolicy.KEY),"new opt-in namespace");
        for(boolean enabled:new boolean[]{false,true}) {
            for(boolean sensitive:new boolean[]{false,true}) {
                boolean active=enabled && !sensitive;
                yes(OfflineDictionaryActivationPolicy.usable(enabled,sensitive)==active,
                        "only opted-in ordinary fields usable");
                for(boolean loading:new boolean[]{false,true}) {
                    for(boolean ready:new boolean[]{false,true}) {
                        yes(OfflineDictionaryActivationPolicy.startWorker(
                                enabled,sensitive,loading,ready)==
                                (active&&!loading&&!ready),
                                "worker only once when permitted and not ready");
                    }
                }
                for(boolean sameWorker:new boolean[]{false,true}) {
                    for(boolean validPack:new boolean[]{false,true}) {
                        yes(OfflineDictionaryActivationPolicy.acceptCallback(
                                enabled,sensitive,sameWorker,validPack)==
                                (active&&sameWorker&&validPack),
                                "stale worker, malformed pack or private field cannot activate");
                    }
                }
            }
        }
        yes(!OfflineDictionaryActivationPolicy.usable(false,false),
                "default false must not activate");
        yes(!OfflineDictionaryActivationPolicy.usable(true,true),
                "password/URI/nontext/Keep protected editor must not activate");
        yes(OfflineDictionaryActivationPolicy.usable(true,false),
                "normal Keep note must be eligible for opt-in");
        System.out.println("PASS: OfflineDictionaryActivationPolicyTest "+
                checks+" assertions");
    }
}
