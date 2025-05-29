import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Example Java class with various deprecated methods and classes from Java 16 that should be updated for Java 21.
 */
public class DeprecatedExample implements Serializable {
    
    public static void main(String[] args) {
        // Example 1: Using deprecated Date constructors and methods
        Date date = new Date(2023, 5, 15); // Deprecated constructor
        System.out.println("Date using deprecated constructor: " + date);
        
        // Example 2: Using deprecated formatting method
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.applyPattern("MM/dd/yyyy"); // Still exists but there are better alternatives
        System.out.println("Formatted date: " + dateFormat.format(date));
        
        // Example 3: Using deprecated collections
        Vector<String> vector = new Vector<>(); // Vector is considered outdated, ArrayList preferred
        vector.addElement("First"); // addElement is deprecated/outdated, add() is preferred
        vector.addElement("Second");
        System.out.println("Vector size: " + vector.size());
        
        // Example 4: Using Hashtable (older than HashMap)
        Hashtable<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("One", 1);
        hashtable.put("Two", 2);
        System.out.println("Value for 'One': " + hashtable.get("One"));
        
        // Example 5: Using deprecated Thread methods
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Deprecated way to handle interruption
                Thread.currentThread().stop(); // stop() is deprecated for safety reasons
            }
        });
        
        // Example 6: Using deprecated Runtime methods
        Runtime runtime = Runtime.getRuntime();
        // These would actually run commands, so we'll just print instead of executing
        System.out.println("Would execute: runtime.runFinalization()"); // Deprecated
        
        // Example 7: Using String methods that have better alternatives
        String text = "Hello World";
        char[] chars = new char[text.length()];
        text.getChars(0, text.length(), chars, 0); // There are better ways in modern Java
        System.out.println("Characters: " + new String(chars));
        
        // Example 8: Using deprecated Stack class
        Stack<String> stack = new Stack<>();
        stack.push("First");
        stack.push("Second");
        System.out.println("Stack size: " + stack.size());
        
        // Example 9: Using deprecated Enumeration interface
        Enumeration<String> elements = vector.elements();
        while (elements.hasMoreElements()) {
            System.out.println("Element: " + elements.nextElement());
        }
        
        // Example 10: Using deprecated StringTokenizer
        StringTokenizer tokenizer = new StringTokenizer("Hello,World,Java", ",");
        while (tokenizer.hasMoreTokens()) {
            System.out.println("Token: " + tokenizer.nextToken());
        }
        
        // Example 11: Using deprecated File methods
        File file = new File("test.txt");
        try {
            // Using deprecated FileInputStream/FileOutputStream constructors
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject("Test data");
            oos.close();
            
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            String data = (String) ois.readObject();
            ois.close();
            System.out.println("Read data: " + data);
            
            // Clean up
            file.delete();
        } catch (Exception e) {
            System.out.println("Error in file operations: " + e.getMessage());
        }
        
        System.out.println("All examples completed.");
    }
} 