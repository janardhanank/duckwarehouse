package com.example.ducks.store;

import com.example.ducks.api.ApiException;
import org.springframework.http.HttpStatus;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CountryPolicy {
    private CountryPolicy() {}
    private static final Set<String> CODES = Set.of(("AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ " +
        "BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ " +
        "CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ " +
        "DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR " +
        "GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY " +
        "HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP " +
        "KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY " +
        "MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ " +
        "NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY " +
        "QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ " +
        "TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ " +
        "VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW").split(" "));
    private static final Map<String, String> ALIASES = Map.of(
        "USA", "US", "UNITED STATES", "US", "UNITED STATES OF AMERICA", "US",
        "BOLIVIA", "BO", "INDIA", "IN"
    );
    public static String normalize(String value) {
        String input = value.strip().toUpperCase(Locale.ROOT);
        String code = ALIASES.getOrDefault(input, input);
        if (!CODES.contains(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUNTRY",
                "Use an ISO 3166-1 alpha-2 country code, or USA, Bolivia, or India");
        }
        return code;
    }
    public static int percentage(String countryCode) {
        return switch (countryCode) { case "US" -> 18; case "BO" -> 13; case "IN" -> 19; default -> 15; };
    }
}
