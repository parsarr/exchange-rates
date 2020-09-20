package com.currency.rates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CsvParser {

    private Map<String, Map<LocalDate, String>> supportedCurrencies;
    private String header;
    private String[] headerTokens;

    public CsvParser() {
        this.supportedCurrencies = new HashMap<>();
    }

    public Map<String, Map<LocalDate, String>> getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(Map<String, Map<LocalDate, String>> supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
    }

    public String getHeader() {
        return header;
    }

    public String[] getHeaderTokens() {
        return headerTokens;
    }

    // this method will load the contents of the csv file in memory
    public void parseCsv(String filename) {

        ClassLoader classLoader = CsvParser.class.getClassLoader();

        // try to load the csv file from maven's resources folder
        try (InputStream inputStream = classLoader.getResourceAsStream(filename);
             InputStreamReader streamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(streamReader)) {

            // since this is a comma separated values file every line of text will be split using that delimiter
            header = reader.readLine();
            if (header != null) {
                headerTokens = header.split(",");

                // every string token from the header, except the date tag, will be used to initialise a nested hash map
                // the key set on the "outer" hash map will be the set of currency codes that were retrieved from the csv file's header
                // the values will be the "inner" hash maps, which will be using a date as a key and the exchange rate for that day as a value
                for (String token : headerTokens) {
                    if (!token.equalsIgnoreCase("Date")) {
                        supportedCurrencies.put(token.toUpperCase(), new HashMap<>());
                    }
                }
            }

            // once the currency codes are in place the rest of the file gets processed in order to populate the "inner" hash maps
            String dailyRates;
            while ((dailyRates = reader.readLine()) != null) {
                String[] ratesTokens = dailyRates.split(",");
                LocalDate date = LocalDate.parse(ratesTokens[0]);
                for (int i = 1; i < ratesTokens.length; i++) {
                    String rate = ratesTokens[i];
                    HashMap<LocalDate, String> ratesMap = (HashMap<LocalDate, String>) supportedCurrencies.get(headerTokens[i]);
                    ratesMap.put(date, rate);
                    supportedCurrencies.put(headerTokens[i], ratesMap);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // this is essentially the inverse of the process that took place when the "inner" hash maps
    // by traversing the array of header's tokens every rate gets retrieved from its respective hash map
    public String retrieveRatesForSpecificDate(LocalDate date) {

        StringBuilder builder = new StringBuilder();
        //builder.append(header + "\n");
        builder.append(date + ",");
        Arrays.stream(headerTokens).skip(1).forEach(currency -> builder.append(supportedCurrencies.get(currency).get(date) + ","));
        String dailyRates = builder.toString();
        if (validRates(dailyRates))
            return dailyRates;
        else throw new RuntimeException("There are no valid rates for the given date");
    }

    // this is the case where exchange rates get retrieved on a day were there are actually no rates, i.e during a weekend
    private boolean validRates(String dailyRates) {
        String[] tokens = dailyRates.split(",");
        for (String str : tokens) {
            if (str == null || str.equalsIgnoreCase("null"))
                return false;
        }

        return true;
    }

    // this method is acting as a protective barrier prior to the invocation of two recursive methods that will
    // either retrieve the highest rate or the average rate for a specified period of time
    public Double getStatsForSpecificDates(LocalDate start, LocalDate end, String currency, boolean retrieveHighestRate) {

        Double result = 0.0;
        Double total = 0.0;
        int applicableDays = 0;

        HashMap<LocalDate, String> rates = (HashMap<LocalDate, String>) supportedCurrencies.get(currency);

        // if the given currency is not part of the supported ones abort
        if (rates == null || rates.isEmpty())
            throw new RuntimeException("No rates found, invalid currency provided");

        if (start.isBefore(end) || start.isEqual(end)) {
            // the number of days within the given date range helps to identify cases where deep recursion will
            // lead to stack overflow conditions
            long numberOfDaysInRange = ChronoUnit.DAYS.between(start, end);

            LocalDate tempStart = start;
            LocalDate tempEnd = end;

            int thousands = (int) (numberOfDaysInRange / 1000);
            int remainingDays = (int) (numberOfDaysInRange % 1000);

            // if it is necessary, the recursive methods will be invoked for 1000 iterations / 1000 days
            // then the date range will shift to the next frame of 1000 days and so on
            if (numberOfDaysInRange > 1000) {
                for (int i = 0; i < thousands; i++) {
                    if (i > 0)
                        tempStart = tempStart.plusDays(1000);
                    tempEnd = tempStart.plusDays(1000);
                    if (retrieveHighestRate)
                        result = getHighestRate(tempStart, tempEnd, rates, result);
                    else
                        result = getAverageRate(tempStart, tempEnd, rates, total, applicableDays);
                }

                tempStart = tempEnd;
                tempEnd = tempStart.plusDays(remainingDays);
            }

            if (retrieveHighestRate)
                result = getHighestRate(tempStart, tempEnd, rates, result);
            else
                result = getAverageRate(tempStart, tempEnd, rates, total, applicableDays);
        } else
            throw new RuntimeException("Invalid date range");

        return result;
    }

    // recursive method that keeps track of the value of the highest exchange rate
    private Double getHighestRate(LocalDate start, LocalDate end, HashMap<LocalDate, String> rates, Double highestRate) {

        if (start.isBefore(end) || start.isEqual(end)) {
            String rate = getRate(start, rates);
            // days with non-applicable rates are skipped
            if (!rate.equals("") && !rate.equalsIgnoreCase("N/A")) {
                Double currentRate = Double.parseDouble(rate);
                if (currentRate > highestRate)
                    highestRate = currentRate;
            }
            start = start.plusDays(1);
        }

        return (start.isAfter(end)) ? highestRate : getHighestRate(start, end, rates, highestRate);
    }

    // recursive method that keeps track of the properties required to calculate the average exchange rate
    private Double getAverageRate(LocalDate start, LocalDate end, HashMap<LocalDate, String> rates, Double total, int applicableDays) {
        Double average = 0.0;

        // when the date range has been exhausted
        if (start.isAfter(end)) {
            // make sure that division by 0 is avoided
            if (applicableDays != 0) {
                average = total / applicableDays;
                // the result gets formatted in order to maintain 4 decimal points and match the format of the values from the csv file
                String avg = String.format("%.4f", average);
                average = Double.parseDouble(avg);
            }
        } else {
            String rate = getRate(start, rates);
            if (!rate.equals("") && !rate.equalsIgnoreCase("N/A")) {
                total += Double.parseDouble(rate);
                applicableDays++;
            }
            average = getAverageRate(start.plusDays(1), end, rates, total, applicableDays);
        }

        return average;
    }

    // helper method to retrieve the exchange rate
    private String getRate(LocalDate date, HashMap<LocalDate, String> rates) {
        if (rates.containsKey(date))
            return rates.get(date);
        else
            return "";
    }

    public Double convertCurrencies(LocalDate date, String sourceCurrency, String targetCurrency, Double amount) {


        Double exchangedAmount = 0.0;

        if (amount <= 0)
            throw new RuntimeException("Invalid amount provided, aborting conversion");

        HashMap<LocalDate, String> sourceRates = (HashMap<LocalDate, String>) supportedCurrencies.get(sourceCurrency);
        HashMap<LocalDate, String> targetRates = (HashMap<LocalDate, String>) supportedCurrencies.get(targetCurrency);

        if (sourceRates == null || sourceRates.isEmpty())
            throw new RuntimeException("No rates found, invalid source currency provided");

        if (targetRates == null || targetRates.isEmpty())
            throw new RuntimeException("No rates found, invalid target currency provided");

        String sourceRate = getRate(date, sourceRates);
        String targetRate = getRate(date, targetRates);

        if (sourceRate.equals("") || sourceRate.equalsIgnoreCase("N/A"))
            throw new RuntimeException("The rate for the source currency is not applicable, aborting conversion");

        if (targetRate.equals("") || targetRate.equalsIgnoreCase("N/A"))
            throw new RuntimeException("The rate for the target currency is not applicable, aborting conversion");

        // the csv file contains the rates that represent what is the equivalent of 1 EUR to the rest of the currencies
        // since we are not converting to euros, we have to invert the source currency's rate
        Double inverseSourceRate = 1 / Double.parseDouble(sourceRate);
        exchangedAmount = inverseSourceRate * Double.parseDouble(targetRate);
        Double result = exchangedAmount * amount;
        String resultString = String.format("%.4f", result);
        result = Double.parseDouble(resultString);
        System.out.println("The amount of " + amount + " " + sourceCurrency + " on " + date + " is equal to " + result + " " + targetCurrency + "\n");

        return result;
    }
}
