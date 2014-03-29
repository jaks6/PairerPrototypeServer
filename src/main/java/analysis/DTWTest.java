package analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;


import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.fastdtw.FastDTW;
import net.sf.javaml.tools.data.FileHandler;

public class DTWTest {



	public DenseInstance createInstanceFromString(List<String> resultList){

		List<String> valuesList = resultList.subList(2, resultList.size()-1);
		int size = valuesList.size();
		double[] doubles = new double[size];
		for (int i = 0; i< size; i++){
			doubles[i] = Double.parseDouble(valuesList.get(i));
		}

		return new DenseInstance(doubles);
	}

	public void runDTW(	){

		/* values of the attributes. */
		double[] values = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		/*
		 * The simplest incarnation of the DenseInstance constructor will only
		 * take a double array as argument an will create an instance with given
		 * values as attributes and no class value set. For unsupervised machine
		 * learning techniques this is probably the most convenient constructor.
		 */
		DenseInstance instance = new DenseInstance(values);
		FastDTW dtw = new FastDTW(3); 
	}

	public static double compareWithDTW(Instance x, Instance y, int radius){
		FastDTW dtw = new FastDTW(radius); 
		return  dtw.measure(x, y);
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Dataset data = FileHandler.loadDataset(new File("iris.data"), 4, ",");
		Clusterer km = new KMeans();
		Dataset[]clusters=km.cluster(data);
	}

}
