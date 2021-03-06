 package dk.teachus.frontend.pages.persons;
 
 import wicket.ajax.AjaxRequestTarget;
 import wicket.markup.html.basic.Label;
 import wicket.markup.html.form.validation.EqualInputValidator;
 import wicket.markup.html.form.validation.IFormValidator;
 import wicket.markup.html.form.validation.StringValidator;
 import wicket.model.PropertyModel;
 import dk.teachus.bean.MailBean;
 import dk.teachus.dao.PersonDAO;
 import dk.teachus.domain.Pupil;
 import dk.teachus.domain.impl.WelcomeIntroductionTeacherAttribute;
 import dk.teachus.frontend.TeachUsApplication;
 import dk.teachus.frontend.TeachUsSession;
 import dk.teachus.frontend.UserLevel;
 import dk.teachus.frontend.components.form.ButtonPanelElement;
 import dk.teachus.frontend.components.form.FormPanel;
 import dk.teachus.frontend.components.form.GeneratePasswordElement;
 import dk.teachus.frontend.components.form.PasswordFieldElement;
 import dk.teachus.frontend.components.form.TextAreaElement;
 import dk.teachus.frontend.components.form.FormPanel.FormValidator;
 import dk.teachus.frontend.models.PupilModel;
 import dk.teachus.frontend.pages.AuthenticatedBasePage;
 
 public class SendNewPasswordPage extends AuthenticatedBasePage {
 	private static final long serialVersionUID = 1L;
 
 	private String password1;
 	private String password2;
 	private String introMessage;
 	
 	public SendNewPasswordPage(Long pupilId) {
 		super(UserLevel.TEACHER, true);
 		
 		if (pupilId == null) {
 			throw new IllegalArgumentException("Must provide a valid pupilId"); //$NON-NLS-1$
 		}
 		
 		final PupilModel pupilModel = new PupilModel(pupilId);
 		
 		// Find intro message from teachers attributes
 		PersonDAO personDAO = TeachUsApplication.get().getPersonDAO();
 		WelcomeIntroductionTeacherAttribute welcomeIntroduction = personDAO.getAttribute(WelcomeIntroductionTeacherAttribute.class, pupilModel.getObject(this).getTeacher());
 		if (welcomeIntroduction != null) {
 			setIntroMessage(welcomeIntroduction.getValue());
 		}
 		
 		String title = TeachUsSession.get().getString("SendNewPasswordPage.title"); //$NON-NLS-1$
 		title = title.replace("{pupilname}", pupilModel.getObject(this).getName()); //$NON-NLS-1$
		add(new Label("title", title)); //$NON-NLS-1$
 		
 		FormPanel formPanel = new FormPanel("passwordForm"); //$NON-NLS-1$
 		add(formPanel);
 		
 		// Password 1
 		final PasswordFieldElement password1Field = new PasswordFieldElement(TeachUsSession.get().getString("General.password"), new PropertyModel(this, "password1"), true); //$NON-NLS-1$ //$NON-NLS-2$
 		password1Field.add(StringValidator.lengthBetween(4, 32));
 		formPanel.addElement(password1Field);
 		
 		// Password 2
 		final PasswordFieldElement password2Field = new PasswordFieldElement(TeachUsSession.get().getString("PersonPanel.repeatPassword"), new PropertyModel(this, "password2"), true); //$NON-NLS-1$ //$NON-NLS-2$
 		formPanel.addElement(password2Field);
 		
 		// Password validator
 		formPanel.addValidator(new FormValidator() {
 			private static final long serialVersionUID = 1L;
 
 			public IFormValidator getFormValidator() {
 				return new EqualInputValidator(password1Field.getFormComponent(), password2Field.getFormComponent());
 			}			
 		});
 		
 		// Password generator
 		formPanel.addElement(new GeneratePasswordElement("", pupilModel.getObject(this).getUsername()) { //$NON-NLS-1$
 			private static final long serialVersionUID = 1L;
 
 			@Override
 			protected void passwordGenerated(AjaxRequestTarget target, String password) {
 				setPassword1(password);
 				setPassword2(password);
 				
 				target.addComponent(password1Field.getFormComponent());
 				target.addComponent(password1Field.getFeedbackPanel());
 				target.addComponent(password2Field.getFormComponent());
 				target.addComponent(password2Field.getFeedbackPanel());
 			}			
 		});
 		
 		// Text
 		formPanel.addElement(new TextAreaElement(TeachUsSession.get().getString("SendNewPasswordPage.introMessage"), new PropertyModel(this, "introMessage"))); //$NON-NLS-1$ //$NON-NLS-2$
 		
 		// Buttons
 		formPanel.addElement(new ButtonPanelElement(TeachUsSession.get().getString("General.send")) { //$NON-NLS-1$
 			private static final long serialVersionUID = 1L;
 
 			@Override
 			protected void onCancel(AjaxRequestTarget target) {
 				getRequestCycle().setResponsePage(PupilsPage.class);
 			}
 
 			@Override
 			protected void onSave(AjaxRequestTarget target) {
 				MailBean mailBean = TeachUsApplication.get().getMailBean();
 				PersonDAO personDAO = TeachUsApplication.get().getPersonDAO();
 				
 				Pupil pupil = pupilModel.getObject(SendNewPasswordPage.this);
 				pupil.setPassword(getPassword1());
 				
 				personDAO.save(pupil);
 				
 				mailBean.sendWelcomeMail(pupil, getIntroMessage(), TeachUsApplication.get().getServerName());
 				
 				getRequestCycle().setResponsePage(PupilsPage.class);
 			}			
 		});
 	}
 
 	@Override
 	protected AuthenticatedPageCategory getPageCategory() {
 		return AuthenticatedPageCategory.PUPILS;
 	}
 
 	@Override
 	protected String getPageLabel() {
 		return TeachUsSession.get().getString("SendNewPasswordPage.sendNewPassword"); //$NON-NLS-1$
 	}
 
 	public String getPassword1() {
 		return password1;
 	}
 
 	public void setPassword1(String password1) {
 		this.password1 = password1;
 	}
 
 	public String getPassword2() {
 		return password2;
 	}
 
 	public void setPassword2(String password2) {
 		this.password2 = password2;
 	}
 	
 	public String getIntroMessage() {
 		return introMessage;
 	}
 
 	public void setIntroMessage(String introMessage) {
 		this.introMessage = introMessage;
 	}
 
 }
