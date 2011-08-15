package betamax.server

import org.apache.http.HttpResponseInterceptor
import org.apache.http.impl.nio.DefaultServerIOEventDispatch
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor
import org.apache.http.nio.protocol.BufferingHttpServiceHandler
import org.apache.http.nio.reactor.IOReactor
import org.apache.http.params.SyncBasicHttpParams
import org.apache.http.impl.*
import static org.apache.http.params.CoreConnectionPNames.*
import static org.apache.http.params.CoreProtocolPNames.ORIGIN_SERVER
import org.apache.http.protocol.*

/**
 * Basic, yet fully functional and spec compliant, HTTP/1.1 server based on the non-blocking 
 * I/O model.
 * <p>
 * Please note the purpose of this application is demonstrate the usage of HttpCore APIs.
 * It is NOT intended to demonstrate the most efficient way of building an HTTP server. 
 */
class HttpProxyServer {

	private IOReactor reactor
	final int port

	HttpProxyServer() {
		port = 5555
	}

	void start() {
        def params = new SyncBasicHttpParams()
        params.setIntParameter(SO_TIMEOUT, 5000)
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024)
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false)
		params.setBooleanParameter(TCP_NODELAY, true)
		params.setParameter(ORIGIN_SERVER, "HttpComponents/1.1")

        def httpproc = new ImmutableHttpProcessor([
                new ResponseDate(),
                new ResponseServer(),
                new ResponseContent(),
                new ResponseConnControl()
        ] as HttpResponseInterceptor[])

        def handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params)

        def reqistry = new HttpRequestHandlerRegistry()
        reqistry.register "*", new HttpProxyHandler()

        handler.handlerResolver = reqistry

        handler.eventListener = new EventLogger()

        def ioEventDispatch = new DefaultServerIOEventDispatch(handler, params)
        def ioReactor = new DefaultListeningIOReactor(2, params)
        ioReactor.listen(new InetSocketAddress(port))
        Thread.start {
            ioReactor.execute(ioEventDispatch)
        }

        reactor = ioReactor
    }

	void stop() {
		reactor.shutdown()
	}

}

