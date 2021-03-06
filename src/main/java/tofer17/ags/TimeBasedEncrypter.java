package tofer17.ags;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.Base64.Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://docs.oracle.com/javase/9/security/java-cryptography-architecture-jca-reference-guide.htm#JSSEC-GUID-36893ED5-E3CA-496B-BC78-B13EE971C736
 * https://stackoverflow.com/questions/1205135/how-to-encrypt-string-in-java
 *
 * @author cmetyko
 *
 */
public class TimeBasedEncrypter extends HttpServlet {

	private static final long serialVersionUID = 6444279358615606979L;

	private static final Logger logger = LoggerFactory.getLogger( TimeBasedEncrypter.class );

	private static final Encoder B64Encoder = Base64.getEncoder();

	public static final int TS_MODE_QUERY = 0;

	public static final int TS_MODE_EMBARGO = 1;

	public static final int TS_MODE_OTHER = -1;

	private String signedTimestampAlgo = null;

	private String signedTimestampFormat = null;

	private String signedTimestampTSFormat = "%1$s:%2$s";

	private String signedTimestampError = null;

	private String embargoKeyError = "{\"error\":-1}";

	private String embargoKeyFormat = "{\"k\":\"%s\"}";

	private String embargoKeyTransformation = "RSA/ECB/PKCS1Padding";

	private int embargoKeySize = 32;

	private String embargoKeyAlgo = "AES";

	private KeyPair keyPair = null;

	private CharSequence pubKeyExport = null;

	public TimeBasedEncrypter () {
		super();
	}

	private static final String EncodeToB64 ( byte[] bytes ) {
		return B64Encoder.encodeToString( bytes );
	}

	private static Properties DEFAULT_PROPERTIES;
	static {
		final Properties p = new Properties();
		p.setProperty( "tofer17.ags.tbe.signed.timestamp.algo", "SHA256withRSA" );
		p.setProperty( "tofer17.ags.tbe.signed.timestamp.format",
			"{\"t\":%1$s,\"m\":%2$s,\"s\":\"%3$s\",\"k\":\"%4$s\"}" );
		p.setProperty( "tofer17.ags.tbe.signed.timestamp.error", "{\"error\":-1}" );

		p.setProperty( "tofer17.ags.tbe.embargo.key.error", "{\"error\":-1}" );
		p.setProperty( "tofer17.ags.tbe.embargo.key.format", "{\"k\":\"%s\"}" );
		p.setProperty( "tofer17.ags.tbe.embargo.key.transformation", "RSA/ECB/PKCS1Padding" );
		p.setProperty( "tofer17.ags.tbe.embargo.key.size", "32" );
		p.setProperty( "tofer17.ags.tbe.embargo.key.algo", "AES" );
		// Although a neat idea-- not happening.
		// p.setProperty( "tofer17.ags.tbe.key.algo", "RSA" );
		// p.setProperty( "tofer17.ags.tbe.key.size", "2048" );
		DEFAULT_PROPERTIES = new Properties( p );
	}

	private static final KeyPair importBase64KeyPair ( String algo, String publicKeyBase64, String privateKeyBase64 )
		throws GeneralSecurityException {

		final byte[] encodedPublicKey = Base64.getDecoder().decode( publicKeyBase64 );
		final byte[] encodedPrivateKey = Base64.getDecoder().decode( privateKeyBase64 );

		final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec( encodedPublicKey );
		final PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec( encodedPrivateKey );

		final KeyFactory keyFactory = KeyFactory.getInstance( algo );

		final PublicKey publicKey = keyFactory.generatePublic( pubKeySpec );
		final PrivateKey privateKey = keyFactory.generatePrivate( priKeySpec );

		return new KeyPair( publicKey, privateKey );
	}

	private static final KeyPair generateNewKeyPair ( String algo, int size ) throws GeneralSecurityException {

		if ( "RSA".equals( algo ) ) {
			final RSAKeyGenParameterSpec params = new RSAKeyGenParameterSpec( size, RSAKeyGenParameterSpec.F0 );

			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance( algo );

			keyGen.initialize( params );

			return keyGen.generateKeyPair();
		}

		throw new NoSuchAlgorithmException( "Cannot work with '" + algo + "'" );
	}

