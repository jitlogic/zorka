package bsh;

import bsh.ast.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class BshNodeVisitor<T> {

    public T visit(Node node) {
        return node.accept(this);
    }

    public T visit(SimpleNode node) {
        return node.accept(this);
    }

    public T visit(BSHAllocationExpression node) {
        return null;
    }

    public T visit(BSHAmbiguousName node) {
        return null;
    }

    public T visit(BSHArguments node) {
        return null;
    }

    public T visit(BSHArrayDimensions node) {
        return null;
    }

    public T visit(BSHArrayInitializer node) {
        return null;
    }

    public T visit(BSHAssignment node) {
        return null;
    }

    public T visit(BSHBinaryExpression node) {
        return null;
    }

    public T visit(BSHBlock node) {
        return null;
    }

    public T visit(BSHCastExpression node) {
        return null;
    }

    public T visit(BSHClassDeclaration node) {
        return null;
    }

    public T visit(BSHEnhancedForStatement node) {
        return null;
    }

    public T visit(BSHFormalComment node) {
        return null;
    }

    public T visit(BSHFormalParameter node) {
        return null;
    }

    public T visit(BSHFormalParameters node) {
        return null;
    }

    public T visit(BSHForStatement node) {
        return null;
    }

    public T visit(BSHIfStatement node) {
        return null;
    }

    public T visit(BSHImportDeclaration node) {
        return null;
    }

    public T visit(BSHLiteral node) {
        return null;
    }

    public T visit(BSHMethodDeclaration node) {
        return null;
    }

    public T visit(BSHMethodInvocation node) {
        return null;
    }

    public T visit(BSHPackageDeclaration node) {
        return null;
    }

    public T visit(BSHPrimaryExpression node) {
        return null;
    }

    public T visit(BSHPrimarySuffix node) {
        return null;
    }

    public T visit(BSHPrimitiveType node) {
        return null;
    }

    public T visit(BSHReturnStatement node) {
        return null;
    }

    public T visit(BSHReturnType node) {
        return null;
    }

    public T visit(BSHStatementExpressionList node) {
        return null;
    }

    public T visit(BSHSwitchLabel node) {
        return null;
    }

    public T visit(BSHSwitchStatement node) {
        return null;
    }

    public T visit(BSHTernaryExpression node) {
        return null;
    }

    public T visit(BSHThrowStatement node) {
        return null;
    }

    public T visit(BSHTryStatement node) {
        return null;
    }

    public T visit(BSHType node) {
        return null;
    }

    public T visit(BSHTypedVariableDeclaration node) {
        return null;
    }

    public T visit(BSHUnaryExpression node) {
        return null;
    }

    public T visit(BSHVariableDeclarator node) {
        return null;
    }

    public T visit(BSHWhileStatement node) {
        return null;
    }
}
