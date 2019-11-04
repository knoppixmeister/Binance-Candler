package bn.candler;

public interface TradeListener {
	void onNewTrade(final Trade trade, final String pair, final String rawData);
}
