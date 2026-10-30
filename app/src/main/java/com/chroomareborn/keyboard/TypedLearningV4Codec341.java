package com.chroomareborn.keyboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Research codec for one future all-in-one user-exported V4 backup.
 *
 * V1/V2/V3 imports decode as legacy with no saved typed-context records.
 * V4 is not understood by old Candidate 340, so the V3 exporter remains
 * untouched until version migration and phone tests are explicitly qualified.
 * No Android storage, background backups, or data collection in this class.
 */
final class TypedLearningV4Codec341 {
    static final String HEADER="ORIVRA_LOCAL_LEARNING\t4";
    private static final String FOOTER="SHA256\t";
    private TypedLearningV4Codec341(){}

    static final class Data {
        final FrequencyBackupFormat330.Data previous;
        final Map<String,Integer> words;
        final Map<String,Integer> pairs;
        Data(FrequencyBackupFormat330.Data base,
                Map<String,Integer> savedWords,Map<String,Integer> savedPairs) {
            previous=base;
            words=Collections.unmodifiableMap(new TreeMap<>(savedWords));
            pairs=Collections.unmodifiableMap(new TreeMap<>(savedPairs));
        }
    }

    private static String digest(String text) {
        try {
            byte[] sha=MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder(64);
            for(byte b:sha){
                hex.append(Character.forDigit((b>>>4)&15,16));
                hex.append(Character.forDigit(b&15,16));
            }
            return hex.toString();
        }catch(Exception noProvider){throw new IllegalStateException(noProvider);}
    }

    private static void requireAnchoredPairs(Map<String,Integer> words,
            Map<String,Integer> pairs) {
        for(String pair:pairs.keySet()) {
            int split=pair.indexOf(' ');
            if(split<1 || !words.containsKey(pair.substring(0,split)) ||
                    !words.containsKey(pair.substring(split+1)))
                throw new IllegalArgumentException(
                        "Word pair missing confirmed component vocabulary");
        }
    }

