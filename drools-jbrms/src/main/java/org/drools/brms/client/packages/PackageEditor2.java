package org.drools.brms.client.packages;
/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.util.Date;

import org.drools.brms.client.common.FormStylePopup;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.ImageButton;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.common.PrettyFormLayout;
import org.drools.brms.client.common.StatusChangePopup;
import org.drools.brms.client.common.ValidationMessageWidget;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.ValidatedResponse;
import org.drools.brms.client.rulelist.EditItemEvent;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the package editor and viewer for package configuration.
 *
 * @author Michael Neale
 */
public class PackageEditor2 extends PrettyFormLayout {



    private PackageConfigData conf;
    private HTML status;
    protected ValidatedResponse previousResponse;
	private Command close;
	private Command refreshPackageList;
	private EditItemEvent editEvent;

    public PackageEditor2(PackageConfigData data, Command close, Command refreshPackageList, EditItemEvent editEvent) {
        this.conf = data;
        this.close = close;
        this.refreshPackageList = refreshPackageList;
        this.editEvent = editEvent;

        //setStyleName( "package-Editor" );
        setWidth( "100%" );
        refreshWidgets();
    }

    private void refreshWidgets() {
        clear();

        startSection("Package name: [" + conf.name + "]");


        FlexTable headerWidgets = new FlexTable();
        headerWidgets.setWidget(0, 0, new HTML("<b>Package name:</b>"));
        headerWidgets.setWidget(0, 1, new Label(this.conf.name));
        if (!conf.isSnapshot) {

        	//headerWidgets.setWidget(1, 1, modifyWidgets() );
        	addAttribute("Modify:", modifyWidgets());
        }

        endSection();

        //addHeader( "images/package_large.png", headerWidgets );

        startSection("Configuration");

        addRow( warnings() );
        addAttribute( "Configuration:", header() );
        addAttribute( "Description:", description() );
        if (!conf.isSnapshot) {
            Button save = new Button("Save and validate configuration");
            save.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    doSaveAction(null);
                }
            } );
        	addAttribute("", save);
        }

        endSection();

        if (!conf.isSnapshot) {
	        startSection("Build and validate");
	        addRow(new PackageBuilderWidget(this.conf, editEvent));
	        endSection();
        }

        startSection("Information");

        addAttribute( "Last modified:", new Label(getDateString(conf.lastModified))  );
        addAttribute( "Last contributor:", new Label(this.conf.lasContributor));
        addAttribute( "Date created:", new Label(getDateString(this.conf.dateCreated)));
		Button buildSource = new Button("Show package source");
		buildSource.addClickListener(new ClickListener() {
			public void onClick(Widget w) {
				PackageBuilderWidget.doBuildSource(conf.uuid, conf.name);
			}
		});
		addAttribute("View source for package:", buildSource);



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



        endSection();

    }



	private String getDateString(Date d) {
		if (d != null)
			return d.toLocaleString();
		else
			return "";
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
    private Widget modifyWidgets() {

        HorizontalPanel horiz = new HorizontalPanel();

        Button copy = new Button("Copy");
        copy.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                showCopyDialog();
            }
        } );
        horiz.add( copy );

        Button rename = new Button("Rename");
        rename.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                showRenameDialog();
            }
        } );
        horiz.add( rename );


        Button archive = new Button("Archive");
        archive.addClickListener(new ClickListener() {
            public void onClick(Widget w) {
                if ( Window.confirm( "Are you sure you want to archive (remove) this package?" ) ) {
                    conf.archived = true;
                    doSaveAction(close);
                    close.execute();
                    refreshPackageList.execute();
                }
            }
        });
        horiz.add(archive);

        return horiz;
    }



    private void showRenameDialog() {
        final FormStylePopup pop = new FormStylePopup("images/new_wiz.gif", "Rename the package");
        pop.addRow( new HTML("<i>Rename the package. A new unique name is required.</i>") );
        final TextBox name = new TextBox();
        pop.addAttribute( "New package name:", name );
        Button ok = new Button("OK");
        pop.addAttribute( "", ok );

        ok.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                RepositoryServiceFactory.getService().renamePackage( conf.uuid, name.getText(), new GenericCallback() {
                    public void onSuccess(Object data) {
                        refreshPackageList.execute();
                        conf.name = name.getText();
                        refreshWidgets();
                        Window.alert( "Package renamed successfully." );
                        pop.hide();
                    }
                });
            }
        } );

        pop.show();
    }



    /**
     * Will show a copy dialog for copying the whole package.
     */
    private void showCopyDialog() {
        final FormStylePopup pop = new FormStylePopup("images/new_wiz.gif", "Copy the package");
        pop.addRow( new HTML("<i>Copy the package and all its assets. A new unique name is required.</i>") );
        final TextBox name = new TextBox();
        pop.addAttribute( "New package name:", name );
        Button ok = new Button("OK");
        pop.addAttribute( "", ok );

        ok.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                if (!PackageNameValidator.validatePackageName(name.getText())) {
                    Window.alert("Not a valid package name.");
                    return;
                }
                RepositoryServiceFactory.getService().copyPackage( conf.name, name.getText(), new GenericCallback() {
                    public void onSuccess(Object data) {
                        refreshPackageList.execute();
                        Window.alert( "Package copied successfully." );
                        pop.hide();
                    }
                });
            }
        } );

        pop.show();

    }

    protected void doCopyPackage(String name) {

    }

    private void doSaveAction(final Command refresh) {
        LoadingPopup.showMessage( "Saving package configuration. Please wait ..." );
        RepositoryServiceFactory.getService().savePackage( this.conf, new GenericCallback() {
            public void onSuccess(Object data) {

                previousResponse = (ValidatedResponse) data;

                reload();
                LoadingPopup.showMessage( "Package configuration updated successfully, refreshing content cache..." );

                SuggestionCompletionCache.getInstance().refreshPackage( conf.name, new Command() {
                    public void execute() {
                        if (refresh != null) {
                            refresh.execute();
                        }
                        LoadingPopup.close();
                    }
                });




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

    	return new PackageHeaderWidget(this.conf);

//        final TextArea area = new TextArea();
//        area.setWidth( "100%" );
//        area.setVisibleLines( 8 );
//
//        area.setCharacterWidth( 100 );
//
//        area.setText( this.conf.header );
//        area.addChangeListener( new ChangeListener() {
//            public void onChange(Widget w) {
//                 conf.header = area.getText();
//            }
//        });
//
//
//
//        HorizontalPanel panel = new HorizontalPanel();
//        panel.add( area );
//
//        VerticalPanel vert = new VerticalPanel();
//
//        Image max = new Image("images/max_min.gif");
//        max.addClickListener( new ClickListener() {
//            public void onClick(Widget w) {
//                if (area.getVisibleLines() != 32) {
//                    area.setVisibleLines( 32 );
//                } else {
//                    area.setVisibleLines( 8 );
//                }
//            }
//        } );
//        max.setTitle( "Increase view area." );
//        vert.add( max );
//
//        Image newImport = new Image("images/new_import.gif");
//        newImport.addClickListener( new ClickListener() {
//            public void onClick(Widget w) {
//                area.setText( area.getText(  ) + "\n" +
//                              "import <your class here>");
//                conf.header = area.getText();
//            }
//        });
//        vert.add( newImport );
//        newImport.setTitle( "Add a new Type/Class import to the package." );
//
//        Image newGlobal = new Image("images/new_global.gif");
//        newGlobal.addClickListener( new ClickListener() {
//            public void onClick(Widget w) {
//                area.setText( area.getText() + "\n" +
//                              "global <your class here> <variable name>");
//                conf.header = area.getText();
//            }
//        });
//        newGlobal.setTitle( "Add a new global variable declaration." );
//        vert.add( newGlobal );
//
//        Image newFactTemplate = new Image("images/fact_template.gif");
//        newFactTemplate.addClickListener( new ClickListener() {
//            public void onClick(Widget w) {
//                final FactTemplateWizard wiz = new FactTemplateWizard();
//                wiz.setOKClick( new Command() {
//                    public void execute() {
//                        area.setText( area.getText() + "\n" +
//                                      wiz.getTemplateText() );
//                        conf.header = area.getText();
//
//                    }
//                } );
//                wiz.show();
//            }
//        });
//        newFactTemplate.setTitle( "Add a new fact template." );
//        //vert.add( newFactTemplate );
//
//        panel.setWidth( "100%" );
//
//        panel.add( vert );
//        return panel;
    }


//    private HorizontalPanel expandableTextArea(final TextArea area) {
//        HorizontalPanel panel = new HorizontalPanel();
//        panel.add( area );
//
//        Image max = new Image("images/max_min.gif");
//        max.setTitle( "Increase view area" );
//
//        panel.add( max );
//        max.addClickListener( new ClickListener() {
//            public void onClick(Widget w) {
//                if (area.getVisibleLines() != 32) {
//                    area.setVisibleLines( 32 );
//                } else {
//                    area.setVisibleLines( 8 );
//                }
//            }
//        } );
//        return panel;
//    }

    private Widget description() {

    	final TextBox box = new TextBox();
    	box.setText(conf.description);
    	box.addChangeListener(new ChangeListener() {
			public void onChange(Widget arg0) {
				conf.description = box.getText();
			}
    	});
    	box.setVisibleLength(64);

    	return box;
    }

}