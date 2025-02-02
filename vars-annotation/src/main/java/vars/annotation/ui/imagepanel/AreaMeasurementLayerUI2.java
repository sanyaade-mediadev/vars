/*
 * @(#)AreaMeasurementLayerUI2.java   2013.02.04 at 03:57:35 PST
 *
 * Copyright 2009 MBARI
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package vars.annotation.ui.imagepanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.jxlayer.JXLayer;
import org.mbari.awt.AwtUtilities;
import org.mbari.geometry.Point2D;
import org.mbari.swing.JImageCanvas;
import org.mbari.swing.JImageUrlCanvas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vars.ToolBelt;
import vars.annotation.Observation;


/**
 * @author Brian Schlining
 * @since 2013-02-04
 *
 * @param <T>
 */
public class AreaMeasurementLayerUI2<T extends JImageUrlCanvas> extends ImageFrameLayerUI<T> {

    /*

       NOTES:

       IC = image coordinates, e.g. pixels. This can be stored but need to be converted to
           component coordinates to be drawn.

       CC = component coordinates, e.g. suitable for drawing with Graphics2D

    */

    /** The diameter of the start of measurement marker */
    private static final int markerDiameter = 10;

    //extends CrossHairLayerUI<T> {

    //private JXPainter<T> crossHairPainter = new JXCrossHairPainter<T>();
    private JXPainter<T> notSelectedObservationsPainter = new JXNotSelectedObservationsPainter<T>(MarkerStyle.FAINT);
    private JXPainter<T> selectedObservationsPainter = new JXSelectedObservationsPainter<T>(MarkerStyle.NOTSELECTED);
    private JXPainter<T> selectedAreaMeasurementPainter = new JXSelectedAreaMeasurementPainter<T>();
    private JXHorizontalLinePainter<T> horizontalLinePainter;
    private Logger log = LoggerFactory.getLogger(getClass());

    /** Collection of pointsIC to be used to create the AreaMeasurement in image pixels */
    private List<Point2D<Integer>> pointsIC = new CopyOnWriteArrayList<Point2D<Integer>>();

    /** Path used to draw measurement polygonCC in component coordinates*/
    private GeneralPath polygonCC = new GeneralPath();

    /** The current point where the mouse is in component coordinates*/
    private java.awt.geom.Point2D currentPointCC = new java.awt.geom.Point2D.Double();

    /**
     * Path used to draw the triangle that will be added to the polygonCC if user addes another
     * click. In component coordiantes*/
    private GeneralPath bridgeCC = new GeneralPath();

    /**
     * Runnable that resets the measurment UI state
     */
    private final Runnable resetRunable = new Runnable() {

        @Override
        public void run() {

            // TODO finish implementation
            polygonCC.reset();
            bridgeCC.reset();

            //areaMeasurementPaths.clear();
            pointsIC.clear();

            //relatedObservations.clear();
            setDirty(true);
        }
    };

    /**
     * This is a reference to the image being drawn by the JImageUrlCanvas. THis is needed so
     * that the area measurement doesn't extend outside the bounds of the image. It needs to be
     * a bufferedImage so that we can read the width and height easily.
     */
    private BufferedImage image;

    /** The observation that we're currently adding measurements to */
    private Observation observation;
    private final ToolBelt toolBelt;

    /**
     * Constructs ...
     *
     * @param toolBelt
     */
    public AreaMeasurementLayerUI2(ToolBelt toolBelt, JImageCanvas imageCanvas,
                                   CommonPainters<T> commonPainters) {
        super(commonPainters);
        horizontalLinePainter = commonPainters.getHorizontalLinePainter();
        setDisplayName("Area");
        setSettingsBuilder(new AreaMeasurementLayerSettingsBuilder<T>(this));
        this.toolBelt = toolBelt;
        AnnotationProcessor.process(this);
        setObservation(null);
        //addPainter(crossHairPainter);
        addPainter(notSelectedObservationsPainter);
        addPainter(selectedObservationsPainter);
        addPainter(selectedAreaMeasurementPainter);
        //addPainter(horizontalLinePainter);

    }

    /**
     */
    @Override
    public void clearPainters() {
        super.clearPainters();
        //addPainter(crossHairPainter);
    }

    /**
     * @return
     */
    public BufferedImage getImage() {
        return image;
    }

    private AreaMeasurement newAreaMeasurement(String comment) {
        return new AreaMeasurement(new ArrayList<Point2D<Integer>>(pointsIC), comment);
    }

