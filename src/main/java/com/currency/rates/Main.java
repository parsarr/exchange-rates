package com.currency.rates;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        String fileName = "eurofxref-hist.csv";

        Map<String, Map<LocalDate, String>> supportedCurrencies;
        String header;
        String[] headerTokens;
        CsvParser csvParser = new CsvParser();
        String userInput;
        Scanner scanner = new Scanner(System.in);

        csvParser.parseCsv(fileName);
        supportedCurrencies = csvParser.getSupportedCurrencies();
        header = csvParser.getHeader();
        headerTokens = csvParser.getHeaderTokens();

        do {
            System.out.println("Options");
            System.out.println("Type 1 to retrieve the rates of a specific date.");
            System.out.println("Type 2 to retrieve the highest rate of a currency within a specific region of dates.");
            System.out.println("Type 3 to retrieve the average rate of a currency within a specific region of dates.");
            System.out.println("Type 4 to convert an amount of money from a source currency to a target one using the exchange rates of a particular date.");
            System.out.println("Type Q or q to quit");

            userInput = scanner.next();
            switch (userInput) {
                case "1":
                    System.out.println("\nPlease provide a date with the format YYYY-MM-DD");
                    String date = scanner.next();
                    try {
                        LocalDate ld = LocalDate.parse(date);
                        System.out.println("\n" + header);
                        System.out.println(csvParser.retrieveRatesForSpecificDate(ld));
                        System.out.println("\n");
                    }
                    catch (DateTimeParseException e) {
                        //e.printStackTrace();
                        System.out.println("Invalid date\n");
                    }
                    break;
                case "2":
                case "3":
                    System.out.println("\nPlease provide a start date with the format YYYY-MM-DD");
                    String startDate = scanner.next();
                    LocalDate startLocalDate = null;
                    try {
                        startLocalDate = LocalDate.parse(startDate);
                        /*System.out.println("\n" + header);
                        System.out.println(csvParser.retrieveRatesForSpecificDate(ld));
                        System.out.println("\n");*/
                    }
                    catch (DateTimeParseException e) {
                        //e.printStackTrace();
                        System.out.println("Invalid date\n");
                        break;
                    }

                    System.out.println("\nPlease provide an end date with the format YYYY-MM-DD");
                    String endDate = scanner.next();
                    LocalDate endLocalDate = null;
                    try {
                        endLocalDate = LocalDate.parse(endDate);
                    }
                    catch (DateTimeParseException e) {
                        //e.printStackTrace();
                        System.out.println("Invalid date\n");
                        break;
                    }

                    System.out.println("\nPlease provide the currency");
                    String currency = scanner.next();
                    try {
                        //System.out.println(csvParser.getHighestRateForSpecificDates(startLocalDate, endLocalDate, currency.toUpperCase()) + "\n");
                        if (userInput.equalsIgnoreCase("2"))
                            System.out.println(csvParser.getStatsForSpecificDates(startLocalDate, endLocalDate, currency.toUpperCase(), true) + "\n");
                        else if (userInput.equalsIgnoreCase("3"))
                            System.out.println(csvParser.getStatsForSpecificDates(startLocalDate, endLocalDate, currency.toUpperCase(), false) + "\n");
                    }
                    catch (Exception e) {
                        //e.printStackTrace();
                        //System.out.println("Invalid date\n");
                        System.out.println(e.getMessage() + "\n");
                    }
                    break;
                case "4":
                    Double moneyAmount = 0.0;
                    System.out.println("\nPlease provide a date with the format YYYY-MM-DD");
                    String givenDate = scanner.next();
                    LocalDate givenLocalDate = null;
                    try {
                        givenLocalDate = LocalDate.parse(givenDate);
                    }
                    catch (DateTimeParseException e) {
                        System.out.println("Invalid date\n");
                        break;
                    }

                    System.out.println("\nPlease provide the source currency");
                    String sourceCurrency = scanner.next();

                    System.out.println("\nPlease provide the target currency");
                    String targetCurrency = scanner.next();

                    System.out.println("\nPlease provide the amount of money to be converted");
                    String amount = scanner.next();
                    try {
                        moneyAmount = Double.parseDouble(amount);
                    }
                    catch (Exception e) {
                        System.out.println("Invalid amount\n");
                        break;
                    }

                    try {
                        csvParser.convertCurrencies(givenLocalDate, sourceCurrency.toUpperCase(), targetCurrency.toUpperCase(), moneyAmount);
                    }
                    catch (Exception e) {
                        //e.printStackTrace();
                        //System.out.println("Invalid date\n");
                        System.out.println(e.getMessage() + "\n");
                    }
                    break;
            }




        } while (!userInput.equalsIgnoreCase("Q"));
    }
}
