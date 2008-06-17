/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2008, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * -------------------------
 * CategoryStepRenderer.java
 * -------------------------
 *
 * (C) Copyright 2004-2008, by Brian Cole and Contributors.
 *
 * Original Author:  Brian Cole;
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *
 * Changes
 * -------
 * 21-Apr-2004 : Version 1, contributed by Brian Cole (DG);
 * 22-Apr-2004 : Fixed Checkstyle complaints (DG);
 * 05-Nov-2004 : Modified drawItem() signature (DG);
 * 08-Mar-2005 : Added equals() method (DG);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 30-Nov-2006 : Added checks for series visibility (DG);
 * 22-Feb-2007 : Use new state object for reusable line, enable chart entities
 *               (for tooltips, URLs), added new getLegendItem() override (DG);
 * 20-Apr-2007 : Updated getLegendItem() for renderer change (DG);
 * 18-May-2007 : Set dataset and seriesKey for LegendItem (DG);
 * 17-Jun-2008 : Apply legend shape, font and paint attributes (DG);
 *
 */

package org.jfree.chart.renderer.category;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.PublicCloneable;

/**
 * A "step" renderer similar to {@link XYStepRenderer} but
 * that can be used with the {@link CategoryPlot} class.
 */
public class CategoryStepRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {

    /**
     * State information for the renderer.
     */
    protected static class State extends CategoryItemRendererState {

        /**
         * A working line for re-use to avoid creating large numbers of
         * objects.
         */
        public Line2D line;

        /**
         * Creates a new state instance.
         *
         * @param info  collects plot rendering information (<code>null</code>
         *              permitted).
         */
        public State(PlotRenderingInfo info) {
            super(info);
            this.line = new Line2D.Double();
        }

    }

    /** For serialization. */
    private static final long serialVersionUID = -5121079703118261470L;

    /** The stagger width. */
    public static final int STAGGER_WIDTH = 5; // could make this configurable

    /**
     * A flag that controls whether or not the steps for multiple series are
     * staggered.
     */
    private boolean stagger = false;

    /**
     * Creates a new renderer (stagger defaults to <code>false</code>).
     */
    public CategoryStepRenderer() {
        this(false);
    }

    /**
     * Creates a new renderer.
     *
     * @param stagger  should the horizontal part of the step be staggered by
     *                 series?
     */
    public CategoryStepRenderer(boolean stagger) {
        this.stagger = stagger;
        setBaseLegendShape(new Rectangle2D.Double(-4.0, -3.0, 8.0, 6.0));
    }

    /**
     * Returns the flag that controls whether the series steps are staggered.
     *
     * @return A boolean.
     */
    public boolean getStagger() {
        return this.stagger;
    }

    /**
     * Sets the flag that controls whether or not the series steps are
     * staggered and sends a {@link RendererChangeEvent} to all registered
     * listeners.
     *
     * @param shouldStagger  a boolean.
     */
    public void setStagger(boolean shouldStagger) {
        this.stagger = shouldStagger;
        fireChangeEvent();
    }