    @Override
    protected void paintLayer(Graphics2D g2, JXLayer<? extends T> jxl) {
        super.paintLayer(g2, jxl);
        g2.setPaintMode();    // Make sure xor is turned off
        if (observation != null) {

            polygonCC.reset();
            if (pointsIC.size() > 0) {

                // --- Paint current area measurement
                Color color = (pointsIC.size() < 3) ? Color.RED : Color.CYAN;
                g2.setStroke(new BasicStroke(2));
                g2.setPaint(color);

                // TODO polygonCC should start and end with the currentPointCC marker.
                for (int i = 0; i < pointsIC.size(); i++) {
                    org.mbari.geometry.Point2D<Integer> coordinate = pointsIC.get(i);
                    java.awt.geom.Point2D imagePoint = new java.awt.geom.Point2D.Double(coordinate.getX(),
                        coordinate.getY());
                    java.awt.geom.Point2D componentPoint = jxl.getView().convertToComponent(imagePoint);
                    if (i == 0) {
                        polygonCC.moveTo(componentPoint.getX(), componentPoint.getY());
                    }
                    else {
                        polygonCC.lineTo(componentPoint.getX(), componentPoint.getY());
                        polygonCC.moveTo(componentPoint.getX(), componentPoint.getY());
                    }
                }

                // Close path

                org.mbari.geometry.Point2D<Integer> coordinate = pointsIC.get(0);
                java.awt.geom.Point2D imagePoint = new java.awt.geom.Point2D.Double(coordinate.getX(),
                    coordinate.getY());
                java.awt.geom.Point2D componentPoint = jxl.getView().convertToComponent(imagePoint);
                polygonCC.lineTo(componentPoint.getX(), componentPoint.getY());
                g2.draw(polygonCC);

                // --- Draw current mouse point as a hint to where next point will be placed
                final int markerOffset = markerDiameter / 2;
                if (pointsIC.size() > 1) {
                    g2.setPaint(Color.RED);
                    g2.draw(bridgeCC);
                    Point p = AwtUtilities.toPoint(currentPointCC);
                    Ellipse2D marker = new Ellipse2D.Double(p.x - markerOffset, p.y - markerOffset, markerDiameter,
                        markerDiameter);
                    g2.draw(marker);
                }
            }

        }
    }

    @Override
    protected void processMouseEvent(MouseEvent me, JXLayer<? extends T> jxl) {
        super.processMouseEvent(me, jxl);
        Point point = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), jxl);
        switch (me.getID()) {
        case MouseEvent.MOUSE_PRESSED:
            java.awt.geom.Point2D imagePoint = jxl.getView().convertToImage(point);
            int x = (int) Math.round(imagePoint.getX());
            int y = (int) Math.round(imagePoint.getY());
            if ((me.getClickCount() == 1) && (me.getButton() == MouseEvent.BUTTON1)) {
                if (image != null) {
                    if (x < 0) {
                        x = 0;
                    }
                    if (x > image.getWidth()) {
                        x = image.getWidth();
                    }
                    if (y < 0) {
                        y = 0;
                    }
                    if (y > image.getHeight()) {
                        y = image.getHeight();
                    }

                    pointsIC.add(new org.mbari.geometry.Point2D<Integer>(x, y));
                    setDirty(true);
                }
            }
            else if ((me.getClickCount() == 2) || (me.getButton() != MouseEvent.BUTTON1)) {
                if ((pointsIC.size() > 2) && (observation != null)) {

                    // --- Publish action via EventBus
                    AreaMeasurement areaMeasurement = newAreaMeasurement(null);
                    AddAreaMeasurementEvent event = new AddAreaMeasurementEvent(observation, areaMeasurement);
                    EventBus.publish(event);
                }
                resetUI();
                setObservation(observation);
            }
        default:

        // Do nothing
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent me, JXLayer<? extends T> jxl) {
        super.processMouseMotionEvent(me, jxl);
        if ((me.getID() == MouseEvent.MOUSE_MOVED) && (pointsIC.size() > 1)) {
            Point point = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), jxl);

            currentPointCC.setLocation(point.getX(), point.getY());
            int w = jxl.getWidth();
            int h = jxl.getHeight();

            bridgeCC.reset();
            if ((point.y <= h) && (point.x <= w) && (point.y >= 0) && (point.x >= 0)) {
                java.awt.geom.Point2D start = jxl.getView().convertToComponent(pointsIC.get(0).toJavaPoint2D());
                java.awt.geom.Point2D end = jxl.getView().convertToComponent(pointsIC.get(pointsIC.size() -
                    1).toJavaPoint2D());
                bridgeCC.moveTo(start.getX(), start.getY());
                bridgeCC.lineTo(currentPointCC.getX(), currentPointCC.getY());
                bridgeCC.lineTo(end.getX(), end.getY());
            }

            // mark the ui as dirty and needed to be repainted
            setDirty(true);
        }
    }

    /**
     * resets the ui to a known state
     */
    public void resetUI() {
        if (SwingUtilities.isEventDispatchThread()) {
            resetRunable.run();
        }
        else {
            SwingUtilities.invokeLater(resetRunable);
        }
    }

    /**
     *
     * @param event
     */
    @EventSubscriber(eventClass = IAFRepaintEvent.class)
    public void respondTo(IAFRepaintEvent event) {
        UIDataCoordinator dataCoordinator = event.get();
        Collection<Observation> selectedObservations = dataCoordinator.getSelectedObservations();
        Observation selectedObservation = (selectedObservations.size() == 1)
                                          ? selectedObservations.iterator().next() : null;
        setObservation(selectedObservation);

    }

    /**
     *
     * @param image
     */
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    /**
     *
     * @param observation
     */
    public void setObservation(Observation observation) {
        Observation oldObservation = this.observation;
        this.observation = observation;
        resetUI();
    }

    public JXHorizontalLinePainter<T> getHorizontalLinePainter() {
        return horizontalLinePainter;
    }
}
