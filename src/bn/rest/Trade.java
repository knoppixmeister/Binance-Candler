package bn.rest;

import com.squareup.moshi.Moshi;

/*
{
    "symbol":			"BNBBTC",
    "id":				28...,
    "orderId":			10...,
    "orderListId":		-1, //Unless OCO, the value will always be -1
    "price":			"4.00000100",
    "qty":				"12.00000000",
    "quoteQty":			"48.000012",
    "commission":		"10.10000000",
    "commissionAsset":	"BNB",
    "time":				149...,
    "isBuyer":			true,
    "isMaker":			false,
    "isBestMatch":		true
}
*/
public class Trade {
	public String symbol, price, qty, quoteQty, commission, commissionAsset;
	public long id, orderId, orderListId, time;
	public boolean isBuyer, isMaker, isBestMatch;

	public String toString() {
		return new Moshi.Builder().build().adapter(Trade.class).toJson(this);
	}
}
