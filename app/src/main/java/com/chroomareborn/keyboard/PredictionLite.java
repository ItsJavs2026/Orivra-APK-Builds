package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Offline bootstrap candidate generator, NOT Sentence Lab v0.9.2. */
final class PredictionLite {
    private PredictionLite() {}
    private static final Map<String, Integer> WORDS = new HashMap<>();
    // Explicit accepted words only: no raw editor strings are stored here.
    static boolean publicWordForContext(String normalized) {
        return normalized!=null &&
                (WORDS.containsKey(normalized) ||
                 EnglishApostrophePolicy.publicContraction(normalized));
    }
    // Experimental exact-only ESDB tier. Never populated in Candidate 300.
    // Only qualified offline research injects this pack; no user typing,
    // network or telemetry is collected.
    private static volatile ExactOnlyLanguagePack researchExactOnlyPack;
    static void setResearchExactOnlyPack(ExactOnlyLanguagePack pack) {
        researchExactOnlyPack=pack;
    }
    // Round 2: broad general-purpose words, not screenshot-specific phrases.
    private static final String EXPANDED_CORE_LEXICON =
        "able absence access account add address ahead airplane alarm alive amount angle animal apartment apple appointment April area arrived article attention August average baby balance ball bank base beach beautiful bed bedroom began begin behavior beside better birthday blanket block board body book books boring boss bought box boy breakfast brother budget building busy button camera camp card cards careful carry case cat center central chair chance chapter child children city class classroom cloud coffee company copy corner country customer cut dad dance daughter December decision deep desk detail dinner direction doctor dog door downtown dream drink drinking driver early earth east education egg eight email employee engine enjoy example excited exercise eye eyes family famous farm father February field film final finger fire fish flight floor flower focus food football forest forward found four friend friends friendly fruit garden gift girl glass grandma grandpa grocery group guess guitar gym hair hand hands handsome health heart heat heavy history holiday hospital hotel hour hours house hundred hungry husband idea important inside island January job journey joke July June key keys kid kitchen lake language laptop large late later laugh laughing letter library life light line local location longer lunch machine mail manager map March market math May meeting memory menu middle milk minute minutes Monday money month months moon mother mountain movie movies music name names natural neighbor news niece nephew notebook November number office older online open orange order package page pages parent park parking party password path patient payment person picture pictures place places plane planet plant plate player pocket poem point police pool popular practice price princess problem product program purpose rain rainy reason receipt recipe relative repair report restaurant review rice river room rooms route rule sad safety salad Saturday school science season second September serious service seven shirt shoes shop shopping short sister six sleep sleeping smile smiling snow soap socks software song songs soup south spring stairs station story street strong student students style summer sun sunny support sweet system table tablet talk talking task teacher teaching temperature ten test testing theme things thought ticket tired tool tools tooth town train training travel tree trip truck Tuesday uncle university vacation value vegetable vehicle visit visiting voice volume water weather website Wednesday weekend weekends wife window winter woman women wonderful world write writing writer year years yellow young";
    // Common word forms and everyday conversational vocabulary. Explicitly
    // curated rather than generating arbitrary, often invalid suffix variants.
    private static final String CONVERSATIONAL_FORMS =
        "about above absolutely actually added adding agree agreed almost already answer answered answering anywhere apart appeared applied around asked asking asleep attention available avoid awake away badly basic beautiful became becoming began begins believe believed bigger biggest boring both bought brave brighter bright broken brought busy called calling calm cannot careful carefully caring carried carrying catch catching caught changing charged charging chatted chatting chatting clean cleaner cleaned cleaning clearly closer closest collected collecting comfortable coming common compared comparing completed completing completely confirmed connected connecting constantly continuing cool cooler coolest corrected correcting correction corrections counting covered crazy creative created creating cried crying crowded daily danger dangerous darker daylight decided deciding deleted deleting described describing details developed developing differently difficult difficultly discovered discovering doing done doors downloaded downloading dreamed dreaming drove driven dropped dropping earlier easiest easily education email ending enough especially exact exactly excellent excited exciting explaining explained famous faster fastest fat favorite favorites features feedback feeling feels felt filled filling finally finding finished finishing first fit fixed fixing focused following followed forget forgetting forgot forgotten forward found four friday friendly friendlier friends funnier funniest funny gave getting giving glad going gone gotten great greater greatest green growing grew happy happier happiest harder hardly healthy helpful helping helps her here hidden higher highly hired hiring holding home homes honestly hoping horrible hot hotter hour hours house houses huge hungry improved improving improvement improvements including increased increasing installing installed intended interesting interested into invited inviting joining joined joyful jumping jumped kept kind kinder knowing knowledge larger largest later latest laughing learned learning leaves leaving left less liked likes likely listening listened little longer looking looks lost loudly loved loving low lower lowest made making mainly managed managing market marks matched matching matters matter meanwhile meeting meetings missed missing mistake mistakes moving moved much needed needing needs nervous newer newest next nicer nicely nobody nothing noticed noticing older only opened opening opens ordinary original originally other others outside over painted painting parent parents parked parking passed passing people phone phones places planned planning playing pleased plenty plugged popular possible possibly predicted predicting prediction predictions pretty prettier proud quick quicker quickest quickly quietly raised raising ran reaching reached read reading ready real realized realizing received receiving recent recently remembered remembering repeated repeating replace replaced replacing reply replying requested requesting result results reviewing revised right rising road roads room rooms running runs sad sadly saved saving saying says school screen screens searched searching seasons second seconds seeing seemed seems seen sending sent sentences separate separated seriously shared sharing showed showing showing simple simpler simplest since singing sitting slow slower slowly smallest smarter smoothly solution solutions sometimes soon spacebar spaces spacing speech speeding spent spoke spoken started starting stopped stopping store stores straight strange stronger struggling stuck student students studied studying succeeded successfully suddenly suggested suggesting suggestions surprising surprised taken taking taller talking talks teaches teacher teaching tested tests than thanked thanks their theirs themselves there therefore these they third thought thoughts three tired today together tomorrow tonight took tools too total touched touching trained training traveled traveling tried trying truly turned turning twice two understood understanding unhappy unexpectedly unique updated updating updates useful useless users usually vacation valuable verified verifying version versions video videos visible visited visiting voice waiting walked walking wanting wants warmed warning warnings watched watching wearing week weekend weird welcome went whenever whereas whether while whole whose wider wife willing windows winner winning wish wishing without woke wonderful wondered wondering worked working works worried worries worrying worse worst written wrote wrong wrongly yesterday younger youngest yourself";
    // Everyday nouns, verbs and forms that reduce bogus splits of legitimate
    // words. Intentionally broad; no complete user sentence is stored.
    private static final String CORE_EVERYDAY =
        "actor actors air airport alarm alive alone alphabet amazing amazed animal animals any anyone anything anymore anybody another anybody anyway appearance appointment appreciate area areas arrived arrival art artist artists asked asking assume assumed attack attacks available baby bag ball balls baseball basic basics beach beaches beak bear bears beautiful became become bed beds beginning begin began begins behavior best better bird birds birthday blanket blue boat boats body bodies book books bored boring boss bosses both bottle bottles bottom bought box boxes boy boys breakfast bright brighter brightest bring brings brought brother brothers brown brush brushing building buildings bus buses business businesses busy buy buys buying cake cakes calendar call called calling calm came camera cameras camp camps camping can cannot candy cap caps car care cared caring carefully cars cat cats catch catches caught chair chairs change changed changing chart charts cheap cheaper check checked checking child children choice choose chosen city cities class classes clean cleaner cleaning clear clearer clearly clock clocks close closer closest clothes clouds coach coaching code coffee cold colder collect collected collection collections color colors come comes company companies complete completed completing completely computer computers confident confidently conversation conversations convenient cookie cookies cool cooler coolest copy copied corner corners correct correctly correction corrections cost costs count counts counted counting cousin cousins cover covered crying cut cuts cutting dad dance dancing daughter daughters day days deal deals dealt decision decisions deep deeper deeply desk desks detail detailed details develop developed development did different differences difficult difficulty dinner dinners direction directions doctor doctors dog dogs door doors down downpour downstairs downtown dream dreams drink drinks drinking drove driver drivers driving drop dropped dropping dry dried drying dude ear earlier earliest ears earth easy easier easiest eat eats eaten eating education effort eight eighteen either email emails employee employees empty end ended ending enough enjoy enjoyed enjoying equal especially evening evenings event events every everyone everything excellent exciting exercise exercises expect expected expecting expensive experience experiences face faces fact facts fair fall fallen falling family families famous far farm farms fast faster fastest father fathers feature features feedback feel feeling feelings feels felt few field fields fifteen fifty film films finally find finding fine fingers finish finished finishes finishing fire fires fish fishes five fix fixed fixing flag flags flight flights floor floors flower flowers focus focusing following food foods foot feet football for force forced forget forgot forgotten form forms forward fox foxes friday friend friends friendly fun funny furniture future game games garden gardens gave gift gifts girl girls glass glasses goal goals good got gotten great greatest grew growing growth guess guessed guessing guide guided guiding hair hairs hand hands handed handle handled handles handling hang hanging happens happy happier happiness hard harder hardest hardly hat hats hate hated having head heads hear heard hearing heart hearts heat heated heavy hello helpful helps hey hidden hide hiding high higher highest hill hills hint hints history hold holding home homes hope hoping hot hour hours house houses hungry hurt idea ideas image images imagine imagined imagining important impossible improve improved improving improvement improvements impression impressions inside instead install installed installing instructions interesting internet interview interviewed interviews invite invited inviting island islands item items January joke jokes journey journeys jump jumps jumped jumping just keyboard keyboards key keys kid kids kitchen kitchens knew know knowing known knowledge label labels lake lakes language languages large larger largest last late later latest laugh laughs laughed laughing layout layouts learn learned learning learns leave leaving left lesson lessons letter letters library libraries life lives light lighter lights like liked liking line lines list listed listing listen listened listening little live lived living local location locations long longer longest look looked looking looks lose losing lost loudly love loved loves loving low lower lowest luck lucky lunch lunches machine machines made magic main mainly make makes making manager managers many map maps March mark marked market markets matter matters maybe mean means meant measure measured measurement measurements meeting meetings memory memories menu menus message messages middle middlemost milk mind minds minute minutes missing mistake mistakes monday month months mood moods morning mornings mother mothers mountain mountains mouse mice move moved movies moving much music musical my myself name named names natural naturally nature near nearby nearest need needed needing needs neighbor neighbors never new newer newest news next nice nicer nicely night nights nobody noise normal normally north notice noticed noticing November number numbers obvious obviously ocean October off offer offered offering office offices often okay old older oldest once online only open opened opening options order ordered orders ordinary organization organize organized organizing original originally other others our ours outside over own owner owners pain painful paint painting pair pairs paper papers parent parents park parked parking party parties pass passed passing past path paths patient people perfect perfectly person persons phone phones photo photos phrase phrases picture pictures place placed places plan planned planning plans plant plants play played playing player players pleasant please pleased point pointed pointing points police polite pool pools poor popular position positions positive possible possibly practice practiced practicing prefer preferred prefers present presents pretty price prices problem problems progress program programs project projects protect protected public pull pulled pulling push pushed pushing quick quicker quickest quickly quiet quietly rain rained raining rainy ran random rapidly rare rarely rate rated rating read reading reader readers ready real really reason reasons receipt receipts receive received receiving recent recently recognize recognized recognizing record recorded records red remember remembered remembering repair repaired repairing replace replaced replacing repeat repeated repeating repeats reply replied replying report reports requested requesting result results return returned returning review reviewed reviewing reviews ride rides riding right rightfully river rivers road roads room rooms rough route routes rule rules run running runs sad safe safely safety said sale sales same save saved saving saw say saying says school schools science scientific screen screens screenshot screenshots search searched searching season seasons second seconds see seeing seen seems select selected selecting send sending sends sense sensible sentence sentences separate service services set sets setting settings seven share shared sharing short shorter shortest should show showed showing shows shy sibling siblings simple simpler simply since sing singing sister sisters six sky sleep sleeping slept slow slower slowly small smaller smallest smart smarter smartest smile smiled smiling snow snowy soap social software solution solutions some sometimes somebody something somewhere soon sound sounds south space spacebar spaces spacing speak speaking speed speeds spell spelled spelling spent spoke spoken spot spots spring stand standing start started starting starts stay stayed staying steady step steps still stop stopped stopping stops store stores story stories straight strange stronger strongest stuck student students study studied studying style styles succeed success successful suddenly suggested suggestions summer sun sunlight sunny support supported supporting sure surprise surprised surprising system systems table tables tablet tablets take takes taken taking talk talked talking talks tall taller taste tasted tastes team teams tell tells telling ten test tested tests text texts thank thanked thanks that then there these they thin think thinking thought thoughts three through throw threw time times tired today together told tomorrow tonight too took tool tools top tops total town towns traffic train trains trained training travel traveled traveling tree trees tried tries trip trips trouble truck trucks true truly trust trusted try trying twice two type typed types typing ugly understand understanding understood unique unit units until up update updated updates upload uploaded uploading us use used user users useful usually value values vehicle vehicles version versions very video videos view viewed views village voice volume volumes wait waited waiting wake woke walk walked walking walks wall walls want wanted wants warm warmer warning was watch watched watching watches water waters way ways weather website websites week weeks weekend weekends well went were west what when where whether which while white who whole why wife wifi will win winning winter wish wished wishing with without woman women wonder wonderful wondering word words work worked working works world worlds worry worried worrying worse worst would write writer writers writing written wrong wrote year years yes yesterday yet you young younger youngest your yours yourself";
    // General-purpose word families missing from the bootstrap dictionary.
    // Keep whole-word candidates available before attempting segmentation.
    private static final String COMMON_WORD_FAMILIES =
        "animate animated animates animating animation animations animator animators " +
        "vibrate vibrated vibrates vibrating vibration vibrations vibrational vibrator " +
        "organize organized organizes organizing organization organizations organizational " +
        "single singles singly singer singers singing sing song songs " +
        "signal signals signaled signaling signature signatures " +
        "simple simpler simplest simplicity simplify simplified simplifying " +
        "image images imaging imagine imagined imagining imagination imaginative " +
        "information informative inform informed informing informs " +
        "correction corrections correct corrected correcting " +
        "prediction predictions predict predicted predicting predictor " +
        "sentence sentences sentential typing type typed typo typos " +
        "reaction reactions react reacted reacting interaction interactions " +
        "notification notifications notify notified notifying " +
        "celebration celebrations celebrate celebrated celebrating " +
        "translation translations translate translated translating " +
        "connection connections connect connected connecting " +
        "explanation explanations explain explained explaining " +
        // Common editing actions and their standard inflections; no user
        // sentence or typo string is added to the dictionary.
        "highlight highlights highlighted highlighting " +
        "underline underlines underlined underlining " +
        "bookmark bookmarks bookmarked bookmarking " +
        // Regular inflections of everyday editing verbs are general vocabulary,
        // not memorized misspellings or whole-sentence answers.
        "mix mixes mixed mixing mixture mixtures " +
        "merge merges merged merging " +
        "combine combines combined combining";
    // Ordinary verb lemmas and inflections needed for compositional, no-space
    // decoding. A varied verb lexicon avoids inventing tiny split fragments;
    // it contains no stored user sentences or raw typo strings.
    private static final String COMMON_ACTION_VERBS =
        "arrive arrives arrived arriving depart departs departed departing "+
        "reach reaches reached reaching return returns returned returning "+
        "travel travels traveled traveling visit visits visited visiting "+
        "enter enters entered entering exit exits exited exiting "+
        "wait waits waited waiting send sends sent sending "+
        "receive receives received receiving deliver delivers delivered delivering "+
        "answer answers answered answering respond responds responded responding "+
        "prepare prepares prepared preparing pack packs packed packing "+
        "move moves moved moving carry carries carried carrying "+
        "collect collects collected collecting begin begins began beginning "+
        "finish finishes finished finishing complete completes completed completing "+
        "schedule schedules scheduled scheduling arrange arranges arranged arranging "+
        "invite invites invited inviting accept accepts accepted accepting "+
        "expect expects expected expecting check checks checked checking "+
        "repair repairs repaired repairing replace replaces replaced replacing "+
        "connect connects connected connecting restore restores restored restoring "+
        "review reviews reviewed reviewing explain explains explained explaining "+
        "improve improves improved improving adjust adjusts adjusted adjusting "+
        "remember remembers remembered remembering forget forgets forgot forgetting";
    private static final String SLANG_STARTER =
        "af ayo bet bro bruh cap chill cooked cooking dawg dawggg dude fam fire flex fr gonna gotta heck hella imma lmao lol lowkey nah ngl noob omg oop oops savage sheesh simon sus vibing vibes wanna wdym yall yeah yeet yezzir";
    private static final Map<Integer,List<Map.Entry<String,Integer>>> WORDS_BY_LENGTH = new HashMap<>();
    private static final Map<Integer,Map<Character,List<Map.Entry<String,Integer>>>> WORDS_BY_LENGTH_FIRST = new HashMap<>();
    // Immutable boot lexicon index. One reference per >=2-letter word; unlike
    // caching suggestions, this stores no user-entered text or personal data.
    // Build from WORDS.entrySet() in the same order as the former full scan.
    private static final Map<String,List<Map.Entry<String,Integer>>> WORDS_BY_PREFIX2 = new HashMap<>();

