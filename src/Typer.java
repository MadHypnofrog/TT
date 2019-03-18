import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Typer {

    private Parser parser = new Parser();

    String type(String s) {
        return type(parser.parse(s));
    }

    String type(Expression ex) {
        HashMap<Type, ArrayList<Type>> equations = buildEquations(ex, new HashMap<>());
        try {
            equations = solve(equations);
        } catch (IllegalArgumentException e) {
            return "Expression has no type";
        }
        replaceTypes(ex, equations);
        return buildProof(ex).stream().collect(Collectors.joining("\n"));
    }

    private List<String> buildProof(Expression ex) {
        List<String> res = new ArrayList<>();
        if (ex instanceof Variable) {
            String varName = ((Variable) ex).name;
            res.add(varName + " : " + ex.type + " |- " + varName + " : " + ex.type + " [rule #1]");
        }
        if (ex instanceof Lambda) {
            String varName = ((Lambda) ex).variable;
            List<String> prfNested = buildProof(((Lambda) ex).nested);
            String[] vars = prfNested.get(0).split("\\|-")[0].trim().split(", ");
            String context = Arrays.stream(vars).filter(s -> !s.startsWith(varName + " : ")).collect(Collectors.joining(", "));
            if (!context.isEmpty()) context += " ";
            res.add(context + "|- " + ex + " : " + ex.type + " [rule #3]");
            HashSet<String> addContext = new HashSet<>();
            addContext.add(((Lambda) ex).variable + " : " + ((Implication)ex.type).first);
            for (String s: prfNested) res.add("*   " + addToContext(s, addContext));
        }
        if (ex instanceof Application) {
            List<String> prfLeft = buildProof(((Application) ex).first);
            List<String> prfRight = buildProof(((Application) ex).second);
            String[] varsFirst = prfLeft.get(0).split("\\|-")[0].trim().split(", ");
            String[] varsSecond = prfRight.get(0).split("\\|-")[0].trim().split(", ");
            HashSet<String> united = new HashSet<>(Arrays.asList(varsFirst));
            Collections.addAll(united, varsSecond);
            united.remove("");
            String context = united.stream().collect(Collectors.joining(", "));
            if (!context.isEmpty()) context += " ";
            res.add(context + "|- " + ex + " : " + ex.type + " [rule #2]");
            prfLeft.addAll(prfRight);
            if (context.isEmpty()) {
                for (String s: prfLeft) {
                    res.add("*   " + s);
                }
            } else {
                for (String s: prfLeft) {
                    res.add("*   " + addToContext(s, united));
                }
            }
        }
        return res;
    }

    private String addToContext(String s, HashSet<String> newContext) {
        String[] oldWithIndent = s.split("\\|-")[0].split("\\*   ");
        String[] oldContext;
        if (oldWithIndent.length == 0) oldContext = new String[0];
        else oldContext = oldWithIndent[oldWithIndent.length - 1].trim().split(", ");
        HashSet<String> nc = new HashSet<>(Arrays.asList(oldContext));
        nc.remove("");
        nc.addAll(newContext);
        String context = nc.stream().collect(Collectors.joining(", "));
        if (!context.isEmpty()) context += " ";
        int dex = s.lastIndexOf("*   ") + 4;
        if (dex == 3) dex = 0;
        return s.substring(0, dex) + context + "|-" + s.split("\\|-")[1];
    }

    private HashMap<Type, ArrayList<Type>> buildEquations(Expression ex, HashMap<String, Type> bound) {
        HashMap<Type, ArrayList<Type>> ans = new HashMap<>();
        if (ex instanceof Variable) {
            if (bound.containsKey(((Variable) ex).name)) ex.type = bound.get(((Variable) ex).name);
            else ex.type = new Simple("0" + ((Variable) ex).name);
        }
        if (ex instanceof Application) {
            ans.putAll(buildEquations(((Application) ex).first, bound));
            ans.putAll(buildEquations(((Application) ex).second, bound));
            ex.type = new Simple(getRandomTypeName());
            ans.computeIfAbsent(((Application) ex).first.type, (k) -> new ArrayList<>())
                    .add(new Implication(((Application) ex).second.type, ex.type));
        }
        if (ex instanceof Lambda) {
            Type varType = new Simple(getRandomTypeName());
            bound.put(((Lambda) ex).variable, varType);
            ans.putAll(buildEquations(((Lambda) ex).nested, bound));
            bound.remove(((Lambda) ex).variable);
            ex.type = new Implication(varType, ((Lambda) ex).nested.type);
        }
        return ans;
    }

    private String getRandomTypeName() {
        String res = "";
        res += (char) ('0' + new Random().nextInt(9));
        res += (char) ('0' + new Random().nextInt(9));
        res += (char) ('0' + new Random().nextInt(9));
        res += (char) ('0' + new Random().nextInt(9));
        return res;
    }

    private HashMap<Type, ArrayList<Type>> solve(HashMap<Type, ArrayList<Type>> equations) throws IllegalArgumentException {
        HashMap<Type, ArrayList<Type>> helper = new HashMap<>();
        while (true) {
            while (!equations.isEmpty()) {
                Iterator<Map.Entry<Type, ArrayList<Type>>> it = equations.entrySet().iterator();
                HashMap<Type, ArrayList<Type>> newEq = new HashMap<>();
                while (it.hasNext()) {
                    Map.Entry<Type, ArrayList<Type>> e = it.next();
                    for (Type right : e.getValue()) {
                        if (e.getKey().equals(right)) continue;
                        if (e.getKey() instanceof Implication && right instanceof Implication) {
                            newEq.computeIfAbsent(((Implication) e.getKey()).first, (k) -> new ArrayList<>()).add(((Implication) right).first);
                            newEq.computeIfAbsent(((Implication) e.getKey()).second, (k) -> new ArrayList<>()).add(((Implication) right).second);
                        } else {
                            if (e.getKey() instanceof Implication) {
                                helper.computeIfAbsent(right, (k) -> new ArrayList<>()).add(e.getKey());
                            } else {
                                helper.computeIfAbsent(e.getKey(), (k) -> new ArrayList<>()).add(right);
                            }
                        }
                    }
                }
                equations = newEq;
            }

            HashSet<Type> typesRight = getTypesRight(helper);
            for (Type t : helper.keySet()) {
                if (typesRight.contains(t) || helper.get(t).size() > 1) {
                    Type replacement = helper.get(t).get(0);
                    if (getTypes(replacement).contains(t)) throw new IllegalArgumentException();
                    helper.get(t).remove(replacement);
                    for (Type other : helper.get(t)) {
                        if (getTypes(other).contains(t)) throw new IllegalArgumentException();
                        equations.computeIfAbsent(replacement, (k) -> new ArrayList<>()).add(other);
                    }
                    helper.get(t).clear();
                    helper.get(t).add(replacement);
                    HashMap<Type, ArrayList<Type>> reversed = reverse(helper);
                    for (Type r: reversed.keySet()) {
                        if (getTypes(r).contains(t)) {
                            for (Type left: reversed.get(r)) {
                                helper.get(left).remove(r);
                                equations.computeIfAbsent(left, (k) -> new ArrayList<>()).add(replaceAll(r, t, replacement));
                            }
                        }
                    }
                    break;
                }
            }

            if (equations.isEmpty()) return helper;
        }
    }

    private HashMap<Type, ArrayList<Type>> reverse(HashMap<Type, ArrayList<Type>> map) {
        HashMap<Type, ArrayList<Type>> res = new HashMap<>();
        for (Type t : map.keySet()) {
            for (Type r : map.get(t)) {
                res.computeIfAbsent(r, (k) -> new ArrayList<>()).add(t);
            }
        }
        return res;
    }

    private HashSet<Type> getTypesRight(HashMap<Type, ArrayList<Type>> map) {
        HashSet<Type> res = new HashSet<>();
        for (ArrayList<Type> l : map.values()) {
            for (Type t : l) {
                res.addAll(getTypes(t));
            }
        }
        return res;
    }

    private HashSet<Type> getTypes(Type t) {
        HashSet<Type> res = new HashSet<>();
        if (t instanceof Simple) res.add(t);
        else if (t instanceof Implication) {
            res.addAll(getTypes(((Implication) t).first));
            res.addAll(getTypes(((Implication) t).second));
        }
        return res;
    }

    private Type replaceAll(Type first, Type toReplace, Type replacement) {
        if (first instanceof Simple) {
            if (first.equals(toReplace)) return copy(replacement);
        } else if (first instanceof Implication) {
            return new Implication(replaceAll(((Implication) first).first, toReplace, replacement),
                    replaceAll(((Implication) first).second, toReplace, replacement));
        }
        return first;
    }

    private Type copy(Type other) {
        if (other instanceof Simple) return new Simple(((Simple) other).name);
        else if (other instanceof Implication) return new Implication(copy(((Implication) other).first), copy(((Implication) other).second));
        return null;
    }

    private void replaceTypes(Expression ex, HashMap<Type, ArrayList<Type>> rep) {
        ex.type = replaceType(ex.type, rep);
        if (ex instanceof Lambda) replaceTypes(((Lambda) ex).nested, rep);
        if (ex instanceof Application) {
            replaceTypes(((Application) ex).first, rep);
            replaceTypes(((Application) ex).second, rep);
        }
    }

    private Type replaceType(Type first, HashMap<Type, ArrayList<Type>> rep) {
        if (first instanceof Simple) {
            if (rep.containsKey(first)) return rep.get(first).get(0);
            else return first;
        }
        if (first instanceof Implication) {
            return new Implication(replaceType(((Implication) first).first, rep), replaceType(((Implication) first).second, rep));
        }
        return null;
    }
}
