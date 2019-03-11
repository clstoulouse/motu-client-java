package cls.motu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.google.common.io.CountingInputStream;

import cls.motu.exceptions.DownloadServiceException;
import cls.motu.exceptions.DownloadServiceIrrecoverableException;
import cls.motu.exceptions.DownloadServiceRecoverableException;
import cls.motu.exceptions.MotuException;
import cls.motu.exceptions.MotuUrlException;
import cls.motu.objs.ImmutableMotuDownloadProductParameters;
import cls.motu.objs.ImmutableMotuProductReference;
import cls.motu.objs.MotuDownloadProductParameters;
import cls.motu.objs.MotuProductReference;
import cls.motu.objs.MotuRequestStatus;
import cls.motu.objs.RemoteMotu;
import cls.motu.properties.MotuClientProperties;
import cls.motu.services.MotuService;
import cls.motu.utils.MotuDescribeDatasetResponseParser;
import cls.motu.utils.MotuUrlsHelper;
import fr.cls.atoll.motu.api.message.xml.ErrorType;
import fr.cls.atoll.motu.api.message.xml.StatusModeType;
import okhttp3.ResponseBody;

/**
 * . <br>
 * <br>
 * Copyright : Copyright (c) 2019 <br>
 * <br>
 * Company : CLS (Collecte Localisation Satellites)
 * 
 * @author Sylvain MARTY
 * @version $Revision: 1456 $ - $Date: 2011-04-08 18:37:34 +0200 $
 */
@SpringBootApplication
@EnableAutoConfiguration
public class MotuClientApplication implements CommandLineRunner {

    private MotuService motuService;
    private MotuClientProperties motuClientProperties;
    private static final Logger log = LoggerFactory.getLogger(MotuService.class);

    public MotuService getMotuService() {
        return motuService;
    }

    public MotuClientProperties getMotuProperties() {
        return motuClientProperties;
    }

    private RemoteMotu buildMotuRemoteURI() throws MotuUrlException, URISyntaxException {
        final URI motuUri = new URI(getMotuProperties().getUrl().toString());
        RemoteMotu remoteMotu = MotuUrlsHelper.getRemoteMotuFromUri(motuUri, getMotuProperties().getUsername(), getMotuProperties().getPassword());
        return remoteMotu;
    }

    private MotuProductReference buildMotuProductReference(String... args) {
        String serviceName = "Mercator_Ocean_Model_Global-TDS";
        String productName = "dataset-mercator-psy4v3-gl12-bestestimate-sshst";
        if (args.length == 2) {
            serviceName = args[0];
            productName = args[1];
        }

        return ImmutableMotuProductReference.builder().service(serviceName).product(productName).build();
    }

    private MotuDownloadProductParameters buildMotuDownloadProductParametersForLastDay(MotuDescribeDatasetResponseParser describeProductResponse) {
        return ImmutableMotuDownloadProductParameters.builder().addAllVariables(describeProductResponse.getVariablesStandardNames())
                .lowestDepth(describeProductResponse.getAvailableDepthsAsSortedList().get(0))
                .highestDepth(describeProductResponse.getAvailableDepthsAsSortedList().get(1)).

                lowestLatitude(describeProductResponse.getLatBounds().getLeft()).highestLatitude(describeProductResponse.getLatBounds().getRight()).

                lowestLongitude(describeProductResponse.getLonBounds().getLeft()).highestLongitude(describeProductResponse.getLonBounds().getRight()).

                lowestTime(describeProductResponse.getTimeCoverage().getRight().minusSeconds(86400))
                .highestTime(describeProductResponse.getTimeCoverage().getRight()).build();
    }

    @Override
    public void run(String... args) {
        MotuDescribeDatasetResponseParser describeProductResponse = null;
        RemoteMotu remoteMotu = null;
        try {
            remoteMotu = buildMotuRemoteURI();
        } catch (MotuUrlException e) {
            log.error("MotuRemoteURI", e);
        } catch (URISyntaxException e) {
            log.error("MotuRemoteURI", e);
        }

        if (remoteMotu != null) {
            log.info("");
            // DescribeProduct
            MotuProductReference motuProductReference = buildMotuProductReference(args);
            try {
                describeProductResponse = describeProduct(remoteMotu, motuProductReference);
            } catch (MotuException e) {
                log.error("describeDataset", e);
            }

            log.info("");
            // GetSize
            MotuDownloadProductParameters motuDownloadProductParameters = buildMotuDownloadProductParametersForLastDay(describeProductResponse);
            try {
                long resultSize = getProductSize(remoteMotu, motuProductReference, motuDownloadProductParameters);
            } catch (MotuException e) {
                log.error("downloadProduct", e);
            }

            log.info("");
            // Download
            try {
                File result = downloadProduct(remoteMotu, motuProductReference, motuDownloadProductParameters);
            } catch (MotuException e) {
                log.error("downloadProduct", e);
            }
        }
    }

