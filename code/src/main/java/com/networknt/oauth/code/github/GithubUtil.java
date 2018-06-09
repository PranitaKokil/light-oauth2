package com.networknt.oauth.code.github;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the utility to access github.com api. Before calling this, you need to download the certificate from
 * api.github.com and import it into the client.truststore
 *
 */
public class GithubUtil {
	private final static Logger logger = LoggerFactory.getLogger(GithubUtil.class);
	private final static String CONFIG_GITHUB = "github";
	private final static String CONFIG_SECRET = "secret";
	private final static String GITHUB_TOKEN = "githubToken";

	private final static GithubConfig config = (GithubConfig) Config.getInstance().getJsonObjectConfig(CONFIG_GITHUB,
			GithubConfig.class);
	private final static Map<String, Object> secret = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
	private final static String githubToken = (String) secret.get(GITHUB_TOKEN);

	/**
	 * Get the user roles from github.com repository.
	 *
	 * @param username String username
	 * @return A set of group attributes for the username on github DB. You can
	 *         only call this method if the username has been authenticated
	 * @throws ClientException ClientException
	 */
	public static Set<String> authorize(String username) throws Exception {
		Set<String> groups = new HashSet<String>();

		String apiURL = config.protocol + "://" + config.host + config.pathPrefix;
		String contentsURL = "/repos/" + config.owner + "/" + config.repo + "/contents/" + config.path;

		final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        
        final ClientConnection connection = client.connect(new URI(apiURL), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
        	final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(contentsURL);
			request.getRequestHeaders().put(Headers.AUTHORIZATION, "token " + githubToken);
			request.getRequestHeaders().put(Headers.HOST, "api.github.com");
			request.getRequestHeaders().put(Headers.ACCEPT, "application/vnd.github.v3.raw");
			request.getRequestHeaders().put(Headers.CACHE_CONTROL, "no-cache");
			request.getRequestHeaders().put(Headers.USER_AGENT, "stevehu");
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await(); 
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
		int statusCode = reference.get().getResponseCode();
		System.out.println("statusCode = " + statusCode);
		String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
		System.out.println("testHttp2Get: statusCode = " + statusCode + " body = " + body);

		return groups;
	}

}
