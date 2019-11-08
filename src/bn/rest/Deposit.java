package bn.rest;

import com.squareup.moshi.*;

/*
{
	"address":		"18....",
	"addressTag":	"",
	"amount":		"0.001",
	"coin":			"BTC",
	"insertTime":	1572...,
	"network":		"BTC",
	"status":		1,
	"txId":			"511..."
}
*/
public class Deposit {
	public String address, addressTag, amount, coin, network, txId;
	public long insertTime;
	public int status;

	public String toString() {
		return new Moshi.Builder().build().adapter(Deposit.class).toJson(this);
	}
}
