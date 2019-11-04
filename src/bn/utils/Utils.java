package bn.utils;

import java.math.*;
import java.nio.charset.*;
import java.util.UUID;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.apache.commons.codec.binary.*;

public class Utils {
	public static String encodeHMACSHA256(final String key, final String data) throws Exception {
		final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

		return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
	}

	public static double round(double value, int places) {
		if(places < 0) throw new IllegalArgumentException();

		return (new BigDecimal(value)).setScale(places, RoundingMode.HALF_UP).doubleValue();
	}

	public static boolean isUUID(String uuid) {
		if(uuid == null || uuid.isEmpty()) return false;

		try {
			UUID.fromString(uuid);
	        return true;
		}
		catch(Exception e) {			
		}

		return false;
	}
}
