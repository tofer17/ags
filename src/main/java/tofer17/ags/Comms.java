package tofer17.ags;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

	private static final long RETRANS = 1000 * 10;

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

			// logger.info( envelope.recipient + " " + envelope.message );

			final AsyncContext ac = waiters.get( envelope.to );

			// logger.info( "{}", ac );

			if ( ac != null ) {
				try {
					logger.info( "Sending {}...", envelope );
					PrintWriter writer = ac.getResponse().getWriter();

					writer.println( envelope.attempt() );
					writer.flush();

					ac.complete();
				} catch ( IOException ioe ) {
					logger.info( ioe.toString() );
					waiters.remove( envelope.to );
					messages.put( envelope );
				}
			} else { // Couldn't find a waiter, put it back in the queue
				// TODO: This should go into a retry queue

				// Check if it hasn't reached the retrans limit
				if ( envelope.received + RETRANS >= System.currentTimeMillis() ) {
					messages.put( envelope );
				} else {
					logger.warn( "Envelope to '{}' timed out: {}", envelope.to, envelope );
				}
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

		final String i = request.getParameter( "i" );
		if ( i != null ) {
			PrintWriter writer = response.getWriter();
			writer.println( String.format( "waiters: %s messages: %s", waiters.size(), messages.size() ) );
			writer.flush();
			writer.close();
			return;
		}

		final String waiter = request.getParameter( "w" );

		if ( waiter == null || "".equals( waiter ) ) {
			response.sendError( 422, "nocando" );
			return;
		} else {
			logger.info( "Establishing connection with '{}'...", waiter );
		}

		final PrintWriter writer = response.getWriter();
		// for IE
		writer.println( "\n" );
		writer.flush();

		final AsyncContext ac = request.startAsync( request, response );
		ac.setTimeout( 10 * 60 * 1000 );
		// ac.setTimeout( 1 * 5 * 1000 );

		waiters.put( waiter, ac );

		final HttpServletResponse sr = response;
		final HttpServletRequest sq = request;
		// writer.close();
		ac.addListener( new AsyncListener() {

			public void onComplete ( AsyncEvent event ) throws IOException {
				waiters.remove( waiter );
				logger.info( "completed '{}'", waiter );
				// event.getSuppliedResponse().flushBuffer();
				// event.getAsyncContext().getResponse().getOutputStream().close();
				// event.getAsyncContext().getResponse().
				// sr.getOutputStream().close();
				logger.info( "t={}", Thread.currentThread().getName() );
				Thread.currentThread().interrupt();
				// Thread.currentThread().getClass();

				logger.info( "alldone {}", writer.checkError() );
			}

			public void onTimeout ( AsyncEvent event ) throws IOException {
				waiters.remove( waiter );
				logger.info( "timedout '{}'", waiter );
			}

			public void onError ( AsyncEvent evt ) throws IOException {
				logger.info( "errored '{}'", waiter );
				;
			}

			public void onStartAsync ( AsyncEvent evt ) throws IOException {
				logger.info( "started '{}'", waiter );
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

		// final String recipient = request.getParameter( "r" );
		final String[] to = request.getParameterValues( "t" );
		final String message = request.getParameter( "m" );
		final String from = "Mr. X";

		if ( to == null || to.length < 1 ) {
			response.sendError( 422, "nocando" );
			return;
		} else if ( message == null || "".equals( message ) ) {
			response.sendError( 423, "nocando" );
			return;
		}

		for ( int i = 0; i < to.length; i++ ) {
			final Envelope envelope = new Envelope( to[ i ], to, from, message );
			logger.info( "Env to {} => {}", to[ i ], envelope.toJSON() );
			try {
				messages.put( envelope );
			} catch ( InterruptedException e ) {
				e.printStackTrace();
				response.sendError( 521, "sump'n went snap" );
			}
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

	private static final class XEnvelope {

		public final long created = System.currentTimeMillis();

		public final String recipient;

		public final String message;

		public XEnvelope ( String recipient, String message ) {
			this.recipient = recipient;
			this.message = message;
		}

		@Override
		public String toString () {
			return String.format( "(%s) %s -> %s", created, recipient, message );
		}
	}

	public static final String stringArrayToJSONArray ( String[] sa ) {
		final StringBuilder sb = new StringBuilder( "[" );
		for ( int i = 0; i < sa.length; i++ ) {
			// TODO: need to do some escaping, Lucy!
			sb.append( i > 0 ? ",\"" : "\"" ).append( sa[ i ] ).append( "\"" );
		}
		return sb.append( "]" ).toString();
	}

	public static final String longArrayToJSONArray ( ArrayList<Long> la ) {
		final StringBuilder sb = new StringBuilder( "[" );
		for ( int i = 0; i < la.size(); i++ ) {
			sb.append( i > 0 ? "," : "" ).append( la.get( i ) );
		}
		return sb.append( "]" ).toString();
	}

	// Received
	private static final class Envelope {

		public final String to;

		public final String toList;

		public final String from;

		public final long received = System.currentTimeMillis();

		public final ArrayList<Long> attempts = new ArrayList<Long>();

		public final String message;

		public Envelope ( String to, String[] toList, String from, String message ) {
			this.to = to;
			this.toList = stringArrayToJSONArray( toList );
			this.from = from;
			this.message = message;
		}

		public Envelope addAttempt ( long time ) {
			attempts.add( time );
			return this;
		}

		public Envelope addAttempt () {
			return addAttempt( System.currentTimeMillis() );
		}

		public String attempt () {
			return addAttempt().toJSON();
		}

		public String toJSON () {
			// {t:[a,b,c],f:x,r:l,a:[l0,l1],m:msg}
			return String.format( "{\"t\":%s," + "\"f\":\"%s\"," + "\"r\":%s," + "\"a\":%s," + "\"m\":\"%s\"}", toList,
				from, received, longArrayToJSONArray( attempts ), message );
		}

		@Override
		public String toString () {
			return toJSON();
		}

	}

}
