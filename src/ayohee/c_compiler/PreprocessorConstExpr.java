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
        return root.evaluate();
    }

    private class ConstExprTreeNode {
        private ConstExprTreeNode left;
        private ConstExprTreeNode right;
        private PreprocessingToken value;

        public ConstExprTreeNode(ConstExprTreeNode _left, ConstExprTreeNode _right, PreprocessingToken _value) {
            left = _left;
            right = _right;
            value = _value;
        }

        public PreprocessingToken evaluate() {
            if (value.is(PreprocessingToken.TokenType.PP_NUMBER)) {
                return value;
            }

            String operator = value.toString();
            if (operator.contentEquals("?")) {
                //ternary doesn't follow the usual execution path
                return ternary();
            }

            PreprocessingToken leftVal = left.evaluate();
            PreprocessingToken rightVal = right.evaluate();
            if (!leftVal.is(PreprocessingToken.TokenType.PP_NUMBER) || !rightVal.is(PreprocessingToken.TokenType.PP_NUMBER)) {
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

        private PreprocessingToken unaryBinaryNot() {
        }

        private PreprocessingToken plus() {
            if (left == null) {
                return unaryPlus();
            } else {
                return binaryPlus();
            }
        }

        private PreprocessingToken unaryPlus() {
        }

        private PreprocessingToken binaryPlus() {
        }

        private PreprocessingToken minus() {
            if (left == null) {
                return unaryMinus();
            } else {
                return binaryMinus();
            }
        }

        private PreprocessingToken unaryMinus() {
        }

        private PreprocessingToken binaryMinus() {
        }

        private PreprocessingToken unaryBooleanNot() {
        }

        private PreprocessingToken binaryMult() {
        }

        private PreprocessingToken binaryDiv() {
        }

        private PreprocessingToken binaryMod() {
        }

        private PreprocessingToken binaryBSL() {
        }

        private PreprocessingToken binaryBSR() {
        }

        private PreprocessingToken binaryLT() {
        }

        private PreprocessingToken binaryGT() {
        }

        private PreprocessingToken binaryEq() {
        }

        private PreprocessingToken binaryNeq() {
        }

        private PreprocessingToken binaryAnd() {
        }

        private PreprocessingToken binaryXor() {
        }

        private PreprocessingToken binaryOr() {
        }

        private PreprocessingToken booleanAnd() {
        }

        private PreprocessingToken booleanOr() {
        }

        private PreprocessingToken ternary() {
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
