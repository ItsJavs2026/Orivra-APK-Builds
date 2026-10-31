package com.chroomareborn.keyboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Research-only portable context format. V1 export/import remains untouched.
 * This codec is NOT connected to the live Android importer until exhaustive
 * version, checksum, privacy and merge tests qualify the migration.
 */
final class ContextBackupFormat322 {
    static final String HEADER="ORIVRA_LOCAL_LEARNING\t2";
    static final int MAX_CONTEXT=LearningCapacity390.MAX_CONTEXT_KEYS;
    private ContextBackupFormat322(){}

    static final class Counts {
        final int accepted,used;
        Counts(int a,int u){accepted=a;used=u;}
    }
    static final class Data {
        final LearningTransferCodec.Bundle legacy;
        final Map<String,Counts> contexts;
        Data(LearningTransferCodec.Bundle l,Map<String,Counts> c){
            legacy=l;contexts=Collections.unmodifiableMap(new TreeMap<>(c));
        }
    }
    private static String checksum(String text) {
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
                    text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(byte b:bytes)out.append(String.format("%02x",b&255));
            return out.toString();
        }catch(Exception unavailable){throw new IllegalStateException(unavailable);}
    }
    private static String key(String raw,int a,int u) {
        if(raw==null || a<0||u<0||a>1000||u>1000||a+u==0)
            throw new IllegalArgumentException("Invalid context counts");
        int sep=raw.indexOf(':');
        if(sep<1||sep!=raw.lastIndexOf(':'))
            throw new IllegalArgumentException("Invalid context key");
        String left=AcceptedContextPolicy319.safeWord(raw.substring(0,sep));
        String right=AcceptedContextPolicy319.acceptedWord(raw.substring(sep+1));
        if(left==null||right==null||left.equals(right))
            throw new IllegalArgumentException("Invalid contextual words");
        return left+":"+right;
    }
    static String encode(java.util.List<String> words,Map<String,Integer> corrections,
            Map<String,Counts> contexts) {
        if(contexts==null||contexts.size()>MAX_CONTEXT)
            throw new IllegalArgumentException("Too many context records");
        String v1=LearningTransferCodec.encode(words,corrections);
        int footer=v1.lastIndexOf("SHA256\t");
        StringBuilder body=new StringBuilder(HEADER).append('\n')
                .append(v1,LearningTransferCodec.HEADER.length()+1,footer);
        TreeMap<String,Counts> canonical=new TreeMap<>();
        for(Map.Entry<String,Counts> entry:contexts.entrySet()) {
            Counts v=entry.getValue();
            if(v==null)throw new IllegalArgumentException("Missing counts");
            String k=key(entry.getKey(),v.accepted,v.used);
            if(canonical.putIfAbsent(k,v)!=null)
                throw new IllegalArgumentException("Duplicate context");
        }
        for(Map.Entry<String,Counts> e:canonical.entrySet()) {
            int sep=e.getKey().indexOf(':');
            body.append("CTX\t").append(e.getKey(),0,sep).append('\t')
                .append(e.getKey().substring(sep+1)).append('\t')
                .append(e.getValue().accepted).append('\t')
                .append(e.getValue().used).append('\n');
        }
        String result=body+"SHA256\t"+checksum(body.toString())+"\n";
        if(result.getBytes(StandardCharsets.UTF_8).length>
                LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Backup too large");
        return result;
    }
    static Data decode(String text) {
        if(text==null)throw new IllegalArgumentException("No backup");
        if(text.startsWith(LearningTransferCodec.HEADER+"\n")) {
            return new Data(LearningTransferCodec.decode(text),Map.of());
        }
        if(!text.startsWith(HEADER+"\n") || !text.endsWith("\n") ||
                text.indexOf('\r')>=0 || text.indexOf('\uFEFF')>=0 ||
                text.getBytes(StandardCharsets.UTF_8).length>
                        LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Unknown portable backup");
        int footer=text.lastIndexOf("SHA256\t");
        if(footer<0 || footer==0)throw new IllegalArgumentException("Missing checksum");
        String body=text.substring(0,footer),end=text.substring(footer);
        if(!end.matches("SHA256\\t[0-9a-f]{64}\\n") ||
                !MessageDigest.isEqual(checksum(body).getBytes(StandardCharsets.US_ASCII),
                    end.substring(7,71).getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("Corrupted or forged context");
        String[] lines=body.split("\n",-1);
        TreeMap<String,Counts> contexts=new TreeMap<>();
        StringBuilder v1=new StringBuilder(LearningTransferCodec.HEADER).append('\n');
        for(int i=1;i<lines.length;i++){
            String line=lines[i];
            if(line.isEmpty())continue;
            String[] parts=line.split("\t",-1);
            if(parts.length==5 && parts[0].equals("CTX")) {
                int a,u;
                try{
                    a=Integer.parseInt(parts[3]);u=Integer.parseInt(parts[4]);
                }catch(NumberFormatException bad){
                    throw new IllegalArgumentException("Invalid counts");
                }
                String normalized=key(parts[1]+":"+parts[2],a,u);
                if(contexts.putIfAbsent(normalized,new Counts(a,u))!=null ||
                        contexts.size()>MAX_CONTEXT)
                    throw new IllegalArgumentException("Duplicate context");
            }else {
                v1.append(line).append('\n');
            }
        }
        String known=v1+"SHA256\t"+checksum(v1.toString())+"\n";
        LearningTransferCodec.Bundle legacy=LearningTransferCodec.decode(known);
        return new Data(legacy,contexts);
    }
}
