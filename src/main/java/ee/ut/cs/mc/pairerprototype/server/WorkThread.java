package ee.ut.cs.mc.pairerprototype.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.distance.fastdtw.FastDTW;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WorkThread extends Thread {
	ScheduledThreadPoolExecutor scheduledExecutor;
	BlockingQueue<JSONObject> queue = null;
	private volatile boolean running = true;
	static Logger log = Logger.getLogger("WorkThread");

	public WorkThread(BlockingQueue<JSONObject> queue) {
		scheduledExecutor = (ScheduledThreadPoolExecutor)
				Executors.newScheduledThreadPool(3);
		this.queue = queue;
	}

	@Override
	public void run() {
		log.info("WorkThread running.");
		waitTilTimeSynced();
		long initialDelay = SntpClient.calculateInitialDelay();
		scheduledExecutor.scheduleAtFixedRate(new ClustererThread(queue), initialDelay, 10000, TimeUnit.MILLISECONDS);
		
	}

	private void waitTilTimeSynced() {
		while(AppContextListener.timediff == 0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info("Waiting for time syncing to finish");
		}
		log.info("timeDiff ="+ AppContextListener.timediff);
	}

	void writeToFile(String data, String path) throws IOException {
		File file = new File(path);
		log.info("Writing=" + data);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		// true = append file
		FileWriter fileWritter = new FileWriter(file, true);
		BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		bufferWritter.write(data + "\n");
		bufferWritter.close();

	}

	

	public static double[] normalizeArray(double[] array) {
		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = new DescriptiveStatistics();

		// Add the data from the array
		for (int i = 0; i < array.length; i++) {
			stats.addValue(array[i]);
		}

		// Compute some statistics
		double mean = stats.getMean();
		double std = stats.getStandardDeviation();
		double median = stats.getPercentile(50);

		for (int i = 0; i < array.length; i++) {
			// if array element is smaller than the median, change it to 1,
			// otherwise change it to 0
			array[i] = (array[i] <= median) ? 0 : 1;
		}
		return array;
	}

	public void testClustering() {
		DefaultDataset dataset = new DefaultDataset();

		// A set of actual data, 3 signals are recorded at the same time &
		// place, fourth one (Device: Outlier) is recorded at a different time
		String[] sampleDataStrings = {
				"{\"timestamp\":1396257980348,\"sequence\":[0,295,258,330,334,305,362,355,6486,6830,1043,9393,13436,4274,795,539,387,375,340,328,379,501,507,496,464,857,852,515,439,400,592,479,408,509,443,424,416,365,348,844,1727,1441,1146,752,959,960,750,538,405,437],\"device\":\"GT-I9100\"}",
				"{\"timestamp\":1396257980382,\"sequence\":[0,885,352,677,27057,665,902,783,2313,8142,8589,986,573,6141,38,62,83,23,19,38,19,30,19,33,706,1092,327,23,23,45,93,228,94,239,131,145,197,185,50,151,102,85,34,19,33,22,34,42,34,103],\"device\":\"Outlier\"}",
				"{\"timestamp\":1396257980102,\"sequence\":[0,1090,319,364,426,525,514,494,375,342,1769,14378,7956,378,20993,18898,8302,1062,113,19,19,18,19,19,22,319,821,1240,938,659,853,581,69,54,34,82,70,120,143,405,114,31,34,19,94,1186,3141,2545,1917,545],\"device\":\"Nexus 5\"}",
				"{\"timestamp\":1396257980382,\"sequence\":[0,0,0,0,0,37,31,30,985,1325,258,1265,1966,663,334,152,41,37,36,37,34,32,52,47,59,71,49,74,82,59,48,57,46,40,44,75,92,49,40,67,158,95,177,208,229,162,77,66,80,104],\"device\":\"LT26w\"}" };
		for (String singleEntry : sampleDataStrings) {
			try {
				JSONObject json = (JSONObject) new JSONParser()
						.parse(singleEntry);
				JSONArray array = (JSONArray) json.get("sequence");
				dataset.add(JSONTools.createInstance(json));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		runClustering(dataset);
	}

	public static void testDTW(JSONObject o1, JSONObject o2) {

		FastDTW fastdtw = new FastDTW(5);
		DenseInstance i1 = JSONTools.createInstance(o1);
		DenseInstance i2 = JSONTools.createInstance(o2);
		log.info("FastDTW measure=" + fastdtw.measure(i1, i2));

	}

	public static void runClustering(DefaultDataset dataset) {
		log.info("DATASET=" + dataset);
		FastDTW fastdtw = new FastDTW(5);
		/*
		 * Create a new instance of the KMeans algorithm, with no options
		 * specified. By default this will generate 4 clusters.
		 */
		Clusterer km = new KMeans(2, 1, fastdtw);
		Dataset[] clusters = km.cluster(dataset);
		for (Dataset ds : clusters) {
			log.info("RESULT = " + ds.toString());
		}

	}





	public void terminate() {
		running = false;
	}

}
