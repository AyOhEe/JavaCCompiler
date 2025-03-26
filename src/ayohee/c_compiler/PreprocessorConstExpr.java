package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


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

        root = constructTree(_condition);
    }

    private static ConstExprTreeNode constructTree(List<PreprocessingToken> condition) {
        List<ConstExprTreeNode> nodes = new ArrayList<>();
        List<Integer> priorities = new ArrayList<>();
        Stack<Integer> lastLParen = new Stack<>();

        //look for and deal with nested expressions
        for (int i = 0; i < condition.size(); ++i) {
            PreprocessingToken token = condition.get(i);

            if (token.is("(")) {
                lastLParen.add(i);
            }
            if (token.is(")") && lastLParen.size() == 1) {
                List<PreprocessingToken> subExpression = condition.subList(lastLParen.pop() + 1, i);
                ConstExprTreeNode resultantTree = constructTree(subExpression);

                if (resultantTree != null) {
                    nodes.add(resultantTree);
                }

                priorities.add(0); //subexpressions should have the same priority as values. functionally, they *are* values
            }

            if (lastLParen.empty()) {
                priorities.add(operatorPriority(token.toString()));
                if (token.is(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR)) {
                    nodes.add(new ConstExprTreeNode(null, null, token.toString()));
                } else {
                    nodes.add(new ConstExprTreeNode(null, null, ConstExprTreeNode.parseNum(token.toString())));
                }
            }
        }

        if (!lastLParen.isEmpty()) {
            throw new IllegalStateException("Unmatched lParen in constant expression");
        }

        return assembleTreeFromPriorities(nodes, priorities);
    }

    private static ConstExprTreeNode assembleTreeFromPriorities(List<ConstExprTreeNode> nodes, List<Integer> priorities) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Empty constant expression");
        }

        while (nodes.size() > 1) {
            int nextTarget = firstLargest(priorities);
            priorities.remove(nextTarget);

            ConstExprTreeNode next = nodes.remove(nextTarget);
            if (next.value != null) {
                //only values. either we're done, or the expression was bad from the start
                break;
            }

            switch (next.operator) {
                case "~", "!":
                    ConstExprTreeNode right = nodes.remove(nextTarget); //the node to the right shifted by one when we removed the operator
                    priorities.remove(nextTarget);

                    nodes.add(nextTarget, new ConstExprTreeNode(null, right, next.operator));
                    priorities.add(nextTarget, 0);
                    break;

                case "+", "-":
                    if (nextTarget == 0 || nodes.get(nextTarget - 1).hasChildren() || nodes.get(nextTarget - 1).value != null) {
                        //unary
                        right = nodes.remove(nextTarget);
                        priorities.remove(nextTarget);

                        nodes.add(nextTarget, new ConstExprTreeNode(null, right, next.operator));
                        priorities.add(nextTarget, 0);
                    } else {
                        //binary - defer until later to preserve priority
                        ConstExprTreeNode left = nodes.get(nextTarget - 1);
                        left.operator = "b" + left.operator;
                        priorities.set(nextTarget - 1, operatorPriority(left.operator));
                    }
                    break;

                default:
                    right = nodes.remove(nextTarget);
                    ConstExprTreeNode left = nodes.remove(nextTarget - 1);
                    priorities.remove(nextTarget);
                    priorities.remove(nextTarget - 1);

                    nodes.add(nextTarget - 1, new ConstExprTreeNode(left, right, next.operator));
                    priorities.add(nextTarget - 1, 0);
            }
        }

        if (nodes.size() == 1) {
            return nodes.getFirst();
        } else {
            throw new IllegalStateException("Constant expression had values without operators");
        }
    }

    private static <T extends Comparable<T>> int firstLargest(List<T> list) {
        T max = list.getFirst();
        int maxIndex = 0;
        int i = 0;

        for (T v : list) {
            if (v.compareTo(max) > 0) {
                max = v;
                maxIndex = i;
            }
            ++i;
        }

        return maxIndex;
    }

    public PreprocessingToken evaluate(PreprocessingContext context) throws CompilerException {
        return new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, root.evaluate().toString());
    }

    private static class ConstExprTreeNode {
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

            if (operator.contentEquals("?")) {
                //ternary doesn't follow the usual execution path
                return ternary();
            }

            if ((left != null && left.operator != null) || (right != null && right.operator != null)) {
                throw new IllegalStateException("Attempted to apply operator to another operator: " + left + ", " + operator + ", " + right);
            }

            return switch (operator) {
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


        private static Number parseNum(String s) {
            if (s.startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            } else if (s.startsWith("0b")) {
                return Integer.parseInt(s.substring(2), 2);
            } else if (s.contains("f")) {
                return Float.parseFloat(s);
            } else if (s.contains(".")) {
                return Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        }

        private static boolean asBool(Number n) {
            return n.doubleValue() != 0.0;
        }

        public boolean hasChildren() {
            return (left != null) || (right != null);
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

    private static int operatorPriority(String operator) {
        return switch (operator) {
            case "~" -> 22;
            case "+" -> 21;
            case "-" -> 20;
            case "!" -> 19;
            case "*" -> 18;
            case "/" -> 17;
            case "%" -> 16;
            case "b+" -> 15;
            case "b-" -> 14;
            case "<<" -> 13;
            case ">>" -> 12;
            case "<" -> 11;
            case ">" -> 10;
            case "==" -> 9;
            case "!=" -> 8;
            case "&" -> 7;
            case "^" -> 6;
            case "|" -> 5;
            case "&&" -> 4;
            case "||" -> 3;
            case "?" -> 2;
            case ":" -> 1;

            default -> 0;
        };
    }
}
