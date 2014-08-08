package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.sf.javaml.core.Instance;

/*** A single row of the table which describes how nodes of the ring of masters
 * network connect to each other.
 * @author jaks
 *
 */
public class TableEntry {

	public String mac;
	public HashSet<String> connectToList;
	public HashSet<String> acceptFromList;
	
	
	public TableEntry(String mac, HashSet<String> connectToList,
			HashSet<String> acceptFromList) {
		super();
		this.mac = mac;
		this.connectToList = connectToList;
		this.acceptFromList = acceptFromList;
	}
	
	
	public TableEntry(String mac) {
		super();
		this.mac = mac;
		this.connectToList = new HashSet<>();
		this.acceptFromList = new HashSet<>();
	}
	
	public TableEntry(Instance node) {
		super();
		this.mac = (String) node.classValue();
		this.connectToList = new HashSet<>();
		this.acceptFromList = new HashSet<>();
	}
	
	
	public boolean isMaster(){
		return ! acceptFromList.isEmpty();
	}
}
