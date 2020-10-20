package no.ssb.rawdata.converter.app.csv;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("rawdata.converter.app.csv")
@Data
public class CsvRawdataConverterConfig {

    /**
     * Optional csv parser settings overrides.
     * E.g. allowing to explicitly specify the delimiter character
     */
    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    private Map<String, Object> csvSettings = new HashMap<>();

}