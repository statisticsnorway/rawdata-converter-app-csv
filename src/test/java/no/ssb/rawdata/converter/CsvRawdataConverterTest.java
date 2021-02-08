package no.ssb.rawdata.converter;

import no.ssb.avro.convert.csv.CsvParserSettings;
import no.ssb.rawdata.converter.app.csv.CsvRawdataConverter;
import no.ssb.rawdata.converter.app.csv.CsvRawdataConverterConfig;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.test.message.RawdataMessageFixtures;
import no.ssb.rawdata.converter.test.message.RawdataMessages;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class CsvRawdataConverterTest {

    static RawdataMessageFixtures fixtures;

    @BeforeAll
    static void loadFixtures() {
        fixtures = RawdataMessageFixtures.init("private");
    }

    @Disabled
    @Test
    void shouldConvertRawdataMessages() {
        RawdataMessages messages = fixtures.rawdataMessages("private");
        CsvRawdataConverterConfig config = new CsvRawdataConverterConfig();
        config.getCsvSettings().put(CsvParserSettings.DELIMITERS, ";");
        config.getCsvSettings().put(CsvParserSettings.COLUMN_HEADERS_PRESENT, false);

        CsvRawdataConverter converter = new CsvRawdataConverter(config, new ValueInterceptorChain());
        converter.init(messages.index().values());
        ConversionResult res = converter.convert(messages.index().get("0177199c-150d-0000-0000-000000000002"));
    }

}
