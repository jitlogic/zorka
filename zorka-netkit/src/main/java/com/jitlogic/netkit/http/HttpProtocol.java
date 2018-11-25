package com.jitlogic.netkit.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various constants, regexps and common functions defining HTTP protocol.
 */
public class HttpProtocol {

    // RFC-2616 2.2 - basic syntax rules
    public static final String CHAR     = "[\0-\127]";
    public static final String UPALPHA  = "[A-Z]";
    public static final String LOALPHA  = "[a-z]";
    public static final String ALPHA    = "[A-Za-z]";
    public static final String DIGIT    = "\\d";
    public static final String CTL      = "[\\0-\\1f]";
    public static final String CR       = "\r";
    public static final String LF       = "\n";
    public static final String SP       = " ";
    public static final String HT       = "\t";
    public static final String QUOT     = "\"";
    public static final String CRLF     = "\r\n";
    public static final String LWS      = "[\r\n\t ]";
    public static final String TEXT     = "[\32-\127\r\n\t]";
    public static final String HEX      = "[a-fA-F0-9]";
    public static final String SEP      = "[()<>@,;:/?={}\\[\\] \t\\\\\"]";
    public static final String TOKEN    = "[A-Za-z0-9_\\-!#$%^&*~`+'|.]+";
    public static final String QUOTED   = "\"(?:[\t\r\n !#-\127]|\\\\\")*\"";
    public static final String COMMENT  = "\\((?:[\t\r\n -'*-\127]|" + QUOTED +")\\)"; // we don't support recursive comments
    public static final String KEY_VAL  = "(" + TOKEN + ")=(" + TOKEN + "|" + QUOTED + ")";
    public static final String KEY_VALS = "(?:\\s*;\\s*" + KEY_VAL + ")*";

    // RFC-2616 3. - protocol parameters

    // HTTP protocol versions
    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";
    public static final String HTTP_VERSION = "HTTP/1\\.[01]";

    // URLs and URIs
    public static final String IPV4_SEG  = "(?:0|1\\d{0,2}|2(?:[0-4]\\d*|5[0-5]?|[6-9])?|[3-9]\\d?)";
    public static final String IPV4_ADDR = IPV4_SEG + "(?:\\." + IPV4_SEG + "){3}";
    public static final Pattern RE_IPV4_ADDR = Pattern.compile(IPV4_ADDR);

    public static final String URI_PROTO = "[hH][tT][tT][pP][sS]?";
    public static final String URI_PORT  = "[1-9]\\d{0,4}";

    public static final String URI_ESC   = "%" + HEX + "{2}";
    public static final String URI_HSEG  = "(?:[0-9A-Za-z_!~*`\\-]|" + URI_ESC + ")+";
    public static final String URI_HOST  = URI_HSEG + "(?:\\."+ URI_HSEG +")*";
    public static final String URI_PATH  = "/(?:[0-9A-Za-z_!~*`/#:^$\\-.]|" + URI_ESC + ")*";
    public static final String URI_QSTR  = "\\?[0-9A-Za-z_!~*`/\\-&=\\[\\]]*";

    public static final String URL = "(" + URI_PROTO + ")://(" + IPV4_ADDR + "|" + URI_HOST + "|" + ")"
            + "(:" + URI_PORT + ")?(" + URI_PATH + ")?(" + URI_QSTR + ")?";
    public static final Pattern RE_URL = Pattern.compile(URL);

    // rfc2616 3.5 - Content Codings
    public static final String ENCODING  = "gzip|compress|deflate|identity";

    // rfc2616 3.6 - Transfer Coding
    public static final String TRANSFER  = "(?:(chunked)|(" + TOKEN + ")" + KEY_VALS + ")";
    public static final Pattern RE_TRANSFER = Pattern.compile(TRANSFER);

    public static final String CHUNK     = "(" + HEX + "+)" + KEY_VALS;
    public static final Pattern RE_CHUNK = Pattern.compile(CHUNK);

    // rfc2616 3.7 - Media Types
    public static final String MEDIA_TYPE = "(" + TOKEN + ")/(" + TOKEN + ")" + KEY_VALS;
    public static final Pattern RE_MEDIA_TYPE = Pattern.compile(MEDIA_TYPE);

