package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;


public class PreprocessorConstExpr {
    private List<PreprocessingToken> condition;
    private ConstExprTreeNode root;

    public PreprocessorConstExpr(List<PreprocessingToken> _condition, PreprocessingContext context) throws CompilerException {
        condition = new ArrayList<>(_condition);

        for (int i = 0; i < condition.size(); ++i) {
            if (condition.get(i).is(PreprocessingToken.TokenType.IDENTIFIER)) {
                condition.set(i, new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "0"));
            }
        }

        //-----ORDER OF OPERATIONS-----
        //(expr)
        //unary ~, unary +, unary -, unary !
        //*, /, %
        //binary +, binary -
        //<<, >>
        //<, >
        //==, !=
        //&, ^, |
        //&&, ||
        //?:

        for (PreprocessingToken token : condition) {
            if (!token.is(PreprocessingToken.TokenType.PP_NUMBER)
                    && !token.is(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR)) {
                throw new CompilerException(context, "Invalid token in constant expression: " + token.toString());
            }

            if (token.is(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR) && !isValidOperator(token.toString())) {
                throw new CompilerException(context, "Invalid operator in constant expression: " + token.toString());
            }
        }

        //TODO construct tree
    }

    public PreprocessingToken evaluate(PreprocessingContext context) throws CompilerException {
        return new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, root.evaluate().toString());
    }

    private class ConstExprTreeNode {
        private ConstExprTreeNode left;
        private ConstExprTreeNode right;
        private Number value;
        private String operator;

        public ConstExprTreeNode(ConstExprTreeNode _left, ConstExprTreeNode _right, Number _value) {
            left = _left;
            right = _right;
            value = _value;
        }
        public ConstExprTreeNode(ConstExprTreeNode _left, ConstExprTreeNode _right, String _operator) {
            left = _left;
            right = _right;
            operator = _operator;
        }

        public Number evaluate() {
            if (operator == null) {
                return value;
            }

            String operator = value.toString();
            if (operator.contentEquals("?")) {
                //ternary doesn't follow the usual execution path
                return ternary();
            }

            if (left.operator != null || right.operator != null) {
                throw new IllegalStateException("Attempted to apply operator to another operator: " + left + ", " + operator + ", " + right);
            }

            return switch (value.toString()) {
                case "~" -> unaryBinaryNot();
                case "+" -> plus();
                case "-" -> minus();
                case "!" -> unaryBooleanNot();
                case "*" -> binaryMult();
                case "/" -> binaryDiv();
                case "%" -> binaryMod();
                case "<<" -> binaryBSL();
                case ">>" -> binaryBSR();
                case "<" -> binaryLT();
                case ">" -> binaryGT();
                case "==" -> binaryEq();
                case "!=" -> binaryNeq();
                case "&" -> binaryAnd();
                case "^" -> binaryXor();
                case "|" -> binaryOr();
                case "&&" -> booleanAnd();
                case "||" -> booleanOr();

                default -> throw new IllegalStateException("Bad operator in constant expression");
            };
        }

        private Number unaryBinaryNot() {
            Number rightVal = right.evaluate();
            return ~(rightVal.intValue());
        }

        private Number plus() {
            if (left == null) {
                return unaryPlus();
            } else {
                return binaryPlus();
            }
        }

        private Number unaryPlus() {
            return right.evaluate();
        }

        private Number binaryPlus() {
            Number leftVal = left.evaluate();
            Number rightVal = right.evaluate();

            if ((leftVal instanceof Double) || (rightVal instanceof Double)) {
                return leftVal.doubleValue() + rightVal.doubleValue();
            } else if ((leftVal instanceof Float) || (rightVal instanceof Float)) {
                return leftVal.floatValue() + rightVal.floatValue();
            } else if ((leftVal instanceof Integer) && (rightVal instanceof Integer)) {
                return leftVal.intValue() + rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated numbers apparently are not double, float, or int");
            }
        }

        private Number minus() {
            if (left == null) {
                return unaryMinus();
            } else {
                return binaryMinus();
            }
        }

        private Number unaryMinus() {
            Number rightVal = right.evaluate();

            if (rightVal instanceof Double) {
                return -rightVal.doubleValue();
            } else if (rightVal instanceof Float) {
                return -rightVal.floatValue();
            } else if (rightVal instanceof Integer) {
                return -rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated number apparently are not double, float, or int");
            }
        }

        private Number binaryMinus() {
            Number leftVal = left.evaluate();
            Number rightVal = right.evaluate();

            if ((leftVal instanceof Double) || (rightVal instanceof Double)) {
                return leftVal.doubleValue() - rightVal.doubleValue();
            } else if ((leftVal instanceof Float) || (rightVal instanceof Float)) {
                return leftVal.floatValue() - rightVal.floatValue();
            } else if ((leftVal instanceof Integer) && (rightVal instanceof Integer)) {
                return leftVal.intValue() - rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated numbers apparently are not double, float, or int");
            }
        }

        private Number unaryBooleanNot() {
            Number rightVal = right.evaluate();
            return asBool(rightVal) ? 0 : 1;
        }

        private Number binaryMult() {
            Number leftVal = left.evaluate();
            Number rightVal = right.evaluate();

            if ((leftVal instanceof Double) || (rightVal instanceof Double)) {
                return leftVal.doubleValue() * rightVal.doubleValue();
            } else if ((leftVal instanceof Float) || (rightVal instanceof Float)) {
                return leftVal.floatValue() * rightVal.floatValue();
            } else if ((leftVal instanceof Integer) && (rightVal instanceof Integer)) {
                return leftVal.intValue() * rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated numbers apparently are not double, float, or int");
            }
        }

        private Number binaryDiv() {
            Number leftVal = left.evaluate();
            Number rightVal = right.evaluate();

            if ((leftVal instanceof Double) || (rightVal instanceof Double)) {
                return leftVal.doubleValue() / rightVal.doubleValue();
            } else if ((leftVal instanceof Float) || (rightVal instanceof Float)) {
                return leftVal.floatValue() / rightVal.floatValue();
            } else if ((leftVal instanceof Integer) && (rightVal instanceof Integer)) {
                return leftVal.intValue() / rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated numbers apparently are not double, float, or int");
            }
        }

        private Number binaryMod() {
            Number leftVal = left.evaluate();
            Number rightVal = right.evaluate();

            if ((leftVal instanceof Double) || (rightVal instanceof Double)) {
                return leftVal.doubleValue() % rightVal.doubleValue();
            } else if ((leftVal instanceof Float) || (rightVal instanceof Float)) {
                return leftVal.floatValue() % rightVal.floatValue();
            } else if ((leftVal instanceof Integer) && (rightVal instanceof Integer)) {
                return leftVal.intValue() % rightVal.intValue();
            } else {
                throw new IllegalStateException("Evaluated numbers apparently are not double, float, or int");
            }
        }

        private Number binaryBSL() {
            return (left.evaluate().intValue()) << (right.evaluate().intValue());
        }

        private Number binaryBSR() {
            return (left.evaluate().intValue()) >> (right.evaluate().intValue());
        }

        private Number binaryLT() {
            if (left.evaluate().doubleValue() < right.evaluate().doubleValue()) {
                return 1;
            } else {
                return 0;
            }
        }

        private Number binaryGT() {
            if (left.evaluate().doubleValue() > right.evaluate().doubleValue()) {
                return 1;
            } else {
                return 0;
            }
        }

        private Number binaryEq() {
            if (left.evaluate().doubleValue() == right.evaluate().doubleValue()) {
                return 1;
            } else {
                return 0;
            }
        }

        private Number binaryNeq() {
            if (left.evaluate().doubleValue() != right.evaluate().doubleValue()) {
                return 1;
            } else {
                return 0;
            }
        }

        private Number binaryAnd() {
            return (left.evaluate().intValue()) & (right.evaluate().intValue());
        }

        private Number binaryXor() {
            return (left.evaluate().intValue()) ^ (right.evaluate().intValue());
        }

        private Number binaryOr() {
            return (left.evaluate().intValue()) | (right.evaluate().intValue());
        }

        private Number booleanAnd() {
            return (asBool(left.evaluate()) && asBool(right.evaluate())) ? 1 : 0;
        }

        private Number booleanOr() {
            return (asBool(left.evaluate()) || asBool(right.evaluate())) ? 1 : 0;
        }

        private Number ternary() {
            if (!right.operator.contentEquals(":")) {
                throw new IllegalArgumentException("? operator with no matching :");
            }

            if (asBool(left.evaluate())) {
                return right.left.evaluate();
            } else {
                return right.right.evaluate();
            }
        }


        private static int parseInt(String s) {
            if (s.startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            }
            if (s.startsWith("0b")) {
                return Integer.parseInt(s.substring(2), 2);
            }
            return Integer.parseInt(s);
        }

        private static boolean asBool(Number n) {
            return n.doubleValue() != 0.0;
        }
    }

    private static boolean isValidOperator(String operator) {
        return switch (operator) {
            case "(", ")" -> true;
            case "~", "+", "-", "!" -> true;
            case "*", "/", "%" -> true;
            case "<<", ">>", "<", ">" -> true;
            case "==", "!=" -> true;
            case "&", "^", "|" -> true;
            case "&&", "||" -> true;
            case "?", ":" -> true;

            default -> false;
        };
    }
}
