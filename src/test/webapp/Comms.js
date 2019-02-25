/**
 *
 */
function setup () {
	let el = document.getElementById( "hkupstatus" );
	el.innerHTML = "Not connected";
	el.style.color = "red";
	el = document.getElementById( "wid" );
	el.value = "tofer17-" + Math.round( Math.random() * 100 );
}

var abort = false;

function hookup () {
	let el = document.getElementById( "hkupstatus" );
	el.innerHTML = "...connecting...";
	el.style.color = "blue";
	el = document.getElementById( "hkup" );
	el.innerHTML = "...abort..";

	if ( xhr ) {
		abort = true;
		xhr.abort();
		el.innerHTML = "Connect";
		el = document.getElementById( "hkupstatus" );
		el.innerHTML = "Not connected";
		el.style.color = "red";
		xhr = null;
	} else {
		connect();
	}
}

var xhr, wid;

function connect () {
	wid = document.getElementById( "wid" ).value;
	xhr = new XMLHttpRequest();
	xhr.onreadystatechange = conn;
	xhr.open( "GET", "com?w=" + wid );
	xhr.onload = connected;
	xhr.send();
}

function conn () {
	//console.log( this );
	let el = document.getElementById( "hkupstatus" );
	switch ( this.readyState ) {
		case XMLHttpRequest.UNSENT : // 0
			break;
		case XMLHttpRequest.OPENED : // 1
			el.innerHTML = "opened";
			console.log( "Opened" );
			break;
		case XMLHttpRequest.HEADERS_RECEIVED : // 2
			break;
		case XMLHttpRequest.LOADING : // 3
			el.innerHTML = "Connected";
			el.style.color = "green";
			el = document.getElementById( "hkup" );
			el.innerHTML = "Disconnect";
			el.disabled = false;
			break;
		case XMLHttpRequest.DONE : // 4
			console.error( "DONE" );
			if ( !abort ) {
				displayMessage( JSON.parse( this.response ) );
				connect();
			} else {
				abort = false;
			}
			break;
		default :
			console.error( "???", this );

	}
}

function displayMessage ( msg ) {
	let recip = document.createElement( "span" );
	recip.className = "recip";
	recip.innerHTML = msg.r;

	let message = document.createElement( "span" );
	message.className = "msgbody";
	message.innerHTML = msg.m;

	let div = document.createElement( "div" );
	div.className = "message";
	div.appendChild( recip );
	div.appendChild( message );

	document.getElementById( "recemsg" ).appendChild( div );

}

function connected () {
	console.error( this );
}

function sendMsg () {
	let s = "f=" + wid;
	let tolist = document.getElementById( "tolist" ).value.split(",");
	console.log(tolist);
	for ( let i = 0; i < tolist.length; i++ ) {
		s += "&t=" + tolist[i];
	}
	s += "&m=" + document.getElementById( "sendmsg" ).value;
	console.log(s);
	let post = new XMLHttpRequest();
	post.open( "POST", "com" );
	post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	post.send( s );
}

/**************************************************** */
window.addEventListener( "load", () => {
	setup();
});

function hideKids ( evt ) {
	const n = evt.target.nextElementSibling;
	n.style.display = n.style.display == "" ? "none" : "";
}