	private final void loadConfig () {
		final Properties props = new Properties( DEFAULT_PROPERTIES );

		String propsFilename = System.getProperty( "tofer17.ags.tbe.properties.file" );

		if ( propsFilename == null || propsFilename.length() < 1 ) {
			propsFilename = "./run/etc/tbe.properties";
		}

		boolean saveNeeded = false;

		FileInputStream fin = null;
		try {
			fin = new FileInputStream( propsFilename );
			props.load( fin );
		} catch ( IOException e ) {
			// e.printStackTrace();
			logger.warn( "Properties file '{}' could not load.", propsFilename );
			saveNeeded = true;
		} finally {
			try {
				if ( fin != null ) {
					fin.close();
				}
			} catch ( IOException e ) {
				// e.printStackTrace();
				logger.warn( "...error closing properties file '{}'...", propsFilename );
			} finally {
				fin = null;
			}
		}

		signedTimestampAlgo = props.getProperty( "tofer17.ags.tbe.signed.timestamp.algo" );
		signedTimestampFormat = props.getProperty( "tofer17.ags.tbe.signed.timestamp.format" );
		signedTimestampError = props.getProperty( "tofer17.ags.tbe.signed.timestamp.error" );

		final String publicKeyBase64 = props.getProperty( "tofer17.ags.tbe.public.key" );
		final String privateKeyBase64 = props.getProperty( "tofer17.ags.tbe.private.key" );

		if ( publicKeyBase64 == null || privateKeyBase64 == null || publicKeyBase64.length() < 1
			|| privateKeyBase64.length() < 1 ) {
			// Something's not right...
			try {
				keyPair = generateNewKeyPair( props.getProperty( "tofer17.ags.tbe.key.algo", "RSA" ),
					Integer.parseInt( props.getProperty( "tofer17.ags.tbe.key.size", "2048" ) ) );
				saveNeeded = true;

			} catch ( NumberFormatException | GeneralSecurityException e ) {
				e.printStackTrace();
				keyPair = null;
			}
		} else {
			// Load 'em in!
			try {
				keyPair = importBase64KeyPair( props.getProperty( "tofer17.ags.tbe.key.algo", "RSA" ), publicKeyBase64,
					privateKeyBase64 );
				logger.info( "Imported key pair from properties: '{}'", keyPair );
			} catch ( GeneralSecurityException e ) {
				e.printStackTrace();
				keyPair = null;
			}
		}

		if ( saveNeeded && keyPair != null ) {
			saveConfig();
		} else if ( saveNeeded ) {
			logger.warn(
				"Configuration needs to be saved, however, we cannot since we could neither load nor generate our key-pair" );
		}

	}

