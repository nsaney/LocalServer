/*
 * Nicholas Saney
 * 
 * Created: August 12, 2015
 * 
 * LocalServer.java
 * LocalServer class definition
 */

package chairosoft.local_server;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LocalServer
{
    //
    // Main Method
    //
    
    /**
     * A short test of LocalServer.
     * @param args (ignored)
     * @throws Exception (not caught)
     */
    public static void main(String[] args)
        throws Exception
    {
        final LocalServer server = new LocalServer(8000, 1, 25);
        System.out.printf("local address is %s \n", LocalServer.getLocalAddress().getHostAddress());
        System.out.printf("server port is %s \n", server.getPort());
        
        server.setDefaultRequestHandler(new LocalHttpRequestHandlerAdapter()
        {
            @Override public void handleGet(LocalHttpExchange x) throws IOException
            {
                String uri = x.REQUEST_URI.toString();
                System.out.println("get requested: " + uri);
                String response = "You requested: " + uri;
                x.sendStringResponse(200, response);
            }
        });
        
        server.createContext("/test", (LocalHttpExchange x) ->
        {
            System.out.println("tested!");
            System.out.println(x.getRequestString());
            String response = "You tested it, yay.";
            x.sendStringResponse(200, response);
        });
        
        server.createContext("/stop", (LocalHttpExchange x) ->
        {
            System.out.println("stopped!");
            String response = "You stopped it, yay.";
            x.sendStringResponse(200, response);
            server.stop(0);
        });
        
        server.start();
    }
    
    
    //
    // non-main Static Methods
    //
    
    /**
     * Convenience method to get the network address of the local host.
     * Converts any exceptions to RuntimeException before rethrowing them.
     * @return the network address of the local host
     */
    public static InetAddress getLocalAddress()
    {
        try
        {
            return InetAddress.getLocalHost();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Convenience method for turning an InputStream into a String.
     * Converts any exceptions to RuntimeException before rethrowing them.
     * @param in the input stream to read from 
     * @return   a String version of the contents of the input stream
     */
    public static String getStringFromInputStream(InputStream in)
    {
        return new String(LocalServer.getByteArrayFromInputStream(in));
    }
    
    /**
     * Convenience method for turning an InputStream into a byte array.
     * Converts any exceptions to RuntimeException before rethrowing them.
     * @param in the input stream to read from 
     * @return   a byte array version of the contents of the input stream
     */
    public static byte[] getByteArrayFromInputStream(InputStream in)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream() { @Override public byte[] toByteArray() { return this.buf; } };
        try 
        {
            byte[] buffer = new byte[2048];
            int bytesRead = 0;
            while (-1 < (bytesRead = in.read(buffer)))
            {
                baos.write(buffer, 0, bytesRead);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }
    
    
    //
    // Static Fields
    //
    
    public static final String DEFAULT_FAVICON_MIME_TYPE = "image/x-icon";
    
    
    //
    // Instance Fields
    //
    
    protected final HttpServer server;
    protected LocalHttpRequestHandler defaultRequestHandler = null;
    protected String faviconMimeType = DEFAULT_FAVICON_MIME_TYPE;
    protected byte[] faviconBytes = null;
    
    
    //
    // Constructors
    //
    
    public LocalServer(int portNumber)
    {
        this(portNumber, 0, 0, 0);
    }
    
    public LocalServer(int portNumber, int backlogMax)
    {
        this(portNumber, 0, 0, backlogMax);
    }
    
    public LocalServer(int preferredPortNumber, int retryIncrement, int maxRetries)
    {
        this(preferredPortNumber, retryIncrement, maxRetries, 0);
    }
    
    public LocalServer(int preferredPortNumber, int retryIncrement, int maxRetries, int backlogMax)
    {
        if (maxRetries < 0)
        {
            String message = String.format("Value for maxRetries (%s) cannot be negative.", maxRetries);
            throw new IllegalArgumentException(message);
        }
        
        HttpServer s = null;
        IOException lastException = null;
        for (int p = preferredPortNumber, r = 0; r < maxRetries; p += retryIncrement, ++r)
        {
            try
            {
                s = HttpServer.create(new InetSocketAddress(p), backlogMax);
                break;
            }
            catch (IOException ex)
            {
                lastException = new IOException(ex);
            }
        }
        
        if (s == null)
        {
            String message = String.format("Unable to start local server with preferredPortNumber = %s, retryIncrement = %s, maxRetries = %s, and backlogMax = %s.", 
                preferredPortNumber, 
                retryIncrement, 
                maxRetries, 
                backlogMax
            );
            throw new RuntimeException(message, lastException);
        }
        
        this.server = s;
    }
    
    
    // 
    // Instance Methods
    //
    
    public void setDefaultRequestHandler(LocalHttpRequestHandler _defaultRequestHandler) { this.defaultRequestHandler = _defaultRequestHandler; }
    public void setFavicon(String _faviconMimeType, byte[] _faviconBytes) { this.faviconMimeType = _faviconMimeType; this.faviconBytes = _faviconBytes; }
    
    public InetSocketAddress getAddress() { return this.server.getAddress(); }
    public int getPort() { return this.getAddress().getPort(); }
    
    public HttpContext createContext(String path, LocalHttpHandler handler) 
    {
        return this.server.createContext(path, handler.toHttpHandler()); 
    }
    public void removeContext(HttpContext context) { this.server.removeContext(context); }
    public void removeContext(String path) { this.server.removeContext(path); }
    
    public void start()
    {
        this.createContext("/", (LocalHttpExchange x) ->
        {
            if (this.defaultRequestHandler == null)
            {
                x.sendNotFoundResponse();
                return;
            }
            
            switch (x.REQUEST_METHOD)
            {
                case "OPTIONS": this.defaultRequestHandler.handleOptions(x); break;
                case "GET": this.defaultRequestHandler.handleGet(x); break;
                case "HEAD": this.defaultRequestHandler.handleHead(x); break;
                case "POST": this.defaultRequestHandler.handlePost(x); break;
                case "PUT": this.defaultRequestHandler.handlePut(x); break;
                case "DELETE": this.defaultRequestHandler.handleDelete(x); break;
                case "TRACE": this.defaultRequestHandler.handleTrace(x); break;
                case "CONNECT": this.defaultRequestHandler.handleConnect(x); break;
                default: this.defaultRequestHandler.handleOtherRequest(x); break;
            }
        });
        
        this.createContext("/favicon.ico", (LocalHttpExchange x) ->
        {
            if (this.faviconBytes == null)
            {
                x.sendNotFoundResponse();
                return;
            }
            
            x.RESPONSE_HEADERS.set("Content-Type", this.faviconMimeType);
            x.sendByteArrayResponse(200, this.faviconBytes);
        });
        
        this.server.setExecutor(null); // creates a default executor
        this.server.start();
    }
    public void stop(int delay) { this.server.stop(delay); }
}