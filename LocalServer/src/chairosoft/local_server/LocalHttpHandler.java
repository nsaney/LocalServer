/*
 * Nicholas Saney
 * 
 * Created: August 14, 2015
 * 
 * LocalHttpHandler.java
 * LocalHttpHandler functional interface definition
 */

package chairosoft.local_server;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@FunctionalInterface
public interface LocalHttpHandler
{
    void handle(LocalHttpExchange exchange) throws IOException;
    
    default HttpHandler toHttpHandler()
    {
        return (HttpExchange exchange) -> this.handle(new LocalHttpExchange(exchange));
    }
}