package vars.annotation.ui.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import org.bushe.swing.event.EventBus;

import vars.MiscDAOFactory;
import vars.UserAccount;
import vars.UserAccountDAO;
import vars.annotation.ui.Lookup;
import vars.shared.preferences.PreferenceUpdater;
import vars.shared.ui.UserAccountPreferencesPanel;

public class UserPreferencesPanelController implements PreferenceUpdater {
    
    private final UserPreferencesPanel panel;
    private final MiscDAOFactory daoFactory;
    
    public UserPreferencesPanelController(UserPreferencesPanel panel, MiscDAOFactory daoFactory) {
        this.panel = panel;
        this.daoFactory = daoFactory;
        
        // Listen for changes to UserAccount
        Lookup.getUserAccountDispatcher().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                UserAccount userAccount = (UserAccount) evt.getNewValue();
                setUserAccount(userAccount);
            }
        });
        setUserAccount((UserAccount) Lookup.getUserAccountDispatcher().getValueObject());
    }

    public void persistPreferences() {
        UserAccountPreferencesPanel p = panel.getPanel();
        UserAccount userAccount = p.getUserAccount();
        boolean changePassword = false;
        boolean passwordsMatch = false;
        if (userAccount != null) {
            UserAccountDAO dao = daoFactory.newUserAccountDAO();
            dao.startTransaction();
            userAccount = dao.find(userAccount);
            if (userAccount != null) {
                userAccount.setAffiliation(p.getAffiliationTextField().getText());
                userAccount.setEmail(p.getEmailTextField().getText());
                userAccount.setFirstName(p.getFirstNameTextField().getText());
                userAccount.setLastName(p.getLastNameTextField().getText());
                //userAccount.setRole((String) p.getRoleComboBox().getSelectedItem());
                
                // Check password
                char[] pwd1 = p.getPasswordField1().getPassword();
                char[] pwd2 = p.getPasswordField2().getPassword();
                changePassword = (pwd1 != null) && (pwd2 != null) && (pwd1.length > 0) && (pwd2.length > 0);
                passwordsMatch = Arrays.equals(pwd1, pwd2);
        
                if (changePassword && passwordsMatch) {
                    userAccount.setPassword(new String(pwd1));
                }
                
            }
            dao.endTransaction();
            dao.close();
            
            if (changePassword && !passwordsMatch) {
                EventBus.publish(Lookup.TOPIC_WARNING, "The two passwords provided did not match each other. " +
                		"Your password was NOT changed");
            }
        }
    }
    
    private void setUserAccount(UserAccount userAccount) {
        UserAccountPreferencesPanel p = panel.getPanel();
        p.setUserAccount(userAccount);
    }
    
}
