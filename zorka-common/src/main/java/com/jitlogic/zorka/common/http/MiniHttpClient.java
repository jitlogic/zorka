package com.jitlogic.zorka.common.http;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jitlogic.zorka.common.http.HttpUtil.RE_HTTP_URL;

/**
 * Minimal HTTP client library. It is designed to have minimum platform dependences,
 * in particular not to interfere with platform HTTP client.
 */
public class MiniHttpClient implements HttpClient {

    private Pattern RE_HTTP_RSP = Pattern.compile("HTTP/1.[01] (\\d+) (.*)");
    private Pattern RE_HTTP_HDR = Pattern.compile("([^:]+): (.*)");

    @Override
    public HttpResponse execute(HttpRequest req) throws IOException {

        req.setHeader("User-Agent", "Zorka:MiniHttpClient/1.1.1");

        Matcher mu = RE_HTTP_URL.matcher(req.url);

        if (!mu.matches()) {
            throw new IOException("Invalid URL: " + req.url);
        }

        String[] hp = mu.group(2).split(":");

        Socket socket = new Socket(hp[0], hp.length == 2 ? Integer.parseInt(hp[1]) : 80);

        try {
            OutputStream os = socket.getOutputStream();
            PrintWriter out = new PrintWriter(os, true);
            out.write(req.method + " " + mu.group(3) + " HTTP/1.1\r\n");
            for (Map.Entry<String, String> e : req.getHeaders().entrySet()) {
                out.write(e.getKey() + ": " + e.getValue() + "\r\n");
            }
            out.write("Content-Length: " + req.getBody().length + "\r\n");
            out.write("\r\n");
            out.flush();
            os.write(req.getBody());
            os.flush();

            InputStream is = socket.getInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
            HttpResponse resp = new HttpResponse();

            String s = rdr.readLine();
            Matcher ms = RE_HTTP_RSP.matcher(s);
            if (!ms.matches()) {
                throw new IOException("Invalid status line: " + s);
            }

            resp.setStatus(Integer.parseInt(ms.group(1)));
            resp.setStatusMsg(ms.group(2));

            for (s = rdr.readLine(); s != null && !"\r".equals(s) && !"".equals(s); s = rdr.readLine()) {
                Matcher mh = RE_HTTP_HDR.matcher(s);
                if (mh.matches()) {
                    resp.setHeader(mh.group(1).toLowerCase(), mh.group(2));
                }
            }

            String ls = resp.getHeader("content-length");

            int l = ls != null ? Integer.parseInt(ls) : 0;

            if (req.isIgnoreResp() || resp.getStatus() == 204) {
                return resp;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            if (l > 0) {
                for (int i = rdr.read(); i != -1; i = rdr.read()) {
                    bos.write(i);
                    l--;
                    if (l <= 0) break;
                }
            }

            resp.setBody(bos.toByteArray());

            return resp;
        } finally {
            socket.close();
        }
    }
}
