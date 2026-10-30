package com.chroomareborn.keyboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** No Android APIs, device files, actual editor contents, or user data. */
public final class LearningTransferCodecTest {
    private static int assertions;
    private static void yes(boolean value,String description) {
        assertions++;
        if(!value)throw new AssertionError(description);
    }
    private static void fail(Runnable action,String why) {
        boolean rejected=false;
        try {action.run();}
        catch(IllegalArgumentException expected){rejected=true;}
        yes(rejected,why);
    }
    private static String signed(String payload)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(
                payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex=new StringBuilder(64);
        for(byte b:digest)hex.append(String.format("%02x",b&0xff));
        return payload+"SHA256\t"+hex+"\n";
    }
    public static void main(String[] args)throws Exception {
        Map<String,Integer> pairs=new LinkedHashMap<>();
        pairs.put("cant:can't",3);
        pairs.put("aint:ain't",2);
        pairs.put("helo:hello",4);
        List<String> words=List.of("O’Connor","Ain't","Javi","D'Arcy");
        String content=LearningTransferCodec.encode(words,pairs);
        yes(content.startsWith(LearningTransferCodec.HEADER+"\n"),
                "versioned header present");
        yes(content.endsWith("\n"),"canonical LF footer");
        yes(content.contains("WORD\tO’Connor\n"),"curly display preserved");
        yes(content.contains("PAIR\tcant\tcan't\t3\n"),
                "accepted correction retained without original sentences");
        yes(!content.contains("EDITOR")&&!content.contains("PASSWORD"),
                "format exports explicit words and word-pairs only");
        LearningTransferCodec.Bundle parsed=LearningTransferCodec.decode(content);
        yes(parsed.words.size()==4,"all manual words round-trip");
        yes(parsed.words.contains("O’Connor"),"proper name punctuation round-trip");
        yes(parsed.learnedPairs.size()==3,"all correction pairs round-trip");
        yes(parsed.learnedPairs.get("aint:ain't")==2,"accepted count round-trip");
        yes(parsed.learnedPairs.get("cant:can't")==3,"reinforced contractions round-trip");
        yes(parsed.learnedPairs.get("helo:hello")==4,"V1 correction count survives");
        yes(content.equals(LearningTransferCodec.encode(
                parsed.words,parsed.learnedPairs)),"deterministic canonical reexport");
        fail(()->LearningTransferCodec.decode(null),"null backup refused");
        fail(()->LearningTransferCodec.decode(""),"empty backup refused");
        fail(()->LearningTransferCodec.decode(content.replace("cant","bank")),
                "tampering fails checksum");
        fail(()->LearningTransferCodec.decode(content.substring(0,content.length()-1)),
                "truncated footer rejected");
        fail(()->LearningTransferCodec.decode(content.replace("\n","\r\n")),
                "unexpected CRLF rejected");
        fail(()->LearningTransferCodec.encode(List.of("can't","can’t"),
                Map.of()),"duplicate straight/curly manual words refused");
        fail(()->LearningTransferCodec.encode(List.of("john@example.com"),
                Map.of()),"email is not a manual dictionary word");
        fail(()->LearningTransferCodec.encode(List.of("my secret word"),
                Map.of()),"full sentences not exported");
        fail(()->LearningTransferCodec.encode(List.of("5'10"),
                Map.of()),"height/number not a dictionary word");
        fail(()->LearningTransferCodec.encode(List.of("Hello\nWorld"),
                Map.of()),"embedded newline not exported");
        fail(()->LearningTransferCodec.encode(List.of(" can't"),
                Map.of()),"whitespace-padded display rejected");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("cant:can't",0)),"zero count not exportable");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("cant:can't",1001)),"count cap enforced");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("can't:can’t",3)),"canonical same-word pair rejected");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("a@b.com:clean",2)),"email in learned pair refused");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("hello world:world",2)),"sentence in learned pair refused");
        fail(()->LearningTransferCodec.encode(List.of(),
                Map.of("cant:can’t",2,"cant:can't",2)),
                "curly duplicate pair never double-counted");

        fail(()->LearningTransferCodec.decode(
                "ORIVRA_LOCAL_LEARNING\t2\nSHA256\t"+"0".repeat(64)+"\n"),
                "future incompatible version rejected");
        fail(()->LearningTransferCodec.decode("x".repeat(
                LearningTransferCodec.MAX_UTF8_BYTES+1)),
                "oversized incoming document rejected before parsing");
        fail(()->LearningTransferCodec.decode("\uFEFF"+content),
                "BOM rejected to avoid ambiguous parsing");
        fail(()->LearningTransferCodec.decode(content.replace(
                "WORD\tJavi","WORD\tJa\tvi")),"unexpected fields rejected");
        fail(()->LearningTransferCodec.decode(content.replace(
                "PAIR\thelo\thello\t4","PAIR\thelo\thello\tNaN")),
                "non-numeric learned count rejected");

        String empty=LearningTransferCodec.encode(List.of(),Map.of());
        yes(LearningTransferCodec.decode(empty).words.isEmpty(),
                "empty backup permitted without creating user data");
        yes(LearningTransferCodec.decode(empty).learnedPairs.isEmpty(),
                "empty correction history permitted");
        String malformedPair=signed(LearningTransferCodec.HEADER+
                "\nPAIR\tcant\tcan t\t2\n");
        fail(()->LearningTransferCodec.decode(malformedPair),
                "even correctly checksummed whole-phrase pair rejected");
        String dup=signed(LearningTransferCodec.HEADER+
                "\nWORD\tcan't\nWORD\tcan’t\n");
        fail(()->LearningTransferCodec.decode(dup),
                "even correctly checksummed duplicate canonical word rejected");
        String unknown=signed(LearningTransferCodec.HEADER+
                "\nEDITOR\tprivate text\n");
        fail(()->LearningTransferCodec.decode(unknown),
                "unknown record kind must never enter private storage");
        String mixed=signed(LearningTransferCodec.HEADER+
                "\nWORD\tAin’t\nPAIR\tfoo\tbar\t2\n");
        LearningTransferCodec.Bundle fromSigned=LearningTransferCodec.decode(mixed);
        yes(fromSigned.words.contains("Ain’t"),"signed curly-name parsed");
        yes(fromSigned.learnedPairs.get("foo:bar")==2,
                "signed user-selected correction parsed");

        // Maximum allowed record counts with valid alphabetic tokens.
        List<String> maximumWords=new ArrayList<>();
        for(int a=0;a<10;a++)for(int b=0;b<25;b++)
            maximumWords.add("saved"+(char)('a'+a)+(char)('a'+b));
        Map<String,Integer> maximumPairs=new LinkedHashMap<>();
        for(int a=0;a<12;a++)for(int b=0;b<25;b++){
            String token="typed"+(char)('a'+a)+(char)('a'+b);
            maximumPairs.put(token+":chosen"+(char)('a'+a)+(char)('a'+b),2);
        }
        String max=LearningTransferCodec.encode(maximumWords,maximumPairs);
        LearningTransferCodec.Bundle limit=LearningTransferCodec.decode(max);
        yes(limit.words.size()==250,"maximum 250 manual words accepted");
        yes(limit.learnedPairs.size()==300,"maximum 300 learned pairs accepted");
        yes(max.getBytes(StandardCharsets.UTF_8).length<100_000,
                "maximum valid export remains well within byte budget");
        fail(()->{
            List<String> tooMany=new ArrayList<>(maximumWords);
            tooMany.add("extra");
            LearningTransferCodec.encode(tooMany,Map.of());
        },"251 manual words rejected");
        fail(()->{
            Map<String,Integer> tooMany=new LinkedHashMap<>(maximumPairs);
            tooMany.put("extrap:extrar",2);
            LearningTransferCodec.encode(List.of(),tooMany);
        },"301 correction pairs rejected");
        System.out.println("PASS: LearningTransferCodecTest "+assertions+
                " synthetic assertions");
    }
}
