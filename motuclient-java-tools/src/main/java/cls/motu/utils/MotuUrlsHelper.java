package cls.motu.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import cls.motu.exceptions.MotuUrlException;
import cls.motu.objs.ImmutableMotuProductReference;
import cls.motu.objs.ImmutableRemoteMotu;
import cls.motu.objs.MotuDownloadProductParameters;
import cls.motu.objs.MotuProductReference;
import cls.motu.objs.RemoteMotu;
import fr.cls.atoll.motu.api.message.MotuRequestParametersConstant;
import okhttp3.HttpUrl;

public final class MotuUrlsHelper {

    private MotuUrlsHelper() {
        // do nothing
    }

    private static final DateTimeFormatter TIMESTAMP_PARAMETERS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static RemoteMotu getRemoteMotuFromUri(final URI motuUri, final String username, final String password) throws MotuUrlException {
        try {
            final URI rootUri = new URI(motuUri.getScheme(), motuUri.getAuthority(), motuUri.getPath(), null, motuUri.getFragment());

            return ImmutableRemoteMotu.builder().rootUri(rootUri).username(username).password(password).build();
        } catch (final URISyntaxException ex) {
            throw new MotuUrlException("Unable to extract remote motu url", ex);
        }
    }

    public static MotuProductReference getProductReferenceFromUri(final URI motuUri) throws MotuUrlException {
        final HttpUrl parsedUri = HttpUrl.get(motuUri);

        if (null == parsedUri) {
            throw new MotuUrlException("Unable to parse motu url: " + motuUri);
        }

        final String service = parsedUri.queryParameter(MotuRequestParametersConstant.PARAM_SERVICE);
        final String product = parsedUri.queryParameter(MotuRequestParametersConstant.PARAM_PRODUCT);

        if (null == service) {
            throw new MotuUrlException("No '" + MotuRequestParametersConstant.PARAM_SERVICE + "' parameter found in motu url: " + motuUri);
        }

        if (null == product) {
            throw new MotuUrlException("No '" + MotuRequestParametersConstant.PARAM_PRODUCT + "' parameter found in motu url: " + motuUri);
        }

        return ImmutableMotuProductReference.builder().service(service).product(product).build();
    }

    public static URI buildDescribeProductUri(final RemoteMotu motu, final MotuProductReference product) throws MotuUrlException {
        final HttpUrl.Builder builder = createMotuActionUriBuilder(motu, MotuRequestParametersConstant.ACTION_DESCRIBE_PRODUCT);

        addProductQueryParameters(builder, product);

        return builder.build().uri();
    }

    public static URI buildDownloadProductUri(final RemoteMotu motu,
                                              final MotuProductReference product,
                                              final MotuDownloadProductParameters parameters)
            throws MotuUrlException {
        return buildDownloadProductUri(motu, product, parameters, MotuRequestParametersConstant.PARAM_MODE_STATUS);
    }

    public static URI buildDownloadProductUri(final RemoteMotu motu,
                                              final MotuProductReference product,
                                              final MotuDownloadProductParameters parameters,
                                              final String mode)
            throws MotuUrlException {
        final HttpUrl.Builder builder = createMotuActionUriBuilder(motu, MotuRequestParametersConstant.ACTION_PRODUCT_DOWNLOAD);

        builder.addQueryParameter(MotuRequestParametersConstant.PARAM_MODE, mode);

        addProductQueryParameters(builder, product);

        addDownloadProductQueryParameters(builder, parameters);

        return builder.build().uri();
    }

    public static URI buildGetStatusUri(final RemoteMotu motu, final MotuProductReference product, final String requestId) throws MotuUrlException {
        final HttpUrl.Builder builder = createMotuActionUriBuilder(motu, MotuRequestParametersConstant.ACTION_GET_REQUEST_STATUS);

        addProductQueryParameters(builder, product);

        builder.addQueryParameter(MotuRequestParametersConstant.PARAM_REQUEST_ID, requestId);

        return builder.build().uri();
    }

    public static URI buildGetSizeUri(final RemoteMotu motu, final MotuProductReference product, final MotuDownloadProductParameters parameters)
            throws MotuUrlException {
        final HttpUrl.Builder builder = createMotuActionUriBuilder(motu, MotuRequestParametersConstant.ACTION_GET_SIZE);

        addProductQueryParameters(builder, product);

        addDownloadProductQueryParameters(builder, parameters);

        return builder.build().uri();
    }

    private static void addProductQueryParameters(final HttpUrl.Builder builder, final MotuProductReference product) {
        builder.addQueryParameter(MotuRequestParametersConstant.PARAM_SERVICE, product.getService());
        builder.addQueryParameter(MotuRequestParametersConstant.PARAM_PRODUCT, product.getProduct());
    }

    private static void addDownloadProductQueryParameters(final HttpUrl.Builder builder, final MotuDownloadProductParameters parameters) {
        if (null != parameters.getVariables()) {
            parameters.getVariables().forEach(variable -> builder.addQueryParameter("variable", variable));
        }

        addTimestampQueryParameter(builder, parameters.getLowestTime(), MotuRequestParametersConstant.PARAM_START_DATE);

        addTimestampQueryParameter(builder, parameters.getHighestTime(), MotuRequestParametersConstant.PARAM_END_DATE);

        addDoubleQueryParameter(builder, parameters.getLowestLatitude(), MotuRequestParametersConstant.PARAM_LOW_LAT);

        addDoubleQueryParameter(builder, parameters.getHighestLatitude(), MotuRequestParametersConstant.PARAM_HIGH_LAT);

        addDoubleQueryParameter(builder, parameters.getLowestLongitude(), MotuRequestParametersConstant.PARAM_LOW_LON);

        addDoubleQueryParameter(builder, parameters.getHighestLongitude(), MotuRequestParametersConstant.PARAM_HIGH_LON);

        addDoubleQueryParameter(builder, parameters.getLowestDepth(), MotuRequestParametersConstant.PARAM_LOW_Z);

        addDoubleQueryParameter(builder, parameters.getHighestDepth(), MotuRequestParametersConstant.PARAM_HIGH_Z);
    }

    private static HttpUrl.Builder createMotuActionUriBuilder(final RemoteMotu motu, final String action) throws MotuUrlException {
        final HttpUrl parsedUri = HttpUrl.get(motu.getRootUri());

        if (null == parsedUri) {
            throw new MotuUrlException("Unable to use remote motu to build uris: " + motu);
        }

        return parsedUri.newBuilder().addQueryParameter(MotuRequestParametersConstant.PARAM_ACTION, action);
    }

    private static void addTimestampQueryParameter(final HttpUrl.Builder builder, final Instant nullableValue, final String queryParameter) {
        Optional.ofNullable(nullableValue).map(value -> TIMESTAMP_PARAMETERS_FORMATTER.format(value.atOffset(ZoneOffset.UTC)))
                .ifPresent(value -> builder.addQueryParameter(queryParameter, value));
    }

    private static void addDoubleQueryParameter(final HttpUrl.Builder builder, final double value, final String queryParameter) {
        builder.addQueryParameter(queryParameter, String.valueOf(value));
    }
}
