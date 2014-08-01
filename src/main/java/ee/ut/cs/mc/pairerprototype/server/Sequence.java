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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
@SuppressWarnings("unchecked")
@WebServlet("/sequence")
public class Sequence extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getOutputStream().println("Get received, but there's nothing here");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String reply = null;
			String reqType = request.getHeader("reqtype");
			
			if (reqType != null){
				log("REQUEST TYPE="+ reqType);
				if (reqType.equals("json")){
					//add JSON object from request to a queue, from which a ClustererThread will gather it and process it
					JSONObject receivedJson = getJSONFromRequest(request);
					ServletContext ctx = getServletContext();
					BlockingQueue<JSONObject> queue = 
							(LinkedBlockingQueue<JSONObject>) ctx.getAttribute("dataQueue");
					queue.add(receivedJson);

					String requesterMac = (String)receivedJson.get("mac");
					reply = findResponse(requesterMac);
				}
			}
			//Write response to request
			OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
			if(reply != null){
				writer.write(reply);	
			} 
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


	private String findResponse(String mac){
		//get instructions map object
		ServletContext ctx = getServletContext();
		ConcurrentHashMap<String, JSONObject> instructionMap = 
				(ConcurrentHashMap<String, JSONObject>) ctx.getAttribute("instructionMap");
		//find instructions for the device making the request
		log("**Fetching instructions for="+ mac);
		
		
//		createMockInstructions(mac, instructionMap);
		
		JSONObject instructionsJson = instructionMap.get(mac);
		if (instructionsJson != null){
			return instructionsJson.toString();
		}
		return "{}";
	}

	private void createMockInstructions(String mac,
			ConcurrentHashMap<String, JSONObject> instructionMap) {
		instructionMap.clear();
		String serverMACSII = "0C:DF:A4:71:6D:06";
		String MACXperia = "D0:51:62:93:E8:CE";
		String nexus5 = "CC:FA:00:16:2B:9A";
		
		JSONObject instruction = new JSONObject();
		if (mac.equals(nexus5)) instruction.put("connectto", MACXperia);
		else instruction.put("connectto", nexus5);
		
		instruction.put("listento", true);
		JSONArray group = new JSONArray();
		group.add("member1");
		group.add("member2");
		instruction.put("group", group);
		
		instructionMap.put(mac, instruction);
	}
}