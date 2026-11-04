import Keycloak from "keycloak-js";

/** @type {Promise<Record<string, string>> | undefined} */
let environmentPromise;

/** @type {Promise<Keycloak> | undefined} */
let keycloakPromise;

export function getEnvironment() {
  if (!environmentPromise) {
    environmentPromise = fetch(new URL("environment.json", document.baseURI)).then(
      async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to load environment.json (${response.status})`);
        }
        return response.json();
      },
    );
  }
  return environmentPromise;
}

export function getKeycloak() {
  if (!keycloakPromise) {
    keycloakPromise = (async () => {
      const env = await getEnvironment();
      const keycloak = new Keycloak({
        url: env.serverBaseUrl,
        realm: env.realm,
        clientId: env.clientId,
      });
      const authenticated = await keycloak.init({
        onLoad: "login-required",
        pkceMethod: "S256",
      });
      if (!authenticated) {
        throw new Error("Not authenticated");
      }
      return keycloak;
    })();
  }
  return keycloakPromise;
}

/**
 * Call the Account REST API.
 * @param {string} path e.g. "/?userProfileMetadata=true" or "/"
 * @param {RequestInit & { method?: string, body?: unknown }} [options]
 */
export async function accountFetch(path, options = {}) {
  const env = await getEnvironment();
  const keycloak = await getKeycloak();
  try {
    await keycloak.updateToken(5);
  } catch {
    await keycloak.login();
  }

  const url = new URL(
    `${env.serverBaseUrl}/realms/${encodeURIComponent(env.realm)}/account${path.startsWith("/") ? path : `/${path}`}`,
  );

  const { body, headers: extraHeaders, ...rest } = options;
  const response = await fetch(url, {
    ...rest,
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${keycloak.token}`,
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...extraHeaders,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  return response;
}
