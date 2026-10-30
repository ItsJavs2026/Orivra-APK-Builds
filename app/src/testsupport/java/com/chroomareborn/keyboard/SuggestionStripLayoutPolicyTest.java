package com.chroomareborn.keyboard;

public final class SuggestionStripLayoutPolicyTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for(int count=0;count<=8;count++) {
            check(SuggestionStripLayoutPolicy.shownCount(count,false)==count,
                    "ordinary count "+count);
            check(SuggestionStripLayoutPolicy.shownCount(count,true)==Math.min(1,count),
                    "sentence count "+count);
            for(boolean emptyInput:new boolean[]{false,true}) {
                int childCount=SuggestionStripLayoutPolicy.childCount(count,emptyInput);
                int next=0, separators=0;
                for(int index=0;index<count;index++) {
                    int position=SuggestionStripLayoutPolicy.chipChildIndex(index,emptyInput);
                    check(position==next,"chip index "+index+" count "+count+
                            " empty "+emptyInput);
                    check(position<childCount,"chip bound");
                    next++;
                    if(SuggestionStripLayoutPolicy.dividerAfter(index,count,emptyInput)) {
                        next++;
                        separators++;
                    }
                }
                check(next==childCount,"child count "+count+" empty "+emptyInput);
                check(separators==(emptyInput?Math.max(0,count-1):Math.max(0,count-3)),
                        "divider count");
            }
        }
        check(SuggestionStripLayoutPolicy.chipChildIndex(-1,false)==-1,
                "negative chip index sentinel");
        for(int count=0;count<=8;count++) {
            check(SuggestionStripLayoutPolicy.centerIdleSuggestions("",count,false)
                            ==(count>0),
                    "idle next-word chips centered only when there are choices");
            check(!SuggestionStripLayoutPolicy.centerIdleSuggestions("hello",count,false),
                    "typed word, correction and sentence strips retain START gravity");
            check(!SuggestionStripLayoutPolicy.centerIdleSuggestions("",count,true),
                    "private fields keep existing protected message layout");
            check(!SuggestionStripLayoutPolicy.centerIdleSuggestions("editing",count,true),
                    "protected editor stays unchanged with composing text");
        }
        check(SuggestionStripLayoutPolicy.centerIdleSuggestions(null,3,false),
                "null empty-composing sentinel centers visible idle choices");
        check(!SuggestionStripLayoutPolicy.centerIdleSuggestions(null,0,false),
                "empty placeholder must never be centered");
        System.out.println("BUILD287_SUGGESTION_LAYOUT checks="+checks+" passed");
    }
}
