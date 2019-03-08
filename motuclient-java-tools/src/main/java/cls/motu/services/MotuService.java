package cls.motu.services;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cls.motu.exceptions.MotuAuthenticationException;
import cls.motu.exceptions.MotuException;
import cls.motu.exceptions.MotuHttpStatusCodeException;
import cls.motu.exceptions.MotuIOException;
import cls.motu.exceptions.MotuResponseParsingException;
import cls.motu.exceptions.MotuUrlException;
import cls.motu.objs.ImmutableMotuRequestStatus;
import cls.motu.objs.MotuDownloadProductParameters;
import cls.motu.objs.MotuProductReference;
import cls.motu.objs.MotuRequestStatus;
import cls.motu.objs.RemoteMotu;
import cls.motu.properties.MotuProperties;
import cls.motu.services.unmarshalling.MotuUnmarshallerPool;
import cls.motu.utils.MotuDescribeDatasetResponseParser;
import cls.motu.utils.MotuHttpHelper;
import cls.motu.utils.MotuUrlsHelper;
import cls.motu.utils.OkHttpLogger;
import fr.cls.atoll.motu.api.message.xml.RequestSize;
import fr.cls.atoll.motu.api.message.xml.StatusModeResponse;
import fr.cls.atoll.motu.api.message.xml.StatusModeType;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

@Service
public class MotuService {
    private static final Logger log = LoggerFactory.getLogger(MotuService.class);

    private static final long MEGABITS_TO_BYTES = 125000L;

    private final MotuProperties motuProperties;
    private final OkHttpClient client;
    private final MotuUnmarshallerPool motuUnmarshallerPool;

    @Autowired
    public MotuService(final MotuProperties motuProperties, final MotuUnmarshallerPool motuUnmarshallerPool) {
        this.motuProperties = motuProperties;
        this.motuUnmarshallerPool = motuUnmarshallerPool;

        final HttpLoggingInterceptor loggingIterceptor = new HttpLoggingInterceptor(new OkHttpLogger(log));
        loggingIterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        final CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        client = new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true).cookieJar(new JavaNetCookieJar(cookieManager))
                .addInterceptor(loggingIterceptor).connectTimeout(motuProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(motuProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS).build();
    }

    /**
     * Makes a call to a MOTU describe dataset url and returns the parsed response.
     *
     * @param motu The remote motu service connection information.
     * @param product The product to describe.
     * @return {@link MotuDescribeDatasetResponseParser} object to be used to retrieve the parsing result.
     * @throws MotuException Raised if the HTTP call or the response parsing failed.
     */
    public MotuDescribeDatasetResponseParser describeDataset(final RemoteMotu motu, final MotuProductReference product) throws MotuException {
        final URI uri = MotuUrlsHelper.buildDescribeProductUri(motu, product);
        return describeDataset(uri, motu.getUsername(), motu.getPassword());
    }

    /**
     * Makes a call to a MOTU describe dataset url and returns the parsed response.
     *
     * @param uri Describe product uri.
     * @param username CAS username.
     * @param password CAS password.
     * @return {@link MotuDescribeDatasetResponseParser} object to be used to retrieve the parsing result.
     * @throws MotuException Raised if the HTTP call or the response parsing failed.
     */
    public MotuDescribeDatasetResponseParser describeDataset(final URI uri, final String username, final String password) throws MotuException {
        try (final Response response = callBehindCas(uri, username, password)) {
            final ResponseBody body = MotuHttpHelper.getResponseBodyIfValid(response);

            filterHtmlResponse(body);

            return MotuDescribeDatasetResponseParser.build(body.byteStream());
        }
    }

    /**
     * Sends a download product request to a remote motu service (with mode=status) and returns the parsed
     * response from the remote server.
     *
     * @param motu The remote motu service connection information.
     * @param product The product to download.
     * @param parameters Download products parameters.
     * @return The response from the server.
     * @throws MotuException When an exception occurs while accessing MOTU.
     */
    public MotuRequestStatus sendDownloadProduct(final RemoteMotu motu,
                                                 final MotuProductReference product,
                                                 final MotuDownloadProductParameters parameters)
            throws MotuException {
        log.debug("sending a download product request for product {}, with the following parameters: {}...", product, parameters);
        final URI uri = MotuUrlsHelper.buildDownloadProductUri(motu, product, parameters);
        try (final Response response = callBehindCas(uri, motu.getUsername(), motu.getPassword())) {
            final ResponseBody body = MotuHttpHelper.getResponseBodyIfValid(response);

            filterHtmlResponse(body);

            final StatusModeResponse statusResponse = motuUnmarshallerPool.unmarshal(body.charStream(), StatusModeResponse.class);

            return buildMotuRequestStatus(statusResponse);
        }
    }

