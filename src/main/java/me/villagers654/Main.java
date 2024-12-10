package me.villagers654;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;

public class Main {

  public static void main(String[] args) {
    WebDriver driver;
    OSChecker.OS os = OSChecker.getOperatingSystem();

    driver = getDriver(os);

    String aptName = JOptionPane.showInputDialog(null, "APT Name? (Case sensitive)");

    if (aptName == null || aptName.trim().isEmpty()) {
      System.err.println("APT Name cannot be empty.");
      driver.quit();
      System.exit(1);
    }

    if (aptName.equals("SimpleWordGame")) {
      aptName = "SimpleWordSearch";
    }

    try {
      JPanel panel = new JPanel(new BorderLayout(5, 5));

      JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
      label.add(new JLabel("NetID", SwingConstants.RIGHT));
      label.add(new JLabel("Password", SwingConstants.RIGHT));
      panel.add(label, BorderLayout.WEST);

      JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
      JTextField username = new JTextField();
      controls.add(username);
      JPasswordField password = new JPasswordField();
      controls.add(password);
      panel.add(controls, BorderLayout.CENTER);

      int result =
          JOptionPane.showConfirmDialog(
              null, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (result != JOptionPane.OK_OPTION) {
        System.err.println("Login canceled by user.");
        driver.quit();
        System.exit(1);
      }

      if (username.getText().trim().isEmpty() || password.getPassword().length == 0) {
        System.err.println("Username or Password cannot be empty.");
        driver.quit();
        System.exit(1);
      }

      driver.navigate().to("https://apt.cs.duke.edu/aptsec/201/fall24/");
      System.out.println("Navigated to login page.");

      WebElement usernameField = driver.findElement(By.name("j_username"));
      WebElement passwordField = driver.findElement(By.name("j_password"));
      usernameField.sendKeys(username.getText());
      passwordField.sendKeys(new String(password.getPassword()));
      System.out.println("Entered credentials.");

      WebElement loginButton = driver.findElement(By.name("Submit"));
      loginButton.click();
      System.out.println("Clicked login button.");

      // Wait for login to complete by checking the presence of a unique element
      long startTime = System.currentTimeMillis();
      long timeout = 20000; // 20 seconds timeout
      By uniqueElementLocator =
          By.xpath("//h1[contains(text(), 'APT Grading: CompSci 201, Fall 2024')]");

      while (System.currentTimeMillis() - startTime < timeout) {
        try {
          WebElement element = driver.findElement(uniqueElementLocator);
          if (element.isDisplayed()) {
            System.out.println("Login successful. Detected unique post-login element.");
            break;
          }
        } catch (NoSuchElementException e) {
          // Element not found yet, keep waiting
        }

        Thread.sleep(500); // Check every 500ms
      }

      if (System.currentTimeMillis() - startTime >= timeout) {
        System.err.println(
            "Login might have failed or unique element not found within the timeout.");
        driver.quit();
        System.exit(1);
      }

      driver.navigate().to("https://cs.duke.edu/csed/newapt/" + aptName.toLowerCase() + ".html");
      System.out.println("Navigated to APT page: " + aptName.toLowerCase() + ".html");

      String classCode = extractClassFromHTML(driver);
      String formattedCode =
          formatJavaCode(classCode); // Ensure the code is formatted before submission
      String methodSignature = findFirstMethodSignature(formattedCode);

      if (methodSignature == null) {
        System.err.println("Error: Method signature not found in the formatted code.");
        // Optionally, save the formatted code for manual inspection
        try (FileWriter writer = new FileWriter(aptName + "_formatted.java")) {
          writer.write(formattedCode);
          System.err.println(
              "Formatted code saved to " + aptName + "_formatted.java for manual inspection.");
        } catch (IOException e) {
          System.err.println("Failed to save formatted code: " + e.getMessage());
        }
        driver.quit();
        System.exit(1);
      }

      if (aptName.equals("SimpleWordSearch")) {
        try (FileWriter writer = new FileWriter("SimpleWordGame.java")) {
          writer.write(formattedCode);
          System.out.println("Formatted code written to SimpleWordGame.java");
        }
      } else {
        try (FileWriter writer = new FileWriter(aptName + ".java")) {
          writer.write(formattedCode);
          System.out.println("Formatted code written to " + aptName + ".java");
        }
      }

      driver.navigate().to("https://apt.cs.duke.edu/aptsec/201/fall24/");
      System.out.println("Navigated back to APT submission page.");

      if (aptName.equals("TxMsg")) {
        aptName = "TxtMsg";
      }

      startTime = System.currentTimeMillis();
      timeout = 20000; // 20 seconds timeout
      By radioButtonLocator = By.xpath("//input[@type='radio'][@name='problem']");

      while (System.currentTimeMillis() - startTime < timeout) {
        try {
          List<WebElement> radioButtons = driver.findElements(radioButtonLocator);
          if (!radioButtons.isEmpty()) {
            System.out.println("Located problem radio buttons.");
            break;
          }
        } catch (NoSuchElementException e) {
          // Elements not found yet, keep waiting
        }

        Thread.sleep(500); // Check every 500ms
      }

      if (System.currentTimeMillis() - startTime >= timeout) {
        System.err.println("Radio buttons not found within the timeout.");
        driver.quit();
        System.exit(1);
      }

      List<WebElement> radioButtons = driver.findElements(radioButtonLocator);

      WebElement radioButton = null;
      for (WebElement rb : radioButtons) {
        if (Objects.requireNonNull(rb.getAttribute("value"))
            .equalsIgnoreCase(aptName.toLowerCase())) {
          radioButton = rb;
          break;
        }
      }

      if (radioButton == null) {
        throw new NoSuchElementException("Radio button for " + aptName + " not found");
      }

      ((JavascriptExecutor) driver)
          .executeScript("arguments[0].scrollIntoView(true);", radioButton);
      radioButton.click();
      System.out.println("Selected the radio button for " + aptName);

      WebElement fileInput =
          driver.findElement(
              By.xpath(
                  "//input[@type='radio'][@name='problem'][@value='"
                      + aptName.toLowerCase()
                      + "']/following::input[@type='file'][1]"));

      ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", fileInput);

      String filePath;
      if (aptName.equals("SimpleWordSearch")) {
        filePath = new File("SimpleWordGame.java").getAbsolutePath();
      } else if (aptName.equals("TxtMsg")) {
        filePath = new File("TxtMsg.java").getAbsolutePath();
      } else {
        filePath = new File(aptName + ".java").getAbsolutePath();
      }

      fileInput.sendKeys(filePath);
      System.out.println("Uploaded file: " + filePath);

      WebElement submitButton =
          driver.findElement(
              By.xpath(
                  "//input[@type='radio'][@name='problem'][@value='"
                      + aptName.toLowerCase()
                      + "']/following::input[@type='submit'][@value='test/run'][1]"));

      submitButton.click();
      System.out.println("Clicked the submit button.");

      startTime = System.currentTimeMillis();
      timeout = 30000; // 30 seconds timeout

      while (System.currentTimeMillis() - startTime < timeout) {
        try {
          if (!submitButton.isDisplayed()) {
            System.out.println("Submission completed. Waiting for results.");
            break;
          }
        } catch (StaleElementReferenceException e) {
          System.out.println("Submit button is no longer attached to the DOM.");
          break;
        }

        Thread.sleep(500); // Check every 500ms
      }

      if (System.currentTimeMillis() - startTime >= timeout) {
        System.err.println("Submission results not loaded within the timeout.");
        driver.quit();
        System.exit(1);
      }

      String pageSource = driver.getPageSource();

      assert pageSource != null;
      APTCrawler.doWork(Jsoup.parse(pageSource), methodSignature);
    } catch (NoSuchElementException e) {
      System.err.println("Error during Selenium operations: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("An unexpected error occurred: " + e.getMessage());
      e.printStackTrace();
    } finally {
      driver.quit();
      System.out.println("Browser closed.");
    }
  }

  /**
   * Formats the Java code by ensuring that all non-void methods have a return statement. For array
   * return types, it appends 'return new Type[0];' before the method's closing brace if no return
   * statement is present.
   *
   * @param code The original Java code as a string.
   * @return The formatted Java code with necessary return statements appended.
   */
  private static String formatJavaCode(String code) {
    // Step 1: Insert line breaks before and after braces to make parsing easier
    code = code.replace("{", "{\n");
    code = code.replace("}", "}\n");

    StringBuilder formattedCode = new StringBuilder();
    String[] lines = code.split("\n");
    boolean inMethod = false;
    String returnType = null;
    boolean hasReturn = false;
    int braceCount = 0; // To track nested braces within methods

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String trimmedLine = line.trim();
      formattedCode.append(line).append("\n"); // Preserve original formatting

      // Detect method signatures (public/protected/private, non-abstract, with return type)
      if (!inMethod
          && trimmedLine.matches(
              "^(public|protected|private)\\s+[^\\s]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?$")) {
        inMethod = true;

        // Extract return type
        String[] parts = trimmedLine.split("\\s+");
        if (parts.length >= 3) {
          returnType = parts[1];
        } else {
          returnType = "void";
        }

        // Check if the opening brace is on the same line
        if (trimmedLine.endsWith("{")) {
          braceCount = 1;
        } else {
          // If brace is on the next line
          if (i + 1 < lines.length) {
            i++;
            String nextLine = lines[i].trim();
            formattedCode.append(lines[i]).append("\n");
            if (nextLine.equals("{")) {
              braceCount = 1;
            }
          }
        }
        continue;
      }

      if (inMethod) {
        // Update brace count
        braceCount += countOccurrences(trimmedLine, '{');
        braceCount -= countOccurrences(trimmedLine, '}');

        // Check for return statement
        if (trimmedLine.startsWith("return ")) {
          hasReturn = true;
        }

        // If braceCount drops to 0, method ends
        if (braceCount == 0) {
          if (!hasReturn && !returnType.equals("void")) {
            String defaultReturn = getDefaultReturn(returnType);
            // Determine indentation (assume closing brace is aligned)
            String indent = getIndentation(lines[i]);

            if (trimmedLine.equals("}")) {
              // Insert return statement before the closing brace
              formattedCode.insert(
                  formattedCode.lastIndexOf("}"), indent + "    " + defaultReturn + "\n");
            } else {
              // Split the line at the closing brace
              int braceIndex = line.lastIndexOf('}');
              String beforeBrace = line.substring(0, braceIndex).trim();
              String afterBrace = line.substring(braceIndex).trim();

              // Replace the current line with beforeBrace and return statement
              formattedCode.setLength(formattedCode.length() - line.length());
              formattedCode.append(beforeBrace).append("\n");
              formattedCode.append(indent).append("    ").append(defaultReturn).append("\n");
              formattedCode.append(indent).append(afterBrace).append("\n");
            }
          }
          inMethod = false;
          returnType = null;
          hasReturn = false;
        }
      }
    }

    return formattedCode.toString();
  }

  /**
   * Finds the first method signature in the formatted code using regex.
   *
   * @param formattedCode The formatted Java code.
   * @return The method signature as a string, or null if not found.
   */
  private static String findFirstMethodSignature(String formattedCode) {
    // Regex pattern to match method signatures
    return APTCrawler.findFirstMethodSignature(formattedCode);
  }

  /**
   * Counts the number of occurrences of a character in a string.
   *
   * @param str The string to search.
   * @param c The character to count.
   * @return The number of occurrences.
   */
  private static int countOccurrences(String str, char c) {
    int count = 0;
    for (char ch : str.toCharArray()) {
      if (ch == c) count++;
    }
    return count;
  }

  /**
   * Retrieves the indentation (whitespace) from the beginning of a line.
   *
   * @param line The line of code.
   * @return The indentation as a string.
   */
  private static String getIndentation(String line) {
    StringBuilder indent = new StringBuilder();
    for (char ch : line.toCharArray()) {
      if (ch == ' ' || ch == '\t') {
        indent.append(ch);
      } else {
        break;
      }
    }
    return indent.toString();
  }

  /**
   * Generates a default return statement based on the return type.
   *
   * @param returnType The return type of the method.
   * @return The default return statement as a string.
   */
  private static String getDefaultReturn(String returnType) {
    // Check if the return type is an array
    if (returnType.endsWith("[]")) {
      String baseType = returnType.substring(0, returnType.indexOf('['));
      return "return new " + baseType + "[0];";
    }

    // Handle primitive and object types
    return switch (returnType) {
      case "int", "long", "short", "byte" -> "return 0;";
      case "float", "double" -> "return 0.0;";
      case "boolean" -> "return false;";
      case "char" -> "return '\\0';";
      default -> "return null;";
    };
  }

  /**
   * Extracts the Java class code from the HTML page using Jsoup.
   *
   * @param driver The WebDriver instance.
   * @return The raw Java class code as a string.
   */
  public static String extractClassFromHTML(WebDriver driver) {
    String pageSource = driver.getPageSource();

    assert pageSource != null;
    Document doc = Jsoup.parse(pageSource);

    Elements xmpElements = doc.getElementsByTag("xmp");

    if (!xmpElements.isEmpty()) {
      String rawText = xmpElements.getFirst().html();
      System.out.println("Extracted class code from <xmp> tag.");
      return rawText;
    } else {
      System.err.println("Error: No <xmp> tags found in the HTML.");
      return "";
    }
  }

  /**
   * Initializes and returns the appropriate WebDriver based on the operating system.
   *
   * @param os The operating system.
   * @return The initialized WebDriver.
   */
  public static WebDriver getDriver(OSChecker.OS os) {
    try {
      return switch (os) {
        case WINDOWS -> WebDriverManager.edgedriver().create();
        case MAC -> WebDriverManager.safaridriver().create();
        case LINUX -> WebDriverManager.firefoxdriver().create();
        default -> throw new WebDriverException("Unsupported operating system.");
      };
    } catch (WebDriverException e1) {
      System.err.println("Primary WebDriver initialization failed: " + e1.getMessage());
      e1.printStackTrace();
      try {
        System.out.println("Attempting fallback to ChromeDriver...");
        return WebDriverManager.chromedriver().create();
      } catch (WebDriverException e2) {
        System.err.println("Fallback WebDriver initialization also failed: " + e2.getMessage());
        e2.printStackTrace();
        System.exit(1);
        return null;
      }
    }
  }
}
