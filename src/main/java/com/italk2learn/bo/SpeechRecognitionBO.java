package com.italk2learn.bo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.italk2learn.bo.inter.ISpeechRecognitionBO;
import com.italk2learn.dao.inter.IAudioStreamDAO;
import com.italk2learn.exception.ITalk2LearnException;
import com.italk2learn.speech.util.EnginesMap;
import com.italk2learn.vo.ASRInstanceVO;
import com.italk2learn.vo.AudioRequestVO;
import com.italk2learn.vo.AudioResponseVO;
import com.italk2learn.vo.SpeechRecognitionRequestVO;
import com.italk2learn.vo.SpeechRecognitionResponseVO;

@Service("speechRecognitionBO")
@Transactional(rollbackFor = { ITalk2LearnException.class, ITalk2LearnException.class })
public class SpeechRecognitionBO implements ISpeechRecognitionBO {
	
	private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionBO.class);
	private static final int NUM_SECONDS = 30 * 1000;
	
	@Autowired
	private RestTemplate restTemplate;
	
	public IAudioStreamDAO audioStreamDAO;
	
	private byte[] audio=new byte[0];
	//Store all audio in current exercise
	private byte[] audioExercise=new byte[0];
	
	private EnginesMap em= EnginesMap.getInstance();
	
	private int SEQ=0;
	
	
	//524288 each 5 seconds
	//12 times per minute
	private static final int SIZE_AUDIO_1MINUTES = 1 * 12 * 524288;
	
	private static final int SIZE_AUDIO_10MINUTES = 10 * 12 * 524288;
	
	
	@Autowired
	public SpeechRecognitionBO(IAudioStreamDAO audioStreamDAO) {
		this.audioStreamDAO = audioStreamDAO;
	}

	/*
	 * Call http service to init the ASR Engine
	 */
	public SpeechRecognitionResponseVO initASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException{
		logger.info("JLF SpeechRecognitionBO initASREngine()--- Initialising ASREngine instance by user="+request.getHeaderVO().getLoginUser());
		cleanAllAudioVariables();
		Timer timer = new Timer();
		//JLF Testing each 30 seconds if the speech component is sending data to the server, otherwise close the connection
		timer.scheduleAtFixedRate(new SpeechRecoTask(request), NUM_SECONDS,NUM_SECONDS);
		SpeechRecognitionResponseVO res=new SpeechRecognitionResponseVO();
		try {
			Map<String, String> vars = new HashMap<String, String>();
			//Get an available instance
			if (em.getInstanceByUser(request.getHeaderVO().getLoginUser())==null){
				ASRInstanceVO aux= em.getInstanceEngineAvailable(request.getHeaderVO().getLoginUser());
				logger.info("Speech module available for user: "+request.getHeaderVO().getLoginUser()+" with instance: "+aux.getInstance().toString() );
				System.out.println("Speech module available for user: "+request.getHeaderVO().getLoginUser()+" with instance: "+aux.getInstance().toString());
				vars.put("user", request.getHeaderVO().getLoginUser());
				vars.put("instance", aux.getInstance().toString());
				vars.put("server", aux.getServer());
				vars.put("language", aux.getLanguageCode());
				vars.put("model", aux.getModel());
				//Call initEngineService of an available instance
				Boolean resp=this.restTemplate.getForObject(aux.getUrl() + "/initEngine?user={user}&instance={instance}&server={server}&language={language}&model={model}",Boolean.class, vars);
				res.setOpen(resp);
				return res;
			}
			else {
				res.setOpen(true);
				return res;
			}
		} catch (Exception e) {
			em.releaseEngineInstance(request.getHeaderVO().getLoginUser());
			logger.error(e.toString());
		}
		return res;	
	}
	
	/*
	 * Call http service to close the engine and it receives the final transcription
	 */
	public SpeechRecognitionResponseVO closeASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException{
		logger.info("JLF SpeechRecognitionBO closeASREngine() --- Closing ASREngine instance by user="+request.getHeaderVO().getLoginUser());
		cleanAllAudioVariables();
		SpeechRecognitionResponseVO res=new SpeechRecognitionResponseVO();
		String url=em.getUrlByUser(request.getHeaderVO().getLoginUser());
		Integer instanceNum=em.getInstanceByUser(request.getHeaderVO().getLoginUser());
		if (instanceNum==null){
			System.out.println("Instance already released by user="+ request.getHeaderVO().getLoginUser()+" or never used");
			logger.info("closeASREngine()--- Instance already released by user="+ request.getHeaderVO().getLoginUser()+" or never used");
			return res;
		}
		em.releaseEngineInstance(request.getHeaderVO().getLoginUser());
		request.setInstance(instanceNum);
		try {
			System.out.println("Speech module released by user: "+request.getHeaderVO().getLoginUser()+" with instance: "+instanceNum.toString());
			logger.info("Speech module released by user: "+request.getHeaderVO().getLoginUser()+" with instance: "+instanceNum.toString());
			String response=this.restTemplate.getForObject(url + "/closeEngine?instance={instance}",String.class, instanceNum.toString());
			res.setResponse(response);
			return res;
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return res;
	}
	
	/*
	 * Call http service to send audio chunks
	 */
	public SpeechRecognitionResponseVO sendNewAudioChunk(SpeechRecognitionRequestVO request) throws ITalk2LearnException{
		SEQ++;
		logger.info("JLF SpeechRecognitionBO sendNewAudioChunk()--- Sending new audio chunk by user="+request.getHeaderVO().getLoginUser());
		SpeechRecognitionResponseVO res=new SpeechRecognitionResponseVO();
		request.setInstance(em.getInstanceByUser(request.getHeaderVO().getLoginUser()));
		try {
			res=this.restTemplate.postForObject(em.getUrlByUser(request.getHeaderVO().getLoginUser())+"/sendData", request, SpeechRecognitionResponseVO.class);
			for (int i=0;i<res.getLiveResponse().size();i++)
				logger.info("liveResponse word="+ res.getLiveResponse().get(i));
		} catch (Exception e) {
			em.releaseEngineInstance(request.getHeaderVO().getLoginUser());
			logger.error(e.toString());
		}
		return res;
	}
	
	public SpeechRecognitionResponseVO saveByteArray(SpeechRecognitionRequestVO request) throws ITalk2LearnException {
		logger.info("JLF SpeechRecognitionBO saveByteArray()--- Saving sound ByteArray on the database by user="+request.getHeaderVO().getLoginUser());
		SpeechRecognitionResponseVO response= new SpeechRecognitionResponseVO();
		try {
			getAudioStreamDAO().saveByteArray(request.getFinalByteArray(), request.getHeaderVO().getIdUser());
		}
		catch (Exception e){
			logger.error(e.toString());
		}
		return response;
	}
	
	public AudioResponseVO concatenateAudioStream(AudioRequestVO request) throws ITalk2LearnException {
		logger.info("JLF --- Concatenating audio chunk which it comes each 5 seconds from the audio component");
		AudioResponseVO response= new AudioResponseVO();
		try {
			//JLF:Copying byte array
			byte[] destination = new byte[request.getAudio().length + getAudio().length];
			// copy audio into start of destination (from pos 0, copy audio.length bytes)
			System.arraycopy(getAudio(), 0, destination, 0, getAudio().length);
			// copy body into end of destination (from pos audio.length, copy body.length bytes)
			System.arraycopy(request.getAudio(), 0, destination, getAudio().length, request.getAudio().length);
			//setAudio(Arrays.copyOfRange(destination, 0, destination.length));
			this.audio=destination.clone();
			this.audioExercise=destination.clone();
		}
		catch (Exception e){
			logger.error(e.toString());
		}
		return response;
	}
	
	// JLF: Get the current stored audio, 2 minutes
	public AudioResponseVO getCurrentAudioFromPlatform(AudioRequestVO request) throws ITalk2LearnException {
		logger.info("JLF --- getCurrentAudioFromPlatform-- Get current audio to use on TIS current_audio_length= "+this.audio.length);
		AudioResponseVO response= new AudioResponseVO();
		try {
			//JLF:The audio should be more than 2 minutes for analysis
			if (this.audio.length>SIZE_AUDIO_1MINUTES) {
				int init=this.audio.length-SIZE_AUDIO_1MINUTES;
				if ((init % 16)!=0)
					init=init-(init % 16);
				byte[] destination = Arrays.copyOfRange(this.audio, init, this.audio.length);
				this.audio=destination.clone();
			}
			response.setAudio(this.audio);
		}
		catch (Exception e){
			logger.error(e.toString());
		}
		return response;
	}
	
	// JLF: Get whole audio from exercise, 10 minutes
	public AudioResponseVO getCurrentAudioFromExercise(AudioRequestVO request) throws ITalk2LearnException {
		logger.info("JLF --- getCurrentAudioFromExercise-- Get current audio to use on SNA current_audio_length= "+this.audioExercise.length);
		AudioResponseVO response= new AudioResponseVO();
		try {
			if (this.audioExercise.length>SIZE_AUDIO_10MINUTES) {
				int init=this.audioExercise.length-SIZE_AUDIO_10MINUTES;
				if ((init % 16)!=0)
					init=init-(init % 16);
				byte[] destination = Arrays.copyOfRange(this.audioExercise, init, this.audioExercise.length);
				this.audioExercise=destination.clone();
			}
			response.setAudioExercise(this.audioExercise);
		}
		catch (Exception e){
			logger.error(e.toString());
		}
		return response;
	}
	
	public void cleanAllAudioVariables() throws ITalk2LearnException {
		this.audio=new byte[0];
		this.audioExercise=new byte[0];
	}
	
	public EnginesMap getEm() {
		return em;
	}

	public void setEm(EnginesMap em) {
		this.em = em;
	}
	
	public IAudioStreamDAO getAudioStreamDAO() {
		return audioStreamDAO;
	}

	public void setAudioStreamDAO(IAudioStreamDAO audioStreamDAO) {
		this.audioStreamDAO = audioStreamDAO;
	}

	public byte[] getAudio() {
		return audio;
	}

	public void setAudio(byte[] audio) {
		this.audio = audio;
	}
	
	
	public byte[] getAudioExercise() {
		return audioExercise;
	}

	public void setAudioExercise(byte[] audioExercise) {
		this.audioExercise = audioExercise;
	}


	//JLF: This class check if speech component is sending data to the Speech Reco, if not it closes the instance with the engine and release this 
	class SpeechRecoTask extends TimerTask {
	    
		private int SEQ_ACK=0;
		private SpeechRecognitionRequestVO request;
		
		public SpeechRecoTask(SpeechRecognitionRequestVO request) {
			super();
			this.request = request;
		}

		public void run() {
	    	if (SEQ==SEQ_ACK) {
	    		try {
					closeASREngine(request);
					this.cancel();
				} catch (ITalk2LearnException e) {
					// TODO Auto-generated catch block
					logger.error(e.toString());
				}
		    }
	    	else {
	    		SEQ_ACK=SEQ;
	    	}
	    }
	}

}
