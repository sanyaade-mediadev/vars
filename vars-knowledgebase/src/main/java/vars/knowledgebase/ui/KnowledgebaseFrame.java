/*
 * @(#)KnowledgebaseFrame.java   2009.10.27 at 09:33:07 PDT
 *
 * Copyright 2009 MBARI
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package vars.knowledgebase.ui;

import com.google.inject.Inject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.bushe.swing.event.EventBus;
import org.mbari.awt.event.ActionAdapter;
import org.mbari.swing.SearchableTreePanel;
import org.mbari.util.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vars.CacheClearedEvent;
import vars.CacheClearedListener;
import vars.UserAccount;
import vars.knowledgebase.Concept;
import vars.knowledgebase.ConceptDAO;
import vars.knowledgebase.ConceptNameTypes;
import vars.knowledgebase.SimpleConceptBean;
import vars.knowledgebase.SimpleConceptNameBean;
import vars.shared.ui.ILockableEditor;
import vars.shared.ui.dialogs.LoginAction;
import vars.shared.ui.dialogs.ModifyUserDialog;
import vars.shared.ui.kbtree.ConceptTree;
import vars.shared.ui.kbtree.SearchableConceptTreePanel;
import vars.shared.ui.kbtree.TreeConcept;

/**
 * <p><!-- Class description --></p>
 *
 * @version    $Id: KnowledgebaseFrame.java 295 2006-07-06 23:47:31Z hohonuuli $
 * @author     <a href="http://www.mbari.org">Monterey Bay Aquarium Research Institute</a>
 */
public class KnowledgebaseFrame extends JFrame {

    private static final long serialVersionUID = -6933314608809904289L;
    private static final Logger log = LoggerFactory.getLogger(KnowledgebaseFrame.class);
    private ConceptPanel conceptPanel = null;
    private HistoryEditorPanel historyEditorPanel = null;
    private LinkRealizationEditorPanel linkRealizationEditorPanel = null;
    private LinkTemplateEditorPanel linkTemplateEditorPanel = null;
    private JMenuBar myMenuBar = null;
    private NamesEditorPanel namesEditorPanel = null;
    private JPanel rightPanel = null;
    private JSplitPane splitPane = null;
    private JTabbedPane tabbedPane = null;
    private SearchableTreePanel treePanel = null;
    private final LockAction lockAction = new LockAction();
    private final KnowledgebaseFrameController controller;
    private LoginAction loginAction;
    private MediaEditorPanel mediaEditorPanel;
    private final ToolBelt toolBelt;
    private TreeSelectionListener treeSelectionListener;

    /**
     * Constructs ...
     *
     *
     * @param toolBelt
     */
    @Inject
    public KnowledgebaseFrame(ToolBelt toolBelt) {
        this.toolBelt = toolBelt;
        controller = new KnowledgebaseFrameController(this, toolBelt);
        initialize();
        initUserAccountDispatcher();
        toolBelt.getPersistenceCache().addCacheClearedListener(new ACacheClearedListener());
    }


