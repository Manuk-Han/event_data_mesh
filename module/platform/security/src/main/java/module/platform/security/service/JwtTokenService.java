package module.platform.security.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import module.platform.security.config.SecurityProps;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtTokenService {
    private final SecurityProps props;
    public JwtTokenService(SecurityProps props) { this.props = props; }

    public String issue(String subject, List<String> roles) throws Exception {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(props.issuer())
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(props.ttlSeconds())))
                .claim("roles", roles)
                .build();
        var header = new JWSHeader(JWSAlgorithm.HS256);
        var signed = new SignedJWT(header, claims);
        signed.sign(new MACSigner(props.hmacSecret().getBytes()));
        return signed.serialize();
    }

    public JWTClaimsSet verify(String token) throws Exception {
        var jwt = SignedJWT.parse(token);
        var ok = jwt.verify(new MACVerifier(props.hmacSecret().getBytes()));
        if (!ok) throw new JOSEException("bad signature");
        return jwt.getJWTClaimsSet();
    }
}

