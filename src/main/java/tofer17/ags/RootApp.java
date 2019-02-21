package tofer17.ags;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootApp extends HttpServlet {
       
	private static final long serialVersionUID = 5227877557424883496L;

	@SuppressWarnings( "unused" )
	private static final Logger logger = LoggerFactory.getLogger( RootApp.class );

	public RootApp () {
        super();
    }

	protected void doGet ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		response.getWriter().append( "Served at: " ).append( request.getContextPath() );
	}

	protected void doPost ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doGet( request, response );
	}

}
