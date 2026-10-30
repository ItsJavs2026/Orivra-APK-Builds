package com.chroomareborn.keyboard;

import java.util.Map;

/**
 * Research 316: modest, curated English everyday-word tier.
 * Written as general source vocabulary, NOT copied from a third-party list,
 * actual user typing, private messages, contact names, or passwords.
 *
 * Prior known words keep their frequencies. New entries have modest evidence
 * (14), allowing full-word recognition and conservative fuzzy candidates.
 * Do not insert generated suffixes or preserve full user sentences.
 */
final class EverydayEnglishCore316 {
    private EverydayEnglishCore316() {}
    private static final String CONNECTIONANDGRAMMAR =
            "such under within between during however although though besides despite perhaps rather instead otherwise unless until since among against beyond around inside outside upon whether whereas regarding including including specific certain necessary additional general overall entire various several";
    private static final String COMMUNICATIONANDWRITING =
            "contact post posts posted comments comment policy policies privacy rights links link title description content copy quote source author subject reference resources report reporting discussion questions question reply response responses provide provided provides request requests mention mentioned mentions include includes included compare comparison notes note details detail print sign signed document documents statement statements";
    private static final String DAILYCONVERSATION =
            "must being put someone else others already already okay anyways already together right later meet meets met meeting meetups else also still during below above mostly especially really actually certainly probably probably maybe often rarely usually never always great sure nice enough much less little more almost still plus rather simply already";
    private static final String LIFEANDWORK =
            "member members group groups community staff team teams manager managers employee employees client clients customer customers office work project projects task tasks process processes progress products product order orders delivery delivering shipping store shop shopping package packages payment payments receipt receipts budget budgets pay paid cost costs purchase purchases sell selling sale sales quality standard standards performance job jobs professional career careers school college course courses";
    private static final String TECHNOLOGYANDAPPS =
            "web site sites click clicked clicking network networks mobile digital media technology technical security login register logged account accounts online search browser browse browsing settings option options profile profiles status version versions hardware software server servers screen screens device devices model models file files directory directories application applications app apps tools tool support supported supporting update updates privacy storage database databases device link links downloaded uploads upload download downloaded wireless power electronics settings text typing keyboard";
    private static final String INFORMATIONANDMEASUREMENT =
            "state states fact facts general research analysis result results method methods study studies studies type types data information info reference details explanation descriptions design designs based basis category categories section sections size sizes level levels amount amounts total totals per percent percentage period periods previous related next following major minor average value values figure figures effect effects condition conditions requirements requirement necessary possible process series history category change changes";
    private static final String SOCIALCULTURE =
            "human people person personal individual individuals public private community society family families friends friend groups sports sport game games music movie movies audio radio photo photos video videos art arts activity activities event events club clubs language english spanish english learning teaching reading writing news blog blogs article articles story stories special usual normal ordinary natural";
    private static final String EVERYDAYACTIONS =
            "create creates created creating apply applies applied applying increase increases increased increasing decrease decreases decreased decreasing improve improves improved improving control controls controlled controlling include includes included including publish published publishing prepare prepared preparing review reviewed reviewing submit submitted submitting select selected selecting compare compared comparing display displays displayed displaying design designed designing share shared sharing connect connected connecting check checked checking open opened opening close closed closing receive received receiving deliver delivered delivering";
    static void enrich(Map<String,Integer> words) {
        if(words==null)return;
        String[] groups={
                CONNECTIONANDGRAMMAR,
                COMMUNICATIONANDWRITING,
                DAILYCONVERSATION,
                LIFEANDWORK,
                TECHNOLOGYANDAPPS,
                INFORMATIONANDMEASUREMENT,
                SOCIALCULTURE,
                EVERYDAYACTIONS
        };
        for(String group:groups) for(String word:group.split(" ")) {
            if(word.matches("[a-z]{2,20}"))words.putIfAbsent(word,8);
        }
    }
}
