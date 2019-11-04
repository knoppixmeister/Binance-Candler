package bn.candler;

import java.util.List;

/*
	{
		"lastUpdateId":181496931,
		"bids":[
			["9303.90000000","0.00214800"],
			["9303.88000000","1.00000000"],
			["9301.89000000","0.03171000"],

			...
		],
		"asks":[
			["9310.19000000","0.00214600"],
			["9310.20000000","1.00000000"],
			["9310.51000000","0.10000000"],
			["9311.37000000","0.02650000"],
			["9312.09000000","1.00000000"],

			....
		]
	}
*/
public class OrderBook {
	public long lastUpdateId;
	public List<List<String>> bids;
	public List<List<String>> asks;
}
