package network.spiritscorp.ai

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Standard food entries for offline carb estimation and nutritional reference.
 * Prepared for i18n via resource keys/enum structure.
 */
enum class StandardFoodDatabase(
    val keyName: String,
    val germanName: String,
    val standardPortionText: String,
    val standardPortionGramsOrMl: Double,
    val carbsPer100g: Double,
    val carbsPerPortion: Double,
    val caloriesPerPortion: Int,
    val glycemicIndexNote: String,
    val keywords: List<String>
) {
    // --- Backwaren & Getreideprodukte ---
    BROT_MISCHBROT(
        "bread_mixed", "Mischbrot / Graubrot", "1 Scheibe (ca. 50g)", 50.0, 48.0, 24.0, 110,
        "Moderater BZ-Anstieg", listOf("mischbrot", "graubrot", "brot", "scheibe brot", "brotzeit")
    ),
    BROT_VOLLKORN(
        "bread_wholegrain", "Vollkornbrot", "1 Scheibe (ca. 50g)", 50.0, 38.0, 19.0, 100,
        "Ballaststoffreich, verzögerter BZ-Anstieg", listOf("vollkornbrot", "vollkorn", "pumpernickel", "schwarzbrot")
    ),
    BROETCHEN_WEISS(
        "roll_white", "Weizenbrötchen / Semmel", "1 Stück (ca. 60g)", 60.0, 52.0, 31.0, 150,
        "Schneller BZ-Anstieg durch Auszugsmehl", listOf("brötchen", "semmel", "weizenbrötchen", "schrippe", "rundstück")
    ),
    TOASTBROT(
        "toast_bread", "Toastbrot / Sandwichbrot", "1 Scheibe (ca. 30g)", 30.0, 49.0, 15.0, 78,
        "Schnell wirkende Kohlenhydrate", listOf("toast", "toastbrot", "sandwich", "sandwichtoast")
    ),
    CROISSANT(
        "croissant", "Buttercroissant / Hörnchen", "1 Stück (ca. 65g)", 65.0, 45.0, 29.0, 260,
        "Sehr hoher Fettanteil verzögert den BZ-Anstieg", listOf("croissant", "hörnchen", "buttercroissant", "plunder")
    ),
    HAFERFLOCKEN(
        "oatmeal", "Haferflocken / Porridge", "1 Portion roh (ca. 50g)", 50.0, 58.0, 29.0, 185,
        "Beta-Glucane sorgen für einen sehr stabilen BZ-Verlauf", listOf("haferflocken", "porridge", "oatmeal", "haferbrei", "müsli")
    ),

    // --- Beilagen & Grundnahrungsmittel ---
    KARTOFFELN_GEKOCHT(
        "potatoes_boiled", "Salzkartoffeln / Pellkartoffeln", "1 Portion (ca. 200g)", 200.0, 15.0, 30.0, 140,
        "Guter Sättigungseffekt, moderater GI", listOf("kartoffel", "kartoffeln", "salzkartoffeln", "pellkartoffeln")
    ),
    KARTOFFELPÜREE(
        "mashed_potatoes", "Kartoffelpüree / Kartoffelbrei", "1 Portion (ca. 200g)", 200.0, 14.0, 28.0, 170,
        "Pürierte Stärke steigt schneller an", listOf("kartoffelpüree", "kartoffelbrei", "stampfkartoffeln", "püree")
    ),
    POMMES_FRITES(
        "french_fries", "Pommes Frites", "1 Portion (ca. 150g)", 150.0, 32.0, 48.0, 430,
        "Fett verzögert den Glukoseanstieg deutlich (FPE-Effekt)", listOf("pommes", "pommes frites", "fritten", "french fries")
    ),
    NUDELN_GEKOCHT(
        "pasta_cooked", "Nudeln / Spaghetti / Pasta (gekocht)", "1 Teller (ca. 200g)", 200.0, 28.0, 56.0, 290,
        "Al dente gekocht langsamer als weichgekocht", listOf("nudeln", "pasta", "spaghetti", "penne", "fussili", "makkaroni", "tagliatelle")
    ),
    REIS_GEKOCHT(
        "rice_cooked", "Reis (weiß, gekocht)", "1 Portion (ca. 180g)", 180.0, 28.0, 50.0, 235,
        "Hoher glykämischer Index", listOf("reis", "basmatireis", "jasminreis", "langkornreis")
    ),
    REIS_VOLLKORN(
        "rice_wholegrain", "Naturreis / Vollkornreis (gekocht)", "1 Portion (ca. 180g)", 180.0, 23.0, 41.0, 210,
        "Langsamere Resorption durch Schale & Ballaststoffe", listOf("naturreis", "vollkornreis", "brauner reis")
    ),

    // --- Beliebte Gerichte & Traditionelle Küche ---
    BRATEN_FLEISCH(
        "roast_meat", "Braten / Fleischgericht (z.B. Reh-, Rind- oder Schweinebraten)", "1 Portion Fleisch mit Soße (ca. 200g)", 200.0, 3.0, 6.0, 320,
        "Reines Fleisch hat 0g KH, Soße enthält oft 4-8g KH aus Bindung & Zwiebeln. Fett & Eiweiß verzögern Resorption.",
        listOf("rehbraten", "braten", "rinderbraten", "schweinebraten", "sauerbraten", "gulasch", "wildbraten", "hirschbraten")
    ),
    PREISELBEEREN_KOMPOTT(
        "lingonberries_compote", "Preiselbeeren (Kompott / gezuckert)", "2 EL (ca. 40g)", 40.0, 38.0, 15.0, 65,
        "Gezuckertes Fruchtkompott enthält viel schnellen Frucht- & Haushaltszucker",
        listOf("preiselbeere", "preiselbeeren", "preiselbeerkompott", "cranberry", "preiselbeermarmelade")
    ),
    KNOEDEL_KLOESSE(
        "dumpling_knoedel", "Semmelknödel / Kartoffelklöße", "1 großer Kloß (ca. 100g)", 100.0, 28.0, 28.0, 145,
        "Klassische Beilage mit 2.5 bis 3 KE pro Kloß",
        listOf("kloß", "klöße", "knödel", "semmelknödel", "kartoffelkloß", "kartoffelknödel")
    ),
    SPAETZLE(
        "spaetzle", "Spätzle / Eierteigwaren (gekocht)", "1 Portion (ca. 180g)", 180.0, 26.0, 47.0, 260,
        "Stärkehaltige Beilage, ca. 4.5 bis 5 KE",
        listOf("spätzle", "spaetzle", "knöpfle", "eierteigwaren")
    ),
    PIZZA_MARGHERITA(
        "pizza_margherita", "Pizza Margherita", "1 ganze Pizza (ca. 350g)", 350.0, 25.0, 88.0, 780,
        "Klassische FPE-Mahlzeit: Mehrstufige Insulinabgabe empfohlen", listOf("pizza", "pizza margherita", "steinofenpizza")
    ),
    PIZZA_SALAMI(
        "pizza_salami", "Pizza Salami / Spezial", "1 ganze Pizza (ca. 380g)", 380.0, 24.0, 91.0, 920,
        "Sehr hoher Fettgehalt, BZ steigt oft noch nach 4-6h an", listOf("pizza salami", "salamipizza", "pizza speck")
    ),
    DOENER_KEBAB(
        "doner_kebab", "Döner Kebab im Fladenbrot", "1 Portion (ca. 350g)", 350.0, 19.0, 66.0, 680,
        "Fladenbrot liefert Haupt-KH; Fleisch/Soße bringen Fett", listOf("döner", "doener", "kebab", "döner kebab", "doner")
    ),
    DOENER_DURUM(
        "doner_durum", "Dürüm Döner / Yufka", "1 Rolle (ca. 380g)", 380.0, 21.0, 80.0, 720,
        "Dünner Fladenteig ist sehr dicht und kohlenhydratreich", listOf("dürüm", "duerum", "yufka", "dönerrolle")
    ),
    BURGER_CLASSIC(
        "burger_classic", "Hamburger / Cheeseburger", "1 Stück (ca. 160g)", 160.0, 22.0, 35.0, 380,
        "Brötchen & Soße enthalten schnelle Zucker", listOf("burger", "hamburger", "cheeseburger")
    ),
    CURRYWURST_POMMES(
        "currywurst_fries", "Currywurst mit Pommes", "1 Portion (ca. 350g)", 350.0, 18.0, 63.0, 750,
        "Currysauce enthält ca. 15g Zucker + Pommes-Stärke", listOf("currywurst", "currywurst pommes", "curry sausage")
    ),
    LASAGNE(
        "lasagna", "Lasagne Bolognese", "1 Portion (ca. 350g)", 350.0, 14.0, 49.0, 540,
        "Kombination aus Nudelplatten, Béchamel & Hackfleisch", listOf("lasagne", "lasagna")
    ),
    SUSHI_SET(
        "sushi_set", "Sushi Set (Nigiri / Maki)", "8 Stück (ca. 240g)", 240.0, 24.0, 58.0, 360,
        "Sushireis ist mit Zucker & Essig gesäuert (schneller BZ-Anstieg)", listOf("sushi", "maki", "nigiri", "sushi rollen")
    ),
    SCHNITZEL_PANIERT(
        "schnitzel_breaded", "Paniertes Schnitzel (ohne Beilage)", "1 Stück (ca. 180g)", 180.0, 10.0, 18.0, 390,
        "Panade liefert ca. 1.5 bis 2 KE", listOf("schnitzel", "paniertes schnitzel", "wiener schnitzel")
    ),
    FALAFEL_SANDWICH(
        "falafel_sandwich", "Falafel-Tasche / Sandwich", "1 Portion (ca. 300g)", 300.0, 22.0, 66.0, 520,
        "Kichererbsen & Fladenbrot, moderater Anstieg", listOf("falafel", "falafeltasche", "falafelsandwich")
    ),

    // --- Obst & Früchte ---
    APFEL(
        "apple", "Apfel (mittelgroß)", "1 Stück (ca. 150g)", 150.0, 12.0, 18.0, 75,
        "Fruchtzucker mit Pektin sorgt für stetigen Anstieg", listOf("apfel", "äpfel", "apple")
    ),
    BANANE(
        "banana", "Banane (reif)", "1 Stück (ca. 120g ohne Schale)", 120.0, 20.0, 24.0, 105,
        "Reife Bananen treiben den BZ sehr schnell an", listOf("banane", "bananen", "banana")
    ),
    ORANGE(
        "orange", "Orange / Apfelsine", "1 Stück (ca. 150g)", 150.0, 9.0, 14.0, 65,
        "Ganzes Obst deutlich langsamer als Orangensaft", listOf("orange", "apfelsine", "mandarine", "clementine")
    ),
    ERDBEEREN(
        "strawberries", "Erdbeeren / Beeren", "1 Schale (ca. 200g)", 200.0, 6.0, 12.0, 64,
        "Sehr wenige KH, ideal für Diabetiker", listOf("erdbeeren", "erdbeere", "himbeeren", "blaubeeren", "heidelbeeren", "beeren")
    ),
    TRAUBEN(
        "grapes", "Weintrauben", "1 Portion (ca. 150g)", 150.0, 16.0, 24.0, 105,
        "Reiner Traubenzucker / Glukose, extrem schneller BZ-Anstieg!", listOf("trauben", "weintrauben", "reben")
    ),
    WASSERMELONE(
        "watermelon", "Wassermelone", "1 Scheibe (ca. 250g)", 250.0, 7.5, 19.0, 95,
        "Hoher Wassergehalt, aber hoher glykämischer Index", listOf("wassermelone", "melone")
    ),

    // --- Getränke & Erfrischungen ---
    COLA_REGULAR(
        "cola_regular", "Cola / Limo (Zuckerhaltig)", "1 Glas (250ml)", 250.0, 10.8, 27.0, 105,
        "Flüssiger Zucker: Schießt sofort ins Blut (Hypo-Helfer)!", listOf("cola", "limo", "fanta", "sprite", "spezi", "softdrink", "coke")
    ),
    APFELSAFTSCHORLE(
        "apple_spritzer", "Apfelsaftschorle (1:1)", "1 Glas (300ml)", 300.0, 5.5, 16.5, 75,
        "Flüssige Fruchtzucker-Glukose-Mischung", listOf("schorle", "apfelsaftschorle", "apfelschorle")
    ),
    ORANGENSAFT(
        "orange_juice", "Orangensaft (100% Fruchtgehalt)", "1 Glas (200ml)", 200.0, 9.5, 19.0, 90,
        "Keine Ballaststoffe, schneller Blutzuckeranstieg", listOf("o-saft", "orangensaft", "apfelsaft", "fruchtsaft")
    ),
    BIER_PILS(
        "beer_pils", "Bier / Pils (mit Alkohol)", "1 Glas (330ml)", 330.0, 3.1, 10.2, 140,
        "Alkohol hemmt die nächtliche Glukoseproduktion der Leber!", listOf("bier", "pils", "export", "helles")
    ),
    MILCH_VOLL(
        "milk_whole", "Vollmilch (3.5% Fett)", "1 Glas (200ml)", 200.0, 4.8, 9.6, 130,
        "Laktose führt zu langsamem, stetigem Anstieg", listOf("milch", "vollmilch", "fettarme milch")
    ),

    // --- Snacks & Süßigkeiten ---
    SCHOKOLADE_MILCH(
        "chocolate_milk", "Vollmilchschokolade", "1/2 Tafel (ca. 50g)", 50.0, 56.0, 28.0, 265,
        "Fett verzögert den Zuckerabbau spürbar", listOf("schokolade", "vollmilchschokolade", "schoki")
    ),
    GUMMIBÄRCHEN(
        "gummy_bears", "Gummibärchen / Weingummi", "1 Handvoll (ca. 40g)", 40.0, 77.0, 31.0, 140,
        "Fast fettfrei, fast reine schnelle Kohlenhydrate", listOf("gummibärchen", "haribo", "fruchtgummi", "weingummi")
    ),
    KEKSE_BUTTER(
        "cookies_butter", "Butterkekse / Cookies", "4 Stück (ca. 40g)", 40.0, 72.0, 29.0, 180,
        "Mehl & Zucker sorgen für raschen Anstieg", listOf("keks", "kekse", "butterkeks", "cookie", "plätzchen")
    ),
    EISCREME_KUGEL(
        "ice_cream_scoop", "Kugeleis / Milcheis", "2 Kugeln (ca. 100g)", 100.0, 24.0, 24.0, 200,
        "Kombination aus Zucker und Milchfett", listOf("eis", "speiseeis", "eiscreme", "vanilleeis", "schokoeis")
    ),
    CHIPS_KARTOFFEL(
        "potato_chips", "Kartoffelchips", "1 kleine Schale (ca. 40g)", 40.0, 50.0, 20.0, 215,
        "Sehr fettreich, verlangsamte Glukoseaufnahme", listOf("chips", "kartoffelchips", "stapelchips")
    ),

    // --- Milchprodukte & Frühstück ---
    JOGHURT_NATUR(
        "yogurt_natural", "Naturjoghurt (3.5% Fett)", "1 Becher (ca. 150g)", 150.0, 4.5, 6.8, 100,
        "Sehr geringer BZ-Einfluss", listOf("joghurt", "naturjoghurt", "quark", "magerquark")
    ),
    FRUCHTJOGHURT(
        "yogurt_fruit", "Fruchtjoghurt mit Zucker", "1 Becher (ca. 150g)", 150.0, 14.0, 21.0, 145,
        "Industrieller Fruchtjoghurt enthält viel Kristallzucker", listOf("fruchtjoghurt", "erdbeerjoghurt")
    ),
    KAE_SE_SCHNITT(
        "cheese_slice", "Schnittkäse (Gouda / Emmentaler)", "1 Scheibe (ca. 30g)", 30.0, 0.1, 0.0, 110,
        "Nahezu 0g KH (reines Fett & Eiweiß)", listOf("käse", "gouda", "emmentaler", "butterkäse", "cheddar")
    ),
    EI_GEKOCHT(
        "egg_boiled", "Hühnerei (gekocht / Rührei)", "1 Stück (ca. 60g)", 60.0, 0.5, 0.3, 90,
        "Praktisch keine Kohlenhydrate", listOf("ei", "eier", "rührei", "spiegelei", "gekochtes ei")
    );

    companion object {
        private val STOP_WORDS = setOf(
            "mit", "und", "oder", "auf", "ein", "eine", "einer", "einem", "einen",
            "zwei", "drei", "vier", "fünf", "scheibe", "scheiben", "stück", "portion",
            "teller", "glas", "tasse", "becher", "g", "gr", "gramm", "ml", "l", "liter",
            "ca", "circa", "etwa", "etwas", "große", "großer", "großes", "kleine", "kleiner", "kleines"
        )

        /**
         * Search matching food items by words in the query with strict token matching.
         * Prevents erroneous substring matches like 'reis', 'eis' or 'ei' inside composite words.
         */
        fun findMatches(query: String): List<StandardFoodDatabase> {
            val lowerQuery = query.lowercase().trim()
            val tokens = lowerQuery
                .split(Regex("[\\s,;+&/()]+"))
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.length >= 2 && !STOP_WORDS.contains(it) }

            if (tokens.isEmpty()) return emptyList()

            val matchedEntries = mutableSetOf<StandardFoodDatabase>()

            for (item in entries) {
                for (keyword in item.keywords) {
                    val kw = keyword.lowercase()
                    if (kw.contains(" ")) {
                        // Multi-word keyword phrase (e.g. "pizza margherita", "currywurst pommes")
                        if (lowerQuery.contains(kw)) {
                            matchedEntries.add(item)
                            break
                        }
                    } else {
                        // Single-word keyword matching
                        val matchesToken = tokens.any { token ->
                            token == kw ||
                            token == kw + "n" ||
                            token == kw + "en" ||
                            token == kw + "s" ||
                            token == kw + "e" ||
                            token.removeSuffix("n") == kw ||
                            token.removeSuffix("en") == kw ||
                            token.removeSuffix("e") == kw ||
                            token.removeSuffix("s") == kw ||
                            // Only match compounds if keyword is long (>= 5 chars) and token starts/ends with keyword
                            (kw.length >= 5 && (token.startsWith(kw) || token.endsWith(kw)))
                        }
                        if (matchesToken) {
                            matchedEntries.add(item)
                            break
                        }
                    }
                }
            }

            return matchedEntries.toList()
        }
    }
}
