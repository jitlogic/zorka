package com.jitlogic.zorka.net.http.nano;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2016 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLException;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.net.http.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPSession implements IHTTPSession {

    private static Logger log = LoggerFactory.getLogger(HTTPSession.class);

    public static final String POST_DATA = "postData";

    private static final int REQUEST_BUFFER_LEN = 512;

    private static final int MEMORY_STORE_LIMIT = 64 * 1024;

    public static final int BUFSIZE = 8192;

    public static final int MAX_HEADER_SIZE = 8192;

    private final OutputStream outputStream;

    private final BufferedInputStream inputStream;

    private int splitbyte;

    private int rlen;

    private String uri;

    private RequestMethod method;

    private Map<String, List<String>> parms;

    private Map<String, String> headers;

    private CookieHandler cookies;

    private String queryParameterString;

    private String remoteIp;

    private String remoteHostname;

    private String protocolVersion;

    private IHandler<IHTTPSession, Response> handler;

    public HTTPSession(IHandler<IHTTPSession, Response> handler, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
        this.handler = handler;
        this.inputStream = new BufferedInputStream(inputStream, HTTPSession.BUFSIZE);
        this.outputStream = outputStream;
        this.remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress();
        this.remoteHostname = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "localhost" : inetAddress.getHostName();
        this.headers = new HashMap<String, String>();
    }

    /**
     * Decodes the sent headers and loads the data into Key/value pairs
     */
    private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, List<String>> parms, Map<String, String> headers) throws ResponseException {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
            }

            pre.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = HttpUtils.urlDecode(uri.substring(0, qmi));
            } else {
                uri = HttpUtils.urlDecode(uri);
            }

            // If there's another token, its protocol version,
            // followed by HTTP headers.
            // NOTE: this now forces header names lower case since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                protocolVersion = st.nextToken();
            } else {
                protocolVersion = "HTTP/1.1";
                log.debug("no protocol version specified, strange. Assuming HTTP/1.1.");
            }
            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            throw new ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
        }
    }


    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
     */
    private void decodeParms(String parms, Map<String, List<String>> p) {
        if (parms == null) {
            this.queryParameterString = "";
            return;
        }

        this.queryParameterString = parms;
        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            String key = null;
            String value = null;

            if (sep >= 0) {
                key = HttpUtils.urlDecode(e.substring(0, sep)).trim();
                value = HttpUtils.urlDecode(e.substring(sep + 1));
            } else {
                key = HttpUtils.urlDecode(e).trim();
                value = "";
            }

            List<String> values = p.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                p.put(key, values);
            }

            values.add(value);
        }
    }

    @Override
    public void execute() throws IOException {
        Response r = null;
        try {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header
            // at once!
            byte[] buf = new byte[HTTPSession.BUFSIZE];
            this.splitbyte = 0;
            this.rlen = 0;

            int read = -1;
            this.inputStream.mark(HTTPSession.BUFSIZE);
            try {
                read = this.inputStream.read(buf, 0, HTTPSession.BUFSIZE);
            } catch (SSLException e) {
                throw e;
            } catch (IOException e) {
                ZorkaUtil.close(this.inputStream);
                ZorkaUtil.close(this.outputStream);
                throw new SocketException("NanoHttpd Shutdown");
            }
            if (read == -1) {
                // socket was been closed
                ZorkaUtil.close(this.inputStream);
                ZorkaUtil.close(this.outputStream);
                throw new SocketException("NanoHttpd Shutdown");
            }
            while (read > 0) {
                this.rlen += read;
                this.splitbyte = findHeaderEnd(buf, this.rlen);
                if (this.splitbyte > 0) {
                    break;
                }
                read = this.inputStream.read(buf, this.rlen, HTTPSession.BUFSIZE - this.rlen);
            }

            if (this.splitbyte < this.rlen) {
                this.inputStream.reset();
                this.inputStream.skip(this.splitbyte);
            }

            this.parms = new HashMap<String, List<String>>();
            if (null == this.headers) {
                this.headers = new HashMap<String, String>();
            } else {
                this.headers.clear();
            }

            // Create a BufferedReader for parsing the header.
            BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, this.rlen)));

            // Decode the header into parms and header java properties
            Map<String, String> pre = new HashMap<String, String>();
            decodeHeader(hin, pre, this.parms, this.headers);

            if (null != this.remoteIp) {
                this.headers.put("remote-addr", this.remoteIp);
                this.headers.put("http-client-ip", this.remoteIp);
            }

            this.method = RequestMethod.lookup(pre.get("method"));
            if (this.method == null) {
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. HTTP verb " + pre.get("method") + " unhandled.");
            }

            this.uri = pre.get("uri");

            this.cookies = new CookieHandler(this.headers);

            String connection = this.headers.get("connection");
            boolean keepAlive = "HTTP/1.1".equals(protocolVersion) && (connection == null || !connection.matches("(?i).*close.*"));

            // Ok, now do the serve()

            // TODO: long body_size = getBodySize();
            // TODO: long pos_before_serve = this.inputStream.totalRead()
            // (requires implementation for totalRead())
            r = handler.handle(this);
            // TODO: this.inputStream.skip(body_size -
            // (this.inputStream.totalRead() - pos_before_serve))

            if (r == null) {
                throw new ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
            } else {
                String acceptEncoding = this.headers.get("accept-encoding");
                this.cookies.unloadQueue(r);
                r.setRequestMethod(this.method);
                if (acceptEncoding == null || !acceptEncoding.contains("gzip")) {
                    r.setUseGzip(false);
                }
                r.setKeepAlive(keepAlive);
                r.send(this.outputStream);
            }
            if (!keepAlive || r.isCloseConnection()) {
                throw new SocketException("NanoHttpd Shutdown");
            }
        } catch (SocketException e) {
            // throw it out to close socket object (finalAccept)
            throw e;
        } catch (SocketTimeoutException ste) {
            // treat socket timeouts the same way we treat socket exceptions
            // i.e. close the stream & finalAccept object by throwing the
            // exception up the call stack.
            throw ste;
        } catch (SSLException ssle) {
            Response resp = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, HttpUtils.MIME_PLAINTEXT, "SSL PROTOCOL FAILURE: " + ssle.getMessage());
            resp.send(this.outputStream);
            ZorkaUtil.close(resp);
            ZorkaUtil.close(this.outputStream);
        } catch (IOException ioe) {
            Response resp = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, HttpUtils.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            resp.send(this.outputStream);
            ZorkaUtil.close(resp);
            ZorkaUtil.close(this.outputStream);
        } catch (ResponseException re) {
            Response resp = Response.newFixedLengthResponse(re.getStatus(), HttpUtils.MIME_PLAINTEXT, re.getMessage());
            resp.send(this.outputStream);
            ZorkaUtil.close(resp);
            ZorkaUtil.close(this.outputStream);
        } finally {
            ZorkaUtil.close(r);
        }
    }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     */
    private int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 1 < rlen) {

            // RFC2616
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && splitbyte + 3 < rlen && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }

            // tolerance
            if (buf[splitbyte] == '\n' && buf[splitbyte + 1] == '\n') {
                return splitbyte + 2;
            }
            splitbyte++;
        }
        return 0;
    }

    @Override
    public CookieHandler getCookies() {
        return this.cookies;
    }

    @Override
    public final Map<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public final InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public final RequestMethod getMethod() {
        return this.method;
    }

    @Override
    public final Map<String, List<String>> getParameters() {
        return this.parms;
    }

    @Override
    public String getQueryParameterString() {
        return this.queryParameterString;
    }

    @Override
    public final String getUri() {
        return this.uri;
    }

    /**
     * Deduce body length in bytes. Either from "content-length" header or read
     * bytes.
     */
    public long getBodySize() {
        if (this.headers.containsKey("content-length")) {
            return Long.parseLong(this.headers.get("content-length"));
        } else if (this.splitbyte < this.rlen) {
            return this.rlen - this.splitbyte;
        }
        return 0;
    }

    @Override
    public void parseBody(Map<String, String> files) throws IOException, ResponseException {
            long size = getBodySize();
            ByteArrayOutputStream baos = null;
            DataOutput requestDataOutput = null;

            // Store the request in memory or a file, depending on size
            if (size > MEMORY_STORE_LIMIT) {
                throw new ResponseException(Status.BAD_REQUEST, "request body too big");
            }

            baos = new ByteArrayOutputStream();
            requestDataOutput = new DataOutputStream(baos);

            // Read all the body and write it to request_data_output
            byte[] buf = new byte[REQUEST_BUFFER_LEN];
            while (this.rlen >= 0 && size > 0) {
                this.rlen = this.inputStream.read(buf, 0, (int) Math.min(size, REQUEST_BUFFER_LEN));
                size -= this.rlen;
                if (this.rlen > 0) {
                    requestDataOutput.write(buf, 0, this.rlen);
                }
            }

            ByteBuffer fbuf = ByteBuffer.wrap(baos.toByteArray(), 0, baos.size());

            // If the method is POST, there may be parameters
            // in data section, too, read it:
            if (RequestMethod.POST.equals(this.method)) {
                ContentType contentType = new ContentType(this.headers.get("content-type"));
                if (contentType.isMultipart()) {
                    throw new ResponseException(Status.BAD_REQUEST, "Multipart content type not supported.");
                } else {
                    byte[] postBytes = new byte[fbuf.remaining()];
                    fbuf.get(postBytes);
                    String postLine = new String(postBytes, contentType.getEncoding()).trim();
                    // Handle application/x-www-form-urlencoded
                    if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType.getContentType())) {
                        decodeParms(postLine, this.parms);
                    } else if (postLine.length() != 0) {
                        // Special case for raw POST data => create a
                        // special files entry "postData" with raw content
                        // data
                        files.put(POST_DATA, postLine);
                    }
                }
            } else if (RequestMethod.PUT.equals(this.method)) {
                ContentType contentType = new ContentType(this.headers.get("content-type"));
                byte[] postBytes = new byte[fbuf.remaining()];
                fbuf.get(postBytes);
                String postLine = new String(postBytes, contentType.getEncoding()).trim();
                files.put("content", postLine);
            }
    }


    @Override
    public String getRemoteIpAddress() {
        return this.remoteIp;
    }

    @Override
    public String getRemoteHostName() {
        return this.remoteHostname;
    }
}
