package tofer17.ags;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comet-style reflector
 *
 * @author cmetyko
 *
 */
public class Comms extends HttpServlet {

	private static final long serialVersionUID = -2664038576539200092L;

	private static final Logger logger = LoggerFactory.getLogger( TimeBasedEncrypter.class );

	private static final Map<String,AsyncContext> waiters = new Hashtable<String,AsyncContext>();

	private static final BlockingQueue<Envelope> messages = new LinkedBlockingQueue<Envelope>();

	private Thread notifierThread = null;

	public Comms () {
		super();
	}

	private boolean pollQueue () {
		try {

			final Envelope envelope = messages.take();

			logger.info( envelope.recipient + " " + envelope.message );

			final AsyncContext ac = waiters.get( envelope.recipient );

			logger.info( "{}", ac );

			if ( ac != null ) {
				try {
					PrintWriter writer = ac.getResponse().getWriter();

					writer.println(
						String.format( "{\"r\":\"%1$s\",\"m\":\"%2$s\"}", envelope.recipient, envelope.message ) );
					writer.flush();

					ac.complete();
				} catch ( IOException ioe ) {
					logger.info( ioe.toString() );
					waiters.remove( envelope.recipient );
					messages.put( envelope );
				}
			} else {
				messages.put( envelope );
			}
		} catch ( InterruptedException iex ) {
			return false;
		}
		return true;
	}

	@Override
	public void init ( ServletConfig config ) throws ServletException {
		super.init( config );

		logger.info( "Comms init.." );
		Runnable notifierRunnable = new Runnable() {

			public void run () {
				while ( pollQueue() ) {
					;
				}
			}
		};

		notifierThread = new Thread( notifierRunnable );
		notifierThread.start();

	}

	protected void doGet ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {
		logger.info( "doGet..." );

		response.setContentType( "text/html" );
		response.setHeader( "Cache-Control", "private" );
		response.setHeader( "Pragma", "no-cache" );

		final String waiter = request.getParameter( "w" );

		if ( waiter == null || "".equals( waiter ) ) {
			response.sendError( 422, "nocando" );
			return;
		} else {
			logger.info( "Establishing connection with '{}'...", waiter );
		}

		PrintWriter writer = response.getWriter();
		// for IE
		writer.println( "\n" );
		writer.flush();

		final AsyncContext ac = request.startAsync( request, response );
		ac.setTimeout( 10 * 60 * 1000 );

		waiters.put( waiter, ac );

		ac.addListener( new AsyncListener() {

			public void onComplete ( AsyncEvent event ) throws IOException {
				waiters.remove( waiter );
			}

			public void onTimeout ( AsyncEvent event ) throws IOException {
				waiters.remove( waiter );
			}

			public void onError ( AsyncEvent evt ) throws IOException {
				;
			}

			public void onStartAsync ( AsyncEvent evt ) throws IOException {
				;
			}
		} );

	}

	protected void doPost ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {
		logger.info( "doPost..." );

		response.setContentType( "text/plain" );
		response.setHeader( "Cache-Control", "private" );
		response.setHeader( "Pragma", "no-cache" );

		request.setCharacterEncoding( "UTF-8" );

		final String recipient = request.getParameter( "r" );
		final String message = request.getParameter( "m" );

		if ( recipient == null || "".equals( recipient ) ) {
			response.sendError( 422, "nocando" );
			return;
		} else if ( message == null || "".equals( message ) ) {
			response.sendError( 423, "nocando" );
			return;
		}

		final Envelope envelope = new Envelope( recipient, message );

		try {
			messages.put( envelope );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
			response.sendError( 521, "sump'n went snap" );
		}
	}

	@Override
	public void destroy () {
		logger.info( "Comms going dark bruh..." );

		waiters.clear();
		notifierThread.interrupt();

		logger.info( "done" );
		super.destroy();
	}

	private static final class Envelope {

		public final String recipient;

		public final String message;

		public Envelope ( String recipient, String message ) {
			this.recipient = recipient;
			this.message = message;
		}

	}

}
