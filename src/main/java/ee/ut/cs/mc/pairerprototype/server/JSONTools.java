package ee.ut.cs.mc.pairerprototype.server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.SparseInstance;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONTools {

	static DenseInstance createInstance(JSONObject json) {
		String device = (String) json.get("device");

		double[] sequence = getDoubleArray(json, "sequence");
		// log.info("About to normalize");
		// for (int i=0; i<sequence.length; i++)
		// System.out.print(sequence[i]+";");
		// System.out.println(" END");
		// //Normalize
		// sequence = normalizeArray(sequence);
		// log.info("Done with normalize");
		// for (int i=0; i<sequence.length; i++)
		// System.out.print(sequence[i]+";");
		// System.out.println(" END");

		DenseInstance instance = new DenseInstance(sequence, device);
		return instance;

	}
	
	static void readSequenceFromJSON(String filename, ArrayList<DenseInstance> instanceList){
		JSONParser parser = new JSONParser();
		double[] target = null;
		 
		try {
	 
			Object obj = parser.parse(new FileReader(filename));
	 
			JSONObject jsonObject = (JSONObject) obj;
			
			List<Number> sequence = new ArrayList<Number>(
					(JSONArray) jsonObject.get("sequence"));
			target = new double[sequence.size()];
			for (int i = 0; i < target.length; i++) {
				target[i] = sequence.get(i).doubleValue();
			}
			
			normalizeSeq(target);
			
			DenseInstance instance = new DenseInstance(target, (jsonObject).get("device"));
			instanceList.add(instance);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	
	public SparseInstance createInstanceFromString(List<String> resultList) {
		List<String> valuesList = resultList.subList(2, resultList.size() - 1);
		int size = valuesList.size();
		double[] doubles = new double[size];
		for (int i = 0; i < size; i++) {
			doubles[i] = Double.parseDouble(valuesList.get(i));
		}

		return new SparseInstance(doubles);
	}
	private static void normalizeSeq(double[] seq) {
		double mean = new Mean().evaluate(seq);
		for (int i=0; i<seq.length; i++){
			seq[i]= seq[i]/mean;
		}
	}
	
	/** Takes the array with the given key from the given json file,
	 * tries to make it into a double array and returns the double aray
	 */
	@SuppressWarnings("unchecked")
	private static double[] getDoubleArray(JSONObject json,
			String string) {
		List<Number> sequence = new ArrayList<Number>(
				(JSONArray) json.get(string));
		double[] target = new double[sequence.size()];
		for (int i = 0; i < target.length; i++) {
			target[i] = sequence.get(i).doubleValue();
		}
		return target;

	}

}
