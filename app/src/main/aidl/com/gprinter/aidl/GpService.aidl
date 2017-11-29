package com.gprinter.aidl;
interface GpService{  
	int openPort(int PrinterId,int PortType,String DeviceName,int PortNumber);
	void closePort(int PrinterId);
	int getPrinterConnectStatus(int PrinterId);
	int printeTestPage(int PrinterId);   
  	void queryPrinterStatus(int PrinterId,int Timesout,int requestCode);
  	int getPrinterCommandType(int PrinterId);
	int sendEscCommand(int PrinterId, String b64);
  	int sendLabelCommand(int PrinterId, String  b64);
	void isUserExperience(boolean userExperience);
	String getClientID();
	int setServerIP(String ip, int port);
}       