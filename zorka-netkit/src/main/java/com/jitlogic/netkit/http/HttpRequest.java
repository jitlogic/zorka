package com.jitlogic.netkit.http;

import com.jitlogic.netkit.BufDecoder;
import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetException;
import com.jitlogic.netkit.NetRequest;
import com.jitlogic.netkit.util.TextUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.regex.Matcher;

public class HttpRequest extends NetRequest {

    public static final Object BODY = new Object();

    private String uri;
    private String query;
    private HttpMethod method;

    private String[] headers;
    private int headersPos = 0;

    private Object bodyData;

    private HttpConfig config;
    private BufDecoder decoder;
    private HttpListener listener;

    public HttpRequest(HttpConfig config, HttpListener listener, String url) {
        this(config, listener, url, HttpMethod.GET);
    }

    public HttpRequest(HttpConfig config, HttpListener listener, String url, HttpMethod method, Object...args) {
        super(HttpProtocol.urlToAddr(url), HttpProtocol.urlToPort(url));

        this.config = config;
        this.listener = listener;
        this.decoder = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, listener);
        this.method = method;

        Matcher m = HttpProtocol.RE_URL.matcher(url);
        if (m.matches()) {
            uri = m.group(4);
            query = m.group(5);
        } else {
            throw new NetException("Unparsable URL: " + url);
        }

        if (uri == null) {
            uri = "/";
        }

        parseArgs(args);
    }

    public HttpRequest header(String name, String value) {
        if (headers == null) {
            headers = new String[12];
        } else if (headers.length >= headersPos) {
            headers = Arrays.copyOf(headers, headers.length+16);
        }
        headers[headersPos] = name;
        headers[headersPos+1] = value;
        headersPos += 2;
        return this;
    }

    private void parseArgs(Object[] args) {
        boolean hdrproc = true;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == BODY) {
                hdrproc = false;
            } else if (hdrproc) {
                if (i < args.length-1) {
                    header(TextUtil.camelCase((String)args[i]), args[i+1].toString());
                    i += 1;
                }
            } else {
                // TODO handle more than one body part (if possible)
                bodyData = arg;
            }
        }
    }

    @Override
    public void sendRequest(SelectionKey key, BufHandler output) {
        HttpEncoder encoder = new HttpEncoder(config, output);
        encoder.request(key, HttpProtocol.HTTP_1_1,  method, uri, query);
        for (int i = 0; i < headersPos; i+=2) {
            encoder.header(key, headers[i], headers[i+1]);
        }
        encoder.finish(key);
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        return decoder.submit(key, copyOnSchedule, buffers);
    }
}