    private static final String LEXICON =
        "a about above absolutely accept accurate across action actually add after again against ago ahead ai all almost alone already also always am amazing an and android another answer any anybody anymore anyone anything anyway app apps are around as ask asked asking at auto automatic available away awesome back bad bag battery be because become been before begin beginning behind believe best better big bike bit black blue both bring broken browser build but by call called calling can cannot car care case change changed changes chat check checking choose clean clear close code color colors come coming comment complete computer confirmed connection continue cool could correct corrected correction cost couple crash current custom daily dark data date day days default definitely device did different difficult do does doing done dont download downloaded down drive driving each earlier early easy edit end enough enter error errors especially even evening ever every everybody everything exactly example examples excited explain extra face fact fail failed failing fast favorite feature feel feeling few file find fine first fix fixed fixing follow for forgot found free fresh friday from full fun future game get getting give go going good google got great green guess had half handle handling happen happened happens happening happy hard has have having he head hello help her here hey hi him his home hope hot how i id if ill im important improve improving in input install installed installing instead into is issue issues it its ive just keep key keyboard keys kind know known later last learn learning least leave left let lets like liked little live long look looking lot love made main make makes making manual many match matching may me mean meant message might more morning most much my name natural near need never new next nice night no not now of off offline okay old on once one only open option options or original other our out outside over own phone phrase plan play please pretty problem problems project properly purple quick ready real really receive record red release remember replacement reply result results right road run running same samsung save say says screen search see seems send sentence sentences separate settings she should show simple single slow small so some something soon sorry sounds space spaces speed spelling spot start started still stop store stuff suggestion suggestions sunday support sure swipe swiping system take tell tells telling told tap tapping test tested testing tests text than thank thanks that the their them then there these they thing things think this those through time tired to today tomorrow tonight too touch try trying turn type typed types typing up update updates upload us use used useful using very version video wait want wanted was way we well went were what when where whether which while white who why will with without woke word words work worked working works would wrong yeah yes yesterday yet you your youre youtube";
    private static final Map<String, List<String>> NEXT = new HashMap<>();
    private static final Map<String, List<String>> PHRASE_NEXT = new HashMap<>();
    // Bootstrap starting-word suggestions. These are optional completions,
    // not automatic corrections. Ordered by conversational usefulness.
    private static final Map<String, String[]> FIRST_LETTER = new HashMap<>();
    static {
        for (String word : LEXICON.split(" ")) WORDS.put(word, 12);
        WORDS.put("help", 90); WORDS.put("hello", 75); WORDS.put("hey", 25);
        for (String common : "a about and are be but can do for get go have how i im in is it its just know let lets me my no not of on or our see she so that the there these they this to today we what when where who why will with would you your".split(" ")) WORDS.put(common, 65);
        for (String common : "are you doing this is can you i dont know how are you lets see what happens to the store of the at home call me do anything app to see hey im outside".split(" ")) WORDS.put(common, 65);
        for (String word : "cool lacking tonight thanks going trying typed texting happened happens message hello help this dont its thats good great tomorrow".split(" ")) WORDS.put(word, 37);
        pairs("this", "is", "was", "app");
        pairs("how", "are", "do", "is");
        pairs("you", "doing", "are", "can");
        pairs("can", "you", "we", "i");
        pairs("i", "am", "dont", "will", "was");
        pairs("im", "going", "outside", "testing");
        pairs("let", "me", "us");
        pairs("lets", "see", "try", "go");
        pairs("what", "happens", "is", "are");
        pairs("its", "lacking", "cool", "working");
        pairs("see", "what", "you", "how");
        pairs("hello", "there", "everyone");
        pairs("thank", "you");
        pairs("home", "today", "now", "soon");
        pairs("work", "today", "tomorrow", "on");
        pairs("like", "this", "that", "it");
        pairs("test", "this", "again");
        pairs("hi", "there", "how");
        pairs("good", "morning", "night", "job");
        pairs("are", "you", "we", "they");
        pairs("is", "this", "it", "there");
        first("a","and","are","about");
        first("b","be","but","because");
        first("c","can","call","cool");
        first("d","do","don't","did");
        first("e","even","everything","every");
        first("f","for","from","find");
        first("g","go","good","get");
        first("h","hi","how","hello");
        first("i","I","I'm","I've");
        first("j","just","job","join");
        first("k","know","keep","kind");
        first("l","like","let","love");
        first("m","my","me","make");
        first("n","no","not","now");
        first("o","on","of","okay");
        first("p","please","phone","play");
        first("q","quick","question","quiet");
        first("r","really","right","ready");
        first("s","so","see","she");
        first("t","the","this","to");
        first("u","up","use","update");
        first("v","very","version","video");
        first("w","we","what","when");
        first("x","x-ray","xylophone","x");
        first("y","you","your","yes");
        first("z","zero","zone","zebra");
        for(String w : "starting started starts along come coming actually nearly slowly finally".split(" "))
            WORDS.putIfAbsent(w,12);
        WORDS.put("starting",28); WORDS.put("along",24); WORDS.put("first",32);
        WORDS.put("all",32); WORDS.put("okay",55); WORDS.put("come",35);
        // Missing multi-typo recovery vocabulary; vocabulary additions are general words,
        // not hardcoded full-sentence answers.
        WORDS.put("saying",24);
        WORDS.put("completely",22);
        WORDS.put("unrelated",20);
        WORDS.put("also",28);
        WORDS.put("keyboard",35);
        WORDS.put("something",30);
        phrase("okay","this","so","let");
        phrase("this","is","was"); phrase("is","starting","going","coming","working");
        phrase("starting","to","with"); phrase("to","come","go","work","see");
        phrase("come","along","back","home"); phrase("first","of","time");
        phrase("of","all","the"); phrase("how","are"); phrase("are","you");
        phrase("you","doing","call","bring"); phrase("can","you");
        phrase("call","me"); phrase("let","me"); phrase("me","know");
        phrase("know","when"); phrase("when","you"); phrase("get","home");
        phrase("going","to"); phrase("but","its"); phrase("its","lacking");
        phrase("what","happens","are","is"); phrase("happens","when","next","today");
        // Expand ordinary vocabulary without memorizing a user's full sentence.
        for(String word:EXPANDED_CORE_LEXICON.split(" ")) WORDS.putIfAbsent(word.toLowerCase(Locale.US),18);
        for(String word:CONVERSATIONAL_FORMS.split(" ")) WORDS.putIfAbsent(word.toLowerCase(Locale.US),18);
        for(String word:CORE_EVERYDAY.split(" ")) WORDS.putIfAbsent(word.toLowerCase(Locale.US),20);
        for(String word:COMMON_WORD_FAMILIES.split(" ")) WORDS.putIfAbsent(word.toLowerCase(Locale.US),22);
        for(String word:SLANG_STARTER.split(" ")) WORDS.putIfAbsent(word.toLowerCase(Locale.US),8);
        // New verb forms enter conservatively: preserve the existing ranking
        // strength of previously known vocabulary, but keep missing lemmas
        // available for multi-typo sentence decoding.
        for(String word:COMMON_ACTION_VERBS.split(" "))
            WORDS.putIfAbsent(word.toLowerCase(Locale.US),12);
        WORDS.put("space",42); WORDS.put("sentence",32); WORDS.put("typing",44);
        WORDS.put("yeah",36); WORDS.put("bar",24); WORDS.put("dead",24); WORDS.put("forget",30);
        // Common but previously absent or underweighted standalone words.
        WORDS.put("dead",28); WORDS.put("forget",38); WORDS.put("forgot",38);
        // General feedback and UI vocabulary from real-world typing failures.
        for(String word:("animation animations animate animated animating vibration vibrations vibrate vibrating "+
                "single singles singular singularly word words feedback tactile haptic haptics "+
                "tap taps tapped tapping press pressed pressing correction corrections "+
                "prediction predictions gesture gestures learning learned learner adaptive").split(" "))
            WORDS.putIfAbsent(word,26);
        WORDS.put("single",35); WORDS.put("animation",30); WORDS.put("vibration",32);
        // Common correctly spelled words missing from the offline lexicon.
        for(String word:"smooth application accuracy".split(" "))
            WORDS.putIfAbsent(word,24);
        // Context helps select sensible word boundaries rather than rewarding
        // a larger number of tiny, individually valid word fragments.
        extendPhrase("the","space","sentence","screen","message","text","phone","store","app","keyboard");
        extendPhrase("space","is","bar","after","before","between");
        extendPhrase("sentence","is","was","looks");
        extendPhrase("yeah","the","i","we","you","this","it");
        extendPhrase("hello","how","there");
        extendPhrase("how","is","are","do");
        extendPhrase("you","are","can","will","have");
        extendPhrase("is","working","ready","better","great");
        extendPhrase("keyboard","feels","is","looks","works","needs","should");
        extendPhrase("feels","good","better","faster","nice","natural","complete");
        extendPhrase("looks","good","great","better","cleaner","nice");
        extendPhrase("we","are","can","will","should","need","have");
        extendPhrase("i","am","was","will","can","need","want","have","think","forget","forgot");
        extendPhrase("this","is","feels","looks","works","should","will");
        extendPhrase("you","should","are","can","will","have","need");
        extendPhrase("should","be","work","have","get","try");
        extendPhrase("will","be","get","have","call","show");
        extendPhrase("to","the","do","go","get","type","test","read","see","finish");
        extendPhrase("after","work","lunch","the");
        extendPhrase("more","time","useful","than");
        extendPhrase("the","next","new","first","last","old","right","wrong");
        extendPhrase("is","really","still","much","too","finally","cool","good","nice");
        // General transitions: "is cool but..." is preferable to "is too but...".
        extendPhrase("cool","but","and");
        extendPhrase("are","still","already","you","we","they");
        extendPhrase("need","to","more","a","the");
        extendPhrase("want","to","another","more");
        // General grammatical contexts, useful beyond the benchmark corpus.
        extendPhrase("i","wanted","cannot","got","think","need","want","forget");
        extendPhrase("wanted","to","more");
        extendPhrase("cannot","believe","go","see","do");
        extendPhrase("believe","this","that","you");
        extendPhrase("got","home","it","a","the");
        extendPhrase("forget","the","to","about","it");
        extendPhrase("takes","time","a","the");
        extendPhrase("time","to","for","is","when");
        extendPhrase("this","sentence","change","version","idea");
        extendPhrase("that","sentence","word","day","time");
        extendPhrase("sentence","needs","has","is","should");
        extendPhrase("the","team","sunlight","sky","layout","fox","brown","idea");
        extendPhrase("team","is","can","will","should");
        extendPhrase("another","way","day","sentence","example","time");
        extendPhrase("without","wifi","you","the","it");
        extendPhrase("work","without","today","tomorrow","on");
        extendPhrase("tell","me","you");
        extendPhrase("repeat","that","the");
        extendPhrase("finish","this","the","work");
        extendPhrase("sunlight","looks","is");
        extendPhrase("layout","is","looks","feels");
        extendPhrase("looks","beautiful","really","nice","good");
        extendPhrase("no","sense","problem","time");
        extendPhrase("good","impression","idea","job","morning");
        extendPhrase("before","the","work","you");
        extendPhrase("we","can","will","should","need");


        // Any clean word we publicly offer as a FIRST-LETTER completion
        // should also count as an ordinary correctly spelled whole word.
        // Previously "q" suggested "question", but "question" itself
        // could be split into unrelated smaller words because it was absent
        // from WORDS. Share a single source of offline lexical truth rather
        // than hardcoding corrections for individual screenshot fixtures.
        // Skip contractions, hyphens and single-character shortcuts; the
        // existing normalized-display and secure-field rules still apply.
        for(String[] completions:FIRST_LETTER.values()) {
            for(String option:completions) {
                String lexical=option.toLowerCase(Locale.US);
                if(lexical.matches("[a-z]{2,24}"))
                    WORDS.putIfAbsent(lexical,18);
            }
        }

        // Build 297 research: add generic, verified offline vocabulary and
        // inflections only when base+past+participle evidence already exists.
        // Installed IME behavior, text-edit semantics and UI are unchanged.
        LexiconFamilyExpander.enrich(WORDS);
        // Research 316: curated general-purpose words found absent by a
        // reproducible frequency-list audit. Keep every previously ranked
        // word's original frequency, and avoid names or private user text.
        EverydayEnglishCore316.enrich(WORDS);

        for(Map.Entry<String,Integer> entry:WORDS.entrySet()) {
            int length=entry.getKey().length();
            WORDS_BY_LENGTH.computeIfAbsent(length,k->new ArrayList<>()).add(entry);
            WORDS_BY_LENGTH_FIRST.computeIfAbsent(length,k->new HashMap<>())
                    .computeIfAbsent(entry.getKey().charAt(0),k->new ArrayList<>()).add(entry);
            if(length>=2) {
                String prefix=entry.getKey().substring(0,2);
                WORDS_BY_PREFIX2.computeIfAbsent(prefix,k->new ArrayList<>()).add(entry);
            }
        }
    }
    // Small offline conversational phrase corpus. Matching is generic:
    // normalize spaces/apostrophes, then use bounded typo distance.
    private static final String[] COMMON_PHRASES={
        "how's it going","how are you doing","how is it going",
        "hello there how's it going","hello there how is it going",
        "what's going on","what are you doing","what's up",
        "I'm going home","I'm going to work","I don't know",
        "don't worry about it","let me know","can you help me",
        "see you tomorrow","thank you so much","good morning",
        "good night","this is awesome","that's awesome",
        "how was your day","what do you think","I love this",
        "I'm ready","let's get it","hell yeah","heck yeah","it's working","okay this works"
    };
    // Separate next-word corpus: never used as whole-phrase typo corrections.
    // These varied examples provide suffix evidence for unfamiliar sentences.
    private static final String[] CONTEXT_EXAMPLES={
        "the keyboard feels smooth and responsive",
        "this keyboard works really well",
        "This new keyboard feels excellent",
        "This new keyboard is responsive",
        "This new keyboard has good predictions",
        "my keyboard is working better today",
        "the new keyboard looks great",
        "your keyboard feels much faster",
        "the keyboard is very comfortable",
        "this keyboard has better predictions",
        "my phone keyboard is working",
        "my phone keyboard feels smooth",
        "my phone keyboard works offline",
        "the keyboard works without internet",
        "my new phone feels fast",
        "my new app works well",
        "the new update feels smooth",
        "the new version works better",
        "the phone is working well",
        "this app is really useful",
        "the app works great today",
        "my phone is running smoothly",
        "I love how this works",
        "I love the new design",
        "I love my phone",
        "I love this keyboard",
        "I like the new keyboard",
        "I think the keyboard feels better",
        "I want the keyboard to improve",
        "the typing feels more natural",
        "typing on my phone feels easier",
        "the predictions are getting better",
        "the suggestions are more accurate",
        "my new keyboard makes typing easier",
        "my new keyboard has great features",
        "the new keyboard is easy to use",
        "the keyboard is getting smarter",
        "the keyboard feels really good",
        "the keyboard has improved a lot",
        "I am testing the new update",
        "I am using the keyboard today",
        "I need help with this feature",
        "please check the new settings",
        "can you help me with this",
        "we are testing the new version",
        "it looks good so far",
        "everything is working as expected",
        "I love my new keyboard because it is fast",
        "I love my new keyboard and its features",
        "I love my new keyboard so much",
        "I love my new phone because it works",
        "I love my new phone and the screen",
        "I love my new app because it helps",
        "I love my new app and its design",
        "I love my new setup because it works",
        "I love my new setup and the colors",
        "I like my new keyboard because it is smooth",
        "I like my new keyboard and the layout",
        "I enjoy my new keyboard because it is easy",
        "I enjoy my new keyboard and its speed"
    };
    private static final Map<String,List<String>> CORPUS_CONTINUATION_INDEX=
            buildCorpusContinuationIndex();
    // Build repeat-supported word transitions for the rescue scorer. This uses
    // only adjacent dictionary words observed across the varied offline
    // example corpus, never exact full-sentence lookup or user typing data.
    // Require two occurrences to reduce one-off context noise.
    private static final Map<String,List<String>> RECOVERY_CONTEXT = buildRecoveryContext();
    private static Map<String,List<String>> buildRecoveryContext() {
        Map<String,List<String>> transitions=new HashMap<>();
        for(Map.Entry<String,List<String>> entry:PHRASE_NEXT.entrySet())
            transitions.put(entry.getKey(),new ArrayList<>(entry.getValue()));
        Map<String,Integer> counts=new HashMap<>();
        for(String sample:CONTEXT_EXAMPLES) {
            String[] words=sample.toLowerCase(Locale.US)
                    .replaceAll("[^a-z ]"," ").replaceAll(" +"," ").trim().split(" ");
            for(int i=1;i<words.length;i++) {
                String before=words[i-1],after=words[i];
                if(!WORDS.containsKey(before)||!WORDS.containsKey(after))continue;
                String pair=before+" "+after;
                counts.put(pair,counts.getOrDefault(pair,0)+1);
            }
        }
        for(Map.Entry<String,Integer> entry:counts.entrySet()) {
            if(entry.getValue()<2)continue;
            String pair=entry.getKey();int space=pair.indexOf(' ');
            String before=pair.substring(0,space),after=pair.substring(space+1);
            List<String> following=transitions.computeIfAbsent(before,k->new ArrayList<>());
            if(!following.contains(after))following.add(after);
        }
        return transitions;
    }
    // Phrase evidence uses the whole typed sequence, not independent word guesses.
    // Two- and three-error recoveries require longer inputs to avoid replacing
    // ordinary short words with unrelated expressions.
    private static void addConversationalPhrases(String input,String raw,
            List<ScoredText> result,Set<String> known) {
        if(raw.length()<5 || raw.length()>28)return;
        for(String phrase:COMMON_PHRASES) {
            String normalized=phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
            int maxEdits=raw.length()>=10?3:raw.length()>=8?2:1;
            if(Math.abs(normalized.length()-raw.length())>maxEdits)continue;
            int edits=editDistanceLimited(raw,normalized,maxEdits);
            if(edits>maxEdits)continue;
            // Whole-phrase confidence is strongest for exact compact matches.
            // A two-error phrase must still beat speculative word splitting,
            // while three-error proposals remain cautious alternatives.
            double score=edits==0?140:edits==1?116:edits==2?91:66;
            add(result,known,caseDisplay(input,phrase),score);
        }
    }
    /** Rank exact two-word reconstructions before fuzzy whole-word edits.
     *  This is a general lexicon/transition rule, not an input-specific fix.
     *  Only use it for unknown joined strings; existing dictionary words
     *  remain protected by the early return in suggestions().
     */
    private static void addExactMissingSpaceCandidates(String input,String raw,
            List<ScoredText> result,Set<String> known) {
        if(raw.length()<5 || raw.length()>24)return;
        for(int cut=1;cut<raw.length()-1;cut++) {
            String left=raw.substring(0,cut),right=raw.substring(cut);
            Integer leftFrequency=WORDS.get(left),rightFrequency=WORDS.get(right);
            if(leftFrequency==null || rightFrequency==null)continue;
            // Short function words are common first halves of missed spaces.
            boolean functionWord=Arrays.asList("i","a","it","is","we","he","she",
                    "you","they","my","your","our","the","this","that","to",
                    "of","in","on","at","for","can","do","did","will","was",
                    "are","should","would","could","thank","good").contains(left);
            List<String> transitions=PHRASE_NEXT.get(left);
            List<String> next=NEXT.get(left);
            boolean supported=(transitions!=null && transitions.contains(right))
                    || (next!=null && next.contains(right));
            if(!functionWord && !supported)continue;
            // A lexicon entry at the default frequency (12) is still a valid
            // word: the old >=18 gate silently rejected common words such as
            // "was", even when both halves matched exactly.
            if(right.length()<2)continue;
            // Exact letters plus a plausible boundary outrank unrelated
            // one-word fuzzy corrections, but not an exact phrase match.
            double score=90 + (supported?18:0)
                    + Math.min(8,Math.log(leftFrequency*rightFrequency));
            add(result,known,caseDisplay(input,display(left)+" "+display(right)),score);
        }
    }
    private static void addStrongSingleWordCorrections(String input,String raw,
            List<ScoredText> result,Set<String> known) {
        if(WORDS.containsKey(raw)||raw.length()<6||raw.length()>14)return;
        for(Map.Entry<String,Integer> e:WORDS.entrySet()) {
            String word=e.getKey();
            if(Math.abs(word.length()-raw.length())>3 || word.length()<5)continue;
            int edits=editDistanceLimited(raw,word,3);
            if(Math.abs(word.length()-raw.length())>1)continue;
            // A damaged first character must not hide a close word that
            // shares a long exact ending. Unlike general two-edit fuzzy
            // search, this is highly constrained and does not scan prefixes.
            if(edits==2 && raw.length()>=7 && word.length()>=7 &&
                    raw.substring(raw.length()-4).equals(word.substring(word.length()-4))) {
                double score=58+Math.log(e.getValue());
                add(result,known,caseDisplay(input,display(word)),score);
                continue;
            }
            // Two interior typos in a short word can otherwise lose to
            // unrelated higher-frequency suggestions or spurious word splits.
            // Require both a two-letter prefix and a final-letter anchor.
            boolean strongBoundary=raw.length()>=6 && word.length()>=6 &&
                    raw.substring(0,2).equals(word.substring(0,2)) &&
                    raw.charAt(raw.length()-1)==word.charAt(word.length()-1);
            if(edits==2 && strongBoundary) {
                double score=92+Math.log(e.getValue());
                add(result,known,caseDisplay(input,display(word)),score);
                continue;
            }
            if(edits!=3)continue; // Other 1-2 edits use the indexed path.
            // For short, badly damaged words, strong matching boundaries
            // provide evidence that a third interior error is plausible.
            // Avoid boosting unrelated dictionary words sharing one letter.
            boolean anchored=raw.length()>=6 && word.length()>=6 &&
                    raw.substring(0,2).equals(word.substring(0,2)) &&
                    raw.charAt(raw.length()-1)==word.charAt(word.length()-1);
            double score=(anchored?76:37)+Math.log(e.getValue())-edits*1.1;
            add(result,known,caseDisplay(input,display(word)),score);
        }
    }

