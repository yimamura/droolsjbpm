package org.drools.brms.client.packages;

import org.drools.brms.client.common.FormStyleLayout;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.ImageButton;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.common.StatusChangePopup;
import org.drools.brms.client.common.ValidationMessageWidget;
import org.drools.brms.client.common.YesNoDialog;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.ValidatedResponse;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the package editor and viewer for package configuration.
 * 
 * @author Michael Neale
 */
public class PackageEditor extends FormStyleLayout {
    
   
    private Command dirtyCommand;
    private Command cleanCommand;
    
    private PackageConfigData conf;
    private HTML status;
    protected ValidatedResponse previousResponse;
    private Command refreshCommand;

    public PackageEditor(PackageConfigData data, Command dCommand, Command cCommand, Command command) {
        this.conf = data;
        this.dirtyCommand = dCommand;
        this.cleanCommand = cCommand;
        this.refreshCommand = command;
        
        setStyleName( "package-Editor" );
        setWidth( "100%" );
        refreshWidgets();
    }

    private void refreshWidgets() {
        clear();
        //addHeader( "images/package_large.png", this.conf.name );
        

        addRow( warnings() );
        
        addAttribute( "Description:", description() );
        addAttribute( "Header:", header() );
        //addAttribute( "External repository sync URI:", externalURI() );
        addRow(new HTML("<hr/>"));
        addAttribute( "Last modified:", new Label(this.conf.lastModified.toLocaleString())  );
        addAttribute( "Last contributor:", new Label(this.conf.lasContributor));

        addRow(new HTML("<hr/>"));
        
        status = new HTML();
        HorizontalPanel statusBar = new HorizontalPanel();
        Image editState = new ImageButton("images/edit.gif");
        editState.setTitle( "Change status." );
        editState.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                showStatusChanger(w);
            }

        } );
        statusBar.add( status );
        
        if (!this.conf.isSnapshot) {
            statusBar.add( editState );
        }
        
        setState(conf.state);
        addAttribute("Status:", statusBar);

        if (!conf.isSnapshot) {
            addRow( saveWidgets() );
        }
        addRow(new HTML("<hr/>"));

        
    }
    



    

    


    private Widget warnings() {
        if (this.previousResponse != null && this.previousResponse.hasErrors) {
            Image img = new Image("images/warning.gif");
            HorizontalPanel h = new HorizontalPanel();
            h.add( img );
            HTML msg = new HTML("<b>There were errors validating this package configuration.");
            h.add( msg );
            Button show = new Button("View errors");
            show.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    ValidationMessageWidget wid = new ValidationMessageWidget(previousResponse.errorHeader, previousResponse.errorMessage);
                    wid.setPopupPosition( Window.getClientWidth()/4, w.getAbsoluteTop()  );
                    wid.show();
                }
            } );
            h.add( show );
            return h;
        } else {
            return new SimplePanel();
        }
    }

    protected void showStatusChanger(Widget w) {
        final StatusChangePopup pop = new StatusChangePopup(conf.uuid, true);
        pop.setChangeStatusEvent(new Command() {
            public void execute() {
                setState( pop.getState() );
            }                    
        });
        pop.setPopupPosition( w.getAbsoluteLeft(), w.getAbsoluteTop() );
        pop.show();
        
    }

    private void setState(String state) {
        status.setHTML( "<b>" + state + "</b>" );
    }

    /**
     * This will get the save widgets.
     */
    private Widget saveWidgets() {
        
        HorizontalPanel horiz = new HorizontalPanel();
        

        
            Button save = new Button("Save configuration changes");
            
            save.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    doSaveAction();                
                }
            } );
            
            horiz.add( save );
            
            
            Button archive = new Button("Archive");
            archive.addClickListener(new ClickListener() {
                public void onClick(Widget w) {
                    if ( Window.confirm( "Are you sure you want to archive (remove) this package?" ) ) {
                        conf.archived = true;
                        doSaveAction();
                        refreshCommand.execute();
                    }
                }
            });
            horiz.add(archive);
        return horiz;
    }



    private void doSaveAction() {
        LoadingPopup.showMessage( "Saving package configuration. Please wait ..." );
        RepositoryServiceFactory.getService().savePackage( this.conf, new GenericCallback() {
            public void onSuccess(Object data) {
                
                cleanCommand.execute();
                
                previousResponse = (ValidatedResponse) data;
                
                reload();
                SuggestionCompletionCache.getInstance().removePackage( conf.name );
                LoadingPopup.showMessage( "Package configuration updated successfully" );
                Timer t = new Timer() {
                    public void run() {
                        LoadingPopup.close();
                    }
                    
                };
                t.schedule( 2000 );
                
            }            
        });
        
    }



    /**
     * Will refresh all the data.
     */
    private void reload() {
        LoadingPopup.showMessage( "Refreshing package data..." );
        RepositoryServiceFactory.getService().loadPackageConfig( this.conf.uuid, new GenericCallback() {
            public void onSuccess(Object data) {
                LoadingPopup.close();
                conf = (PackageConfigData) data;
                refreshWidgets();
            }
        });
    }

    private Widget externalURI() {
        final TextBox box = new TextBox();
        box.setWidth( "100%" );
        box.setText( this.conf.externalURI );
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                conf.externalURI = box.getText();
            }            
        });
        return box;
    }

    private Widget header() {
        
        final TextArea area = new TextArea();
        area.setWidth( "100%" );
        area.setVisibleLines( 8 );
        
        area.setCharacterWidth( 100 );
        
        area.setText( this.conf.header );
        area.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                 conf.header = area.getText();
                 dirtyCommand.execute();
            }            
        });
        
        
        
        HorizontalPanel panel = new HorizontalPanel();
        panel.add( area );

        VerticalPanel vert = new VerticalPanel();

        Image max = new Image("images/max_min.gif");
        max.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                if (area.getVisibleLines() != 32) {
                    area.setVisibleLines( 32 );
                } else {
                    area.setVisibleLines( 8 );
                }
            }
        } );
        max.setTitle( "Increase view area." );
        vert.add( max );

        Image newImport = new Image("images/new_import.gif");
        newImport.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                area.setText( area.getText(  ) + "\n" + 
                              "import <your class here>");
                conf.header = area.getText();
            }
        });
        vert.add( newImport );
        newImport.setTitle( "Add a new Type/Class import to the package." );
        
        Image newGlobal = new Image("images/new_global.gif");
        newGlobal.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                area.setText( area.getText() + "\n" + 
                              "global <your class here> <variable name>");
                conf.header = area.getText();
            }
        });
        newGlobal.setTitle( "Add a new global variable declaration." );
        vert.add( newGlobal );
        
        Image newFactTemplate = new Image("images/fact_template.gif");
        newFactTemplate.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                final FactTemplateWizard wiz = new FactTemplateWizard();
                wiz.setPopupPosition( w.getAbsoluteLeft() - 400, w.getAbsoluteTop() - 250 );
                wiz.setOKClick( new Command() {
                    public void execute() {
                        area.setText( area.getText() + "\n" +
                                      wiz.getTemplateText() );
                        conf.header = area.getText();
                        
                    }
                } );
                wiz.show();
            }
        });
        newFactTemplate.setTitle( "Add a new fact template." );
        vert.add( newFactTemplate );
        
        panel.setWidth( "100%" );
        
        panel.add( vert );
        return panel;
    }


    private HorizontalPanel expandableTextArea(final TextArea area) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add( area );
        
        Image max = new Image("images/max_min.gif");
        max.setTitle( "Increase view area" );
        
        panel.add( max );
        max.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                if (area.getVisibleLines() != 32) {
                    area.setVisibleLines( 32 );                    
                } else {
                    area.setVisibleLines( 8 );
                }
            }
        } );
        return panel;
    }

    private Widget description() {
        final TextArea area = new TextArea();
        area.setWidth( "100%" );
        area.setVisibleLines( 8 );
        area.setText( conf.description );
        
        area.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                conf.description = area.getText();    
                dirtyCommand.execute();
            }            
        });
        
        area.setCharacterWidth( 100 );
        
        return expandableTextArea( area );
    }
    
}
