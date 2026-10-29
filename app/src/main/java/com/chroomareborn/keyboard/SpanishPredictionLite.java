package com.chroomareborn.keyboard;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Experimental offline Spanish lexicon and joined-word research engine.
 *
 * No Context, network, editor access, learning, or persistent user strings.
 * Explicit language routing is required: ENGLISH stays on PredictionLite.
 * The research engine never silently commits text. Vocabulary is generic
 * language material, NOT a memorized list of user messages.
 */
final class SpanishPredictionLite {
    private SpanishPredictionLite() {}

    private static final class Word {
        final String display,normalized;
        final int frequency;
        Word(String display,int frequency){
            this.display=display;
            this.normalized=fold(display);
            this.frequency=frequency;
        }
    }
    private static final class Choice {
        final Word word;
        final int edits;
        final double score;
        Choice(Word w,int edits,double score){
            word=w;this.edits=edits;this.score=score;
        }
    }
    private static final class Path {
        final String text,last;
        final double score;
        final int words,errors;
        Path(String text,String last,double score,int words,int errors){
            this.text=text;this.last=last;this.score=score;
            this.words=words;this.errors=errors;
        }
    }
    private static final Map<String,Word> EXACT=new HashMap<>();
    private static final List<Word> WORDS=new ArrayList<>();
    private static final Map<String,Set<String>> NEXT=new HashMap<>();

