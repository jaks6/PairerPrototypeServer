package ee.ut.cs.mc.pairerprototype.server;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InstructionJSON extends JSONObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4603105263230868139L;
	private static final String ACCEPTFROMKEY = "acceptfrom";
	private static final String CONNECTTOKEY = "connectto";
	private static final String GROUPIDKEY = "groupid";
	private static final String GROUPMEMBERSKEY = "groupmembers";

	
	/** Constructor for masters instructions */
	public InstructionJSON(JSONArray acceptFrom,
			JSONArray connectto,
			String groupID,
			JSONArray groupmembers) {
		
		super();
		this.put(ACCEPTFROMKEY, acceptFrom);
		this.put(CONNECTTOKEY, connectto);
		this.put(GROUPIDKEY, groupID);
		this.put(GROUPMEMBERSKEY, groupmembers);
	}
	
	
	/** Constructor for creating instructions for a slave */
	public InstructionJSON(String master,
			String groupID,
			JSONArray groupmembers) {
		
		super();
		JSONArray connectToArray = new JSONArray();
		connectToArray.add(master);
		this.put(CONNECTTOKEY, master);
		this.put(GROUPIDKEY, groupID);
		this.put(GROUPMEMBERSKEY, groupmembers);
	}

	public InstructionJSON(Map map) {
		super(map);
		// TODO Auto-generated constructor stub
	}

}
