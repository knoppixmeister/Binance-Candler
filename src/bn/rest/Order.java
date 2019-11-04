package bn.rest;

import com.squareup.moshi.Moshi;

/*
{
	"symbol":				"BTCPAX",
	"orderId":				959,
	"orderListId":			-1,
	"clientOrderId":		"web_ec",
	"price":				"0.00000000",
	"origQty":				"0.00121000",
	"executedQty":			"0.00121000",
	"cummulativeQuoteQty":	"11.14851685",
	"status":				"FILLED",
	"timeInForce":			"GTC",
	"type":					"MARKET",
	"side":					"SELL",
	"stopPrice":			"0.00000000",
	"icebergQty":			"0.00000000",
	"time":					157,
	"updateTime":			157,
	"isWorking":			true
}
*/
public class Order {
	public String	symbol, clientOrderId,
					price, origQty, executedQty,
					cummulativeQuoteQty, status,
					timeInForce, type,
					side, stopPrice, icebergQty;
	public long orderId, time, updateTime;
	public boolean isWorking;

	public String toString() {
		return new Moshi.Builder().build().adapter(Order.class).toJson(this);
	}
}
