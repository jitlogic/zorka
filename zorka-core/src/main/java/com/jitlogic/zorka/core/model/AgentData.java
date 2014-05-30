package com.jitlogic.zorka.core.model;

import java.util.ArrayList;


public class AgentData {

	private String request;
	private ArrayList<Data> data;
	private long clock;

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public ArrayList<Data> getData() {
		return data;
	}

	public void setData(ArrayList<Data> data) {
		this.data = data;
	}
	
	public long getClock() {
		return clock;
	}
	
	public void setClock(long clock) {
		this.clock = clock;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder(100);
		stringBuilder.append("{request=");
		stringBuilder.append(request);
		stringBuilder.append(", data=[");
		
		for (Data dataItem : data){
			stringBuilder.append(dataItem);
		}
		stringBuilder.append("], clock=");
		stringBuilder.append(clock);
		stringBuilder.append("}");
		
		return stringBuilder.toString();
	}
}
