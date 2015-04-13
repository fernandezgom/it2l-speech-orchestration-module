package com.italk2learn.vo;

public class AudioResponseVO extends ResponseVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String type;
	
	private byte[] audio;
	
	private byte[] audioExercise;

	public byte[] getAudio() {
		return audio;
	}

	public void setAudio(byte[] audio) {
		this.audio = audio;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public byte[] getAudioExercise() {
		return audioExercise;
	}

	public void setAudioExercise(byte[] audioExercise) {
		this.audioExercise = audioExercise;
	}

}
