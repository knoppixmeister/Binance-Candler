package bn.rest;

import com.squareup.moshi.*;

/*
{
	"price":			"4000.00000000",
	"qty":				"1.00000000",
	"commission":		"4.00000000",
	"commissionAsset":	"USDT"
}
*/
public class Fill {
	public String price, commision, commisionAsset;

	@Json(name = "qty")
	public String quantity;

	public String toString() {
		return new Moshi.Builder().build().adapter(Fill.class).toJson(this);
	}
}