    public MotuDescribeDatasetResponseParser describeProduct(RemoteMotu remoteMotu, MotuProductReference motuProductReference) throws MotuException {
        log.info("MOTU START DescribeProduct: ");
        MotuDescribeDatasetResponseParser response = getMotuService().describeDataset(remoteMotu, motuProductReference);
        String jsonDescribeProduct = String.format("{variables:[%s], " + "lat:'%s', lon:'%s', time:'%s'" + "}",
                                                   String.join(", ", response.getVariablesStandardNames()),
                                                   response.getLatBounds().toString(),
                                                   response.getLonBounds().toString(),
                                                   response.getTimeCoverage().toString());
        log.info("DescribeProduct JSON: " + jsonDescribeProduct);
        log.info("MOTU END DescribeProduct: ");
        return response;
    }

    public long getProductSize(RemoteMotu remoteMotu,
                               MotuProductReference motuProductReference,
                               MotuDownloadProductParameters motuDownloadProductParameters)
            throws MotuException {
        log.info("MOTU START getProductSize: ");
        log.info("Download request parameters: " + motuDownloadProductParameters.toString());
        MotuRequestStatus response = getMotuService().getSize(remoteMotu, motuProductReference, motuDownloadProductParameters);
        long sizeResponse = response.getSize();
        log.info("Size JSON: {displaySize:'" + FileUtils.byteCountToDisplaySize(sizeResponse) + "', responseSize='" + sizeResponse + " B'}");
        log.info("MOTU END getProductSize");
        return sizeResponse;
    }

    public File downloadProduct(RemoteMotu remoteMotu,
                                MotuProductReference motuProductReference,
                                MotuDownloadProductParameters motuDownloadProductParameters)
            throws MotuException {
        log.info("MOTU START download");
        MotuRequestStatus response = getMotuService().sendDownloadProduct(remoteMotu, motuProductReference, motuDownloadProductParameters);
        log.info("Download request status: "
                + String.format("{code:%s, message:'%s', requestId:%s}", response.getCode(), response.getMessage(), response.getRequestId()));
        MotuRequestStatus terminalStatus = null;
        File tempLocation = null;
        try {
            terminalStatus = motuService.getRequestStatusUntilTerminalStateReached(remoteMotu,
                                                                                   motuProductReference,
                                                                                   response.getRequestId(),
                                                                                   Duration.of(10, ChronoUnit.SECONDS));
            checkMotuRequestStatus(terminalStatus);
            ResponseBody rbody = motuService.downloadExtractedProduct(remoteMotu, terminalStatus.getRemoteUri());
            CountingInputStream countingStream = new CountingInputStream(rbody.byteStream());

            tempLocation = File.createTempFile("." + terminalStatus.getRequestId(), ".nc");
            long writtenBytes = writeContentToTemporaryFile(countingStream, tempLocation);
            log.info("Download: {motuDownloadedFilePath:'" + tempLocation.getAbsolutePath() + "', displaySize:'"
                    + FileUtils.byteCountToDisplaySize(writtenBytes) + "', responseSize='" + writtenBytes + " B'}");
            log.info("MOTU END download");

        } catch (InterruptedException e) {
            log.error("downloadDataset", e);
        } catch (DownloadServiceException e) {
            log.error("downloadDataset", e);
        } catch (IOException e) {
            log.error("downloadDataset", e);
        }

        return tempLocation;
    }

    private static final int BUFFER_SIZE = 1024 * 4;

    private long writeContentToTemporaryFile(final InputStream inputStream, final File tempLocation) throws InterruptedException, IOException {
        log.debug("writing the content to temporary location '%s'...", tempLocation);
        long writtenBytes = 0L;
        try (final OutputStream outputStream = FileUtils.openOutputStream(tempLocation)) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (!Thread.currentThread().isInterrupted() && IOUtils.EOF != (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
                writtenBytes += n;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("The thread was interrupted");
            }
        }
        return writtenBytes;
    }

    private static final Set<ErrorType> retriableErrorTypes = EnumSet.of(ErrorType.SYSTEM,
                                                                         ErrorType.EXCEEDING_QUEUE_CAPACITY,
                                                                         ErrorType.EXCEEDING_USER_CAPACITY,
                                                                         ErrorType.INVALID_QUEUE_PRIORITY,
                                                                         ErrorType.SHUTTING_DOWN);

    public static MotuRequestStatus checkMotuRequestStatus(final MotuRequestStatus motuRequestStatus) throws DownloadServiceException {
        if (StatusModeType.ERROR == motuRequestStatus.getStatusType()) {
            final String message = "MOTU download product failed (" + motuRequestStatus.getCode() + ':' + motuRequestStatus.getErrorType() + "):"
                    + motuRequestStatus.getMessage();

            final ErrorType errorType = motuRequestStatus.getErrorType();

            if ((null != errorType) && retriableErrorTypes.contains(errorType)) {
                throw new DownloadServiceRecoverableException(message);
            } else {
                throw new DownloadServiceIrrecoverableException(message);
            }

        }

        return motuRequestStatus;
    }

    // DI Setters
    @Autowired
    public void setMotuClientProperties(final MotuClientProperties motuClientProperties_) {
        this.motuClientProperties = motuClientProperties_;
    }

    @Autowired
    public void setMotuService(final MotuService motuService_) {
        this.motuService = motuService_;
    }

    /**
     * .
     * 
     * @param args
     */
    public static void main(String[] args) {
        log.debug("Start Motuclient Java");
        SpringApplication.run(MotuClientApplication.class, args);
        log.debug("End Motuclient Java");
    }
}