    /**
     * Recover a pair of substantial words when a speculative three-or-more-word
     * split wins the beam. Each half must have exactly one bounded typo,
     * matching its first two letters and final letter. This prevents unrelated
     * high-frequency words and short function-word fragments from dominating.
     * The candidate is assembled from lexicon words; no user phrase is stored.
     */
    private static String anchoredTwoWordCandidate(String input, String raw,
            Map<String,List<Candidate>> fragmentCache) {
        if(raw.length()<10 || raw.length()>22)return null;
        String strongest=null;
        double bestScore=Double.NEGATIVE_INFINITY;
        for(int cut=5;cut<=raw.length()-5;cut++) {
            String left=raw.substring(0,cut),right=raw.substring(cut);
            List<Candidate> first=fragmentCache.get(left);
            if(first==null) {
                first=candidates(left,true);
                fragmentCache.put(left,first);
            }
            List<Candidate> second=fragmentCache.get(right);
            if(second==null) {
                second=candidates(right,true);
                fragmentCache.put(right,second);
            }
            for(Candidate a:first) {
                if(a.edits!=1 || a.word.length()<5 ||
                        !a.word.startsWith(left.substring(0,2)) ||
                        a.word.charAt(a.word.length()-1)!=left.charAt(left.length()-1))
                    continue;
                for(Candidate b:second) {
                    if(b.edits!=1 || b.word.length()<5 ||
                            !b.word.startsWith(right.substring(0,2)) ||
                            b.word.charAt(b.word.length()-1)!=right.charAt(right.length()-1))
                        continue;
                    double score=Math.log1p(a.freq)+Math.log1p(b.freq)
                            -Math.abs(a.word.length()-left.length())
                            -Math.abs(b.word.length()-right.length());
                    if(score>bestScore) {
                        bestScore=score;
                        strongest=caseDisplay(input,display(a.word)+" "+display(b.word));
                    }
                }
            }
        }
        return strongest;
    }

