package bn.candler;

public interface OrderEventListener {
	public void onOrderEvent(ExecutionReportResponse orderEvent, final String rawData);
}
