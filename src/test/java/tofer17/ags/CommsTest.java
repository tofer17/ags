/**
 *
 */
package tofer17.ags;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommsTest {

	@SuppressWarnings ( "unused" )
	private static final Logger logger = LoggerFactory.getLogger( CommsTest.class );

	@BeforeAll
	static void setUpBeforeClass () throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass () throws Exception {
	}

	@BeforeEach
	void setUp () throws Exception {
	}

	@AfterEach
	void tearDown () throws Exception {
	}

	@Test
	void testComms () {
		if ( "1".equals( "2" ) ) {
			fail( "Not yet implemented" );
		}
		logger.trace( "Success" );
	}

	@Test
	void testDoGetHttpServletRequestHttpServletResponse () {
		if ( "1".equals( "2" ) ) {
			fail( "Not yet implemented" );
		}
		logger.trace( "Success" );
	}

	@Test
	void testDoPostHttpServletRequestHttpServletResponse () {
		if ( "1".equals( "2" ) ) {
			fail( "Not yet implemented" );
		}
		logger.trace( "Success" );
	}

}
