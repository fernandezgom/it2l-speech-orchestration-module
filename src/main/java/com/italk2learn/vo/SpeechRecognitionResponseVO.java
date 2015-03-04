package com.italk2learn.vo;

import java.util.List;


public class SpeechRecognitionResponseVO extends ResponseVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String response;
	
	private boolean isOpen=false;
	
	private List<String> liveResponse;

	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public List<String> getLiveResponse() {
		return liveResponse;
	}

	public void setLiveResponse(List<String> liveResponse) {
		this.liveResponse = liveResponse;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public SpeechRecognitionResponseVO(String response) {
		super();
		this.response = response;
	}
	
	public SpeechRecognitionResponseVO() {
		super();
	}

}
