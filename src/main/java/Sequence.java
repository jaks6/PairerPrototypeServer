import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.SparseInstance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import weka.classifiers.functions.SMO;

import analysis.IncomingDataListener;

@WebServlet("/sequence1")
public class Sequence extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private IncomingDataListener listener;
	private DefaultDataset myDataset;
	private int counter;

	public Sequence() {
		//Servlet constructor
		super();
		counter = 0;
		myDataset = new DefaultDataset();
		listener = new IncomingDataListener();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getOutputStream().println("Hurray !! This Servlet Works");
		log("doGET");
	}

	public void somethingHappened() {
		listener.singleDataEvent("Something Happened");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		double[] vector = {1,2,3};
		SparseInstance singleInstance = new SparseInstance(vector);
		myDataset.add(singleInstance);
		try {
			log("***--- Instance size is  " + myDataset.toString());
			JSONObject receivedJson = getJSONFromRequest(request);

			long timestamp = (Long) receivedJson.get("timestamp");
			String device = (String) receivedJson.get("device");
			List<Integer> sequence = new ArrayList<Integer>(
					(JSONArray) receivedJson.get("sequence"));
			log("--TIMESTAMP--:"+ parseMsTimeToHHMMSS(timestamp));

			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:\\Users\\jaks\\desktop\\file.csv", true)))) {
				out.println(device + ";"+ timestamp);
			}catch (IOException e) {
				//exception handling left as an exercise for the reader
			}
			
			
			OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
			writer.write("Server successfully received your POST");
			writer.flush();
			writer.close();


		} catch (IOException e) {
			try{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().print(e.getMessage());
				response.getWriter().close();
			} catch (IOException ioe) {
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}  
	}

	private JSONObject getJSONFromRequest(HttpServletRequest request)
			throws ParseException, IOException {
		return (JSONObject) new JSONParser().parse(request.getReader());
	}

	private static String parseMsTimeToHHMMSS(Long timestamp)
	{
		Date date = new Date(timestamp);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
		String dateFormatted = formatter.format(date);

		return dateFormatted;
	}

}