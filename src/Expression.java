abstract class Expression {
    Type type;
}

class Variable extends Expression {
    Variable(String n) {
        name = n;
    }
    String name;
    public String toString() {
        return name;
    }
}

class Lambda extends Expression {
    Lambda(String v, Expression n) {
        variable = v;
        nested = n;
    }
    String variable;
    Expression nested;
    public String toString() {
        return "(\\" + variable + "." + nested.toString() + ")";
    }
}

class Application extends Expression {
    Application(Expression f, Expression s) {
        first = f;
        second = s;
    }
    Expression first, second;
    public String toString() {
        return "(" + first.toString() + " " + second.toString() + ")";
    }
}
