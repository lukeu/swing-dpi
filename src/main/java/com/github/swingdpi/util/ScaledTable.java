package com.github.swingdpi.util;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.github.swingdpi.UiScaling;

public class ScaledTable extends JTable {

    /**
     * null until setRowHeight() is called, after which it records the scalingset when that
     * method was called. If the scaling preference is later updated, the table updates the
     * row-height accordingly, and sets this value again.
     */
    private Integer uiScaleWhenRowHeightSet = null;

    public ScaledTable() {
    }

    public ScaledTable(TableModel dm) {
        super(dm);
    }

    public ScaledTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public ScaledTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ScaledTable(Vector<?> rowData, Vector<?> columnNames) {
        super((Vector<? extends Vector>) rowData, (Vector<?>) columnNames);
    }

    public ScaledTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    public ScaledTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        resetDefaultRowHeight();
        rescaleRowHeightIfExplicitlySet();
    }

    /**
     * Use a more modern default row height (19 @ 100% scaling).
     * Calling setRowHeight() overrides this default.
     */
    private void resetDefaultRowHeight() {
        int height = UiScaling.scale(19);
        LookAndFeel.installProperty(this, "rowHeight", height);
    }

    private void rescaleRowHeightIfExplicitlySet() {

        // Confusingly, since our base class's constructor calls setRowHeight() and updateUI(),
        // m_ScalingWhenRowHeightSet is non-null in the first call here, during construction!
        // However the the following 'if' condition will be false, which is what we want.
        // Then m_ScalingWhenRowHeightSet gets set back to null until setRowHeight() is caled
        if (uiScaleWhenRowHeightSet != null && uiScaleWhenRowHeightSet != UiScaling.getScaling()) {

            // TODO: this may have rounding errors so may 'drift' after multiple changes.
            setRowHeight(getRowHeight() * UiScaling.getScaling() / uiScaleWhenRowHeightSet);
        }
    }

    /**
     * NB: the argument is expected to have scaling applied.
     *
     * {@inheritDoc}
     */
    @Override
    public void setRowHeight(int scaledRowHeight) {
        super.setRowHeight(scaledRowHeight);
        uiScaleWhenRowHeightSet  = UiScaling.getScaling();
    }
}