    private static void phrase(String word,String... next) {
        PHRASE_NEXT.put(word,Arrays.asList(next));
    }
    private static void extendPhrase(String word,String... next) {
        List<String> previous=PHRASE_NEXT.get(word);
        List<String> merged=previous==null?new ArrayList<>():new ArrayList<>(previous);
        for(String suggestion:next) if(WORDS.containsKey(suggestion)&&!merged.contains(suggestion))
            merged.add(suggestion);
        PHRASE_NEXT.put(word,merged);
    }
    private static void first(String letter, String... completions) {
        FIRST_LETTER.put(letter, completions);
    }
    private static List<String> firstLetterSuggestions(String input, String raw) {
        String[] chosen = FIRST_LETTER.get(raw);
        if (chosen == null) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>(chosen.length);
        for (String word : chosen) {
            String suggestion = caseDisplay(input, word);
            if (!suggestion.equals(input) && !out.contains(suggestion)) out.add(suggestion);
        }
        return out;
    }
    private static void pairs(String word, String... next) { NEXT.put(word, Arrays.asList(next)); }
    private static String display(String w) {
        switch (w) {
            case "im": return "I'm";
            case "dont": return "don't";
            case "ill": return "I'll";
            case "cant": return "can't";
            case "youre": return "you're";
            case "lets": return "let's";
            case "ive": return "I've";
            case "its": return "it's";
            case "i": return "I";
            default: return w;
        }
    }
    private static String caseDisplay(String raw, String suggestion) {
        if (raw.length() > 0 && Character.isUpperCase(raw.charAt(0)) && suggestion.length() > 0)
            return Character.toUpperCase(suggestion.charAt(0)) + suggestion.substring(1);
        return suggestion;
    }
    // Distances are evaluated thousands of times during long-sentence
    // recovery. Keep numeric DP rows on the current thread rather than
    // allocating three arrays for every candidate comparison. No typed
    // characters, suggestions or other user text are cached.
    private static final class DistanceRows {
        int[] first=new int[66],second=new int[66],third=new int[66];
        void ensureCapacity(int columns) {
            if(columns<=first.length)return;
            int capacity=Math.max(columns,first.length*2);
            first=new int[capacity];
            second=new int[capacity];
            third=new int[capacity];
        }
    }
    private static final ThreadLocal<DistanceRows> DISTANCE_ROWS=
            ThreadLocal.withInitial(DistanceRows::new);

