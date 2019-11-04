package bn.rest;

import java.util.List;
import java.util.concurrent.*;
import java.util.*;
import com.squareup.moshi.*;
import bn.utils.*;
import okhttp3.*;

public class BNRest {
	public static enum ORDER_TYPE {
		LIMIT,
		MARKET
	}
	public static enum ORDER_SIDE {
		BUY,
		SELL
	}

	public static final String BASE_REST_API_URL = "https://api.binance.com";

	private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

	private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().retryOnConnectionFailure(true)
																				.connectTimeout(10, TimeUnit.SECONDS)
																				.build();

	private String apiKey, apiSecret;

	public BNRest() {
	}

	public BNRest(String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	@SuppressWarnings("deprecation")
	public String createListenKey() {
		// https://api.binance.com
		// POST /api/v3/userDataStream

		if(apiKey == null || apiKey.isEmpty()) return null;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/userDataStream")
													.header("X-MBX-APIKEY", this.apiKey)
													.post(RequestBody.create(MEDIA_TYPE_JSON, ""))
													.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					String json = response.body().string();

					if(json == null || json.isEmpty() || !json.contains("\"listenKey\"")) return null;

					// System.out.println(json);

					json = json.replaceAll(" ", "").substring(json.indexOf(":")+2, json.lastIndexOf("\""));

					return json;
				}
				else System.out.println(response.body().string());

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	public boolean updateListenKey(final String listenKey) {
		if(listenKey == null || listenKey.isEmpty() || apiKey == null || apiKey.isEmpty()) return false;

		// https://api.binance.com
		// PUT /api/v3/userDataStream
		// istenKey	STRING	YES

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/userDataStream")
													.header("X-MBX-APIKEY", this.apiKey)
													.put(RequestBody.create(MEDIA_TYPE_JSON, "listenKey="+listenKey))
													.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty()) return false;

					response.close();

					return json.trim().equals("{}");
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public List<Symbol> getSymbols() {
		// GET /api/v3/exchangeInfo

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/exchangeInfo").build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty()) return null;

					System.out.println(json.replaceAll(",", ",\r\n").replaceAll("\\{", "{\r\n"));

					ExchangeInfo ei = new Moshi.Builder().build().adapter(ExchangeInfo.class).fromJson(json);

					response.close();

					return ei != null ? ei.symbols : null;
				}
				else System.out.println(response.body().string());

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public PlaceOrderFullResponse placeLimitOrder(final ORDER_SIDE side, final String symbol, double price, final double quantity) {
		return placeOrder(ORDER_TYPE.LIMIT, side, symbol, price, quantity, null);
	}

	public PlaceOrderFullResponse placeLimitOrder(final ORDER_SIDE side, final String symbol, double price, final double quantity, String clientOrderId) {
		return placeOrder(ORDER_TYPE.LIMIT, side, symbol, price, quantity, clientOrderId);
	}

	public PlaceOrderFullResponse placeMarketOrder(final ORDER_SIDE side, final String symbol, final double quantity) {
		return placeMarketOrder(side, symbol, quantity, "");
	}

	public PlaceOrderFullResponse placeMarketOrder(final ORDER_SIDE side, final String symbol, final double quantity, String clientOrderId) {
		return placeOrder(ORDER_TYPE.MARKET, side, symbol, 0, quantity, clientOrderId);
	}

	public PlaceOrderFullResponse placeOrder(final ORDER_TYPE type, final ORDER_SIDE side, final String symbol, final double price, final double quantity) {
		return placeOrder(type, side, symbol, price, quantity, "");
	}

	@SuppressWarnings("deprecation")
	public PlaceOrderFullResponse placeOrder(final ORDER_TYPE type, final ORDER_SIDE side, String symbol, final double price, final double quantity, String clientOrderId) {
		// POST /api/v3/order/test (HMAC SHA256)

		if(symbol == null) return null;

		symbol = symbol.replaceAll("-", "").replaceAll("_", "").toUpperCase();

		if(clientOrderId == null || clientOrderId.isEmpty()) clientOrderId = UUID.randomUUID().toString();

		/*
			LIMIT
			MARKET
			STOP_LOSS
			STOP_LOSS_LIMIT
			TAKE_PROFIT
			TAKE_PROFIT_LIMIT
			LIMIT_MAKER
		 */

		/*
			BUY
			SELL
		 */

		final String params =	"symbol="+symbol+
								"&side="+side+
								"&type="+type+
								(type == ORDER_TYPE.LIMIT ? "&price="+String.format(Locale.US, "%.8f", price) : "")+
								(type == ORDER_TYPE.LIMIT ? "&timeInForce=GTC" : "")+
								"&quantity="+String.format(Locale.US, "%.8f", quantity)+
								"&newClientOrderId="+clientOrderId+
								"&timestamp="+System.currentTimeMillis();

		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return null;
		}
		if(sign == null || sign.isEmpty()) return null;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/order")
														.header("X-MBX-APIKEY", this.apiKey)
														.post(RequestBody.create(MEDIA_TYPE_JSON, params+"&signature="+sign))
														.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return null;

					// System.out.println(json);

					return new Moshi.Builder().build().adapter(PlaceOrderFullResponse.class).fromJson(json);
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean cancelOrder(String symbol, Long orderId) {
		return cancelOrder(symbol, orderId, null);
	}

	public boolean cancelOrder(String symbol, String clientOrderId) {
		return cancelOrder(symbol, null, clientOrderId);
	}

	@SuppressWarnings("deprecation")
	public boolean cancelOrder(String symbol, Long orderId, String clientOrderId) {
		// DELETE /api/v3/order (HMAC SHA256)

		if(symbol == null || symbol.isEmpty()) return false;

		symbol = symbol.replaceAll("-", "").toUpperCase();

		if(orderId == null && (clientOrderId == null || clientOrderId.isEmpty())) return false;

		final String params =	"symbol="+symbol+
								(orderId != null ? "&orderId="+orderId : "")+
								(clientOrderId != null && !clientOrderId.isEmpty() ? "&origClientOrderId="+clientOrderId : "")+
								"&timestamp="+System.currentTimeMillis();

		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return false;
		}
		if(sign == null || sign.isEmpty()) return false;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/order")
													.header("X-MBX-APIKEY", this.apiKey)
													.delete(RequestBody.create(MEDIA_TYPE_JSON, params+"&signature="+sign))
													.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return false;

					System.out.println(json);
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public void getOrder(String symbol, Long orderId) {
		getOrder(symbol, orderId, null);
	}

	public void getOrder(String symbol, String clientOrderId) {
		getOrder(symbol, null, clientOrderId);
	}

	public void getOrder(String symbol, Long orderId, String clientOrderId) {
		// GET /api/v3/order (HMAC SHA256)

		if(symbol == null || symbol.isEmpty()) return;

		symbol = symbol.replaceAll("-", "").replaceAll("_", "").toUpperCase();

		if(orderId == null && (clientOrderId == null || clientOrderId.isEmpty())) return;

		final String params =	"symbol="+symbol+
								(orderId != null ? "&orderId="+orderId : "")+
								(clientOrderId != null && !clientOrderId.isEmpty() ? "&origClientOrderId="+clientOrderId : "")+
								"&timestamp="+System.currentTimeMillis();

		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return;
		}
		if(sign == null || sign.isEmpty()) return;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/order?"+params+"&signature="+sign)
														.header("X-MBX-APIKEY", this.apiKey)
														.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return;

					System.out.println(json);
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return;
	}

