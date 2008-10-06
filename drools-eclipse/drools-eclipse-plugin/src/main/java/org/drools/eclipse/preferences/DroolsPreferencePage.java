package org.drools.eclipse.preferences;

import org.drools.eclipse.DroolsEclipsePlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DroolsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button buildAllCheckBox;
	private Button collapseEditorCheckBox;
	private Button cacheParsedRulesCheckBox;
	private Combo processSkinCombo;
	
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);
        
		buildAllCheckBox = createCheckBox(composite,
			"Automatically reparse all rules if a Java resource is changed.");
		collapseEditorCheckBox = createCheckBox(composite,
			"Use code folding in DRL editor.");
		cacheParsedRulesCheckBox = createCheckBox(composite,
			"When parsing rules, always cache the result for future use. Warning: when disabled, debugging of rules will not work.");
		Label label = new Label(composite, SWT.NONE);
		label.setText("Preferred process skin: ");
		processSkinCombo = new Combo(composite, SWT.LEFT);
		processSkinCombo.add("default");
		processSkinCombo.add("BPMN");

		initializeValues();

		return composite;
	}

	private Button createCheckBox(Composite group, String label) {
        Button button = new Button(group, SWT.CHECK | SWT.LEFT);
        button.setText(label);
        GridData data = new GridData();
        data.horizontalSpan = 2;
        button.setLayoutData(data);
        return button;
    }
	
	protected IPreferenceStore doGetPreferenceStore() {
        return DroolsEclipsePlugin.getDefault().getPreferenceStore();
    }
	
	private void initializeDefaults() {
        IPreferenceStore store = getPreferenceStore();
        buildAllCheckBox.setSelection(store.getDefaultBoolean(IDroolsConstants.BUILD_ALL));
        collapseEditorCheckBox.setSelection(store.getDefaultBoolean(IDroolsConstants.EDITOR_FOLDING));
        cacheParsedRulesCheckBox.setSelection(store.getDefaultBoolean(IDroolsConstants.CACHE_PARSED_RULES));
        String skin = store.getDefaultString(IDroolsConstants.SKIN);
        int index = 0;
        if ("BPMN".equals(skin)) {
        	index = 1;
        }
        processSkinCombo.select(index);
    }

	private void initializeValues() {
        IPreferenceStore store = getPreferenceStore();
        buildAllCheckBox.setSelection(store.getBoolean(IDroolsConstants.BUILD_ALL));
        collapseEditorCheckBox.setSelection(store.getBoolean(IDroolsConstants.EDITOR_FOLDING));
        cacheParsedRulesCheckBox.setSelection(store.getBoolean(IDroolsConstants.CACHE_PARSED_RULES));
        String skin = store.getString(IDroolsConstants.SKIN);
        int index = 0;
        if ("BPMN".equals(skin)) {
        	index = 1;
        }
        processSkinCombo.select(index);
    }

	protected void performDefaults() {
        super.performDefaults();
        initializeDefaults();
    }

	public boolean performOk() {
        storeValues();
        DroolsEclipsePlugin.getDefault().savePluginPreferences();
        return true;
    }
	
	private void storeValues() {
        IPreferenceStore store = getPreferenceStore();
        store.setValue(IDroolsConstants.BUILD_ALL, buildAllCheckBox.getSelection());
        store.setValue(IDroolsConstants.EDITOR_FOLDING, collapseEditorCheckBox.getSelection());
        store.setValue(IDroolsConstants.CACHE_PARSED_RULES, cacheParsedRulesCheckBox.getSelection());
        store.setValue(IDroolsConstants.SKIN,
    		(String) processSkinCombo.getItem(processSkinCombo.getSelectionIndex()));
    }

	public void init(IWorkbench workbench) {
		// do nothing
	}
}