    static String encode(String legacyBackup,Map<String,Integer> words,
            Map<String,Integer> pairs) {
        if(legacyBackup==null || words==null || pairs==null ||
                words.size()>TypedSequenceArchive341.MAX_WORDS ||
                pairs.size()>TypedSequenceArchive341.MAX_PAIRS)
            throw new IllegalArgumentException("Invalid V4 backup data");
        // Normalize and cryptographically validate the old portion first.
        FrequencyBackupFormat330.Data previous=
                FrequencyBackupFormat330.decode(legacyBackup);
        String canonicalV3=FrequencyBackupFormat330.encode(
                previous.earlier,previous.frequencies);
        int footer=canonicalV3.lastIndexOf(FOOTER);
        if(footer<=0)throw new IllegalArgumentException("Malformed V3 base");
        TreeMap<String,Integer> orderedWords=new TreeMap<>();
        for(Map.Entry<String,Integer> e:words.entrySet()){
            TypedSequenceArchive341.verifyWord(e.getKey(),e.getValue());
            if(orderedWords.putIfAbsent(e.getKey(),e.getValue())!=null)
                throw new IllegalArgumentException("Duplicate word");
        }
        TreeMap<String,Integer> orderedPairs=new TreeMap<>();
        for(Map.Entry<String,Integer> e:pairs.entrySet()){
            TypedSequenceArchive341.verifyPair(e.getKey(),e.getValue());
            if(orderedPairs.putIfAbsent(e.getKey(),e.getValue())!=null)
                throw new IllegalArgumentException("Duplicate pair");
        }
        requireAnchoredPairs(orderedWords,orderedPairs);
        StringBuilder body=new StringBuilder(HEADER).append('\n')
                .append(canonicalV3,FrequencyBackupFormat330.HEADER.length()+1,footer);
        for(Map.Entry<String,Integer> e:orderedWords.entrySet())
            body.append("VOC\t").append(e.getKey()).append('\t')
                    .append(e.getValue()).append('\n');
        for(Map.Entry<String,Integer> e:orderedPairs.entrySet()){
            int space=e.getKey().indexOf(' ');
            body.append("NXT\t").append(e.getKey(),0,space).append('\t')
                    .append(e.getKey().substring(space+1)).append('\t')
                    .append(e.getValue()).append('\n');
        }
        String output=body+FOOTER+digest(body.toString())+"\n";
        if(output.getBytes(StandardCharsets.UTF_8).length>
                LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Backup too large");
        return output;
    }

    private static int count(String raw){
        if(raw==null || !raw.matches("[1-9][0-9]{0,3}"))
            throw new IllegalArgumentException("Invalid evidence count");
        try {
            int x=Integer.parseInt(raw);
            if(x<TypedSequenceArchive341.MIN_EVIDENCE ||
                    x>TypedSequenceArchive341.MAX_COUNT)
                throw new IllegalArgumentException("Evidence count out of bounds");
            return x;
        }catch(NumberFormatException invalid){
            throw new IllegalArgumentException("Evidence count overflow",invalid);
        }
    }

    static Data decode(String content) {
        if(content==null)throw new IllegalArgumentException("Missing backup");
        if(!content.startsWith(HEADER+"\n")) {
            // Compatible one-file migration FROM existing V1, V2 and V3 only.
            return new Data(FrequencyBackupFormat330.decode(content),
                    Map.of(),Map.of());
        }
        if(!content.endsWith("\n") || content.indexOf('\r')>=0 ||
                content.indexOf('\uFEFF')>=0 ||
                content.getBytes(StandardCharsets.UTF_8).length>
                        LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IllegalArgumentException("Malformed V4 backup");
        int footer=content.lastIndexOf(FOOTER);
        if(footer<=HEADER.length() || !content.substring(footer)
                .matches("SHA256\\t[0-9a-f]{64}\\n"))
            throw new IllegalArgumentException("Missing V4 checksum");
        String body=content.substring(0,footer);
        String expected=content.substring(footer+FOOTER.length(),footer+FOOTER.length()+64);
        if(!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                digest(body).getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("V4 checksum mismatch");
        StringBuilder v3Body=new StringBuilder(FrequencyBackupFormat330.HEADER)
                .append('\n');
        TreeMap<String,Integer> words=new TreeMap<>();
        TreeMap<String,Integer> pairs=new TreeMap<>();
        String[] lines=body.substring(HEADER.length()+1).split("\n",-1);
        for(int i=0;i<lines.length;i++) {
            String line=lines[i];
            if(line.isEmpty()){
                if(i!=lines.length-1)throw new IllegalArgumentException("Blank V4 record");
                continue;
            }
            String[] fields=line.split("\t",-1);
            if(fields.length>=1 && fields[0].equals("VOC")) {
                if(fields.length!=3)throw new IllegalArgumentException("Malformed vocabulary");
                int n=count(fields[2]);
                TypedSequenceArchive341.verifyWord(fields[1],n);
                if(words.putIfAbsent(fields[1],n)!=null ||
                        words.size()>TypedSequenceArchive341.MAX_WORDS)
                    throw new IllegalArgumentException("Duplicate or excess vocabulary");
            } else if(fields.length>=1 && fields[0].equals("NXT")) {
                if(fields.length!=4)throw new IllegalArgumentException("Malformed pair");
                int n=count(fields[3]);
                String key=fields[1]+" "+fields[2];
                TypedSequenceArchive341.verifyPair(key,n);
                if(pairs.putIfAbsent(key,n)!=null ||
                        pairs.size()>TypedSequenceArchive341.MAX_PAIRS)
                    throw new IllegalArgumentException("Duplicate or excess pairs");
            } else {
                // Must be a valid legacy record; V3 validation rejects any
                // unknown records before an Android importer could touch data.
                v3Body.append(line).append('\n');
            }
        }
        requireAnchoredPairs(words,pairs);
        String canonicalV3=v3Body+FOOTER+digest(v3Body.toString())+"\n";
        FrequencyBackupFormat330.Data earlier=
                FrequencyBackupFormat330.decode(canonicalV3);
        return new Data(earlier,words,pairs);
    }
}
