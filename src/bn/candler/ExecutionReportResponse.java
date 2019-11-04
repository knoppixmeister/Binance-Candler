package bn.candler;

import com.squareup.moshi.*;

/*
	"e":"executionReport",
	"E":15724,
	"s":"IOTABTC",
	"c":"web_",
	"S":"SELL",
	"o":"LIMIT",
	"f":"GTC",
	"q":"15.00000000",
	"p":"0.00003200",
	"P":"0.00000000",
	"F":"0.00000000",
	"g":-1,
	"C":"web_",
	"x":"CANCELED",
	"X":"CANCELED",
	"r":"NONE",
	"i":126541099,
	"l":"0.00000000",
	"z":"0.00000000",
	"L":"0.00000000",
	"n":"0",
	"N":null,
	"T":15,
	"t":-1,
	"I":2705,
	"w":false,
	"m":false,
	"M":false,
	"O":157,
	"Z":"0.00000000",
	"Y":"0.00000000"
*/
public class ExecutionReportResponse {
	@Json(name = "E")
	public long eventTime;

	@Json(name = "i")
	public long orderId;

	@Json(name = "O")
	public long orderCreateTime;

	@Json(name = "s")
	public String symbol;

	@Json(name = "o")
	public String orderType;

	@Json(name = "c")
	public String clientOrderId;

	@Json(name = "S")
	public String side;

	@Json(name = "f")
	public String timeInForce;

	@Json(name = "q")
	public String amount;

	@Json(name = "p")
	public String price;

	@Json(name = "P")
	public String stopPrice;

	@Json(name = "x")
	public String status;

	public String toString() {
		return new Moshi.Builder().build().adapter(ExecutionReportResponse.class).toJson(this);
	}
}
