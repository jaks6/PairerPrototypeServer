package ee.ut.cs.mc.pairerprototype.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/sequence")
public class Sequence extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String UUID_BASE ="7d0cea40-c618-11e3-9c1a-"; // UUID_BASE+ MAC_ADDRESS will form a valid UUID for use in bluetooth connections

	public Sequence() {
		//Servlet constructor
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getOutputStream().println("Hurray !! This Servlet Works");

//		WorkThread thread = new WorkThread(); //this instance is created for testing purposes
//		thread.testClustering();
		
		try {
			JSONObject o1 = (JSONObject) new JSONParser().parse("{\"timestamp\":1396434160113,\r\n" + 
					"\"sequence\":[0,978,238,18675,19433,8218,30529,7085,2748,30699,30494,29451,13379,4354,30446,3506,8162,18571,19435,8014,23051,16371,19070,8538,17557,12878,21543,11494,7749,12055,6778,581,19,371,4467,15522,18446,14175,12756,6916,6830,19307,4299,2859,1624,19,21,19,19,19],\r\n" + 
					"\"device\":\"Nexus 5\"}");
			JSONObject o2 = (JSONObject) new JSONParser().parse("{\"timestamp\":1396434160113,\r\n" + 
					"\"sequence\":[0,0,1,175,19433,8218,30529,7085,2748,30699,30494,29451,13379,4354,30446,3506,8162,18571,19435,8014,23051,16371,19070,8538,17557,12878,21543,11494,7749,12055,6778,581,19,371,4467,15522,18446,14175,12756,6916,6830,19307,4299,2859,1624,19,21,19,19,19],\r\n" + 
					"\"device\":\"Nexus 5_2\"}");
//			WorkThread.testDTW(o1,o2);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String reply = null;
			String reqType = request.getHeader("reqtype");
			if (reqType != null){
				log("REQUEST TYPE="+ reqType);
				if (reqType.equals("json")){
					//add JSON object from request to a queue, from which a WorkThread will gather it and process it
					JSONObject receivedJson = getJSONFromRequest(request);
					ServletContext ctx = getServletContext();
					BlockingQueue<JSONObject> queue = 
							(LinkedBlockingQueue<JSONObject>) ctx.getAttribute("queue");
					ConcurrentHashMap<String, JSONObject> instructionMap = 
							(ConcurrentHashMap<String, JSONObject>) ctx.getAttribute("instructionmap");
					queue.add(receivedJson);
					
					
					String requesterMac = (String)receivedJson.get("mac");
					reply = MockResponse(requesterMac);
				}
				else if (reqType.equals("signOn")){
					
				} else if (reqType.equals("signOff")){
					
				}
			}
			//Write response to request
			OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
			if(reply != null){
				writer.write(reply);	
			} else {
				writer.write("no mock response");
			}
//			writer.write("Server successfully received your POST");
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
	
	@SuppressWarnings("unchecked")
	private String MockResponse(String mac){
		String MACSII = "0C:DF:A4:71:6D:06";
		String MACXperia = "D0:51:62:93:E8:CE";
		String MACNexus5 = "CC:FA:00:16:2B:9A";
		
		
		//get instructions map object
		ServletContext ctx = getServletContext();
		ConcurrentHashMap<String, JSONObject> instructionMap = 
				(ConcurrentHashMap<String, JSONObject>) ctx.getAttribute("instructionmap");
		
		//add mock instructions to the map
		JSONObject json = new JSONObject();
		json.put("connectto", MACNexus5);
		instructionMap.put(mac, json);
		
		//find instructions for the device making the request
		JSONObject instructionsJson = instructionMap.remove(mac);
		if (instructionsJson != null){
			return instructionsJson.toString();
		}
		else return "";
	}
	

}