/*
 * Copyright 2005 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package vars.knowledgebase.ui;

import vars.shared.ui.ILockableEditor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mbari.swing.ListListModel;
import vars.UserAccount;
import vars.knowledgebase.Concept;
import vars.knowledgebase.History;
import vars.knowledgebase.ui.actions.ApproveHistoryTask;
import vars.knowledgebase.ui.actions.RejectHistoryTask;
import vars.shared.ui.OkCancelButtonPanel;


//~--- classes ----------------------------------------------------------------

/**
 * <p><!-- Class description --></p>
 *
 * @version    $Id: HistoryEditorPanel.java 295 2006-07-06 23:47:31Z hohonuuli $
 * @author     <a href="http://www.mbari.org">Monterey Bay Aquarium Research Institute</a>
 */
public class HistoryEditorPanel extends EditorPanel implements ILockableEditor {


    private static final Logger log = LoggerFactory.getLogger(HistoryEditorPanel.class);

    private JPanel listPanel = null;

    private JList historyList = null;

    private UserAccount userAccount;


    private Set histories = new TreeSet(new HistoryComparator());

    private OkCancelButtonPanel buttonPanel = null;

    private HistoryViewPanel viewPanel = null;
    
    private JScrollPane scrollPane = null;

    private final ApproveHistoryTask approveHistoryTask;
    private final RejectHistoryTask rejectHistoryTask;

    //~--- constructors -------------------------------------------------------

    /**
     * This is the default constructor
     */
    public HistoryEditorPanel(ToolBelt toolBelt) {
        super();
        initialize();
        setLocked(isLocked());
        approveHistoryTask = toolBelt.getApproveHistoryTask();
        rejectHistoryTask = toolBelt.getRejectHistoryTask();
    }

    //~--- get methods --------------------------------------------------------

    /**
     * This method initializes acceptButton
     *
     * @return javax.swing.JButton
     */
    private JButton getAcceptButton() {
        return ((OkCancelButtonPanel) getButtonPanel()).getOkButton();
    }



    /**
	 * This method initializes buttonPanel
	 * @return  javax.swing.JPanel
	 * @uml.property  name="buttonPanel"
	 */
    private OkCancelButtonPanel getButtonPanel() {
        if (buttonPanel == null) {
            final OkCancelButtonPanel p = new OkCancelButtonPanel();
            
            /*
             * Set up the OK button
             */
            final JButton okButton = p.getOkButton();
            okButton.setText("Accept");
            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final UserAccount userAccount = getUserAccount();
                    final History history = (History) getHistoryList().getSelectedValue();
                    String name = history.getConceptMetadata().getConcept().getPrimaryConceptName().getName();
                    approveHistoryTask.approve(userAccount, history);
                    final KnowledgebaseApp app = (KnowledgebaseApp) Lookup.getApplicationDispatcher().getValueObject();

                    /*
                     * At this point the concept may be modified or removed. We'll check the name again; if the
                     * check fails we'll end up using the name fetched earlier
                     */
                    try {
                        String newName = history.getConceptMetadata().getConcept().getPrimaryConceptName().getName();
                        name = newName;
                    }
                    catch (NullPointerException e1) {
                        // Do nothing
                    }

                    // TODO the refresh should be done by the individual tasks. A full refresh may not always be needed.
                    app.getKnowledgebaseFrame().refreshTreeAndOpenNode(name);
                }
                
            });
            
            /*
             * Set up the reject button
             */
            final JButton cancelButton = p.getCancelButton();
            cancelButton.setText("Reject");
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final UserAccount userAccount = getUserAccount();
                    final History history = (History) getHistoryList().getSelectedValue();
                    String name = history.getConceptMetadata().getConcept().getPrimaryConceptName().getName();
                    rejectHistoryTask.reject(userAccount, history);
                    final KnowledgebaseApp app = (KnowledgebaseApp) Lookup.getApplicationDispatcher().getValueObject();

