/*
 * Nicholas Saney
 * 
 * Created: August 14, 2015
 * 
 * LocalHttpExchange.java
 * LocalHttpExchange class definition
 */

package chairosoft.local_server;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class LocalHttpExchange implements Closeable
{
    //
    // Instance Fields
    //
    
    public final HttpExchange exchange;
    public final String REQUEST_METHOD;
    public final URI REQUEST_URI;
    public final String REQUEST_PROTOCOL;
    public final Headers REQUEST_HEADERS;
    public final String REQUEST_BODY;
    public final Headers RESPONSE_HEADERS;
    
    
    //
    // Constructor
    //
    
    public LocalHttpExchange(HttpExchange _exchange)
    {
        this.exchange = _exchange;
        this.REQUEST_METHOD = this.exchange.getRequestMethod();
        this.REQUEST_URI = this.exchange.getRequestURI();
        this.REQUEST_PROTOCOL = this.exchange.getProtocol();
        this.REQUEST_HEADERS = this.exchange.getRequestHeaders();
        this.REQUEST_BODY = LocalServer.getStringFromInputStream(this.exchange.getRequestBody());
        this.RESPONSE_HEADERS = this.exchange.getResponseHeaders();
    }
    
    
    //
    // Instance Methods
    //
    
    @Override public void close() throws IOException { this.exchange.close(); }
    
    /**
     * Get a string representation of this exchange's request.
     * @return a string representation of this exchange's request
     */
    public String getRequestString()
    {
        StringBuilder sb = new StringBuilder();
        String SP = " ";
        String CRLF = "\r\n";
        String KVSEP = ": ";
        sb.append(this.REQUEST_METHOD + SP + this.REQUEST_URI + SP + this.REQUEST_PROTOCOL + CRLF);
        for (String headerName : this.REQUEST_HEADERS.keySet())
        {
            for (String headerValue : this.REQUEST_HEADERS.get(headerName))
            {
                sb.append(headerName + KVSEP + headerValue + CRLF);
            }
        }
        sb.append(CRLF);
        sb.append(this.REQUEST_BODY);
        return sb.toString();
    }
    
    /**
     * Get a map of the query parameters in this exchange's request.
     * 
     * @param  encoding  the name of a supported character encoding
     * @return a map of the query parameter keys and value lists for
     *         this exchange's request, in the encoding given
     * @throws UnsupportedEncodingException If character encoding needs to be 
     *         consulted, but named character encoding is not supported
     * @see    URLDecoder#decode(String, String)
     */
    public Map<String, List<String>> getParameterMap(String encoding) throws UnsupportedEncodingException
    {
        Map<String, List<String>> result = new HashMap<>();
        
        String rawQuery = this.REQUEST_URI.getRawQuery();
        if (rawQuery != null)
        {
            String[] rawKeyValuePairs = rawQuery.split("&");
            for (String rawKVPair : rawKeyValuePairs)
            {
                String[] rawKVPairSplit = rawKVPair.split("=", 2);
                String rawKey = rawKVPairSplit[0];
                String rawValue = rawKVPairSplit.length > 1 ? rawKVPairSplit[1] : null;
                String key = URLDecoder.decode(rawKey, encoding);
                String value = URLDecoder.decode(rawValue, encoding);
                List<String> valueList = result.get(key);
                if (valueList == null) 
                {
                    valueList = new ArrayList<String>();
                    result.put(key, valueList);
                }
                valueList.add(value);
            }
        }
        
        return result;
    }
    
    public String getResponseHeaderString()
    {
        StringBuilder sb = new StringBuilder();
        String CRLF = "\r\n";
        String KVSEP = ": ";
        for (String headerName : this.RESPONSE_HEADERS.keySet())
        {
            for (String headerValue : this.RESPONSE_HEADERS.get(headerName))
            {
                sb.append(headerName + KVSEP + headerValue + CRLF);
            }
        }
        sb.append(CRLF);
        return sb.toString();
    }
    
    
    /**
     * Sends a header-only response.
     * @param statusCode   the HTTP status code for the response
     * @throws IOException if a problem occurs while writing the response
     */
    public void sendHeadersOnly(int statusCode)
        throws IOException
    {
        this.exchange.sendResponseHeaders(statusCode, -1);
    }
    
    /**
     * Sends a 404 not-found response.
     * @throws IOException if a problem occurs while writing the response
     */
    public void sendNotFoundResponse()
        throws IOException
    {
        if (this.REQUEST_METHOD.equals("HEAD"))
        {
            this.sendHeadersOnly(404);
        }
        else
        {
            this.sendStringResponse(404, "text/plain", "Not found");
        }
    }
    
    /**
     * Sends a string as an HTTP response.
     * @param statusCode   the HTTP status code for the response
     * @param contentType  the contenty type for the response
     * @param response     the string to send as a response
     * @throws IOException if a problem occurs while writing the response or closing the exchange's streams
     */
    public void sendStringResponse(int statusCode, String contentType, String response)
        throws IOException
    {
        this.sendByteArrayResponse(statusCode, contentType, response.getBytes());
    }
    
    /**
     * Sends a byte array as an HTTP response.
     * @param statusCode   the HTTP status code for the response
     * @param contentType  the contenty type for the response
     * @param response     the byte array to send as a response
     * @throws IOException if a problem occurs while writing the response or closing the exchange's streams
     */
    public void sendByteArrayResponse(int statusCode, String contentType, byte[] response)
        throws IOException
    {
        // deal with ranges
        // see http://stackoverflow.com/questions/18336174/how-to-properly-provide-data-for-audio
        boolean hasRange = this.REQUEST_HEADERS.containsKey("Range");
        boolean responseHasContentRange = this.RESPONSE_HEADERS.containsKey("Content-Range");
        if (hasRange && !responseHasContentRange)
        {
            // looks like "bytes=0-1"
            String[] rangeValues = this.REQUEST_HEADERS.getFirst("Range").split("=|-");
            int startPos = Integer.parseInt(rangeValues[1]);
            int lastPos = response.length - 1;
            int endPos = lastPos;
            if (rangeValues.length > 2 && !rangeValues[2].equals(""))
            {
                endPos = Integer.parseInt(rangeValues[2]);
                if (endPos > lastPos) { endPos = lastPos; }
            }
            if (startPos != 0 || endPos != lastPos || (rangeValues.length > 2 && rangeValues[2].equals("")))
            {
                statusCode = 206;
            }
            int rangeLength = endPos + 1 - startPos;
            byte[] rangedResponse = new byte[rangeLength];
            System.arraycopy(response, startPos, rangedResponse, 0, rangeLength);
            response = rangedResponse;
            String contentRange = String.format("bytes %d-%d/%d", startPos, endPos, rangeLength);
            this.RESPONSE_HEADERS.add("Content-Range", contentRange);
        }
        
        // add content type if not already set
        if (!this.RESPONSE_HEADERS.containsKey("Content-Type"))
        {
            this.RESPONSE_HEADERS.add("Content-Type", contentType);
        }
        
        // send headers and write response
        this.exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream out = this.exchange.getResponseBody())
        {
            out.write(response);
        }
    }
    
    
}