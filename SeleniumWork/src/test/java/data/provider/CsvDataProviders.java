package data.provider;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.opencsv.exceptions.CsvException;
import org.testng.annotations.DataProvider;

public class CsvDataProviders {

    @DataProvider(name = "csvData")
    public Object[][] getCsvData() throws IOException, CsvException {
        String csvFile = "src/test/resources/login-cred.csv"; // Adjust path as needed
        CSVReader reader = new CSVReader(new FileReader(csvFile));
        List<String[]> csvLines = reader.readAll();

        //Remove header
        csvLines.removeFirst();
        reader.close();

        // Convert List<String[]> to Object[][] for TestNG
        Object[][] data = new Object[csvLines.size()][];
        for (int i = 0; i < csvLines.size(); i++) {
            data[i] = csvLines.get(i);
        }
        return data;
    }
}