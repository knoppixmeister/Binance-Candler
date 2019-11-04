package bn.candler;

import java.math.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import org.jfree.data.time.*;
import org.jfree.data.time.ohlc.*;
import org.joda.time.*;
import com.squareup.moshi.*;
import bn.Config;
import bn.rest.*;
import bn.utils.*;
import okhttp3.*;
import okhttp3.internal.ws.RealWebSocket;

public class BNCandler {
	public static final String BASE_WS_URL = "wss://stream.binance.com:9443";

	private RealWebSocket webSocket = null;
	private RealWebSocket udWebSocket = null;
	public static int OHLC_ITEMS_COUNT = 500;

	private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().retryOnConnectionFailure(true)
																				.pingInterval(10, TimeUnit.SECONDS)
																				.build();

	private final Moshi MOSHI											=	new Moshi.Builder().build();
	public final Map<String, Map<Integer, OHLCSeries>> OHLC_SERIES		=	new ConcurrentHashMap<>();

	private long mdReconnectCnt = -1;
	private long udReconnectCnt = -1;

	private boolean allowMarketDataReconnect = true;
	private boolean allowUserDataStreamReconnect = true;

	private final List<ConnectedListener> connectedListeners	=	new CopyOnWriteArrayList<>();
	private final List<CandleListener> candleListeners			=	new CopyOnWriteArrayList<>();
	private final List<TradeListener> tradeListeners			=	new CopyOnWriteArrayList<>();
	private final List<OrderEventListener> orderEventListeners	=	new CopyOnWriteArrayList<>();
	private final List<OrderBookListener> orderBookListeners	=	new CopyOnWriteArrayList<>();

	private final ConcurrentSkipListMap<Double, Double> BIDS	=	new ConcurrentSkipListMap<Double, Double>();
	private final ConcurrentSkipListMap<Double, Double> ASKS	=	new ConcurrentSkipListMap<Double, Double>();

	private Timer listenKeyUpdater;

	private String apiKey, apiSecret;

	private String listenKey;

	public BNCandler() {
	}

