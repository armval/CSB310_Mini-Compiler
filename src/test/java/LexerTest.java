import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

public class LexerTest {

    /**
     * Helper to collect all tokens from input string.
     */
    private List<Lexer.Token> collectTokens(String source) {
        Lexer lexer = new Lexer(source);
        List<Lexer.Token> tokens = new ArrayList<>();
        Lexer.Token t;
        while ((t = lexer.getToken()).tokentype != Lexer.TokenType.End_of_input) {
            tokens.add(t);
        }
        return tokens;
    }

    @Test
    public void testIntegerAndSemicolon() {
        List<Lexer.Token> tokens = collectTokens("123;");
        assertEquals(2, tokens.size());
        assertEquals(Lexer.TokenType.Integer, tokens.get(0).tokentype);
        assertEquals("123", tokens.get(0).value);
        assertEquals(Lexer.TokenType.Semicolon, tokens.get(1).tokentype);
    }

    @Test
    public void testIdentifierAndKeyword() {
        List<Lexer.Token> tokens = collectTokens("if else foo;");
        assertEquals(Lexer.TokenType.Keyword_if, tokens.get(0).tokentype);
        assertEquals(Lexer.TokenType.Keyword_else, tokens.get(1).tokentype);
        assertEquals(Lexer.TokenType.Identifier, tokens.get(2).tokentype);
        assertEquals("foo", tokens.get(2).value);
        assertEquals(Lexer.TokenType.Semicolon, tokens.get(3).tokentype);
    }

    @Test
    public void testStringLiteralWithEscape() {
        String src = "\"Hello\\nWorld\\t!\\\\\\\"";  // Represents: \"Hello\nWorld\t!\\\"
        List<Lexer.Token> tokens = collectTokens(src);
        assertEquals(2, tokens.size());
        assertEquals(Lexer.TokenType.String, tokens.get(0).tokentype);
        assertEquals("Hello\nWorld\t!\\", tokens.get(0).value);
    }

    @Test
    public void testCharLiteral() {
        List<Lexer.Token> tokens1 = collectTokens("'a';");
        assertEquals(Lexer.TokenType.Integer, tokens1.get(0).tokentype);
        assertEquals(Integer.toString((int)'a'), tokens1.get(0).value);

        List<Lexer.Token> tokens2 = collectTokens("'\\n';");
        assertEquals(Lexer.TokenType.Integer, tokens2.get(0).tokentype);
        assertEquals(Integer.toString((int)'\n'), tokens2.get(0).value);
    }

    @Test
    public void testOperators() {
        String ops = "+ - * / % == != <= >= && || < > =";
        List<Lexer.Token> tokens = collectTokens(ops);
        Lexer.TokenType[] expected = {
                Lexer.TokenType.Op_add,
                Lexer.TokenType.Op_subtract,
                Lexer.TokenType.Op_multiply,
                Lexer.TokenType.Op_divide,
                Lexer.TokenType.Op_mod,
                Lexer.TokenType.Op_equal,
                Lexer.TokenType.Op_notequal,
                Lexer.TokenType.Op_lessequal,
                Lexer.TokenType.Op_greaterequal,
                Lexer.TokenType.Op_and,
                Lexer.TokenType.Op_or,
                Lexer.TokenType.Op_less,
                Lexer.TokenType.Op_greater,
                Lexer.TokenType.Op_assign
        };
        assertEquals(expected.length, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).tokentype, "Mismatch at operator " + i);
        }
    }

    @Test
    public void testCommentSkipping() {
        String src = "123 // comment\n456 /* block */ 789";
        List<Lexer.Token> tokens = collectTokens(src);
        assertEquals(Lexer.TokenType.Integer, tokens.get(0).tokentype);
        assertEquals("123", tokens.get(0).value);
        assertEquals(Lexer.TokenType.Integer, tokens.get(1).tokentype);
        assertEquals("456", tokens.get(1).value);
        assertEquals(Lexer.TokenType.Integer, tokens.get(2).tokentype);
        assertEquals("789", tokens.get(2).value);
    }

    @Test
    public void testParensAndBraces() {
        String src = "( ) { } , ;";
        List<Lexer.Token> tokens = collectTokens(src);
        Lexer.TokenType[] expected = {
                Lexer.TokenType.LeftParen,
                Lexer.TokenType.RightParen,
                Lexer.TokenType.LeftBrace,
                Lexer.TokenType.RightBrace,
                Lexer.TokenType.Comma,
                Lexer.TokenType.Semicolon
        };
        assertEquals(expected.length, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).tokentype);
        }
    }
}
