package moa.gui.experimentertab.statisticaltests;

/**
 * T�tulo:
 *
 * <p>Descripci�n:
 *
 * <p>Copyright: Copyright (c) 2005
 *
 * <p>Empresa:
 *
 * @author sin atribuir
 * @version 1.0
 */
public class Relation {

    public int i;
    public int j;

    public Relation() {}

    public Relation(int x, int y) {
        i = x;
        j = y;
    }

    @Override
    public String toString() {
        return "(" + i + "," + j + ")";
    }
}
