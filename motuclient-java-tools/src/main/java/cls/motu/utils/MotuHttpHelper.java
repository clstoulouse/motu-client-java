package cls.motu.utils;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import cls.motu.exceptions.MotuHttpStatusCodeException;
import cls.motu.exceptions.MotuResponseParsingException;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MotuHttpHelper {

    private MotuHttpHelper() {
        // do nothing
    }

    public static String getHttpUrlFromResponse(final Response response) {
        return Optional.ofNullable(response.request()).map(Request::url).map(HttpUrl::toString).orElse(null);
    }

    /**
     * Gets the response body from a {@link Response} object only if the HTTP response is valid (success HTTP
     * status code and non-empty body).
     *
     * @param response Response to check.
     * @return The response body.
     * @throws MotuHttpStatusCodeException If the response has a non-success HTTP status code or an empty
     *                                     body.
     */
    public static ResponseBody getResponseBodyIfValid(final Response response) throws MotuHttpStatusCodeException {
        final String httpUrl = getHttpUrlFromResponse(response);

        if (!response.isSuccessful()) {
            throw new MotuHttpStatusCodeException(response.code());
        }
        if (null == response.body()) {
            throw new MotuHttpStatusCodeException("Empty body returned after a call to '" + httpUrl + '\'', response.code());
        }

        return response.body();
    }

    /**
     * Checks if a response to a motu request is in fact CAS login form.
     *
     * @param response HTTP response
     * @return {@code true} if a cas login form was detected.
     */
    public static boolean checkIfCasLoginForm(final Response response) {
        final boolean isLoginUrl = response.request().url().encodedPath().endsWith("/login");
        final Boolean isHtml = Optional.ofNullable(response.body())
                                       .map(ResponseBody::contentType)
                                       .map(mediaType -> "text".equals(mediaType.type()) && "html".equals(mediaType.subtype()))
                                       .orElse(false);
        return isLoginUrl && isHtml;
    }

    /**
     * Parses the HTML content of CAS login form and builds a a login request using the provided username and
     * password.
     *
     * @param originalUrl          Url of the CAS login form.
     * @param htmlContent          HTML content to parse.
     * @param casLoginFormSelector CSS Selector that points to the CAS login form.
     * @param username             CAS username.
     * @param password             CAS password.
     * @return A CAS login request to execute in order to log in with CAS.
     * @throws MotuResponseParsingException If The html content could not be parsed to a valid CAS login form.
     */
    public static Request buildLoginRequest(final String originalUrl,
                                            final String htmlContent,
                                            final String casLoginFormSelector,
                                            final String username,
                                            final String password) throws MotuResponseParsingException {
        // Parsing the HTML to get the CAS endpoint and the hidden values from the login form
        final Document html = Jsoup.parse(htmlContent, originalUrl);
        final Elements form = html.select(casLoginFormSelector);
        final String casUrl = form.attr("abs:action");
        if (StringUtils.isBlank(casUrl)) {
            throw new MotuResponseParsingException(
                    "MOTU request redirected to cas url '" + originalUrl + "' but no CAS login target could be parsed from the HTML");
        }
        final Map<String, String> hiddenValues = form.select("input[type=hidden]")
                                                     .stream()
                                                     .filter(x -> (null != x.attr("name")) && (null != x.attr("value")))
                                                     .collect(Collectors.toMap(x -> x.attr("name"), x -> x.attr("value")));

        // Constructing the POST request body
        FormBody.Builder formBodyBuilder = new FormBody.Builder().add("username", username).add("password", password);
        for (final Map.Entry<String, String> entry : hiddenValues.entrySet()) {
            formBodyBuilder = formBodyBuilder.add(entry.getKey(), entry.getValue());
        }

        // Constructing the POST request
        return new Request.Builder().url(casUrl).post(formBodyBuilder.build()).build();
    }
}
