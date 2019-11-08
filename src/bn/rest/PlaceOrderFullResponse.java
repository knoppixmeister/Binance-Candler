package bn.rest;

import java.util.List;

/*
{
	"symbol":				"BTCUSDT",
	"orderId":				28,
	"orderListId":			-1,							//Unless OCO, value will be -1
	"clientOrderId":		"6gC....",
	"transactTime":			150....,
	"price":				"1.00000000",
	"origQty":				"10.00000000",
	"executedQty":			"10.00000000",
	"cummulativeQuoteQty":	"10.00000000",
	"status":				"FILLED",
	"timeInForce":			"GTC",
	"type":					"MARKET",
	"side":					"SELL",
	"fills": [
		{
			"price":			"4000.00000000",
			"qty":				"1.00000000",
			"commission":		"4.00000000",
			"commissionAsset":	"USDT"
		},
	]
}
*/
public class PlaceOrderFullResponse {
	public String symbol, clientOrderId, price, status, timeInForce, type, side, origQty, executedQty;
	public long orderId, orderListId;

	public List<Fill> fills;
}
