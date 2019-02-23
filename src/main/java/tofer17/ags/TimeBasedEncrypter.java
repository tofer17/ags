package tofer17.ags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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
import java.util.Date;
import java.util.concurrent.AbstractExecutorService;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
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
 * TimeBasedEncrypter (TBE) does 3 things: 1. Provide signed timestamp: the
 * current time, signature, and public key (for verification) 2. An encrypted
 * object with: Payload and signed timestamp 3. A key
 *
 * When POSTed with a payload and future timestamp -> #2 When POSTed or GETed
 * with a past timestamp -> #3 Otherwise -> #1
 *
 * TBE signs timestamps with its own private key, but it encrypts payloads with
 * a symmetric key based on the timestamp provided. When TBE initializes, it
 * will attempt to load its public and private keys-- if that fails, it will
 * generate a new key pair and save that. During initialization it will also
 * create a JWK export of its public key.
 *
 * When a future timestamp is requested, it will compute a symmetric key and
 * encrypt the provided object; it'll then store the key in a hashtable so as to
 * not have to recompute the key again. Of course, it can recreate the key at
 * any point...
 *
 * In all cases, a JSON string is returned: { "timestamp":{ "time":"...",
 * "signature":[...], "key":"..."}, "payload":[...],
 *
 * }
 *
 * See:
 * https://docs.oracle.com/javase/9/security/java-cryptography-architecture-jca-reference-guide.htm#JSSEC-GUID-36893ED5-E3CA-496B-BC78-B13EE971C736
 * https://stackoverflow.com/questions/1205135/how-to-encrypt-string-in-java
 *
 * @author cmetyko
 *
 */
public class TimeBasedEncrypter extends HttpServlet {

	private static final long serialVersionUID = 6444279358615606979L;

	@SuppressWarnings ( "unused" )
	private static final Logger logger = LoggerFactory.getLogger( TimeBasedEncrypter.class );

	private KeyPair keyPair = null;

	private CharSequence pubKeyExport = null;

	public TimeBasedEncrypter () {
		super();
	}