    private static final String FUNCTION_WORDS=
        "a al algo alguien algunos algunas ambos ante antes aquí así aunque "
        +"bajo bien bueno buenos buenas cada casi como con contra cual cuales "
        +"cuando de del desde donde dos el ella ellas ellos en entre era eran "
        +"eres es esa esas ese esos esta estás estamos están estar este esto "
        +"estos estoy fue fueron fui haber había hace hacen hacer hacia hay "
        +"hoy la las le les lo los más menos mi mis muy nada nadie ni no nos "
        +"nosotros nuestra nuestro o para pero por porque pues que qué quien "
        +"quién quiero quieres queremos se sea ser si sí sin sobre solo son "
        +"soy su sus también tan tanto te tener tengo tiene tienes tienen "
        +"ti tiempo todo todos todas tu tus un una unas unos usted ustedes "
        +"va vamos van veo ver veces vez y ya yo tú él mí míos nosotros";
    private static final String CONVERSATION_WORDS=
        "hola adiós gracias muchas mucho mucho gusto buenos días buenas noches "
        +"tardes mañana ahora luego después hasta pronto saludos perdón disculpa "
        +"disculpe favor porfavor bienvenida bienvenido encantado felicidades "
        +"excelente genial perfecto claro seguro correcto cierto verdad realmente "
        +"todavía siempre nunca quizá quizás probablemente posible imposible "
        +"necesito necesitas necesita necesitamos necesito ayuda ayudar ayudarte "
        +"puedo puedes puede podemos quieren quieren quería quisiera querer "
        +"saber sabes sabe sabemos hablar hablando hablo hablas hablamos "
        +"escribir escribiendo escrito escribes escribe escribimos "
        +"leer leyendo leo lees lee mensaje mensajes respuesta responder "
        +"pregunta preguntas preguntando entender entiendo entiendes entendemos "
        +"entendido comprendo comprende comprender sabemos conocer conozco "
        +"conocemos aprender aprendiendo aprendido aprendemos "
        +"enviar enviando enviamos envié enviará mandar mandando "
        +"llamar llamando llamo llamas llamamos después ahora pronto";
    private static final String EVERYDAY_WORDS=
        "casa casas trabajo trabajos escuela clase estudiante estudiantes "
        +"oficina coche carro carros bicicleta moto patinete teléfono teléfonos "
        +"pantalla pantallas teclado teclados aplicación aplicaciones "
        +"programa programas computadora computadoras ordenador internet español española "
        +"red redes conexión conexiones configuración configuraciones ajustes "
        +"actualización actualizaciones versión versiones archivo archivos "
        +"foto fotos vídeo videos cámara cámaras batería baterías cargador "
        +"cargar carga energía luz luces oscuro clara claro brillo "
        +"color colores azul verde rojo negra negro blanco blanca "
        +"tamaño tamaños letra letras palabra palabras frase frases "
        +"oración oraciones texto textos escritura escribir escribiendo "
        +"predicción predicciones sugerencia sugerencias "
        +"corrección correcciones error errores prueba pruebas "
        +"velocidad rápido rápida rápido lenta lento mejor mejores "
        +"nuevo nueva nuevos nuevas viejo vieja viejo antiguo "
        +"grande grandes pequeño pequeña pequeños pequeñas "
        +"bonito bonita bonitos bonitas hermoso hermosa "
        +"amigo amiga amigos amigas familia hermano hermana "
        +"madre padre mamá papá hijo hija niños niñas "
        +"persona personas gente hombre mujer mujeres "
        +"nombre nombres ciudad ciudades calle calles "
        +"dirección direcciones dirección casa casas "
        +"tienda tiendas mercado comida comidas agua café "
        +"pan fruta frutas desayuno almuerzo cena "
        +"mañana tarde noche día días semana semanas mes meses año años "
        +"lunes martes miércoles jueves viernes sábado domingo "
        +"ayer hoy mañana ayer tarde noche temprano "
        +"mundo país países viaje viajes lugar lugares "
        +"parque parques música canciones canción "
        +"juego juegos jugar jugando libro libros película películas";
    private static final String ACTION_WORDS=
        "voy vas vamos va van ir iba ibas iba iremos iré irás "
        +"vengo viene vienen venir venido regresó regreso regresando "
        +"camino camina caminar caminando caminamos caminaste "
        +"quiero quieres quiere queremos quieren quería querían "
        +"tengo tienes tiene tenemos tienen tenía tenían "
        +"puedo puedes puede podemos pueden podía "
        +"siento sientes siente sentimos sentir "
        +"está estamos están estará estaré estaban "
        +"fue fueron era eras eran seré sería "
        +"hago haces hace hacemos hacen haciendo hecho "
        +"veo ves ve vemos viendo visto vería "
        +"digo dices dice decimos dicen diciendo dicho "
        +"quiero aprender mejorar arreglar reparar "
        +"funciona funcionan funcionando funcionaba "
        +"trabajo trabajas trabaja trabajamos trabajando trabajar "
        +"esperar esperando espero esperas esperamos "
        +"gustar gusta gustan gustó me te le les "
        +"necesito necesitamos necesita necesitar "
        +"revisar comprobando comprobar probando probar "
        +"continuar continúa continuamos continuando "
        +"encontrar encuentro encuentras encontramos "
        +"terminar termino termina terminamos terminé "
        +"hacerlo verlo decirlo escribirlo leerlo";
    private static void addWords(String corpus,int frequency) {
        for(String raw:corpus.split(" +")){
            if(raw.isEmpty())continue;
            String literal=raw.toLowerCase(Locale.ROOT);
            if(!accepted(literal))continue;
            if(!EXACT.containsKey(literal)){
                Word word=new Word(literal,frequency);
                EXACT.put(literal,word);
                WORDS.add(word);
            }
        }
    }
    private static void next(String left,String... right) {
        Set<String> out=NEXT.computeIfAbsent(left,k->new HashSet<>());
        for(String word:right)out.add(fold(word));
    }
    static {
        addWords(FUNCTION_WORDS,60);
        addWords(CONVERSATION_WORDS,38);
        addWords(EVERYDAY_WORDS,24);
        addWords(ACTION_WORDS,18);
        // Generic conversational transitions and sentence grammar evidence,
        // NOT whole-sentence memorization.
        next("buenos","días","amigos");
        next("buenas","noches","tardes");
        next("hola","cómo","que","amigo");
        next("cómo","estás","te","va","se","está");
        next("estoy","en","bien","aquí","trabajando");
        next("estás","bien","en","aquí");
        next("quiero","saber","ver","hacer","aprender","ir");
        next("necesito","ayuda","saber","ver","hablar");
        next("muchas","gracias","veces");
        next("gracias","por","a","amigo");
        next("por","favor","eso","la","el","que");
        next("que","estás","es","la","el","te","yo");
        next("de","la","las","los","mi","su","un","una");
        next("en","la","el","casa","trabajo","este");
        next("la","casa","pantalla","batería","aplicación","vida");
        next("el","teclado","teléfono","trabajo","coche","día");
        next("mi","casa","teléfono","amigo","familia","teclado");
        next("tu","casa","mensaje","teléfono","amigo");
        next("está","bien","aquí","en","funcionando");
        next("vamos","a","al","para");
        next("voy","a","al","para");
        next("ya","estoy","está","vamos");
        next("te","quiero","llamo","veo","escribo");
        next("me","gusta","gustan","puedes","ayudas");
        next("sin","problema","internet","la");
        next("más","tarde","rápido","tiempo");
        next("muy","bien","bueno","bonita","rápido");
        next("nos","vemos","vamos");
        next("hasta","mañana","luego","pronto");
    }

