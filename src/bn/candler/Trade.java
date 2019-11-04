package bn.candler;

import com.squareup.moshi.*;

/*
{
	"e": "trade",     // Event type
	"E": 123,   		// Event time
	"s": "BNBBTC",    // Symbol
	"t": 12345,       // Trade ID
	"p": "0.001",     // Price
	"q": "100",       // Quantity
	"b": 88,          // Buyer order ID
	"a": 50,          // Seller order ID
	"T": 123,   		// Trade time
	"m": true,        // Is the buyer the market maker?
	"M": true         // Ignore
}
*/
public class Trade {
	public String e, s, p, q;
	public long E, t, b, a, T;
	public boolean m, M;

	public String toString() {
		return new Moshi.Builder().build().adapter(Trade.class).toJson(this);
	}
}
