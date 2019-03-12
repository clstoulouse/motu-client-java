package cls.atoll.motu.utils;

import static cls.atoll.motu.utils.StreamUtils.nullableValue;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cls.atoll.motu.exceptions.MotuException;
import cls.atoll.motu.exceptions.MotuResponseParsingException;

public class MotuDescribeDatasetResponseParser {
    private final Element mainElement;
    private final DateTimeFormatter xmlDateTimeFormatter = DateTimeFormatter.ofPattern(MotuConstants.DATE_FORMAT_LONG).withZone(ZoneId.of("UTC"));
    private final DateTimeFormatter dateTimeFormatterLong = DateTimeFormatter.ofPattern(MotuConstants.DATE_FORMAT_LONG).withZone(ZoneId.of("UTC"));

    public MotuDescribeDatasetResponseParser(final Element mainElement) {
        this.mainElement = mainElement;
    }

    public static MotuDescribeDatasetResponseParser build(final InputStream stream) throws MotuException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(stream);

            final Element mainElement = doc.getDocumentElement();
            if (null == mainElement) {
                throw new MotuResponseParsingException("Unable to find XML root, in describe dataset response");
            }

            return new MotuDescribeDatasetResponseParser(mainElement);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new MotuResponseParsingException("Unable to build describe-dataset parser from xml", e);
        }
    }

    private Stream<Node> nodeListToStream(final NodeList nodeList) {
        if (null == nodeList) {
            return Stream.empty();
        }

        final ArrayList<Node> objects = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node item = nodeList.item(i);
            objects.add(item);
        }

        return objects.stream();
    }

    private <T extends Node> Stream<T> nodeListToStream(final NodeList nodeList, final Class<T> clazz) {
        return nodeListToStream(nodeList).filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Return the time coverage from the describeProduct XML request (first and last date of the period)
     */
    public Pair<Instant, Instant> getTimeCoverage() {
        final Stream<Element> nodeStream = nodeListToStream(mainElement.getElementsByTagName(MotuConstants.XML_TAG_TIME_COVERAGE), Element.class);
        return nodeStream.map(x -> {
            final Instant start = Optional.ofNullable(x.getAttribute("start")).map(y -> xmlDateTimeFormatter.parse(y, Instant::from)).orElse(null);
            final Instant end = Optional.ofNullable(x.getAttribute("end")).map(y -> xmlDateTimeFormatter.parse(y, Instant::from)).orElse(null);

            return Pair.of(start, end);
        }).findFirst().orElseGet(() -> Pair.of(null, null));
    }

    public Stream<Instant> getAvailableTimesAsStream() {
        final Stream<Node> nodeStream = nodeListToStream(mainElement.getElementsByTagName(MotuConstants.XML_TAG_AVAILABLE_TIMES));
        return nodeStream.map(node -> StringUtils.normalizeSpace(node.getTextContent())).flatMap(text -> Arrays.stream(text.split(",")))
                .flatMap(this::parseAvailableTime);
    }

    public List<Instant> getAvailableTimesAsSortedList() {
        final List<Instant> collect = new ArrayList<>();
        NodeList availableTimeNodeList = mainElement.getElementsByTagName(MotuConstants.XML_TAG_AVAILABLE_TIMES);
        if (availableTimeNodeList.getLength() == 1) {
            Node availableTimeNode = availableTimeNodeList.item(0);
            if (availableTimeNode != null) {
                String availableTimeNodeValue = availableTimeNode.getNodeValue();
                if (availableTimeNodeValue != null) {
                    String[] availableTimeList = availableTimeNodeValue.split(",");
                    if (availableTimeList.length > 0) {
                        String[] lastAvailableTimeValuePeriod = availableTimeList[availableTimeList.length - 1].split("/");
                        final Instant finalInstant = computeInstantFromStrDate(lastAvailableTimeValuePeriod[1]);
                        final Duration instantInterval = Duration.parse(lastAvailableTimeValuePeriod[2]);
                        final Instant secondToLastInstant = finalInstant.minus(instantInterval);
                        collect.add(finalInstant);
                        collect.add(secondToLastInstant);
                    }
                }
            }
        }
        return collect;
    }

    private Stream<Instant> parseAvailableTime(final String strPeriod) {
        final String[] periodSection = strPeriod.split("/");
        Instant startInstant = computeInstantFromStrDate(periodSection[0]);
        final Instant endInstant = computeInstantFromStrDate(periodSection[1]);
        final Duration instantInterval = Duration.parse(periodSection[2]);
        final List<Instant> parseInstantList = new ArrayList<>();

        while (startInstant.compareTo(endInstant) < 0) {
            parseInstantList.add(startInstant);
            startInstant = startInstant.plus(instantInterval);
        }
        return parseInstantList.stream();
    }

    private Instant computeInstantFromStrDate(final String strDate) {
        final TemporalAccessor temporalAccessor = dateTimeFormatterLong.parse(strDate);
        final Instant parseInstant;
        if (temporalAccessor.isSupported(HOUR_OF_DAY)) {
            parseInstant = Instant.from(temporalAccessor);
        } else {
            parseInstant = LocalDate.from(temporalAccessor).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        return parseInstant;
    }

    /**
     * Return the list of variables standard names
     */
    public List<String> getVariablesStandardNames() {

        final Stream<Element> variablesNodes = nodeListToStream(mainElement.getElementsByTagName(MotuConstants.XML_TAG_VARIABLES), Element.class);
        final Stream<Element> variableNodes = variablesNodes
                .flatMap(elemVars -> nodeListToStream(elemVars.getElementsByTagName(MotuConstants.XML_TAG_VARIABLE), Element.class));

        return variableNodes.flatMap(nullableValue(node -> node.getAttribute(MotuConstants.XML_ATT_STANDARD))).filter(x -> !StringUtils.isBlank(x))
                .collect(Collectors.toList());
    }

    public Stream<Double> getAvailableDepthsAsStream() {
        final Stream<Node> nodeStream = nodeListToStream(mainElement.getElementsByTagName(MotuConstants.XML_TAG_AVAILABLE_DEPTHS));
        return nodeStream.map(node -> StringUtils.normalizeSpace(node.getTextContent())).flatMap(text -> Arrays.stream(text.split(";")))
                .map(x -> "surface".equalsIgnoreCase(x) ? "0" : x).map(Double::valueOf);
    }

    public List<Double> getAvailableDepthsAsSortedList() {
        final List<Double> collect = getAvailableDepthsAsStream().distinct().collect(Collectors.toList());
        Collections.sort(collect);
        return collect;
    }

    public Pair<Optional<String>, Optional<String>> getAxisBoundsByAxisType(final String axisType) {
        final Stream<Element> nodeStream = nodeListToStream(mainElement.getElementsByTagName(MotuConstants.XML_TAG_GEO_COV), Element.class);
        final Stream<Element> axisNodes = nodeStream
                .flatMap(elemVars -> nodeListToStream(elemVars.getElementsByTagName(MotuConstants.XML_TAG_AXIS), Element.class));
        return axisNodes.filter(x -> Optional.ofNullable(x.getAttribute("axisType")).map(axisType::equals).orElse(false)).map(x -> {
            final Optional<String> start = Optional.ofNullable(x.getAttribute("lower"));
            final Optional<String> end = Optional.ofNullable(x.getAttribute("upper"));

            return Pair.of(start, end);
        }).findFirst().orElseGet(() -> Pair.of(null, null));
    }

    public Pair<Double, Double> getLatBounds() {
        final Pair<Optional<String>, Optional<String>> rawBounds = getAxisBoundsByAxisType("Lat");

        return Pair.of(rawBounds.getLeft().map(Double::valueOf).orElse(null), rawBounds.getRight().map(Double::valueOf).orElse(null));
    }

    public Pair<Double, Double> getLonBounds() {
        final Pair<Optional<String>, Optional<String>> rawBounds = getAxisBoundsByAxisType("Lon");

        return Pair.of(rawBounds.getLeft().map(Double::valueOf).orElse(null), rawBounds.getRight().map(Double::valueOf).orElse(null));
    }

    public Pair<Double, Double> getHeightBounds() {
        final Pair<Optional<String>, Optional<String>> rawBounds = getAxisBoundsByAxisType("Height");

        return Pair.of(rawBounds.getLeft().map(Double::valueOf).orElse(null), rawBounds.getRight().map(Double::valueOf).orElse(null));
    }
}