    private ConceptPanel getConceptPanel() {
        if (conceptPanel == null) {
            conceptPanel = new ConceptPanel();
            final Dispatcher dispatcher = Lookup.getSelectedConceptDispatcher();
            dispatcher.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    final Concept concept = (Concept) dispatcher.getValueObject();
                    conceptPanel.setConcept(concept);
                }

            });

            /*
             * Allow the user to login when they click on the lock button
             */
            final JButton lockButton = conceptPanel.getLockButton();
            lockButton.addActionListener(getLoginAction());

            /*
             * When an account is set the tooltip text shoudl tell us what user
             */
            final Dispatcher userAccountDispatcher = Lookup.getUserAccountDispatcher();
            userAccountDispatcher.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    final UserAccount userAccount = (UserAccount) evt.getNewValue();
                    conceptPanel.setUserAccount(userAccount);
                }

            });
        }

        return conceptPanel;
    }


    private HistoryEditorPanel getHistoryEditorPanel() {
        if (historyEditorPanel == null) {
            historyEditorPanel = new HistoryEditorPanel(toolBelt);
            setupEditorPanel(historyEditorPanel);

            /*
             * The history panel will remain locked if the user credentials
             * are not verified. We register with the UserAccount Dispatcher
             * to keep track of credentials
             */
            Dispatcher.getDispatcher(UserAccount.class).addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    historyEditorPanel.setUserAccount((UserAccount) evt.getNewValue());
                }

            });
        }

        return historyEditorPanel;
    }

    private LinkRealizationEditorPanel getLinkRealizationEditorPanel() {
        if (linkRealizationEditorPanel == null) {
            linkRealizationEditorPanel = new LinkRealizationEditorPanel(toolBelt);
            setupEditorPanel(linkRealizationEditorPanel);
        }

        return linkRealizationEditorPanel;
    }


    private LinkTemplateEditorPanel getLinkTemplateEditorPanel() {
        if (linkTemplateEditorPanel == null) {
            linkTemplateEditorPanel = new LinkTemplateEditorPanel(toolBelt);
            setupEditorPanel(linkTemplateEditorPanel);
        }

        return linkTemplateEditorPanel;
    }

    /**
     * The default LoginAction jsut logins in. For this application we want it
     * to toggle the state of the lock. i.e If the user is already logged in we
     * want it to log out.
     * @return  A subclass of LoginAction
     */
    private LoginAction getLoginAction() {
        if (loginAction == null) {
            loginAction = new LoginAction(toolBelt.getMiscFactory(), toolBelt.getMiscDAOFactory().newUserAccountDAO()) {

                @Override
                public void doAction() {
                    if (lockAction.isLocked()) {
                        super.doAction();
                    }
                    else {
                        Dispatcher.getDispatcher(UserAccount.class).setValueObject(null);
                    }
                }
            };
        }

        return loginAction;
    }


    private MediaEditorPanel getMediaEditorPanel() {
        if (mediaEditorPanel == null) {
            mediaEditorPanel = new MediaEditorPanel(toolBelt);
            setupEditorPanel(mediaEditorPanel);
        }

        return mediaEditorPanel;
    }


    private JMenuBar getMyMenuBar() {
        if (myMenuBar == null) {
            myMenuBar = new JMenuBar();

            final JMenu menuEdit = new JMenu("Edit");
            menuEdit.setMnemonic('E');
            myMenuBar.add(menuEdit);

            // create EditUser menu item
            final JMenuItem editUser = new JMenuItem(new EditUserAccountAction());
            menuEdit.add(editUser);
            editUser.setMnemonic('U');
            editUser.setText("Edit User Accounts");
            editUser.setEnabled(false);

            // create EditConcept menu item
            final JMenuItem editConcept = new JMenuItem(new ActionAdapter() {

                private static final long serialVersionUID = 1L;

                public void doAction() {
                    final EditableConceptTreePopupMenu popupMenu = (EditableConceptTreePopupMenu) ((ConceptTree) getTreePanel()
                        .getJTree()).getPopupMenu();
                    popupMenu.triggerEditAction();
                }

            });
            menuEdit.add(editConcept);
            editConcept.setMnemonic('C');
            editConcept.setText("Edit Selected Concept");
            editConcept.setEnabled(false);

            final Dispatcher dispatcher = Lookup.getUserAccountDispatcher();
            dispatcher.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    final UserAccount userAccount = (UserAccount) evt.getNewValue();
                    final boolean enable = (userAccount != null) && !userAccount.isReadOnly();
                    editUser.setEnabled(enable);
                    editConcept.setEnabled(enable);
                }

            });
        }

        return myMenuBar;
    }


    private NamesEditorPanel getNamesEditorPanel() {
        if (namesEditorPanel == null) {
            namesEditorPanel = new NamesEditorPanel(toolBelt);
            setupEditorPanel(namesEditorPanel);
        }

        return namesEditorPanel;
    }

    public JPanel getRightPanel() {
        if (rightPanel == null) {
            rightPanel = new JPanel();
            rightPanel.setLayout(new BorderLayout());
            rightPanel.add(getConceptPanel(), BorderLayout.NORTH);
            rightPanel.add(getTabbedPane(), BorderLayout.CENTER);
        }

        return rightPanel;

    }


    private JSplitPane getSplitPane() {
        if (splitPane == null) {
            splitPane = new JSplitPane();

            splitPane.setLeftComponent(getTreePanel());
            splitPane.setRightComponent(getRightPanel());
        }

        return splitPane;
    }


    private JTabbedPane getTabbedPane() {
        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane();

            tabbedPane.addTab("Names", null, getNamesEditorPanel(), null);
            tabbedPane.addTab("Templates", null, getLinkTemplateEditorPanel(), null);
            tabbedPane.addTab("Properties", null, getLinkRealizationEditorPanel(), null);
            tabbedPane.addTab("Media", null, getMediaEditorPanel(), null);
            tabbedPane.addTab("History", null, getHistoryEditorPanel(), null);

        }

        return tabbedPane;
    }


    protected SearchableTreePanel getTreePanel() {
        if (treePanel == null) {
            ConceptDAO conceptDAO = toolBelt.getKnowledgebaseDAOFactory().newConceptDAO();
            treePanel = new SearchableConceptTreePanel(conceptDAO,
                    toolBelt.getKnowledgebaseDAOFactory().newConceptNameDAO(), toolBelt.getPersistenceCache());

            /*
             * Fetch the root concept. We need this to intialize the ConceptTree
             */
            Concept concept = null;

            try {
                concept = conceptDAO.findRoot();
            }
            catch (Exception e) {
                concept = new SimpleConceptBean(new SimpleConceptNameBean("ERROR!!",
                        ConceptNameTypes.PRIMARY.toString()));
                log.error("Failed to load knowledgebase", e);
                EventBus.publish(Lookup.TOPIC_FATAL_ERROR, "Failed to load knowledgebase.");
            }

            /*
             * Build the conceptTree and add a listener. This listener should
             * pass the concept to a Dispatcher so other objects will get
             * notified when the concept changes.
             */
            final EditableConceptTree conceptTree = new EditableConceptTree(concept, toolBelt);
            conceptTree.addTreeSelectionListener(getTreeSelectionListener());
            lockAction.addEditor(conceptTree);
            treePanel.setJTree(conceptTree);
            JPopupMenu popupMenu = conceptTree.getPopupMenu();
            popupMenu.addSeparator();
            JMenuItem menuIteum = new JMenuItem("Refresh", 'r');
            menuIteum.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    refreshTreeAndOpenNode(conceptTree.getSelectedConcept().getPrimaryConceptName().toString());
                }

            });
            popupMenu.add(menuIteum);

            /*
             * Register the tree with a dispatcher so that other components can
             * attempt to fetch it.
             */
            Lookup.getConceptTreeDispatcher().setValueObject(conceptTree);
        }

        return treePanel;
    }


    private TreeSelectionListener getTreeSelectionListener() {
        if (treeSelectionListener == null) {
            treeSelectionListener = new ATreeSelectionListener();
        }

        return treeSelectionListener;
    }

    /**
     * Initializes the UserAccount Dispatcher. This watches for changes in
     * the user logged in and toggles the editors states appropriately. It uses
     * the LockAction to toggle the editors.
     */
    private void initUserAccountDispatcher() {
        Dispatcher dispatcher = Lookup.getUserAccountDispatcher();

        dispatcher.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                UserAccount userAccount = (UserAccount) evt.getNewValue();
                boolean locked = ((userAccount == null) || (userAccount.isReadOnly()));

                if (log.isDebugEnabled()) {
                    log.debug("Using UserAccount '" + userAccount + "'; setting locked to " + locked);
                }

                lockAction.setLocked(locked);
            }

        });
    }


    void initialize() {
        this.setSize(new Dimension(459, 367));
        this.setJMenuBar(getMyMenuBar());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(getSplitPane());

        ResourceBundle bundle = ResourceBundle.getBundle("vars-knowledgebase");
        final String title = bundle.getString("frame.title");
        this.setTitle(title);
    }

    public void refreshTreeAndOpenNode(String name) {
        controller.refreshTreeAndOpenNode(name);
    }

    private void setupEditorPanel(final EditorPanel editorPanel) {

        final Dispatcher conceptDispatcher = Lookup.getSelectedConceptDispatcher();

        /*
         * Add a listener so that this panel is updated whenever a new
         * concept is selected in the Knowledgebasetree.
         */
        conceptDispatcher.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                final Concept concept = (Concept) evt.getNewValue();
                editorPanel.setConcept(concept);
            }

        });

        /*
         * Register the panel with the lockAction. This will signal the
         * editor that the user is logged in.
         */
        lockAction.addEditor(editorPanel);


    }

    /**
     * Listens for when the knowledgebase cache is cleared. When it's cleared
     * this rebuilds the treepanel.
     * @author brian
     *
     */
    private class ACacheClearedListener implements CacheClearedListener {

        /**
         * @param evt
         */
        public void afterClear(CacheClearedEvent evt) {

            // Do nothing
        }

        /**
         * @param evt
         */
        public void beforeClear(CacheClearedEvent evt) {
            Lookup.getSelectedConceptDispatcher().setValueObject(null);
        }
    }


    /**
     * Listens to the ConceptTree. When a concept is selected int the tree this
     * listener sets the concept in a Dispatcher so all components can listen
     * for the currently selected concept.
     */
    private class ATreeSelectionListener implements TreeSelectionListener {

        /**
         * @param e
         */
        public void valueChanged(TreeSelectionEvent e) {
            TreePath selectionPath = e.getNewLeadSelectionPath();
            Concept concept = null;

            if (selectionPath != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof TreeConcept) {
                    TreeConcept treeConcept = (TreeConcept) userObject;
                    concept = treeConcept.getConcept();
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Selected " + concept + " in the knowledgebase tree");
            }

            Dispatcher dispatcher = Lookup.getSelectedConceptDispatcher();
            dispatcher.setValueObject(concept);
        }
    }


    /**
     * @author  brian
     */
    private class EditUserAccountAction extends ActionAdapter {

        private static final long serialVersionUID = 1L;
        private final ModifyUserDialog dialog = new ModifyUserDialog((Frame) Lookup.getApplicationFrameDispatcher().getValueObject(),
                toolBelt.getMiscDAOFactory().newUserAccountDAO());

        public void doAction() {

            dialog.setUserAccount((UserAccount) Lookup.getUserAccountDispatcher().getValueObject());
            dialog.setVisible(true);
        }
    }


    /**
         * This action deals with toggling the locks on the editors. TO unlock, the users credentials will need to be validated.
         * @author  brian
         */
    private class LockAction {

        private boolean locked = true;
        private final Set editors = new HashSet();

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param lockableEditor
         */
        void addEditor(ILockableEditor lockableEditor) {
            lockableEditor.setLocked(isLocked());
            editors.add(lockableEditor);
        }

        /**
                 * @return  Returns the locked.
                 * @uml.property  name="locked"
                 */
        boolean isLocked() {
            return locked;
        }

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param lockableEditor
         */
        void removeEditor(ILockableEditor lockableEditor) {
            editors.remove(lockableEditor);
        }

        /**
                 * Lock all the editors
                 * @param locked  The locked to set.
                 * @uml.property  name="locked"
                 */
        void setLocked(boolean locked) {
            if (this.locked != locked) {
                this.locked = locked;

                for (Iterator i = editors.iterator(); i.hasNext(); ) {
                    ILockableEditor editor = (ILockableEditor) i.next();

                    editor.setLocked(locked);
                }
            }
        }
    }

}    

