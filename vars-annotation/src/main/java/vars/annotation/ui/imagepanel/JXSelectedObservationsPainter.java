/*
 * @(#)JXSelectedObservationsPainter.java   2012.08.07 at 02:17:25 PDT
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

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.mbari.swing.JImageUrlCanvas;

/**
 * @author Brian Schlining
 * @since 2012-08-03
 *
 * @param <T>
 */
public class JXSelectedObservationsPainter<T extends JImageUrlCanvas> extends JXObservationsPainter<T> {

    /**
     * Constructs ...
     */
    public JXSelectedObservationsPainter() {
        super(MarkerStyle.SELECTED, true, false);
        AnnotationProcessor.process(this);
    }

    /**
     *
     * @param event
     */
    @EventSubscriber(eventClass = IAFRepaintEvent.class)
    public void respondTo(IAFRepaintEvent event) {
        setObservations(event.get().getSelectedObservations());
    }
}
