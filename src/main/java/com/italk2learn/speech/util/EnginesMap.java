package com.italk2learn.speech.util;

import java.util.HashMap;
import java.util.ResourceBundle;

import com.italk2learn.vo.ASRInstanceVO;

public class EnginesMap {
	
	private static EnginesMap instance = null;
	
	private String LANGUAGE = "en_ux";
	private static final String SERVER = "localhost";
	private String MODEL = "base";
	private String SERVER_1 = "http://193.61.29.166:";
	private String SERVER_2 = "http://193.61.29.166:";
	private String SERVER_3 = "http://193.61.29.166:";
	private static final String SERVER_PART2 = "/italk2learnsm/speechRecognition";
	
	//JLF: Key=ServerID, Value=AsrInstanceVO 
	private HashMap<Integer, ASRInstanceVO> engines;
	
	protected EnginesMap() {
		ResourceBundle rb= ResourceBundle.getBundle("italk2learn-config");
		LANGUAGE=rb.getString("speech.language");
		MODEL=rb.getString("speech.model");
		SERVER_1=rb.getString("speech.server1");
		SERVER_2=rb.getString("speech.server2");
		SERVER_3=rb.getString("speech.server3");
		int numInstances=Integer.parseInt(rb.getString("speech.numinstances"));
		engines= new HashMap<Integer, ASRInstanceVO>();
		//JLF: Making an engine of 30 instances. The summative evaluations will contain 30 students		
		int port=8081;
		int instance=1;
		for (int i=1;i<=numInstances;i+=3){
			ASRInstanceVO server= new ASRInstanceVO();
			server.setAvailability(true);
			server.setInstance(instance);
			server.setId(i);
			server.setLanguageCode(LANGUAGE);
			server.setServer(SERVER);
			server.setModel(MODEL);
			server.setUser("");
			server.setUrl(SERVER_1+port+SERVER_PART2);
			port++;
			instance++;
			engines.put(i,server);			
		}
		port=8081;
		instance=1;
		for (int i=2;i<=numInstances;i+=3){
			ASRInstanceVO server= new ASRInstanceVO();
			server.setAvailability(true);
			server.setInstance(instance);
			server.setId(i);
			server.setLanguageCode(LANGUAGE);
			server.setServer(SERVER);
			server.setModel(MODEL);
			server.setUser("");
			server.setUrl(SERVER_2+port+SERVER_PART2);
			port++;
			instance++;
			engines.put(i,server);			
		}
		port=8081;
		instance=1;
		for (int i=3;i<=numInstances;i+=3){
			ASRInstanceVO server= new ASRInstanceVO();
			server.setAvailability(true);
			server.setInstance(instance);
			server.setId(i);
			server.setLanguageCode(LANGUAGE);
			server.setServer(SERVER);
			server.setModel(MODEL);
			server.setUser("");
			server.setUrl(SERVER_3+port+SERVER_PART2);
			port++;
			instance++;
			engines.put(i,server);			
		}
		this.setEngines(engines);
	}
	
	public static EnginesMap getInstance() {
		if (instance == null) {
			instance = new EnginesMap();
		}
		return instance;
	}
	
	//JLF: It retrieves an available instance of the engine
	public ASRInstanceVO getInstanceEngineAvailable(String user){
		ASRInstanceVO aux= new ASRInstanceVO();
		for(Integer key: engines.keySet())
			if (engines.get(key).getAvailability()==true) {
				aux=engines.get(key);
				aux.setAvailability(false);
				aux.setUser(user);
				return aux;
			}
		return aux;
	}
	
	public String getAllInfo(){
		String result="";
		String result2="";
		int counter=0;
		for(Integer key: engines.keySet()) {
			if (engines.get(key).getAvailability()==false) {
				counter++;
				result2=result2+"Instance "+engines.get(key).getInstance().toString()+" is used by user '"+engines.get(key).getUser()+ "' on machine '" + engines.get(key).getUrl() ;
				result2=result2+"'\n";
			}
		   result="There are " + counter +" speech reco instances working (out of "+ engines.size()+")\n";
		   result=result+result2;
		}
		return result;
	}
	
	public String getUrlByUser(String user) {
		for(Integer key: engines.keySet())
			if (engines.get(key).getUser().contentEquals(user)) {
				return engines.get(key).getUrl();
			}
		return null;
	}
	
	public Integer getInstanceByUser(String user) {
		for(Integer key: engines.keySet())
			if (engines.get(key).getUser().contentEquals(user)) {
				return engines.get(key).getInstance();
			}
		return null;
	}
	
	//JLF It releases an instance when the connection is close or the session is destroyed by a specific user 
	public void releaseEngineInstance(String user){
		for(Integer key: engines.keySet())
			if (engines.get(key).getUser().contentEquals(user)) {
				engines.get(key).setAvailability(true);
				engines.get(key).setUser("");
			}
	}

	public HashMap<Integer, ASRInstanceVO> getEngines() {
		return engines;
	}

	public void setEngines(HashMap<Integer, ASRInstanceVO> engines) {
		this.engines = engines;
	}

	
}
