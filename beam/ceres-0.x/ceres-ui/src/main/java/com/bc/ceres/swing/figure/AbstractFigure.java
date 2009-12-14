package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.support.ScaleHandle;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFigure implements Figure {

    private List<FigureChangeListener> listenerList;
    private static final String OPERATION_NOT_SUPPORTED = "Operation not supported.";
    protected static final Handle[] NO_HANDLES = new Handle[0];

    private boolean selectable;
    private boolean selected;

    protected AbstractFigure() {
    }

    /**
     * The default implementation returns {@code false}.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        if (this.selectable != selectable) {
            this.selectable = selectable;
            fireFigureChanged();
        }
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            fireFigureChanged();
        }
    }

    /**
     * The default implementation returns {@code false}.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean contains(Figure figure) {
        return false;
    }

    /**
     * The default implementation returns {@code 0}.
     *
     * @return Always {@code 0}.
     */
    @Override
    public int getFigureCount() {
        return 0;
    }

    /**
     * The default implementation returns {@code 0}.
     *
     * @return Always {@code 0}.
     */
    @Override
    public int getFigureIndex(Figure figure) {
        return 0;
    }

    /**
     * The default implementation returns {@code null}.
     *
     * @return Always {@code null}.
     */
    @Override
    public Figure getFigure(Point2D p, AffineTransform m2v) {
        return null;
    }

    /**
     * The default implementation returns an empty array.
     *
     * @param shape The shape defining the area in which the figures must be contained.
     *
     * @return Always an empty array.
     */
    @Override
    public Figure[] getFigures(Shape shape) {
        return new Figure[0];
    }

    /**
     * The default implementation returns an empty array.
     *
     * @return Always an empty array.
     */
    @Override
    public Figure[] getFigures() {
        return new Figure[0];
    }

    @Override
    public synchronized boolean addFigure(Figure figure) {
        return addFigure(getFigureCount(), figure);
    }

    @Override
    public boolean addFigure(int index, Figure figure) {
        if (!contains(figure)) {
            boolean added = addFigureImpl(index, figure);
            if (added) {
                fireFigureChanged();
                fireFiguresAdded(figure);
            }
            return added;
        }
        return false;
    }

    @Override
    public synchronized Figure[] addFigures(Figure... figures) {
        Figure[] added = addFiguresImpl(figures);
        if (added.length > 0) {
            fireFigureChanged();
            fireFiguresAdded(added);
        }
        return added;
    }

    @Override
    public synchronized boolean removeFigure(Figure figure) {
        boolean removed = removeFigureImpl(figure);
        if (removed) {
            fireFigureChanged();
            fireFiguresRemoved(figure);
        }
        return removed;
    }

    @Override
    public Figure[] removeFigures(Figure... figures) {
        if (getFigureCount() > 0 && figures.length > 0) {
            Figure[] removed = removeFiguresImpl(figures);
            fireFigureChanged();
            fireFiguresRemoved(removed);
            return removed;
        }
        return new Figure[0];
    }


    @Override
    public Figure[] removeAllFigures() {
        if (getFigureCount() > 0) {
            Figure[] figures = removeFiguresImpl();
            fireFigureChanged();
            fireFiguresRemoved(figures);
            return figures;
        }
        return new Figure[0];
    }

    /**
     * The default implementation throws an [@code IllegalStateException}.
     *
     * @return Does never return normally.
     */
    @Override
    public Figure getFigure(int index) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    /**
     * The default implementation throws an [@code IllegalStateException}.
     *
     * @param index  The index.
     * @param figure The figure.
     *
     * @return Does never return normally.
     */
    protected boolean addFigureImpl(int index, Figure figure) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    protected boolean addFigureImpl(Figure figure) {
        return addFigureImpl(getFigureCount(), figure);
    }

    protected Figure[] addFiguresImpl(Figure[] figures) {
        ArrayList<Figure> added = new ArrayList<Figure>(figures.length);
        for (Figure figure : figures) {
            if (addFigureImpl(figure)) {
                added.add(figure);
            }
        }
        return added.toArray(new Figure[added.size()]);
    }

    /**
     * The default implementation throws an [@code IllegalStateException}.
     *
     * @param figure The figure.
     *
     * @return Does never return normally.
     */
    protected boolean removeFigureImpl(Figure figure) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    protected Figure[] removeFiguresImpl(Figure[] figures) {
        ArrayList<Figure> removed = new ArrayList<Figure>(figures.length);
        for (Figure figure : figures) {
            if (removeFigureImpl(figure)) {
                removed.add(figure);
            }
        }
        return removed.toArray(new Figure[removed.size()]);
    }

    protected Figure[] removeFiguresImpl() {
        return removeFiguresImpl(getFigures());
    }

    @Override
    public void move(double dx, double dy) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void scale(Point2D point, double sx, double sy) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void rotate(Point2D point, double theta) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public double[] getSegment(int index) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setSegment(int index, double[] segment) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void addSegment(int index, double[] segment) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void removeSegment(int index) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    /**
     * The default implementation returns zero.
     *
     * @return Always {@code 0}.
     */
    @Override
    public int getMaxSelectionStage() {
        return 0;
    }

    /**
     * The default implementation returns an empty array.
     * Clients should override in order to create an array of handles suitable for the given selection stage.
     *
     * @param selectionStage The selection stage. {@code 1 &lt;= selectionLevel &lt;=} {@link #getMaxSelectionStage()}
     */
    @Override
    public Handle[] createHandles(int selectionStage) {
        return NO_HANDLES;
    }

    /**
     * The default implementation returns [@code null}.
     * Clients should override in order to produce something more meaningful.
     *
     * @return Always {@code null}.
     */
    @Override
    public Object createMemento() {
        return null;
    }

    /**
     * The default implementation does nothing.
     * Clients should override in order to evaluate the passed in {@code memento}. object.
     *
     * @param memento A memento object.
     */
    @Override
    public void setMemento(Object memento) {
    }

    @Override
    public synchronized void addChangeListener(FigureChangeListener listener) {
        if (listenerList == null) {
            listenerList = new ArrayList<FigureChangeListener>(3);
        }
        listenerList.add(listener);
    }

    @Override
    public synchronized void removeChangeListener(FigureChangeListener listener) {
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }

    @Override
    public synchronized FigureChangeListener[] getChangeListeners() {
        if (listenerList != null) {
            return listenerList.toArray(new FigureChangeListener[listenerList.size()]);
        } else {
            return new FigureChangeListener[0];
        }
    }

    @Override
    public synchronized void dispose() {
        if (listenerList != null) {
            listenerList.clear();
            listenerList = null;
        }
    }

    @Override
    public Object clone() {
        try {
            final AbstractFigure figure = (AbstractFigure) super.clone();
            figure.listenerList = null;
            return figure;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void fireFigureChanged() {
        FigureChangeEvent event = FigureChangeEvent.createChangedEvent(this);
        for (FigureChangeListener listener : getChangeListeners()) {
            listener.figureChanged(event);
        }
    }

    protected void fireFiguresAdded(Figure... figures) {
        FigureChangeEvent event = FigureChangeEvent.createAddedEvent(this, figures);
        for (FigureChangeListener listener : getChangeListeners()) {
            listener.figuresAdded(event);
        }
    }

    protected void fireFiguresRemoved(Figure... figures) {
        FigureChangeEvent event = FigureChangeEvent.createRemovedEvent(this, figures);
        for (FigureChangeListener listener : getChangeListeners()) {
            listener.figuresRemoved(event);
        }
    }

    protected Handle[] createScaleHandles(double distance) {
        final ArrayList<Handle> handleList = new ArrayList<Handle>(8);

        FigureStyle handleStyle = getHandleStyle();

        //  TODO - reactivate RotateHandle and fix rotation implementation in 4.7-FINAL
//        handleList.add(new RotateHandle(this, handleStyle));

        handleList.add(new ScaleHandle(this, ScaleHandle.NW, -distance, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.NE, +distance, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.SE, +distance, +distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.SW, -distance, +distance, handleStyle));

        handleList.add(new ScaleHandle(this, ScaleHandle.N, 0.0, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.E, +distance, 0.0, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.S, 0.0, +distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.W, -distance, 0.0, handleStyle));

        return handleList.toArray(new Handle[handleList.size()]);
    }

    protected FigureStyle getHandleStyle() {
        return StyleDefaults.HANDLE_STYLE;
    }

    protected FigureStyle getSelectedHandleStyle() {
        return StyleDefaults.SELECTED_HANDLE_STYLE;
    }

}
