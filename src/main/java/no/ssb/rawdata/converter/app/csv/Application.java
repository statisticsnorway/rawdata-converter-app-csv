package no.ssb.rawdata.converter.app.csv;

import io.micronaut.runtime.Micronaut;
import no.ssb.rawdata.converter.app.AbstractRawdataConverterApplication;

public class Application extends AbstractRawdataConverterApplication {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

}