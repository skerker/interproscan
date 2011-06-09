package uk.ac.ebi.interpro.scan.io.pirsf;

import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to read in the pirsf.dat file.
 *
 * @author Matthew Fraser
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class PirsfDatFileParser implements Serializable {

    /*
     * Example file content:
     * >PIRSF000077
     * Thioredoxin
     * 110.136452241715 9.11541109440914 20.3 167.482261208577 57.6586203540026
     * BLAST: No
     *
     * [where "110.136452241715 9.11541109440914 20.3 167.482261208577 57.6586203540026" is
     * lm=mean(L); lsd=Std(L); smin=min(S); sm=mean(S); ssd=Std(S)]
     *
     * [where L is the query sequence length and S is the query HMM score.
     * mean(L) and std(L) are for the length of the family
     * mean(S), std(S) and min(S) are for the HMM score of the family]
     */

    private static final Logger LOGGER = Logger.getLogger(PirsfDatFileParser.class.getName());

    private static final Pattern PIRSF_DAT_PATTERN = Pattern.compile("^>PIRSF[0-9]{6}$");

    public Map<String, PirsfDatRecord> parse(Resource pirsfDatFileResource) throws IOException {
        if (pirsfDatFileResource == null) {
            throw new NullPointerException("Resource is null");
        }
        if (!pirsfDatFileResource.exists()) {
            throw new IllegalStateException(pirsfDatFileResource.getFilename() + " does not exist");
        }
        if (!pirsfDatFileResource.isReadable()) {
            throw new IllegalStateException(pirsfDatFileResource.getFilename() + " is not readable");
        }
        final Map<String, PirsfDatRecord> data = new HashMap<String, PirsfDatRecord>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(pirsfDatFileResource.getInputStream()));
            String line = null;
            String modelAccession = null;
            String modelName = null;
            String[] values = null;
            boolean isBlastRequired = false;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                Matcher modelStart = PIRSF_DAT_PATTERN.matcher(line);
                if (modelStart.find()) {
                    // New accession
                    row = 1;
                    modelAccession = line.substring(1);
                    modelName = null;
                    values = null;
                    isBlastRequired = false;
                } else if (row == 2) {
                    // Model name
                    modelName = line;
                } else if (row == 3) {
                    values = line.split("\\s+");
                } else if (row == 4 && line.startsWith("BLAST: ")) {
                    int index = line.indexOf(":");
                    if (index > -1 && line.length() >= index + 1) {
                        line = line.substring(index + 1).trim();
                    }
                    isBlastRequired = (true ? line.equalsIgnoreCase("YES") : false);
                    data.put(modelAccession, new PirsfDatRecord(modelAccession, modelName, values, isBlastRequired));
                } else {
                    LOGGER.warn("Unexpected line in pirsf.dat: " + line);
                }
                row++;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return data;
    }


}