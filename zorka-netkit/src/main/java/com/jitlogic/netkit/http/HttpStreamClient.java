package com.jitlogic.netkit.http;

import com.jitlogic.netkit.NetException;
import com.jitlogic.netkit.util.BufStreamOutput;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.regex.Matcher;

/**
 * HTTP client using traditional
 */
public class HttpStreamClient implements HttpMessageListener {

    private int port;
    private boolean tls;
    private InetAddress host;

    private Socket socket;
    private String baseUri;
    private HttpConfig config;

    private SocketFactory socketFactory;

    private BufStreamOutput streamOutput;
    private HttpMessageHandler output;
    private HttpStreamInput input;

    private HttpMessage result;

    public HttpStreamClient(HttpConfig config, String baseUrl) {
        this.config = config;

        Matcher m = HttpProtocol.RE_URL.matcher(baseUrl);
        if (!m.matches()) throw new NetException("Invalid URL: " + baseUrl);

        this.tls = "https".equalsIgnoreCase(m.group(1));
        this.port = (m.group(3) != null) ? Integer.parseInt(m.group(3).substring(1)) : tls ? 443 : 80;
        try {
            this.host = InetAddress.getByName(m.group(2));
        } catch (UnknownHostException e) {
            throw new NetException("Error resolving host: " + m.group(2));
        }

        this.baseUri = m.group(4) != null ? m.group(4) : "/";

        if (!tls) {
            socketFactory = SocketFactory.getDefault();
        } else {
            throw new NetException("TLS not implemented (yet).");
        }
    }

    public HttpMessage exec(HttpMessage in) {

        if (socket == null || !socket.isConnected()) {
            connect();
        }

        output.submit(new HttpEncoder(config, streamOutput), null, in);
        input.run();

        return result;
    }

    private void connect() {
        try {
            socket = socketFactory.createSocket(host, port);
            input = new HttpStreamInput(config, this, HttpDecoderState.READ_RESP_LINE, socket.getInputStream());
            streamOutput = new BufStreamOutput(socket.getOutputStream());
            output = new HttpMessageHandler(config, null);
        } catch (IOException e) {
            throw new NetException("Cannot connect to " + host + ":" + port, e);
        }
    }

    @Override
    public void submit(SelectionKey key, HttpMessage message) {
        this.result = message;
    }
}
