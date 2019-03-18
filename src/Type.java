public interface Type {
    @Override
    public boolean equals(Object obj);
}

class Simple implements Type {
    Simple(String s) {
        name = s;
    }
    String name;
    public String toString() {
        return "t" + name;
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Simple)) return false;
        return ((Simple) obj).name.equals(this.name);
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

class Implication implements Type {
    Implication(Type t1, Type t2) {
        first = t1;
        second = t2;
    }
    Type first, second;
    public String toString() {
        return "(" + first + " -> " + second + ")";
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Implication)) return false;
        return ((Implication) obj).first.equals(this.first) && ((Implication) obj).second.equals(this.second);
    }
    @Override
    public int hashCode() {
        return first.hashCode() * 29 + second.hashCode();
    }
}