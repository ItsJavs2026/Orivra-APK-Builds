package com.chroomareborn.keyboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Opt-in V3 portable format for complete-word frequency. Old V1 and V2
 * formats are read unchanged; the encoder never overwrites those formats.
 * All records/checksums are validated before callers may write preferences.
 */
final class FrequencyBackupFormat330 {
    static final String HEADER="ORIVRA_LOCAL_LEARNING\t3";
    static final int MAX_WORDS=LearningCapacity390.MAX_FREQUENCIES;
    private FrequencyBackupFormat330(){}
    static final class Data {
        final ContextBackupFormat322.Data earlier;
        final Map<String,Integer> frequencies;
        Data(ContextBackupFormat322.Data previous,Map<String,Integer> words) {
            earlier=previous;
            frequencies=Collections.unmodifiableMap(new TreeMap<>(words));
        }
    }
    private static String digest(String text) {
        try {
            byte[] sha=MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder(64);
            for(byte b:sha) {
                result.append(Character.forDigit((b>>>4)&15,16));
                result.append(Character.forDigit(b&15,16));
            }
            return result.toString();
        }catch(Exception cryptoUnavailable){
            throw new IllegalStateException(cryptoUnavailable);
        }
    }
    private static String token(String word,int count) {
        String canonical=EnglishApostrophePolicy.canonical(word);
        if(canonical==null || !canonical.equals(word) ||
                count<1 || count>1000)
            throw new IllegalArgumentException("Invalid frequency word/count");
        return canonical;
    }
    static String encode(ContextBackupFormat322.Data previous,
            Map<String,Integer> learnedWords) {
        if(previous==null || previous.legacy==null ||
                learnedWords==null || learnedWords.size()>MAX_WORDS)
            throw new IllegalArgumentException("Invalid portable word data");
        String legacy=ContextBackupFormat322.encode(
                previous.legacy.words,previous.legacy.learnedPairs,
                previous.contexts);
        int footer=legacy.lastIndexOf("SHA256\t");
        if(footer<=0)throw new IllegalArgumentException("Invalid V2 base");
        StringBuilder body=new StringBuilder(HEADER).append('\n')
                .append(legacy,ContextBackupFormat322.HEADER.length()+1,footer);
        TreeMap<String,Integer> sorted=new TreeMap<>();
        for(Map.Entry<String,Integer> e:learnedWords.entrySet()) {
            if(e.getValue()==null)throw new IllegalArgumentException("Null count");
            String key=token(e.getKey(),e.getValue());
            if(sorted.putIfAbsent(key,e.getValue())!=null)
                throw new IllegalArgumentException("Duplicate frequency");
        }
        for(Map.Entry<String,Integer> e:sorted.entrySet())
            body.append("FRQ\t").append(e.getKey()).append('\t')
                    .append(e.getValue()).append('\n');
        String output=body+"SHA256\t"+digest(body.toString())+"\n";
        if(output.getBytes(StandardCharsets.UTF_8).length>
                LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Backup too large");
        return output;
    }
    static Data decode(String content) {
        if(content==null)throw new IllegalArgumentException("Missing backup");
        if(content.startsWith(LearningTransferCodec.HEADER+"\n") ||
                content.startsWith(ContextBackupFormat322.HEADER+"\n"))
            return new Data(ContextBackupFormat322.decode(content),Map.of());
        if(!content.startsWith(HEADER+"\n") || !content.endsWith("\n") ||
                content.indexOf('\r')>=0 || content.indexOf('\uFEFF')>=0 ||
                content.getBytes(StandardCharsets.UTF_8).length>
                        LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Unknown V3 backup");
        int footer=content.lastIndexOf("SHA256\t");
        if(footer<0)throw new IllegalArgumentException("Checksum missing");
        String body=content.substring(0,footer);
        String check=content.substring(footer);
        if(!check.matches("SHA256\\t[0-9a-f]{64}\\n") ||
                !MessageDigest.isEqual(digest(body).getBytes(StandardCharsets.US_ASCII),
                check.substring(7,71).getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("V3 checksum invalid");
        StringBuilder oldBody=new StringBuilder(ContextBackupFormat322.HEADER)
                .append('\n');
        TreeMap<String,Integer> frequencies=new TreeMap<>();
        String[] lines=body.split("\n",-1);
        for(int i=1;i<lines.length;i++) {
            String line=lines[i];
            if(line.isEmpty())continue;
            String[] fields=line.split("\t",-1);
            if(fields.length==3 && "FRQ".equals(fields[0])) {
                int count;
                try {
                    // Reject signs, whitespace, leading zeros and overflows.
                    if(!fields[2].matches("[1-9][0-9]{0,3}"))
                        throw new NumberFormatException();
                    count=Integer.parseInt(fields[2]);
                }catch(NumberFormatException invalid){
                    throw new IllegalArgumentException("Malformed frequency");
                }
                String key=token(fields[1],count);
                if(frequencies.putIfAbsent(key,count)!=null ||
                        frequencies.size()>MAX_WORDS)
                    throw new IllegalArgumentException("Duplicate/too many words");
            } else oldBody.append(line).append('\n');
        }
        String old=oldBody+"SHA256\t"+digest(oldBody.toString())+"\n";
        ContextBackupFormat322.Data verified=ContextBackupFormat322.decode(old);
        return new Data(verified,frequencies);
    }
}
