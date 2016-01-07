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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.concurrent.Executor;

import com.sun.net.httpserver.HttpContext;
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
        
        server.createDefaultContext(new LocalHttpRequestHandlerAdapter()
        {
            @Override public void handleGet(LocalHttpExchange x) throws IOException
            {
                String uri = x.REQUEST_URI.toString();
                System.out.println("get requested: " + uri);
                String response = "You requested: " + uri;
                x.sendStringResponse(200, "text/plain", response);
            }
        });
        
        server.createContext("/existence", (LocalHttpExchange x) ->
        {
            System.out.println("existence!");
            String response = "true";
            x.RESPONSE_HEADERS.set("Access-Control-Allow-Origin", "*");
            x.sendStringResponse(200, "text/plain", response);
        });
        
        server.createContext("/test", (LocalHttpExchange x) ->
        {
            System.out.println("tested!");
            System.out.println(x.getRequestString());
            String response = "You tested it, yay.";
            x.sendStringResponse(200, "text/plain", response);
        });
        
        server.createContext("/stop", (LocalHttpExchange x) ->
        {
            System.out.println("stopped!");
            String response = "You stopped it, yay.";
            x.sendStringResponse(200, "text/plain", response);
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
        return LocalServer.getByteArrayFromInputStream(in, 2048);
    }
    
    /**
     * Convenience method for turning an InputStream into a byte array.
     * Converts any exceptions to RuntimeException before rethrowing them.
     * @param in         the input stream to read from 
     * @param bufferSize the size of the buffer to use while reading
     * @return           a byte array version of the contents of the input stream
     */
    public static byte[] getByteArrayFromInputStream(InputStream in, int bufferSize)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try 
        {
            LocalServer.copyIO(in, baos, bufferSize);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }
    
    /**
     * Convenience method for copying from an InputStream to an OutputStream.
     * This method will attempt to copy until the read method on 
     * the InputStream returns -1.
     * @param  in          the input stream to read from 
     * @param  out         the output stream to write to
     * @param  bufferSize  the size of the buffer to use while copying
     * @throws IOException in case an error occurs while reading or writing data
     */
    public static void copyIO(InputStream in, OutputStream out, int bufferSize) 
        throws IOException
    {
        byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        while (-1 < (bytesRead = in.read(buffer)))
        {
            out.write(buffer, 0, bytesRead);
        }
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
    
    public void createDefaultContext(LocalHttpRequestHandler _defaultRequestHandler)
    {
        this.setDefaultRequestHandler(_defaultRequestHandler);
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
    }
    public void removeDefaultContext() { this.removeContext("/"); }
    
    public void createFaviconContext(String _faviconMimeType, byte[] _faviconBytes)
    {
        this.setFavicon(_faviconMimeType, _faviconBytes);
        this.createContext("/favicon.ico", (LocalHttpExchange x) ->
        {
            if (this.faviconBytes == null)
            {
                x.sendNotFoundResponse();
                return;
            }
            
            x.sendByteArrayResponse(200, this.faviconMimeType, this.faviconBytes);
        });
    }
    public void removeFaviconContext() { this.removeContext("/favicon.ico"); }
    
    
    public void start() { this.server.start(); }
    public void stop(int delay) { this.server.stop(delay); }
    
    public Executor getExecutor() { return this.server.getExecutor(); }
    public void setExecutor(Executor e) { this.server.setExecutor(e); }
    
    public HttpServer getWrappedHttpServer() { return this.server; }
}