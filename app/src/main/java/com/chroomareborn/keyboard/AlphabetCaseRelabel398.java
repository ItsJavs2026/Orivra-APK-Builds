package com.chroomareborn.keyboard;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Research 398: modify only labels in a validated mounted ABC keyboard.
 * No removals, listener replacement, geometry changes, input/IME events,
 * personal words, or editor writes. Caller falls back to full refresh if
 * the displayed keyboard is not exactly the known alphabet structure.
 */
final class AlphabetCaseRelabel398 {
    private AlphabetCaseRelabel398() {}

    static boolean relabel(LinearLayout keys,boolean shift,boolean capsLock) {
        if(keys==null || keys.getChildCount()!=4)return false;
        final String[] rows=KeyboardLayoutSpec.rows(KeyboardLayoutSpec.ABC);
        final Button[][] letters=new Button[3][];
        final String[][] hints=new String[3][];
        Button shiftKey=null;
        // Phase one: validate the entire expected tree BEFORE mutating views.
        // A numeric, symbol, resized/unmounted or host-replaced structure
        // fails closed rather than partially repainting the keyboard.
        for(int rowIndex=0;rowIndex<rows.length;rowIndex++){
            View root=keys.getChildAt(rowIndex);
            if(!(root instanceof LinearLayout))return false;
            LinearLayout row=(LinearLayout)root;
            int offset=rowIndex==2?1:0;
            if(row.getChildCount()!=rows[rowIndex].length()+(rowIndex==2?2:0))
                return false;
            if(rowIndex==2){
                View first=row.getChildAt(0);
                if(!(first instanceof Button))return false;
                shiftKey=(Button)first;
                if(!TextUtils.equals(shiftKey.getText(),"⇧") &&
                        !TextUtils.equals(shiftKey.getText(),"⇪"))
                    return false;
            }
            letters[rowIndex]=new Button[rows[rowIndex].length()];
            hints[rowIndex]=new String[rows[rowIndex].length()];
            for(int col=0;col<rows[rowIndex].length();col++){
                View cellView=row.getChildAt(col+offset);
                if(!(cellView instanceof FrameLayout))return false;
                FrameLayout cell=(FrameLayout)cellView;
                if(cell.getChildCount()!=2 ||
                        !(cell.getChildAt(0) instanceof Button) ||
                        !(cell.getChildAt(1) instanceof TextView))
                    return false;
                Button button=(Button)cell.getChildAt(0);
                TextView hintView=(TextView)cell.getChildAt(1);
                String letter=String.valueOf(rows[rowIndex].charAt(col));
                String capital=letter.toUpperCase(java.util.Locale.ROOT);
                String hint=KeyboardLayoutSpec.longPressHint(rowIndex,col);
                if(hint==null || !TextUtils.equals(hintView.getText(),hint) ||
                        (!TextUtils.equals(button.getText(),letter) &&
                         !TextUtils.equals(button.getText(),capital)))
                    return false;
                letters[rowIndex][col]=button;
                hints[rowIndex][col]=hint;
            }
        }
        if(shiftKey==null)return false;
        // Phase two: only TextView text/a11y state, never replace a touch key.
        for(int rowIndex=0;rowIndex<rows.length;rowIndex++) {
            for(int col=0;col<rows[rowIndex].length();col++) {
                Button key=letters[rowIndex][col];
                String letter=String.valueOf(rows[rowIndex].charAt(col));
                String shown=shift?letter.toUpperCase(java.util.Locale.ROOT):letter;
                if(!TextUtils.equals(key.getText(),shown))key.setText(shown);
                String description=shown+", hold for "+hints[rowIndex][col];
                if(!TextUtils.equals(key.getContentDescription(),description))
                    key.setContentDescription(description);
            }
        }
        String modifier=capsLock?"⇪":"⇧";
        if(!TextUtils.equals(shiftKey.getText(),modifier))shiftKey.setText(modifier);
        if(shiftKey.isSelected()!=capsLock)shiftKey.setSelected(capsLock);
        return true;
    }
}
