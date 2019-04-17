package moa.gui.experimentertab.statisticaltests;

/**
 * <p>
 * T�tulo:
 * <p>
 * Descripci�n: </p>
 * <p>
 * Copyright: Copyright (c) 2005</p>
 * <p>
 * Empresa: </p>
 *
 * @author sin atribuir
 * @version 1.0
 */
public class Relation {

    public int i;
    public int j;

    public Relation() {

    }

    public Relation(int x, int y) {
        i = x;
        j = y;
    }

    @Override
    public String toString() {
        return "(" + i + "," + j + ")";
    }

}
