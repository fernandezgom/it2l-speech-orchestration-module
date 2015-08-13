package com.italk2learn.bo.inter;

import java.util.concurrent.Future;

import com.italk2learn.exception.ITalk2LearnException;
import com.italk2learn.vo.AudioResponseVO;
import com.italk2learn.vo.SpeechRecognitionRequestVO;
import com.italk2learn.vo.SpeechRecognitionResponseVO;

public interface ISpeechRecognitionBO {
	
	public Future<SpeechRecognitionResponseVO> sendNewAudioChunk(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO initASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO closeASREngine(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public SpeechRecognitionResponseVO saveByteArray(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO concatenateAudioStream(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO getCurrentAudioFromPlatform(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO getCurrentAudioFromExercise(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
	public AudioResponseVO createAudioFile(SpeechRecognitionRequestVO request) throws ITalk2LearnException;
	
}
