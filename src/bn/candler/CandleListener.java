package bn.candler;

import org.jfree.data.time.ohlc.*;

public interface CandleListener {
	void onNewCandleData(final OHLCSeries series, final String pair, final int intervalTs, final boolean isCandleUpdate, final String rawData);
}
