package com.chroomareborn.keyboard;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Research 392: read-back verification after explicit V4 export, and an
 * aggregate on-device learning health report. No file contents, learned
 * words, URIs, hashes or exception messages ever leave this helper.
 *
 * Never invoked by the IME or input thread; caller uses Settings' IO worker.
 * No mutation of learning, consent, documents other than the one document
 * the user explicitly selected to export.
 */
final class LearningBackupSafety392 {
    private LearningBackupSafety392(){}

    enum ExportState {
        VERIFIED, WRITTEN_NOT_VERIFIED, SAVED_BYTES_DIFFER,
        WRITE_FAILED
    }
    static final class ExportResult {
        final ExportState state;
        ExportResult(ExportState status){state=status;}
        String display(){
            switch(state){
                case VERIFIED:
                    return "V4 backup saved and verified: exact file contents and checksum match.";
                case WRITTEN_NOT_VERIFIED:
                    return "V4 was written, but Android could not reopen it to verify. "+
                            "Keep previous backups; check this file with the offline verifier.";
                case SAVED_BYTES_DIFFER:
                    return "WARNING: saved V4 differs from the intended backup or fails "+
                            "checksum verification. Keep previous backups and export a new copy.";
                case WRITE_FAILED:
                default:
                    return "V4 backup could not be completed. Keep all existing backups and "+
                            "do not erase learning.";
            }
        }
    }

    /** No writes. A failed read-back does NOT imply the original user data is bad. */
    static ExportResult verifyWritten(Context context,Uri selected,String expected){
        if(context==null || selected==null || expected==null)
            return new ExportResult(ExportState.WRITTEN_NOT_VERIFIED);
        try{
            String actual=LearningTransferDocumentIO.read(context,selected);
            if(!expected.equals(actual))
                return new ExportResult(ExportState.SAVED_BYTES_DIFFER);
            // Decode again to reject an accidentally damaged expected payload.
            TypedLearningV4Codec341.decode(actual);
            return new ExportResult(ExportState.VERIFIED);
        }catch(IOException | SecurityException unreadable){
            return new ExportResult(ExportState.WRITTEN_NOT_VERIFIED);
        }catch(RuntimeException invalid){
            return new ExportResult(ExportState.SAVED_BYTES_DIFFER);
        }
    }

    static ExportResult exportAndVerify(Context context,Uri selected){
        if(context==null || selected==null)
            return new ExportResult(ExportState.WRITE_FAILED);
        try{
            String encoded=TypedLearningV4Transfer347.exportV4(context);
            TypedLearningV4Codec341.decode(encoded);
            LearningTransferDocumentIO.write(context,selected,encoded);
            return verifyWritten(context,selected,encoded);
        }catch(Exception cannotWrite){
            return new ExportResult(ExportState.WRITE_FAILED);
        }
    }

    static final class Health {
        final boolean valid;
        final int dictionary,corrections,contexts,frequencies,typedWords,typedPairs,bytes;
        Health(boolean ok,int d,int c,int ctx,int freq,int tw,int tp,int size){
            valid=ok;dictionary=d;corrections=c;contexts=ctx;
            frequencies=freq;typedWords=tw;typedPairs=tp;bytes=size;
        }
        static Health unavailable(){return new Health(false,0,0,0,0,0,0,0);}
        private static boolean near(int used,int max){
            // 80% warning threshold; no confidence or privacy inferences.
            return used>=0 && max>0 && (long)used*5>=(long)max*4;
        }
        boolean nearingCapacity(){
            return valid && (
                near(dictionary,PersonalDictionaryPolicy.MAX_WORDS) ||
                near(corrections,LearningCapacity390.MAX_CORRECTIONS) ||
                near(contexts,LearningCapacity390.MAX_CONTEXT_KEYS) ||
                near(frequencies,LearningCapacity390.MAX_FREQUENCIES) ||
                near(typedWords,TypedSequenceArchive341.MAX_WORDS) ||
                near(typedPairs,TypedSequenceArchive341.MAX_PAIRS) ||
                near(bytes,LearningTransferCodec.MAX_UTF8_BYTES));
        }
        String display(){
            if(!valid)return "Backup health could not be validated. No data was changed. "+
                    "Keep existing V4 files and avoid erasing learning.";
            String message="On-device V4 learning health (aggregate counts only):\n"+
                    "Dictionary: "+dictionary+"/"+PersonalDictionaryPolicy.MAX_WORDS+"\n"+
                    "Corrections: "+corrections+"/"+LearningCapacity390.MAX_CORRECTIONS+"\n"+
                    "Context patterns: "+contexts+"/"+LearningCapacity390.MAX_CONTEXT_KEYS+"\n"+
                    "Word frequencies: "+frequencies+"/"+LearningCapacity390.MAX_FREQUENCIES+"\n"+
                    "Typed vocabulary: "+typedWords+"/"+TypedSequenceArchive341.MAX_WORDS+"\n"+
                    "Typed pairs: "+typedPairs+"/"+TypedSequenceArchive341.MAX_PAIRS+"\n"+
                    "Export size: "+bytes+"/"+LearningTransferCodec.MAX_UTF8_BYTES+" bytes.";
            return message+(nearingCapacity()?
                "\nSome categories are at least 80% full. Save a NEW V4 backup "+
                "and keep previous copies before further migration.":
                "\nAll categories are below the 80% advisory threshold.")+
                "\nThis is a read-only snapshot, not proof of a previously "+
                "exported file's integrity.";
        }
    }

    static Health inspect(Context context){
        if(context==null)return Health.unavailable();
        try{
            String encoded=TypedLearningV4Transfer347.exportV4(context);
            TypedLearningV4Codec341.Data data=
                    TypedLearningV4Codec341.decode(encoded);
            return new Health(true,
                    data.previous.earlier.legacy.words.size(),
                    data.previous.earlier.legacy.learnedPairs.size(),
                    data.previous.earlier.contexts.size(),
                    data.previous.frequencies.size(),data.words.size(),
                    data.pairs.size(),
                    encoded.getBytes(StandardCharsets.UTF_8).length);
        }catch(RuntimeException failure){
            return Health.unavailable();
        }
    }
}
