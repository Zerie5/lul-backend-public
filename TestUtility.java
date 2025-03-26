package com.test.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A utility class containing helper methods for testing purposes.
 * This class is completely independent of the main application.
 */
public class TestUtility {
    
    private static final Random random = new Random();
    
    /**
     * Generates a random string of specified length.
     * 
     * @param length The length of the random string
     * @return A random alphanumeric string
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * Generates a random UUID string.
     * 
     * @return A random UUID as string
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates a random integer within a specified range.
     * 
     * @param min The minimum value (inclusive)
     * @param max The maximum value (exclusive)
     * @return A random integer
     */
    public static int generateRandomInt(int min, int max) {
        return random.nextInt(max - min) + min;
    }
    
    /**
     * Generates a list of random integers.
     * 
     * @param size The size of the list
     * @param min The minimum value (inclusive)
     * @param max The maximum value (exclusive)
     * @return A list of random integers
     */
    public static List<Integer> generateRandomIntList(int size, int min, int max) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(generateRandomInt(min, max));
        }
        return result;
    }
    
    /**
     * Gets the current timestamp as a formatted string.
     * 
     * @return The current timestamp as string
     */
    public static String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
    
    /**
     * Calculates the average of a list of integers.
     * 
     * @param numbers The list of integers
     * @return The average as a double
     */
    public static double calculateAverage(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0.0;
        }
        
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        
        return (double) sum / numbers.size();
    }
    
    /**
     * Simple demonstration method to show how the utility class can be used.
     */
    public static void main(String[] args) {
        System.out.println("Random String (10): " + generateRandomString(10));
        System.out.println("Random UUID: " + generateUUID());
        System.out.println("Random Int (1-100): " + generateRandomInt(1, 100));
        
        List<Integer> randomList = generateRandomIntList(5, 1, 100);
        System.out.println("Random List: " + randomList);
        System.out.println("Average: " + calculateAverage(randomList));
        System.out.println("Current Timestamp: " + getCurrentTimestamp());
    }
} 