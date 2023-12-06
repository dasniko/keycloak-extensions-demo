<form id="webauth" action="${url.loginAction}" method="post">
	<input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
	<input type="hidden" id="authenticatorData" name="authenticatorData"/>
	<input type="hidden" id="signature" name="signature"/>
	<input type="hidden" id="credentialId" name="credentialId"/>
	<input type="hidden" id="userHandle" name="userHandle"/>
	<input type="hidden" id="error" name="error"/>
</form>

<script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
<script type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
<script type="text/javascript">
	let abortController;

	function startWebAuthnAuthentication() {
		if (abortController) {
			abortController.abort('restart');
		}
		doAuthenticate(true);
	}

	async function doAuthenticate(forceAuthentication) {

		// Check if WebAuthn is supported by this browser
		if (!window.PublicKeyCredential) {
			$("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
			$("#webauth").submit();
			return;
		}

		let isCMA = false;
		if (!forceAuthentication) {
			if (!window.PublicKeyCredential.isConditionalMediationAvailable) {
				// do not prepare/initiate conditional mediation
				return;
			} else {
				isCMA = await window.PublicKeyCredential.isConditionalMediationAvailable();
			}
		}

		if (!isCMA && !forceAuthentication) {
			// in case CMA is not available and authentication is not forced, e.g. button was not pressed,
			// abort the conditional attempt, do not intiate a get-credentials call if not explicitly requested.
			return;
		}

		abortController = new AbortController();

		const challenge = "${challenge}";
		const userVerification = "${userVerification}";
		const rpId = "${rpId}";
		const publicKey = {
			rpId : rpId,
			challenge: base64url.decode(challenge, { loose: true }),
		};

		const createTimeout = ${createTimeout};
		if (createTimeout !== 0) publicKey.timeout = createTimeout * 1000;

		if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

		try {
			const result = await navigator.credentials.get({
				publicKey,
				signal: isCMA ? abortController.signal : undefined,
				mediation: isCMA ? "conditional" : undefined,
			});
			window.result = result;

			const clientDataJSON = result.response.clientDataJSON;
			const authenticatorData = result.response.authenticatorData;
			const signature = result.response.signature;

			$("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), { pad: false }));
			$("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), { pad: false }));
			$("#signature").val(base64url.encode(new Uint8Array(signature), { pad: false }));
			$("#credentialId").val(result.id);
			if(result.response.userHandle) {
				$("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), { pad: false }));
			}
			$("#webauth").submit();
		} catch (err) {
			if (err === 'restart') {
				return;
			}
			$("#error").val(err);
			$("#webauth").submit();
		}
	}

	doAuthenticate();
</script>
