package tofer17.ags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TimeBasedEncrypter (TBE) does 3 things:
 * 	1. Provide signed timestamp: the current time, signature, and public key (for verification)
 * 	2. An encrypted object with: Payload and signed timestamp
 * 	3. A key
 * 
 * When POSTed with a payload and future timestamp -> #2
 * When POSTed or GETed with a past timestamp -> #3
 * Otherwise -> #1
 * 
 * TBE signs timestamps with its own private key, but it encrypts payloads with
 * a symmetric key based on the timestamp provided. When TBE initializes, it
 * will attempt to load its public and private keys-- if that fails, it will
 * generate a new key pair and save that. During initialization it will also
 * create a JWK export of its public key.
 * 
 * When a future timestamp is requested, it will compute a symmetric key and
 * encrypt the provided object; it'll then store the key in a hashtable so
 * as to not have to recompute the key again. Of course, it can recreate the key
 * at any point...
 * 
 * In all cases, a JSON string is returned:
 * {
 * 		"timestamp":{
 * 			"time":"...",
 * 			"signature":[...],
 * 			"key":"..."},
 * 		"payload":[...],
 * 
 * }
 * @author cmetyko
 *
 */
public class TimeBasedEncrypter extends HttpServlet {

	private static final long serialVersionUID = 6444279358615606979L;

	@SuppressWarnings( "unused" )
	private static final Logger logger = LoggerFactory.getLogger( TimeBasedEncrypter.class );

	private KeyPair keyPair = null;
	
	public TimeBasedEncrypter () {
        super();
    }

	private static final KeyPair loadOrGenerateKeyPair ( File publicKeyFile, File privateKeyFile ) {
		
		// Check if the files exist
		if ( !publicKeyFile.canRead() || !privateKeyFile.canRead() ) {
			logger.info( "Cannot read from '{}' or '{}'-- creating new key pair.", publicKeyFile, privateKeyFile);
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
	
	public void init( ServletConfig config ) throws ServletException {
		keyPair = loadOrGenerateKeyPair( 
				new File( System.getProperty("tofer17.ags.tbe.publicKey") ), 
				new File( System.getProperty("tofer17.ags.tbe.privateKey") )
				);
		if ( keyPair == null ) {
			logger.error( "FATAL: could not load or generate keys!" );
		}
	}

	protected void doGet ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		response.getWriter().append( "Served at: " ).append( request.getContextPath() );
	}

	protected void doPost ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doGet( request, response );
	}

}
