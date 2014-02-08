/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.core.eql;


import com.jitlogic.zico.core.eql.ast.EqlBinaryExpr;
import com.jitlogic.zico.core.eql.ast.EqlFunCall;
import com.jitlogic.zico.core.eql.ast.EqlLiteral;
import com.jitlogic.zico.core.eql.ast.EqlSymbol;
import com.jitlogic.zico.core.eql.ast.EqlUnaryExpr;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;

@BuildParseTree
public class ParserSyntax extends BaseParser<Object> {

    Rule SoleExpression() {
        return Sequence(Expression(), EOI);
    }

    Rule Expression() {
        return BinaryExpression();
    }

    Rule FunCall() {
        return Sequence(
                Test(Sequence(FuncExpression(), OpToken("("))),
                FuncExpression(), OpToken("("),
                push(EqlFunCall.funcall(pop())),
                Optional(Sequence(Expression(), push(EqlFunCall.argument(pop(), pop())))),
                ZeroOrMore(Sequence(OpToken(","), Sequence(Expression(), push(EqlFunCall.argument(pop(), pop()))))),
                OpToken(")")
        );
    }

    Rule FuncExpression() {
        return FirstOf(Subexpression(), LiteralExpression(), SYMBOL());
    }

    Rule BinaryExpression() {
        return Sequence(UnaryExpression(), Optional(
                Sequence(BINARY_OP(), Expression(), push(EqlBinaryExpr.make(pop(), pop(), pop())))));
    }

    Rule UnaryExpression() {
        return FirstOf(Sequence(UNARY_OP(), Expression(), push(EqlUnaryExpr.make(pop(), pop()))), BaseExpression());
    }

    Rule BaseExpression() {
        return FirstOf(FunCall(), Subexpression(), LiteralExpression(), SYMBOL());
    }

    Rule Subexpression() {
        return Sequence(OpToken("("), Expression(), push(EqlBinaryExpr.precede(pop())), StringToken(")"));
    }

    Rule LiteralExpression() {
        return Sequence(LITERAL(), push(new EqlLiteral(pop())));
    }

    Rule TIMESTAMP() {
        return Sequence(
                Sequence(OneOrMore(CharRange('0', '9')), push(ZorkaUtil.map("t1", Long.parseLong(match())))),
                Optional(Sequence(".", OneOrMore(CharRange('0', '9')), push(Parser.extendMap(pop(), "t2", Long.parseLong(match()))))),
                Sequence(TIME_SUFFIX(), push(Parser.timestamp(pop(), match()))));
    }

    Rule TIME_SUFFIX() {
        return FirstOf("ms", "us", "ns", "h", "m", "s");
    }

    Rule SYMBOL() {
        return Sequence(TestNot(KEYWORD()), Sequence(ALPHA(), ZeroOrMore(ALPHA_NUM())),
                push(new EqlSymbol(match())), FirstOf(WS0(), TestNot(ALPHA_NUM())));
    }

    Rule LITERAL() {
        return Sequence(FirstOf(TIMESTAMP(), LONG(), INTEGER(), NULL(), BOOLEAN(), STRING()), WS0());
    }

    Rule NULL() {
        return Sequence(StringToken("null"), push(null));
    }

    Rule BOOLEAN() {
        return FirstOf(TRUE(), FALSE());
    }

    Rule TRUE() {
        return Sequence(StringToken("true"), push(true));
    }

    Rule FALSE() {
        return Sequence(StringToken("false"), push(false));
    }

    Rule LONG() {
        return FirstOf(HEX_LONG(), DECIMAL_LONG());
    }

    Rule DECIMAL_LONG() {
        return Sequence(Sequence(Optional("-"), OneOrMore(DIGIT()), "L"),
                push(Long.parseLong(match().substring(0, match().length() - 1))));
    }

    Rule HEX_LONG() {
        return Sequence(Sequence(Optional("-"), "0x", OneOrMore(HEX_DIGIT()), "L"),
                push(Long.parseLong(match().substring(0, match().length() - 1).replace("0x", ""), 16)));
    }

    Rule INTEGER() {
        return FirstOf(HEX_INTEGER(), DECIMAL_INTEGER());
    }

    Rule DECIMAL_INTEGER() {
        return Sequence(Sequence(Optional("-"), OneOrMore(DIGIT())), push(Integer.parseInt(match())));
    }

    Rule HEX_INTEGER() {
        return Sequence(Sequence(Optional("-"), "0x", OneOrMore(HEX_DIGIT())), push(Integer.parseInt(match().replace("0x", ""), 16)));
    }

    Rule ECHAR() {
        return Sequence('\\', AnyOf("tbnrf\\\"\'"));
    }

    Rule STRING() {
        return Sequence('\'', Sequence(ZeroOrMore(
                FirstOf(Sequence(TestNot(AnyOf("\'\\\r\n")), ANY), ECHAR())), push(Parser.unescape(match()))), '\'');
    }

    Rule KEYWORD() {
        return FirstOf(BINARY_OP(), PARENTH_OP(), StringToken("and"), StringToken("or"),
                StringToken("false"), StringToken("not"), StringToken("null"), StringToken("true"));
    }

    // Basic tokens

    Rule ALPHA_NUM() {
        return FirstOf(DIGIT(), ALPHA());
    }

    Rule OP_CHAR() {
        return FirstOf('+', '-', '*', '/', '~', '&', '|', '^', '<', '>', '=', '%');
    }

    Rule HEX_DIGIT() {
        return FirstOf(DIGIT(), CharRange('a', 'f'), CharRange('A', 'F'));
    }

    Rule DIGIT() {
        return CharRange('0', '9');
    }

    Rule ALPHA() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_');
    }

    // Operators


    Rule UNARY_OP() {
        return Sequence(FirstOf(OpToken("not"), OpToken("~"), OpToken("!")), push(match().trim()));
    }


    Rule BINARY_OP() {
        return Sequence(FirstOf(OpToken("+"), OpToken("-"), OpToken("*"), OpToken("/"), OpToken("%"),
                StringToken("and"), StringToken("or"), OpToken("&&"), OpToken("||"),
                OpToken("="), OpToken("=="), OpToken("!="), OpToken("<>"), OpToken("~="),
                OpToken("<"), OpToken(">"), OpToken("<="), OpToken(">="),
                OpToken("&"), OpToken("|"), OpToken("^"),
                OpToken(".")), push(match().trim()));
    }

    Rule PARENTH_OP() {
        return FirstOf(OpToken("("), OpToken(")"));
    }

    // Whitespaces

    @SuppressNode
    Rule WS0() {
        return ZeroOrMore(FirstOf(COMMENT(), PLAIN_WS()));
    }

    @SuppressNode
    Rule WS1() {
        return OneOrMore(FirstOf(COMMENT(), PLAIN_WS()));
    }

    @SuppressNode
    Rule PLAIN_WS() {
        return AnyOf(" \t\f\n\r");
    }

    @SuppressNode
    Rule EOL() {
        return AnyOf("\r\n");
    }

    @SuppressNode
    Rule COMMENT() {
        return Sequence(FirstOf("//", "--"), ZeroOrMore(Sequence(TestNot(EOL()), ANY)), EOL());
    }

    // Helper functions

    public Rule CharToken(char c) {
        return Sequence(Ch(c), FirstOf(WS0(), TestNot(ALPHA_NUM())));
    }

    public Rule StringToken(String s) {
        return Sequence(String(s), FirstOf(WS1(), TestNot(ALPHA_NUM())));
    }

    public Rule OpToken(String s) {
        return Sequence(String(s), FirstOf(WS1(), TestNot(OP_CHAR())));
    }
}
