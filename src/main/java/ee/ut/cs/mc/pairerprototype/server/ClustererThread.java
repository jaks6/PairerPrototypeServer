package ee.ut.cs.mc.pairerprototype.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.fastdtw.FastDTW;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class ClustererThread extends Thread {
	Logger log = Logger.getLogger("ClustererThreads");
	BlockingQueue<JSONObject> queue = null;
//	HashMap<String, JSONObject> deviceDetailsMap;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static JSONParser parser = new JSONParser();;

	public ClustererThread(BlockingQueue<JSONObject> queue, ConcurrentHashMap<String, JSONObject> instructionsMap) {
		super();
		this.queue = queue;
		this.instructionsMap = instructionsMap;
	}

	@Override
	public void run() {
		//Print synced time
		log.info("Running new clusterer thread		" +
				SntpClient.parseMsTimeToHHMMSS(
						TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) + 
						AppContextListener.timediff));
		
		//!TODO : add filter for too old timestamps 
		DefaultDataset dataset = new DefaultDataset();
		fillDataSetFromQueue(dataset);
		if(!dataset.isEmpty()){
			log.info("Clustering dataset of"+ dataset.size() + " elements");
			
			Dataset[] clusters = runClusterer(dataset);
			System.out.println("Result.		no of clusters=" + clusters.length);
//			storeClustersToCSV(clusters);
			//Provide instructions for each device (and cluster)
			GroupsManager.getInstance().processClusters(clusters);
			for (int i=0; i<clusters.length; i++) {
				log.info("SHOWING CLASSES=" + clusters[i].classes());
				for (int j=0; j <clusters[i].size(); j++){
				}
			}
		}
		log.info("Stopping clusterer thread");
	}

	private void storeClustersToCSV(Dataset[] clusters) {
		String[] clustersString = new String[clusters.length];
		if (clusters.length==0){
			clustersString = new String[2];
			clustersString[0]= "-";
			clustersString[1]= "-";
		}
		for (int i = 0; i< clusters.length; i++){
			for (Instance is : clusters[i]){
				clustersString[i] =clustersString[i]+ "___"+((RecordingInstance) is).getDeviceNickName();
			}
			clustersString[i].replaceFirst("^\\s+", "");
		}
		try
		{
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date date = new Date();
	    		File file =new File(dateFormat.format(date)+"test.csv");
	    		System.out.println(file.getAbsolutePath());
	    		//if file doesnt exists, then create it
	    		if(!file.exists()){
	    			file.createNewFile();
	    		}
	 
	    		//true = append file
	    		FileWriter fileWritter = new FileWriter(file.getName(),true);
	    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	    	        for (String s : clustersString){
	    	        	bufferWritter.write(s + ";");
	    	        }
	    	        bufferWritter.write("\n");
	    	        bufferWritter.close();
		}
		catch(IOException e)
		{
		     e.printStackTrace();
		} 
		
	}

	private void fillDataSetFromQueue(DefaultDataset dataset) {
		while(! queue.isEmpty()){
			JSONObject json = queue.poll();
			if (json != null) {

				RecordingInstance instance = createNormalizedInstance(json);
				dataset.add(instance);
			} else {

			}
		}
	}

	/** Produces a DenseInstance subclass RecordingInstance from the json in which the sample values are normalized.
	 * NB - this method removes the "sequence" object from the argument json.
	 * The classvalue of the instance which is used to later identify the device once clustered,
	 *   will be the MAC address of the device
	 * @param dataset
	 * @param json
	 */
	private RecordingInstance createNormalizedInstance(JSONObject json) {
		List<Number> sequence = new ArrayList<Number>(
				(JSONArray) json.remove("sequence"));
		double[] target = new double[sequence.size()];
		
		for (int i = 0; i < target.length; i++) {
			target[i] = sequence.get(i).doubleValue();
		}
		normalizeSeq(target);
		RecordingInstance instance = new RecordingInstance(target, (String)json.get("mac"));
		instance.setDeviceNickName((String)json.get("nickname"));
		return instance;
	}

	private void normalizeSeq(double[] seq) {
		double mean = new Mean().evaluate(seq);
		for (int i=0; i<seq.length; i++){
			seq[i]= seq[i]/mean;
		}
	}
	
	private double[] energy(double[] seq){
		final int windowLength = 5;
		double[] hammondVals = {0.08, 0.54, 1, 0.54, 0.08};
		int samples = seq.length;
		double[] result = new double[samples];
		double sum;
		for (int i = 0; i<samples-windowLength; i++){
			sum = 0;
			for (int w=0; w< windowLength; w++){
				sum += Math.sqrt((seq[i+w]*seq[i+w])* hammondVals[w]);
			}
			result[i] = sum / windowLength;
		}
		return result;
	}
	
	

	private Dataset[] runClusterer(Dataset dataset) {
		FastDTW fastdtw = new FastDTW(5);

		int minPts = 2;
		double epsilon = 15.5;
		DensityBasedSpatialClustering dbsc = new DensityBasedSpatialClustering(
				epsilon, minPts, fastdtw);
		return dbsc.cluster(dataset);

	}

}