    public MotuRequestStatus getSize(final RemoteMotu motu, final MotuProductReference product, final MotuDownloadProductParameters parameters)
            throws MotuException {
        log.debug("sending a get size request for product {}, with the following parameters: {}...", product, parameters);
        final URI uri = MotuUrlsHelper.buildGetSizeUri(motu, product, parameters);
        try (final Response response = callBehindCas(uri, motu.getUsername(), motu.getPassword())) {
            final ResponseBody body = MotuHttpHelper.getResponseBodyIfValid(response);

            filterHtmlResponse(body);

            final RequestSize statusResponse = motuUnmarshallerPool.unmarshal(body.charStream(), RequestSize.class);

            return buildMotuRequestStatus(statusResponse);
        }
    }

    /**
     * Sends a get request status to a remote motu server and returns the parsed server response.
     *
     * @param motu The remote motu server.
     * @param product Product parameters.
     * @param requestId The request id. @return The response from the server.
     */
    public MotuRequestStatus getRequestStatus(final RemoteMotu motu, final MotuProductReference product, final String requestId)
            throws MotuException {
        log.debug("Getting status for request '{}'...", requestId);
        final URI uri = MotuUrlsHelper.buildGetStatusUri(motu, product, requestId);
        try (final Response response = callBehindCas(uri, motu.getUsername(), motu.getPassword())) {
            final ResponseBody body = MotuHttpHelper.getResponseBodyIfValid(response);

            filterHtmlResponse(body);

            final StatusModeResponse statusResponse = motuUnmarshallerPool.unmarshal(body.charStream(), StatusModeResponse.class);

            return buildMotuRequestStatus(statusResponse);
        }
    }

    /**
     * Detects if the motu response provided in the parameter is an HTML response in which case the method
     * raises an exception with the content of the content of the response.
     *
     * @param body Body of the response.
     * @throws MotuException Exception raised when the response is of type HTML.
     */
    private void filterHtmlResponse(final ResponseBody body) throws MotuException {
        try {
            final MediaType contentType = body.contentType();
            if ((null != contentType) && "html".equals(contentType.subtype())) {
                final String string = body.string();

                final Document doc = Jsoup.parse(string);

                final String text = doc.select("#content").text().trim();

                throw new MotuException("Motu replied with an HTML response: " + text);
            }
        } catch (final IOException ex) {
            throw new MotuIOException("Exception occured while reading motu response", ex);
        }
    }

    /**
     * Iterates periodically over calls to {@link #getRequestStatus(RemoteMotu, MotuProductReference, String)}
     * until the server responds with a 'Done' or 'Error' status.
     *
     * @param motu The remote motu server.
     * @param product Product parameters.
     * @param requestId The request id.
     * @param pollingDelay Duration of waiting between two successive polls. @return The final status returned
     *            by the server.
     */
    public MotuRequestStatus getRequestStatusUntilTerminalStateReached(final RemoteMotu motu,
                                                                       final MotuProductReference product,
                                                                       final String requestId,
                                                                       final Duration pollingDelay)
            throws InterruptedException, MotuException {
        MotuRequestStatus rs = this.getRequestStatus(motu, product, requestId);
        while ((StatusModeType.DONE != rs.getStatusType()) && (StatusModeType.ERROR != rs.getStatusType())) {
            log.debug("non-terminal status '{}' returned by the remote server for the request '{}', waiting for {} ms before the next call...",
                      rs.getStatusType(),
                      requestId,
                      pollingDelay.toMillis());
            // noinspection BusyWait
            Thread.sleep(pollingDelay.toMillis());
            rs = this.getRequestStatus(motu, product, requestId);
        }

        log.debug("terminal status '{}' reached for request '{}', no more calls will be made", rs.getStatusType(), requestId);
        return rs;
    }

    public ResponseBody downloadExtractedProduct(final RemoteMotu motu, final URI productUri) throws MotuException {
        log.debug("Downloading extracted product '{}'...", productUri);
        final OkHttpClient downloadClient = client.newBuilder().readTimeout(30, TimeUnit.MINUTES).build();

        final Response response = callBehindCas(downloadClient, productUri, motu.getUsername(), motu.getPassword());

        return MotuHttpHelper.getResponseBodyIfValid(response);
    }

