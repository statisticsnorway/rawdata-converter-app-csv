package no.ssb.rawdata.converter.app.csv;

import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.csv.CsvParserSettings;
import no.ssb.avro.convert.csv.CsvToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.app.csv.schema.CsvSchemaAdapter;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ConversionResult.ConversionResultBuilder;
import no.ssb.rawdata.converter.core.convert.RawdataConverter;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.core.exception.RawdataConverterException;
import no.ssb.rawdata.converter.core.schema.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.schema.DcManifestSchemaAdapter;
import no.ssb.rawdata.converter.util.RawdataMessageAdapter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static no.ssb.rawdata.converter.util.RawdataMessageAdapter.posAndIdOf;

@Slf4j
public class CsvRawdataConverter implements RawdataConverter {

    private static final String RAWDATA_ITEMNAME_ENTRY = "entry";
    private static final String FIELDNAME_DC_MANIFEST = "dcManifest";
    private static final String FIELDNAME_CSV_DATA = "data";

    private final CsvRawdataConverterConfig converterConfig;
    private final ValueInterceptorChain valueInterceptorChain;

    private DcManifestSchemaAdapter dcManifestSchemaAdapter;
    private CsvSchemaAdapter csvSchemaAdapter;
    private Schema targetAvroSchema;

    public CsvRawdataConverter(CsvRawdataConverterConfig converterConfig, ValueInterceptorChain valueInterceptorChain) {
        this.converterConfig = converterConfig;
        this.valueInterceptorChain = valueInterceptorChain;
    }

    @Override
    public void init(Collection<RawdataMessage> sampleRawdataMessages) {
        log.info("Determine target avro schema from {}", sampleRawdataMessages);
        RawdataMessage sample = sampleRawdataMessages.stream()
          .findFirst()
          .orElseThrow(() ->
            new CsvRawdataConverterException("Unable to determine target avro schema since no sample rawdata messages were supplied. Make sure to configure `converter-settings.rawdata-samples`")
          );

        RawdataMessageAdapter msg = new RawdataMessageAdapter(sample);
        dcManifestSchemaAdapter = DcManifestSchemaAdapter.of(sample);
        csvSchemaAdapter = CsvSchemaAdapter.of(sample, RAWDATA_ITEMNAME_ENTRY);
        log.info("Data column names: {}", csvSchemaAdapter.getHeaders());

        String targetNamespace = "dapla.rawdata." + msg.getTopic().orElse("csv");

        targetAvroSchema = new AggregateSchemaBuilder(targetNamespace)
          .schema(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter.getDcManifestSchema())
          .schema(FIELDNAME_CSV_DATA, csvSchemaAdapter.getTargetSchema())
          .build();
    }

    public DcManifestSchemaAdapter dcMetadataSchemaAdapter() {
        if (dcManifestSchemaAdapter == null) {
            throw new IllegalStateException("dcManifestSchemaAdapter is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return dcManifestSchemaAdapter;
    }

    @Override
    public Schema targetAvroSchema() {
        if (targetAvroSchema == null) {
            throw new IllegalStateException("targetAvroSchema is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return targetAvroSchema;
    }

    @Override
    public boolean isConvertible(RawdataMessage rawdataMessage) {
        return true;
    }

    @Override
    public ConversionResult convert(RawdataMessage rawdataMessage) {
        ConversionResultBuilder resultBuilder = ConversionResult.builder(new GenericRecordBuilder(targetAvroSchema));
        addDcManifest(rawdataMessage, resultBuilder);
        convertCsvData(rawdataMessage, csvSchemaAdapter, resultBuilder);
        return resultBuilder.build();
    }

    void addDcManifest(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        resultBuilder.withRecord(FIELDNAME_DC_MANIFEST, dcMetadataSchemaAdapter().newRecord(rawdataMessage));
    }

    void convertCsvData(RawdataMessage rawdataMessage, CsvSchemaAdapter csvSchema, ConversionResultBuilder resultBuilder) {
        byte[] data = rawdataMessage.get(RAWDATA_ITEMNAME_ENTRY);
        CsvParserSettings csvParserSettings = new CsvParserSettings().configure(converterConfig.getCsvSettings());
        csvParserSettings.headers(csvSchema.getHeaders());

        try (CsvToRecords records = new CsvToRecords(new ByteArrayInputStream(data), csvSchema.getItemSchema(), csvParserSettings)
          .withValueInterceptor(valueInterceptorChain::intercept)) {

            List<GenericRecord> dataItems = new ArrayList<>();
            records.forEach(dataItems::add);
            resultBuilder.appendCounter("totalLinesCount", dataItems.size());
            resultBuilder.withRecord(FIELDNAME_CSV_DATA, csvSchema.toTargetRecord(dataItems));

        }
        catch (Exception e) {
            resultBuilder.addFailure(e);
            throw new CsvRawdataConverterException("Error converting CSV data at " + posAndIdOf(rawdataMessage), e);
        }
    }

    public static class CsvRawdataConverterException extends RawdataConverterException {
        public CsvRawdataConverterException(String msg) {
            super(msg);
        }
        public CsvRawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}