	private static final KeyPair loadOrGenerateKeyPair ( File publicKeyFile, File privateKeyFile ) {

		// Check if the files exist
		if ( !publicKeyFile.canRead() || !privateKeyFile.canRead() ) {
			logger.info( "Cannot read from '{}' or '{}'-- creating new key pair.", publicKeyFile, privateKeyFile );
			try {

				final RSAKeyGenParameterSpec params = new RSAKeyGenParameterSpec( 2048, RSAKeyGenParameterSpec.F0 );

				final KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );

				keyGen.initialize( params );

				final KeyPair kp = keyGen.generateKeyPair();

				Files.write( Paths.get( publicKeyFile.getAbsolutePath() ), kp.getPublic().getEncoded() );
				Files.write( Paths.get( privateKeyFile.getAbsolutePath() ), kp.getPrivate().getEncoded() );

				return kp;

			} catch ( NoSuchAlgorithmException e ) {
				e.printStackTrace();
			} catch ( InvalidAlgorithmParameterException e ) {
				e.printStackTrace();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		} else { // Load 'em up!

			try {
				final byte[] encodedPublicKey = Files.readAllBytes( Paths.get( publicKeyFile.getAbsolutePath() ) );
				final byte[] encodedPrivateKey = Files.readAllBytes( Paths.get( privateKeyFile.getAbsolutePath() ) );

				final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec( encodedPublicKey );
				final PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec( encodedPrivateKey );

				final KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );

				final PublicKey publicKey = keyFactory.generatePublic( pubKeySpec );
				final PrivateKey privateKey = keyFactory.generatePrivate( priKeySpec );

				final KeyPair kp = new KeyPair( publicKey, privateKey );

				logger.info( "Loaded keys from '{}' and '{}'.", publicKeyFile, privateKeyFile );

				return kp;
			} catch ( NoSuchAlgorithmException e ) {
				e.printStackTrace();
			} catch ( InvalidKeySpecException e ) {
				e.printStackTrace();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * t = time, s = signature, k = key {"t":"...", "s":"...", "k":"..."}
	 *
	 * @return
	 */
	private CharSequence getTimestampJSON () {

		byte[] signature = new byte[ 0 ];

		final String time = "" + System.currentTimeMillis();

		try {
			final Signature sig = Signature.getInstance( "SHA256withRSA" );
			sig.initSign( keyPair.getPrivate() );
			sig.update( time.getBytes() );
			signature = sig.sign();
		} catch ( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		} catch ( InvalidKeyException e ) {
			e.printStackTrace();
		} catch ( SignatureException e ) {
			e.printStackTrace();
		}

		return new StringBuffer( 771 ).append( "{\"t\":\"" ).append( time ).append( "\",\"s\":\"" )
			.append( Base64.getEncoder().encodeToString( signature ) ).append( "\"," ).append( pubKeyExport )
			.append( "}" );
	}

	private CharSequence getKeyFor ( long t ) {
		try {
			final SecretKey key = genKey( t );
			return "{\"k\":\"" + Base64.getEncoder().encodeToString( key.getEncoded() ) + "\"}";
		} catch ( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		} catch ( InvalidKeySpecException e ) {
			e.printStackTrace();
		}

		return "{\"e\":\"error\"}";
	}

	private void test ( long t, String o ) {
		try {
			logger.info( "-------------" );
			// encrypt "t" to "et" using private key
			// Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
// TODO: "ECB"??
			Cipher cipher = Cipher.getInstance( "RSA/ECB/PKCS1Padding" );
			cipher.init( Cipher.ENCRYPT_MODE, keyPair.getPrivate() );
			final byte[] et = cipher.doFinal( ( t + "" ).getBytes() );
			logger.info( "et=>({}) '{}'", et.length, et );

			// use "et" to create new key "etk"
			// final PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec( et );
			// final KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
			// final PrivateKey etk = keyFactory.generatePrivate( priKeySpec );
			final byte[] trunc = new byte[ 32 ]; // <- "32" can be parameterized
			System.arraycopy( et, 0, trunc, 0, Math.min( trunc.length, et.length ) );
			final SecretKeySpec etk = new SecretKeySpec( trunc, "AES" );
			// final KeyFactory skFactory = KeyFactory.getInstance( "AES" );
			// final PrivateKey etk = skFactory.generatePrivate( skSpec );// generateSecret(
			// skSpec );

			logger.info( "etk=>({}) {}", etk.getEncoded().length, etk.getEncoded() );

			// xform "o" to "oo" as such {"ts":{JSON-TS},"o":"..."}
			final String oo = new StringBuilder( o.length() + 200 ).append( "{\"t\":" ).append( t )
				.append( ",\"o\":\"" ).append( o ).append( "\"," ).append( "\"ts\":" ).append( getTimestampJSON() )
				.append( "}" ).toString();

			logger.info( "oo='{}'", oo );

			// encrypt "oo" to "eoo" using "etk"
			cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
			// cipher = Cipher.getInstance( "AES/CBC/PKCS5PADDING" );
			// final IvParameterSpec iv = new IvParameterSpec( "1234123412341234".getBytes(
			// "UTF-8" ) );
			// cipher.init( Cipher.ENCRYPT_MODE, etk, iv );
// https://stackoverflow.com/questions/15554296/simple-java-aes-encrypt-decrypt-example

			cipher.init( Cipher.ENCRYPT_MODE, etk );

			final byte[] eoo = cipher.doFinal( oo.getBytes() );
			logger.info( "eoo='{}'", eoo );

// TODO: Prob gon'a need to ship the IV too
			logger.info( "IV={}", cipher.getIV() );

			// xform "eoo" to "eooj" as such {"t":1234,"c":"base64"}
			final String eooj = new StringBuilder().append( "{\"t\":" ).append( t ).append( ",\"c\":\"" )
				.append( Base64.getEncoder().encodeToString( eoo ) ).append( "\"}" ).toString();

			logger.info( "eooj=>({}) '{}'", eooj.length(), eooj );

		} catch ( Throwable ex ) {
			ex.printStackTrace();
		}
	}

	private SecretKey genKey ( long t ) throws NoSuchAlgorithmException, InvalidKeySpecException {

		// FIXME: This is clearly a bad idea...
		final char[] pw = ( t + "-" + serialVersionUID ).toCharArray();

		final PBEKeySpec spec = new PBEKeySpec( pw );

		final SecretKeyFactory factory = SecretKeyFactory.getInstance( "PBEWithHmacSHA256AndAES_256" );

		final SecretKey key = factory.generateSecret( spec );

		return key;
	}

	private CharSequence embargo ( long t, String d ) {

		try {
			final SecretKey key = genKey( t );

			final byte[] salt = new byte[ 50 ];

			new SecureRandom().nextBytes( salt );

			final int iterations = 1717;

			final Cipher cipher = Cipher.getInstance( "PBEWithHmacSHA256AndAES_256" );

			final PBEParameterSpec params = new PBEParameterSpec( salt, iterations );

			cipher.init( Cipher.ENCRYPT_MODE, key, params );

			final byte[] buf = cipher.doFinal( d.getBytes() );

			return "{\"c\":\"" + Base64.getEncoder().encodeToString( buf ) + "\"}";

		} catch ( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		} catch ( InvalidKeySpecException e ) {
			e.printStackTrace();
		} catch ( NoSuchPaddingException e ) {
			e.printStackTrace();
		} catch ( InvalidKeyException e ) {
			e.printStackTrace();
		} catch ( InvalidAlgorithmParameterException e ) {
			e.printStackTrace();
		} catch ( IllegalBlockSizeException e ) {
			e.printStackTrace();
		} catch ( BadPaddingException e ) {
			e.printStackTrace();
		}

		return "{\"e\":\"error\"}";
	}

	public void init ( ServletConfig config ) throws ServletException {
		keyPair = loadOrGenerateKeyPair( new File( System.getProperty( "tofer17.ags.tbe.publicKey" ) ),
			new File( System.getProperty( "tofer17.ags.tbe.privateKey" ) ) );

		if ( keyPair == null ) {
			logger.error( "FATAL: could not load or generate keys!" );
		}

		pubKeyExport = new StringBuffer( 400 ).append( "\"k\":\"" )
			.append( Base64.getEncoder().encodeToString( keyPair.getPublic().getEncoded() ) ).append( "\"" ).toString();

		if ( "1".equals( "2" ) ) { // for debugging, adds r:[...] as part of the pubKeyExp
			byte[] b = keyPair.getPublic().getEncoded();
			StringBuffer sb = new StringBuffer().append( "\"r\":[" );
			for ( int i = 0; i < b.length; i++ ) {
				sb.append( i > 0 ? "," : "" ).append( b[ i ] );
			}
			sb.append( "]," ).append( pubKeyExport );
			pubKeyExport = sb.toString();
		}
	}

	protected void doGet ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {

		final String t = request.getParameter( "t" );

		test( System.currentTimeMillis(), "Hello world!" );

		if ( t == null || t.length() < 1 ) {
			response.getWriter().append( getTimestampJSON() );
			return;
		}

		logger.info( "Got request for '{}'", t );
		final long d = Long.parseLong( t );
		final long now = System.currentTimeMillis();
		CharSequence s;
		logger.info( "d={} now={} ?={}", d, now, ( d < now ) );
		if ( d < now ) {
			s = getKeyFor( d );
		} else {
			String o = request.getParameter( "o" );
			if ( o == null ) {
				o = getTimestampJSON().toString();
			}
			s = embargo( d, o );
		}

		response.getWriter().append( s );

	}

	protected void doPost ( HttpServletRequest request, HttpServletResponse response )
		throws ServletException, IOException {
		doGet( request, response );
	}

}
