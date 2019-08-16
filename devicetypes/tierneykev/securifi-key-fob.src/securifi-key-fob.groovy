import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata {
    definition (name: "Securifi Key Fob", namespace: "hubitat", author: "Kevin Tierney") {
        capability "PushableButton"
        capability "Configuration"
        fingerprint profileId: "0104", inClusters: "0000,0003,0500", outClusters: "0003,0501", manufacturer: "Sercomm Corp.", model: "SZ-KFB01", deviceJoinName: "Securifi Key Fob"
    }
preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}







def parse(String description) {

  if (description?.startsWith("enroll request")) {
    if (logEnable) log.debug "RECEVED ENROLL REQUEST: ${description}"
        List cmds = zigbee.enrollResponse(1200)
        result = cmds?.collect { new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
  } else {
    if (description?.startsWith("zone status")){
      ZoneStatus zs = zigbee.parseZoneStatus(description)
      if (logEnable) log.debug zs
    } else {
      def descMap = zigbee.parseDescriptionAsMap(description)
	  if descMap.profileid = '0000' return      
	  if (logEnable) log.debug "Parsed Zigbee Map: ${descMap}"
      
	  def cluster = descMap.clusterId
      def sourceEndpoint = descMap.sourceEndpoint
      def button = descMap.data[0]
      
	  if (logEnable) log.debug "Cluster: ${cluster} Source Endpoint: ${sourceEndpoint} Button: ${button}"
	  
	  if(button) process_button(button)
	  
    }
 }
}

def process_button(button_number){
	//***NEED TO TEST
	 //Securifi Numbering - 0 = Unlock, 1 = * (only used to join), 2 = Home, 3 = Lock
	 if(button == 0){
		//Unlock Button Sec-0/Hub-3
		sendEvent( name: "pushed", value: 3, isStateChange: true )
		if (logEnable) log.debug "Unlock Button Pressed"
	 } else if (button == 2){
		//Home Button Sec-2/Hub-2
		sendEvent( name: "pushed", value: 2, isStateChange: true )
		if (logEnable) log.debug "Home Button Pressed"
	 
	 } else if (button == 3){
		//Lock Button Sec-3/Hub-1
		sendEvent( name: "pushed", value: 1, isStateChange: true )
		if (logEnable) log.debug "Lock Button Pressed"
	 }
}

def configure(){
	log.debug "Config Called"
	String hubZigbeeId = swapEndianHex(device.hub.zigbeeId)
	def configCmds = [
		//------IAS Zone/CIE setup------//
		//zcl global write [cluster:2] [attributeId:2] [type:4] [data:-1]
		"zcl global write 0x500 0x10 0xf0 {${hubZigbeeId}}", "delay 200",
		"send 0x${device.deviceNetworkId} 0x08 1", "delay 1500",

		//------Set up binding------//
		//zdo bind Dev_Nework_ID Src_Endpoint Dest_Endpoint Cluster Zigbee_ID
		"zdo bind 0x${device.deviceNetworkId} 0x08 0x01 0x0501 {${device.zigbeeId}} {}", "delay 500",
		//**Do we need this
		"zdo bind 0x${device.deviceNetworkId} 0x08 1 1 {${device.zigbeeId}} {}"
	] 
	return configCmds
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}