    /**
     * Returns a legend item for a series.
     *
     * @param datasetIndex  the dataset index (zero-based).
     * @param series  the series index (zero-based).
     *
     * @return The legend item.
     */
    public LegendItem getLegendItem(int datasetIndex, int series) {

        CategoryPlot p = getPlot();
        if (p == null) {
            return null;
        }

        // check that a legend item needs to be displayed...
        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {
            return null;
        }

        CategoryDataset dataset = p.getDataset(datasetIndex);
        String label = getLegendItemLabelGenerator().generateLabel(dataset,
                series);
        String description = label;
        String toolTipText = null;
        if (getLegendItemToolTipGenerator() != null) {
            toolTipText = getLegendItemToolTipGenerator().generateLabel(
                    dataset, series);
        }
        String urlText = null;
        if (getLegendItemURLGenerator() != null) {
            urlText = getLegendItemURLGenerator().generateLabel(dataset,
                    series);
        }
        Shape shape = lookupLegendShape(series);
        Paint paint = lookupSeriesPaint(series);

        LegendItem item = new LegendItem(label, description, toolTipText,
                urlText, shape, paint);
        item.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
        	item.setLabelPaint(labelPaint);
        }
        item.setSeriesKey(dataset.getRowKey(series));
        item.setSeriesIndex(series);
        item.setDataset(dataset);
        item.setDatasetIndex(datasetIndex);
        return item;
    }

    /**
     * Creates a new state instance.  This method is called from
     * {@link #initialise(Graphics2D, Rectangle2D, CategoryPlot, int,
     * PlotRenderingInfo)}, and we override it to ensure that the state
     * contains a working Line2D instance.
     *
     * @param info  the plot rendering info (<code>null</code> is permitted).
     *
     * @return A new state instance.
     */
    protected CategoryItemRendererState createState(PlotRenderingInfo info) {
        return new State(info);
    }

    /**
     * Draws a line taking into account the specified orientation.
     * <p>
     * In version 1.0.5, the signature of this method was changed by the
     * addition of the 'state' parameter.  This is an incompatible change, but
     * is considered a low risk because it is unlikely that anyone has
     * subclassed this renderer.  If this *does* cause trouble for you, please
     * report it as a bug.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param orientation  the plot orientation.
     * @param x0  the x-coordinate for the start of the line.
     * @param y0  the y-coordinate for the start of the line.
     * @param x1  the x-coordinate for the end of the line.
     * @param y1  the y-coordinate for the end of the line.
     */
    protected void drawLine(Graphics2D g2, State state,
            PlotOrientation orientation, double x0, double y0, double x1,
            double y1) {

        if (orientation == PlotOrientation.VERTICAL) {
            state.line.setLine(x0, y0, x1, y1);
            g2.draw(state.line);
        }
        else if (orientation == PlotOrientation.HORIZONTAL) {
            state.line.setLine(y0, x0, y1, x1); // switch x and y
            g2.draw(state.line);
        }

    }

    /**
     * Draw a single data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area in which the data is drawn.
     * @param plot  the plot.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param pass  the pass index.
     */
    public void drawItem(Graphics2D g2,
                         CategoryItemRendererState state,
                         Rectangle2D dataArea,
                         CategoryPlot plot,
                         CategoryAxis domainAxis,
                         ValueAxis rangeAxis,
                         CategoryDataset dataset,
                         int row,
                         int column,
                         int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(row, column)) {
            return;
        }

        Number value = dataset.getValue(row, column);
        if (value == null) {
            return;
        }
        PlotOrientation orientation = plot.getOrientation();

        // current data point...
        double x1s = domainAxis.getCategoryStart(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());
        double x1 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());
        double x1e = 2 * x1 - x1s; // or: x1s + 2*(x1-x1s)
        double y1 = rangeAxis.valueToJava2D(value.doubleValue(), dataArea,
                plot.getRangeAxisEdge());
        g2.setPaint(getItemPaint(row, column));
        g2.setStroke(getItemStroke(row, column));

        if (column != 0) {
            Number previousValue = dataset.getValue(row, column - 1);
            if (previousValue != null) {
                // previous data point...
                double previous = previousValue.doubleValue();
                double x0s = domainAxis.getCategoryStart(column - 1,
                        getColumnCount(), dataArea, plot.getDomainAxisEdge());
                double x0 = domainAxis.getCategoryMiddle(column - 1,
                        getColumnCount(), dataArea, plot.getDomainAxisEdge());
                double x0e = 2 * x0 - x0s; // or: x0s + 2*(x0-x0s)
                double y0 = rangeAxis.valueToJava2D(previous, dataArea,
                        plot.getRangeAxisEdge());
                if (getStagger()) {
                    int xStagger = row * STAGGER_WIDTH;
                    if (xStagger > (x1s - x0e)) {
                        xStagger = (int) (x1s - x0e);
                    }
                    x1s = x0e + xStagger;
                }
                drawLine(g2, (State) state, orientation, x0e, y0, x1s, y0);
                // extend x0's flat bar

                drawLine(g2, (State) state, orientation, x1s, y0, x1s, y1);
                // upright bar
           }
       }
       drawLine(g2, (State) state, orientation, x1s, y1, x1e, y1);
       // x1's flat bar

       // draw the item labels if there are any...
       if (isItemLabelVisible(row, column)) {
            drawItemLabel(g2, orientation, dataset, row, column, x1, y1,
                    (value.doubleValue() < 0.0));
       }

       // add an item entity, if this information is being collected
       EntityCollection entities = state.getEntityCollection();
       if (entities != null) {
           Rectangle2D hotspot = new Rectangle2D.Double();
           if (orientation == PlotOrientation.VERTICAL) {
               hotspot.setRect(x1s, y1, x1e - x1s, 4.0);
           }
           else {
               hotspot.setRect(y1 - 2.0, x1s, 4.0, x1e - x1s);
           }
           addItemEntity(entities, dataset, row, column, hotspot);
       }

    }

    /**
     * Tests this renderer for equality with an arbitrary object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CategoryStepRenderer)) {
            return false;
        }
        CategoryStepRenderer that = (CategoryStepRenderer) obj;
        if (this.stagger != that.stagger) {
            return false;
        }
        return super.equals(obj);
    }

}