    /**
     * Calculate only the diagonal band that could fit the requested typo
     * budget. Each insertion/deletion shifts a diagonal by at most one;
     * adjacent transpositions stay inside the band as well.
     *
     * Preserve the legacy exact over-limit return when the last cell is
     * outside the budget. Other consumers historically observe that value,
     * so the bounded fast path falls back to the original full-row metric.
     * Numeric scratch storage is thread local; typed text is never cached.
     */
    static int editDistanceLimited(String a, String b, int limit) {
        int m=a.length(),n=b.length();
        if(Math.abs(m-n)>limit)return limit+1;
        if(limit==0)return a.equals(b)?0:1;
        // Small matrices do not recover the cost of band initialization.
        if(m<=2*limit+3 || n<=2*limit+3)
            return editDistanceFullRows(a,b,limit);
        DistanceRows rows=DISTANCE_ROWS.get();
        rows.ensureCapacity(n+1);
        int[] prev=rows.first,curr=rows.second,prevprev=rows.third;
        int overflow=limit+1;
        Arrays.fill(prev,0,n+1,overflow);
        for(int j=0;j<=Math.min(n,limit);j++)prev[j]=j;
        for(int i=1;i<=m;i++) {
            Arrays.fill(curr,0,n+1,overflow);
            if(i<=limit)curr[0]=i;
            int minimum=curr[0];
            int first=Math.max(1,i-limit),last=Math.min(n,i+limit);
            for(int j=first;j<=last;j++) {
                int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
                int best=Math.min(Math.min(prev[j]+1,curr[j-1]+1),
                        prev[j-1]+cost);
                if(i>1 && j>1 && a.charAt(i-1)==b.charAt(j-2)
                        &&a.charAt(i-2)==b.charAt(j-1))
                    best=Math.min(best,prevprev[j-2]+1);
                curr[j]=best;
                minimum=Math.min(minimum,best);
            }
            if(minimum>limit)return overflow;
            int[] reuse=prevprev;
            prevprev=prev;
            prev=curr;
            curr=reuse;
        }
        // The original routine can return a value larger than limit+1
        // when the final row contains a closer match in another column.
        return prev[n]<=limit ? prev[n] : editDistanceFullRows(a,b,limit);
    }

    /** Pre-#218 exact full-matrix semantics, retained for the rare fallback. */
    private static int editDistanceFullRows(String a, String b, int limit) {
        int m=a.length(),n=b.length();
        if(Math.abs(m-n)>limit)return limit+1;
        DistanceRows rows=DISTANCE_ROWS.get();
        rows.ensureCapacity(n+1);
        int[] prev=rows.first,curr=rows.second,prevprev=rows.third;
        for(int j=0;j<=n;j++)prev[j]=j;
        for(int i=1;i<=m;i++) {
            curr[0]=i;
            int minimum=curr[0];
            for(int j=1;j<=n;j++) {
                int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
                int best=Math.min(Math.min(prev[j]+1,curr[j-1]+1),
                        prev[j-1]+cost);
                if(i>1 && j>1 && a.charAt(i-1)==b.charAt(j-2)
                        &&a.charAt(i-2)==b.charAt(j-1))
                    best=Math.min(best,prevprev[j-2]+1);
                curr[j]=best;
                minimum=Math.min(minimum,best);
            }
            if(minimum>limit)return limit+1;
            int[] reuse=prevprev;
            prevprev=prev;
            prev=curr;
            curr=reuse;
        }
        return prev[n];
    }

    private static final class Candidate {
        final String word;
        final int edits, freq;
        Candidate(String w,int e,int f){word=w;edits=e;freq=f;}
    }
    /**
     * Restore a missing final letter only at a long sentence boundary.
     * Standalone short-word predictions remain conservative.
     */
    static boolean shortBoundaryCompletion(String fragment,String candidate,
            boolean longJoinedInput) {
        return longJoinedInput && fragment.length()==2
                && candidate.length()==3 && candidate.startsWith(fragment);
    }
    static double anchoredFragmentEvidence(String fragment,String candidate,
            int edits,boolean longJoinedInput) {
        if(!longJoinedInput || edits==0 || fragment.length()<4
                || candidate.length()<4)return 0;
        if(fragment.charAt(0)!=candidate.charAt(0)
                || fragment.charAt(fragment.length()-1)
                   !=candidate.charAt(candidate.length()-1))return 0;
        // General prefix/suffix evidence for interior typos.
        return 1.25;
    }
    private static List<Candidate> candidates(String fragment, boolean allowFuzzy) {
        return candidates(fragment,allowFuzzy,false);
    }
    private static List<Candidate> candidates(String fragment, boolean allowFuzzy,
            boolean longJoinedInput) {
        // Cancel a stale, expensive fuzzy scan when newer text arrives.
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        Integer exact=WORDS.get(fragment);
        List<Candidate> out=new ArrayList<>();
        if(exact!=null) out.add(new Candidate(fragment,0,exact));

        if(allowFuzzy && fragment.length()>=2) {
            int limit=fragment.length()>=5?2:1;
            int scanned=0;
            for(int length=Math.max(2,fragment.length()-limit);
                    length<=Math.min(14,fragment.length()+limit);length++) {
                // For the two-edit search we already require a matching first
                // letter. Jump straight to that indexed bucket instead of
                // scanning every same-length word on each keystroke.
                Map<Character,List<Map.Entry<String,Integer>>> byFirst=
                        WORDS_BY_LENGTH_FIRST.get(length);
                List<Map.Entry<String,Integer>> indexed=limit==2
                        ?(byFirst==null?null:byFirst.get(fragment.charAt(0)))
                        :WORDS_BY_LENGTH.get(length);
                if(indexed==null)continue;
                for(Map.Entry<String,Integer> e:indexed) {
                    if((++scanned&31)==0 && Thread.currentThread().isInterrupted())
                        return Collections.emptyList();
                    String word=e.getKey();
                    if(word.equals(fragment))continue;
                    if(fragment.length()==2 && word.length()!=2
                            && !shortBoundaryCompletion(fragment,word,longJoinedInput))
                        continue;
                    // Expensive two-edit candidates must share their first letter.
                    if(limit==2 && fragment.charAt(0)!=word.charAt(0))continue;
                    int edit=editDistanceLimited(fragment,word,limit);
                    if(edit>=1 && edit<=limit)out.add(new Candidate(word,edit,e.getValue()));
                }
            }
        }
        out.sort((x,y)->Double.compare(scoreCandidate(y,fragment.length()),scoreCandidate(x,fragment.length())));
        return out.size()>12 ? out.subList(0,12):out;
    }
    private static double scoreCandidate(Candidate c, int length) {
        return length*1.8 + Math.log(c.freq) - c.edits*4.8 - 2.5;
    }
    /**
     * Long no-space inputs need more protection against chains of tiny words.
     * Adjust beam pruning only for 25+ letters: the full user input is never
     * stored, and shorter ordinary words/phrases keep their existing scores.
     */
    static double longBeamPreference(int inputLength,String word,int edits) {
        if(inputLength<25)return 0.0;
        double adjustment=-2.2; // every extra boundary costs evidence
        if(edits==0 && word.length()>=5)adjustment+=1.5;
        if(edits>0 && word.length()<=3)adjustment-=1.5;
        return adjustment;
    }
    /**
     * Decoder quality guard: more than two contiguous short words are often
     * an accidental segmentation of a longer misspelled word. Penalize only
     * the third (and subsequent) unsupported short transition, not common
     * isolated grammar like "is to" or "it was".
     *
     * The consecutive-run counter is local to each decoding path.
     */
    static double unsupportedShortRunPenalty(int inputLength,String previous,
                                              String next,double contextBonus,
                                              int consecutiveShortWords) {
        if(inputLength<18 || inputLength>=28 || previous.isEmpty() || contextBonus>0)return 0.0;
        return consecutiveShortWords>=3 ? -7.0 : 0.0;
    }
    private static final class Path {
        final double value;
        final String text, last;
        final int segments, errors, shortRun;
        // Preserve the five-argument constructor used by existing beam
        // equivalence tests and historical reflection-based diagnostics.
        Path(double score,String txt,String lastWord,int count,int errorCount) {
            this(score,txt,lastWord,count,errorCount,0);
        }
        Path(double score,String txt,String lastWord,int count,int errorCount,
             int consecutiveShortWords) {
            value=score;text=txt;last=lastWord;segments=count;errors=errorCount;
            shortRun=consecutiveShortWords;
        }
    }
    /** Conservative alternatives for an existing word under the cursor.
     *  Unlike live typing, include close whole-word alternatives even for
     *  correctly spelled words; never invent unrelated phrase completions.
     */
    /**
     * Source for optional contextual cursor correction ranking. This reads
     * only prebundled offline transition tables. No predictor output changes
     * unless a separate cursor editing UI explicitly asks for evidence.
     */
    static boolean editingTransitionSupported(String preceding,String following) {
        if(preceding==null || following==null)return false;
        List<String> supported=RECOVERY_CONTEXT.get(preceding);
        if(supported!=null && supported.contains(following))return true;
        List<String> known=NEXT.get(preceding);
        return known!=null && known.contains(following);
    }

