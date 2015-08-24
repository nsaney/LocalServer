/*
 * Nicholas Saney
 * 
 * Created: August 12, 2015
 * 
 * LocalHttpRequestHandlerAdapter.java
 * LocalHttpRequestHandlerAdapter class definition
 */

package chairosoft.local_server;

import java.io.IOException;

public class LocalHttpRequestHandlerAdapter implements LocalHttpRequestHandler
{
    @Override public void handleOptions(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleGet(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleHead(LocalHttpExchange x) throws IOException { x.sendHeadersOnly(404); }
    @Override public void handlePost(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handlePut(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleDelete(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleTrace(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleConnect(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
    @Override public void handleOtherRequest(LocalHttpExchange x) throws IOException { x.sendNotFoundResponse(); }
}