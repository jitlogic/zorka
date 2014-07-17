package com.jitlogic.zorka.common.model;

import java.util.ArrayList;


public class ActiveCheckResponse {

	private String response;
	private ArrayList<ActiveCheckData> data;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public ArrayList<ActiveCheckData> getData() {
		return data;
	}

	public void setData(ArrayList<ActiveCheckData> data) {
		this.data = data;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder(100);
		stringBuilder.append("{response=");
		stringBuilder.append(response);
		stringBuilder.append(", data=[");
		
		int size = data.size();
		if (size > 0) {
			stringBuilder.append(data.get(0));
			for (int i = 1; i < size; i++) {
				stringBuilder.append(", ");
				stringBuilder.append(data.get(i));
			}
		}
		stringBuilder.append("]}");
		
		return stringBuilder.toString();
	}
}