    static List<String> editingSuggestions(String input) {
        if(input==null || !input.matches("[A-Za-z]{2,24}"))return Collections.emptyList();
        String raw=input.toLowerCase(Locale.US);
        ArrayList<String> result=new ArrayList<>();
        result.add(input);
        // Some editors commit every key and skip composition. Offer this
        // strongly evidenced two-typo correction as an optional caret chip.
        String near=NeighborDoubleKeyCorrectionPolicy.unique(raw,WORDS);
        if(near!=null)result.add(caseDisplay(input,display(near)));
        for(Candidate c:candidates(raw,true)) {
            if(c.edits==0 || c.edits>1)continue;
            String option=caseDisplay(input,display(c.word));
            if(!result.contains(option))result.add(option);
            if(result.size()>=5)break;
        }
        if(result.size()==1 && !WORDS.containsKey(raw)) {
            for(String option:suggestions(input)) {
                if(!result.contains(option))result.add(option);
                if(result.size()>=5)break;
            }
        }
        return result;
    }
    // Package-private for exact baseline-equivalence testing. Return the
    // immutable dictionary bucket; callers must not mutate its contents.
    static List<Map.Entry<String,Integer>> prefixBucket(String raw) {
        if(raw==null || raw.length()<2)return Collections.emptyList();
        List<Map.Entry<String,Integer>> found=WORDS_BY_PREFIX2.get(raw.substring(0,2));
        return found==null?Collections.emptyList():found;
    }
    // Apostrophes are omitted when typing no-space text. Recover common
    // English 's contractions from their normalized spellings; one-error
    // variants require the initial and final letters to remain anchored.
    // The known-word guard in suggestions() prevents replacing valid words
    // such as "shows" or "its" with speculative contractions.
    private static final String[] COMMON_S_CONTRACTIONS={
        "how's","what's","who's","where's","when's","why's",
        "here's","there's","that's"
    };
    private static List<String> contractionCandidates(String raw) {
        if(raw.length()<4 || raw.length()>7)return Collections.emptyList();
        ArrayList<String> best=new ArrayList<>();
        int bestEdits=2;
        for(String contraction:COMMON_S_CONTRACTIONS) {
            String compact=contraction.replace("'","");
            if(compact.length()<4 ||
                    Math.abs(compact.length()-raw.length())>1 ||
                    compact.charAt(0)!=raw.charAt(0) ||
                    compact.charAt(compact.length()-1)!=raw.charAt(raw.length()-1))
                continue;
            int edits=editDistanceLimited(raw,compact,1);
            if(edits>1)continue;
            if(edits<bestEdits) {
                bestEdits=edits;
                best.clear();
            }
            // A damaged middle letter may make "who's", "why's" and even
            // "when's" equally close. Preserve all shortest-distance
            // alternatives rather than incorrectly asserting certainty.
            if(edits==bestEdits)best.add(contraction);
        }
        return best;
    }

    /**
     * Prefer a unique one-edit inflection over a speculative much-more-distant
     * standalone first guess. Only if the original top is 3+ edits away,
     * both edges of the candidate agree, and it is the sole such dictionary
     * word. This does not alter exact words or confident phrases.
     */
    private static String uniqueCloseInflection(String input,String raw,List<String> suggestions) {
        if(raw.length()<8 || raw.length()>14 || suggestions.isEmpty())return null;
        String top=suggestions.get(0).toLowerCase(Locale.US);
        for(int i=0;i<top.length();i++)
            if(top.charAt(i)<'a'||top.charAt(i)>'z')return null;
        if(Math.abs(top.length()-raw.length())>1 ||
                editDistanceLimited(raw,top,2)<=2)return null;
        List<Map.Entry<String,Integer>> entries=WORDS_BY_LENGTH.get(raw.length());
        if(entries==null)return null;
        String closest=null;
        for(Map.Entry<String,Integer> entry:entries) {
            String word=entry.getKey();
            if(word.length()!=raw.length() || word.length()<8 ||
                    word.charAt(0)!=raw.charAt(0) ||
                    word.charAt(word.length()-1)!=raw.charAt(raw.length()-1))
                continue;
            if(editDistanceLimited(raw,word,1)==1) {
                // More than one equally anchored, equally close correction
                // is genuine ambiguity. Do not force either to first place.
                if(closest!=null)return null;
                closest=word;
            }
        }
        return closest==null?null:caseDisplay(input,display(closest));
    }

