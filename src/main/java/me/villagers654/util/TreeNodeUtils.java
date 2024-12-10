package me.villagers654.util;

import java.util.ArrayList;
import java.util.List;

public class TreeNodeUtils {
  // Construct a binary tree from preorder input
  public static TreeNode buildTree(String[] nodes, int[] index) {
    if (index[0] >= nodes.length || nodes[index[0]].equals("x")) {
      index[0]++;
      return null;
    }

    TreeNode node = new TreeNode(Integer.parseInt(nodes[index[0]++]));
    node.left = buildTree(nodes, index);
    node.right = buildTree(nodes, index);
    return node;
  }

  public static TreeNode buildTreeFromString(String input) {
    String[] nodes = input.split(" ");
    int[] index = {0};
    return buildTree(nodes, index);
  }

  // Generate all root-to-leaf paths
  public static List<String> binaryTreePaths(TreeNode root) {
    List<String> paths = new ArrayList<>();
    if (root != null) {
      findPaths(root, "", paths);
    }
    return paths;
  }

  private static void findPaths(TreeNode node, String path, List<String> paths) {
    if (node.left == null && node.right == null) {
      paths.add(path + node.info);
    }
    if (node.left != null) {
      findPaths(node.left, path + node.info + "->", paths);
    }
    if (node.right != null) {
      findPaths(node.right, path + node.info + "->", paths);
    }
  }
}
