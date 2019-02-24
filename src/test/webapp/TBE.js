/*
 *
 */
var fetchAt, importAt, verifyAt;

function getSignedTimestamp () {
	fetchAt = new Date().getTime();
	const btn = document.getElementById( "fetchbtn" );
	btn.disabled = true;
	btn.innerHTML = "...fetching...";
	document.getElementById( "fetchpb" ).value = 0;
	let req = new XMLHttpRequest();
	req.open( "GET", "tbe");
	req.onload = recieveSignedTimestamp;
	req.send();
}


function b64toi8array ( b64 ) {
	return Int8Array.from( Array.prototype.map.call( window.atob( b64 ), c => c.charCodeAt( 0 ) ) );
}

var ts;

function debugKey ( k ) {
	return !k?"undefined":(""+
			k.constructor.name + " " +
			k.type + " " +
			k.algorithm.name + " " +
			k.algorithm.hash.name + " " +
			k.algorithm.modulusLength + " " +
			k.usages
			);
}

function recieveSignedTimestamp ( e ) {
	if ( this.readyState === XMLHttpRequest.DONE && this.status === 200 ) {
		document.getElementById( "fetchms" ).innerHTML = ( new Date().getTime() - fetchAt ) + "ms";
		const btn = document.getElementById( "fetchbtn" );
		btn.disabled = false;
		btn.innerHTML = "Fetch";
		document.getElementById( "fetchpb" ).value = 1;
		document.getElementById( "tsr" ).value = this.response;
		const tsr = JSON.parse( this.response );

		ts = {};

		ts.mode = tsr.m;

		ts.time = new Date( parseInt( tsr.t) );
		// The data to verify is: "<t>:<m>"
		ts.dat = new TextEncoder().encode( tsr.t + ":" + ts.mode );
		document.getElementById( "tsT" ).value = tsr.t;
		document.getElementById( "tsTime" ).value = ts.time;
		document.getElementById( "tsTimeEnc" ).value = ts.dat;

		ts.sig = b64toi8array( tsr.s );
		document.getElementById( "tsS" ).value = tsr.s;
		document.getElementById( "tsSig" ).value = ts.sig;

		const keyData = b64toi8array( tsr.k );
		document.getElementById( "tsK" ).value = tsr.k;
		document.getElementById( "tsKeyData" ).value = keyData;
		importAt = new Date().getTime();
		window.crypto.subtle.importKey(
			"spki",
			keyData,
			{ name : "RSASSA-PKCS1-v1_5", hash : "SHA-256" },
			true,
			[ "verify" ]
			).then( k => {
				ts.key = k;
				document.getElementById( "tsKey" ).value = debugKey( ts.key );
				document.getElementById( "impms" ).innerHTML = ( new Date().getTime() - importAt ) + "ms";
				verifySignedTimestamp();
				});
	}
}

function verifySignedTimestamp () {
	verifyAt = new Date().getTime();
	window.crypto.subtle.verify(
		{ name : "RSASSA-PKCS1-v1_5" },
		ts.key,
		ts.sig,
		ts.dat
		).then( r => { console.log(r);
			const sp = document.getElementById( "tsValid" );
			sp.innerHTML = r ? "Verified" : "FAILED";
			sp.style.color = r ? "green" : "red";
			document.getElementById( "verifyms" ).innerHTML = ( new Date().getTime() - verifyAt ) + "ms";
		}).catch( r => {
			const sp = document.getElementById( "tsValid" );
			sp.innerHTML = "FAILED";
			sp.style.color = "red";
			document.getElementById( "verifyms" ).innerHTML = ( new Date().getTime() - verifyAt ) + "ms";
		});
}

function setTime ( t ) {
	document.getElementById( "tim" ).value = t;
	getAboveTime();
}

function requestEscrow () {
	let req = new XMLHttpRequest();
	req.open( "POST", "tbe");
	req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	req.onload = recieveEscrowResponse;
	req.send(
		"t=" + document.getElementById( "tim" ).value +
		"&o=" + document.getElementById( "dat" ).value
	);
}

var emb;

function recieveEscrowResponse () {
	if ( this.readyState === XMLHttpRequest.DONE && this.status === 200 ) {
		document.getElementById( "req" ).value = this.response;
		emb = JSON.parse( this.response );
	}
}

function getAboveTime () {
	document.getElementById( "k4tim" ).value = document.getElementById( "tim" ).value;
}

function requestKey () {
	let req = new XMLHttpRequest();
	req.open( "GET", "tbe?"+"t=" + document.getElementById( "k4tim" ).value);
	req.onload = recieveKeyResponse;
	req.send();
}

var embKeyJSON, embKey;

function recieveKeyResponse () {
	if ( this.readyState === XMLHttpRequest.DONE && this.status === 200 ) {
		document.getElementById( "k4req" ).value = this.response;
		let k4res = JSON.parse(this.response);
		console.log( this, k4res );
		embKeyJSON = JSON.parse( this.response );
		if ( embKeyJSON.error ) {
			console.error("ERROR!");
			return;
		}

		// Import the key:
		window.crypto.subtle.importKey(
			"raw",
			b64toi8array( embKeyJSON.k ),
			{ name : "AES-GCM" },
			true,
			["decrypt"]
			).then( r=> {
				console.log(r);
				embKey = r;
				decrypt();
			});
	}
}

function decrypt () {
	let iv = b64toi8array( emb.iv );
	let algo = {
		name : "AES-GCM",
		iv : iv
	};

	let data = b64toi8array( emb.ct );

	window.crypto.subtle.decrypt(
		algo,
		embKey,
		data
	).then( r => {
		let d = new TextDecoder().decode( r );
		let res = JSON.parse( d );
		console.log( res );
	});
}

window.addEventListener( "load", () => {
	;
});

function hideKids ( evt ) {
	const n = evt.target.nextElementSibling;
	n.style.display = n.style.display == "" ? "none" : "";
}
