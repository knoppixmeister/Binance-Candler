package bn.candler;

import java.util.concurrent.*;

public interface OrderBookListener {
	public void onNewOrderBook(final String symbol, ConcurrentSkipListMap<Double, Double> BIDS, ConcurrentSkipListMap<Double, Double> ASKS);
}
