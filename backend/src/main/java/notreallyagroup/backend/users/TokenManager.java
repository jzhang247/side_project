package notreallyagroup.backend.users;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import notreallyagroup.backend.Constants;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenManager {
    private static final String ENCRYPT_KEY = "jfioajefioafneuaih111dwa";


    public static final String NAME_EXPIRATION_DATE = "expr";


    public static String makeToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .claim(NAME_EXPIRATION_DATE, System.currentTimeMillis() + Constants.TOKEN_LIFE_SPAN_IN_MILLISECONDS)
                .signWith(SignatureAlgorithm.HS256, ENCRYPT_KEY).compact();
    }

    public static Map<String, Object> validateToken(String encrypted) {
        var claims = Jwts.parser().setSigningKey(ENCRYPT_KEY).parseClaimsJws(encrypted).getBody();
        long now = System.currentTimeMillis();
        long exp = claims.get(NAME_EXPIRATION_DATE, Long.class);
        if (now > exp) {
            return null;
        }
        return claims;
    }


}