    // rfc2616 3.8 - Product Tokens
    public static final String PRODUCT   = "(" + TOKEN + ")/(" + TOKEN + ")";

    // rfc2616 3.9 - Quality Value
    public static final String QVAL      = "(0\\." + DIGIT + "{3}|1.000)";

    // rfc2616 3.10 - Language Tags
    public static final String LANG_TAG  = "(" + ALPHA + "+(?:-" + ALPHA + "+)*)";
    public static final Pattern RE_LANG_TAG = Pattern.compile(LANG_TAG);

    // rfc2616 3.11 Entity Tags
    public static final String ENTITY_TAG = "(W/)?(" + QUOTED + ")";
    public static final Pattern RE_ENTITY_TAG = Pattern.compile(ENTITY_TAG);


    // rfc2616 4. HTTP Message

    // rfc 2616 4.2 HTTP Header
    // TODO parse comma-separated headers here
    public static final String HEADER_LINE = "(" + TOKEN + "):" + LWS + "*(" + QUOTED + "|" + TEXT + ")*" + LWS + "*";
    public static final Pattern RE_HEADER_LINE = Pattern.compile(HEADER_LINE);

    public static final Pattern RE_HEADER = Pattern.compile("\\s*(" + TOKEN + "):\\s*(\\S.*)\\s*"); // TODO get rid of this


    // TODO implement way to limit available methods early on message decoding stage
    public static final String HTTP_METHOD = "GET|HEAD|POST|PUT|DELETE|TRACE|OPTIONS|CONNECT";

    public static final String REQ_LINE = "(" + HTTP_METHOD + ")" + "\\s+" +
            "(\\*|" + URI_PATH + ")(" + URI_QSTR + ")?"  + "\\s+(" + HTTP_VERSION + ")";

    public static final String RESP_LINE = "\\s*(" + HTTP_VERSION + ")\\s+([12345]\\d{2})\\s*(.+)?";

    public static final Pattern RE_REQ_LINE = Pattern.compile(REQ_LINE);



    public static final Pattern RE_RESP_LINE = Pattern.compile(RESP_LINE);



    /**
     * The PROXY protocol header is quite strict in what it allows, specifying for example that only
     * a single space character (\x20) is allowed between components.
     */
    public static final Pattern RE_PROXY_LINE = Pattern.compile(
            "PROXY TCP4 (" + IPV4_ADDR + ") (" + IPV4_ADDR +") (" + URI_PORT + ") (" + URI_PORT + ")");