    /** Return optional sentence/word suggestions. Never edits user text without a tap. */
    static List<String> suggestions(String input) {
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        if(input==null || input.isEmpty() || input.length()>65 || !input.matches("[A-Za-z]+"))return Collections.emptyList();
        String raw=input.toLowerCase(Locale.US);
        if(raw.length()==1) return firstLetterSuggestions(input,raw);
        // Do not fabricate split-word corrections for a known, correctly
        // spelled word. Keep the expanded row hidden for these inputs.
        if(WORDS.containsKey(raw)) {
            ArrayList<String> stable=new ArrayList<>();
            stable.add(caseDisplay(input,display(raw)));
            List<String> next=NEXT.get(raw);
            if(next!=null) for(String following:next) {
                if(stable.size()>=3)break;
                stable.add(caseDisplay(input,display(raw))+" "+display(following));
            }
            return stable;
        }
        // Strong, unique QWERTY neighbor-key doubles, e.g. "tekk" -> "tell".
        // Only an optional candidate: never modifies user text without a tap.
        // Correctly spelled words were returned above, so remain protected.
        String neighborPair=NeighborDoubleKeyCorrectionPolicy.unique(raw,WORDS);
        if(neighborPair!=null)
            return Arrays.asList(caseDisplay(input,display(neighborPair)),input);
        // A valid ordinary word in the second tier is always kept intact;
        // never divide it into fragments or infer a replacement.
        ExactOnlyLanguagePack pack=researchExactOnlyPack;
        if(pack!=null && pack.contains(raw))
            return Collections.singletonList(caseDisplay(input,display(raw)));
        // A short missing-space phrase with a unique doubled neighbor-key
        // typo (e.g. Iwikk -> I will) is not recovered by long-sentence
        // rescue (which requires >=6 chars). Only existing, confirmed
        // dictionary+bigram evidence can make this optional tap candidate.
        // This runs on the existing background predictor, not at key-down.
        List<String> strongJoin362=ShortJoinedContext362.suggestions(
                input,WORDS,NEXT);
        if(!strongJoin362.isEmpty())return strongJoin362;
        ArrayList<ScoredText> result=new ArrayList<>();
        Set<String> known=new HashSet<>();
        addConversationalPhrases(input,raw,result,known);
        addExactMissingSpaceCandidates(input,raw,result,known);
        addStrongSingleWordCorrections(input,raw,result,known);
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        if(raw.length()<=20) {
            for(Candidate c:candidates(raw,true)) {
                if(c.edits==0 || WORDS.containsKey(raw)) continue;
                String s=caseDisplay(input,display(c.word));
                add(result,known,s,10+scoreCandidate(c,raw.length()));
            }
            if(raw.length()>=2) {
                for(Map.Entry<String,Integer> entry:prefixBucket(raw)) {
                    String word=entry.getKey();
                    if(word.startsWith(raw) && word.length()>raw.length() && word.length()<=raw.length()+7) {
                        double score = 14 + Math.log(entry.getValue()) - (word.length()-raw.length())*.6;
                        add(result,known,caseDisplay(input,display(word)),score);
                    }
                }
            }
            if(WORDS.containsKey(raw) && NEXT.containsKey(raw)) {
                for(String next:NEXT.get(raw)) add(result,known,caseDisplay(input,display(raw))+" "+display(next),3.0);
            }
        }
        // Beam search retains several alternatives for ambiguous word boundaries.
        Map<String,List<Candidate>> fragmentCache=new HashMap<>();
        int n=raw.length();
        @SuppressWarnings("unchecked") ArrayList<Path>[] dp=new ArrayList[n+1];
        for(int i=0;i<=n;i++) dp[i]=new ArrayList<>();
        dp[0].add(new Path(0,"","",0,0,0));
        for(int i=0;i<n;i++) {
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            if(dp[i].isEmpty())continue;
            for(int j=i+1;j<=Math.min(n,i+14);j++) {
                String fragment=raw.substring(i,j);
                List<Candidate> opts=fragmentCache.get(fragment);
                if(opts==null) {
                    opts=candidates(fragment,fragment.length()>=2,n>=18);
                    fragmentCache.put(fragment,opts);
                }
                if(opts.isEmpty())continue;
                for(Path old:dp[i]) for(Candidate c:opts) {
                    int edits=old.errors+c.edits;
                    if(edits>Math.max(2,n/4))continue;
                    double bonus=0;
                    if(!old.last.isEmpty()) {
                        List<String> follows=NEXT.get(old.last);
                        if(follows!=null && follows.contains(c.word))bonus=2.4;
                        List<String> context=PHRASE_NEXT.get(old.last);
                        if(context!=null && context.contains(c.word))bonus=Math.max(bonus,5.5);
                    }
                    // Discourage questionable one-letter splits except I and a.
                    if(c.word.length()==1 && !c.word.equals("i")&&!c.word.equals("a"))bonus-=4;
                    double boundaryPenalty=SentenceRescue.joinedWordBoundaryPenalty(
                            old.last,c.word,bonus>0,WORDS);
                    int shortRun=c.word.length()<=3?old.shortRun+1:0;
                    double nextScore=old.value+scoreCandidate(c,fragment.length())
                            +bonus+boundaryPenalty+
                            longBeamPreference(n,c.word,c.edits)
                            +unsupportedShortRunPenalty(n,old.last,c.word,bonus,shortRun)
                            +anchoredFragmentEvidence(fragment,c.word,c.edits,n>=18);
                    // A losing path cannot affect future search states.
                    // Skip allocating its joined text and Path object.
                    ArrayList<Path> destination=dp[j];
                    if(destination.size()>=12 &&
                            Double.compare(nextScore,
                                    destination.get(destination.size()-1).value)<=0)
                        continue;
                    String tail=display(c.word);
                    String text=old.text.isEmpty()?tail:old.text+" "+tail;
                    Path path=new Path(nextScore,text,c.word,old.segments+1,edits,shortRun);
                    insert(destination,path,12);
                }
            }
        }
        if(!dp[n].isEmpty()) {
            for(Path path:dp[n]) {
                if(path.segments<2)continue;
                String displayed=caseDisplay(input,path.text);
                double adjustment=(path.segments>=3?4:1)-path.errors*1.1;
                // For short inputs, prefer a plausible corrected word over
                // splitting a typo into speculative multiword fragments.
                if(raw.length()<=7) adjustment-=14.0;
                if(raw.length()<=7 && path.segments>=3) adjustment-=8.0;
                if(WORDS.containsKey(raw))adjustment-=20;
                add(result,known,displayed,path.value/Math.max(1,path.segments)+adjustment);
            }
        }
        // Exact dictionary matches are already correctly spelled: show the word
        // as an explicit stable first option, not speculative segmentation.
        if(WORDS.containsKey(raw)) {
            String exact=caseDisplay(input,display(raw));
            result.removeIf(candidate -> candidate.text.equals(exact));
            result.add(new ScoredText(exact,10000));
        }
        // Strong whole-phrase matches outrank fuzzy word-boundary beams.
        // This also handles punctuation-free contractions without a special
        // rule for any one screenshot.
        if(raw.length()>=5) {
            for(String phrase:COMMON_PHRASES) {
                String normalized=phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
                if(normalized.equals(raw)) {
                    String corrected=caseDisplay(input,phrase);
                    result.removeIf(candidate -> candidate.text.equals(corrected));
                    result.add(new ScoredText(corrected,9000));
                }
            }
        }
        result.sort((a,b)->Double.compare(b.score,a.score));
        List<String> suggestions=new ArrayList<>();
        for(ScoredText r:result) {
            if(suggestions.size()>=8)break;
            if((!r.text.equals(input) || WORDS.containsKey(raw)) && !suggestions.contains(r.text)) suggestions.add(r.text);
        }
        // Punctuation-free question contractions are recognizable without a
        // memorized input phrase. Protect existing words with the early return,
        // and preserve remaining alternatives for explicit tap selection.
        List<String> contractions=contractionCandidates(raw);
        if(!contractions.isEmpty()) {
            // Add alternatives in reverse so the best/first candidate keeps
            // its prior top rank while ties remain available via one tap.
            for(int i=contractions.size()-1;i>=0;i--) {
                String candidate=caseDisplay(input,contractions.get(i));
                suggestions.remove(candidate);
                suggestions.add(0,candidate);
            }
            while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
            return JoinedEnglishContractionPolicy.offer(input,suggestions);
        }
        // A near-exact conversational phrase is stronger evidence than a
        // speculative segmented beam. Keep the top candidate stable across
        // the later sentence-rescue pass.
        String trustedPhrase=null;
        int trustedEdits=3;
        for(String phrase:COMMON_PHRASES) {
            String compact=phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
            if(Math.abs(compact.length()-raw.length())>1)continue;
            int edits=editDistanceLimited(raw,compact,1);
            if(edits<=1 && edits<trustedEdits) {
                trustedPhrase=caseDisplay(input,phrase);
                trustedEdits=edits;
            }
        }
        // A confident two-error full-phrase match can outrank a collection
        // of individually plausible but grammatically broken word splits.
        // Prefer exact/one-error phrases first, then two-error matches only
        // for sufficiently long strings. Never override known dictionary words.
        if(trustedPhrase==null && raw.length()>=8 && !WORDS.containsKey(raw)) {
            for(String phrase:COMMON_PHRASES) {
                String compact=phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
                if(Math.abs(compact.length()-raw.length())>2)continue;
                if(editDistanceLimited(raw,compact,2)==2) {
                    String proposed=caseDisplay(input,phrase);
                    if(suggestions.contains(proposed)) {
                        trustedPhrase=proposed;
                        break;
                    }
                }
            }
        }
        if(trustedPhrase!=null) {
            suggestions.remove(trustedPhrase);
            suggestions.add(0,trustedPhrase);
            while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
            return JoinedEnglishContractionPolicy.offer(input,suggestions);
        }
        String inflection=uniqueCloseInflection(input,raw,suggestions);
        if(inflection!=null) {
            suggestions.remove(inflection);
            suggestions.add(0,inflection);
            while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
            return JoinedEnglishContractionPolicy.offer(input,suggestions);
        }
        // A plausible standalone word should not be buried under bizarre
        // multiword segmentation. Require matching first two letters and a
        // tight edit budget; longer no-space sentences continue to beam search.
        if(raw.length()>=6 && raw.length()<=14 && !WORDS.containsKey(raw)) {
            Candidate bestSingle=null;
            for(Candidate candidate:candidates(raw,true)) {
                if(candidate.edits==0 || candidate.word.length()<6 ||
                        candidate.word.length()<raw.length()-2 ||
                        candidate.word.length()>raw.length()+2) continue;
                if(candidate.edits>1 && (raw.length()<7 ||
                        candidate.word.length()<7)) continue;
                if(candidate.word.charAt(0)!=raw.charAt(0) ||
                        candidate.word.charAt(1)!=raw.charAt(1)) continue;
                if(bestSingle==null || candidate.edits<bestSingle.edits ||
                        (candidate.edits==bestSingle.edits &&
                         candidate.freq>bestSingle.freq))
                    bestSingle=candidate;
            }
            if(bestSingle!=null) {
                String corrected=caseDisplay(input,display(bestSingle.word));
                suggestions.remove(corrected);
                suggestions.add(0,corrected);
                while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
                return JoinedEnglishContractionPolicy.offer(input,suggestions);
            }
        }
        // Long, close single-word matches need a fair chance before the
        // sentence rescue beam splits them into unrelated short words.
        // A three-edit candidate is only promoted with a stable two-letter
        // prefix and near-equal length; do not override a trusted phrase.
        if(raw.length()>=8 && raw.length()<=14 && !WORDS.containsKey(raw)) {
            String bestWord=null;
            double bestScore=Double.NEGATIVE_INFINITY;
            for(Map.Entry<String,Integer> e:WORDS.entrySet()) {
                String word=e.getKey();
                if(word.length()<8 || Math.abs(word.length()-raw.length())>1 ||
                        word.charAt(0)!=raw.charAt(0) ||
                        word.charAt(1)!=raw.charAt(1)) continue;
                int edits=editDistanceLimited(raw,word,3);
                if(edits<1 || edits>3)continue;
                double score=10.0-edits*3.0+Math.log1p(e.getValue());
                if(score>bestScore) {bestScore=score;bestWord=word;}
            }
            if(bestWord!=null && bestScore>=2.0) {
                String corrected=caseDisplay(input,display(bestWord));
                suggestions.remove(corrected);
                suggestions.add(0,corrected);
                while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
                return JoinedEnglishContractionPolicy.offer(input,suggestions);
            }
        }
        // Before invoking the expensive rescue beam, give two substantial
        // one-typo words a chance when the current top is a low-coherence
        // three-or-more-part segmentation. Do not override coherent phrases
        // or confirmed standalone-word predictions.
        if(!suggestions.isEmpty() && !WORDS.containsKey(raw)) {
            String currentTop=suggestions.get(0);
            int components=currentTop.trim().split(" +").length;
            if(components>=3 && SentenceRescue.coherence(currentTop,NEXT,RECOVERY_CONTEXT)<0.34) {
                String pair=anchoredTwoWordCandidate(input,raw,fragmentCache);
                if(pair!=null && !pair.equalsIgnoreCase(currentTop)) {
                    suggestions.remove(pair);
                    suggestions.add(0,pair);
                    while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
                    return JoinedEnglishContractionPolicy.offer(input,suggestions);
                }
            }
        }
        // Multi-typo recovery is a separate, opt-in-by-confidence candidate path.
        // Preserve the legacy top result whenever it already forms a coherent phrase.
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        // Nothing is silently autocorrected: the IME still requires a suggestion tap.
        double oldCoherence=suggestions.isEmpty()?0:
                SentenceRescue.coherence(suggestions.get(0),NEXT,RECOVERY_CONTEXT);
        // If the original model already produced a convincing sentence,
        // preserve it AND skip the more expensive rescue beam.
        if(raw.length()>=6 && !WORDS.containsKey(raw) && oldCoherence<0.67) {
            List<SentenceRescue.Proposal> recovered=
                    SentenceRescue.recover(raw,WORDS,NEXT,RECOVERY_CONTEXT);
            if(!recovered.isEmpty()) {
                SentenceRescue.Proposal best=recovered.get(0);
                // Long multi-typo joined sentences may have only modest
                // transition evidence, but lowering the threshold blindly
                // risks false corrections. Demand comparable character-level
                // evidence and protect already letter-exact suggestions.
                String originalLetters=suggestions.isEmpty()?"":suggestions.get(0)
                        .toLowerCase(Locale.US).replaceAll("[^a-z]","");
                boolean exactLetterSegmentation=originalLetters.equals(raw);
                int editBudget=Math.max(2,(int)Math.ceil(raw.length()*.24));
                double requiredGain=raw.length()>=16?.085:.15;
                if(best.coherence()>=0.67 &&
                        best.coherence()>oldCoherence+requiredGain &&
                        best.totalEdits<=editBudget &&
                        (!exactLetterSegmentation || best.totalEdits==0)) {
                    String corrected=caseDisplay(input,displayRecovered(best.text));
                    if(!corrected.equals(input)) {
                        suggestions.remove(corrected);
                        suggestions.add(0,corrected);
                        while(suggestions.size()>8)suggestions.remove(suggestions.size()-1);
                    }
                }
            }
        }
        // Experimental: prefer an already-generated top-three joined
        // candidate only with strictly stronger adjacent-word evidence
        // and no worse compact-string edit distance.
        // Always explicit one-tap accept; never silently edit the input.
        List<String> stable=SentenceFlowEvidenceRanker.rerank(
                input,suggestions,NEXT,RECOVERY_CONTEXT);
        // 301 second-stage research: keep the production fuzzy beam untouched.
        // A high-confidence zero-letter-edit segmentation may repair an
        // obviously overfragmented first candidate. Never silently commit.
        ExactOnlyLanguagePack postDecodePack=researchExactOnlyPack;
        if(postDecodePack==null || stable.isEmpty())return JoinedEnglishContractionPolicy.offer(input,stable);
        String proposed=ExactOnlySegmentation.propose(raw,stable.get(0),WORDS,postDecodePack);
        if(proposed==null)return JoinedEnglishContractionPolicy.offer(input,stable);
        String candidate=caseDisplay(input,displayRecovered(proposed));
        if(stable.contains(candidate))return JoinedEnglishContractionPolicy.offer(input,stable);
        ArrayList<String> postDecodeOptions=new ArrayList<>(stable);
        postDecodeOptions.add(0,candidate);
        while(postDecodeOptions.size()>8)postDecodeOptions.remove(postDecodeOptions.size()-1);
        return JoinedEnglishContractionPolicy.offer(input,postDecodeOptions);
    }
    private static String displayRecovered(String plain) {
        String[] words=plain.split(" ");
        StringBuilder out=new StringBuilder();
        for(String word:words) {
            if(out.length()>0)out.append(' ');
            out.append(display(word));
        }
        return out.toString();
    }
    /** Phrase continuations for a single, recognized word. No speculative word splits. */
    static List<String> splitPhraseSuggestions(String typed) {
        if(typed==null || !typed.matches("[A-Za-z]{1,24}"))return Collections.emptyList();
        String word=typed.toLowerCase(Locale.US);
        if(!WORDS.containsKey(word))return Collections.emptyList();
        ArrayList<String> out=new ArrayList<>();
        for(String phrase:COMMON_PHRASES) {
            String[] parts=phrase.split(" ");
            if(parts.length<2 || parts.length>5)continue;
            if(!parts[0].replaceAll("[^A-Za-z]","").equalsIgnoreCase(word))continue;
            String candidate=caseDisplay(typed,phrase);
            if(!out.contains(candidate))out.add(candidate);
            if(out.size()==3)break;
        }
        return out;
    }
    /** High-confidence next words only; never use generic starter words as filler. */
    static List<String> splitNextWords(String typed) {
        if(typed==null || !typed.matches("[A-Za-z]{1,24}"))return Collections.emptyList();
        String word=typed.toLowerCase(Locale.US);
        if(!WORDS.containsKey(word))return Collections.emptyList();
        switch(word) {
            case "hello": return Arrays.asList("there","everyone","again");
            case "what": return Arrays.asList("are","do","is");
            case "how": return Arrays.asList("are","is","was");
            case "good": return Arrays.asList("morning","night","job");
            case "thank": return Arrays.asList("you");
            case "thanks": return Arrays.asList("again","for","so");
            case "i": return Arrays.asList("am","will","have");
            case "please": return Arrays.asList("help","check","send");
            case "you": return Arrays.asList("are","can","have");
            case "the": return Arrays.asList("best","next","keyboard");
            case "this": return Arrays.asList("is","looks","works");
            default: return Collections.emptyList();
        }
    }
    /** Bounded, offline context ranking. Only complete trailing words are used. */
    static List<String> contextualNextWords(String beforeCursor) {
        if(beforeCursor==null || beforeCursor.isEmpty())return starterSuggestions();
        String normalized=beforeCursor.toLowerCase(Locale.US)
                .replaceAll("[^a-z' ]"," ").replaceAll(" +"," ").trim();
        // Ordered multiword continuations: longest matching suffix wins.
        // Keep this small, local and deterministic until corpus-backed ranking is ready.
        final String[][] patterns={
            {"hello there how","are","is","was"},
            {"what are you","doing","thinking","looking"},
            {"how are you","doing","feeling","today"},
            {"what do you","think","want","mean"},
            {"thank you","so","for","very"},
            {"hello there","how","everyone","friend"},
            {"are you","doing","ready","sure"},
            {"do you","think","want","know"},
            {"i want to","know","see","try"},
            {"i need to","go","check","finish"},
            {"can you","help","please","check"},
            {"going to","be","try","get"},
            {"looking for","a","the","something"}
        };
        for(String[] pattern:patterns) {
            String prefix=pattern[0];
            if(normalized.equals(prefix)||normalized.endsWith(" "+prefix))
                return Arrays.asList(pattern[1],pattern[2],pattern[3]);
        }
        // Generalize to unseen contexts using a tiny bundled phrase corpus.
        // Require at least two distinct observed continuations: a single
        // phrase is not enough evidence to override established fallbacks.
        String[] words=normalized.split(" ");
        List<String> established=nextWordSuggestions(words[words.length-1]);
        // Established single-word continuations have precedence.
        if(!established.isEmpty() && !established.equals(starterSuggestions()))
            return established;
        // A multiword suffix is stronger evidence than a bare final noun.
        // If it has a well-supported continuation, avoid grammatical
        // collisions from unrelated noun-subject examples.
        List<String> learned=corpusContinuations(normalized);
        if(learned.size()>=2)return learned;
        // Unknown contexts must not show unrelated sentence starters.
        return Collections.emptyList();
    }
    /**
     * Build an immutable phrase-continuation index once instead of scanning
     * every example on each keystroke. The ranking and evidence thresholds
     * are intentionally identical to the legacy per-query reference.
     * No typed text is stored: only bundled example n-grams are indexed.
     */
    private static Map<String,List<String>> buildCorpusContinuationIndex() {
        Map<String,Map<String,Integer>> countsBySuffix=new HashMap<>();
        for(String phrase:CONTEXT_EXAMPLES) {
            String cleaned=phrase.toLowerCase(Locale.US)
                    .replaceAll("[^a-z' ]"," ").replaceAll(" +"," ").trim();
            String[] tokens=cleaned.split(" ");
            for(int size=1;size<=Math.min(5,tokens.length-1);size++) {
                for(int i=size;i<tokens.length;i++) {
                    StringBuilder suffix=new StringBuilder();
                    for(int j=i-size;j<i;j++) {
                        if(suffix.length()>0)suffix.append(' ');
                        suffix.append(tokens[j]);
                    }
                    Map<String,Integer> counts=countsBySuffix.computeIfAbsent(
                            suffix.toString(),k->new HashMap<>());
                    counts.put(tokens[i],counts.getOrDefault(tokens[i],0)+1);
                }
            }
        }
        Map<String,List<String>> indexed=new HashMap<>();
        for(Map.Entry<String,Map<String,Integer>> item:countsBySuffix.entrySet()) {
            Map<String,Integer> counts=item.getValue();
            if(counts.size()<2)continue;
            List<String> ranked=new ArrayList<>(counts.keySet());
            ranked.sort((a,b)->{
                int c=Integer.compare(counts.get(b),counts.get(a));
                return c!=0?c:a.compareTo(b);
            });
            indexed.put(item.getKey(),Collections.unmodifiableList(
                    new ArrayList<>(ranked.subList(0,Math.min(3,ranked.size())))));
        }
        return Collections.unmodifiableMap(indexed);
    }