/*
                     * At this point the concept may be modified or removed. We'll check the name again; if the
                     * check fails we'll end up using the name fetched earlier
                     */
                    try {
                        String newName = history.getConceptMetadata().getConcept().getPrimaryConceptName().getName();
                        name = newName;
                    }
                    catch (NullPointerException e1) {
                        // Do nothing
                    }
                    

                    // TODO the refresh should be done by the individual tasks. A full refresh may not always be needed.
                    app.getKnowledgebaseFrame().refreshTreeAndOpenNode(name);
                }
                
            });
            
            buttonPanel = p;
        }

        return buttonPanel;
    }


    /**
	 * This method initializes historyList
	 * @return  javax.swing.JList
	 * @uml.property  name="historyList"
	 */
    private JList getHistoryList() {
        if (historyList == null) {
            historyList = new JList();
            historyList.setModel(new ListListModel(new ArrayList()));
            historyList.setSelectionMode(
                    javax.swing.ListSelectionModel.SINGLE_SELECTION);
            historyList.setCellRenderer(new HistoryListCellRenderer());
            historyList.addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    final History history = (History) historyList.getSelectedValue();
                    getViewPanel().setHistory(history);
                        
                    /*
                     * Toggle the buttons off if no action needs to be taken
                     */
                    
                    if (!isLocked() && getUserAccount().isAdministrator()) {
                        final OkCancelButtonPanel panel = getButtonPanel();
                        panel.getOkButton().setEnabled(history != null && !history.isApproved());
                        panel.getCancelButton().setEnabled(history != null && !history.isApproved());
                    }

                }

            });
        }

        return historyList;
    }

    /**
	 * This method initializes listPanel
	 * @return  javax.swing.JPanel
	 * @uml.property  name="listPanel"
	 */
    private JPanel getListPanel() {
        if (listPanel == null) {
            listPanel = new JPanel();
            listPanel.setLayout(new BorderLayout());
            listPanel.setBorder(
                    javax.swing.BorderFactory.createTitledBorder(null,
                    "Entries",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                            javax.swing.border.TitledBorder.DEFAULT_POSITION,
                                null, null));
            listPanel.add(getScrollPane(), java.awt.BorderLayout.CENTER);
            listPanel.add(getViewPanel(), java.awt.BorderLayout.SOUTH);
        }

        return listPanel;
    }


    /**
     * This method initializes rejectButton
     *
     * @return javax.swing.JButton
     */
    private JButton getRejectButton() {
        return ((OkCancelButtonPanel) getButtonPanel()).getCancelButton();
    }
    
    /**
	 * @return  the scrollPane
	 * @uml.property  name="scrollPane"
	 */
    private JScrollPane getScrollPane() {
        if (scrollPane == null) {
            scrollPane = new JScrollPane();
            scrollPane.setViewportView(getHistoryList());
        }
        return scrollPane;
    }


    /**
	 * This method initializes viewPanel
	 * @return  javax.swing.JPanel
	 * @uml.property  name="viewPanel"
	 */
    private HistoryViewPanel getViewPanel() {
        if (viewPanel == null) {
            viewPanel = new HistoryViewPanel();
            viewPanel.setBorder(
                    javax.swing.BorderFactory.createTitledBorder(null, "",
                            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                                    null, null));
        }

        return viewPanel;
    }

    //~--- methods ------------------------------------------------------------

    /**
     * This method initializes this
     *
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(300, 200);
        this.setPreferredSize(new java.awt.Dimension(450, 116));
        this.add(getListPanel(), java.awt.BorderLayout.CENTER);
        this.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
    }

    //~--- set methods --------------------------------------------------------

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param concept
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setConcept(final Concept concept) {
        super.setConcept(concept);
        histories.clear();
        Set<History> historySet = getConcept().getConceptMetadata().getHistories();
        histories.addAll(historySet);
        final ListListModel listModel = (ListListModel) getHistoryList().getModel();
        listModel.clear();
        listModel.addAll(histories);
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param locked
     */
    @Override
    public void setLocked(final boolean locked) {
        super.setLocked(locked);
        getButtonPanel().getOkButton().setEnabled(!locked);
        getButtonPanel().getCancelButton().setEnabled(!locked);
        setUserAccount(userAccount);
    }

    /**
	 * <p><!-- Method description --></p>
	 * @param  userAccount
	 * @uml.property  name="userAccount"
	 */
    public void setUserAccount(final UserAccount userAccount) {
        this.userAccount = userAccount;
        boolean enable = false;
        if ((userAccount != null) && userAccount.isAdministrator() &&!isLocked()) {
            enable = true;
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Using user account '" + userAccount + "'. Enable = " +
                    enable);
        }

        getAcceptButton().setEnabled(enable);
        getRejectButton().setEnabled(enable);
    }
    
    /**
	 * @return  the userAccount
	 * @uml.property  name="userAccount"
	 */
    public UserAccount getUserAccount() {
        return userAccount;
    }

    //~--- inner classes ------------------------------------------------------

    private class HistoryComparator implements Comparator {
        
        HistoryComparator() {
            super();
        }

        /**
         * <p>COmpare by creation date</p>
         *
         *
         * @param arg0
         * @param arg1
         *
         * @return
         */
        public int compare(final Object arg0, final Object arg1) {
            final History h0 = (History) arg0;
            final History h1 = (History) arg1;
            final Date t0 = (h0.getCreationDate() == null) ? new Date(0) : h0.getCreationDate();
            final Date t1 = (h1.getCreationDate() == null) ? new Date(0) : h1.getCreationDate();
            
            // Compare by creation date
            int c =  t0.compareTo(t1);
            // If it's the same creation date, then compare by the default history compareTo
            if (c == 0) {
                c = h0.toString().compareTo(h1.toString());
            }
            return c;
        }
    }


    private static final class HistoryListCellRenderer
            extends DefaultListCellRenderer {

        // Selection colors for JList cell renderer
        private static final Color BACKGROUND = (Color) UIManager.get("List.background");
        private static final Color FOREGROUND = (Color) UIManager.get("List.foreground");
        private static final Color SELECTION_BACKGROUND = (Color) UIManager.get("List.selectionBackground");
        private static final Color SELECTION_FOREGROUND = (Color) UIManager.get("List.selectionForeground");
        private static final Color PENDING_COLOR = Color.RED;
        private static final Color APPROVED_COLOR = Color.BLACK;
        private static final Color REJECTED_COLOR = Color.GRAY;

        // CONSTRUCTOR

        /**
         * Constructs ...
         *
         */
        HistoryListCellRenderer() {
            super();
            setOpaque(true);
        }

        // Override DefaultListCellRenderer impl

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param list
         * @param value
         * @param index
         * @param isSelected
         * @param cellHasFocus
         *
         * @return
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
            // Switch back/foreground colors.
            setBackground(isSelected ? SELECTION_BACKGROUND : BACKGROUND);
            setForeground(isSelected ? SELECTION_FOREGROUND : FOREGROUND);
            final History history = (History) value;
            this.setText(history.stringValue());

            // Color sensitive to History approval
            if (history.isRejected()) {
                setForeground(REJECTED_COLOR);
            }
            else if (history.isApproved()) {
                setForeground(APPROVED_COLOR);
            }
            else {
                setForeground(PENDING_COLOR);
            }

            return this;
        }

        // Tooltip to explain color coding

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @return
         */
        @Override
        public String getToolTipText() {
            return "Black -- approved; Gray -- rejected; Red -- pending approval";
        }
    }
}