    public static final String H_ACCEPT              = "Accept";              // 14.1
    public static final String H_ACCEPT_CHARSET      = "Accept-Charset";      // 14.2
    public static final String H_ACCEPT_ENCODING     = "Accept-Encoding";     // 14.3
    public static final String H_ACCEPT_LANGUAGE     = "Accept-Language";     // 14.4
    public static final String H_ACCEPT_RANGES       = "Accept-Ranges";       // 14.5
    public static final String H_AGE                 = "Age";                 // 14.6
    public static final String H_ALLOW               = "Allow";               // 14.7
    public static final String H_AUTHORIZATION       = "Authorization";       // 14.8
    public static final String H_CACHE_CONTROL       = "Cache-Control";       // 14.9
    public static final String H_CONNECTION          = "Connection";          // 14.10
    public static final String H_CONTENT_ENCODING    = "Content-Encoding";    // 14.11
    public static final String H_CONTENT_LANGUAGE    = "Content-Language";    // 14.12
    public static final String H_CONTENT_LENGTH      = "Content-Length";      // 14.13
    public static final String H_CONTENT_LOCATION    = "Content-Location";    // 14.14
    public static final String H_CONTENT_MD5         = "Content-Md5";         // 14.15
    public static final String H_CONTENT_RANGE       = "Content-Range";       // 14.16
    public static final String H_CONTENT_TYPE        = "Content-Type";        // 14.17
    public static final String H_DATE                = "Date";                // 14.18
    public static final String H_ETAG                = "Etag";                // 14.19
    public static final String H_EXPECT              = "Expect";              // 14.20
    public static final String H_EXPIRES             = "Expires";             // 14.21
    public static final String H_FROM                = "From";                // 14.22
    public static final String H_HOST                = "Host";                // 14.23
    public static final String H_IF_MATCH            = "If-Match";            // 14.24
    public static final String H_IF_MODIFIED_SINCE   = "If-Modified-Since";   // 14.25
    public static final String H_IF_NONE_MATCH       = "If-None-Match";       // 14.26
    public static final String H_IF_RANGE            = "If-Range";            // 14.27
    public static final String H_IF_UNMODIFIED_SINCE = "If-Unmodified-Since"; // 14.28
    public static final String H_LAST_MODIFIED       = "Last-Modified";       // 14.29
    public static final String H_LOCATION            = "Location";            // 14.30
    public static final String H_MAX_FORWARDS        = "Max-Forwards";        // 14.31
    public static final String H_PRAGMA              = "Pragma";              // 14.32
    public static final String H_PROXY_AUTHENTICATE  = "Proxy-Authenticate";  // 14.33
    public static final String H_PROXY_AUTHORIZATION = "Proxy-Authorization"; // 14.34
    public static final String H_RANGE               = "Range";               // 14.35
    public static final String H_REFERER             = "Referer";             // 14.36
    public static final String H_RETRY_AFTER         = "Retry-After";         // 14.37
    public static final String H_SERVER              = "Server";              // 14.38
    public static final String H_TE                  = "Te";                  // 14.39
    public static final String H_TRAILER             = "Trailer";             // 14.40
    public static final String H_TRANSFER_ENCODING   = "Transfer-Encoding";   // 14.41
    public static final String H_UPGRADE             = "Upgrade";             // 14.42
    public static final String H_USER_AGENT          = "User-Agent";          // 14.43
    public static final String H_VARY                = "Vary";                // 14.44
    public static final String H_VIA                 = "Via";                 // 14.45
    public static final String H_WARNING             = "Warning";             // 14.46
    public static final String H_WWW_AUTHENTICATE    = "Www-Authenticate";    // 14.47

    public static final String H_KEEP_ALIVE          = "Keep-Alive";

    public static final String H_X_FORWARDED_FOR    = "X-Forwarded-For";
    public static final String H_X_FORWARDED_PORT   = "X-Forwarded-Port";
    public static final String H_X_FORWARDED_PROTO  = "X-Forwarded-Proto";


    public static boolean emptyBodyExpected(int status) {
        return status / 100 == 1 || status == 204 || status == 304;
    }

    private static ThreadLocal<SimpleDateFormat> DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        protected SimpleDateFormat initialValue() {
            // Formats into HTTP date format (RFC 822/1123).
            SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            return f;
        }
    };

    public static String dateFormat(Date date) {
        return DATE_FORMATTER.get().format(date);
    }

    public static String dateFormat() {
        return DATE_FORMATTER.get().format(new Date());
    }

    public static boolean urlToSsl(String url) {
        Matcher m = HttpProtocol.RE_URL.matcher(url != null ? url : "");
        return m.matches() && "https".equalsIgnoreCase(m.group(1));
    }


    public static String urlToAddr(String url) {
        Matcher m = HttpProtocol.RE_URL.matcher(url != null ? url : "");
        if (m.matches()) {
            return m.group(2);
        }
        return null;
    }


    public static int urlToPort(String url) {
        Matcher m = HttpProtocol.RE_URL.matcher(url != null ? url : "");
        if (m.matches()) {
            String g3 = m.group(3);
            if (g3 != null) return Integer.parseInt(g3.substring(1));
            return "http".equalsIgnoreCase(m.group(1)) ? 80 : 443;
        }
        return -1;
    }


    public static String urlToPath(String url) {
        Matcher m = HttpProtocol.RE_URL.matcher(url != null ? url : "");
        if (m.matches()) {
            String g4 = m.group(4);
            return g4 != null && !g4.isEmpty() ? g4 : "/";
        }
        return null;
    }

}
