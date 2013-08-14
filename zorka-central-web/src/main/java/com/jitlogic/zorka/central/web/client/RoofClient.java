/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.central.web.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.HashMap;
import java.util.Map;

public class RoofClient<T extends JavaScriptObject> {


    private String url;


    public RoofClient(String path) {
        url = GWT.getHostPageBaseURL() + path;
    }


    public String getUrl() {
        return url;
    }


    public void list(String path, final AsyncCallback<JsArray<T>> callback) {
        list(path, new HashMap<String, String>(), callback);
    }


    public void list(String path, Map<String,String> params, final AsyncCallback<JsArray<T>> callback) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String,String> e : params.entrySet()) {
            sb.append(sb.length() == 0 ? "?" : "&");
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
        }

        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, (path != null ?  url + "/" + path : url) + sb);
        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        callback.onSuccess((JsArray<T>)JsonUtils.safeEval(response.getText()));
                    } else {
                        callback.onFailure(new RequestException("HTTP error "
                                + response.getStatusCode() + ": " + response.getStatusText()));
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }


    public void call(String entity, String method, final AsyncCallback<String> callback) {
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url + "/" + entity + "/actions/" + method);
        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        callback.onSuccess(response.getText());
                    } else {
                        callback.onFailure(new RequestException("HTTP error "
                                + response.getStatusCode() + ": " + response.getStatusText()));
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        } catch (RequestException e) {
            callback.onFailure(e);
        }

    }


    public void get(String entity, Object id, final AsyncCallback<T> callback) {
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url + "/" + entity + "/" + id);
        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        callback.onSuccess((T)JsonUtils.safeEval(response.getText()));
                    } else {
                        callback.onFailure(new RequestException("HTTP error "
                                + response.getStatusCode() + ": " + response.getStatusText()));
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }

}