	public BNCandler(String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public boolean add(String pair, final int interval) {
		pair = pair.replaceAll("-", "");

		if(!OHLC_SERIES.containsKey(pair.toUpperCase())) {
			OHLC_SERIES.put(pair.toUpperCase(), new ConcurrentHashMap<>());
		}
		if(!OHLC_SERIES.get(pair.toUpperCase()).containsKey(interval)) {
			OHLC_SERIES.get(pair.toUpperCase()).put(interval, new OHLCSeries(""));
		}

		if(!fetchOHLCs(pair, interval)) {
			System.out.println("COULD NOT RECEIVE INITIAL CANDLES. EXIT !!!");
			System.exit(0);
		}

		return true;
	}

	public boolean add(String pair, String interval) {
		return add(pair, Config.TFs.get(interval));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private boolean fetchOHLCs(final String pair, final int interval) {
		System.out.print("BN_CANDLER. START FETCHING INITIAL CANDLES "+pair+"/"+interval+" .... ");

		OHLC_SERIES.get(pair.toUpperCase()).get(interval).clear();

		final String ohlcsUrl = "https://api.cryptowat.ch/markets/binance/"+pair.replaceAll("-", "").toLowerCase()+"/ohlc?periods="+interval;

		final Request request = new Request.Builder().url(ohlcsUrl).build();
		try {
			final Response response = HTTP_CLIENT.newCall(request).execute();
			if(response.isSuccessful()) {
				String json = response.body().string();

				json = json.substring(json.indexOf("["));
				json = json.substring(0, json.indexOf("]]")+2);

				final List<List<Object>> restKlines = new Moshi.Builder().build().adapter(List.class).fromJson(json);

				DateTime dtEnd, dtStart;
				List<Object> rlRes;

				OHLC_SERIES.get(pair.toUpperCase()).get(interval).setNotify(false);

				for(int key=restKlines.size()-1; key >= 0; key--) {
					rlRes 	= restKlines.get(key);
					dtEnd 	= new DateTime(new BigDecimal(rlRes.get(0)+"").longValue()*1000);
					dtStart = new DateTime((new BigDecimal(rlRes.get(0)+"").longValue()-interval)*1000);

					try {
						OHLC_SERIES.get(pair.toUpperCase()).get(interval).add(
							new FixedMillisecond((new BigDecimal(rlRes.get(0)+"").longValue()-interval)*1000),
							new BigDecimal(rlRes.get(1)+"").doubleValue(),
							new BigDecimal(rlRes.get(2)+"").doubleValue(),
							new BigDecimal(rlRes.get(3)+"").doubleValue(),
							new BigDecimal(rlRes.get(4)+"").doubleValue(),
							new BigDecimal(rlRes.get(5)+"").doubleValue()
						);
					}
					catch(Exception e1) {
						e1.printStackTrace();
					}
				}

				OHLC_SERIES.get(pair.toUpperCase()).get(interval).setNotify(true);

				System.out.println("DONE");

				return true;
			}
			else {
				System.err.println("ERROR");
				System.err.println(response.body().string());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public void addConnectedListener(ConnectedListener connectedListener) {
		connectedListeners.add(connectedListener);
	}

	public void addCandleListener(CandleListener listener) {
		candleListeners.add(listener);
	}

	public void addTradeListener(TradeListener tradeListener) {
		tradeListeners.add(tradeListener);
	}

	public void addOrderEventListener(OrderEventListener orderEventListener) {
		orderEventListeners.add(orderEventListener);
	}

	public void addOrderBookListener(OrderBookListener orderBookListener) {
		orderBookListeners.add(orderBookListener);
	}

	public long getMarketDataReconnectCnt() {
		return mdReconnectCnt;
	}

	public long getUserDataReconnectCnt() {
		return udReconnectCnt;
	}

	public void connectMarketData() {
		Log.i("BN_CANDLER. CONNECTING ... ");

		if(OHLC_SERIES.keySet().isEmpty()) return;

		mdReconnectCnt += 1;

		if(webSocket != null) {
			allowMarketDataReconnect = false;
			webSocket.close(1000, "CLOSE WS BEFORE CONNECT IF ALREADY CREATED CONNECTION");
			webSocket = null;
		}

		if(mdReconnectCnt > 0) {
			boolean allPairCandlesReceived = true;
			while(true) {
				allPairCandlesReceived = true;

				for(String pair : OHLC_SERIES.keySet()) {
					for(Integer interval : OHLC_SERIES.get(pair.toUpperCase()).keySet()) {
						OHLC_SERIES.get(pair.toUpperCase()).get(interval).clear();

						if(!fetchOHLCs(pair, interval)) allPairCandlesReceived = false;
					}
				}

				if(!allPairCandlesReceived) {
					try {
						System.out.println("NOT ALL INITIAL CANDLE PAIRS RECEIVED AFTER RECONNECT ATTMPT. TRY AGAIN AFTER 5 sec.");

						Thread.sleep(5 * 1000);
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				else break;
			}
		}

		allowMarketDataReconnect = true;

		// <symbol>@bookTicker
		// <symbol>@depth<levels>@100ms

		String wsConectUrl = BASE_WS_URL+"/stream?streams=";
		for(String pair : OHLC_SERIES.keySet()	) {
			wsConectUrl += pair.replaceAll("-", "").toLowerCase()+"@trade/";
			wsConectUrl += pair.replaceAll("-", "").toLowerCase()+"@bookTicker/";
			wsConectUrl += pair.replaceAll("-", "").toLowerCase()+"@depth20@100ms/";
		}
		wsConectUrl = wsConectUrl.substring(0, wsConectUrl.lastIndexOf("/"));

		// System.out.println(wsConectUrl);

		webSocket = (RealWebSocket) HTTP_CLIENT.newWebSocket(
			new Request.Builder().url(wsConectUrl).build(),
			new WebSocketListener() {
				public void onOpen(WebSocket socket, Response response) {
					Log.i("BN_CANDLER. CONNECTING DONE");

					try {
						Log.i("BN_WS_ON_OPEN"+(response != null ? ": "+response.body().string() : ""));
					}
					catch(Exception e) {
						e.printStackTrace();
					}

					for(ConnectedListener cl : connectedListeners) {
						cl.onConnectedListener();
					}
				}

				public void onFailure(WebSocket socket, Throwable t, Response response) {
					System.out.println("WS_ON_FAIL");

					t.printStackTrace();

					if(allowMarketDataReconnect) connectMarketData();
				}

				public void onClosed(WebSocket socket, int code, String reason) {
					System.out.println("WS_CLOSED. CODE: "+code+" | REASON: "+reason);

					if(allowMarketDataReconnect) connectMarketData();
				}

				public void onMessage(WebSocket socket, String text) {
					// Log.i("MD_WS_MSG: "+text);

					parseMessage(text);
				}
			}
		);
	}

	public void connectUserDataStream() {
		if(listenKeyUpdater != null) listenKeyUpdater.cancel();

		if(this.apiKey == null || this.apiKey.isEmpty()) return;

		listenKey = new BNRest(this.apiKey, this.apiSecret).createListenKey();

		if(listenKey == null || listenKey.isEmpty()) return;

		udReconnectCnt += 1;

		if(udWebSocket != null) {
			allowUserDataStreamReconnect = false;
			udWebSocket.close(1000, "");
			udWebSocket = null;
		}

		allowUserDataStreamReconnect = true;

		listenKeyUpdater = new Timer();
		listenKeyUpdater.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if(
						apiKey == null ||
						apiKey.isEmpty() ||
						!new BNRest(apiKey, apiSecret).updateListenKey(listenKey)
					)
					{
						allowUserDataStreamReconnect = false;
						udWebSocket.close(1000, "");
						udWebSocket = null;

						listenKey = null;

						Log.i("UD_WS_LISTEN_KEY_____NOT___UPDATED");
					}
					else Log.i("UD_WS_LISTEN_KEY_UPDATED");
				}
			},
			// 30 * 60 * 1000,
			0,
			30 * 60 * 1000
		);

		udWebSocket = (RealWebSocket) HTTP_CLIENT.newWebSocket(
			new Request.Builder().url("wss://stream.binance.com:9443/ws/"+listenKey).build(),
			new WebSocketListener() {
				public void onOpen(WebSocket socket, Response response) {
					Log.i("UD_WS_OPENED");
				}

				public void onClosed(WebSocket socket, int code, String reason) {
					Log.i("UD_WS_CLOSED. CODE: "+code+" | REASON: "+reason);

					if(allowUserDataStreamReconnect) connectUserDataStream();
				}

				public void onFailure(WebSocket socket, Throwable t, Response response) {
					Log.i("UD_WS_FAIL");

					t.printStackTrace();

					if(allowUserDataStreamReconnect) connectUserDataStream();
				}

				public void onMessage(final WebSocket socket, final String message) {
					// Log.i("WS_US_ON_MSG: "+message);

					if(message.contains("executionReport")) {
						try {
							ExecutionReportResponse err = MOSHI.adapter(ExecutionReportResponse.class).fromJson(message);

							for(OrderEventListener oel : orderEventListeners) {
								oel.onOrderEvent(err, message);
							}
						}
						catch(Exception e) {
							e.printStackTrace();
						}

						return;
					}
				}
			}
		);
	}

	public void parseMessage(final String msg) {
		if(msg.contains("\"stream\"") && msg.contains("@depth")) {
			try {
				DepthEvent de = MOSHI.adapter(DepthEvent.class).fromJson(msg);

				if(de.data.asks != null) {
					for(List<String> a : de.data.asks) {
						ASKS.put(Double.valueOf(a.get(0)), Double.valueOf(a.get(1)));
					}
				}
				if(de.data.bids != null && de.data.bids.size() > 0) {
					for(List<String> b : de.data.bids) {
						BIDS.put(Double.valueOf(b.get(0)), Double.valueOf(b.get(1)));
					}
				}

				for(OrderBookListener obl : orderBookListeners) {
					obl.onNewOrderBook(de.stream.substring(0, de.stream.indexOf("@")), BIDS, ASKS);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			return;
		}

		if(msg.startsWith("{\"stream\"") && msg.contains("@trade\"")) {
			DateTime tradeDT;
			try {
				TradeEvent te = MOSHI.adapter(TradeEvent.class).fromJson(msg);

				for(TradeListener tl : tradeListeners) {
					tl.onNewTrade(te.data, te.data.s, msg);
				}

				tradeDT = new DateTime(te.data.T);

				OHLCItem lastCandle;
				long lastCandleEndMs;

				if(OHLC_SERIES != null && OHLC_SERIES.containsKey(te.data.s.toUpperCase())) {
					boolean isCandleUpdate = true;

					for(Integer intervalKey : OHLC_SERIES.get(te.data.s.toUpperCase()).keySet()) {
						if(OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).getItemCount() < 1) continue;

						lastCandle		=	(OHLCItem) OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).getDataItem(OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).getItemCount()-1);
						lastCandleEndMs	=	lastCandle.getPeriod().getFirstMillisecond()+(intervalKey*1000);

						if(tradeDT.getMillis() > lastCandleEndMs) {
							OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).add(
								new FixedMillisecond(lastCandleEndMs),
								Double.parseDouble(te.data.p),
								Double.parseDouble(te.data.p),
								Double.parseDouble(te.data.p),
								Double.parseDouble(te.data.p),
								Double.parseDouble(te.data.q)
							);

							if(OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).getItemCount() > OHLC_ITEMS_COUNT) {
								OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).remove(0);
							}

							isCandleUpdate = false;
						}
						else {// update last candle
							OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey).updatePriceVolume(
								Double.parseDouble(te.data.p), lastCandle.getVolume()+Double.parseDouble(te.data.q)
							);

							isCandleUpdate = true;
						}

						for(CandleListener cl : candleListeners) {
							cl.onNewCandleData(
								OHLC_SERIES.get(te.data.s.toUpperCase()).get(intervalKey),
								te.data.s.toUpperCase().toUpperCase(),
								intervalKey,
								isCandleUpdate,
								msg
							);
						}
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			return;
		}
	}
}

class TradeEvent {
	public String stream;
	public Trade data;
}

class DepthEvent {
	public String stream;
	public OrderBook data;
}
