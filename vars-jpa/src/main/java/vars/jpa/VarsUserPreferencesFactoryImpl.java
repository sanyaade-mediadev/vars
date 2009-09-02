/*
 * @(#)VarsUserPreferencesFactory.java   2009.08.25 at 01:48:05 PDT
 *
 * Copyright 2009 MBARI
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package vars.jpa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.prefs.Preferences;
import org.mbari.jpax.EAO;

/**
 *
 * @author brian
 */
public class VarsUserPreferencesFactoryImpl implements VarsUserPreferencesFactory {

    /**
     *  Description of the Field
     */
    public final static String DEFAULT_USER = "default";
    private final EAO eao;

    /**
     * Constructs ...
     *
     * @param eao
     */
    @Inject
    public VarsUserPreferencesFactoryImpl(@Named("miscEAO") EAO eao) {
        this.eao = eao;
    }

    /**
     * This method returns the system root of the preferences tree.  It contains
     * all the preferences for the users
     *
     * @return  Preferences that contain all the users preferences
     */
    public Preferences systemRoot() {
        return new VarsUserPreferences(eao);
    }

    /**
     * This method returns the preferences object for the user specified by
     * System.getProperty("user.name")
     *
     * @return  Preferences 
     */
    public Preferences userRoot() {
        return userRoot(System.getProperty("user.name"));
    }

    /**
     * This method returns the preferences object for the user specified in the parameter
     *
     * @param  userName is a String that is the username for the preferences object being requested
     * @return  Preferences that is the preferences node for the user specified.
     */
    public Preferences userRoot(String userName) {
        return systemRoot().node(userName);
    }
    
}