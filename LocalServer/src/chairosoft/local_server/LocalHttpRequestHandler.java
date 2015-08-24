/*
 * Nicholas Saney
 * 
 * Created: August 12, 2015
 * 
 * LocalHttpRequestHandler.java
 * LocalHttpRequestHandler interface definition
 */

package chairosoft.local_server;

import java.io.IOException;

public interface LocalHttpRequestHandler
{
    void handleOptions(LocalHttpExchange x) throws IOException;
    void handleGet(LocalHttpExchange x) throws IOException;
    void handleHead(LocalHttpExchange x) throws IOException;
    void handlePost(LocalHttpExchange x) throws IOException;
    void handlePut(LocalHttpExchange x) throws IOException;
    void handleDelete(LocalHttpExchange x) throws IOException;
    void handleTrace(LocalHttpExchange x) throws IOException;
    void handleConnect(LocalHttpExchange x) throws IOException;
    void handleOtherRequest(LocalHttpExchange x) throws IOException; 
}