 /*
  *  soapUI, copyright (C) 2004-2012 smartbear.com
  *
  *  soapUI is free software; you can redistribute it and/or modify it under the
  *  terms of version 2.1 of the GNU Lesser General Public License as published by
  *  the Free Software Foundation.
  *
  *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU Lesser General Public License for more details at gnu.org.
  */
 
 package com.eviware.soapui.impl.rest.panels.request;
 
 import com.eviware.soapui.impl.rest.RestMethod;
 import com.eviware.soapui.impl.rest.RestRequestInterface;
 import com.eviware.soapui.impl.rest.RestResource;
 import com.eviware.soapui.impl.rest.support.RestParamProperty;
 import com.eviware.soapui.impl.rest.support.RestUtils;
 import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
 import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
 import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestInterface;
 import com.eviware.soapui.model.ModelItem;
 import com.eviware.soapui.model.iface.Request.SubmitException;
 import com.eviware.soapui.model.iface.Submit;
 import com.eviware.soapui.model.support.TestPropertyListenerAdapter;
 import com.eviware.soapui.model.testsuite.TestProperty;
 import com.eviware.soapui.support.DocumentListenerAdapter;
 import com.eviware.soapui.support.UISupport;
 import com.eviware.soapui.support.components.JUndoableTextField;
 import com.eviware.soapui.support.components.JXToolBar;
 import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
 
 import javax.swing.*;
 import javax.swing.event.DocumentListener;
 import javax.swing.text.Document;
 import java.awt.*;
 import java.awt.event.ItemEvent;
 import java.awt.event.ItemListener;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 
 import static com.eviware.soapui.impl.rest.RestRequestInterface.RequestMethod;
 import static com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle;
 import static com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle.QUERY;
 
 public abstract class AbstractRestRequestDesktopPanel<T extends ModelItem, T2 extends RestRequestInterface> extends
 		AbstractHttpXmlRequestDesktopPanel<T, T2>
 {
 	private boolean updatingRequest;
 	private TextPanelWithTopLabel resourcePanel;
 	private TextPanelWithTopLabel queryPanel;
 	private JUndoableTextField pathTextField;
 	private JComboBox acceptCombo;
 	private JLabel pathLabel;
 	private boolean updating;
 	private InternalTestPropertyListener testPropertyListener = new InternalTestPropertyListener();
 	private RestParamPropertyChangeListener restParamPropertyChangeListener = new RestParamPropertyChangeListener();
 	private JComboBox pathCombo;
 	private JComboBox<RequestMethod> methodComboBox;
 
 	public AbstractRestRequestDesktopPanel( T modelItem, T2 requestItem )
 	{
 		super( modelItem, requestItem );
 
 		if( requestItem.getResource() != null )
 		{
 			requestItem.getResource().addPropertyChangeListener( this );
 		}
 
 		requestItem.addTestPropertyListener( testPropertyListener );
 
 		for( TestProperty param : requestItem.getParams().getProperties().values() )
 		{
 			( ( RestParamProperty )param ).addPropertyChangeListener( restParamPropertyChangeListener );
 		}
 	}
 
 	public void propertyChange( PropertyChangeEvent evt )
 	{
 		updateFullPathLabel();
 
 		if( evt.getPropertyName().equals( "accept" ) && !updatingRequest )
 		{
 			acceptCombo.setSelectedItem( evt.getNewValue() );
 		}
 		else if( evt.getPropertyName().equals( "responseMediaTypes" ) && !updatingRequest )
 		{
 			Object item = acceptCombo.getSelectedItem();
 			acceptCombo.setModel( new DefaultComboBoxModel( ( Object[] )evt.getNewValue() ) );
 			acceptCombo.setSelectedItem( item );
 		}
 		else if( ( evt.getPropertyName().equals( "path" ) || evt.getPropertyName().equals( "restMethod" ) )
 				&& ( getRequest().getResource() == null || getRequest().getResource() == evt.getSource() ) )
 		{
 			if( pathLabel != null )
 			{
 				updateFullPathLabel();
 			}
 
 			if( !updating && pathTextField != null )
 			{
 				updating = true;
 				pathTextField.setText( ( String )evt.getNewValue() );
 				pathTextField.setToolTipText( pathTextField.getText() );
 				updating = false;
 			}
 		}
 
 		super.propertyChange( evt );
 	}
 
 
 	@Override
 	protected Submit doSubmit() throws SubmitException
 	{
 		return getRequest().submit( new WsdlSubmitContext( getModelItem() ), true );
 	}
 
 	@Override
 	protected String getHelpUrl()
 	{
 		return null;
 	}
 
 	@Override
 	protected void insertButtons( JXToolBar toolbar )
 	{
 		if( getRequest().getResource() == null )
 		{
 			addToolbarComponents( toolbar );
 		}
 	}
 
 	@Override
 	protected JComponent buildToolbar()
 	{
 		if( getRequest().getResource() != null )
 		{
 			JComponent baseToolBar = UISupport.createToolbar();
 			baseToolBar.setPreferredSize( new Dimension( 600, 45 ) );
 
 			JComponent submitButton = super.getSubmitButton();
 
 			JPanel methodPanel = new JPanel( new BorderLayout() );
 			methodPanel.setMaximumSize( new Dimension( 75, 45 ) );
 			methodComboBox = new JComboBox<RequestMethod>( new RestRequestMethodModel( getRequest() ) );
 			methodComboBox.setSelectedItem( getRequest().getMethod() );
 
 			JLabel methodLabel = new JLabel( "Method" );
 			methodPanel.add( methodLabel, BorderLayout.NORTH );
 			methodPanel.add( methodComboBox, BorderLayout.SOUTH );
 
 			JPanel endpointPanel = new JPanel( new BorderLayout() );
 			endpointPanel.setMinimumSize( new Dimension( 75, 45 ) );
 
			JComponent endpointCombo = buildEndpointComponent();
 			super.setEndpointComponent( endpointCombo );
 
 			JLabel endPointLabel = new JLabel( "Endpoint" );
 
 			endpointPanel.add( endPointLabel, BorderLayout.NORTH );
 			endpointPanel.add( endpointCombo, BorderLayout.SOUTH );
 
 
 			String path = getRequest().getResource().getPath();
 			resourcePanel = new TextPanelWithTopLabel( "Resource", path, new DocumentListenerAdapter()
 			{
 				@Override
 				public void update( Document document )
 				{
 					getRequest().getResource().setPath( resourcePanel.getText() );
 				}
 			} );
 
 			String query = RestUtils.getQueryParamsString( getRequest().getParams(), getRequest() );
 			queryPanel = new TextPanelWithTopLabel( "Query", query, false );
 
 			baseToolBar.add( submitButton );
 			baseToolBar.add( methodPanel );
 			baseToolBar.add( Box.createHorizontalStrut( 4 ) );
 			baseToolBar.add( endpointPanel );
 			baseToolBar.add( Box.createHorizontalStrut( 4 ) );
 			baseToolBar.add( resourcePanel );
 			baseToolBar.add( Box.createHorizontalStrut( 4 ) );
 			baseToolBar.add( queryPanel );
 
 
 			return baseToolBar;
 		}
 		else
 		{
 			//TODO: If we don't need special clause for empty resources then remove it
 			return super.buildToolbar();
 		}
 	}
 
 	protected void addToolbarComponents( JXToolBar toolbar )
 	{
 		toolbar.addSeparator();
 
 		if( getRequest().getResource() != null )
 		{
 			acceptCombo = new JComboBox( getRequest().getResponseMediaTypes() );
 			acceptCombo.setEditable( true );
 			acceptCombo.setToolTipText( "Sets accepted encoding(s) for response" );
 			acceptCombo.setSelectedItem( getRequest().getAccept() );
 			acceptCombo.addItemListener( new ItemListener()
 			{
 				public void itemStateChanged( ItemEvent e )
 				{
 					updatingRequest = true;
 					getRequest().setAccept( String.valueOf( acceptCombo.getSelectedItem() ) );
 					updatingRequest = false;
 				}
 			} );
 
 			toolbar.addLabeledFixed( "Accept", acceptCombo );
 			toolbar.addSeparator();
 
 			if( getRequest() instanceof RestTestRequestInterface )
 
 			{
 				pathCombo = new JComboBox( new PathComboBoxModel() );
 				pathCombo.setRenderer( new RestMethodListCellRenderer() );
 				pathCombo.setPreferredSize( new Dimension( 200, 20 ) );
 				pathCombo.setSelectedItem( getRequest().getRestMethod() );
 
 				toolbar.addLabeledFixed( "Resource/Method:", pathCombo );
 				toolbar.addSeparator();
 			}
 			else
 			{
 				toolbar.add( new JLabel( "Full Path: " ) );
 			}
 
 			pathLabel = new JLabel();
 			updateFullPathLabel();
 
 			toolbar.add( pathLabel );
 		}
 		else
 		{
 			pathTextField = new JUndoableTextField();
 			pathTextField.setPreferredSize( new Dimension( 300, 20 ) );
 			pathTextField.setText( getRequest().getPath() );
 			pathTextField.setToolTipText( pathTextField.getText() );
 			pathTextField.getDocument().addDocumentListener( new DocumentListenerAdapter()
 			{
 				@Override
 				public void update( Document document )
 				{
 					if( updating )
 						return;
 
 					updating = true;
 					getRequest().setPath( pathTextField.getText() );
 					updating = false;
 				}
 			} );
 			PropertyExpansionPopupListener.enable( pathTextField, getModelItem() );
 
 			toolbar.addLabeledFixed( "Request URL:", pathTextField );
 		}
 
 		toolbar.addSeparator();
 	}
 
 	protected boolean release()
 	{
 		if( getRequest().getResource() != null )
 		{
 			getRequest().getResource().removePropertyChangeListener( this );
 		}
 
 		getRequest().removeTestPropertyListener( testPropertyListener );
 
 		for( TestProperty param : getRequest().getParams().getProperties().values() )
 		{
 			( ( RestParamProperty )param ).removePropertyChangeListener( restParamPropertyChangeListener );
 		}
 
 		return super.release();
 	}
 
 	private class InternalTestPropertyListener extends TestPropertyListenerAdapter
 	{
 		@Override
 		public void propertyValueChanged( String name, String oldValue, String newValue )
 		{
 			RestParamProperty property = getRequest().getParams().getProperty( name );
 			ParameterStyle style = property.getStyle();
 			if( style.equals( QUERY ) )
 			{
 				resetQueryPanelText();
 			}
 			else if( style.equals( ParameterStyle.TEMPLATE ) )
 			{
 				//resourcePanel.setText(  );
 			}
 			updateFullPathLabel();
 		}
 
 		@Override
 		public void propertyAdded( String name )
 		{
 			updateFullPathLabel();
 
 			getRequest().getParams().getProperty( name ).addPropertyChangeListener( restParamPropertyChangeListener );
 		}
 
 		@Override
 		public void propertyRemoved( String name )
 		{
 			updateFullPathLabel();
 		}
 
 		@Override
 		public void propertyRenamed( String oldName, String newName )
 		{
 			updateFullPathLabel();
 		}
 	}
 
 	private void resetQueryPanelText()
 	{
 		queryPanel.setText( RestUtils.getQueryParamsString( getRequest().getParams(), getRequest() ) );
 	}
 
 	private void updateFullPathLabel()
 	{
 		if( pathLabel != null && getRequest().getResource() != null )
 		{
 			String text = RestUtils.expandPath( getRequest().getResource().getFullPath(), getRequest().getParams(),
 					getRequest() );
 			pathLabel.setText( "[" + text + "]" );
 			pathLabel.setToolTipText( text );
 		}
 	}
 
 
 	private class RestParamPropertyChangeListener implements PropertyChangeListener
 	{
 		public void propertyChange( PropertyChangeEvent evt )
 		{
 			removeParamForStyle( (RestParamProperty)evt.getSource(), ( ParameterStyle )evt.getOldValue() );
 			addPropertyForStyle( (RestParamProperty)evt.getSource(), ( ParameterStyle ) evt.getNewValue() );
 			updateFullPathLabel();
 		}
 
 
 	}
 
 	private void removeParamForStyle( RestParamProperty property, ParameterStyle style )
 	{
 		switch( style )
 		{
 			case QUERY:
 				resetQueryPanelText();
 				break;
 			case TEMPLATE:
 				resourcePanel.setText( resourcePanel.getText().replaceAll( "\\{"+property.getName() +"\\}","" ) );
 				break;
 			case MATRIX:
 				resourcePanel.setText( resourcePanel.getText().replaceAll( ";"+property.getName() +"=" +
 						property.getValue(),"" ) );
 				break;
 			default:
 				break;
 		}
 	}
 
 	private void addPropertyForStyle( RestParamProperty property, ParameterStyle style )
 	{
 		switch( style )
 		{
 			case QUERY:
 				resetQueryPanelText();
 				break;
 			case TEMPLATE:
 				resourcePanel.setText( resourcePanel.getText() + "{"+property.getName() +"}" );
 				break;
 			case MATRIX:
 				resourcePanel.setText( resourcePanel.getText() + ";"+property.getName() +"=" + property.getValue() );
 				break;
 			default:
 				break;
 		}
 	}
 
 	private class PathComboBoxModel extends AbstractListModel implements ComboBoxModel
 	{
 		public int getSize()
 		{
 			int sz = 0;
 			for( RestResource resource : getRequest().getResource().getService().getAllResources() )
 			{
 				sz += resource.getRestMethodCount();
 			}
 
 			return sz;
 		}
 
 		public Object getElementAt( int index )
 		{
 			int sz = 0;
 			for( RestResource resource : getRequest().getResource().getService().getAllResources() )
 			{
 				if( index < sz + resource.getRestMethodCount() )
 				{
 					return resource.getRestMethodAt( index - sz );
 				}
 
 				sz += resource.getRestMethodCount();
 			}
 
 			return null;
 		}
 
 		public void setSelectedItem( Object anItem )
 		{
 			( ( RestTestRequestInterface )getRequest() ).getTestStep().setRestMethod( ( RestMethod )anItem );
 		}
 
 		public Object getSelectedItem()
 		{
 			return getRequest().getRestMethod();
 		}
 	}
 
 	private class RestMethodListCellRenderer extends DefaultListCellRenderer
 	{
 		@Override
 		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected,
 																	  boolean cellHasFocus )
 		{
 			Component result = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
 
 			if( value instanceof RestMethod )
 			{
 				RestMethod item = ( RestMethod )value;
 				setIcon( item.getIcon() );
 				setText( item.getResource().getName() + " -> " + item.getName() );
 			}
 
 			return result;
 		}
 
 	}
 
 
 	private class TextPanelWithTopLabel extends JPanel
 	{
 		JLabel textLabel;
 		JTextField textField;
 
 		public TextPanelWithTopLabel( String label, String text, boolean isEditable )
 		{
 			textLabel = new JLabel( label );
 			textField = new JTextField( text );
 			textField.setEditable( isEditable );
 			super.setLayout( new BorderLayout() );
 			super.add( textLabel, BorderLayout.NORTH );
 			super.add( textField, BorderLayout.SOUTH );
 		}
 
 		public TextPanelWithTopLabel( String label, String text, DocumentListener documentListener )
 		{
 			textLabel = new JLabel( label );
 			textField = new JTextField( text );
 			textField.getDocument().addDocumentListener( documentListener );
 			super.setLayout( new BorderLayout() );
 			super.add( textLabel, BorderLayout.NORTH );
 			super.add( textField, BorderLayout.SOUTH );
 		}
 
 		public String getText()
 		{
 			return textField.getText();
 		}
 
 		public void setText( String text )
 		{
 			textField.setText( text );
 		}
 	}
 
 }
