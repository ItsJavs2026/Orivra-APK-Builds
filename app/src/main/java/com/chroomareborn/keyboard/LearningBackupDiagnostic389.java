package com.chroomareborn.keyboard;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.util.Locale;

/**
 * Research 389: local-only, no-write backup diagnosis. Input may contain
 * highly private vocabulary; no file contents, paths, hashes, exception
 * messages, or individual words are persisted, logged, transmitted or shown.
 * Import and existing local learning are never mutated by this checker.
 */
final class LearningBackupDiagnostic389 {
    private LearningBackupDiagnostic389(){}

    enum Status {
        READY,FILE_ACCESS,FILE_TOO_LARGE,BAD_ENCODING,
        WRONG_FILE,CHECKSUM,INVALID_RECORDS,LOCAL_MERGE_REJECTED
    }
    static final class Check {
        final Status status;
        final int words,corrections,contexts,frequencies,typedWords,typedPairs;
        Check(Status s,int w,int c,int x,int f,int tw,int tp){
            status=s;words=w;corrections=c;contexts=x;frequencies=f;
            typedWords=tw;typedPairs=tp;
        }
        String message(){
            switch(status){
                case READY:
                    return "The selected Orivra backup passed its format, checksum "
                            +"and current-store merge preview. No data was changed.\n\n"
                            +"It contains "+words+" saved dictionary words, "+
                            corrections+" corrections, "+contexts+" context patterns, "+
                            frequencies+" word-frequency records, "+typedWords+
                            " typed-word records and "+typedPairs+" typed-word pairs.\n\n"
                            +"Use Import V4 only if these are the records you intend "
                            +"to merge. Import will still request confirmation.";
                case FILE_ACCESS:
                    return "The document could not be opened or read. Check its "
                            +"location/permissions and try a different saved copy. "
                            +"No learning data was changed.";
                case FILE_TOO_LARGE:
                    return "The backup exceeds Orivra's safe 100 KB import limit. "
                            +"Do not erase learning or truncate the file. Preserve "
                            +"the original for a capacity-safe recovery. Nothing changed.";
                case BAD_ENCODING:
                    return "The file isn't valid UTF-8 plain text. Re-export an "
                            +"untouched V4 file from the Orivra version holding your "
                            +"learning. Do not edit or resave the original.";
                case WRONG_FILE:
                    return "This isn't a recognized Orivra V1/V2/V3/V4 learning "
                            +"file. Select the original .txt export rather than an "
                            +"APK, screenshot, ZIP or unrelated document.";
                case CHECKSUM:
                    return "The backup integrity checksum does not match, or its "
                            +"line endings/encoding were altered. Keep the original "
                            +"and make a NEW export from the source Orivra install; "
                            +"do not edit the backup text.";
                case INVALID_RECORDS:
                    return "The Orivra file was recognized, but its record format "
                            +"or evidence consistency was rejected. Keep the file "
                            +"unchanged; a safe compatibility fix needs testing.";
                case LOCAL_MERGE_REJECTED:
                    return "The file decoded, but the current app's existing local "
                            +"learning could not pass the non-writing merge preview "
                            +"(capacity or incompatible stored evidence). Keep both "
                            +"the file and your installed app. No data was changed.";
                default:return "Backup check did not complete. No changes were made.";
            }
        }
    }
    private static Check status(Status state){
        return new Check(state,0,0,0,0,0,0);
    }
    static Check readFailure(IOException e){
        final String name=e==null?"":e.getMessage();
        if("Document exceeds safe import size".equals(name))
            return status(Status.FILE_TOO_LARGE);
        if("Invalid UTF-8 document".equals(name))
            return status(Status.BAD_ENCODING);
        return status(Status.FILE_ACCESS);
    }
    static Check checkDocument(Context ctx,Uri selected){
        final String contents;
        try {contents=LearningTransferDocumentIO.read(ctx,selected);}
        catch(IOException e){return readFailure(e);}
        catch(RuntimeException ignored){return status(Status.FILE_ACCESS);}
        return checkText(ctx,contents);
    }
    static Check checkText(Context ctx,String raw){
        // Strict heading checks only; never expose other contents in diagnostics.
        if(raw==null)return status(Status.WRONG_FILE);
        // CRLF or BOM modifications must be reported as altered Orivra
        // format/integrity, never mislabeled as an unrelated file.
        boolean bom=!raw.isEmpty() && raw.charAt(0)==0xFEFF;
        String headerView=bom?raw.substring(1):raw;
        if(!headerView.startsWith("ORIVRA_LOCAL_LEARNING"+(char)9))
            return status(Status.WRONG_FILE);
        if(bom || raw.indexOf((char)13)>=0)return status(Status.CHECKSUM);
        boolean known=headerView.startsWith(LearningTransferCodec.HEADER+(char)10) ||
                headerView.startsWith(ContextBackupFormat322.HEADER+(char)10) ||
                headerView.startsWith(FrequencyBackupFormat330.HEADER+(char)10) ||
                headerView.startsWith(TypedLearningV4Codec341.HEADER+(char)10);
        if(!known)return status(Status.WRONG_FILE);
        final TypedLearningV4Codec341.Data parsed;
        try{parsed=TypedLearningV4Codec341.decode(raw);}
        catch(RuntimeException rejected){
            String internal=rejected.getMessage();
            String hint=internal==null?"":internal.toLowerCase(Locale.ROOT);
            if(hint.contains("checksum") || hint.contains("sha256"))
                return status(Status.CHECKSUM);
            // V4 decode rejects BOM and CRLF before checking SHA256.
            if(raw.indexOf('\r')>=0 || raw.indexOf('\uFEFF')>=0)
                return status(Status.CHECKSUM);
            return status(Status.INVALID_RECORDS);
        }
        try {
            TypedLearningV4Transfer347.preview(ctx,parsed);
        }catch(RuntimeException invalidLocalMerge){
            return status(Status.LOCAL_MERGE_REJECTED);
        }
        return new Check(Status.READY,
                parsed.previous.earlier.legacy.words.size(),
                parsed.previous.earlier.legacy.learnedPairs.size(),
                parsed.previous.earlier.contexts.size(),
                parsed.previous.frequencies.size(),
                parsed.words.size(),parsed.pairs.size());
    }
}
