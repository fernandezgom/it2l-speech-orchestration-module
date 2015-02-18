package com.italk2learn.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.gienah.testing.junit.Configuration;
import org.gienah.testing.junit.Dependency;
import org.gienah.testing.junit.SpringRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.client.RestTemplate;

import com.italk2learn.vo.HeaderVO;
import com.italk2learn.vo.SpeechRecognitionRequestVO;
import com.italk2learn.vo.SpeechRecognitionResponseVO;

@RunWith(value = SpringRunner.class)
@Configuration(locations = { "web-application-config.xml" })
public class SpeechRecognitionTest {
	
	private static final int ARRAY_SIZE = 500000;// Should be a multiple of 16
	private static final int NUM_SECONDS = 5 * 1000;
	
	@Dependency
	private RestTemplate restTemplate;
	
	private SpeechRecognitionResponseVO liveResponse=new SpeechRecognitionResponseVO();
	private List<byte[]> audioChunks=new ArrayList<byte[]>();
	private boolean oneChunk=false;
	private int srUsed=0;
	private static int TEST_SR=30;
	
	
	private static final Logger LOGGER = Logger.getLogger(SpeechRecognitionTest.class);
	
	@Test
	public void callAllSpeechRecos() throws Exception {
		final int dataSize = (int) (Runtime.getRuntime().maxMemory());
		System.out.println("Max amount of memory is: "+dataSize);
		boolean testOk = false;
		File f=new File("no-maths-vocab-example-01-mono.wav");
		long l=f.length();
		long numChunks=l/ARRAY_SIZE;
		System.out.println("the file is " + l + " bytes long");
		FileInputStream finp = null;
		byte[] b=new byte[(int)l];
		try {
			finp = new FileInputStream(f);
			int i;
			i=finp.read(b);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (!oneChunk) {
			int initialChunk=0;
			int finalChunk=(int)l/(int)numChunks;
		    if ((finalChunk % 16)!=0)
		           finalChunk=finalChunk-(finalChunk % 16);
				
			for (int i=0;i<numChunks;i++){
				byte[] aux=Arrays.copyOfRange(b, initialChunk, finalChunk);
				audioChunks.add(aux);
				System.out.println("Chunk "+i+" starts at "+initialChunk+" bytes and finish at "+finalChunk+" bytes");
				initialChunk=finalChunk;
				finalChunk=finalChunk+((int)l/(int)numChunks);
			    if ((finalChunk % 16)!=0)
			        finalChunk=finalChunk-(finalChunk % 16);
			}
			if (initialChunk<l){
				System.out.println("Last chunk starts at "+initialChunk+" bytes and finish at "+l+" bytes");
				audioChunks.add(Arrays.copyOfRange(b, initialChunk, (int)l));
			}
		}
		executeSpeechRecognisers(TEST_SR);
		while (true){
			if (srUsed==TEST_SR){
				srUsed=0;
				executeSpeechRecognisers(TEST_SR);
			}
		}
		//		System.out.println("All jobs finished");
	}
	
	private void executeSpeechRecognisers(int con) throws Exception {
		
		Map<String, String> vars = new HashMap<String, String>();
		int port=8080;
		int instance;
		String user="";
		String student="student";
		for (int i=1;i<=con;i++){
			SpeechRecognitionRequestVO request= new SpeechRecognitionRequestVO();
			request.setHeaderVO(new HeaderVO());
			port++;
			user=student+Integer.toString(i);
			instance=i;
			request.getHeaderVO().setLoginUser(user);
			request.setInstance(instance);
			vars.put("user", user);
			vars.put("instance", Integer.toString(instance));
			vars.put("server", "localhost");
			vars.put("language", "en_ux");
			vars.put("model", "base");
			try {
				//Call initEngineService of an available instance
				System.out.println("URL= "+"http://193.61.29.166:"+Integer.toString(port)+"/italk2learnsm/speechRecognition/initEngine");
				Boolean isOpen=this.restTemplate.getForObject("http://193.61.29.166:"+Integer.toString(port)+"/italk2learnsm/speechRecognition/initEngine?user={user}&instance={instance}&server={server}&language={language}&model={model}",Boolean.class, vars);
				if (isOpen){
					Timer timer = new Timer();
					//JLF:Send chunk each NUM_SECONDS
					System.out.println("Creating TIMER="+i+" INSTANCE= "+request.getInstance()+" USER="+request.getHeaderVO().getLoginUser());
					timer.scheduleAtFixedRate(new SpeechTask(port,instance,audioChunks,request), NUM_SECONDS,NUM_SECONDS);
				}
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e);
			}
		}
	}
	
	  class SpeechTask extends TimerTask {
		  	private int port;
		  	private int instance;
		  	private int counter=0;
		  	private List<byte[]> audioChunks=new ArrayList<byte[]>();
		  	private SpeechRecognitionRequestVO req= new SpeechRecognitionRequestVO();
		  	
		  	 public SpeechTask(int port, int instance, List<byte[]> ac, SpeechRecognitionRequestVO request) {
		         this.port = port;
		         this.instance=instance;
		         this.audioChunks=ac;
		         this.req=request;
		     }
		  
		    public void run() {
		    	if (counter<audioChunks.size()) {
		    		int aux=counter+1;
		    		System.out.println("Sending chunk: " + aux);
		    		System.out.println("the chunk is " + audioChunks.get(counter).length + " bytes long");
		    		this.req.setData(this.audioChunks.get(counter));
		    		try {
		    			System.out.println("URL= "+"http://193.61.29.166:"+Integer.toString(this.port)+"/italk2learnsm/speechRecognition/sendData"+" INSTANCE= "+this.req.getInstance()+" USER="+this.req.getHeaderVO().getLoginUser());
		    			liveResponse=restTemplate.postForObject("http://193.61.29.166:"+Integer.toString(this.port)+"/italk2learnsm/speechRecognition/sendData", this.req, SpeechRecognitionResponseVO.class);
		    		} catch (Exception e) {
						e.printStackTrace();
						LOGGER.error(e);
					}
		    		counter++;
			    }
		    	else {
		    		String response=restTemplate.getForObject("http://193.61.29.166:"+Integer.toString(this.port)+"/italk2learnsm/speechRecognition/closeEngine?instance={instance}",String.class, Integer.toString(this.instance));
		    		if (response!=null){
		    			System.out.println("INSTANCE= "+Integer.toString(this.instance)+" OUTPUT= "+response);
		    			Assert.assertTrue(true);
		    		}
		    		srUsed++;
		    		this.cancel();
		    	}
		    }

			public int getPort() {
				return port;
			}

			public void setPort(int port) {
				this.port = port;
			}

			public int getInstance() {
				return instance;
			}

			public void setInstance(int instance) {
				this.instance = instance;
			}
		  }
	

}
