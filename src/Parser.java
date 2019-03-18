import java.math.BigInteger;
import java.util.Stack;

class Parser {

    String toParse;
    int level = 0, curPos = 0;
    Stack<Integer> lambdas = new Stack<>();

    Expression parse(String s) {
        toParse = s;
        return parseLevel();
    }

    int getChar() {
        if (curPos >= toParse.length()) return -1;
        else return toParse.charAt(curPos);
    }

    int nextChar() {
        curPos++;
        if (curPos >= toParse.length()) return -1;
        else return toParse.charAt(curPos);
    }

    void skipSpaces() {
        while (Character.isWhitespace(getChar())) {
            nextChar();
        }
    }

    Expression parseLevel() {
        Expression current = null;
        while (true) {
            skipSpaces();
            Expression next;
            switch (getChar()) {
                case '(': {
                    level++;
                    nextChar();
                    next = parseLevel();
                    break;
                }
                case ')': {
                    if (!lambdas.isEmpty() && lambdas.peek() == level) {
                        lambdas.pop();
                        return current;
                    }
                    level--;
                    nextChar();
                    return current;
                }
                case -1: {
                    if (!lambdas.isEmpty() && lambdas.peek() == level) {
                        lambdas.pop();
                        return current;
                    }
                    return current;
                }
                case '\\': {
                    StringBuilder var = new StringBuilder();
                    nextChar();
                    while (getChar() != '.') {
                        if (!Character.isWhitespace(getChar())) var.append((char) (getChar()));
                        nextChar();
                    }
                    nextChar();
                    lambdas.push(level);
                    Expression nested = parseLevel();
                    next = new Lambda(var.toString(), nested);
                    break;
                }
                default: {
                    StringBuilder var = new StringBuilder();
                    while (Character.isAlphabetic(getChar()) || Character.isDigit(getChar()) || getChar() == '\'') {
                        var.append((char) (getChar()));
                        nextChar();
                    }
                    next = new Variable(var.toString());
                }
            }
            if (current == null) current = next;
            else current = new Application (current, next);
        }
    }

}