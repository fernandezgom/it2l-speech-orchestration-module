package com.italk2learn.bo.inter;

import com.italk2learn.exception.ITalk2LearnException;
import com.italk2learn.vo.AudioRequestVO;
import com.italk2learn.vo.AudioResponseVO;
import com.italk2learn.vo.SpeechRecognitionRequestVO;
import com.italk2learn.vo.SpeechRecognitionResponseVO;

public interface ISpeechRecognitionBO {
	
	public SpeechRecognitionResponseVO sendNewAudioChunk(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO initASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO closeASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO saveByteArray(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO concatenateAudioStream(AudioRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO getCurrentAudioFromPlatform(AudioRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO getCurrentAudioFromExercise(AudioRequestVO request) throws ITalk2LearnException;
	
}
