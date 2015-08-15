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

import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class LocalHttpExchange implements Closeable
{
    //
    // Instance Fields
    //
    
    protected final HttpExchange exchange;
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
        this.sendStringResponse(404, "Not found");
    }
    
    /**
     * Sends a string as an HTTP response.
     * @param statusCode   the HTTP status code for the response
     * @param response     the string to send as a response
     * @throws IOException if a problem occurs while writing the response or closing the exchange's streams
     */
    public void sendStringResponse(int statusCode, String response)
        throws IOException
    {
        this.sendByteArrayResponse(statusCode, response.getBytes());
    }
    
    /**
     * Sends a byte array as an HTTP response.
     * @param statusCode   the HTTP status code for the response
     * @param response     the byte array to send as a response
     * @throws IOException if a problem occurs while writing the response or closing the exchange's streams
     */
    public void sendByteArrayResponse(int statusCode, byte[] response)
        throws IOException
    {
        this.exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream out = this.exchange.getResponseBody())
        {
            out.write(response);
        }
    }
    
    
}