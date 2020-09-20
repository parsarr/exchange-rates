package com.currency.rates;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvParserTest {

    private static Map<String, Map<LocalDate, String>> supportedCurrencies;
    private static String header;
    private static String[] headerTokens;
    private static CsvParser csvParser;

    @BeforeAll
    static void setUp() {
        String fileName = "eurofxref-hist.csv";
        //String fileName = "rates.csv";

        try {
            csvParser = new CsvParser();
            csvParser.parseCsv(fileName);
            supportedCurrencies = csvParser.getSupportedCurrencies();
            header = csvParser.getHeader();
            headerTokens = csvParser.getHeaderTokens();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @DisplayName("Parsed header matches the file's header")
    @Test
    void checkHeader() {
        // the expectedHeader as it was copied from csv's first line
        String expectedHeader = "Date,USD,JPY,BGN,CYP,CZK,DKK,EEK,GBP,HUF,LTL,LVL,MTL,PLN,ROL,RON,SEK,SIT,SKK,CHF,ISK,NOK,HRK,RUB,TRL,TRY,AUD,BRL,CAD,CNY,HKD,IDR,ILS,INR,KRW,MXN,MYR,NZD,PHP,SGD,THB,ZAR,";

        assertEquals(expectedHeader, header);
    }

    @DisplayName("Supported currencies' size matches the number of currencies on the header")
    @Test
    void checkNumberOfSupportedCurrencies() {
        assertEquals(41, supportedCurrencies.size());
    }

    @DisplayName("The number of rates for a currency matches those on the file")
    @Test
    void checkNumberOfEntriesInCurrencyMap() {
        String currency = "USD";
        // this value confirms what Excel was reporting when a column of rates par the header was selected
        assertEquals(5555, supportedCurrencies.get(currency).size());
    }

    @DisplayName("Get rates for a certain date")
    @Test
    void getRatesForDate() {
        // the rates from the first line of the csv file
        String expectedRates = "2020-09-14,1.1876,125.82,1.9558,N/A,26.66,7.4398,N/A,0.9219,357.65,N/A,N/A,N/A,4.4504,N/A,4.858,10.4178,N/A,N/A,1.0768,160,10.6933,7.5368,89.5924,N/A,8.8997,1.6327,6.3109,1.5641,8.0987,9.2041,17671.49,4.0807,87.3415,1404.73,25.1792,4.9232,1.7739,57.587,1.6207,37.172,19.7876,";
        LocalDate date = LocalDate.parse("2020-09-14");

        assertEquals(expectedRates, csvParser.retrieveRatesForSpecificDate(date));
    }

    @DisplayName("If there are no rates, throw Runtime Exception")
    @Test
    void getRatesForInvalidDate() {

        String expectedExceptionMessage = "There are no valid rates for the given date";
        // no rates expected for a Sunday
        LocalDate date = LocalDate.parse("2020-09-13");

        Exception exception = assertThrows(RuntimeException.class, () -> csvParser.retrieveRatesForSpecificDate(date));
        String actualExceptionMessage = exception.getMessage();

        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }

    @DisplayName("Get average rate for a date range")
    @Test
    void getAverageRateForDateRange() {
        String currency = "USD";
        LocalDate start = LocalDate.parse("2020-09-07");
        LocalDate end = LocalDate.parse("2020-09-14");

        // value matching the average appearing on Excel's status bar when these cells were selected
        assertEquals(1.1827, csvParser.getStatsForSpecificDates(start, end, currency, false));
    }

    @DisplayName("Get average rate for a 10 year period")
    @Test
    void getTenYearAverageRate() {
        String currency = "USD";
        LocalDate start = LocalDate.parse("2010-09-07");
        LocalDate end = LocalDate.parse("2020-09-14");

        // value matching the average appearing on Excel's status bar when these cells were selected
        assertEquals(1.1213, csvParser.getStatsForSpecificDates(start, end, currency, false));
    }

    @DisplayName("Get the highest rate in the last 20 years")
    @Test
    void getHighestTwentyYearRate() {
        LocalDate start = LocalDate.parse("2000-09-07");
        LocalDate end = LocalDate.parse("2020-09-14");
        String currency = "USD";

        // value matching the max appearing on Excel's status bar when these cells were selected
        assertEquals(1.599, csvParser.getStatsForSpecificDates(start, end, currency, true));
    }

    @DisplayName("Get the highest rate in the last week")
    @Test
    void getHighestRateForLastWeek() {
        LocalDate start = LocalDate.parse("2020-09-07");
        LocalDate end = LocalDate.parse("2020-09-14");
        String currency = "USD";

        // value matching the max appearing on Excel's status bar when these cells were selected
        assertEquals(1.1876, csvParser.getStatsForSpecificDates(start, end, currency, true));
    }

    @DisplayName("Throw a Runtime Exception for an Invalid Currency")
    @Test
    void getHighestRateForInvalidCurrency() {
        LocalDate start = LocalDate.parse("2020-09-07");
        LocalDate end = LocalDate.parse("2020-09-14");
        String currency = "USB";

        String expectedExceptionMessage = "No rates found, invalid currency provided";
        Exception exception = assertThrows(RuntimeException.class, () -> csvParser.getStatsForSpecificDates(start, end, currency, true));
        String actualExceptionMessage = exception.getMessage();

        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }

    @DisplayName("Convert from 10 GBP to USD")
    @Test
    void convertPoundsToDollars() {
        LocalDate date = LocalDate.parse("2020-09-11");
        String sourceCurrency = "GBP";
        String targetCurrency = "USD";
        Double amount = 10.0;

        assertEquals(12.8279, csvParser.convertCurrencies(date, sourceCurrency, targetCurrency, amount));
    }

    @DisplayName("A negative amount to convert will throw an Exception")
    @Test
    void convertANegativeAmount() {
        LocalDate date = LocalDate.parse("2020-09-11");
        String sourceCurrency = "GBP";
        String targetCurrency = "USD";
        Double amount = -10.0;

        String expectedExceptionMessage = "Invalid amount provided, aborting conversion";
        Exception exception = assertThrows(RuntimeException.class, () -> csvParser.convertCurrencies(date, sourceCurrency, targetCurrency, amount));
        String actualExceptionMessage = exception.getMessage();

        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }

    @DisplayName("A conversion on an invalid date will throw an Exception")
    @Test
    void convertOnInvalidDate() {
        LocalDate date = LocalDate.parse("2020-09-12");
        String sourceCurrency = "GBP";
        String targetCurrency = "USD";
        Double amount = 10.0;

        String expectedExceptionMessage = "The rate for the source currency is not applicable, aborting conversion";
        Exception exception = assertThrows(RuntimeException.class, () -> csvParser.convertCurrencies(date, sourceCurrency, targetCurrency, amount));
        String actualExceptionMessage = exception.getMessage();

        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }

    @DisplayName("A conversion to a target currency with N/A rates will throw an Exception")
    @Test
    void convertToInvalidTarget() {
        LocalDate date = LocalDate.parse("2020-09-11");
        String sourceCurrency = "GBP";
        String targetCurrency = "CYP";
        Double amount = 10.0;

        String expectedExceptionMessage = "The rate for the target currency is not applicable, aborting conversion";
        Exception exception = assertThrows(RuntimeException.class, () -> csvParser.convertCurrencies(date, sourceCurrency, targetCurrency, amount));
        String actualExceptionMessage = exception.getMessage();

        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }
}