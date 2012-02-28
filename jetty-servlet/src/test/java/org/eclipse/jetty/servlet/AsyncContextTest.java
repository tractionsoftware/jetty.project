package org.eclipse.jetty.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.eclipse.jetty.server.AsyncContinuation;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This tests the correct functioning of the AsyncContext
 * 
 * tests for #371649 and #371635
 */
public class AsyncContextTest
{

    private Server _server = new Server();
    private ServletContextHandler _contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    private LocalConnector _connector = new LocalConnector();

    @Before
    public void setUp() throws Exception
    {
        _connector.setMaxIdleTime(30000);
        _server.setConnectors(new Connector[]
        { _connector });

        _contextHandler.setContextPath("/");
        _contextHandler.addServlet(new ServletHolder(new TestServlet()),"/servletPath");
        _contextHandler.addServlet(new ServletHolder(new TestServlet2()),"/servletPath2");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]
        { _contextHandler, new DefaultHandler() });

        _server.setHandler(handlers);
        _server.start();
    }

    @Test
    //Ignore ("test fails without a patch")
    public void testSimpleAsyncContext() throws Exception
    {    	
        String request = "GET /servletPath HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = new BufferedReader(new StringReader(responseString));
        
        Assert.assertEquals("HTTP/1.1 200 OK",br.readLine());
        
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        
        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath", br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath", br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:/servletPath", br.readLine());
    }
    
    @Test
    //Ignore ("test fails without a patch")
    public void testDispatchAsyncContext() throws Exception
    {        
        String request = "GET /servletPath?dispatch=true HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);
        
        BufferedReader br = new BufferedReader(new StringReader(responseString));
        
        Assert.assertEquals("HTTP/1.1 200 OK",br.readLine());
        
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        
        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath2", br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath2", br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:/servletPath2", br.readLine());
        Assert.assertEquals("servlet path attr is original","async:run:attr:servletPath:/servletPath", br.readLine());
        Assert.assertEquals("path info attr is correct","async:run:attr:pathInfo:null", br.readLine());
        Assert.assertEquals("query string attr is correct","async:run:attr:queryString:dispatch=true", br.readLine());
        Assert.assertEquals("context path attr is correct","async:run:attr:contextPath:", br.readLine());
        Assert.assertEquals("request uri attr is correct","async:run:attr:requestURI:/servletPath", br.readLine());
    }  

    @Test
    //Ignore ("test fails without a patch")
    public void testSimpleWithContextAsyncContext() throws Exception
    {           
        _contextHandler.setContextPath("/foo");
        
        String request = "GET /foo/servletPath HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);
 
        BufferedReader br = new BufferedReader(new StringReader(responseString));
        
        Assert.assertEquals("HTTP/1.1 200 OK",br.readLine());
        
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        
        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath", br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath", br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:/servletPath", br.readLine());
    }
    
    @Test
    //Ignore ("test fails without a patch")
    public void testDispatchWithContextAsyncContext() throws Exception
    {        
        _contextHandler.setContextPath("/foo");
        
        String request = "GET /foo/servletPath?dispatch=true HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);
        
        System.out.println(responseString);
        
        BufferedReader br = new BufferedReader(new StringReader(responseString));
        
        Assert.assertEquals("HTTP/1.1 200 OK",br.readLine());
        
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        
        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath2", br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath2", br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:/servletPath2", br.readLine());
        Assert.assertEquals("servlet path attr is original","async:run:attr:servletPath:/servletPath", br.readLine());
        Assert.assertEquals("path info attr is correct","async:run:attr:pathInfo:null", br.readLine());
        Assert.assertEquals("query string attr is correct","async:run:attr:queryString:dispatch=true", br.readLine());
        Assert.assertEquals("context path attr is correct","async:run:attr:contextPath:/foo", br.readLine());
        Assert.assertEquals("request uri attr is correct","async:run:attr:requestURI:/foo/servletPath", br.readLine());
    } 
    
    
    @After
    public void tearDown() throws Exception
    {
        _server.stop();
        _server.join();
    }

    private class TestServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getParameter("dispatch") != null)
            {                
                AsyncContext asyncContext = request.startAsync(request,response);
                asyncContext.dispatch("/servletPath2");
            }
            else
            {
                response.getOutputStream().print("doGet:getServletPath:" + request.getServletPath() + "\n");
                AsyncContext asyncContext = request.startAsync(request,response);
                response.getOutputStream().print("doGet:async:getServletPath:" + ((HttpServletRequest)asyncContext.getRequest()).getServletPath() + "\n");
                asyncContext.start(new AsyncRunnable(asyncContext));
            }
            return;

        }
    }

    private class TestServlet2 extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.getOutputStream().print("doGet:getServletPath:" + request.getServletPath() + "\n");
            AsyncContext asyncContext = request.startAsync(request, response);
            response.getOutputStream().print("doGet:async:getServletPath:" + ((HttpServletRequest)asyncContext.getRequest()).getServletPath() + "\n");         
            asyncContext.start(new AsyncRunnable(asyncContext));
            
            return;
        }
    }
    
    private class AsyncRunnable implements Runnable
    {
        private AsyncContext _context;

        public AsyncRunnable(AsyncContext context)
        {
            _context = context;
        }

        @Override
        public void run()
        {
            HttpServletRequest req = (HttpServletRequest)_context.getRequest();         
                        
            try
            {
                _context.getResponse().getOutputStream().print("async:run:" + req.getServletPath() + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:servletPath:" + req.getAttribute(AsyncContext.ASYNC_SERVLET_PATH) + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:pathInfo:" + req.getAttribute(AsyncContext.ASYNC_PATH_INFO) + "\n");              
                _context.getResponse().getOutputStream().print("async:run:attr:queryString:" + req.getAttribute(AsyncContext.ASYNC_QUERY_STRING) + "\n");              
                _context.getResponse().getOutputStream().print("async:run:attr:contextPath:" + req.getAttribute(AsyncContext.ASYNC_CONTEXT_PATH) + "\n");              
                _context.getResponse().getOutputStream().print("async:run:attr:requestURI:" + req.getAttribute(AsyncContext.ASYNC_REQUEST_URI) + "\n");              
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            _context.complete();         
        }
    }

}
