import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.SparseInstance;
import net.sf.javaml.distance.fastdtw.FastDTW;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class WorkThread extends Thread {
	BlockingQueue<JSONObject> queue = null;
	private volatile boolean running = true;
	Logger log = Logger.getLogger("WorkThread");
	
	public WorkThread(BlockingQueue<JSONObject> queue2) {
		super();
		this.queue = queue2;
	}
	public WorkThread() {
		super();
	}

	@Override
	public void run() {
		log.info("WorkThread running.");
		DefaultDataset dataset = new DefaultDataset();
		
		while (running){
			JSONObject json = queue.poll();
			if (json!= null){
				
				log.info("PEEKING : " + json.toJSONString());
				long timestamp = (Long) json.get("timestamp");
				log.info("--TIMESTAMP--:"+ parseMsTimeToHHMMSS(timestamp));

				SparseInstance instance = createInstanceFromJson(json);
				dataset.add(instance);
				log.info(dataset.toString());
				
				
			} else{
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	private static SparseInstance createInstanceFromJson(JSONObject json) {
		
		String device = (String) json.get("device");
		double[] sequence = getDoubleArrayFromJSONArray(json, "sequence");
		
		
		SparseInstance instance = new SparseInstance(sequence, device);
		return instance;
		
	}
	
	public void testClustering(){
		DefaultDataset dataset = new DefaultDataset();
		
		//A set of actual data, 3 signals are recorded at the same time & place, fourth one (Device: Outlier) is recorded at a different time
		String[] sampleDataStrings = {"{\"timestamp\":1396257980348,\"sequence\":[0,295,258,330,334,305,362,355,6486,6830,1043,9393,13436,4274,795,539,387,375,340,328,379,501,507,496,464,857,852,515,439,400,592,479,408,509,443,424,416,365,348,844,1727,1441,1146,752,959,960,750,538,405,437],\"device\":\"GT-I9100\"}",
		                   "{\"timestamp\":1396257980382,\"sequence\":[0,885,352,677,27057,665,902,783,2313,8142,8589,986,573,6141,38,62,83,23,19,38,19,30,19,33,706,1092,327,23,23,45,93,228,94,239,131,145,197,185,50,151,102,85,34,19,33,22,34,42,34,103],\"device\":\"Outlier\"}", 
		                   "{\"timestamp\":1396257980102,\"sequence\":[0,1090,319,364,426,525,514,494,375,342,1769,14378,7956,378,20993,18898,8302,1062,113,19,19,18,19,19,22,319,821,1240,938,659,853,581,69,54,34,82,70,120,143,405,114,31,34,19,94,1186,3141,2545,1917,545],\"device\":\"Nexus 5\"}",
		                   "{\"timestamp\":1396257980382,\"sequence\":[0,0,0,0,0,37,31,30,985,1325,258,1265,1966,663,334,152,41,37,36,37,34,32,52,47,59,71,49,74,82,59,48,57,46,40,44,75,92,49,40,67,158,95,177,208,229,162,77,66,80,104],\"device\":\"LT26w\"}"
		};
		for( String singleEntry : sampleDataStrings){
			try {
				JSONObject json = (JSONObject) new JSONParser().parse(singleEntry);
				dataset.add(createInstanceFromJson(json));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		runClustering(dataset);
	}
	
	
	public static void runClustering(DefaultDataset dataset) {
		FastDTW fastdtw=  new FastDTW(5);
		/* Create a new instance of the KMeans algorithm, with no options
		  * specified. By default this will generate 4 clusters. */
		Clusterer km = new KMeans(2, 1, fastdtw);
		Dataset[]clusters=km.cluster(dataset);
		
		
		for (Dataset ds : clusters){
			Logger.getLogger("WorkThread").info("RESULT = " + ds.toString());
		}
		
	}

	private static double[] getDoubleArrayFromJSONArray(JSONObject json, String string) {
		List<Number> sequence = new ArrayList<Number>(
				(JSONArray) json.get(string));
		double[] target = new double[sequence.size()];
		 for (int i = 0; i < target.length; i++) {
		    target[i] = sequence.get(i).doubleValue();
		 }
		return target;
		
	}

	private static String parseMsTimeToHHMMSS(Long timestamp)
	{
		Date date = new Date(timestamp);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
		String dateFormatted = formatter.format(date);

		return dateFormatted;
	}
	
	public DenseInstance createInstanceFromString(List<String> resultList){
		List<String> valuesList = resultList.subList(2, resultList.size()-1);
		int size = valuesList.size();
		double[] doubles = new double[size];
		for (int i = 0; i< size; i++){
			doubles[i] = Double.parseDouble(valuesList.get(i));
		}

		return new DenseInstance(doubles);
	}
	
	public void terminate() {
        running = false;
    }

	
}