    static String fold(String value) {
        if(value==null)return "";
        String input=Normalizer.normalize(value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFC);
        StringBuilder output=new StringBuilder(input.length());
        for(int i=0;i<input.length();i++){
            char ch=input.charAt(i);
            switch(ch){
                case 'á':case 'à':case 'â':output.append('a');break;
                case 'é':case 'è':case 'ê':output.append('e');break;
                case 'í':case 'ì':output.append('i');break;
                case 'ó':case 'ò':case 'ô':output.append('o');break;
                case 'ú':case 'ù':case 'û':case 'ü':output.append('u');break;
                default:output.append(ch);
            }
        }
        return output.toString();
    }

    private static boolean accepted(String input){
        if(input==null||input.isEmpty()||input.length()>65)return false;
        for(int i=0;i<input.length();i++){
            char c=fold(String.valueOf(input.charAt(i))).charAt(0);
            if(!(c>='a'&&c<='z')&&c!='ñ')return false;
        }
        return true;
    }
    private static String casing(String source,String target) {
        if(source.isEmpty()||target.isEmpty())return target;
        if(source.equals(source.toUpperCase(Locale.ROOT)) &&
                source.length()>=2)return target.toUpperCase(Locale.ROOT);
        if(Character.isUpperCase(source.charAt(0)))
            return Character.toUpperCase(target.charAt(0))+target.substring(1);
        return target;
    }
    private static int distance(String a,String b,int budget){
        if(Math.abs(a.length()-b.length())>budget)return budget+1;
        int[] prev=new int[b.length()+1],curr=new int[b.length()+1];
        for(int j=0;j<=b.length();j++)prev[j]=j;
        for(int i=1;i<=a.length();i++){
            curr[0]=i;
            for(int j=1;j<=b.length();j++){
                int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
                curr[j]=Math.min(Math.min(prev[j]+1,curr[j-1]+1),
                        prev[j-1]+cost);
            }
            int[] tmp=prev;prev=curr;curr=tmp;
        }
        return prev[b.length()];
    }
    private static List<Choice> matches(String fragment,boolean fuzzy) {
        ArrayList<Choice> out=new ArrayList<>();
        int max=fuzzy && fragment.length()>=4?1:0;
        for(Word word:WORDS) {
            if(word.normalized.length()>14 ||
                    Math.abs(fragment.length()-word.normalized.length())>max)
                continue;
            if(fragment.length()>1 && word.normalized.length()>1 &&
                    fragment.charAt(0)!=word.normalized.charAt(0))continue;
            int edits=distance(fragment,word.normalized,max);
            if(edits>max)continue;
            double score=Math.log1p(word.frequency)*1.4+
                    Math.min(5,word.normalized.length())*.6-
                    edits*5.0-2.5;
            out.add(new Choice(word,edits,score));
        }
        out.sort(Comparator.comparingDouble((Choice a)->-a.score));
        return out.size()>12?out.subList(0,12):out;
    }
    private static void insert(List<Path> dest,Path p) {
        int position=0;
        while(position<dest.size() && dest.get(position).score>=p.score)position++;
        if(position>=8)return;
        dest.add(position,p);
        if(dest.size()>8)dest.remove(8);
    }

