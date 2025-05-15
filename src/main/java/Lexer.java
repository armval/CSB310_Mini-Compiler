import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Lexical analyzer (Lexer) for a simple C-like language.
 * Processes one or more input files and outputs corresponding .lex files.
 */
public class Lexer {
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;

    Map<String, TokenType> keywords = new HashMap<>();

    /**
     * Represents a single lexical token.
     */
    static class Token {
        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;

        /**
         * Constructs a Token.
         * @param token the type of token
         * @param value the literal or identifier text
         * @param line the line where the token starts
         * @param pos the column where the token starts
         */
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }

        /**
         * Escapes special characters in string values for display.
         * @param s raw string literal contents
         * @return escaped string representation
         */
        private static String escapeString(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '\n': sb.append("\\n"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\\': sb.append("\\\\"); break;
                    case '"': sb.append("\\\""); break;
                    default: sb.append(c); break;
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.line, this.pos, this.tokentype);
            switch (this.tokentype) {
                case Integer:
                    result += String.format("  %4s", value);
                    break;
                case Identifier:
                    result += String.format(" %s", value);
                    break;
                case String:
                    result += String.format(" \"%s\"", escapeString(value));
                    break;
            }
            return result;
        }
    }

    /**
     * All supported token types.
     */
    static enum TokenType {
        End_of_input, Op_multiply,  Op_divide, Op_mod, Op_add, Op_subtract,
        Op_negate, Op_not, Op_less, Op_lessequal, Op_greater, Op_greaterequal,
        Op_equal, Op_notequal, Op_assign, Op_and, Op_or, Keyword_if,
        Keyword_else, Keyword_while, Keyword_print, Keyword_putc, LeftParen, RightParen,
        LeftBrace, RightBrace, Semicolon, Comma, Identifier, Integer, String
    }

    /**
     * Reports a lexical error with location and exits.
     * @param line the line of the error
     * @param pos the column of the error
     * @param msg the error message
     */
    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

    /**
     * Initializes lexer with full source text.
     * @param source complete source code
     */
    Lexer(String source) {
        this.line = 1;
        this.pos = 1;
        this.position = 0;
        this.s = source;
        this.chr = this.s.charAt(0);
        this.keywords.put("if", TokenType.Keyword_if);
        this.keywords.put("else", TokenType.Keyword_else);
        this.keywords.put("print", TokenType.Keyword_print);
        this.keywords.put("putc", TokenType.Keyword_putc);
        this.keywords.put("while", TokenType.Keyword_while);

    }

    /**
     * Processes a two-character operator or its single-character variant.
     */
    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.End_of_input) {
            error(line, pos, String.format("follow: unrecognized character: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);
    }

    /**
     * Parses a character literal, handling escape sequences.
     */
    Token char_lit(int line, int pos) {
        char c = getNextChar();
        int code = 0;
        if (c == '\\') {
            char esc = getNextChar();
            switch (esc) {
                case 'n': code = '\n'; break;
                case '\\': code = '\\'; break;
                case '\'': code = '\''; break;
                default: error(line, pos, "Unknown escape sequence: \\" + esc);
            }
            getNextChar();
        } else {
            code = c;
            getNextChar();
        }
        if (chr != '\'') error(line, pos, "Unterminated char literal");
        getNextChar();
        return new Token(TokenType.Integer, Integer.toString(code), line, pos);
    }

    /**
     * Parses a string literal, handling escape sequences.
     */
    Token string_lit(char start, int line, int pos) {
        getNextChar();
        StringBuilder sb = new StringBuilder();
        while (chr != '"' && chr != '\u0000') {
            if (chr == '\\') {
                getNextChar();
                switch (chr) {
                    case 'n': sb.append('\n'); break;
                    case '\\': sb.append('\\'); break;
                    default: error(line, pos, "Unknown escape in string: \\" + chr);
                }
            } else {
                sb.append(chr);
            }
            getNextChar();
        }
        if (chr != '"') error(line, pos, "Unterminated string literal");
        getNextChar();
        return new Token(TokenType.String, sb.toString(), line, pos);
    }

    /**
     * Handles '/' to distinguish divide operator vs. comments.
     */
    Token div_or_comment(int line, int pos) {
        getNextChar();
        if (chr == '/') {
            while (chr != '\n' && chr != '\u0000') getNextChar();
            return getToken();
        } else if (chr == '*') {
            getNextChar();
            while (true) {
                if (chr == '\u0000') error(line, pos, "Unterminated block comment");
                if (chr == '*') {
                    if (getNextChar() == '/') { getNextChar(); break; }
                } else getNextChar();
            }
            return getToken();
        } else {
            return new Token(TokenType.Op_divide, "", line, pos);
        }
    }

    /**
     * Parses identifiers and integer literals.
     */
    Token identifier_or_integer(int line, int pos) {
        StringBuilder text = new StringBuilder();
        boolean isNumber = Character.isDigit(chr);
        while (Character.isLetterOrDigit(chr) || chr == '_') {
            text.append(chr);
            getNextChar();
        }
        String word = text.toString();
        if (!isNumber && keywords.containsKey(word)) {
            return new Token(keywords.get(word), "", line, pos);
        } else if (isNumber) {
            return new Token(TokenType.Integer, word, line, pos);
        } else {
            return new Token(TokenType.Identifier, word, line, pos);
        }
    }

    /**
     * Retrieves the next token from the source.
     */
    Token getToken() {
        int startLine, startPos;
        while (Character.isWhitespace(chr)) getNextChar();
        startLine = line;
        startPos = pos;
        switch (chr) {
            case '\u0000': return new Token(TokenType.End_of_input, "", line, pos);
            case '*': getNextChar(); return new Token(TokenType.Op_multiply, "", startLine, startPos);
            case '/': return div_or_comment(startLine, startPos);
            case '%': getNextChar(); return new Token(TokenType.Op_mod, "", startLine, startPos);
            case '+': getNextChar(); return new Token(TokenType.Op_add, "", startLine, startPos);
            case '-': getNextChar(); return new Token(TokenType.Op_subtract, "", startLine, startPos);
            case '!': return follow('=', TokenType.Op_notequal, TokenType.Op_not, startLine, startPos);
            case '<': return follow('=', TokenType.Op_lessequal, TokenType.Op_less, startLine, startPos);
            case '>': return follow('=', TokenType.Op_greaterequal, TokenType.Op_greater, startLine, startPos);
            case '=': return follow('=', TokenType.Op_equal, TokenType.Op_assign, startLine, startPos);
            case '&': return follow('&', TokenType.Op_and, TokenType.End_of_input, startLine, startPos);
            case '|': return follow('|', TokenType.Op_or, TokenType.End_of_input, startLine, startPos);
            case '(': getNextChar(); return new Token(TokenType.LeftParen, "", startLine, startPos);
            case ')': getNextChar(); return new Token(TokenType.RightParen, "", startLine, startPos);
            case '{': getNextChar(); return new Token(TokenType.LeftBrace, "", startLine, startPos);
            case '}': getNextChar(); return new Token(TokenType.RightBrace, "", startLine, startPos);
            case ';': getNextChar(); return new Token(TokenType.Semicolon, "", startLine, startPos);
            case ',': getNextChar(); return new Token(TokenType.Comma, "", startLine, startPos);
            case '\'': return char_lit(startLine, startPos);
            case '\"': return string_lit('\"', startLine, startPos);
            default: return identifier_or_integer(startLine, startPos);
        }
    }

    /**
     * Advances to the next character in the source, updating position counters.
     */
    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.line++;
            this.pos = 0;
        }
        return this.chr;
    }

    /**
     * Produces a full token listing as a string.
     */
    String printTokens() {
        Token t;
        StringBuilder sb = new StringBuilder();
        while ((t = getToken()).tokentype != TokenType.End_of_input) {
            sb.append(t);
            sb.append("\n");
            System.out.println(t);
        }
        sb.append(t);
        System.out.println(t);
        return sb.toString();
    }

    /**
     * Writes token output to a .lex file corresponding to the input.
     * @param result full token output text
     * @param inputPath original input filename
     */
    static void outputToFile(String result, String inputPath) {
        String outPath;
        int dotIndex = inputPath.lastIndexOf('.');
        if (dotIndex != -1) {
            outPath = inputPath.substring(0, dotIndex) + ".lex";
        } else {
            outPath = inputPath + ".lex";
        }
        try {
            FileWriter myWriter = new FileWriter(outPath);
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Entry point; processes each specified file and generates a .lex file.
     * @param args list of source filenames to analyze
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Lexer <inputFile1> [<inputFile2> ...]");
            System.exit(1);
        }
        for (String inputPath : args) {
            try (Scanner scanner = new Scanner(new File(inputPath))) {
                StringBuilder source = new StringBuilder();
                while (scanner.hasNextLine()) {
                    source.append(scanner.nextLine()).append("\n");
                }
                Lexer lexer = new Lexer(source.toString());
                String result = lexer.printTokens();
                outputToFile(result, inputPath);
                System.out.println("Generated: " + inputPath.substring(0, inputPath.lastIndexOf('.')) + ".lex");
            } catch (FileNotFoundException e) {
                error(-1, -1, "File not found: " + inputPath);
            }
        }
    }
}