	public void getOpenOrders() {
		getOpenOrders(null);
	}

	public void getOpenOrders(String symbol) {
		// GET /api/v3/openOrders (HMAC SHA256)

		if(symbol != null && !symbol.isEmpty()) {
			symbol = symbol.replaceAll("-", "").replaceAll("_", "").toUpperCase();
		}

		final String params =	"timestamp="+System.currentTimeMillis()+
								(symbol != null && !symbol.isEmpty() ? "&symbol="+symbol : "");

		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return;
		}
		if(sign == null || sign.isEmpty()) return;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/openOrders?"+params+"&signature="+sign)
														.header("X-MBX-APIKEY", this.apiKey)
														.build();

		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return;

					System.out.println(json);
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return;
	}

	public List<Order> getAllOrders(String symbol) {
		return getAllOrders(symbol, null);
	}

	@SuppressWarnings("unchecked")
	public List<Order> getAllOrders(String symbol, final Long orderId) {
		if(symbol == null || symbol.isEmpty() || this.apiKey == null || this.apiKey.isEmpty()) return null;

		symbol = symbol.replaceAll("-", "").replaceAll("_", "").toUpperCase();

		final String params =	"symbol="+symbol+
								(orderId != null ? "&orderId="+orderId : "")+
								"&limit=1000"+
								"&timestamp="+System.currentTimeMillis();	
		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return null;
		}
		if(sign == null || sign.isEmpty()) return null;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/allOrders?"+params+"&signature="+sign)
													.header("X-MBX-APIKEY", this.apiKey)
													.build();

		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return null;

					// System.out.println(json+"\r\n");

					return (List<Order>) new Moshi.Builder().build().adapter(Types.newParameterizedType(List.class, Order.class)).fromJson(json);
				}
				else {
					System.out.println(response.code()+":"+response.message());
					System.out.println("ERROR_RESPONSE: "+response.body().string());
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public List<bn.rest.Trade> getUserTrades(String symbol) {
		// GET /api/v3/myTrades (HMAC SHA256)

		if(symbol == null || symbol.isEmpty() || this.apiKey == null || this.apiKey.isEmpty()) return null;

		symbol = symbol.replaceAll("-", "").replaceAll("_", "").toUpperCase();

		final String params =	"symbol="+symbol+
								"&limit=1000"+
								"&timestamp="+System.currentTimeMillis();

		String sign = "";
		try {
			sign = Utils.encodeHMACSHA256(apiSecret, params);
		}
		catch(Exception e) {
			e.printStackTrace();

			return null;
		}
		if(sign == null || sign.isEmpty()) return null;

		final Request request = new Request.Builder().url(BASE_REST_API_URL+"/api/v3/myTrades?"+params+"&signature="+sign)
														.header("X-MBX-APIKEY", this.apiKey)
														.build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response != null) {
				if(response.isSuccessful()) {
					final String json = response.body().string();

					if(json == null || json.isEmpty() || json.contains("code")) return null;

					// System.out.println(json);

					return (List<Trade>) new Moshi.Builder().build().adapter(Types.newParameterizedType(List.class, bn.rest.Trade.class)).fromJson(json);
				}

				response.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