    /**
     * Calls a request eventually protected behind CAS, using the provider username and password to login on
     * CAS when needed (transparently logs in and returns a response).
     *
     * @param uri Request to call.
     * @param username CAS username to use when CAS authentication is required.
     * @param password CAS password to use when CAS authentication is required.
     * @return Response of the HTTP call to MOTU.
     */
    private Response callBehindCas(final URI uri, final String username, final String password)
            throws MotuHttpStatusCodeException, MotuResponseParsingException, MotuIOException, MotuUrlException, MotuAuthenticationException {
        return callBehindCas(client, uri, username, password);
    }

    /**
     * Calls a request eventually protected behind CAS, using the provider username and password to login on
     * CAS when needed (transparently logs in and returns a response).
     *
     * @param uri Request to call.
     * @param username CAS username to use when CAS authentication is required.
     * @param password CAS password to use when CAS authentication is required.
     * @return Response of the HTTP call to MOTU.
     */
    private Response callBehindCas(final OkHttpClient client, final URI uri, final String username, final String password)
            throws MotuHttpStatusCodeException, MotuResponseParsingException, MotuIOException, MotuUrlException, MotuAuthenticationException {
        final Request request;
        try {
            request = new Request.Builder().url(uri.toURL()).build();

        } catch (final MalformedURLException ex) {
            throw new MotuUrlException(ex);
        }
        return callBehindCas(client, request, username, password);
    }

    /**
     * Calls a request eventually protected behind CAS, using the provider username and password to login on
     * CAS when needed (transparently logs in and returns a response).
     *
     * @param client Http client to use.
     * @param request Request to call.
     * @param username CAS username to use when CAS authentication is required.
     * @param password CAS password to use when CAS authentication is required.
     * @return Response of the HTTP call to MOTU.
     * @throws MotuIOException On connexion issue.
     * @throws MotuHttpStatusCodeException When MOTU respond with non-success status code.
     * @throws MotuResponseParsingException When MOTU response could not be parsed.
     * @throws MotuAuthenticationException When CAS login could not be performed correctly.
     */
    private Response callBehindCas(final OkHttpClient client, final Request request, final String username, final String password)
            throws MotuHttpStatusCodeException, MotuResponseParsingException, MotuIOException, MotuAuthenticationException {
        try {
            final Response initialResponse = client.newCall(request).execute();
            final Response finalResponse;
            if (MotuHttpHelper.checkIfCasLoginForm(initialResponse)) {
                log.debug("detected CAS login redirection in response to '{}'", request.url());
                if (null == username) {
                    throw new MotuAuthenticationException("No username defined for motu");
                }

                try {
                    final ResponseBody body = MotuHttpHelper.getResponseBodyIfValid(initialResponse);
                    final String stringContent = body.string();
                    final String httpUrl = MotuHttpHelper.getHttpUrlFromResponse(initialResponse);

                    final Request casifiedRequest = MotuHttpHelper
                            .buildLoginRequest(httpUrl, stringContent, this.motuProperties.getCasLoginFormSelector(), username, password);

                    log.debug("CAS login attempt to endpoint '{}'...", casifiedRequest.url());
                    finalResponse = client.newCall(casifiedRequest).execute();
                } finally {
                    initialResponse.close();
                }
            } else {
                finalResponse = initialResponse;
            }

            return finalResponse;
        } catch (final IOException ex) {
            throw new MotuIOException(ex);
        }
    }

    private MotuRequestStatus buildMotuRequestStatus(final RequestSize statusResponse) {
        final Long size;
        if (null != statusResponse.getSize()) {
            size = Math.round(statusResponse.getSize() * MEGABITS_TO_BYTES); // TODO unit can be different
        } else {
            size = null;
        }

        return ImmutableMotuRequestStatus.builder().message(statusResponse.getMsg()).code(statusResponse.getCode()).size(size).build();
    }

    private MotuRequestStatus buildMotuRequestStatus(final StatusModeResponse statusResponse) throws MotuUrlException {
        final URI uri;
        try {
            if (null != statusResponse.getRemoteUri()) {
                uri = new URI(statusResponse.getRemoteUri());
            } else {
                uri = null;
            }
        } catch (final URISyntaxException ex) {
            throw new MotuUrlException("Invalid uri returned in status: " + statusResponse.getRemoteUri(), ex);
        }

        final Long size;
        if (null != statusResponse.getSize()) {
            size = Math.round(statusResponse.getSize() * MEGABITS_TO_BYTES);
        } else {
            size = null;
        }

        return ImmutableMotuRequestStatus.builder().requestId(Optional.ofNullable(statusResponse.getRequestId()).map(String::valueOf).orElse(null))
                .message(statusResponse.getMsg()).code(statusResponse.getCode()).statusType(statusResponse.getStatus()).remoteUri(uri).size(size)
                .build();
    }

}