	private final void saveConfig () {
		final Properties props = new Properties( DEFAULT_PROPERTIES );

		props.setProperty( "tofer17.ags.tbe.signed.timestamp.algo", signedTimestampAlgo );
		props.setProperty( "tofer17.ags.tbe.signed.timestamp.format", signedTimestampFormat );
		props.setProperty( "tofer17.ags.tbe.signed.timestamp.error", signedTimestampError );

		if ( keyPair != null ) {
			props.setProperty( "tofer17.ags.tbe.public.key", EncodeToB64( keyPair.getPublic().getEncoded() ) );
			props.setProperty( "tofer17.ags.tbe.private.key", EncodeToB64( keyPair.getPrivate().getEncoded() ) );
		}

		String propsFilename = System.getProperty( "tofer17.ags.tbe.properties.file" );

		if ( propsFilename == null || propsFilename.length() < 1 ) {
			propsFilename = "./run/etc/tbe.properties";
		}

		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream( propsFilename );
			props.store( fout, "" );
		} catch ( IOException e ) {
			// e.printStackTrace();
			logger.warn( "Could not save properties to '{}'", propsFilename );
		} finally {
			try {
				if ( fout != null ) {
					fout.close();
				}
			} catch ( IOException e ) {
				// e.printStackTrace();
				logger.warn( "...could not close properties file '{}'...", propsFilename );
			} finally {
				fout = null;
			}
		}
	}

	private final CharSequence getSignedTimestampJSON ( int m ) {

		final String timestamp = "" + System.currentTimeMillis();

		if ( pubKeyExport == null ) {
			pubKeyExport = EncodeToB64( keyPair.getPublic().getEncoded() );
		}

		try {
			final Signature sig = Signature.getInstance( signedTimestampAlgo );
			sig.initSign( keyPair.getPrivate() );

			final String dig = String.format( signedTimestampTSFormat, timestamp, m );
			sig.update( dig.getBytes( StandardCharsets.UTF_8 ) );

			final byte[] sigBytes = sig.sign();
			final String sigB64 = EncodeToB64( sigBytes );

			return String.format( signedTimestampFormat, timestamp, m, sigB64, pubKeyExport );
		} catch ( InvalidKeyException | SignatureException | NoSuchAlgorithmException e ) {
			e.printStackTrace();
			return signedTimestampError;
		}
	}

	private final Key generateKeyForTime ( String t ) throws GeneralSecurityException {

		final Cipher cipher = Cipher.getInstance( embargoKeyTransformation );

		cipher.init( Cipher.ENCRYPT_MODE, keyPair.getPrivate() );

		final byte[] encr = cipher.doFinal( t.getBytes( StandardCharsets.UTF_8 ) );

		final byte[] trunc = new byte[ embargoKeySize ];

		System.arraycopy( encr, 0, trunc, 0, Math.min( trunc.length, encr.length ) );

		return new SecretKeySpec( trunc, embargoKeyAlgo );
	}

	private final Key getKeyForTime ( String t ) throws GeneralSecurityException {

		return generateKeyForTime( t );
	}

	private final CharSequence getPastKeyJSON ( String t ) {
		try {
			final String keyBase64 = EncodeToB64( getKeyForTime( t ).getEncoded() );
			return String.format( embargoKeyFormat, keyBase64 );
		} catch ( GeneralSecurityException e ) {
			e.printStackTrace();
		}

		return embargoKeyError;
	}

	private final CharSequence getEmbargoJSON ( String t, String o ) {
		try {
			final Key key = getKeyForTime( t );

			if ( o == null ) {
				o = "";
			}

			// Wrap o -> embargo {t:123,o:"???",ts:{...}}"
			final String embargoString = String.format( "{\"t\":%1$s,\"o\":\"%2$s\",\"ts\":%3$s}", t, o,
				getSignedTimestampJSON( TS_MODE_EMBARGO ) );

			final Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
			cipher.init( Cipher.ENCRYPT_MODE, key );

			final byte[] encr = cipher.doFinal( embargoString.getBytes( StandardCharsets.UTF_8 ) );
			final String encrBase64 = EncodeToB64( encr );

			final byte[] ivBytes = cipher.getIV();
			final String ivBase64 = EncodeToB64( ivBytes );

			return String.format( "{\"t\":%1$s,\"iv\":\"%2$s\",\"ct\":\"%3$s\"}", t, ivBase64, encrBase64 );

		} catch ( GeneralSecurityException e ) {
			e.printStackTrace();
		}

		return embargoKeyError;
	}

	public void init ( ServletConfig config ) throws ServletException {

		super.init( config );

		loadConfig();

		if ( keyPair == null ) {
			logger.error( "FATAL: could not load or generate keys!" );
		} else {
			logger.info( "TBE initialized {}", getSignedTimestampJSON( TS_MODE_OTHER ) );
		}
	}

	@Override
	public void destroy () {

		saveConfig();

		super.destroy();
	}

	protected void doGet ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {

		// GET nothing -> SignedTimeStamp
		final String tParm = request.getParameter( "t" );

		if ( tParm == null ) {
			response.getWriter().append( getSignedTimestampJSON( TS_MODE_QUERY ) );
			return;
		}

		// GET t & t is in past -> Return key-for-t
		if ( tParm.length() >= 13 ) {
			try {

				if ( Long.parseLong( tParm ) < System.currentTimeMillis() ) {
					response.getWriter().append( getPastKeyJSON( tParm ) );
					return;
				}

			} catch ( NumberFormatException nfe ) {
				;
			}
		}

		// * -> Return error
		response.getWriter().append( signedTimestampError );

	}

	protected void doPost ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {

		final String tParm = request.getParameter( "t" );

		// POST t & t is in future -> embargo "o" (which could be null)
		if ( tParm != null && tParm.length() >= 13 ) {
			try {

				if ( Long.parseLong( tParm ) >= System.currentTimeMillis() ) {
					response.getWriter().append( getEmbargoJSON( tParm, request.getParameter( "o" ) ) );
					return;
				}
			} catch ( NumberFormatException nfe ) {
				;
			}

		}

		// * -> Return error
		response.getWriter().append( signedTimestampError );

	}

}
