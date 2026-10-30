package com.chroomareborn.keyboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Research-only portable local-learning format for USER-INITIATED export/import.
 *
 * This never reads editors, keystrokes, network or passwords. It accepts ONLY
 * manually saved words and explicit accepted correction pairs. It is NOT
 * encrypted or authenticated: backups must be treated as private documents.
 *
 * v1 grammar (UTF-8, LF only, bounded):
 * ORIVRA_LOCAL_LEARNING<tab>1
 * WORD<tab>O'Connor
 * PAIR<tab>cant<tab>can't<tab>3
 * SHA256<tab>hex-of-all-previous-lines-including-final-LF
 */
final class LearningTransferCodec {
    static final String HEADER="ORIVRA_LOCAL_LEARNING\t1";
    static final int MAX_UTF8_BYTES=100_000;
    static final int MAX_WORDS=PersonalDictionaryPolicy.MAX_WORDS;
    static final int MAX_PAIRS=300;
    private static final String SEP="\t";
    private LearningTransferCodec(){}

    static final class Bundle {
        final List<String> words;
        final Map<String,Integer> learnedPairs;
        Bundle(List<String> manual,Map<String,Integer> learned){
            words=Collections.unmodifiableList(new ArrayList<>(manual));
            learnedPairs=Collections.unmodifiableMap(new LinkedHashMap<>(learned));
        }
    }

    private static String checksum(String input) {
        try {
            byte[] value=MessageDigest.getInstance("SHA-256").digest(
                    input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(64);
            for(byte b:value){
                out.append(Character.forDigit((b>>>4)&15,16));
                out.append(Character.forDigit(b&15,16));
            }
            return out.toString();
        }catch(NoSuchAlgorithmException unavailable){
            throw new IllegalStateException("SHA-256 unavailable",unavailable);
        }
    }

    private static String exactDisplay(String input) {
        if(input==null||!input.equals(input.trim())||
                PersonalDictionaryPolicy.canonical(input)==null)
            throw new IllegalArgumentException("Invalid saved word");
        return input;
    }
    private static String normalizedPair(String typed,String accepted,int count) {
        String from=EnglishApostrophePolicy.canonical(typed);
        String to=EnglishApostrophePolicy.canonical(accepted);
        if(from==null||to==null||from.equals(to)||count<1||count>1000)
            throw new IllegalArgumentException("Invalid learned word pair");
        return from+":"+to;
    }

    /** All data comes from explicit user-managed vocab and accepted pairs. */
    static String encode(List<String> words,Map<String,Integer> learned) {
        if(words==null||learned==null||words.size()>MAX_WORDS||
                learned.size()>MAX_PAIRS)
            throw new IllegalArgumentException("Too many or missing records");
        TreeMap<String,String> canonicalWords=new TreeMap<>();
        for(String word:words){
            String display=exactDisplay(word);
            String key=PersonalDictionaryPolicy.canonical(display);
            if(canonicalWords.putIfAbsent(key,display)!=null)
                throw new IllegalArgumentException("Duplicate saved word");
        }
        TreeMap<String,Integer> normalized=new TreeMap<>();
        for(Map.Entry<String,Integer> entry:learned.entrySet()){
            String pair=entry.getKey();
            if(pair==null||entry.getValue()==null)
                throw new IllegalArgumentException("Missing learned pair");
            int sep=pair.indexOf(':');
            if(sep<1||sep!=pair.lastIndexOf(':'))
                throw new IllegalArgumentException("Bad learned key");
            String canonical=normalizedPair(
                    pair.substring(0,sep),pair.substring(sep+1),entry.getValue());
            if(normalized.putIfAbsent(canonical,entry.getValue())!=null)
                throw new IllegalArgumentException("Duplicate correction");
        }
        StringBuilder body=new StringBuilder(HEADER).append('\n');
        for(String value:canonicalWords.values())
            body.append("WORD").append(SEP).append(value).append('\n');
        for(Map.Entry<String,Integer> entry:normalized.entrySet()){
            int pos=entry.getKey().indexOf(':');
            body.append("PAIR").append(SEP)
                    .append(entry.getKey(),0,pos).append(SEP)
                    .append(entry.getKey().substring(pos+1)).append(SEP)
                    .append(entry.getValue()).append('\n');
        }
        String encoded=body+"SHA256"+SEP+checksum(body.toString())+"\n";
        if(encoded.getBytes(StandardCharsets.UTF_8).length>MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Backup too large");
        return encoded;
    }

    /** Validate the ENTIRE file before the caller touches any preferences. */
    static Bundle decode(String file) {
        if(file==null||file.getBytes(StandardCharsets.UTF_8).length>MAX_UTF8_BYTES
                ||!file.endsWith("\n")||file.indexOf('\r')>=0||
                file.indexOf('\uFEFF')>=0)
            throw new IllegalArgumentException("Unsupported backup encoding/size");
        String[] lines=file.split("\n",-1);
        if(lines.length<3 ||lines.length>MAX_WORDS+MAX_PAIRS+3 ||
                !HEADER.equals(lines[0])||!lines[lines.length-1].isEmpty())
            throw new IllegalArgumentException("Invalid backup header/record count");
        String footer=lines[lines.length-2];
        if(!footer.matches("SHA256\\t[0-9a-f]{64}"))
            throw new IllegalArgumentException("Missing backup checksum");
        int beginFooter=file.lastIndexOf("SHA256\t");
        if(beginFooter<0||beginFooter!=file.length()-footer.length()-1)
            throw new IllegalArgumentException("Invalid backup footer");
        String payload=file.substring(0,beginFooter);
        String expected=checksum(payload);
        byte[] actualHash=footer.substring(7).getBytes(StandardCharsets.US_ASCII);
        if(!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),actualHash))
            throw new IllegalArgumentException("Corrupted backup checksum");

        TreeMap<String,String> words=new TreeMap<>();
        TreeMap<String,Integer> learned=new TreeMap<>();
        for(int i=1;i<lines.length-2;i++){
            String line=lines[i];
            if(line.length()>128)throw new IllegalArgumentException("Record too long");
            String[] parts=line.split("\t",-1);
            if(parts.length==2&&"WORD".equals(parts[0])) {
                String display=exactDisplay(parts[1]);
                String normalized=PersonalDictionaryPolicy.canonical(display);
                if(words.putIfAbsent(normalized,display)!=null ||
                        words.size()>MAX_WORDS)
                    throw new IllegalArgumentException("Duplicate/too many saved words");
            }else if(parts.length==4&&"PAIR".equals(parts[0])){
                int count;
                try{
                    count=Integer.parseInt(parts[3]);
                }catch(NumberFormatException invalid){
                    throw new IllegalArgumentException("Invalid correction count");
                }
                String pair=normalizedPair(parts[1],parts[2],count);
                if(learned.putIfAbsent(pair,count)!=null ||
                        learned.size()>MAX_PAIRS)
                    throw new IllegalArgumentException("Duplicate/too many corrections");
            }else {
                throw new IllegalArgumentException("Unknown backup record");
            }
        }
        return new Bundle(new ArrayList<>(words.values()),learned);
    }
}
