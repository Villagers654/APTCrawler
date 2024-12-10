package me.villagers654;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class APTCrawler {

    /**
   * Formats the Java code by ensuring that all non-void methods have a return statement.
   *
   * @param code The original Java code as a string.
   * @return The formatted Java code with necessary return statements appended.
   */
  public static String formatJavaCode(String code) {
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
      if (!inMethod && trimmedLine.matches("^(public|protected|private)\\s+[^\\s]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?$")) {
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
                formattedCode.insert(formattedCode.lastIndexOf("}"), indent + "    " + defaultReturn + "\n");
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
  public static String findFirstMethodSignature(String formattedCode) {
    // Regex pattern to match method signatures
    String methodSignaturePattern = "(public|protected|private)\\s+[^\\s]+\\s+\\w+\\s*\\([^)]*\\)";

    Pattern pattern = Pattern.compile(methodSignaturePattern);
    Matcher matcher = pattern.matcher(formattedCode);

    if (matcher.find()) {
      String match = matcher.group();
      System.out.println("Detected Method Signature: " + match);
      return match;
    }

    System.err.println("Error: No valid method signature found in the formatted code.");
    return null;
  }

  /**
   * Processes the extracted test cases and writes them to the GeneratedTest.java file.
   *
   * @param doc             The HTML document containing test case data.
   * @param methodSignature The signature of the method to be tested.
   */
  public static void doWork(Document doc, String methodSignature) {
    String outputFilePath = "GeneratedTest.java";

    MethodInfo methodInfo;
    try {
      methodInfo = parseMethodSignature(methodSignature);
      System.out.println("Parsed Method Name: " + methodInfo.methodName);
      System.out.println("Parsed Return Type: " + methodInfo.returnType);
      System.out.println("Parsed Parameter Types: " + methodInfo.paramTypes);
    } catch (IllegalArgumentException e) {
      System.err.println("Error parsing method signature: " + e.getMessage());
      return;
    }

    String methodName = methodInfo.methodName;
    String returnType = methodInfo.returnType;
    List<String> paramTypes = methodInfo.paramTypes;

    try {
      Elements rows = doc.select("table.border tr"); // Adjust the selector based on actual HTML

      List<String> testCases = new ArrayList<>();
      for (Element row : rows) {
        String testCase = extractData(row, methodName, returnType, paramTypes);
        if (testCase != null) {
          testCases.add(testCase);
        }
      }

      System.out.println("Total Test Cases Generated: " + testCases.size());

      try (FileWriter writer = new FileWriter(outputFilePath)) {
        writer.write("import java.util.Arrays;\n\n");
        writer.write("public class GeneratedTest {\n\n");
        writer.write(getTreeNodeClass());
        writer.write(getListNodeClass());
        writer.write("\n\n    public static void main(String[] args) {\n");
        writer.write("        runAllTests();\n");
        writer.write("    }\n\n");
        writer.write("    private static void runAllTests() {\n");
        writer.write("        boolean pass;\n\n");

        for (String testCase : testCases) {
          writer.write(testCase);
        }

        writer.write("    }\n\n");
        writer.write("}\n");
      }

      System.out.println("Java test file generated at: " + outputFilePath);
    } catch (IOException e) {
      System.err.println("Error writing file: " + e.getMessage());
    }
  }

  /**
   * Parses the method signature to extract the method name, return type, and parameter types.
   *
   * @param methodSignature The method signature as a string.
   * @return A MethodInfo object containing the method name, return type, and a list of parameter types.
   * @throws IllegalArgumentException If the method signature is malformed or null.
   */
  private static MethodInfo parseMethodSignature(String methodSignature) {
    if (methodSignature == null || methodSignature.isEmpty()) {
      throw new IllegalArgumentException("Invalid method signature: Signature is null or empty.");
    }

    // Regex pattern to capture return type, method name, and parameters
    String regex = "^(public|protected|private)\\s+([^\\s]+)\\s+(\\w+)\\s*\\(([^)]*)\\)";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(methodSignature.trim());

    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid method signature format.");
    }

    String returnType = matcher.group(2).trim();
    String methodName = matcher.group(3).trim();
    String params = matcher.group(4).trim();

    List<String> paramTypes = new ArrayList<>();
    if (!params.isEmpty()) {
      String[] paramArray = params.split(",");
      for (String param : paramArray) {
        param = param.trim();
        String[] tokens = param.split("\\s+");
        if (tokens.length < 2) {
          throw new IllegalArgumentException("Invalid parameter format: " + param);
        }
        // Concatenate all tokens except the last one to form the type (handles types like "Map<String, Integer>")
        StringBuilder typeBuilder = new StringBuilder();
        for (int i = 0; i < tokens.length - 1; i++) {
          typeBuilder.append(tokens[i]).append(" ");
        }
        String type = typeBuilder.toString().trim();
        paramTypes.add(type);
      }
    }

    return new MethodInfo(methodName, returnType, paramTypes);
  }

  /**
   * Extracts data from a table row and formats it into a test case.
   *
   * @param row          The table row element containing the test case data.
   * @param methodName   The name of the method to be tested.
   * @param returnType   The return type of the method.
   * @param paramTypes   The list of parameter types extracted from the method signature.
   * @return A formatted test case string or null if the row doesn't represent a failed test.
   */
  public static String extractData(Element row, String methodName, String returnType, List<String> paramTypes) {
    Elements cols = row.select("td");

    if (cols.size() > 2) {
      String status = cols.get(1).text().trim();
      if (status.equalsIgnoreCase("fail")) {
        Element expectedPreTag = cols.get(2).select("pre").first();
        String expectedResult = (expectedPreTag != null) ? expectedPreTag.text().trim().replace("\"", "") : "null";

        // Attempt to extract 'got' value(s)
        Element gotPreTag = cols.get(2).select("pre").last();
        String gotText = "";

        if (gotPreTag != null) {
          // Attempt to get the next sibling after the last 'pre' tag
          if (gotPreTag.nextSibling() != null) {
            gotText = Objects.requireNonNull(gotPreTag.nextSibling()).outerHtml().trim().replace(": ", "").replace("\"", "");
          } else {
            // Fallback: try to get the text within the 'pre' tag
            gotText = gotPreTag.text().trim().replace("\"", "");
          }
        }

        // If 'got' text is still empty, log a warning and skip this test case
        if (gotText.isEmpty()) {
          System.err.println("Warning: 'got' value not found for test case.");
          return null;
        }

        // Debugging: Print the extracted 'got' value
        System.out.println("Extracted 'got' value: " + gotText);

        String parameters = formatParameters(gotText, paramTypes);

        // Debugging: Print formatted parameters
        System.out.println("Formatted parameters: " + parameters);

        // Format expected result based on return type
        String formattedExpected = formatExpectedResult(expectedResult, returnType);

        // Determine comparison method
        String comparison;
        if (isArrayType(returnType)) {
          comparison = "Arrays.equals(" + methodName + "(" + parameters + "), " + formattedExpected + ")";
        } else if (returnType.equals("String")) {
          comparison = methodName + "(" + parameters + ").equals(" + formattedExpected + ")";
        } else {
          comparison = methodName + "(" + parameters + ") == " + formattedExpected;
        }

        return "        pass = " + comparison + ";\n"
                + "        System.out.println(\"Pass: \" + pass);\n\n";
      }
    }
    return null;
  }

  /**
   * Formats parameters based on their types.
   *
   * @param input      The raw parameter string from the test case.
   * @param paramTypes The list of parameter types corresponding to the method signature.
   * @return A formatted parameter string suitable for Java code.
   */
  public static String formatParameters(String input, List<String> paramTypes) {
    List<String> parameters = splitParameters(input.trim());
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < parameters.size() && i < paramTypes.size(); i++) {
      String param = parameters.get(i).trim();
      String type = paramTypes.get(i).trim();

      // Detect and format based on parameter type
        switch (type) {
            case "String" -> result.append("\"").append(param).append("\""); // Add quotes around String
            case "TreeNode" -> result.append("deserializeTreeNode(\"").append(param).append("\")");
            case "ListNode" -> result.append("deserializeListNode(\"").append(param).append("\")");
            case "int[]" -> {
                // Handle int arrays
                if (param.startsWith("[") && param.endsWith("]")) {
                    String arrayContent = param.substring(1, param.length() - 1).trim();
                    result.append("new int[]{").append(arrayContent).append("}");
                } else {
                    // Fallback if format is unexpected
                    System.err.println("Warning: Unexpected format for int[] parameter: " + param);
                    result.append(param);
                }
            }
            case "double[]" -> {
                // Handle double arrays
                if (param.startsWith("[") && param.endsWith("]")) {
                    String arrayContent = param.substring(1, param.length() - 1).trim();
                    result.append("new double[]{").append(arrayContent).append("}");
                } else {
                    // Fallback if format is unexpected
                    System.err.println("Warning: Unexpected format for double[] parameter: " + param);
                    result.append(param);
                }
            }
            case "String[]" -> {
                // Handle String arrays
                if (param.startsWith("[") && param.endsWith("]")) {
                    String arrayContent = param.substring(1, param.length() - 1).trim();
                    // Split the array content by commas and add quotes
                    String[] elements = arrayContent.split(",");
                    StringBuilder arrayBuilder = new StringBuilder();
                    arrayBuilder.append("new String[]{");
                    for (int j = 0; j < elements.length; j++) {
                        arrayBuilder.append("\"").append(elements[j].trim()).append("\"");
                        if (j < elements.length - 1) {
                            arrayBuilder.append(", ");
                        }
                    }
                    arrayBuilder.append("}");
                    result.append(arrayBuilder.toString());
                } else {
                    // Fallback if format is unexpected
                    System.err.println("Warning: Unexpected format for String[] parameter: " + param);
                    result.append(param);
                }
            }
            default -> {
                // Fallback for other types
                System.err.println("Warning: Unhandled parameter type: " + type);
                result.append(param);
            }
        }

      // Add a comma if this is not the last parameter
      if (i < parameters.size() - 1 && i < paramTypes.size() - 1) {
        result.append(", ");
      }
    }

    return result.toString();
  }

  /**
   * Formats the expected result based on the return type.
   *
   * @param expectedResult The expected result as a string.
   * @param returnType     The return type of the method.
   * @return A formatted expected result string suitable for Java code.
   */
  private static String formatExpectedResult(String expectedResult, String returnType) {
    if (isArrayType(returnType)) {
      if (returnType.equals("String[]")) {
        // Split the expectedResult by commas and add quotes
        String[] elements = expectedResult.replace("[", "").replace("]", "").split(",");
        StringBuilder arrayBuilder = new StringBuilder();
        arrayBuilder.append("new String[]{");
        for (int i = 0; i < elements.length; i++) {
          arrayBuilder.append("\"").append(elements[i].trim()).append("\"");
          if (i < elements.length - 1) {
            arrayBuilder.append(", ");
          }
        }
        arrayBuilder.append("}");
        return arrayBuilder.toString();
      }
      // Handle other array types if necessary
      return expectedResult;
    } else if (returnType.equals("String")) {
      return "\"" + expectedResult + "\"";
    } else {
      // For primitive types
      return expectedResult;
    }
  }

  /**
   * Checks if the given type is an array type.
   *
   * @param type The type to check.
   * @return True if it's an array type, false otherwise.
   */
  private static boolean isArrayType(String type) {
    return type.endsWith("[]");
  }

  /**
   * Splits the input string into individual parameters.
   * Handles commas and spaces inside brackets, braces, or quotes.
   *
   * @param input The raw parameter string.
   * @return A list of individual parameter strings.
   */
  private static List<String> splitParameters(String input) {
    List<String> parameters = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int bracketDepth = 0;
    int braceDepth = 0;
    boolean inQuotes = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c == '"') {
        inQuotes = !inQuotes;
        current.append(c);
      } else if ((c == ',' || c == ' ') && !inQuotes && bracketDepth == 0 && braceDepth == 0) {
        if (!current.isEmpty()) {
          parameters.add(current.toString().trim());
          current = new StringBuilder();
        }
      } else {
        current.append(c);
        if (c == '[') {
          bracketDepth++;
        } else if (c == ']') {
          bracketDepth--;
        } else if (c == '{') {
          braceDepth++;
        } else if (c == '}') {
          braceDepth--;
        }
      }
    }

    if (!current.isEmpty()) {
      parameters.add(current.toString().trim());
    }

    return parameters;
  }

  /**
   * Counts the number of occurrences of a character in a string.
   *
   * @param str The string to search.
   * @param c   The character to count.
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
   * Helper class to store method information.
   */
  public static class MethodInfo {
    String methodName;
    String returnType;
    List<String> paramTypes;

    MethodInfo(String methodName, String returnType, List<String> paramTypes) {
      this.methodName = methodName;
      this.returnType = returnType;
      this.paramTypes = paramTypes;
    }
  }

  /**
   * Generates the TreeNode class and its related methods.
   *
   * @return A string containing the TreeNode class definition.
   */
  private static String getTreeNodeClass() {
    return """
                static class TreeNode {
                    int val;
                    TreeNode left;
                    TreeNode right;
                    TreeNode(int x) { val = x; }
                }

                public static TreeNode deserializeTreeNode(String data) {
                    if (data.equals("{}")) return null;
                    String[] vals = data.substring(1, data.length() - 1).split(" ");
                    TreeNode root = new TreeNode(Integer.parseInt(vals[0]));
                    List<TreeNode> queue = new ArrayList<>();
                    queue.add(root);
                    int index = 1;
                    for (TreeNode node : queue) {
                        if (index < vals.length && !vals[index].equals("x")) {
                            node.left = new TreeNode(Integer.parseInt(vals[index]));
                            queue.add(node.left);
                        }
                        index++;
                        if (index < vals.length && !vals[index].equals("x")) {
                            node.right = new TreeNode(Integer.parseInt(vals[index]));
                            queue.add(node.right);
                        }
                        index++;
                    }
                    return root;
                }
                """;
  }

  /**
   * Generates the ListNode class and its related methods.
   *
   * @return A string containing the ListNode class definition.
   */
  private static String getListNodeClass() {
    return """
                static class ListNode {
                    int val;
                    ListNode next;
                    ListNode(int x) { val = x; }
                }

                public static ListNode deserializeListNode(String data) {
                    data = data.substring(1, data.length() - 1); // Remove brackets
                    if (data.trim().isEmpty()) return null;
                    String[] values = data.split(",");
                    ListNode dummy = new ListNode(0);
                    ListNode current = dummy;
                    for (String val : values) {
                        current.next = new ListNode(Integer.parseInt(val.trim()));
                        current = current.next;
                    }
                    return dummy.next;
                }

                public static String serializeListNode(ListNode node) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    while (node != null) {
                        sb.append(node.val);
                        if (node.next != null) sb.append(", ");
                        node = node.next;
                    }
                    sb.append("]");
                    return sb.toString();
                }
                """;
  }
}
