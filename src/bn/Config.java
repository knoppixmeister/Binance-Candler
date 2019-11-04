package bn;

import java.util.*;

public class Config {
	@SuppressWarnings("serial")
	public static final HashMap<String, Integer> TFs = new HashMap<String, Integer>() {{
		put("1m", 60);
		put("5m", 300);
		put("15m", 900);
		put("30m", 1800);
		put("1h", 3600);
		put("2h", 7200);
		put("4h", 14400);
		put("6h", 21600);
		put("12h", 43200);
		put("1d", 86400);
	}};
}