    static List<String> suggestions(String typed) {
        if(typed==null || Thread.currentThread().isInterrupted())
            return Collections.emptyList();
        String cleaned=Normalizer.normalize(typed,Normalizer.Form.NFC);
        if(!accepted(cleaned))return Collections.emptyList();
        String lower=cleaned.toLowerCase(Locale.ROOT);
        Word known=EXACT.get(lower);
        if(known!=null) {
            // An already correctly spelled Spanish word is not replaced
            // automatically or promoted beneath an unrelated segmentation.
            ArrayList<String> stable=new ArrayList<>();
            stable.add(casing(typed,known.display));
            return stable;
        }
        String raw=fold(cleaned);
        ArrayList<Choice> whole= new ArrayList<>();
        if(raw.length()<=14) {
            for(Word w:WORDS) {
                int max=raw.length()>=6?2:raw.length()>=3?1:0;
                if(Math.abs(raw.length()-w.normalized.length())>max)continue;
                if(raw.length()>=2 && w.normalized.length()>=2 &&
                        raw.charAt(0)!=w.normalized.charAt(0))continue;
                int edits=distance(raw,w.normalized,max);
                if(edits>max)continue;
                double score=Math.log1p(w.frequency)*2.2-
                        edits*4.2-(w.normalized.length()-raw.length())*.35;
                whole.add(new Choice(w,edits,score));
            }
            whole.sort(Comparator.comparingDouble((Choice x)->-x.score));
        }
        ArrayList<String> proposed=new ArrayList<>();
        if(raw.length()<=14)for(Choice choice:whole){
            String suggestion=casing(typed,choice.word.display);
            if(!proposed.contains(suggestion))proposed.add(suggestion);
            if(proposed.size()>=3)break;
        }
        if(raw.length()>=4 && raw.length()<=65) {
            int n=raw.length();
            @SuppressWarnings("unchecked")
            List<Path>[] beams=new List[n+1];
            for(int i=0;i<=n;i++)beams[i]=new ArrayList<>();
            beams[0].add(new Path("","",0,0,0));
            Map<String,List<Choice>> cache=new HashMap<>();
            for(int i=0;i<n;i++){
                if(Thread.currentThread().isInterrupted())
                    return Collections.emptyList();
                if(beams[i].isEmpty())continue;
                for(int j=i+1;j<=Math.min(n,i+14);j++){
                    String fragment=raw.substring(i,j);
                    List<Choice> possible=cache.get(fragment);
                    if(possible==null) {
                        possible=matches(fragment,true);
                        cache.put(fragment,possible);
                    }
                    for(Path prev:beams[i])for(Choice choice:possible){
                        int edits=prev.errors+choice.edits;
                        if(edits>Math.max(2,n/8))continue;
                        boolean supported=NEXT.getOrDefault(prev.last,
                                Collections.emptySet()).contains(
                                        choice.word.normalized);
                        double pair=supported?4.0:prev.words==0?0:-1.8;
                        double score=prev.score+choice.score+pair;
                        String text=prev.text.isEmpty()?choice.word.display:
                                prev.text+" "+choice.word.display;
                        insert(beams[j],new Path(text,choice.word.normalized,
                                score,prev.words+1,edits));
                    }
                }
            }
            List<Path> finals=beams[n];
            // The beam is already descending by score. Prepend its entries
            // in reverse order to keep the strongest path at index zero.
            for(int idx=finals.size()-1;idx>=0;idx--) {
                Path path=finals.get(idx);
                if(path.words<2||path.words>10)continue;
                String candidate=casing(typed,path.text);
                if(!proposed.contains(candidate))proposed.add(0,candidate);
                if(proposed.size()>8)proposed.remove(proposed.size()-1);
            }
        }
        // Keep at most eight candidate alternatives for an explicit tap.
        // User text remains unchanged until the existing IME commit policy
        // accepts a selected result.
        if(proposed.size()>8)return new ArrayList<>(proposed.subList(0,8));
        return proposed;
    }
}