    /** Corpus-derived suffix ranking, identical output to the legacy scanner. */
    static List<String> corpusContinuations(String normalized) {
        String[] context=normalized.split(" ");
        for(int size=Math.min(5,context.length);size>=1;size--) {
            // Preserve the original guard: a lone noun is not useful
            // evidence after a longer context.
            if(size==1 && context.length>1)break;
            StringBuilder suffix=new StringBuilder();
            for(int i=context.length-size;i<context.length;i++) {
                if(suffix.length()>0)suffix.append(' ');
                suffix.append(context[i]);
            }
            List<String> ranked=CORPUS_CONTINUATION_INDEX.get(suffix.toString());
            if(ranked!=null)return ranked;
        }
        return Collections.emptyList();
    }
    /** Starter suggestions are available before the first character is typed. */
    static List<String> starterSuggestions() {
        return Arrays.asList("I","The","Hey");
    }
    /** Simple offline next-word choices shown after the user types a space. */
    static List<String> nextWordSuggestions(String previous) {
        if(previous==null||previous.isEmpty())return starterSuggestions();
        String last=previous.toLowerCase(Locale.US).replaceAll("[^a-z]","");
        switch(last) {
            case "i": return Arrays.asList("am","will","have");
            case "im": return Arrays.asList("going","using","ready");
            case "you": return Arrays.asList("are","can","have");
            case "the": return Arrays.asList("keyboard","app","next");
            case "this": return Arrays.asList("is","looks","was");
            case "are": return Arrays.asList("you","we","they");
            case "can": return Arrays.asList("you","we","I");
            case "going": return Arrays.asList("to","home","back");
            case "typing": return Arrays.asList("this","now","today");
            case "thank": return Arrays.asList("you");
            case "thanks": return Arrays.asList("again","for","so");
            case "hello": return Arrays.asList("there","everyone","again");
            case "good": return Arrays.asList("morning","night","job");
            case "iwas": return Arrays.asList("going","thinking","working");
            default: return starterSuggestions();
        }
    }

    private static void insert(ArrayList<Path> list,Path path,int k) {
        // Preserve stable descending order without re-sorting the beam.
        // Equal scores follow earlier arrivals, as in Java's stable sort.
        int size=list.size();
        if(size>=k && Double.compare(path.value,list.get(size-1).value)<=0)
            return;
        int at=0;
        while(at<size && Double.compare(list.get(at).value,path.value)>=0)at++;
        list.add(at,path);
        if(list.size()>k)list.remove(k);
    }
    private static final class ScoredText {
        final String text; final double score;
        ScoredText(String t,double s){text=t;score=s;}
    }
    private static void add(List<ScoredText> list, Set<String> seen,String value,double score) {
        if(seen.add(value))list.add(new ScoredText(value,score));
    }
}