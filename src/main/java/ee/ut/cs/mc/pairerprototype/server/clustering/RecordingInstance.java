package ee.ut.cs.mc.pairerprototype.server.clustering;

import net.sf.javaml.core.DenseInstance;

public class RecordingInstance extends DenseInstance{
	private static final long serialVersionUID = 1597052719921836676L;
	String deviceNickName = null;
	String deviceMAC = null;
	public String lastGroupId = null;
	
	public String getDeviceNickName() {
		return deviceNickName;
	}

	public void setDeviceNickName(String deviceNickName) {
		this.deviceNickName = deviceNickName;
	}
	
	public void setLastGroupId(String id){
		this.lastGroupId = id;
	}

	public String getDeviceMAC() {
		return deviceMAC;
	}

	public void setDeviceMAC(String deviceMAC) {
		this.deviceMAC = deviceMAC;
	}

	public RecordingInstance(double[] att, Object classValue) {
		super(att, classValue);
	}
}
