 package sopc2dts.lib.boardinfo;
 
 import org.xml.sax.Attributes;
 
 public class BICEthernet {
 	String name;
 	Integer miiID = null;
 	Integer phyID = null;
 	int[] mac = { 0, 0, 0, 0, 0, 0 };
 
 	public BICEthernet(String compName) {
 		name = compName;
 	}
 	public BICEthernet(Attributes atts) {
 		this(atts.getValue("name"));
 		String sVal = atts.getValue("mii_id"); 
 		if(sVal != null)
 		{
 			miiID = Integer.decode(sVal);
 		}
 		sVal = atts.getValue("phy_id"); 
 		if(sVal != null)
 		{
			miiID = Integer.decode(sVal);
 		}
 		setMac(atts.getValue("mac")); 
 	}
 
 	public String getName() {
 		return name;
 	}
 
 	public String toXml() {
 		String res = "<Ethernet name=\"" + name + "\" mac=\"" + getMacString() + "\"";
 		if(miiID!=null)
 		{
 			res += " mii_id=\"" + miiID + "\"";
 		}
 		if(phyID!=null)
 		{
 			res += " phy_id=\"" + phyID + "\"";
 		}
 		res +="></Ethernet>\n";
 		return res;
 	}
 	public String getMacString()
 	{
 		return String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1],
 				mac[2], mac[3], mac[4], mac[5]);
 	}
 
 	public Integer getMiiID() {
 		return miiID;
 	}
 
 	public void setMiiID(Integer miiID) {
 		this.miiID = miiID;
 	}
 
 	public Integer getPhyID() {
 		return phyID;
 	}
 
 	public void setPhyID(Integer phyID) {
 		this.phyID = phyID;
 	}
 
 	public int[] getMac() {
 		return mac;
 	}
 
 	public void setMac(int[] mac) {
 		if(mac.length == 6)
 		{
 			this.mac = mac;
 		}
 	}
 	public void setMac(String sVal)
 	{
 		if(sVal != null)
 		{
 			String[] sMac = sVal.split(":");
 			if(sMac.length==6)
 			{
 				for(int i=0; i<6; i++)
 				{
 					mac[i] = Integer.parseInt(sMac[i], 16);
 				}
 			}
 		}
 	}
 }
