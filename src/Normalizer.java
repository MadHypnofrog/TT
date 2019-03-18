import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Normalizer {

    private Parser parser = new Parser();

    Expression normalize(String s) {
        return normalize(parser.parse(s));
    }

    Expression normalize(Expression ex)  {
        buildDeBruijn(ex, new ArrayList<>());
        while (true) {
            Pair<Expression, Integer> ans = findAndReplaceRedux(ex);
            if (ans.getValue() == 0) break;
            if (ans.getValue() == 2) ex = ans.getKey();
        }
        return fixVariables(ex, collectVars(ex), new ArrayList<>());
    }

    private void buildDeBruijn(Expression ex, ArrayList<String> lambdas) {
        if (ex instanceof Lambda) {
            lambdas.add(((Lambda) ex).variable);
            ((Lambda) ex).variable = "";
            buildDeBruijn(((Lambda) ex).nested, lambdas);
            lambdas.remove(lambdas.size() - 1);
        }
        if (ex instanceof Application) {
            buildDeBruijn(((Application) ex).first, lambdas);
            buildDeBruijn(((Application) ex).second, lambdas);
        }
        if (ex instanceof Variable) {
            int index = lambdas.lastIndexOf(((Variable) ex).name);
            if (index != -1) ((Variable) ex).name = Integer.toString(lambdas.size() - index - 1);
        }
    }

    private Pair<Expression, Integer> findAndReplaceRedux(Expression ex) {
        if (ex instanceof Variable) return new Pair<>(ex, 0);
        if (ex instanceof Lambda) {
            Pair<Expression, Integer> ans = findAndReplaceRedux(((Lambda) ex).nested);
            if (ans.getValue() == 2) {
                ((Lambda) ex).nested = ans.getKey();
                return new Pair<>(ex, 1);
            }
            return ans;
        }
        if (ex instanceof Application) {
            if (((Application) ex).first instanceof Lambda) {
                Expression newE = reduce(((Lambda) ((Application) ex).first).nested, ((Application) ex).second, 0);
                return new Pair<>(newE, 2);
            }
            Pair<Expression, Integer> ans = findAndReplaceRedux(((Application) ex).first);
            if (ans.getValue() == 2) {
                ((Application) ex).first = ans.getKey();
                return new Pair<>(ex, 1);
            }
            if (ans.getValue() == 1) return ans;
            ans = findAndReplaceRedux(((Application) ex).second);
            if (ans.getValue() == 2) {
                ((Application) ex).second = ans.getKey();
                return new Pair<>(ex, 1);
            }
            return ans;
        }
        return null;
    }

    private Expression reduce(Expression first, Expression second, int level) {
        if (first instanceof Variable) {
            String name = ((Variable) first).name;
            if (Character.isDigit(name.charAt(0))) {
                int index = Integer.valueOf(name);
                if (index > level) return new Variable(Integer.toString(index - 1));
                if (index == level) return addLevel(second, level, 0);
            }
            return first;
        }
        if (first instanceof Application) return new Application(reduce(((Application) first).first, second, level),
                                                                 reduce(((Application) first).second, second, level));
        if (first instanceof Lambda) return new Lambda("", reduce(((Lambda) first).nested, second, level + 1));
        return null;
    }

    private Expression addLevel(Expression ex, int level, int curLevel) {
        if (level == 0) return ex;
        if (ex instanceof Variable) {
            String name = ((Variable) ex).name;
            if (Character.isDigit(name.charAt(0))) {
                int index = Integer.valueOf(name);
                if (index >= curLevel) return new Variable(Integer.toString(index + level));
            }
            return new Variable(((Variable) ex).name);
        }
        if (ex instanceof Lambda) {
            return new Lambda("", addLevel(((Lambda) ex).nested, level, curLevel + 1));
        }
        if (ex instanceof Application) {
            return new Application(addLevel(((Application) ex).first, level, curLevel), addLevel(((Application) ex).second, level, curLevel));
        }
        return ex;
    }

    private Expression fixVariables(Expression ex, HashSet<String> freeVars, ArrayList<String> names) {
        if (ex instanceof Variable) {
            if (Character.isDigit(((Variable) ex).name.charAt(0))) {
                return new Variable(names.get(names.size() - 1 - Integer.valueOf(((Variable) ex).name)));
            }
        }
        if (ex instanceof Application) {
            return new Application(fixVariables(((Application) ex).first, freeVars, names),
            fixVariables(((Application) ex).second, freeVars, names));
        }
        if (ex instanceof Lambda) {
            String name = Character.toString((char)('a' + new Random().nextInt(26)));
            while (freeVars.contains(name)) name += '\'';
            freeVars.add(name);
            names.add(name);
            Expression newE = fixVariables(((Lambda) ex).nested, freeVars, names);
            names.remove(name);
            return new Lambda(name, newE);
        }
        return ex;
    }

    private HashSet<String> collectVars(Expression ex) {
        HashSet<String> ans = new HashSet<>();
        if (ex instanceof Variable) {
            if (!Character.isDigit(((Variable) ex).name.charAt(0))) {
                ans.add(((Variable) ex).name);
                return ans;
            }
        }
        if (ex instanceof Lambda) {
            return collectVars(((Lambda) ex).nested);
        }
        if (ex instanceof Application) {
            ans.addAll(collectVars(((Application) ex).first));
            ans.addAll(collectVars(((Application) ex).second));
        }
        return ans;
    }
}
